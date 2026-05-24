package co.electriccoin.zcash.ui.screen.swap.upi.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.zapp.ZappBorderedCard
import co.electriccoin.zcash.ui.design.component.zapp.ZappBottomActionBar
import co.electriccoin.zcash.ui.design.component.zapp.ZappButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappButtonVariant
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.ellipsizeMiddle
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.stringRes

@Composable
internal fun UpiOfframpProgressView(state: UpiOfframpProgressState) {
    val c = ZappTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
    ) {
        Column(
            modifier =
                Modifier
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

            state.feeBreakdown?.let { fees ->
                Spacer(modifier = Modifier.height(GAP_LG.dp))
                FeeBreakdownCard(fees)
            }

            state.cancelled?.let { cancelled ->
                Spacer(modifier = Modifier.height(GAP_LG.dp))
                CancelledCard(cancelled)
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
        ZappBottomActionBar(
            onBack = state.onBack,
            primaryAction =
                state.primaryButton?.let { btn ->
                    {
                        ZappButton(
                            text = btn.text.getValue(),
                            enabled = btn.isEnabled,
                            variant = ZappButtonVariant.Primary,
                            onClick = btn.onClick,
                        )
                    }
                },
        )
    }
}

@Composable
private fun OrderSummaryCard(summary: UpiOfframpOrderSummary) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    val uriHandler = LocalUriHandler.current
    ZappBorderedCard {
        SummaryRow(
            label = stringResource(R.string.upi_offramp_summary_amount),
            value = summary.amountUsdcDisplay.getValue(),
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
                value = stringResource(R.string.upi_offramp_summary_order_id_value, orderId),
            )
        }
        Spacer(modifier = Modifier.height(GAP_SM.dp))
        SummaryRow(
            label = stringResource(R.string.upi_offramp_summary_network),
            value = summary.networkName,
        )
        summary.completionDuration?.let { duration ->
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            SummaryRow(
                label = stringResource(R.string.upi_offramp_summary_completion),
                value = duration.getValue(),
            )
        }
        summary.terminalTimestamp?.let { ts ->
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            SummaryRow(
                label = stringResource(R.string.upi_offramp_summary_time),
                value = ts.getValue(),
            )
        }
        if (summary.merchantAddress != null && summary.merchantExplorerUrl != null) {
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            SummaryLinkRow(
                label = stringResource(R.string.upi_offramp_summary_merchant),
                value = summary.merchantAddress,
                url = summary.merchantExplorerUrl,
                uriHandler = uriHandler,
            )
        }
        if (summary.signerAddress != null && summary.signerExplorerUrl != null) {
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            SummaryLinkRow(
                label = stringResource(R.string.upi_offramp_summary_signer),
                value = summary.signerAddress,
                url = summary.signerExplorerUrl,
                uriHandler = uriHandler,
            )
        }
    }
}

@Composable
private fun FeeBreakdownCard(fees: UpiOfframpFeeBreakdown) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    ZappBorderedCard {
        BasicText(
            text = stringResource(R.string.upi_offramp_fee_breakdown_header),
            style = t.button.copy(color = c.text, fontWeight = FontWeight.SemiBold),
        )
        fees.youSend?.let {
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            SummaryRow(stringResource(R.string.upi_offramp_fee_breakdown_you_send), it.getValue())
        }
        fees.fee?.let {
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            SummaryRow(stringResource(R.string.upi_offramp_fee_breakdown_fee), it.getValue())
        }
        fees.youReceive?.let {
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            SummaryRow(stringResource(R.string.upi_offramp_fee_breakdown_you_receive), it.getValue())
        }
    }
}

@Composable
private fun CancelledCard(cancelled: UpiOfframpCancelledCard) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    ZappBorderedCard {
        cancelled.refundedAmount?.let {
            BasicText(
                text = it.getValue(),
                style = t.body.copy(color = c.text, fontWeight = FontWeight.SemiBold),
            )
        }
        cancelled.cancelledAt?.let {
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            BasicText(
                text = it.getValue(),
                style = t.caption.copy(color = c.textMuted),
            )
        }
        Spacer(modifier = Modifier.height(GAP_SM.dp))
        BasicText(
            text = cancelled.tip.getValue(),
            style = t.caption.copy(color = c.textMuted),
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

/**
 * Label on the left, ellipsized clickable monospace explorer link on the right —
 * same row shape as [SummaryRow] but with a tappable underlined value.
 */
@Composable
private fun SummaryLinkRow(label: String, value: String, url: String, uriHandler: UriHandler) {
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
            text = value.ellipsizeMiddle(ADDRESS_ELLIPSIS_PREFIX, ADDRESS_ELLIPSIS_SUFFIX),
            style = t.mono.copy(color = c.accent, textDecoration = TextDecoration.Underline),
            modifier =
                Modifier
                    .padding(start = GAP_MD.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = c.accent),
                        onClick = { uriHandler.openUri(url) },
                    ),
        )
    }
}

/** Ellipsized, tappable monospace link to a block explorer (address or tx hash). */
@Composable
private fun ExplorerLink(
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

@Composable
private fun FailureCard(failure: UpiOfframpFailureCard) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    val uriHandler = LocalUriHandler.current
    ZappBorderedCard(borderColor = c.danger) {
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
            ExplorerLink(
                value = failure.txHash,
                url = failure.txExplorerUrl,
                prefix = TX_HASH_ELLIPSIS_PREFIX,
                suffix = TX_HASH_ELLIPSIS_SUFFIX,
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
                Spacer(modifier = Modifier.height(2.dp))
                BasicText(
                    text = detail.getValue(),
                    style = t.caption.copy(color = c.textMuted),
                )
            }
            if (step.txHash != null && step.txExplorerUrl != null) {
                Spacer(modifier = Modifier.height(4.dp))
                ExplorerLink(
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

private const val HORIZONTAL_PADDING = 18
private const val VERTICAL_PADDING = 16
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

private val previewSummary =
    UpiOfframpOrderSummary(
        amountUsdcDisplay = stringRes("5.00"),
        recipient = "merchant@upi",
        orderId = "12345",
        networkName = "Sepolia",
        signerAddress = "0x1234567890abcdef1234567890abcdef12345678",
        signerExplorerUrl = "https://sepolia.basescan.org/address/0x1234567890abcdef1234567890abcdef12345678",
    )

private fun previewSteps(
    funding: UpiOfframpStepStatus = UpiOfframpStepStatus.Completed,
    approving: UpiOfframpStepStatus = UpiOfframpStepStatus.Completed,
    placing: UpiOfframpStepStatus = UpiOfframpStepStatus.Completed,
    waitingAcceptance: UpiOfframpStepStatus = UpiOfframpStepStatus.Completed,
    sendingUpi: UpiOfframpStepStatus = UpiOfframpStepStatus.Completed,
    waitingCompletion: UpiOfframpStepStatus = UpiOfframpStepStatus.Completed,
    fundingDetails: List<StringResource> = emptyList(),
    waitingDetails: List<StringResource> = emptyList(),
): List<UpiOfframpStep> =
    listOf(
        UpiOfframpStep(stringRes("Picking a merchant pool"), UpiOfframpStepStatus.Completed),
        UpiOfframpStep(stringRes("Bridging funds"), funding, detailLines = fundingDetails),
        UpiOfframpStep(stringRes("Approving USDC"), approving),
        UpiOfframpStep(stringRes("Placing the order"), placing),
        UpiOfframpStep(stringRes("Waiting for merchant to accept"), waitingAcceptance, detailLines = waitingDetails),
        UpiOfframpStep(stringRes("Sending encrypted UPI"), sendingUpi),
        UpiOfframpStep(stringRes("Waiting for merchant payment"), waitingCompletion),
    )

@PreviewScreens
@Composable
private fun PreviewInProgress() {
    ZcashTheme {
        UpiOfframpProgressView(
            state =
                UpiOfframpProgressState(
                    title = stringRes("Sending to merchant"),
                    subtitle = stringRes("Recipient: merchant@upi"),
                    summary = previewSummary,
                    feeBreakdown = null,
                    cancelled = null,
                    steps =
                        previewSteps(
                            placing = UpiOfframpStepStatus.InProgress,
                            waitingAcceptance = UpiOfframpStepStatus.Pending,
                            sendingUpi = UpiOfframpStepStatus.Pending,
                            waitingCompletion = UpiOfframpStepStatus.Pending,
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
private fun PreviewFunding() {
    ZcashTheme {
        UpiOfframpProgressView(
            state =
                UpiOfframpProgressState(
                    title = stringRes("Sending to merchant"),
                    subtitle = stringRes("Recipient: merchant@upi"),
                    summary = previewSummary,
                    feeBreakdown = null,
                    cancelled = null,
                    steps =
                        previewSteps(
                            funding = UpiOfframpStepStatus.InProgress,
                            approving = UpiOfframpStepStatus.Pending,
                            placing = UpiOfframpStepStatus.Pending,
                            waitingAcceptance = UpiOfframpStepStatus.Pending,
                            sendingUpi = UpiOfframpStepStatus.Pending,
                            waitingCompletion = UpiOfframpStepStatus.Pending,
                            fundingDetails =
                                listOf(
                                    stringRes("Bridging 5.00 USDC from ZEC via NEAR Intents"),
                                    stringRes("Deposit 0x833589fC…02913"),
                                ),
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
private fun PreviewWaitingStalled() {
    // Matches the official client's pay/placed UX: no user action during merchant-search. The
    // contract auto-cancels on its own timer and our refund button only surfaces post-Cancelled.
    ZcashTheme {
        UpiOfframpProgressView(
            state =
                UpiOfframpProgressState(
                    title = stringRes("Sending to merchant"),
                    subtitle = stringRes("Recipient: merchant@upi"),
                    summary = previewSummary,
                    feeBreakdown = null,
                    cancelled = null,
                    steps =
                        previewSteps(
                            waitingAcceptance = UpiOfframpStepStatus.InProgress,
                            sendingUpi = UpiOfframpStepStatus.Pending,
                            waitingCompletion = UpiOfframpStepStatus.Pending,
                            waitingDetails =
                                listOf(
                                    stringRes("Polling… attempt 612"),
                                    stringRes("Last on-chain status: PLACED"),
                                    stringRes(
                                        "Still connecting you to a merchant. Your USDC stays in your account; if no merchant accepts, the order auto-cancels on chain."
                                    ),
                                ),
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
            state =
                UpiOfframpProgressState(
                    title = stringRes("Payment sent"),
                    subtitle = stringRes("The merchant has confirmed the UPI transfer."),
                    summary =
                        previewSummary.copy(
                            completionDuration = stringRes("1m 32s"),
                            terminalTimestamp = stringRes("22 May 2026, 02:43 AM"),
                            merchantAddress = "0x1111111111111111111111111111111111111111",
                            merchantExplorerUrl = "https://sepolia.basescan.org/address/0x1111",
                        ),
                    feeBreakdown =
                        UpiOfframpFeeBreakdown(
                            youSend = stringRes("5.000 USDC"),
                            fee = stringRes("0.050 USDC"),
                            youReceive = stringRes("₹445.00"),
                        ),
                    cancelled = null,
                    steps = previewSteps(),
                    failure = null,
                    primaryButton = ButtonState(text = stringRes("Done"), onClick = {}),
                    onBack = {},
                ),
        )
    }
}

@PreviewScreens
@Composable
private fun PreviewCancelled() {
    ZcashTheme {
        UpiOfframpProgressView(
            state =
                UpiOfframpProgressState(
                    title = stringRes("Order cancelled"),
                    subtitle = stringRes("No merchant completed this order in time. Your USDC has been refunded on-chain."),
                    summary =
                        previewSummary.copy(
                            terminalTimestamp = stringRes("22 May 2026, 03:51 AM"),
                        ),
                    feeBreakdown = null,
                    cancelled =
                        UpiOfframpCancelledCard(
                            refundedAmount = stringRes("5.00 USDC refunded to your offramp account"),
                            cancelledAt = stringRes("Cancelled at 22 May 2026, 03:51 AM"),
                            tip = stringRes("Tip: ask the merchant to generate the UPI QR only after this screen opens."),
                        ),
                    steps =
                        previewSteps(
                            waitingAcceptance = UpiOfframpStepStatus.Completed,
                            sendingUpi = UpiOfframpStepStatus.Completed,
                            waitingCompletion = UpiOfframpStepStatus.Failed,
                        ),
                    failure = null,
                    primaryButton = ButtonState(text = stringRes("Close"), onClick = {}),
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
            state =
                UpiOfframpProgressState(
                    title = stringRes("Something went wrong"),
                    subtitle = null,
                    summary = previewSummary,
                    feeBreakdown = null,
                    cancelled = null,
                    steps =
                        previewSteps(
                            funding = UpiOfframpStepStatus.Completed,
                            approving = UpiOfframpStepStatus.Failed,
                            placing = UpiOfframpStepStatus.Pending,
                            waitingAcceptance = UpiOfframpStepStatus.Pending,
                            sendingUpi = UpiOfframpStepStatus.Pending,
                            waitingCompletion = UpiOfframpStepStatus.Pending,
                        ),
                    failure =
                        UpiOfframpFailureCard(
                            stepLabel = stringRes("Approving USDC"),
                            decodedReason = stringRes("Insufficient USDC balance for approval."),
                            rawSelector = "0x91da284f",
                            rawMessage = "execution reverted: ERC20: transfer amount exceeds balance",
                            txHash = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                            txExplorerUrl = "https://sepolia.basescan.org/tx/0xabcdef",
                        ),
                    primaryButton = ButtonState(text = stringRes("Close"), onClick = {}),
                    onBack = {},
                ),
        )
    }
}

@PreviewScreens
@Composable
private fun PreviewFailedRecoverable() {
    ZcashTheme {
        UpiOfframpProgressView(
            state =
                UpiOfframpProgressState(
                    title = stringRes("Something went wrong"),
                    subtitle = null,
                    summary = previewSummary,
                    feeBreakdown = null,
                    cancelled = null,
                    steps =
                        previewSteps(
                            funding = UpiOfframpStepStatus.Completed,
                            approving = UpiOfframpStepStatus.Completed,
                            placing = UpiOfframpStepStatus.Failed,
                            waitingAcceptance = UpiOfframpStepStatus.Pending,
                            sendingUpi = UpiOfframpStepStatus.Pending,
                            waitingCompletion = UpiOfframpStepStatus.Pending,
                        ),
                    failure =
                        UpiOfframpFailureCard(
                            stepLabel = stringRes("Placing the order"),
                            decodedReason = stringRes("No merchant has fiat liquidity for this order right now."),
                            rawSelector = "0xea8e4eb5",
                            rawMessage = "execution reverted",
                            txHash = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                            txExplorerUrl = "https://sepolia.basescan.org/tx/0xabcdef",
                        ),
                    primaryButton = ButtonState(text = stringRes("Bridge USDC back to ZEC"), onClick = {}),
                    onBack = {},
                ),
        )
    }
}
