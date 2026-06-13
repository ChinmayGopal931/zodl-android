package co.electriccoin.zcash.ui.screen.swap.upi.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.ellipsizeMiddle
import co.electriccoin.zcash.ui.design.util.getValue

/** Vertical list of [UpiOfframpStep] rows. Shared by the order-progress and top-up-bridge screens. */
@Composable
internal fun OfframpStepList(
    steps: List<UpiOfframpStep>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        steps.forEachIndexed { idx, step ->
            if (idx > 0) Spacer(modifier = Modifier.height(STEP_GAP.dp))
            OfframpStepRow(step)
        }
    }
}

@Composable
internal fun OfframpStepRow(step: UpiOfframpStep) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        StepIndicator(step.status)
        Spacer(modifier = Modifier.width(STEP_GAP.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = step.label.getValue(),
                style =
                    t.body.copy(
                        color =
                            when (step.status) {
                                UpiOfframpStepStatus.Failed -> c.danger
                                UpiOfframpStepStatus.Pending -> c.textMuted
                                else -> c.text
                            },
                        fontWeight =
                            when (step.status) {
                                UpiOfframpStepStatus.InProgress -> FontWeight.SemiBold
                                else -> FontWeight.Normal
                            },
                    ),
            )
            step.detailLines.forEach { detail ->
                Spacer(modifier = Modifier.height(DETAIL_GAP.dp))
                BasicText(
                    text = detail.getValue(),
                    style = t.caption.copy(color = c.textMuted),
                )
            }
            if (step.txHash != null && step.txExplorerUrl != null) {
                Spacer(modifier = Modifier.height(LINK_GAP.dp))
                OfframpExplorerLink(
                    value = step.txHash,
                    url = step.txExplorerUrl,
                    prefix = TX_HASH_ELLIPSIS_PREFIX,
                    suffix = TX_HASH_ELLIPSIS_SUFFIX,
                    uriHandler = uriHandler,
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(status: UpiOfframpStepStatus) {
    val c = ZappTheme.colors
    val color =
        when (status) {
            UpiOfframpStepStatus.Pending -> c.border
            UpiOfframpStepStatus.InProgress -> c.accent
            UpiOfframpStepStatus.Completed -> c.accent
            UpiOfframpStepStatus.Failed -> c.danger
        }
    Box(
        modifier =
            Modifier
                .padding(top = STEP_INDICATOR_TOP_OFFSET.dp)
                .size(STEP_INDICATOR_SIZE.dp)
                .background(color),
    )
}

/** Ellipsized, tappable monospace link to a block explorer (address or tx hash). */
@Composable
internal fun OfframpExplorerLink(
    value: String,
    url: String,
    prefix: Int,
    suffix: Int,
    uriHandler: UriHandler,
) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    BasicText(
        text = value.ellipsizeMiddle(prefix, suffix),
        style = t.mono.copy(color = c.accent, textDecoration = TextDecoration.Underline),
        modifier =
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = c.accent),
                onClick = { uriHandler.openUri(url) },
            ),
    )
}

private const val STEP_GAP = 10
private const val DETAIL_GAP = 2
private const val LINK_GAP = 4
private const val STEP_INDICATOR_SIZE = 12
private const val STEP_INDICATOR_TOP_OFFSET = 6
internal const val TX_HASH_ELLIPSIS_PREFIX = 12
internal const val TX_HASH_ELLIPSIS_SUFFIX = 8
