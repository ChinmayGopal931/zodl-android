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
import xyz.justzappit.evm.util.toHex
import xyz.justzappit.offramp.config.P2pNetworkConfig
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
import java.math.BigInteger

class OfframpOrchestrator(
    private val rpc: BaseRpcClient,
    private val signer: EoaSigner,
    private val account: EvmKey,
    private val network: P2pNetworkConfig,
    private val subgraph: SubgraphClient,
    private val orderReader: OrderReadSource,
    private val router: CircleRouter = CircleRouter(),
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
    private val acceptanceTimeoutMs: Long = DEFAULT_ACCEPTANCE_TIMEOUT_MS,
    private val completionTimeoutMs: Long = DEFAULT_COMPLETION_TIMEOUT_MS,
) {
    fun run(request: OfframpRequest): Flow<OfframpStatus> = flow {
        var orderId: BigInteger? = null
        var currentStep = OfframpStep.INITIALIZATION
        var lastTxHash: String? = null
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
            ) { id -> validateCircleOnChain(id, request) }
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

            currentStep = OfframpStep.WAITING_FOR_ACCEPTANCE
            val accepted = pollForAcceptance(orderId)
            val acceptedMerchant = requireNotNull(accepted.acceptedMerchantAddress) {
                "Order $orderId reached ACCEPTED but acceptedMerchantAddress is null"
            }

            currentStep = OfframpStep.ENCRYPTING_UPI
            val cipherHex = Ecies.cipherStringify(
                Ecies.encryptWithPublicKey(accepted.merchantPubKey, request.recipientUpi),
            )

            currentStep = OfframpStep.SENDING_UPI
            val setUpiHash = signer.sendTransaction(
                to = network.diamondAddress,
                data = DiamondCalls.setSellOrderUpiCalldata(
                    orderId = orderId,
                    encryptedUpiHex = cipherHex,
                ),
            )
            lastTxHash = setUpiHash
            emit(
                OfframpStatus.SendingEncryptedUpi(
                    orderId = orderId,
                    txHash = setUpiHash,
                    merchantAddress = acceptedMerchant,
                    merchantPubKey = accepted.merchantPubKey,
                ),
            )
            require(signer.awaitReceipt(setUpiHash).success) { "setSellOrderUpi reverted" }

            currentStep = OfframpStep.WAITING_FOR_COMPLETION
            val finished = pollForCompletion(orderId)

            emit(
                OfframpStatus.Completed(
                    orderId = orderId,
                    acceptedMerchant = finished.acceptedMerchantAddress ?: acceptedMerchant,
                    actualUsdcAmount = finished.actualUsdcAmount,
                    actualFiatAmount = finished.actualFiatAmount,
                    completedAtEpochSeconds = finished.completedAtEpochSeconds,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emit(buildFailedStatus(e, orderId, currentStep, lastTxHash))
        }
    }

    private suspend fun validateCircleOnChain(
        circleId: BigInteger,
        request: OfframpRequest,
    ): Boolean = runCatching {
        val ret = rpc.ethCall(
            to = network.diamondAddress,
            data = DiamondCalls.getAssignableMerchantsFromCircleCalldata(
                circleId = circleId,
                assignUpTo = BigInteger.valueOf(ASSIGN_UP_TO),
                currency = request.currency,
                user = account.address,
                usdtAmount = request.usdcAmount,
                fiatAmount = BigInteger.ZERO,
                orderType = OrderType.PAY,
            ),
        )
        OrderReader.decodeAddressArrayNonEmpty(ret)
    }.getOrDefault(false)

    private suspend fun FlowCollector<OfframpStatus>.pollForAcceptance(orderId: BigInteger): OrderSnapshot =
        pollOrderUntil(
            orderId = orderId,
            timeoutMs = acceptanceTimeoutMs,
            timeoutMessage = "merchant did not accept order $orderId in time",
            buildStatus = { attempt, lastSeen ->
                OfframpStatus.WaitingForMerchantAcceptance(
                    orderId = orderId,
                    pollAttempts = attempt,
                    lastObservedStatus = lastSeen,
                )
            },
            predicate = { it.isAccepted },
        )

    private suspend fun FlowCollector<OfframpStatus>.pollForCompletion(orderId: BigInteger): OrderSnapshot =
        pollOrderUntil(
            orderId = orderId,
            timeoutMs = completionTimeoutMs,
            timeoutMessage = "order $orderId did not complete in time",
            buildStatus = { attempt, lastSeen ->
                OfframpStatus.WaitingForCompletion(
                    orderId = orderId,
                    pollAttempts = attempt,
                    lastObservedStatus = lastSeen,
                )
            },
            predicate = { it.status == OrderStatus.COMPLETED },
        )

    private suspend fun FlowCollector<OfframpStatus>.pollOrderUntil(
        orderId: BigInteger,
        timeoutMs: Long,
        timeoutMessage: String,
        buildStatus: (Int, OrderStatus?) -> OfframpStatus,
        predicate: (OrderSnapshot) -> Boolean,
    ): OrderSnapshot {
        var attempt = 0
        emit(buildStatus(attempt, null))
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            attempt++
            val snapshot = orderReader.fetchOrder(orderId)
            if (snapshot != null) {
                if (snapshot.status == OrderStatus.CANCELLED) {
                    error("Order $orderId was cancelled by the merchant")
                }
                if (predicate(snapshot)) return snapshot
                emit(buildStatus(attempt, snapshot.status))
            } else {
                emit(buildStatus(attempt, null))
            }
            delay(pollIntervalMs)
        }
        error(timeoutMessage)
    }

    private fun buildFailedStatus(
        error: Throwable,
        orderId: BigInteger?,
        step: OfframpStep,
        lastTxHash: String?,
    ): OfframpStatus.Failed = when (error) {
        is RpcException.ExecutionReverted -> OfframpStatus.Failed(
            message = error.message ?: "execution reverted",
            orderId = orderId,
            step = step,
            txHash = lastTxHash,
            revertSelector = error.selector,
            knownRevertReason = KnownReverts.explain(error),
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
        private const val DEFAULT_ACCEPTANCE_TIMEOUT_MS = 5L * 60 * 1000
        private const val DEFAULT_COMPLETION_TIMEOUT_MS = 30L * 60 * 1000
    }
}
