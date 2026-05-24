package co.electriccoin.zcash.ui.screen.swap.upi.progress

import co.electriccoin.zcash.ui.common.provider.BridgeTerminallyFailedException
import co.electriccoin.zcash.ui.common.provider.OfframpCheckpointStorageProvider
import xyz.justzappit.evm.types.TxHash
import xyz.justzappit.offramp.orchestrator.OfframpCheckpoint
import xyz.justzappit.offramp.orchestrator.OfframpRequest
import xyz.justzappit.offramp.orchestrator.OfframpStatus
import xyz.justzappit.offramp.orchestrator.OfframpStep
import xyz.justzappit.offramp.orchestrator.orderId
import xyz.justzappit.offramp.orchestrator.step

/**
 * Owns the in-memory tx-hash cache + writes to [OfframpCheckpointStorageProvider] for one
 * in-flight offramp.
 *
 * The orchestrator emits [OfframpStatus.ApprovingUsdc] and [OfframpStatus.PlacingOrder] before
 * the order has an `orderId`; once `orderId` arrives via [OfframpStatus.WaitingForMerchantAcceptance],
 * those earlier statuses are gone — `(status as? PlacingOrder)?.txHash` is always null at the
 * persist point. This class captures each tx hash eagerly as its status fires so the next
 * checkpoint save records them, even though the persist-eligible status no longer carries them.
 *
 * Extracted from [UpiOfframpProgressVM] so it can be unit-tested without standing up a
 * ViewModel + a Dispatchers.Main + a viewModelScope dispatcher.
 */
internal class OfframpCheckpointPersister(
    private val storage: OfframpCheckpointStorageProvider,
    private val request: OfframpRequest,
) {
    private var lastApproveTxHash: TxHash? = null
    private var lastPlaceOrderTxHash: TxHash? = null
    private var lastBridgeDepositAddress: String? = null

    /**
     * Seed from a restored checkpoint so a save mid-resume doesn't drop values captured during the
     * original run (the orchestrator's resume() never re-emits ApprovingUsdc/PlacingOrder/BridgingFunds).
     */
    fun seedFrom(checkpoint: OfframpCheckpoint?) {
        lastApproveTxHash = checkpoint?.approveTxHash
        lastPlaceOrderTxHash = checkpoint?.placeOrderTxHash
        lastBridgeDepositAddress = checkpoint?.bridgeDepositAddress
    }

    suspend fun onStatus(status: OfframpStatus) {
        capture(status)
        persistOrClear(status)
    }

    private fun capture(status: OfframpStatus) {
        when (status) {
            is OfframpStatus.ApprovingUsdc -> lastApproveTxHash = status.txHash
            is OfframpStatus.PlacingOrder -> lastPlaceOrderTxHash = status.txHash
            is OfframpStatus.BridgingFunds -> status.depositAddress?.let { lastBridgeDepositAddress = it }
            else -> Unit
        }
    }

    private suspend fun persistOrClear(status: OfframpStatus) {
        when (status) {
            is OfframpStatus.Completed,
            is OfframpStatus.Cancelled,
            is OfframpStatus.FundsRecovered -> {
                storage.clear()
            }

            is OfframpStatus.Failed -> {
                // Three-way decision:
                //  1. Bridge in-flight, transient failure → keep checkpoint so user can resume.
                //  2. Bridge terminally dead (REFUNDED/FAILED/EXPIRED/INCOMPLETE_DEPOSIT) → clear,
                //     because re-polling the same 1-Click handle just yields the same terminal
                //     status indefinitely and would loop the user with no UX exit. The structural
                //     signal is the typed BridgeTerminallyFailedException carried in Failed.cause.
                //  3. Failure after funding settled (approve / placeOrder / setUpi) → clear,
                //     because the USDC is in the smart account and a fresh attempt will
                //     short-circuit via FundedFromBase.
                val bridgeTerminallyDead = status.cause is BridgeTerminallyFailedException
                val transientFundingFailure =
                    !bridgeTerminallyDead &&
                        status.step == OfframpStep.FUNDING &&
                        lastBridgeDepositAddress != null
                if (transientFundingFailure) {
                    persistCheckpoint(orderId = null, status = status)
                } else {
                    storage.clear()
                }
            }

            else -> {
                val orderId = status.orderId
                // Persist once there's either an order id OR an in-flight bridge to resume — the
                // bridge deposit address must survive process death so resume re-polls it instead of
                // opening a second bridge. Pre-bridge steps (Idle/SelectingCircle) carry nothing.
                if (orderId == null && lastBridgeDepositAddress == null) return
                persistCheckpoint(orderId = orderId?.toString(), status = status)
            }
        }
    }

    private suspend fun persistCheckpoint(orderId: String?, status: OfframpStatus) {
        val previous = storage.get()
        storage.store(
            OfframpCheckpoint(
                orderId = orderId,
                currentStep = status.step,
                bridgeDepositAddress = lastBridgeDepositAddress ?: previous?.bridgeDepositAddress,
                approveTxHash = lastApproveTxHash ?: previous?.approveTxHash,
                placeOrderTxHash = lastPlaceOrderTxHash ?: previous?.placeOrderTxHash,
                setUpiTxHash =
                    (status as? OfframpStatus.SendingEncryptedUpi)?.txHash
                        ?: previous?.setUpiTxHash,
                recipientUpi = request.recipientUpi,
                usdcAmountMicroDecimal = request.usdcAmount.micros.toString(),
                fiatAmountMicroDecimal = request.fiatAmount.micros.toString(),
                payeeName = request.payeeName,
                currency = request.currency,
                createdAtMillis = previous?.createdAtMillis ?: System.currentTimeMillis(),
            ),
        )
    }
}
