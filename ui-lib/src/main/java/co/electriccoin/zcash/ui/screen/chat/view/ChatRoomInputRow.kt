package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.screen.chat.room.ChatRoomInputState

@Composable
internal fun InputRow(state: ChatRoomInputState) {
    val c = ZappTheme.colors
    val attachContentDescription = state.attachContentDescription.getValue()
    val sendContentDescription = state.sendContentDescription.getValue()

    Column(modifier = Modifier.fillMaxWidth().background(c.surface)) {
        state.replyPreview?.let { reply ->
            ReplyPreviewStrip(
                senderName = reply.senderName,
                content = reply.content,
                onDismiss = reply.onDismiss,
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            // Bottom-aligned so the buttons stay anchored beside the last line
            // when the field grows to multiple lines.
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(TextFieldDefaults.MinHeight)
                        .background(c.surfaceAlt, RectangleShape)
                        .border(BorderStroke(1.dp, c.border), RectangleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = c.accent),
                            onClick = state.onAttachClick,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = attachContentDescription,
                    tint = c.accent,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = state.value,
                onValueChange = state.onChange,
                modifier = Modifier.weight(1f),
                textStyle = ZappTheme.typography.body,
                placeholder = {
                    BasicText(
                        text = state.placeholder.getValue(),
                        style = ZappTheme.typography.body.copy(color = c.textSubtle),
                    )
                },
                maxLines = 4,
                shape = RectangleShape,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = c.surfaceInput,
                        unfocusedContainerColor = c.surfaceInput,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = c.text,
                        unfocusedTextColor = c.text,
                        cursorColor = c.accent,
                    ),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier =
                    Modifier
                        .size(TextFieldDefaults.MinHeight)
                        .background(if (state.canSend) c.accent else c.surfaceAlt, RectangleShape)
                        .border(BorderStroke(1.dp, c.border), RectangleShape)
                        .clickable(
                            enabled = state.canSend,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = c.onAccent),
                            onClick = state.onSendClick,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = sendContentDescription,
                    tint = if (state.canSend) c.onAccent else c.textSubtle,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun ReplyPreviewStrip(
    senderName: String,
    content: String,
    onDismiss: () -> Unit,
) {
    val c = ZappTheme.colors

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.border),
    )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(c.surfaceAlt)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .background(c.accent),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
        ) {
            BasicText(
                text = stringResource(R.string.chat_room_reply_to_fmt, senderName),
                style = ZappTheme.typography.chip.copy(color = c.accent),
                maxLines = 1,
            )
            BasicText(
                text = content,
                style = ZappTheme.typography.caption.copy(color = c.textMuted),
                maxLines = 1,
            )
        }
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 16.dp, color = c.accent),
                        onClick = onDismiss,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.chat_room_reply_dismiss),
                tint = c.textSubtle,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
