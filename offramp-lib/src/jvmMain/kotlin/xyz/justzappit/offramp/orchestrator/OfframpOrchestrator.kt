package xyz.justzappit.offramp.orchestrator

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import xyz.justzappit.evm.abi.AbiEncoder
import xyz.justzappit.evm.crypto.Ecies
import xyz.justzappit.evm.hd.EvmKey
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.rpc.RpcException
import xyz.justzappit.evm.signer.EoaSigner
import xyz.justzappit.evm.types.TxHash
import xyz.justzappit.evm.util.toHex
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.p2p.CircleId
import xyz.justzappit.offramp.p2p.CircleRouter
import xyz.justzappit.offramp.p2p.DiamondCalls
import xyz.justzappit.offramp.p2p.Erc20Calls
import xyz.justzappit.offramp.p2p.OrderEvents
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
}

class OfframpOrchestrator(
    private val rpc: BaseRpcClient,
    private val signer: EoaSigner,
    private val account: EvmKey,
    private val network: P2pNetworkConfig,
    private val subgraph: SubgraphClient,
    private val orderReader: OrderReadSource,
    private val router: CircleRouter = CircleRouter(),
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
    /**
     * After this duration of polling with no terminal transition, the WaitingFor* status emits
     * with `stalled = true` so the UI can hint that the order is taking longer than usual. There
     * is no client-side timeout — the order remains live on-chain until merchant acceptance,
     * completion, or the contract's own auto-cancel (~72h per Diamond.getOrderExpiryTime).
     * Killing flows on a client clock would orphan the user's escrowed USDC.
     */
    private val stalledAfterMs: Long = DEFAULT_STALLED_AFTER_MS,
    /**
     * Clock used to compute the stalled-flag deadline. Defaults to wall-clock; tests inject a
     * controllable monotonic counter because `runTest`'s virtual time does not advance
     * `clockMs()`.
     */
    private val clockMs: () -> Long = System::currentTimeMillis,
) : OfframpDriver {
    override fun run(request: OfframpRequest): Flow<OfframpStatus> = flow {
        var orderId: BigInteger? = null
        var currentStep = OfframpStep.INITIALIZATION
        var lastTxHash: TxHash? = null
        emit(OfframpStatus.Idle)

        try {
            val relay = RelayIdentities.generate()
            val currencyHex = "0x" + AbiEncoder.bytes32String(request.currency.code).value.toHex()

            currentStep = OfframpStep.SELECTING_CIRCLE
            val circles = subgraph.circlesForRouting(currencyHex)
            emit(OfframpStatus.SelectingCircle(candidateCount = circles.size))

            val circleId = router.selectCircleForOrder(
                circles = circles,
                orderCurrency = currencyHex,
            ) { id -> validateCircleOnChain(id, request) }.value
            emit(OfframpStatus.SelectingCircle(candidateCount = circles.size, selectedCircleId = circleId))

            currentStep = OfframpStep.APPROVING_USDC
            val approveHash = signer.sendTransaction(
                to = network.usdcAddress,
                data = Erc20Calls.approveCalldata(network.diamondAddress, request.usdcAmount),
            )
            lastTxHash = approveHash
            emit(OfframpStatus.ApprovingUsdc(txHash = approveHash, amount = request.usdcAmount))
            require(signer.awaitReceipt(approveHash).success) { "USDC approve reverted" }

            currentStep = OfframpStep.PLACING_ORDER
            val placeOrderHash = signer.sendTransaction(
                to = network.diamondAddress,
                data = DiamondCalls.placeOrderCalldata(
                    PlaceOrderArgs(
                        relayPubKeyEthCrypto = relay.publicKeyHex,
                        usdcAmount = request.usdcAmount,
                        recipientAddress = account.address,
                        orderType = OrderType.PAY,
                        currency = request.currency,
                        circleId = circleId,
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
            val placeReceipt = signer.awaitReceipt(placeOrderHash)
            require(placeReceipt.success) { "placeOrder reverted" }

            orderId = OrderEvents.parseOrderIdFromReceipt(
                receipt = placeReceipt,
                diamondAddress = network.diamondAddress,
                userAddress = account.address,
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
     * Resumes an in-flight order from a persisted checkpoint. Assumes the orderId is known (which
     * implies approve + placeOrder both already landed). Earlier-step resumes are out of scope for
     * v1; if [checkpoint.orderId] is null the caller must restart fresh via [run].
     */
    override fun resume(checkpoint: OfframpCheckpoint): Flow<OfframpStatus> = flow {
        val orderId = requireNotNull(checkpoint.orderIdBig) {
            "OfframpOrchestrator.resume requires a checkpoint with orderId — got currentStep=${checkpoint.currentStep}"
        }
        val request = checkpoint.toRequest()
        var currentStep = checkpoint.currentStep
        var lastTxHash: TxHash? = checkpoint.setUpiTxHash ?: checkpoint.placeOrderTxHash
        emit(OfframpStatus.Idle)
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
        val upiAlreadyOnChain = accepted.encryptedUserUpi.isNotBlank() ||
            accepted.status.onChain >= OrderStatus.PAID.onChain
        val setUpiHash: TxHash? = when {
            knownSetUpiHash != null -> knownSetUpiHash
            upiAlreadyOnChain -> null
            else -> {
                val cipherHex = Ecies.cipherStringify(
                    Ecies.encryptWithPublicKey(accepted.merchantPubKey, request.recipientUpi),
                )
                onStep(OfframpStep.SENDING_UPI)
                signer.sendTransaction(
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
            require(signer.awaitReceipt(setUpiHash).success) { "setSellOrderUpi reverted" }
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
                user = account.address,
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
            buildStatus = { attempt, lastSeen, stalled ->
                OfframpStatus.WaitingForMerchantAcceptance(
                    orderId = orderId,
                    pollAttempts = attempt,
                    lastObservedStatus = lastSeen,
                    stalled = stalled,
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
            buildStatus = { attempt, lastSeen, stalled ->
                OfframpStatus.WaitingForCompletion(
                    orderId = orderId,
                    pollAttempts = attempt,
                    lastObservedStatus = lastSeen,
                    stalled = stalled,
                    acceptedAtEpochSeconds = accepted.acceptedAtEpochSeconds,
                    paidAtEpochSeconds = null,
                )
            },
            predicate = { it.status == OrderStatus.COMPLETED },
        )

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
        buildStatus: (Int, OrderStatus?, Boolean) -> OfframpStatus,
        predicate: (OrderSnapshot) -> Boolean,
    ): PollOutcome {
        var attempt = 0
        val startedAtMs = clockMs()
        emit(buildStatus(attempt, null, false))
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
                emit(buildStatus(attempt, snapshot.status, stalled))
            } else {
                emit(buildStatus(attempt, null, stalled))
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
            solidityErrorString = error.solidityErrorString,
            cause = error,
        )
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
