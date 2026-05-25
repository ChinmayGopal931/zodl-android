package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.zapp.ZappBottomActionBar
import co.electriccoin.zcash.ui.design.component.zapp.ZappButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.screen.chat.contactedit.ContactEditDeleteDialogState
import co.electriccoin.zcash.ui.screen.chat.contactedit.ContactEditState

@Composable
internal fun ContactEditView(state: ContactEditState, modifier: Modifier = Modifier) {
    val c = ZappTheme.colors

    Scaffold(
        modifier = modifier,
        topBar = {
            ZappScreenHeader(
                title = state.title.getValue(),
                right = {
                    IconButton(onClick = state.onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription =
                                stringResource(R.string.chat_contact_edit_delete_content_description),
                            tint = c.danger,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
        },
        bottomBar = {
            ZappBottomActionBar(
                onBack = state.onBack,
                primaryAction = {
                    ZappButton(
                        text = stringResource(R.string.chat_contact_edit_save),
                        enabled = state.canSave,
                        onClick = state.onSave,
                    )
                },
            )
        },
        containerColor = c.bg,
    ) { paddingValues ->
        if (!state.isContactFound) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
            ) {
                BasicText(
                    text = stringResource(R.string.chat_contact_edit_not_found),
                    style = ZappTheme.typography.body.copy(color = c.textMuted),
                )
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PublicKeyCard(publicKey = state.publicKey)

                OutlinedTextField(
                    value = state.nameInput,
                    onValueChange = state.onNameChange,
                    label = { Text(stringResource(R.string.chat_contact_edit_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    state.deleteDialog?.let { DeleteContactDialog(state = it) }
}

@Composable
private fun PublicKeyCard(publicKey: String) {
    val c = ZappTheme.colors
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(c.surfaceAlt, RectangleShape)
                .border(BorderStroke(1.dp, c.border), RectangleShape)
                .padding(16.dp),
    ) {
        Column {
            BasicText(
                text = stringResource(R.string.chat_contact_edit_public_key_label),
                style = ZappTheme.typography.caption.copy(color = c.textMuted),
            )
            Spacer(modifier = Modifier.height(4.dp))
            BasicText(
                text = publicKey,
                style = ZappTheme.typography.mono.copy(color = c.text),
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun DeleteContactDialog(state: ContactEditDeleteDialogState) {
    AlertDialog(
        onDismissRequest = state.onDismiss,
        title = { Text(stringResource(R.string.chat_contact_edit_delete_dialog_title)) },
        text = { Text(stringResource(R.string.chat_contact_edit_delete_dialog_message)) },
        confirmButton = {
            TextButton(onClick = state.onConfirm) {
                Text(stringResource(R.string.chat_contact_edit_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = state.onDismiss) {
                Text(stringResource(R.string.chat_contact_edit_delete_dialog_cancel))
            }
        },
    )
}
