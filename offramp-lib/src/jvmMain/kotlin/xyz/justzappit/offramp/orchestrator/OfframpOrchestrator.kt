package xyz.justzappit.offramp.orchestrator

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import xyz.justzappit.evm.abi.AbiEncoder
import xyz.justzappit.evm.crypto.Ecies
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.rpc.RpcException
import xyz.justzappit.evm.signer.TxSubmitter
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.TxHash
import xyz.justzappit.evm.util.toHex
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.funding.OfframpFunding
import xyz.justzappit.offramp.funding.OfframpRefund
import xyz.justzappit.offramp.p2p.CircleId
import xyz.justzappit.offramp.p2p.CircleRouter
import xyz.justzappit.offramp.p2p.DiamondCalls
import xyz.justzappit.offramp.p2p.Erc20Calls
import xyz.justzappit.offramp.p2p.OrderEvents
import xyz.justzappit.offramp.p2p.OnChainOrderReader
import xyz.justzappit.offramp.p2p.OrderReadSource
import xyz.justzappit.offramp.p2p.OrderReader
import xyz.justzappit.offramp.p2p.OrderSnapshot
import xyz.justzappit.offramp.p2p.OrderStatus
import xyz.justzappit.offramp.p2p.OrderType
import xyz.justzappit.offramp.p2p.PlaceOrderArgs
import xyz.justzappit.offramp.p2p.RelayIdentities
import xyz.justzappit.offramp.p2p.SubgraphClient
import xyz.justzappit.offramp.p2p.Usdc6
import java.math.BigInteger

/**
 * Surface for the VM layer to depend on. Decouples the UI VM from the concrete RPC/signer wiring
 * so tests can substitute a scripted flow without standing up a real RPC stack.
 */
interface OfframpDriver {
    fun run(request: OfframpRequest): Flow<OfframpStatus>
    fun resume(checkpoint: OfframpCheckpoint): Flow<OfframpStatus>

    /**
     * Single user intent: "get my USDC back to ZEC". State-aware — reads the on-chain order (if
     * [orderId] given), picks the right cleanup contract call, then transfers any USDC sitting in
     * the smart account to the NEAR pullback target (mainnet) or leaves it self-custodial (testnet).
     *
     *  - ACCEPTED / PAID    → `cancelOrder` (user-permitted, refunds escrow) + transfer
     *  - PLACED + expired   → `autoCancelExpiredOrders` (permissionless cleanup) + transfer
     *  - PLACED + active    → transfer only (PAY/SELL hold no escrow at PLACED — funds are still
     *                          in the smart account)
     *  - CANCELLED / null   → transfer only (nothing to cancel)
     *
     * Emits [OfframpStatus.FundsRecovered] on success, [OfframpStatus.Failed] on revert.
     */
    fun bridgeFundsBackToZec(orderId: BigInteger?): Flow<OfframpStatus>
}

class OfframpOrchestrator(
    private val rpc: BaseRpcClient,
    private val submitter: TxSubmitter,
    private val accountAddress: Address,
    private val network: P2pNetworkConfig,
    private val subgraph: SubgraphClient,
    private val orderReader: OrderReadSource,
    private val funding: OfframpFunding,
    private val refund: OfframpRefund,
    private val router: CircleRouter = CircleRouter(),
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
    /**
     * After this duration of polling with no terminal transition, the WaitingFor* status emits
     * with `stalled = true` so the UI can hint that the order is taking longer than usual. There
     * is no client-side timeout — the order remains live on-chain until merchant acceptance,
     * completion, the user cancelling, or the executor's order-sweeper auto-cancelling once the
     * Diamond's getOrderExpiry() window (30 min) elapses. Killing flows on a client clock would
     * orphan the user's escrowed USDC.
     */
    private val stalledAfterMs: Long = DEFAULT_STALLED_AFTER_MS,
    /**
     * Clock used to compute the stalled-flag deadline. Defaults to wall-clock; tests inject a
     * controllable monotonic counter because `runTest`'s virtual time does not advance
     * `clockMs()`.
     */
    private val clockMs: () -> Long = System::currentTimeMillis,
    /**
     * Authoritative on-chain order reader used to verify the merchant encryption pubkey before we
     * encrypt the user's UPI to it (the polling [orderReader] is subgraph-primary and untrusted for
     * this). Defaults to a direct `getOrdersById` reader; injectable for tests.
     */
    private val onChainOrderReader: OrderReadSource = OnChainOrderReader(rpc, network),
) : OfframpDriver {
    override fun run(request: OfframpRequest): Flow<OfframpStatus> = flow {
        emit(OfframpStatus.Idle)
        driveNewOrder(request, resumeBridgeHandle = null)
    }

    /**
     * Drives a fresh — or bridge-resumed — order from circle selection through completion.
     * [resumeBridgeHandle] is a persisted 1-Click deposit address when resuming a mainnet bridge that
     * was already opened: passing it makes the funding step re-poll that bridge instead of opening a
     * second one, so a crash mid-bridge can't double-send the user's ZEC.
     */
    private suspend fun FlowCollector<OfframpStatus>.driveNewOrder(
        request: OfframpRequest,
        resumeBridgeHandle: String?,
    ) {
        var orderId: BigInteger? = null
        var currentStep = OfframpStep.INITIALIZATION
        var lastTxHash: TxHash? = null
        try {
            val relay = RelayIdentities.generate()
            val currencyHex = "0x" + AbiEncoder.bytes32String(request.currency.code).value.toHex()

            currentStep = OfframpStep.SELECTING_CIRCLE
            val circles = subgraph.circlesForRouting(currencyHex)
            emit(OfframpStatus.SelectingCircle(candidateCount = circles.size))

            val selectedCircle = router.selectCircleForOrder(
                circles = circles,
                orderCurrency = currencyHex,
            ) { id -> validateCircleOnChain(id, request) }
            val circleId = selectedCircle.value
            emit(OfframpStatus.SelectingCircle(candidateCount = circles.size, selectedCircleId = circleId))

            // Funding gate, resumable + idempotent: on mainnet bridges ZEC→USDC via NEAR and persists
            // the deposit address (via the emit below) before any ZEC moves; on testnet verifies the
            // account is pre-funded. Runs only after an assignable merchant is confirmed (above) so we
            // never bridge into a market with no route.
            currentStep = OfframpStep.FUNDING
            funding.ensureFunded(accountAddress, request, resumeHandle = resumeBridgeHandle) { depositAddress ->
                emit(OfframpStatus.BridgingFunds(amount = request.usdcAmount, depositAddress = depositAddress))
            }

            // Route re-validation: the funding bridge can take minutes, long enough for the merchant the
            // eligibility gate picked to drop out. Re-confirm the circle still has an assignable merchant
            // before committing funds — otherwise placeOrder reverts and the bridged USDC strands.
            check(validateCircleOnChain(selectedCircle, request)) {
                "Selected circle $circleId lost its assignable merchant during funding — not placing the order"
            }

            currentStep = OfframpStep.APPROVING_USDC
            val approveHash = submitter.sendTransaction(
                to = network.usdcAddress,
                data = Erc20Calls.approveCalldata(network.diamondAddress, request.usdcAmount),
            )
            lastTxHash = approveHash
            emit(OfframpStatus.ApprovingUsdc(txHash = approveHash, amount = request.usdcAmount))
            require(submitter.awaitReceipt(approveHash).success) { "USDC approve reverted" }

            currentStep = OfframpStep.PLACING_ORDER
            val placeOrderHash = submitter.sendTransaction(
                to = network.diamondAddress,
                data = DiamondCalls.placeOrderCalldata(
                    PlaceOrderArgs(
                        relayPubKeyEthCrypto = relay.publicKeyHex,
                        usdcAmount = request.usdcAmount,
                        recipientAddress = accountAddress,
                        orderType = OrderType.PAY,
                        currency = request.currency,
                        circleId = circleId,
                        fiatAmountLimit = request.minFiatAmount ?: Usdc6.ZERO,
                    ),
                ),
            )
            lastTxHash = placeOrderHash
            emit(
                OfframpStatus.PlacingOrder(
                    txHash = placeOrderHash,
                    circleId = circleId,
                    amount = request.usdcAmount,
                ),
            )
            val placeReceipt = submitter.awaitReceipt(placeOrderHash)
            require(placeReceipt.success) { "placeOrder reverted" }

            orderId = OrderEvents.parseOrderIdFromReceipt(
                receipt = placeReceipt,
                diamondAddress = network.diamondAddress,
                userAddress = accountAddress,
            ) ?: error("placeOrder receipt did not contain an OrderPlaced log")

            awaitMerchantAndComplete(
                orderId = orderId,
                request = request,
                knownSetUpiHash = null,
                onStep = { currentStep = it },
                onTxHash = { lastTxHash = it },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emit(buildFailedStatus(e, orderId, currentStep, lastTxHash))
        }
    }

    /**
     * Resumes an in-flight order from a persisted checkpoint.
     *
     * - **Order already placed** ([checkpoint.orderId] non-null): pick up at merchant-acceptance /
     *   completion polling — approve + placeOrder are known to have landed.
     * - **Pre-order** (orderId null): no order was ever placed. If a mainnet funding bridge was in
     *   flight, [OfframpCheckpoint.bridgeDepositAddress] resumes it (re-polled, never re-quoted) and
     *   the order is then placed; otherwise this is just a fresh start. [driveNewOrder] is idempotent
     *   on the bridge via that handle, so this can never double-send the user's ZEC.
     */
    override fun resume(checkpoint: OfframpCheckpoint): Flow<OfframpStatus> = flow {
        emit(OfframpStatus.Idle)
        val request = checkpoint.toRequest()
        val orderId = checkpoint.orderIdBig
        if (orderId == null) {
            driveNewOrder(request, resumeBridgeHandle = checkpoint.bridgeDepositAddress)
            return@flow
        }
        var currentStep = checkpoint.currentStep
        var lastTxHash: TxHash? = checkpoint.setUpiTxHash ?: checkpoint.placeOrderTxHash
        try {
            awaitMerchantAndComplete(
                orderId = orderId,
                request = request,
                knownSetUpiHash = checkpoint.setUpiTxHash,
                onStep = { currentStep = it },
                onTxHash = { lastTxHash = it },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emit(buildFailedStatus(e, orderId, currentStep, lastTxHash))
        }
    }

    override fun bridgeFundsBackToZec(orderId: BigInteger?): Flow<OfframpStatus> = flow {
        try {
            cleanUpOrderIfNeeded(orderId)
            val balance = Usdc6(usdcBalanceOf(accountAddress))
            if (balance <= Usdc6.ZERO) {
                emit(OfframpStatus.FundsRecovered(amount = Usdc6.ZERO))
                return@flow
            }
            val target = refund.pullbackTarget(accountAddress, balance)
            if (target == null) {
                // No NEAR route (testnet): USDC is already in the self-custodial account.
                emit(OfframpStatus.FundsRecovered(amount = balance))
                return@flow
            }
            val transferHash = submitter.sendTransaction(
                to = network.usdcAddress,
                data = Erc20Calls.transferCalldata(target, balance),
            )
            require(submitter.awaitReceipt(transferHash).success) { "USDC pull-back transfer reverted" }
            emit(OfframpStatus.FundsRecovered(amount = balance, target = target, txHash = transferHash))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emit(buildFailedStatus(e, orderId, OfframpStep.WAITING_FOR_ACCEPTANCE, null))
        }
    }

    private suspend fun cleanUpOrderIfNeeded(orderId: BigInteger?) {
        if (orderId == null) return
        val status = runCatching { orderReader.fetchOrder(orderId)?.status }.getOrNull() ?: return
        when (status) {
            OrderStatus.ACCEPTED, OrderStatus.PAID -> {
                val hash = submitter.sendTransaction(
                    to = network.diamondAddress,
                    data = DiamondCalls.cancelOrderCalldata(orderId),
                )
                require(submitter.awaitReceipt(hash).success) { "cancelOrder reverted" }
            }
            OrderStatus.PLACED -> if (checkOrderExpired(orderId)) {
                val hash = submitter.sendTransaction(
                    to = network.diamondAddress,
                    data = DiamondCalls.autoCancelExpiredOrdersCalldata(listOf(orderId)),
                )
                require(submitter.awaitReceipt(hash).success) { "autoCancelExpiredOrders reverted" }
            }
            OrderStatus.COMPLETED, OrderStatus.CANCELLED -> Unit
        }
    }

    // Resume guard: subgraph can lag the chain, so when it claims "no UPI yet" we re-read on-chain
    // before re-broadcasting setSellOrderUpi — otherwise the second broadcast reverts UpiAlreadySent.
    private suspend fun isUpiAlreadyOnChain(orderId: BigInteger, accepted: OrderSnapshot): Boolean {
        if (accepted.status.onChain >= OrderStatus.PAID.onChain) return true
        if (accepted.encryptedUserUpi.isNotBlank()) return true
        if (accepted.source == OrderSnapshot.Source.OnChain) return false
        val onChain = runCatching { onChainOrderReader.fetchOrder(orderId) }.getOrNull() ?: return false
        return onChain.encryptedUserUpi.isNotBlank() ||
            onChain.status.onChain >= OrderStatus.PAID.onChain
    }

    // Encrypt UPI only to the on-chain pubkey: a compromised indexer could swap in an attacker key
    // and harvest the plaintext. Fail closed if the on-chain read disagrees with what subgraph gave us;
    // if subgraph omitted the field, trust the on-chain value (it's the source of truth anyway).
    private suspend fun verifiedMerchantPubKey(orderId: BigInteger, accepted: OrderSnapshot): String {
        if (accepted.source == OrderSnapshot.Source.OnChain) return accepted.merchantPubKey
        val onChain = onChainOrderReader.fetchOrder(orderId)
            ?: error("Cannot verify merchant pubkey on-chain for order $orderId — refusing to encrypt UPI")
        check(onChain.merchantPubKey.isNotBlank()) {
            "On-chain merchant pubkey is empty for order $orderId — refusing to encrypt UPI"
        }
        if (accepted.merchantPubKey.isNotBlank()) {
            check(onChain.merchantPubKey.equals(accepted.merchantPubKey, ignoreCase = true)) {
                "Merchant pubkey disagrees between subgraph and chain for order $orderId — refusing to encrypt UPI"
            }
        }
        return onChain.merchantPubKey
    }

    private suspend fun usdcBalanceOf(account: Address): BigInteger {
        val ret = rpc.ethCall(to = network.usdcAddress, data = Erc20Calls.balanceOfCalldata(account))
        return if (ret.isEmpty()) BigInteger.ZERO else BigInteger(1, ret)
    }

    private suspend fun FlowCollector<OfframpStatus>.awaitMerchantAndComplete(
        orderId: BigInteger,
        request: OfframpRequest,
        knownSetUpiHash: TxHash?,
        onStep: (OfframpStep) -> Unit,
        onTxHash: (TxHash) -> Unit,
    ) {
        onStep(OfframpStep.WAITING_FOR_ACCEPTANCE)
        val accepted = when (val r = pollForAcceptance(orderId)) {
            is PollOutcome.Cancelled -> {
                emitCancelled(orderId, r.snapshot)
                return
            }
            is PollOutcome.Matched -> r.snapshot
        }
        val acceptedMerchant = requireNotNull(accepted.acceptedMerchantAddress) {
            "Order $orderId reached ACCEPTED but acceptedMerchantAddress is null"
        }

        onStep(OfframpStep.ENCRYPTING_UPI)
        // Resume safety: if the encrypted UPI is already on-chain — the setSellOrderUpi tx landed
        // before its hash was checkpointed, or the order already advanced past ACCEPTED — re-sending
        // it reverts with UpiAlreadySent. Broadcast only when we have not already done so.
        val setUpiHash: TxHash? = when {
            knownSetUpiHash != null -> knownSetUpiHash
            isUpiAlreadyOnChain(orderId, accepted) -> null
            else -> {
                val cipherHex = Ecies.cipherStringify(
                    Ecies.encryptWithPublicKey(verifiedMerchantPubKey(orderId, accepted), request.recipientUpi),
                )
                onStep(OfframpStep.SENDING_UPI)
                submitter.sendTransaction(
                    to = network.diamondAddress,
                    data = DiamondCalls.setSellOrderUpiCalldata(
                        orderId = orderId,
                        encryptedUpiHex = cipherHex,
                    ),
                )
            }
        }
        if (setUpiHash != null) {
            onTxHash(setUpiHash)
            onStep(OfframpStep.SENDING_UPI)
            emit(
                OfframpStatus.SendingEncryptedUpi(
                    orderId = orderId,
                    txHash = setUpiHash,
                    merchantAddress = acceptedMerchant,
                    merchantPubKey = accepted.merchantPubKey,
                    acceptedAtEpochSeconds = accepted.acceptedAtEpochSeconds,
                ),
            )
            require(submitter.awaitReceipt(setUpiHash).success) { "setSellOrderUpi reverted" }
        }

        onStep(OfframpStep.WAITING_FOR_COMPLETION)
        val finished = when (val r = pollForCompletion(orderId, accepted)) {
            is PollOutcome.Cancelled -> {
                emitCancelled(orderId, r.snapshot, fallbackAccepted = accepted)
                return
            }
            is PollOutcome.Matched -> r.snapshot
        }
        emit(
            OfframpStatus.Completed(
                orderId = orderId,
                acceptedMerchant = finished.acceptedMerchantAddress ?: acceptedMerchant,
                actualUsdcAmount = finished.actualUsdcAmount,
                actualFiatAmount = finished.actualFiatAmount,
                placedAtEpochSeconds = finished.placedAtEpochSeconds ?: accepted.placedAtEpochSeconds,
                acceptedAtEpochSeconds = finished.acceptedAtEpochSeconds ?: accepted.acceptedAtEpochSeconds,
                paidAtEpochSeconds = finished.paidAtEpochSeconds,
                completedAtEpochSeconds = finished.completedAtEpochSeconds,
            ),
        )
    }

    private suspend fun FlowCollector<OfframpStatus>.emitCancelled(
        orderId: BigInteger,
        snapshot: OrderSnapshot,
        fallbackAccepted: OrderSnapshot? = null,
    ) {
        emit(
            OfframpStatus.Cancelled(
                orderId = orderId,
                cancelledAtEpochSeconds = snapshot.cancelledAtEpochSeconds,
                acceptedMerchant = snapshot.acceptedMerchantAddress
                    ?: fallbackAccepted?.acceptedMerchantAddress,
                // On cancellation the contract refunds the placed USDC; subgraph's actualUsdcAmount
                // is only populated on COMPLETED, so fall back to the originally-placed amount.
                refundedUsdcAmount = snapshot.actualUsdcAmount ?: snapshot.usdcAmount,
                placedAtEpochSeconds = snapshot.placedAtEpochSeconds
                    ?: fallbackAccepted?.placedAtEpochSeconds,
                acceptedAtEpochSeconds = snapshot.acceptedAtEpochSeconds
                    ?: fallbackAccepted?.acceptedAtEpochSeconds,
                paidAtEpochSeconds = snapshot.paidAtEpochSeconds,
            ),
        )
    }

    private suspend fun validateCircleOnChain(
        circleId: CircleId,
        request: OfframpRequest,
    ): Boolean = runCatching {
        val ret = rpc.ethCall(
            to = network.diamondAddress,
            data = DiamondCalls.getAssignableMerchantsFromCircleCalldata(
                circleId = circleId.value,
                assignUpTo = BigInteger.valueOf(ASSIGN_UP_TO),
                currency = request.currency,
                user = accountAddress,
                usdtAmount = request.usdcAmount,
                fiatAmount = Usdc6.ZERO,
                orderType = OrderType.PAY,
            ),
        )
        OrderReader.decodeAddressArrayNonEmpty(ret)
    }.getOrDefault(false)

    private suspend fun FlowCollector<OfframpStatus>.pollForAcceptance(orderId: BigInteger): PollOutcome =
        pollOrderUntil(
            orderId = orderId,
            buildStatus = { attempt, lastSeen, stalled, expired ->
                OfframpStatus.WaitingForMerchantAcceptance(
                    orderId = orderId,
                    pollAttempts = attempt,
                    lastObservedStatus = lastSeen,
                    stalled = stalled,
                    expired = expired,
                )
            },
            predicate = { it.isAccepted },
        )

    private suspend fun FlowCollector<OfframpStatus>.pollForCompletion(
        orderId: BigInteger,
        accepted: OrderSnapshot,
    ): PollOutcome =
        pollOrderUntil(
            orderId = orderId,
            buildStatus = { attempt, lastSeen, stalled, expired ->
                OfframpStatus.WaitingForCompletion(
                    orderId = orderId,
                    pollAttempts = attempt,
                    lastObservedStatus = lastSeen,
                    stalled = stalled,
                    expired = expired,
                    acceptedAtEpochSeconds = accepted.acceptedAtEpochSeconds,
                    paidAtEpochSeconds = null,
                )
            },
            predicate = { it.status == OrderStatus.COMPLETED },
        )

    private suspend fun checkOrderExpired(orderId: BigInteger): Boolean = runCatching {
        val ret = rpc.ethCall(to = network.diamondAddress, data = DiamondCalls.isOrderExpiredCalldata(orderId))
        ret.isNotEmpty() && BigInteger(1, ret).signum() != 0
    }.getOrDefault(false)

    /**
     * Polls [orderReader] indefinitely until [predicate] matches or the order is observed in the
     * CANCELLED state (which is a normal terminal — the contract has refunded the user's USDC
     * on-chain — not an error). There is no client-side deadline; see [stalledAfterMs] for the
     * UX-side "this is taking a while" signal.
     *
     * Transient RPC failures inside [orderReader] are silently absorbed (the fallback reader logs
     * them) and the loop continues. A single bad poll must not kill an order whose USDC is
     * already escrowed on-chain.
     */
    private suspend fun FlowCollector<OfframpStatus>.pollOrderUntil(
        orderId: BigInteger,
        buildStatus: (attempt: Int, lastSeen: OrderStatus?, stalled: Boolean, expired: Boolean) -> OfframpStatus,
        predicate: (OrderSnapshot) -> Boolean,
    ): PollOutcome {
        var attempt = 0
        val startedAtMs = clockMs()
        emit(buildStatus(attempt, null, false, false))
        while (true) {
            attempt++
            val stalled = clockMs() - startedAtMs >= stalledAfterMs
            val snapshot = try {
                orderReader.fetchOrder(orderId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // FallbackOrderReader already logs primary + fallback failures; the orchestrator
                // just keeps polling. Returning null here lets the existing snapshot==null branch
                // re-emit the WaitingFor* status without changing observed on-chain state.
                null
            }
            if (snapshot != null) {
                if (snapshot.status == OrderStatus.CANCELLED) {
                    return PollOutcome.Cancelled(snapshot)
                }
                if (predicate(snapshot)) return PollOutcome.Matched(snapshot)
                val expired = checkOrderExpired(orderId)
                emit(buildStatus(attempt, snapshot.status, stalled, expired))
            } else {
                emit(buildStatus(attempt, null, stalled, false))
            }
            delay(pollIntervalMs)
        }
    }

    private sealed class PollOutcome {
        data class Matched(val snapshot: OrderSnapshot) : PollOutcome()
        data class Cancelled(val snapshot: OrderSnapshot) : PollOutcome()
    }

    private fun buildFailedStatus(
        error: Throwable,
        orderId: BigInteger?,
        step: OfframpStep,
        lastTxHash: TxHash?,
    ): OfframpStatus.Failed = when (error) {
        is RpcException.ExecutionReverted -> OfframpStatus.Failed(
            message = error.message ?: "execution reverted",
            orderId = orderId,
            step = step,
            txHash = lastTxHash,
            revertSelector = error.selector,
            knownRevertReason = KnownReverts.explain(error),
            sdkErrorName = KnownReverts.sdkName(error),
            sdkErrorMessage = KnownReverts.sdkMessage(error),
            solidityErrorString = error.solidityErrorString,
            cause = error,
        )
        // ERC-4337 reverts surface as an opaque bundler error message ("...reverted during
        // simulation with reason: 0xea8e4eb5"), not a structured ExecutionReverted. Recover the
        // selector from the message so AA-path reverts map to the same curated/SDK reasons.
        is RpcException.Unknown -> {
            val selector = KnownReverts.selectorFromMessage(error.errorMessage ?: error.raw)
            OfframpStatus.Failed(
                message = error.errorMessage ?: error.message ?: "Unknown error",
                orderId = orderId,
                step = step,
                txHash = lastTxHash,
                revertSelector = selector,
                knownRevertReason = KnownReverts.explain(selector),
                sdkErrorName = KnownReverts.sdkName(selector),
                sdkErrorMessage = KnownReverts.sdkMessage(selector),
                cause = error,
            )
        }
        else -> OfframpStatus.Failed(
            message = error.message ?: error::class.simpleName ?: "Unknown error",
            orderId = orderId,
            step = step,
            txHash = lastTxHash,
            cause = error,
        )
    }

    companion object {
        private const val ASSIGN_UP_TO = 3L
        private const val DEFAULT_POLL_INTERVAL_MS = 3_000L
        private const val DEFAULT_STALLED_AFTER_MS = 5L * 60 * 1000
    }
}
