package co.electriccoin.zcash.ui.screen.swap.upi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.component.ZashiAddressTextField
import co.electriccoin.zcash.ui.design.component.ZashiImageButton
import co.electriccoin.zcash.ui.design.component.ZashiNumberTextField
import co.electriccoin.zcash.ui.design.component.ZashiNumberTextFieldDefaults
import co.electriccoin.zcash.ui.design.component.zapp.ZappButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappButtonVariant
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.stringRes
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal

@Composable
internal fun UpiOfframpBody() {
    val vm = koinViewModel<UpiOfframpVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    UpiOfframpView(state = state)
}

@Composable
internal fun UpiOfframpView(state: UpiOfframpState) {
    val c = ZappTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BODY_HORIZONTAL_PADDING.dp, vertical = BODY_VERTICAL_PADDING.dp),
        ) {
            AmountFieldBlock(
                label = stringResource(R.string.upi_offramp_you_send),
                tokenLabel = stringResource(R.string.upi_offramp_token_usdc),
                state = state.usdcInput,
                isActive = state.primary == UpiOfframpAmountSide.USDC,
            )

            DirectionSwapButton(onClick = state.onSwapSides)

            AmountFieldBlock(
                label = stringResource(R.string.upi_offramp_recipient_gets),
                tokenLabel = stringResource(R.string.upi_offramp_token_inr),
                state = state.inrInput,
                isActive = state.primary == UpiOfframpAmountSide.INR,
            )

            Spacer(modifier = Modifier.height(GAP_LG.dp))

            BasicText(
                text = state.rateText.getValue(),
                style = ZappTheme.typography.caption.copy(color = c.textMuted),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(GAP_LG.dp))

            BasicText(
                text = stringResource(R.string.upi_offramp_upi_id_label),
                style = ZappTheme.typography.eyebrow.copy(color = c.textMuted),
            )
            Spacer(modifier = Modifier.height(GAP_SM.dp))
            UpiHandleField(state.upiField)

            Spacer(modifier = Modifier.height(GAP_MD.dp))

            state.errorText?.let { err ->
                BasicText(
                    text = err.getValue(),
                    style = ZappTheme.typography.caption.copy(
                        color = c.danger,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(modifier = Modifier.height(GAP_SM.dp))
            }

            state.infoText?.let { info ->
                BasicText(
                    text = info.getValue(),
                    style = ZappTheme.typography.caption.copy(color = c.textMuted),
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(GAP_LG.dp))
        }

        BottomBar(state.sendButton)
    }
}

@Composable
private fun AmountFieldBlock(
    label: String,
    tokenLabel: String,
    state: NumberTextFieldState,
    isActive: Boolean,
) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    BasicText(
        text = label,
        style = t.eyebrow.copy(color = c.textMuted),
    )
    Spacer(modifier = Modifier.height(GAP_SM.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .border(BorderStroke(1.dp, if (isActive) c.accent else c.border))
            .padding(horizontal = AMOUNT_PADDING.dp, vertical = AMOUNT_PADDING.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(c.surfaceAlt)
                .padding(horizontal = TOKEN_PADDING_H.dp, vertical = TOKEN_PADDING_V.dp),
        ) {
            BasicText(
                text = tokenLabel,
                style = t.button.copy(color = c.text, fontWeight = FontWeight.SemiBold),
            )
        }
        Spacer(modifier = Modifier.width(GAP_MD.dp))
        ZashiNumberTextField(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            textStyle = ZappTheme.typography.display.copy(
                color = c.text,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            ),
            contentPadding = PaddingValues(horizontal = INNER_FIELD_PADDING.dp, vertical = INNER_FIELD_PADDING.dp),
            placeholder = {
                ZashiNumberTextFieldDefaults.Placeholder(
                    modifier = Modifier.fillMaxWidth(),
                    style = ZappTheme.typography.display,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    contentAlignment = Alignment.CenterEnd,
                )
            },
        )
    }
}

@Composable
private fun DirectionSwapButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = GAP_SM.dp),
        contentAlignment = Alignment.Center,
    ) {
        val c = ZappTheme.colors
        Box(
            modifier = Modifier
                .size(DIRECTION_BUTTON_SIZE.dp)
                .background(c.surfaceAlt)
                .border(BorderStroke(1.dp, c.border))
                .padding(0.dp),
            contentAlignment = Alignment.Center,
        ) {
            ZashiImageButton(
                state = IconButtonState(
                    icon = R.drawable.ic_swap_change_mode,
                    onClick = onClick,
                ),
                modifier = Modifier.size(DIRECTION_BUTTON_SIZE.dp),
            )
        }
    }
}

@Composable
private fun UpiHandleField(field: TextFieldState) {
    val c = ZappTheme.colors
    ZashiAddressTextField(
        state = field,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = stringResource(R.string.upi_offramp_upi_id_placeholder),
                style = ZappTheme.typography.body,
                color = c.textSubtle,
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
        ),
    )
}

@Composable
private fun BottomBar(send: ButtonState) {
    val c = ZappTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .border(BorderStroke(1.dp, c.border), RectangleShape)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = BODY_HORIZONTAL_PADDING.dp, vertical = BOTTOM_BAR_VERTICAL_PADDING.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        ZappButton(
            text = send.text.getValue(),
            enabled = send.isEnabled,
            variant = ZappButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
            onClick = send.onClick,
        )
    }
}

private const val BODY_HORIZONTAL_PADDING = 18
private const val BODY_VERTICAL_PADDING = 12
private const val BOTTOM_BAR_VERTICAL_PADDING = 12
private const val GAP_SM = 6
private const val GAP_MD = 10
private const val GAP_LG = 16
private const val AMOUNT_PADDING = 12
private const val TOKEN_PADDING_H = 12
private const val TOKEN_PADDING_V = 8
private const val INNER_FIELD_PADDING = 4
private const val DIRECTION_BUTTON_SIZE = 40

@PreviewScreens
@Composable
private fun PreviewEmpty() {
    ZcashTheme {
        UpiOfframpView(
            state = UpiOfframpState(
                primary = UpiOfframpAmountSide.INR,
                usdcInput = NumberTextFieldState(NumberTextFieldInnerState(), onValueChange = {}),
                inrInput = NumberTextFieldState(NumberTextFieldInnerState(), onValueChange = {}),
                onSwapSides = {},
                rateText = stringRes("1 USDC ≈ ₹85"),
                upiField = TextFieldState(stringRes("")) {},
                infoText = null,
                errorText = null,
                sendButton = ButtonState(stringRes("Send")),
            ),
        )
    }
}

@PreviewScreens
@Composable
private fun PreviewFilled() {
    ZcashTheme {
        UpiOfframpView(
            state = UpiOfframpState(
                primary = UpiOfframpAmountSide.INR,
                usdcInput = NumberTextFieldState(
                    NumberTextFieldInnerState.fromAmount(BigDecimal("5.8824")),
                    onValueChange = {},
                ),
                inrInput = NumberTextFieldState(
                    NumberTextFieldInnerState.fromAmount(BigDecimal("500.00")),
                    onValueChange = {},
                ),
                onSwapSides = {},
                rateText = stringRes("1 USDC ≈ ₹85"),
                upiField = TextFieldState(stringRes("merchant@upi")) {},
                infoText = stringRes("Final amount locks when merchant accepts."),
                errorText = null,
                sendButton = ButtonState(stringRes("Send"), isEnabled = true),
            ),
        )
    }
}
