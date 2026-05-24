package co.electriccoin.zcash.ui.common.provider

import cash.z.ecc.android.sdk.model.Memo
import cash.z.ecc.android.sdk.model.WalletAddress
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZecSend
import cash.z.ecc.android.sdk.type.AddressType
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.common.datasource.AFFILIATE_ADDRESS
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.datasource.SwapDataSource
import co.electriccoin.zcash.ui.common.model.KeystoneAccount
import co.electriccoin.zcash.ui.common.model.SubmitResult
import co.electriccoin.zcash.ui.common.model.SwapAsset
import co.electriccoin.zcash.ui.common.model.SwapMode
import co.electriccoin.zcash.ui.common.model.SwapQuote
import co.electriccoin.zcash.ui.common.model.SwapStatus
import co.electriccoin.zcash.ui.common.model.ZashiAccount
import co.electriccoin.zcash.ui.common.model.ZecSwapAsset
import co.electriccoin.zcash.ui.common.repository.KeystoneProposalRepository
import co.electriccoin.zcash.ui.common.repository.SubmitProposalState
import co.electriccoin.zcash.ui.common.repository.ZashiProposalRepository
import co.electriccoin.zcash.ui.common.usecase.SubmitProposalUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.funding.FundingOutcome
import xyz.justzappit.offramp.funding.OfframpFunding
import xyz.justzappit.offramp.funding.OfframpRefund
import xyz.justzappit.offramp.orchestrator.OfframpRequest
import xyz.justzappit.offramp.p2p.Usdc6
import xyz.justzappit.offramp.p2p.getUsdcBalance
import java.math.BigDecimal

/**
 * Wallet-side half of the offramp NEAR bridge that offramp-lib (pure JVM) can't provide: the user's
 * Zcash address (1-Click `refundTo` for bridge-in, recipient for pull-back) and the **user-confirmed**
 * ZEC deposit send. Implemented by [RealOfframpBridgeWallet], which reuses the app's existing send
 * pipeline. Mainnet offramp still stays gated by `ProviderModule`'s account-provider kill-switch until
 * a mainnet account source + end-to-end validation land, so this seam isn't hit in a shipped build yet.
 */
interface OfframpBridgeWallet {
    /** Wallet receive address used as 1-Click `refundTo` (bridge-in) and recipient (pull-back). */
    suspend fun zcashAddress(): String

    /**
     * Sends the bridge's ZEC input (per [quote]) to the 1-Click deposit address with explicit user
     * authorization, returning the Zcash deposit tx id once submitted.
     */
    suspend fun sendZecDeposit(quote: SwapQuote): String
}

/**
 * Reuses the app's existing send pipeline to fund the bridge: build a swap proposal for the 1-Click
 * [quote], authorize + submit through [SubmitProposalUseCase] (biometrics → proposal-repository submit,
 * which for a swap proposal also notifies 1-Click of the deposit tx), then await the terminal submit
 * state for the deposit tx id. The swap UI flow itself is not modified — only its repositories/use case
 * are reused, exactly as `RequestSwapQuoteUseCase` builds and submits a swap.
 *
 * MAINNET-VALIDATION: a cancelled biometric prompt never resolves [submitState], so the await relies on
 * the surrounding offramp flow being cancelled by the user. Keystone signing still routes through the
 * QR sign screen — pre-Keystone-support [navigateAfter=false] is Zashi-only; the Keystone path will
 * need a separate seam.
 */
class RealOfframpBridgeWallet(
    private val accountDataSource: AccountDataSource,
    private val zashiProposalRepository: ZashiProposalRepository,
    private val keystoneProposalRepository: KeystoneProposalRepository,
    private val submitProposal: SubmitProposalUseCase,
    private val synchronizerProvider: SynchronizerProvider,
) : OfframpBridgeWallet {
    override suspend fun zcashAddress(): String = accountDataSource.requestNextShieldedAddress().address

    override suspend fun sendZecDeposit(quote: SwapQuote): String {
        val send =
            ZecSend(
                destination = walletAddress(quote.depositAddress.address),
                amount = Zatoshi(quote.amountIn.toLong()),
                memo = Memo(""),
                proposal = null,
            )
        // Build the proposal the same way the swap flow does, per account type.
        val submitState: Flow<SubmitProposalState?> =
            when (accountDataSource.getSelectedAccount()) {
                is KeystoneAccount -> {
                    keystoneProposalRepository.createExactOutputSwapProposal(send, quote)
                    keystoneProposalRepository.createPCZTFromProposal()
                    keystoneProposalRepository.submitState
                }

                is ZashiAccount -> {
                    zashiProposalRepository.createExactOutputSwapProposal(send, quote)
                    zashiProposalRepository.submitState
                }
            }
        // Keep the user on the offramp progress screen. `navigateAfter = false` suppresses the
        // default replace(TransactionProgressArgs) the standard send/swap UX relies on — see
        // SubmitProposalUseCase.invoke kdoc. The Zashi submit still runs on a background coroutine
        // and `submitState` resolves the same way.
        submitProposal(navigateAfter = false)
        val result = submitState.filterIsInstance<SubmitProposalState.Result>().first().submitResult
        return when (result) {
            is SubmitResult.Success -> {
                result.txIds.firstOrNull()
                    ?: error("ZEC bridge deposit submitted but returned no transaction id")
            }

            else -> {
                error("ZEC bridge deposit did not succeed: $result")
            }
        }
    }

    private suspend fun walletAddress(address: String): WalletAddress =
        when (val r = synchronizerProvider.getSynchronizer().validateAddress(address)) {
            AddressType.Shielded -> WalletAddress.Sapling.new(address)
            AddressType.Tex -> WalletAddress.Tex.new(address)
            AddressType.Transparent -> WalletAddress.Transparent.new(address)
            AddressType.Unified -> WalletAddress.Unified.new(address)
            is AddressType.Invalid -> error("1-Click deposit address invalid: ${r.reason}")
        }
}

/**
 * Mainnet funding: bridges ZEC → USDC into the smart account via NEAR 1-Click, **reusing** the app's
 * existing [SwapDataSource] for the quote and status polling (it is not modified here). The bridge is
 * `EXACT_OUTPUT` so it delivers exactly the order's USDC and refunds any excess ZEC to the user.
 *
 * Resumable + idempotent (per [OfframpFunding]): a non-null `resumeHandle` re-polls the already-opened
 * deposit address instead of quoting a second bridge, and `onBridgeStarted` fires the moment the deposit
 * address is known — before any ZEC moves — so the orchestrator persists it first.
 */
class NearBridgeOfframpFunding(
    private val rpc: BaseRpcClient,
    private val usdc: Address,
    private val swapDataSource: SwapDataSource,
    private val wallet: OfframpBridgeWallet,
    private val slippageTolerancePercent: BigDecimal = DEFAULT_SLIPPAGE_PERCENT,
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
) : OfframpFunding {
    override suspend fun ensureFunded(
        account: Address,
        request: OfframpRequest,
        resumeHandle: String?,
        onBridgeStarted: suspend (depositAddress: String) -> Unit,
    ): FundingOutcome {
        val initialBalance = rpc.getUsdcBalance(usdc, account)
        if (initialBalance >= request.usdcAmount) {
            return FundingOutcome.AlreadyFunded(currentBalance = initialBalance)
        }

        val tokens = swapDataSource.getSupportedTokens()
        val depositAddress =
            if (resumeHandle != null) {
                // Re-emit so the UI's BridgingFunds row repaints with the persisted address on resume.
                onBridgeStarted(resumeHandle)
                resumeHandle
            } else {
                openBridge(account, request, tokens, onBridgeStarted)
            }

        pollUntilSettled(depositAddress, tokens)
        check(rpc.getUsdcBalance(usdc, account) >= request.usdcAmount) {
            "NEAR bridge settled but ${account.checksumHex} is still under-funded for the order."
        }
        return FundingOutcome.Bridged(depositAddress = depositAddress)
    }

    private suspend fun openBridge(
        account: Address,
        request: OfframpRequest,
        tokens: List<SwapAsset>,
        onBridgeStarted: suspend (depositAddress: String) -> Unit,
    ): String {
        val quote =
            swapDataSource.requestQuote(
                swapMode = SwapMode.EXACT_OUTPUT,
                flexInput = false,
                amount = request.usdcAmount.whole,
                refundAddress = wallet.zcashAddress(),
                originAsset = zecAsset(tokens),
                destinationAddress = account.checksumHex,
                destinationAsset = usdcAsset(tokens),
                slippage = slippageTolerancePercent,
                affiliateAddress = AFFILIATE_ADDRESS,
            )
        val depositAddress = quote.depositAddress.address
        // Persist the 1-Click handle BEFORE any ZEC moves; a crash between send and persist would
        // otherwise let resume open a second bridge and double-send the user's ZEC.
        onBridgeStarted(depositAddress)
        wallet.sendZecDeposit(quote)
        return depositAddress
    }

    /**
     * The bridge runs server-side on 1-Click regardless of our polling cadence, so a transient
     * HTTP error here (wifi blip, 5xx, transient timeout) must not propagate — bubbling it would
     * make the orchestrator emit Failed(FUNDING), clear the checkpoint, and orphan the user's
     * in-flight ZEC with no resume path. Only a *terminal* [SwapStatus] from 1-Click counts as
     * the bridge actually dying — and in that case we throw [BridgeTerminallyFailedException],
     * which the checkpoint persister recognises to clear the checkpoint (re-polling the same
     * handle would just yield the same terminal status forever). Cancellation escapes normally
     * for coroutine teardown.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun pollUntilSettled(depositAddress: String, tokens: List<SwapAsset>) {
        while (true) {
            val status =
                try {
                    swapDataSource.checkSwapStatus(depositAddress, tokens).status
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Twig.warn(e) {
                        "NearBridgeOfframpFunding.pollUntilSettled: transient checkSwapStatus failure " +
                            "for $depositAddress — retrying in ${pollIntervalMs}ms"
                    }
                    delay(pollIntervalMs)
                    continue
                }
            when (status) {
                SwapStatus.SUCCESS -> {
                    return
                }

                SwapStatus.REFUNDED, SwapStatus.FAILED, SwapStatus.EXPIRED, SwapStatus.INCOMPLETE_DEPOSIT -> {
                    throw BridgeTerminallyFailedException(terminalStatus = status, depositAddress = depositAddress)
                }

                else -> {
                    delay(pollIntervalMs)
                }
            }
        }
    }

    private fun zecAsset(tokens: List<SwapAsset>): SwapAsset =
        tokens.filterIsInstance<ZecSwapAsset>().firstOrNull()
            ?: error("ZEC is not in the 1-Click supported-token list")

    // 1-Click asset ids embed the on-chain address (e.g. "nep141:base-0x833589…omft.near"), so match
    // USDC by the configured contract address rather than hardcoding a NEP asset id per network.
    private fun usdcAsset(tokens: List<SwapAsset>): SwapAsset =
        tokens.firstOrNull { it.assetId.contains(usdc.lowercaseHex.removePrefix("0x"), ignoreCase = true) }
            ?: error("USDC (${usdc.checksumHex}) is not in the 1-Click supported-token list")

    private companion object {
        const val DEFAULT_POLL_INTERVAL_MS = 5_000L
        val DEFAULT_SLIPPAGE_PERCENT: BigDecimal = BigDecimal("1")
    }
}

/**
 * Thrown by [NearBridgeOfframpFunding.pollUntilSettled] when 1-Click has surfaced a non-recoverable
 * terminal [SwapStatus] for the bridge: REFUNDED (ZEC returned), FAILED (bridge dead),
 * EXPIRED (quote expired), or INCOMPLETE_DEPOSIT (user under-sent).
 *
 * Why a typed exception rather than just a string: the persister (sibling
 * `OfframpCheckpointPersister`) keys off the cause type to decide whether to keep the checkpoint
 * (in-flight bridge, user can resume) or to clear it (bridge is dead, re-polling the same handle
 * yields the same terminal status indefinitely). Keying off `Failed.message` substrings would be
 * fragile to copy edits; the type is structural.
 *
 * The orchestrator's generic catch (Throwable) captures this as `Failed.cause`; UI rendering still
 * works off the (English) [message] as a fallback, but the persister's clear-vs-keep decision is
 * based on `cause is BridgeTerminallyFailedException`.
 */
class BridgeTerminallyFailedException(
    val terminalStatus: SwapStatus,
    val depositAddress: String,
) : RuntimeException(
        "NEAR bridge for $depositAddress reached terminal state $terminalStatus — the bridge cannot be resumed. " +
            "If your ZEC was refunded by 1-Click it should appear at your wallet's refund address shortly.",
    )

/**
 * Mainnet pull-back: resolves a NEAR 1-Click deposit address for a USDC → ZEC swap so the orchestrator
 * can transfer refunded USDC to it and the user receives ZEC. Reuses [SwapDataSource] for the quote; the
 * returned address is on Base (origin = USDC on Base), which is what the sponsored `USDC.transfer` targets.
 */
class NearPullbackOfframpRefund(
    private val usdc: Address,
    private val swapDataSource: SwapDataSource,
    private val wallet: OfframpBridgeWallet,
    private val slippageTolerancePercent: BigDecimal = BigDecimal("1"),
) : OfframpRefund {
    override suspend fun pullbackTarget(account: Address, amount: Usdc6): Address {
        val tokens = swapDataSource.getSupportedTokens()
        val quote =
            swapDataSource.requestQuote(
                swapMode = SwapMode.EXACT_INPUT,
                flexInput = false,
                amount = amount.whole,
                refundAddress = account.checksumHex,
                originAsset =
                    tokens.firstOrNull { it.assetId.contains(usdc.lowercaseHex.removePrefix("0x"), ignoreCase = true) }
                        ?: error("USDC (${usdc.checksumHex}) is not in the 1-Click supported-token list"),
                destinationAddress = wallet.zcashAddress(),
                destinationAsset =
                    tokens.filterIsInstance<ZecSwapAsset>().firstOrNull()
                        ?: error("ZEC is not in the 1-Click supported-token list"),
                slippage = slippageTolerancePercent,
                affiliateAddress = AFFILIATE_ADDRESS,
            )
        return Address.parse(quote.depositAddress.address)
    }
}
