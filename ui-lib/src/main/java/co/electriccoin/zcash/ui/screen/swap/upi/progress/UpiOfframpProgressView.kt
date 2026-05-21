package co.electriccoin.zcash.ui.screen.swap.upi.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.zapp.ZappBackButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappButtonVariant
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.stringRes

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
                .verticalScroll(rememberScrollState())
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

            state.summary?.let { summary ->
                Spacer(modifier = Modifier.height(GAP_LG.dp))
                OrderSummaryCard(summary)
            }

            state.failure?.let { failure ->
                Spacer(modifier = Modifier.height(GAP_LG.dp))
                FailureCard(failure)
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
private fun OrderSummaryCard(summary: UpiOfframpOrderSummary) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .border(BorderStroke(1.dp, c.border))
            .padding(CARD_PADDING.dp),
    ) {
        SummaryRow(
            label = stringResource(R.string.upi_offramp_summary_amount),
            value = summary.amountUsdcDisplay.getValue() + " USDC",
        )
        Spacer(modifier = Modifier.height(GAP_SM.dp))
        SummaryRow(
            label = stringResource(R.string.upi_offramp_summary_recipient),
            value = summary.recipient,
        )
        summary.orderId?.let { orderId ->
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            SummaryRow(
                label = stringResource(R.string.upi_offramp_summary_order_id),
                value = "#$orderId",
            )
        }
        Spacer(modifier = Modifier.height(GAP_SM.dp))
        SummaryRow(
            label = stringResource(R.string.upi_offramp_summary_network),
            value = summary.networkName,
        )
        Spacer(modifier = Modifier.height(GAP_SM.dp))
        BasicText(
            text = stringResource(R.string.upi_offramp_summary_signer),
            style = t.caption.copy(color = c.textMuted, fontWeight = FontWeight.Medium),
        )
        Spacer(modifier = Modifier.height(2.dp))
        AddressLink(
            address = summary.signerAddress,
            url = summary.signerExplorerUrl,
            uriHandler = uriHandler,
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = t.caption.copy(color = c.textMuted, fontWeight = FontWeight.Medium),
        )
        BasicText(
            text = value,
            style = t.body.copy(color = c.text, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = GAP_MD.dp),
        )
    }
}

@Composable
private fun AddressLink(
    address: String,
    url: String,
    uriHandler: androidx.compose.ui.platform.UriHandler,
) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    BasicText(
        text = address.ellipsizeMiddle(ADDRESS_ELLIPSIS_PREFIX, ADDRESS_ELLIPSIS_SUFFIX),
        style = t.mono.copy(color = c.accent, textDecoration = TextDecoration.Underline),
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = c.accent),
            onClick = { uriHandler.openUri(url) },
        ),
    )
}

@Composable
private fun FailureCard(failure: UpiOfframpFailureCard) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .border(BorderStroke(1.dp, c.danger))
            .padding(CARD_PADDING.dp),
    ) {
        BasicText(
            text = stringResource(R.string.upi_offramp_failure_header, failure.stepLabel.getValue()),
            style = t.button.copy(color = c.danger, fontWeight = FontWeight.SemiBold),
        )
        failure.decodedReason?.let { reason ->
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            BasicText(
                text = reason.getValue(),
                style = t.body.copy(color = c.text),
            )
        }
        failure.rawSelector?.let { selector ->
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            BasicText(
                text = stringResource(R.string.upi_offramp_failure_selector, selector),
                style = t.caption.copy(color = c.textMuted),
            )
        }
        Spacer(modifier = Modifier.height(GAP_SM.dp))
        BasicText(
            text = failure.rawMessage,
            style = t.caption.copy(color = c.textMuted, fontFamily = t.mono.fontFamily),
        )
        if (failure.txHash != null && failure.txExplorerUrl != null) {
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            TxHashRow(
                hash = failure.txHash,
                url = failure.txExplorerUrl,
                uriHandler = uriHandler,
            )
        }
    }
}

@Composable
private fun StepRow(step: UpiOfframpStep) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
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
            step.detailLines.forEach { detail ->
                Spacer(modifier = Modifier.height(2.dp))
                BasicText(
                    text = detail.getValue(),
                    style = t.caption.copy(color = c.textMuted),
                )
            }
            if (step.txHash != null && step.txExplorerUrl != null) {
                Spacer(modifier = Modifier.height(4.dp))
                TxHashRow(
                    hash = step.txHash,
                    url = step.txExplorerUrl,
                    uriHandler = uriHandler,
                )
            }
        }
    }
}

@Composable
private fun TxHashRow(
    hash: String,
    url: String,
    uriHandler: androidx.compose.ui.platform.UriHandler,
) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    BasicText(
        text = hash.ellipsizeMiddle(TX_HASH_ELLIPSIS_PREFIX, TX_HASH_ELLIPSIS_SUFFIX),
        style = t.mono.copy(color = c.accent, textDecoration = TextDecoration.Underline),
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = c.accent),
            onClick = { uriHandler.openUri(url) },
        ),
    )
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
            .padding(top = STEP_INDICATOR_TOP_OFFSET.dp)
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

private fun String.ellipsizeMiddle(prefix: Int, suffix: Int): String {
    if (length <= prefix + suffix + 1) return this
    return take(prefix) + "…" + takeLast(suffix)
}

private const val HORIZONTAL_PADDING = 18
private const val VERTICAL_PADDING = 16
private const val BOTTOM_BAR_VERTICAL_PADDING = 12
private const val CARD_PADDING = 14
private const val GAP_SM = 6
private const val GAP_MD = 10
private const val GAP_LG = 20
private const val STEP_INDICATOR_SIZE = 12
private const val STEP_INDICATOR_TOP_OFFSET = 6
private const val ADDRESS_ELLIPSIS_PREFIX = 10
private const val ADDRESS_ELLIPSIS_SUFFIX = 6
private const val TX_HASH_ELLIPSIS_PREFIX = 12
private const val TX_HASH_ELLIPSIS_SUFFIX = 8

private val previewSummary = UpiOfframpOrderSummary(
    amountUsdcDisplay = stringRes("5.00"),
    recipient = "merchant@upi",
    orderId = "12345",
    networkName = "Sepolia",
    signerAddress = "0x1234567890abcdef1234567890abcdef12345678",
    signerExplorerUrl = "https://sepolia.basescan.org/address/0x1234567890abcdef1234567890abcdef12345678",
)

@PreviewScreens
@Composable
private fun PreviewInProgress() {
    ZcashTheme {
        UpiOfframpProgressView(
            state = UpiOfframpProgressState(
                title = stringRes("Sending to merchant"),
                subtitle = stringRes("Recipient: merchant@upi"),
                summary = previewSummary,
                steps = listOf(
                    UpiOfframpStep(stringRes("Picking a merchant pool"), UpiOfframpStepStatus.Completed),
                    UpiOfframpStep(stringRes("Approving USDC"), UpiOfframpStepStatus.Completed),
                    UpiOfframpStep(stringRes("Placing the order"), UpiOfframpStepStatus.InProgress),
                    UpiOfframpStep(stringRes("Waiting for merchant"), UpiOfframpStepStatus.Pending),
                ),
                failure = null,
                primaryButton = null,
                onBack = {},
            ),
        )
    }
}

@PreviewScreens
@Composable
private fun PreviewCompleted() {
    ZcashTheme {
        UpiOfframpProgressView(
            state = UpiOfframpProgressState(
                title = stringRes("Payment sent"),
                subtitle = stringRes("The merchant has confirmed the UPI transfer."),
                summary = previewSummary,
                steps = listOf(
                    UpiOfframpStep(stringRes("Picking a merchant pool"), UpiOfframpStepStatus.Completed),
                    UpiOfframpStep(stringRes("Approving USDC"), UpiOfframpStepStatus.Completed),
                    UpiOfframpStep(stringRes("Placing the order"), UpiOfframpStepStatus.Completed),
                    UpiOfframpStep(stringRes("Waiting for merchant"), UpiOfframpStepStatus.Completed),
                ),
                failure = null,
                primaryButton = ButtonState(
                    text = stringRes("Done"),
                    onClick = {},
                ),
                onBack = {},
            ),
        )
    }
}

@PreviewScreens
@Composable
private fun PreviewFailed() {
    ZcashTheme {
        UpiOfframpProgressView(
            state = UpiOfframpProgressState(
                title = stringRes("Something went wrong"),
                subtitle = null,
                summary = previewSummary,
                steps = listOf(
                    UpiOfframpStep(stringRes("Picking a merchant pool"), UpiOfframpStepStatus.Completed),
                    UpiOfframpStep(stringRes("Approving USDC"), UpiOfframpStepStatus.Failed),
                    UpiOfframpStep(stringRes("Placing the order"), UpiOfframpStepStatus.Pending),
                ),
                failure = UpiOfframpFailureCard(
                    stepLabel = stringRes("Approving USDC"),
                    decodedReason = stringRes("Insufficient USDC balance for approval."),
                    rawSelector = "0x91da284f",
                    rawMessage = "execution reverted: ERC20: transfer amount exceeds balance",
                    txHash = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                    txExplorerUrl = "https://sepolia.basescan.org/tx/0xabcdef",
                ),
                primaryButton = ButtonState(
                    text = stringRes("Close"),
                    onClick = {},
                ),
                onBack = {},
            ),
        )
    }
}
