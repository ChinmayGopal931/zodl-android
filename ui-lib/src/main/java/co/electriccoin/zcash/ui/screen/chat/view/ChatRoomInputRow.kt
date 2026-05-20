package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.screen.chat.room.ChatRoomInputState

@Composable
internal fun InputRow(state: ChatRoomInputState) {
    val c = ZappTheme.colors
    val attachContentDescription = state.attachContentDescription.getValue()
    val sendContentDescription = state.sendContentDescription.getValue()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(c.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
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
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        TextField(
            value = state.value,
            onValueChange = state.onChange,
            modifier =
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 36.dp),
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
                    .size(36.dp)
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
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
