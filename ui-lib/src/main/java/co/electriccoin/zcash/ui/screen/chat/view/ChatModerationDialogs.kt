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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.model.ReportCategory

/**
 * Confirmation dialog for blocking a user.
 */
@Composable
fun BlockUserDialog(
    displayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = ZappTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Text(
                "Block User",
                style = ZappTheme.typography.sectionTitle,
                color = c.text,
            )
        },
        text = {
            Column {
                Text(
                    "Block ${displayName.ifBlank { "this user" }}?",
                    style = ZappTheme.typography.body,
                    color = c.text,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "You will no longer receive messages from this user. " +
                        "They won't be notified that they've been blocked.",
                    style = ZappTheme.typography.caption,
                    color = c.textMuted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Block", color = ZappTheme.colors.danger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = c.textMuted)
            }
        },
    )
}

/**
 * Confirmation dialog for unblocking a user.
 */
@Composable
fun UnblockUserDialog(
    displayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = ZappTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Text(
                "Unblock User",
                style = ZappTheme.typography.sectionTitle,
                color = c.text,
            )
        },
        text = {
            Text(
                "Unblock ${displayName.ifBlank { "this user" }}? " +
                    "You will be able to receive messages from them again.",
                style = ZappTheme.typography.body,
                color = c.text,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Unblock", color = c.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = c.textMuted)
            }
        },
    )
}

/**
 * Report dialog with category selection and optional details.
 */
@Composable
fun ReportUserDialog(
    displayName: String,
    onSubmit: (category: ReportCategory, details: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = ZappTheme.colors
    var selectedCategory by remember { mutableStateOf<ReportCategory?>(null) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Text(
                "Report User",
                style = ZappTheme.typography.sectionTitle,
                color = c.text,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Why are you reporting ${displayName.ifBlank { "this user" }}?",
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
                            category.displayLabel,
                            style = ZappTheme.typography.body,
                            color = c.text,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Additional details (optional)") },
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
                    "Submit Report",
                    color = if (selectedCategory != null) c.danger else c.textMuted,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = c.textMuted)
            }
        },
    )
}

/**
 * Combined block + report dialog — offers to block after reporting.
 */
@Composable
fun ReportAndBlockDialog(
    displayName: String,
    onReport: (category: ReportCategory, details: String) -> Unit,
    onBlock: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = ZappTheme.colors
    var step by remember { mutableStateOf(0) } // 0 = report, 1 = offer block

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
                    "Report Submitted",
                    style = ZappTheme.typography.sectionTitle,
                    color = c.text,
                )
            },
            text = {
                Text(
                    "Your report has been recorded. Would you also like to block " +
                        "${displayName.ifBlank { "this user" }} to stop receiving their messages?",
                    style = ZappTheme.typography.body,
                    color = c.text,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onBlock()
                    onDismiss()
                }) {
                    Text("Block User", color = c.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("No Thanks", color = c.textMuted)
                }
            },
        )
    }
}
