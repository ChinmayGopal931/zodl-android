package xyz.justzappit.offramp.orchestrator

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import xyz.justzappit.evm.abi.AbiDecoder
import xyz.justzappit.evm.abi.AbiEncoder
import xyz.justzappit.evm.abi.keccak256
import xyz.justzappit.evm.crypto.Ecies
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.rpc.RpcException
import xyz.justzappit.evm.signer.EcdsaSigner
import xyz.justzappit.evm.signer.TxSubmitter
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.TxHash
import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.evm.util.padLeftToWord
import xyz.justzappit.evm.util.toHex
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.funding.FundingOutcome
import xyz.justzappit.offramp.funding.OfframpFunding
import xyz.justzappit.offramp.funding.OfframpRefund
import xyz.justzappit.offramp.p2p.CircleId
import xyz.justzappit.offramp.p2p.CircleRouter
import xyz.justzappit.offramp.p2p.CurrencyCode
import xyz.justzappit.offramp.p2p.DiamondCalls
import xyz.justzappit.offramp.p2p.Erc20Calls
import xyz.justzappit.offramp.p2p.InMemoryOrderRecipientUpiCache
import xyz.justzappit.offramp.p2p.InMemoryRelayIdentityStore
import xyz.justzappit.offramp.p2p.OnChainOrderReader
import xyz.justzappit.offramp.p2p.OrderEvents
import xyz.justzappit.offramp.p2p.OrderReadSource
import xyz.justzappit.offramp.p2p.OrderReader
import xyz.justzappit.offramp.p2p.OrderRecipientUpiCache
import xyz.justzappit.offramp.p2p.OrderSnapshot
import xyz.justzappit.offramp.p2p.OrderStatus
import xyz.justzappit.offramp.p2p.OrderType
import xyz.justzappit.offramp.p2p.PlaceOrderArgs
import xyz.justzappit.offramp.p2p.PriceConfigDecoder
import xyz.justzappit.offramp.p2p.RelayIdentityStore
import xyz.justzappit.offramp.p2p.SubgraphClient
import xyz.justzappit.offramp.p2p.UpiPayUri
import xyz.justzappit.offramp.p2p.Usdc6
import xyz.justzappit.offramp.p2p.getOrCreate
import xyz.justzappit.offramp.p2p.getUsdcBalance
import java.math.BigInteger

interface OfframpDriver {
    fun run(request: OfframpRequest): Flow<OfframpStatus>

    fun resume(checkpoint: OfframpCheckpoint): Flow<OfframpStatus>

    /**
     * "Get my USDC back to ZEC". Cleanup-call selection depends on on-chain order state:
     *  - ACCEPTED / PAID    → `cancelOrder` (user-permitted, refunds escrow) + transfer
     *  - PLACED + expired   → `autoCancelExpiredOrders` (permissionless cleanup) + transfer
     *  - PLACED + active    → transfer only (PAY/SELL escrow nothing at PLACED)
     *  - CANCELLED / null   → transfer only
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
    // Wall-clock by default; tests inject a monotonic counter — `runTest` virtual time doesn't
    // advance `System.currentTimeMillis()`.
    private val clockMs: () -> Long = System::currentTimeMillis,
    // Authoritative on-chain reader for the merchant pubkey verification — the polling [orderReader]
    // is subgraph-primary and untrusted for the field we encrypt the user's UPI to.
    private val onChainOrderReader: OrderReadSource = OnChainOrderReader(rpc, network),
    // In-memory default for tests; Android injects an encrypted-prefs store.
    private val relayIdentityStore: RelayIdentityStore = InMemoryRelayIdentityStore(),
    // Locally caches each placed order's recipient UPI so the P2P transactions screen can show
    // it later — encUpi on-chain is encrypted to the merchant, so the user cannot recover the
    // VPA from the chain alone. In-memory default for tests; Android injects encrypted prefs.
    private val orderRecipientUpiCache: OrderRecipientUpiCache = InMemoryOrderRecipientUpiCache(),
) : OfframpDriver {
    override fun run(request: OfframpRequest): Flow<OfframpStatus> =
        flow {
            emit(OfframpStatus.Idle)
            driveNewOrder(request, resumeBridgeHandle = null)
        }

    // [resumeBridgeHandle] is a persisted 1-Click deposit address — passing it forces the funding
    // step to re-poll the existing bridge instead of opening a second one, so a crash mid-bridge
    // can't double-send the user's ZEC.
    private suspend fun FlowCollector<OfframpStatus>.driveNewOrder(
        request: OfframpRequest,
        resumeBridgeHandle: String?,
    ) {
        var orderId: BigInteger? = null
        var currentStep = OfframpStep.INITIALIZATION
        var lastTxHash: TxHash? = null
        try {
            val relay = relayIdentityStore.getOrCreate()
            val currencyHex = "0x" + AbiEncoder.bytes32String(request.currency.code).value.toHex()

            currentStep = OfframpStep.SELECTING_CIRCLE
            val circles = subgraph.circlesForRouting(currencyHex)
            emit(OfframpStatus.SelectingCircle(candidateCount = circles.size))

            val selectedCircle =
                router.selectCircleForOrder(
                    circles = circles,
                    orderCurrency = currencyHex,
                ) { id -> validateCircleOnChain(id, request) }
            val circleId = selectedCircle.value
            emit(OfframpStatus.SelectingCircle(candidateCount = circles.size, selectedCircleId = circleId))

            // AlreadyFunded short-circuits the bridge — common when a previous cancelled order left
            // USDC refunded into the smart account; emit a distinct status so the UI renders
            // "Using Base balance" instead of "Bridging funds".
            currentStep = OfframpStep.FUNDING
            val outcome =
                funding.ensureFunded(accountAddress, request, resumeHandle = resumeBridgeHandle) { depositAddress ->
                    emit(OfframpStatus.BridgingFunds(amount = request.usdcAmount, depositAddress = depositAddress))
                }
            if (outcome is FundingOutcome.AlreadyFunded) {
                emit(OfframpStatus.FundedFromBase(amount = request.usdcAmount, baseBalance = outcome.currentBalance))
            }

            // Route re-validation: the funding bridge can take minutes, long enough for the merchant the
            // eligibility gate picked to drop out. Re-confirm the circle still has an assignable merchant
            // before committing funds — otherwise placeOrder reverts and the bridged USDC strands.
            check(validateCircleOnChain(selectedCircle, request)) {
                "Selected circle $circleId lost its assignable merchant during funding — not placing the order"
            }

            currentStep = OfframpStep.APPROVING_USDC
            // Cover placed + smallOrderFixedFeePay: the Diamond pulls the fee as a second
            // transferFrom inside setSellOrderUpi and silent-cancels if allowance is short. See
            // [readSmallOrderFixedFeePay] for the empirical mainnet trace.
            val smallOrderFee =
                runCatching { readSmallOrderFixedFeePay(request.currency) }
                    .getOrDefault(Usdc6.ZERO)
            val approveAmount = Usdc6(request.usdcAmount.micros + smallOrderFee.micros)
            val approveHash =
                submitter.sendTransaction(
                    to = network.usdcAddress,
                    data = Erc20Calls.approveCalldata(network.diamondAddress, approveAmount),
                )
            lastTxHash = approveHash
            emit(OfframpStatus.ApprovingUsdc(txHash = approveHash, amount = approveAmount))
            require(submitter.awaitReceipt(approveHash).success) { "USDC approve reverted" }

            currentStep = OfframpStep.PLACING_ORDER
            val placeOrderHash =
                submitter.sendTransaction(
                    to = network.diamondAddress,
                    data =
                        DiamondCalls.placeOrderCalldata(
                            PlaceOrderArgs(
                                relayPubKeyEthCrypto = relay.publicKeyHex,
                                usdcAmount = request.usdcAmount,
                                recipientAddress = accountAddress,
                                orderType = OrderType.PAY,
                                currency = request.currency,
                                circleId = circleId,
                                fiatAmountLimit = request.fiatAmountLimit ?: Usdc6.ZERO,
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

            // Cache the user-typed VPA against the on-chain orderId BEFORE awaiting completion:
            // `encUpi` is encrypted to the merchant's key, so the only way to recover "you paid to X"
            // for the history list is to remember it locally at placement.
            orderRecipientUpiCache.put(orderId.toString(), request.recipientUpi)

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

    // Two branches:
    //  - orderId non-null → resume at merchant-acceptance / completion polling.
    //  - orderId null → fresh start; if a mainnet bridge was already opened,
    //    [bridgeDepositAddress] makes [driveNewOrder] re-poll it instead of re-quoting.
    override fun resume(checkpoint: OfframpCheckpoint): Flow<OfframpStatus> =
        flow {
            emit(OfframpStatus.Idle)
            val fallbackFiat = checkpoint.fiatAmount ?: resolveFallbackFiat(checkpoint)
            val request = checkpoint.toRequest(fallbackFiatAmount = fallbackFiat)
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

    override fun bridgeFundsBackToZec(orderId: BigInteger?): Flow<OfframpStatus> =
        flow {
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
                val transferHash =
                    submitter.sendTransaction(
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
                val hash =
                    submitter.sendTransaction(
                        to = network.diamondAddress,
                        data = DiamondCalls.cancelOrderCalldata(orderId),
                    )
                require(submitter.awaitReceipt(hash).success) { "cancelOrder reverted" }
            }

            OrderStatus.PLACED -> {
                if (checkOrderExpired(orderId)) {
                    val hash =
                        submitter.sendTransaction(
                            to = network.diamondAddress,
                            data = DiamondCalls.autoCancelExpiredOrdersCalldata(listOf(orderId)),
                        )
                    require(submitter.awaitReceipt(hash).success) { "autoCancelExpiredOrders reverted" }
                }
            }

            OrderStatus.COMPLETED, OrderStatus.CANCELLED -> {
                Unit
            }
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
        val onChain =
            onChainOrderReader.fetchOrder(orderId)
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

    private suspend fun usdcBalanceOf(account: Address): BigInteger =
        rpc.getUsdcBalance(network.usdcAddress, account).micros

    /**
     * Encrypts the full `upi://pay?…` URI (NOT a bare VPA — bare VPAs trigger the Diamond's
     * same-tx auto-cancel) and broadcasts setSellOrderUpi with `updatedAmount = max(parsed.usdc,
     * placed)`. Dropping below the placed amount strips the merchant's accepted margin and they
     * auto-cancel +4s later. See §6 of the offramp findings doc.
     */
    private suspend fun broadcastSetSellOrderUpi(
        orderId: BigInteger,
        accepted: OrderSnapshot,
        request: OfframpRequest,
        onStep: (OfframpStep) -> Unit,
    ): TxHash {
        val merchantPubKey = verifiedMerchantPubKey(orderId, accepted)
        // Snap to UpiPayUri's am= precision (2dp). Defensive even though the UI already snaps —
        // resume paths could carry a 3dp checkpoint, and we want the URI's `am=` to be exactly
        // what we'll feed into `parsedUsdcMicros` so both sides see identical input.
        val inrAmount = request.fiatAmount.whole.setScale(UpiPayUri.INR_DECIMAL_PLACES, java.math.RoundingMode.FLOOR)
        val qrUri =
            UpiPayUri.build(
                vpa = request.recipientUpi,
                payeeName = request.payeeName,
                inrAmount = inrAmount,
                currencyCode = request.currency.code,
            )

        val sellPrice = runCatching { readSellPriceInrPerUsdc(request.currency) }.getOrNull()
        val parsedUsdcMicros =
            if (sellPrice != null && sellPrice.signum() > 0) {
                UpiPayUri.parsedUsdcMicros(inrAmount, sellPrice).toBigInteger()
            } else {
                request.usdcAmount.micros
            }
        val placedMicros = request.usdcAmount.micros
        val updatedAmount = parsedUsdcMicros.max(placedMicros)

        if (updatedAmount > placedMicros) {
            // Diamond pulls (updatedAmount - placed) AND the small-order fixed fee at setUpi.
            // Top up to cover both — initial approve was `placed + fee`, of which `placed` is
            // already gone, leaving `fee`. Re-approving to `updatedAmount + fee` overwrites that.
            val topUpFee =
                runCatching { readSmallOrderFixedFeePay(request.currency) }
                    .getOrDefault(Usdc6.ZERO)
            val topUpAmount = Usdc6(updatedAmount + topUpFee.micros)
            val topUpHash =
                submitter.sendTransaction(
                    to = network.usdcAddress,
                    data = Erc20Calls.approveCalldata(network.diamondAddress, topUpAmount),
                )
            require(submitter.awaitReceipt(topUpHash).success) {
                "USDC allowance top-up reverted (updatedAmount=$updatedAmount > placed=$placedMicros)"
            }
        }

        val cipherHex = encryptUpiEnvelopeForMerchant(qrUri, merchantPubKey)
        onStep(OfframpStep.SENDING_UPI)
        return submitter.sendTransaction(
            to = network.diamondAddress,
            data =
                DiamondCalls.setSellOrderUpiCalldata(
                    orderId = orderId,
                    encryptedUpiHex = cipherHex,
                    updatedAmount = updatedAmount,
                ),
        )
    }

    /**
     * Wrap the UPI URI in the SDK's signed `{message, signature}` JSON envelope before ECIES.
     * Mirrors `@p2pdotme/sdk` `crypto/encryption.ts:encryptPaymentAddress`. Without the envelope
     * the merchant's parser sees a raw URI, throws on `JSON.parse`, and the strict merchant pool
     * (e.g. `0x70e45df…`) atomic-cancels inside our own setSellOrderUpi. Verified mainnet 2026-05-24:
     * 290-char raw-URI encUpi from this orchestrator vs 610-char SDK-wrapped encUpi from the
     * Node test rig; strict merchant accepts only the wrapped form.
     *
     * Signature is ECDSA over `keccak256(utf8(uri))` with the relay identity's private key, encoded
     * as viem's `serializeSignature`: `r(32) | s(32) | v(1)` where v ∈ {0x1b, 0x1c}.
     */
    private suspend fun encryptUpiEnvelopeForMerchant(qrUri: String, merchantPubKey: String): String {
        val relay = relayIdentityStore.getOrCreate()
        val privateKey = java.math.BigInteger(1, relay.privateKeyHex.removePrefix("0x").hexToBytes())
        val messageHash = keccak256(qrUri.toByteArray(Charsets.UTF_8))
        val sig = EcdsaSigner.sign(messageHash, privateKey)
        val sigBytes =
            sig.r.toByteArray().padLeftToWord() +
                sig.s.toByteArray().padLeftToWord() +
                byteArrayOf((sig.yParity + SIG_V_OFFSET).toByte())
        val sigHex = "0x" + sigBytes.toHex()
        val payload =
            Json.encodeToString(
                kotlinx.serialization.json.JsonObject
                    .serializer(),
                buildJsonObject {
                    put("message", qrUri)
                    put("signature", sigHex)
                },
            )
        return Ecies.cipherStringify(Ecies.encryptWithPublicKey(merchantPubKey, payload))
    }

    private suspend fun readSellPriceInrPerUsdc(currency: CurrencyCode): java.math.BigDecimal {
        val ret =
            rpc.ethCall(
                to = network.diamondAddress,
                data = DiamondCalls.getPriceConfigCalldata(currency),
            )
        return PriceConfigDecoder.decode(ret).sellPriceAsRate()
    }

    // The Diamond pulls `smallOrderFixedFeePay` as a separate transferFrom inside setSellOrderUpi
    // (on top of `placed`). If allowance is short of `placed + fee`, the contract atomic-emits
    // `CancelledOrders` from inside the user's own setUpi call — visually indistinguishable from
    // a merchant decline but actually a silent allowance underflow. Verified mainnet 2026-05-24:
    // 0.99 USDC orders cancelled atomically with allowance == placed; same orders completed once
    // we approved `placed + fee`. user-app-client sidesteps this by approving `MAX_UINT256` once.
    private suspend fun readSmallOrderFixedFeePay(currency: CurrencyCode): Usdc6 {
        val ret =
            rpc.ethCall(
                to = network.diamondAddress,
                data = DiamondCalls.getSmallOrderFixedFeePayCalldata(currency),
            )
        return Usdc6(AbiDecoder(ret).also { it.requireWords(1) }.uint(0))
    }

    private suspend fun resolveFallbackFiat(checkpoint: OfframpCheckpoint): Usdc6 {
        val rate = runCatching { readSellPriceInrPerUsdc(checkpoint.currency) }.getOrNull()
        return if (rate != null && rate.signum() > 0) {
            Usdc6.ofWhole(checkpoint.usdcAmount.whole.multiply(rate))
        } else {
            checkpoint.usdcAmount
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
        val accepted =
            when (val r = pollForAcceptance(orderId)) {
                is PollOutcome.Cancelled -> {
                    emitCancelled(orderId, r.snapshot)
                    return
                }

                is PollOutcome.Matched -> {
                    r.snapshot
                }
            }
        val acceptedMerchant =
            requireNotNull(accepted.acceptedMerchantAddress) {
                "Order $orderId reached ACCEPTED but acceptedMerchantAddress is null"
            }

        onStep(OfframpStep.ENCRYPTING_UPI)
        // Resume safety: if the encrypted UPI is already on-chain — the setSellOrderUpi tx landed
        // before its hash was checkpointed, or the order already advanced past ACCEPTED — re-sending
        // it reverts with UpiAlreadySent. Broadcast only when we have not already done so.
        val setUpiHash: TxHash? =
            when {
                knownSetUpiHash != null -> knownSetUpiHash
                isUpiAlreadyOnChain(orderId, accepted) -> null
                else -> broadcastSetSellOrderUpi(orderId, accepted, request, onStep)
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
        val finished =
            when (val r = pollForCompletion(orderId, accepted)) {
                is PollOutcome.Cancelled -> {
                    emitCancelled(orderId, r.snapshot, fallbackAccepted = accepted)
                    return
                }

                is PollOutcome.Matched -> {
                    r.snapshot
                }
            }
        emit(
            OfframpStatus.Completed(
                orderId = orderId,
                acceptedMerchant = finished.acceptedMerchantAddress ?: acceptedMerchant,
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
                // On cancellation the contract refunds the placed USDC; subgraph's actualUsdcAmount
                // is only populated on COMPLETED, so fall back to the originally-placed amount.
                refundedUsdcAmount = snapshot.actualUsdcAmount ?: snapshot.usdcAmount,
                acceptedMerchant =
                    snapshot.acceptedMerchantAddress
                        ?: fallbackAccepted?.acceptedMerchantAddress,
            ),
        )
    }

    private suspend fun validateCircleOnChain(
        circleId: CircleId,
        request: OfframpRequest,
    ): Boolean =
        runCatching {
            val ret =
                rpc.ethCall(
                    to = network.diamondAddress,
                    data =
                        DiamondCalls.getAssignableMerchantsFromCircleCalldata(
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

    private suspend fun checkOrderExpired(orderId: BigInteger): Boolean =
        runCatching {
            val ret = rpc.ethCall(to = network.diamondAddress, data = DiamondCalls.isOrderExpiredCalldata(orderId))
            ret.isNotEmpty() && BigInteger(1, ret).signum() != 0
        }.getOrDefault(false)

    // No client-side deadline (see [stalledAfterMs] for the UX-only "taking a while" signal).
    // CANCELLED is a normal terminal — the contract has refunded the user's USDC on-chain — and
    // returned as a [PollOutcome.Cancelled], not thrown. Transient RPC failures are swallowed so
    // a single bad poll can't kill an order whose USDC is already escrowed.
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
            val snapshot =
                try {
                    orderReader.fetchOrder(orderId)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    // FallbackOrderReader already logs both legs; orchestrator just keeps polling.
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
        data class Matched(
            val snapshot: OrderSnapshot
        ) : PollOutcome()

        data class Cancelled(
            val snapshot: OrderSnapshot
        ) : PollOutcome()
    }

    private fun buildFailedStatus(
        error: Throwable,
        orderId: BigInteger?,
        step: OfframpStep,
        lastTxHash: TxHash?,
    ): OfframpStatus.Failed =
        when (error) {
            is RpcException.ExecutionReverted -> {
                val lookup = KnownReverts.lookup(error.selector)
                OfframpStatus.Failed(
                    message = error.message ?: "execution reverted",
                    orderId = orderId,
                    step = step,
                    txHash = lastTxHash,
                    revertSelector = error.selector,
                    knownRevertReason = lookup.reason,
                    sdkErrorName = lookup.sdkName,
                    sdkErrorMessage = lookup.sdkMessage,
                    solidityErrorString = error.solidityErrorString,
                    cause = error,
                )
            }

            // ERC-4337 reverts surface as an opaque bundler error message ("...reverted during
            // simulation with reason: 0xea8e4eb5"), not a structured ExecutionReverted. Recover the
            // selector from the message so AA-path reverts map to the same curated/SDK reasons.
            is RpcException.Unknown -> {
                val selector = KnownReverts.selectorFromMessage(error.errorMessage ?: error.raw)
                val lookup = KnownReverts.lookup(selector)
                OfframpStatus.Failed(
                    message = error.errorMessage ?: error.message ?: "Unknown error",
                    orderId = orderId,
                    step = step,
                    txHash = lastTxHash,
                    revertSelector = selector,
                    knownRevertReason = lookup.reason,
                    sdkErrorName = lookup.sdkName,
                    sdkErrorMessage = lookup.sdkMessage,
                    cause = error,
                )
            }

            else -> {
                OfframpStatus.Failed(
                    message = error.message ?: error::class.simpleName ?: "Unknown error",
                    orderId = orderId,
                    step = step,
                    txHash = lastTxHash,
                    cause = error,
                )
            }
        }

    companion object {
        private const val ASSIGN_UP_TO = 3L
        private const val DEFAULT_POLL_INTERVAL_MS = 3_000L
        private const val DEFAULT_STALLED_AFTER_MS = 5L * 60 * 1000

        // viem `serializeSignature` v offset — adds 27 to recId so v ∈ {0x1b, 0x1c}.
        private const val SIG_V_OFFSET = 27
    }
}
