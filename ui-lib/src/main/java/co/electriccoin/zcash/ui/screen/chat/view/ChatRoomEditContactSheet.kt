package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.room.ChatRoomEditContactSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactEditSheet(state: ChatRoomEditContactSheetState) {
    val c = ZappTheme.colors
    var nameInput by remember(state.currentName) { mutableStateOf(TextFieldValue(state.currentName)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = state.onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        shape = RectangleShape,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText(
                text = stringResource(R.string.chat_room_edit_contact_title),
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
            )
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text(stringResource(R.string.chat_room_edit_contact_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ContactEditButton(
                    label = stringResource(R.string.chat_room_edit_contact_cancel),
                    background = c.surfaceAlt,
                    textColor = c.text,
                    onClick = state.onDismiss,
                    modifier = Modifier.weight(1f),
                )
                val canSave = nameInput.text.isNotBlank()
                ContactEditButton(
                    label = stringResource(R.string.chat_room_edit_contact_save),
                    background = if (canSave) c.accent else c.surfaceAlt,
                    textColor = if (canSave) c.onAccent else c.textMuted,
                    enabled = canSave,
                    onClick = { state.onSave(nameInput.text.trim()) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(c.border),
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ContactEditButton(
                    label = stringResource(R.string.chat_room_edit_contact_report),
                    background = c.surfaceAlt,
                    textColor = c.text,
                    onClick = state.onReport,
                    modifier = Modifier.weight(1f),
                )
                ContactEditButton(
                    label = stringResource(R.string.chat_room_edit_contact_block),
                    background = c.danger.copy(alpha = DANGER_BG_ALPHA),
                    textColor = c.danger,
                    onClick = state.onBlock,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ContactEditButton(
    label: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .height(48.dp)
                .background(background, RectangleShape)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = ZappTheme.typography.button.copy(color = textColor, fontWeight = FontWeight.Black),
        )
    }
}

private const val DANGER_BG_ALPHA = 0.1f
