package co.electriccoin.zcash.ui.screen.swap.upi

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.zapp.ZappConfirmationBottomSheet
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.component.ZashiTextField
import co.electriccoin.zcash.ui.design.component.zapp.ZappBottomActionBar
import co.electriccoin.zcash.ui.design.component.zapp.ZappButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappButtonVariant
import co.electriccoin.zcash.ui.design.component.zapp.ZappFab
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.stringRes
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal

@Composable
internal fun UpiOfframpBody(onBack: () -> Unit = {}) {
    val vm = koinViewModel<UpiOfframpVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    val payConfirmation by vm.payConfirmation.collectAsStateWithLifecycle()
    UpiOfframpView(state = state, onBack = onBack)
    ZappConfirmationBottomSheet(state = payConfirmation)
}

@Composable
internal fun UpiOfframpView(
    state: UpiOfframpState,
    onBack: () -> Unit = {},
) {
    val c = ZappTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = BODY_HORIZONTAL_PADDING.dp, vertical = BODY_VERTICAL_PADDING.dp),
            ) {
                ZappButton(
                    text = stringResource(R.string.upi_offramp_scan_and_pay),
                    leadingIcon = Icons.Default.QrCodeScanner,
                    variant = ZappButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = state.onScanQr,
                )
                Spacer(modifier = Modifier.height(GAP_SM.dp))
                BasicText(
                    text = stringResource(R.string.upi_offramp_scan_hint),
                    style = ZappTheme.typography.caption.copy(color = c.textMuted),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(GAP_LG.dp))

                OfframpFieldLabel(stringResource(R.string.upi_offramp_upi_id_label))
                Spacer(modifier = Modifier.height(GAP_SM.dp))
                UpiHandleField(state.upiField)

                Spacer(modifier = Modifier.height(GAP_LG.dp))

                OfframpFieldLabel(stringResource(R.string.upi_offramp_amount_label))
                Spacer(modifier = Modifier.height(GAP_SM.dp))
                OfframpAmountField(
                    tokenLabel = stringResource(R.string.upi_offramp_token_inr),
                    state = state.inrInput,
                )

                Spacer(modifier = Modifier.height(GAP_SM.dp))
                state.usdcEquivalent?.let { usdc ->
                    BasicText(
                        text = usdc.getValue(),
                        style = ZappTheme.typography.caption.copy(color = c.text, fontWeight = FontWeight.Medium),
                    )
                    Spacer(modifier = Modifier.height(GAP_XS.dp))
                }
                BasicText(
                    text = state.rateText.getValue(),
                    style = ZappTheme.typography.caption.copy(color = c.textMuted),
                )
                Spacer(modifier = Modifier.height(GAP_XS.dp))
                BasicText(
                    text = stringResource(R.string.upi_offramp_limit_hint),
                    style = ZappTheme.typography.caption.copy(color = c.textMuted),
                )
                state.baseBalanceText?.let { balance ->
                    Spacer(modifier = Modifier.height(GAP_XS.dp))
                    BasicText(
                        text = balance.getValue(),
                        style = ZappTheme.typography.caption.copy(color = c.textMuted),
                    )
                }

                state.errorText?.let { err ->
                    Spacer(modifier = Modifier.height(GAP_MD.dp))
                    BasicText(
                        text = err.getValue(),
                        style =
                            ZappTheme.typography.caption.copy(
                                color = c.danger,
                                fontWeight = FontWeight.Medium,
                            ),
                    )
                }

                state.infoText?.let { info ->
                    Spacer(modifier = Modifier.height(GAP_MD.dp))
                    BasicText(
                        text = info.getValue(),
                        style = ZappTheme.typography.caption.copy(color = c.textMuted),
                    )
                }

                state.fundingPlanText?.let { plan ->
                    Spacer(modifier = Modifier.height(GAP_SM.dp))
                    BasicText(
                        text = plan.getValue(),
                        style =
                            ZappTheme.typography.caption.copy(
                                color = c.text,
                                fontWeight = FontWeight.Medium,
                            ),
                    )
                }

                Spacer(modifier = Modifier.height(GAP_LG.dp))
                ZappButton(
                    text = stringResource(R.string.upi_offramp_add_funds_button),
                    variant = ZappButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = state.onAddFunds,
                )

                state.onDiscardInFlight?.let { onDiscard ->
                    Spacer(modifier = Modifier.height(GAP_SM.dp))
                    BasicText(
                        text = stringResource(R.string.upi_offramp_discard_in_flight),
                        style =
                            ZappTheme.typography.caption.copy(
                                color = c.danger,
                                textDecoration = TextDecoration.Underline,
                            ),
                        modifier = Modifier.clickable(onClick = onDiscard),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(GAP_LG.dp))
                PoweredByP2p()
            }

            ZappFab(
                icon = Icons.Default.History,
                contentDescription = stringResource(R.string.p2p_transactions_title),
                onClick = state.onHistoryClick,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = BODY_HORIZONTAL_PADDING.dp, bottom = BODY_VERTICAL_PADDING.dp),
            )
        }

        ZappBottomActionBar(
            onBack = onBack,
            primaryAction = {
                ZappButton(
                    text = state.sendButton.text.getValue(),
                    enabled = state.sendButton.isEnabled,
                    variant = ZappButtonVariant.Primary,
                    modifier = Modifier.weight(1f).padding(start = BOTTOM_BAR_GAP.dp),
                    onClick = state.sendButton.onClick,
                )
            },
        )
    }
}

@Composable
private fun UpiHandleField(field: TextFieldState) {
    val c = ZappTheme.colors
    // Plain text field (not ZashiAddressTextField) so a UPI handle shows in full — the address
    // field middle-ellipsizes anything over 16 chars even when it would fit.
    ZashiTextField(
        state = field,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.upi_offramp_upi_id_placeholder),
                style = ZappTheme.typography.body,
                color = c.textSubtle,
            )
        },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
    )
}

@Composable
private fun PoweredByP2p() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_p2p_logo),
            contentDescription = null,
            modifier = Modifier.height(POWERED_BY_LOGO_SIZE.dp),
        )
        Spacer(modifier = Modifier.width(GAP_XS.dp))
        BasicText(
            text = stringResource(R.string.upi_offramp_powered_by),
            style = ZappTheme.typography.caption.copy(color = ZappTheme.colors.textMuted),
        )
    }
}

private const val BODY_HORIZONTAL_PADDING = 18
private const val BODY_VERTICAL_PADDING = 12
private const val BOTTOM_BAR_GAP = 12
private const val GAP_XS = 4
private const val GAP_SM = 6
private const val GAP_MD = 10
private const val GAP_LG = 16
private const val POWERED_BY_LOGO_SIZE = 14

@PreviewScreens
@Composable
private fun PreviewEmpty() {
    ZcashTheme {
        UpiOfframpView(
            state =
                UpiOfframpState(
                    onScanQr = {},
                    upiField = TextFieldState(stringRes("")) {},
                    inrInput = NumberTextFieldState(NumberTextFieldInnerState(), onValueChange = {}),
                    usdcEquivalent = null,
                    rateText = stringRes("1 USDC ≈ ₹85"),
                    infoText = null,
                    errorText = null,
                    sendButton = ButtonState(stringRes("Pay")),
                    onHistoryClick = {},
                    onAddFunds = {},
                ),
        )
    }
}

@PreviewScreens
@Composable
private fun PreviewFilled() {
    ZcashTheme {
        UpiOfframpView(
            state =
                UpiOfframpState(
                    onScanQr = {},
                    upiField = TextFieldState(stringRes("merchant@upi")) {},
                    inrInput =
                        NumberTextFieldState(
                            NumberTextFieldInnerState.fromAmount(BigDecimal("500.00")),
                            onValueChange = {},
                        ),
                    usdcEquivalent = stringRes("≈ 5.88 USDC"),
                    rateText = stringRes("1 USDC ≈ ₹85"),
                    infoText = stringRes("Estimated. Final amount locks in when the merchant accepts."),
                    errorText = null,
                    sendButton = ButtonState(stringRes("Pay"), isEnabled = true),
                    onHistoryClick = {},
                    onAddFunds = {},
                    baseBalanceText = stringRes("Available on Base: 0.85 USDC"),
                    fundingPlanText = stringRes("You'll add about 5.04 USDC to Base first, then pay."),
                ),
        )
    }
}
