package co.electriccoin.zcash.ui.screen.addressbook

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.compose.ZashiTooltipBox
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.stringRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddressBookPopup(
    tooltipState: TooltipState,
    state: AddressBookState,
    modifier: Modifier = Modifier,
    anchor: @Composable () -> Unit
) {
    ZashiTooltipBox(
        modifier = modifier,
        state = tooltipState,
        tooltip = { Tooltip(state = state, onDismissRequest = { tooltipState.dismiss() }) },
        anchor = anchor
    )
}

@Composable
private fun Tooltip(
    state: AddressBookState,
    onDismissRequest: () -> Unit
) {
    val c = ZappTheme.colors
    Column(
        modifier = Modifier
            .width(200.dp)
            .background(c.surface, RectangleShape)
            .border(BorderStroke(1.dp, c.border), RectangleShape),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PopupTextButton(
            modifier = Modifier.fillMaxWidth(),
            state = state.manualButton,
            res = R.drawable.ic_add_contact_manual,
            onDismissRequest = onDismissRequest
        )
        PopupTextButton(
            modifier = Modifier.fillMaxWidth(),
            state = state.scanButton,
            res = R.drawable.ic_add_contact_qr,
            onDismissRequest = onDismissRequest
        )
    }
}

@Composable
private fun PopupTextButton(
    state: ButtonState,
    @DrawableRes res: Int,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = ZappTheme.colors
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = c.accent),
                onClick = {
                    onDismissRequest()
                    state.onClick()
                },
            )
            .semantics { role = Role.Button }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            painter = painterResource(res),
            contentDescription = state.text.getValue(),
            tint = c.textMuted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicText(
            modifier = Modifier.weight(1f),
            text = state.text.getValue(),
            style = ZappTheme.typography.rowTitle.copy(
                color = c.text,
                fontWeight = FontWeight.Black,
            ),
        )
    }
}

@PreviewScreens
@Composable
private fun PopupContentPreview() =
    ZcashTheme {
        Tooltip(
            state =
                AddressBookState(
                    onBack = {},
                    isLoading = false,
                    items = emptyList(),
                    scanButton =
                        ButtonState(
                            text = stringRes("Scan QR code"),
                        ),
                    manualButton =
                        ButtonState(
                            text = stringRes("Manual entry"),
                        ),
                    title = stringRes("Address book"),
                    info = null
                ),
            onDismissRequest = {}
        )
    }
