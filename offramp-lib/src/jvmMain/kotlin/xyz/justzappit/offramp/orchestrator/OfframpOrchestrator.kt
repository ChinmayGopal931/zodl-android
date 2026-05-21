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
import xyz.justzappit.evm.signer.EoaSigner
import xyz.justzappit.evm.util.toHex
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.p2p.CircleRouter
import xyz.justzappit.offramp.p2p.DiamondCalls
import xyz.justzappit.offramp.p2p.Erc20Calls
import xyz.justzappit.offramp.p2p.OrderEvents
import xyz.justzappit.offramp.p2p.OrderReader
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
    private val router: CircleRouter = CircleRouter(),
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
    private val acceptanceTimeoutMs: Long = DEFAULT_ACCEPTANCE_TIMEOUT_MS,
    private val completionTimeoutMs: Long = DEFAULT_COMPLETION_TIMEOUT_MS,
) {
    fun run(request: OfframpRequest): Flow<OfframpStatus> = flow {
        var orderId: BigInteger? = null
        var currentStep = FailedStep.INITIALIZATION
        var lastTxHash: String? = null
        emit(OfframpStatus.Idle)

        try {
            val relay = RelayIdentities.generate()
            val currencyHex = "0x" + AbiEncoder.bytes32String(request.currency).value.toHex()

            currentStep = FailedStep.SELECTING_CIRCLE
            val circles = subgraph.circlesForRouting(currencyHex)
            emit(OfframpStatus.SelectingCircle(candidateCount = circles.size))

            val circleId = router.selectCircleForOrder(
                circles = circles,
                orderCurrency = currencyHex,
            ) { id -> validateCircleOnChain(id, request) }
            emit(OfframpStatus.SelectingCircle(candidateCount = circles.size, selectedCircleId = circleId))

            currentStep = FailedStep.APPROVING_USDC
            val approveHash = signer.sendTransaction(
                to = network.usdcAddress,
                data = Erc20Calls.approveCalldata(network.diamondAddress, request.usdcAmount),
            )
            lastTxHash = approveHash
            emit(OfframpStatus.ApprovingUsdc(txHash = approveHash, amount = request.usdcAmount))
            require(signer.awaitReceipt(approveHash).success) { "USDC approve reverted" }

            currentStep = FailedStep.PLACING_ORDER
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

            currentStep = FailedStep.WAITING_FOR_ACCEPTANCE
            val accepted = pollForAcceptance(orderId)

            currentStep = FailedStep.ENCRYPTING_UPI
            val cipherHex = Ecies.cipherStringify(
                Ecies.encryptWithPublicKey(accepted.merchantPubKey, request.recipientUpi),
            )

            currentStep = FailedStep.SENDING_UPI
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
                    merchantAddress = accepted.acceptedMerchant,
                    merchantPubKey = accepted.merchantPubKey,
                ),
            )
            require(signer.awaitReceipt(setUpiHash).success) { "setSellOrderUpi reverted" }

            currentStep = FailedStep.WAITING_FOR_COMPLETION
            val finished = pollForCompletion(orderId)

            emit(
                OfframpStatus.Completed(
                    orderId = orderId,
                    acceptedMerchant = finished.acceptedMerchant,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val raw = e.message
            val selector = KnownReverts.extractSelector(raw)
            val decoded = KnownReverts.explain(selector) ?: KnownReverts.decodeErrorString(raw)
            emit(
                OfframpStatus.Failed(
                    message = raw ?: e::class.simpleName ?: "Unknown error",
                    orderId = orderId,
                    step = currentStep,
                    txHash = lastTxHash,
                    revertSelector = selector,
                    decodedReason = decoded,
                    cause = e,
                ),
            )
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

    private suspend fun FlowCollector<OfframpStatus>.pollForAcceptance(orderId: BigInteger): OrderReader.Order =
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
            predicate = {
                it.status.onChain >= OrderStatus.ACCEPTED.onChain && it.merchantPubKey.isNotEmpty()
            },
        )

    private suspend fun FlowCollector<OfframpStatus>.pollForCompletion(orderId: BigInteger): OrderReader.Order =
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
        predicate: (OrderReader.Order) -> Boolean,
    ): OrderReader.Order {
        var attempt = 0
        emit(buildStatus(attempt, null))
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            attempt++
            val ret = rpc.ethCall(network.diamondAddress, DiamondCalls.getOrdersByIdCalldata(orderId))
            val order = OrderReader.decodeOrder(ret)
            if (order.status == OrderStatus.CANCELLED) error("Order $orderId was cancelled by the merchant")
            if (predicate(order)) return order
            emit(buildStatus(attempt, order.status))
            delay(pollIntervalMs)
        }
        error(timeoutMessage)
    }

    companion object {
        private const val ASSIGN_UP_TO = 3L
        private const val DEFAULT_POLL_INTERVAL_MS = 3_000L
        private const val DEFAULT_ACCEPTANCE_TIMEOUT_MS = 5L * 60 * 1000
        private const val DEFAULT_COMPLETION_TIMEOUT_MS = 30L * 60 * 1000
    }
}
