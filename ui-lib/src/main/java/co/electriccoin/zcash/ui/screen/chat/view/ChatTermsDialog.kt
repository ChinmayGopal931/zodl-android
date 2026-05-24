package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.theme.ZappTheme

/**
 * One-time Terms of Service and Community Guidelines acceptance dialog
 * shown before the user's first chat interaction. Required by Google Play
 * UGC policy for messaging apps.
 */
@Composable
fun ChatTermsDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val c = ZappTheme.colors
    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = c.surface,
        title = {
            Text(
                "Messaging Terms",
                style = ZappTheme.typography.sectionTitle,
                color = c.text,
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Before using Zapp messaging, please review and accept our community guidelines:",
                    style = ZappTheme.typography.body,
                    color = c.text,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Community Guidelines",
                    style = ZappTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                    color = c.text,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    buildString {
                        appendLine("By using Zapp messaging, you agree to:")
                        appendLine()
                        appendLine("1. Not send spam, unsolicited messages, or engage in harassment.")
                        appendLine()
                        appendLine(
                            "2. Not share illegal content, including but not limited to child exploitation material, illegal substances, or stolen financial information."
                        )
                        appendLine()
                        appendLine("3. Not impersonate others or use the messaging feature for scams or fraud.")
                        appendLine()
                        appendLine("4. Not use the messaging feature to facilitate illegal activities.")
                        appendLine()
                        appendLine("5. Respect other users' privacy and not share their information without consent.")
                        appendLine()
                        appendLine(
                            "Users who violate these guidelines may be blocked by other users. You can block or report any user from the conversation settings menu."
                        )
                        appendLine()
                        appendLine(
                            "Messages are sent peer-to-peer and are end-to-end encrypted. Zapp does not store or have access to your message content."
                        )
                    },
                    style = ZappTheme.typography.caption,
                    color = c.textMuted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("I Accept", color = c.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Decline", color = c.textMuted)
            }
        },
    )
}
