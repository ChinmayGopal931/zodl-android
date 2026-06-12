package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.model.ReportCategory

@Composable
internal fun BlockUserDialog(
    displayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = ZappTheme.colors
    val target = displayName.ifBlank { stringResource(R.string.chat_moderation_fallback_user_name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Text(
                stringResource(R.string.chat_block_dialog_title),
                style = ZappTheme.typography.sectionTitle,
                color = c.text,
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.chat_block_dialog_message_fmt, target),
                    style = ZappTheme.typography.body,
                    color = c.text,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.chat_block_dialog_explanation),
                    style = ZappTheme.typography.caption,
                    color = c.textMuted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.chat_block_dialog_confirm),
                    color = ZappTheme.colors.danger,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.chat_block_dialog_cancel), color = c.textMuted)
            }
        },
    )
}

@Composable
internal fun UnblockUserDialog(
    displayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = ZappTheme.colors
    val target = displayName.ifBlank { stringResource(R.string.chat_moderation_fallback_user_name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Text(
                stringResource(R.string.chat_unblock_dialog_title),
                style = ZappTheme.typography.sectionTitle,
                color = c.text,
            )
        },
        text = {
            Text(
                stringResource(R.string.chat_unblock_dialog_message_fmt, target),
                style = ZappTheme.typography.body,
                color = c.text,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.chat_unblock_dialog_confirm),
                    color = c.accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.chat_unblock_dialog_cancel), color = c.textMuted)
            }
        },
    )
}

@Composable
internal fun ReportUserDialog(
    displayName: String,
    onSubmit: (category: ReportCategory, details: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = ZappTheme.colors
    val target = displayName.ifBlank { stringResource(R.string.chat_moderation_fallback_user_name) }
    var selectedCategory by remember { mutableStateOf<ReportCategory?>(null) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Text(
                stringResource(R.string.chat_report_dialog_title),
                style = ZappTheme.typography.sectionTitle,
                color = c.text,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.chat_report_dialog_message_fmt, target),
                    style = ZappTheme.typography.body,
                    color = c.text,
                )
                Spacer(modifier = Modifier.height(12.dp))

                ReportCategory.entries.forEach { category ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = category }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            colors =
                                RadioButtonDefaults.colors(
                                    selectedColor = c.accent,
                                    unselectedColor = c.textMuted,
                                ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(category.displayNameRes),
                            style = ZappTheme.typography.body,
                            color = c.text,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text(stringResource(R.string.chat_report_dialog_details_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCategory?.let { onSubmit(it, details) }
                },
                enabled = selectedCategory != null,
            ) {
                Text(
                    stringResource(R.string.chat_report_dialog_submit),
                    color = if (selectedCategory != null) c.danger else c.textMuted,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.chat_report_dialog_cancel), color = c.textMuted)
            }
        },
    )
}

@Composable
internal fun ReportAndBlockDialog(
    displayName: String,
    onReport: (category: ReportCategory, details: String) -> Unit,
    onBlock: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = ZappTheme.colors
    val target = displayName.ifBlank { stringResource(R.string.chat_moderation_fallback_user_name) }
    var step by remember { mutableStateOf(0) }

    if (step == 0) {
        ReportUserDialog(
            displayName = displayName,
            onSubmit = { category, details ->
                onReport(category, details)
                step = 1
            },
            onDismiss = onDismiss,
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = c.surface,
            title = {
                Text(
                    stringResource(R.string.chat_report_followup_title),
                    style = ZappTheme.typography.sectionTitle,
                    color = c.text,
                )
            },
            text = {
                Text(
                    stringResource(R.string.chat_report_followup_message_fmt, target),
                    style = ZappTheme.typography.body,
                    color = c.text,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onBlock()
                    onDismiss()
                }) {
                    Text(
                        stringResource(R.string.chat_report_followup_confirm),
                        color = c.danger,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.chat_report_followup_dismiss), color = c.textMuted)
                }
            },
        )
    }
}
