package co.electriccoin.zcash.ui.screen.swap.upi.progress

import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.util.StringResource

enum class UpiOfframpStepStatus { Pending, InProgress, Completed, Failed }

data class UpiOfframpStep(
    val label: StringResource,
    val detail: StringResource? = null,
    val status: UpiOfframpStepStatus,
)

internal data class UpiOfframpProgressState(
    val title: StringResource,
    val subtitle: StringResource?,
    val steps: List<UpiOfframpStep>,
    val primaryButton: ButtonState?,
    val secondaryButton: ButtonState?,
    val onBack: () -> Unit,
)
