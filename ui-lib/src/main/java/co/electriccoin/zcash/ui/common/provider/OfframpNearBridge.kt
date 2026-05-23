package co.electriccoin.zcash.ui.common.provider

import cash.z.ecc.android.sdk.model.Memo
import cash.z.ecc.android.sdk.model.WalletAddress
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZecSend
import cash.z.ecc.android.sdk.type.AddressType
import co.electriccoin.zcash.ui.common.datasource.AFFILIATE_ADDRESS
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.datasource.SwapDataSource
import co.electriccoin.zcash.ui.common.model.KeystoneAccount
import co.electriccoin.zcash.ui.common.model.SwapAsset
import co.electriccoin.zcash.ui.common.model.SwapMode
import co.electriccoin.zcash.ui.common.model.SwapQuote
import co.electriccoin.zcash.ui.common.model.SwapStatus
import co.electriccoin.zcash.ui.common.model.SubmitResult
import co.electriccoin.zcash.ui.common.model.ZashiAccount
import co.electriccoin.zcash.ui.common.model.ZecSwapAsset
import co.electriccoin.zcash.ui.common.repository.KeystoneProposalRepository
import co.electriccoin.zcash.ui.common.repository.SubmitProposalState
import co.electriccoin.zcash.ui.common.repository.ZashiProposalRepository
import co.electriccoin.zcash.ui.common.usecase.SubmitProposalUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.funding.OfframpFunding
import xyz.justzappit.offramp.funding.OfframpRefund
import xyz.justzappit.offramp.orchestrator.OfframpRequest
import xyz.justzappit.offramp.p2p.Erc20Calls
import xyz.justzappit.offramp.p2p.Usdc6
import java.math.BigDecimal
import java.math.BigInteger

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
 * MAINNET-VALIDATION: two integration points must be exercised on mainnet — (1) [SubmitProposalUseCase]
 * navigates to the transaction-progress screen, so the offramp progress UX has to reconcile that
 * round-trip; (2) a cancelled biometric prompt never resolves [submitState], so the await relies on the
 * surrounding offramp flow being cancelled by the user. Keystone signing additionally routes through the
 * QR sign screen.
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
        val send = ZecSend(
            destination = walletAddress(quote.depositAddress.address),
            amount = Zatoshi(quote.amountIn.toLong()),
            memo = Memo(""),
            proposal = null,
        )
        // Build the proposal the same way the swap flow does, per account type.
        val submitState: Flow<SubmitProposalState?> = when (accountDataSource.getSelectedAccount()) {
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
        submitProposal()
        val result = submitState.filterIsInstance<SubmitProposalState.Result>().first().submitResult
        return when (result) {
            is SubmitResult.Success -> result.txIds.firstOrNull()
                ?: error("ZEC bridge deposit submitted but returned no transaction id")
            else -> error("ZEC bridge deposit did not succeed: $result")
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
    ) {
        if (balanceOf(account) >= request.usdcAmount.micros) return

        val tokens = swapDataSource.getSupportedTokens()
        val depositAddress = if (resumeHandle != null) {
            // Re-emit so the UI's BridgingFunds row repaints with the persisted address on resume.
            onBridgeStarted(resumeHandle)
            resumeHandle
        } else {
            openBridge(account, request, tokens, onBridgeStarted)
        }

        pollUntilSettled(depositAddress, tokens)
        check(balanceOf(account) >= request.usdcAmount.micros) {
            "NEAR bridge settled but ${account.checksumHex} is still under-funded for the order."
        }
    }

    private suspend fun openBridge(
        account: Address,
        request: OfframpRequest,
        tokens: List<SwapAsset>,
        onBridgeStarted: suspend (depositAddress: String) -> Unit,
    ): String {
        val quote = swapDataSource.requestQuote(
            swapMode = SwapMode.EXACT_OUTPUT,
            flexInput = false,
            amount = request.usdcAmount.toWholeUnits(),
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

    private suspend fun pollUntilSettled(depositAddress: String, tokens: List<SwapAsset>) {
        while (true) {
            when (swapDataSource.checkSwapStatus(depositAddress, tokens).status) {
                SwapStatus.SUCCESS -> return
                SwapStatus.REFUNDED, SwapStatus.FAILED, SwapStatus.EXPIRED, SwapStatus.INCOMPLETE_DEPOSIT ->
                    error("NEAR bridge did not deliver USDC for $depositAddress — the user's ZEC was refunded.")
                else -> delay(pollIntervalMs)
            }
        }
    }

    private suspend fun balanceOf(account: Address): BigInteger {
        val ret = rpc.ethCall(to = usdc, data = Erc20Calls.balanceOfCalldata(account))
        return if (ret.isEmpty()) BigInteger.ZERO else BigInteger(1, ret)
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
        val quote = swapDataSource.requestQuote(
            swapMode = SwapMode.EXACT_INPUT,
            flexInput = false,
            amount = amount.toWholeUnits(),
            refundAddress = account.checksumHex,
            originAsset = tokens.firstOrNull { it.assetId.contains(usdc.lowercaseHex.removePrefix("0x"), ignoreCase = true) }
                ?: error("USDC (${usdc.checksumHex}) is not in the 1-Click supported-token list"),
            destinationAddress = wallet.zcashAddress(),
            destinationAsset = tokens.filterIsInstance<ZecSwapAsset>().firstOrNull()
                ?: error("ZEC is not in the 1-Click supported-token list"),
            slippage = slippageTolerancePercent,
            affiliateAddress = AFFILIATE_ADDRESS,
        )
        return Address.parse(quote.depositAddress.address)
    }
}

/** USDC micros → whole-token `BigDecimal` (6 decimals), the unit `SwapDataSource.requestQuote` expects. */
private fun Usdc6.toWholeUnits(): BigDecimal = BigDecimal(micros).movePointLeft(USDC_DECIMALS)

private const val USDC_DECIMALS = 6
