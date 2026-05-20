package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.chat.list.ChatListLeaveDialogState

@Composable
internal fun LeaveConfirmationDialog(state: ChatListLeaveDialogState) {
    val c = ZappTheme.colors
    val message =
        stringRes(R.string.chat_list_leave_dialog_message, state.conversationName).getValue()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.overlay)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = state.onDismiss,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .background(c.surface, RectangleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    ).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText(
                text = stringRes(R.string.chat_list_leave_dialog_title).getValue(),
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
            )
            BasicText(
                text = message,
                style = ZappTheme.typography.body.copy(color = c.textMuted),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DialogButton(
                    text = stringRes(R.string.chat_list_leave_dialog_cancel).getValue(),
                    background = c.surfaceAlt,
                    textColor = c.text,
                    onClick = state.onDismiss,
                    modifier = Modifier.weight(1f),
                )
                DialogButton(
                    text = stringRes(R.string.chat_list_leave_dialog_confirm).getValue(),
                    background = c.danger,
                    textColor = c.bg,
                    onClick = state.onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    background: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(48.dp)
                .background(background, RectangleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style =
                ZappTheme.typography.button.copy(
                    color = textColor,
                    fontWeight = FontWeight.Black,
                ),
        )
    }
}
