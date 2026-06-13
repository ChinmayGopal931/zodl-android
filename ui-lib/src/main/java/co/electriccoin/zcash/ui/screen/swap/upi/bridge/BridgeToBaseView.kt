package co.electriccoin.zcash.ui.screen.swap.upi.bridge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.component.zapp.ZappBottomActionBar
import co.electriccoin.zcash.ui.design.component.zapp.ZappButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappButtonVariant
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.swap.upi.OfframpAmountField
import co.electriccoin.zcash.ui.screen.swap.upi.OfframpFieldLabel
import co.electriccoin.zcash.ui.screen.swap.upi.progress.OfframpStepList
import co.electriccoin.zcash.ui.screen.swap.upi.progress.UpiOfframpStep
import co.electriccoin.zcash.ui.screen.swap.upi.progress.UpiOfframpStepStatus

@Composable
internal fun BridgeToBaseView(state: BridgeToBaseState) {
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
                text = stringResource(R.string.bridge_to_base_title),
                style = ZappTheme.typography.display.copy(color = c.text),
            )
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            BasicText(
                text = state.explainer.getValue(),
                style = ZappTheme.typography.body.copy(color = c.textMuted),
            )

            state.baseBalanceText?.let { balance ->
                Spacer(modifier = Modifier.height(GAP_LG.dp))
                BasicText(
                    text = balance.getValue(),
                    style = ZappTheme.typography.caption.copy(color = c.textMuted),
                )
            }

            Spacer(modifier = Modifier.height(GAP_LG.dp))

            if (state.isInputVisible) {
                OfframpFieldLabel(stringResource(R.string.bridge_to_base_amount_label))
                Spacer(modifier = Modifier.height(GAP_SM.dp))
                OfframpAmountField(
                    tokenLabel = stringResource(R.string.upi_offramp_token_usdc),
                    state = state.amountInput,
                )
                state.inrValueText?.let { inr ->
                    Spacer(modifier = Modifier.height(GAP_SM.dp))
                    BasicText(
                        text = inr.getValue(),
                        style = ZappTheme.typography.caption.copy(color = c.text, fontWeight = FontWeight.Medium),
                    )
                }
                state.etaText?.let { eta ->
                    Spacer(modifier = Modifier.height(GAP_XS.dp))
                    BasicText(
                        text = eta.getValue(),
                        style = ZappTheme.typography.caption.copy(color = c.textMuted),
                    )
                }
                state.availabilityHint?.let { hint ->
                    Spacer(modifier = Modifier.height(GAP_MD.dp))
                    BasicText(
                        text = hint.text.getValue(),
                        style =
                            ZappTheme.typography.caption.copy(
                                color = if (hint.isWarning) c.danger else c.textMuted,
                                fontWeight = FontWeight.Medium,
                            ),
                    )
                }
            } else {
                OfframpStepList(state.steps)
            }

            state.errorText?.let { err ->
                Spacer(modifier = Modifier.height(GAP_MD.dp))
                BasicText(
                    text = err.getValue(),
                    style = ZappTheme.typography.caption.copy(color = c.danger, fontWeight = FontWeight.Medium),
                )
            }
        }

        ZappBottomActionBar(
            onBack = state.onBack,
            primaryAction = {
                ZappButton(
                    text = state.primaryButton.text.getValue(),
                    enabled = state.primaryButton.isEnabled,
                    variant = ZappButtonVariant.Primary,
                    modifier = Modifier.weight(1f).padding(start = BOTTOM_BAR_GAP.dp),
                    onClick = state.primaryButton.onClick,
                )
            },
        )
    }
}

private const val HORIZONTAL_PADDING = 18
private const val VERTICAL_PADDING = 16
private const val BOTTOM_BAR_GAP = 12
private const val GAP_XS = 4
private const val GAP_SM = 6
private const val GAP_MD = 10
private const val GAP_LG = 20

@PreviewScreens
@Composable
private fun PreviewInput() {
    ZcashTheme {
        BridgeToBaseView(
            state =
                BridgeToBaseState(
                    amountInput =
                        NumberTextFieldState(NumberTextFieldInnerState.fromAmount(java.math.BigDecimal("5.04"))) {},
                    baseBalanceText = stringRes("Available on Base: 0.85 USDC"),
                    inrValueText = stringRes("≈ ₹428 at the current rate"),
                    etaText = stringRes("Estimated time: ~10 min"),
                    availabilityHint = BridgeAvailabilityHint(stringRes("Merchants are available now."), isWarning = false),
                    explainer = stringRes("We bridge your ZEC to Base. This takes a few minutes; after that, paying merchants is instant."),
                    errorText = null,
                    steps = emptyList(),
                    isInputVisible = true,
                    primaryButton = ButtonState(stringRes("Add funds"), isEnabled = true),
                    onBack = {},
                ),
        )
    }
}

@PreviewScreens
@Composable
private fun PreviewBridging() {
    ZcashTheme {
        BridgeToBaseView(
            state =
                BridgeToBaseState(
                    amountInput = NumberTextFieldState(NumberTextFieldInnerState.fromAmount(java.math.BigDecimal("5.04"))) {},
                    baseBalanceText = stringRes("Available on Base: 0.85 USDC"),
                    inrValueText = null,
                    etaText = null,
                    availabilityHint = null,
                    explainer = stringRes("We bridge your ZEC to Base. This takes a few minutes."),
                    errorText = null,
                    steps =
                        listOf(
                            UpiOfframpStep(stringRes("Bridging from ZEC via NEAR"), UpiOfframpStepStatus.InProgress),
                            UpiOfframpStep(stringRes("Funds arrived on Base"), UpiOfframpStepStatus.Pending),
                        ),
                    isInputVisible = false,
                    primaryButton = ButtonState(stringRes("Bridging…"), isEnabled = false),
                    onBack = {},
                ),
        )
    }
}
