package co.electriccoin.zcash.ui.screen.swap.upi.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.component.zapp.ZappBackButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappButtonVariant
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue

@Composable
internal fun UpiOfframpProgressView(state: UpiOfframpProgressState) {
    val c = ZappTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = HORIZONTAL_PADDING.dp, vertical = VERTICAL_PADDING.dp),
        ) {
            BasicText(
                text = state.title.getValue(),
                style = ZappTheme.typography.display.copy(color = c.text),
            )
            state.subtitle?.let { sub ->
                Spacer(modifier = Modifier.height(GAP_SM.dp))
                BasicText(
                    text = sub.getValue(),
                    style = ZappTheme.typography.body.copy(color = c.textMuted),
                )
            }
            Spacer(modifier = Modifier.height(GAP_LG.dp))
            state.steps.forEachIndexed { idx, step ->
                if (idx > 0) Spacer(modifier = Modifier.height(GAP_MD.dp))
                StepRow(step)
            }
        }
        BottomBar(state)
    }
}

@Composable
private fun StepRow(step: UpiOfframpStep) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepIndicator(step.status)
        Spacer(modifier = Modifier.width(GAP_MD.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = step.label.getValue(),
                style = t.body.copy(
                    color = when (step.status) {
                        UpiOfframpStepStatus.Failed -> c.danger
                        UpiOfframpStepStatus.Pending -> c.textMuted
                        else -> c.text
                    },
                    fontWeight = when (step.status) {
                        UpiOfframpStepStatus.InProgress -> FontWeight.SemiBold
                        else -> FontWeight.Normal
                    },
                ),
            )
            step.detail?.let { detail ->
                Spacer(modifier = Modifier.height(2.dp))
                BasicText(
                    text = detail.getValue(),
                    style = t.caption.copy(color = c.textMuted),
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(status: UpiOfframpStepStatus) {
    val c = ZappTheme.colors
    val color = when (status) {
        UpiOfframpStepStatus.Pending -> c.border
        UpiOfframpStepStatus.InProgress -> c.accent
        UpiOfframpStepStatus.Completed -> c.accent
        UpiOfframpStepStatus.Failed -> c.danger
    }
    Box(
        modifier = Modifier
            .size(STEP_INDICATOR_SIZE.dp)
            .background(color),
    )
}

@Composable
private fun BottomBar(state: UpiOfframpProgressState) {
    val c = ZappTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .border(BorderStroke(1.dp, c.border), RectangleShape)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = HORIZONTAL_PADDING.dp, vertical = BOTTOM_BAR_VERTICAL_PADDING.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ZappBackButton(onClick = state.onBack)
        state.primaryButton?.let { btn ->
            ZappButton(
                text = btn.text.getValue(),
                enabled = btn.isEnabled,
                variant = ZappButtonVariant.Primary,
                onClick = btn.onClick,
            )
        }
    }
}

private const val HORIZONTAL_PADDING = 18
private const val VERTICAL_PADDING = 16
private const val BOTTOM_BAR_VERTICAL_PADDING = 12
private const val GAP_SM = 6
private const val GAP_MD = 10
private const val GAP_LG = 20
private const val STEP_INDICATOR_SIZE = 12
