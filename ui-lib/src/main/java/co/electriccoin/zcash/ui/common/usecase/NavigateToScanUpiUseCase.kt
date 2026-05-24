package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.screen.swap.upi.scan.ScanUpiArgs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

/**
 * Round-trip handle for "open the UPI scanner, wait for the user to scan or cancel".
 * Mirrors [NavigateToScanGenericAddressUseCase] — caller `await`s a single result identified by
 * [ScanUpiArgs.requestId]; the scan screen emits onto the shared flow and the navigation router
 * pops back. Returns `null` if the user backed out of the scanner.
 */
class NavigateToScanUpiUseCase(
    private val navigationRouter: NavigationRouter,
) {
    private val pipeline = MutableSharedFlow<ScanUpiPipelineResult>()

    suspend operator fun invoke(): ScanUpiResult? {
        val args = ScanUpiArgs()
        navigationRouter.forward(args)
        val result = pipeline.first { it.args.requestId == args.requestId }
        return when (result) {
            is ScanUpiPipelineResult.Cancelled -> {
                null
            }

            is ScanUpiPipelineResult.Scanned -> {
                ScanUpiResult(
                    paymentAddress = result.paymentAddress,
                    fiatAmount = result.fiatAmount,
                )
            }
        }
    }

    suspend fun onScanCancelled(args: ScanUpiArgs) {
        pipeline.emit(ScanUpiPipelineResult.Cancelled(args))
        navigationRouter.back()
    }

    suspend fun onScanned(
        paymentAddress: String,
        fiatAmount: BigDecimal?,
        args: ScanUpiArgs,
    ) {
        pipeline.emit(ScanUpiPipelineResult.Scanned(paymentAddress, fiatAmount, args))
        navigationRouter.back()
    }
}

private sealed interface ScanUpiPipelineResult {
    val args: ScanUpiArgs

    data class Cancelled(
        override val args: ScanUpiArgs
    ) : ScanUpiPipelineResult

    data class Scanned(
        val paymentAddress: String,
        val fiatAmount: BigDecimal?,
        override val args: ScanUpiArgs,
    ) : ScanUpiPipelineResult
}

data class ScanUpiResult(
    val paymentAddress: String,
    val fiatAmount: BigDecimal?,
)
