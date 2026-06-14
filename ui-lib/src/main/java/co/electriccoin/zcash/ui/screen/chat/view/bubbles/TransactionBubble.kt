package co.electriccoin.zcash.ui.screen.chat.view.bubbles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun TransactionBubble(message: ChatMessage, isFromMe: Boolean) {
    val parsed =
        try {
            JSONObject(message.content)
        } catch (_: Exception) {
            null
        }
    val amount = parsed?.optDouble("amount", 0.0) ?: 0.0
    val signature = parsed?.optString("signature", "")?.takeIf { it.isNotEmpty() }

    Surface(
        shape = RectangleShape,
        color = ZappTheme.colors.accent.copy(alpha = 0.1f),
        modifier = Modifier.widthIn(max = 280.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ZappTheme.colors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text =
                        if (isFromMe) {
                            stringResource(R.string.chat_bubble_transaction_sent)
                        } else {
                            stringResource(R.string.chat_bubble_transaction_received)
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = ZappTheme.colors.accent
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$amount ZEC",
                style = MaterialTheme.typography.titleMedium,
                color = ZappTheme.colors.text
            )
            signature?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.chat_bubble_transaction_signature_fmt, it.take(8), it.takeLast(4)),
                    style = MaterialTheme.typography.labelSmall,
                    color = ZappTheme.colors.textMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = ZappTheme.colors.textMuted
            )
        }
    }
}
