package co.electriccoin.zcash.ui.screen.chat.view.bubbles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.media.FileUtils
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileBubble(message: ChatMessage, isFromMe: Boolean) {
    val fileName = message.content.takeIf { it.isNotBlank() } ?: "File"
    val fileSize = message.mediaSize?.toLong()

    Surface(
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = if (isFromMe) 0.dp else 4.dp,
            bottomEnd = if (isFromMe) 4.dp else 0.dp
        ),
        color = if (isFromMe) ZappTheme.colors.accent
        else ZappTheme.colors.surfaceAlt,
        modifier = Modifier.widthIn(max = 280.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = if (isFromMe) ZappTheme.colors.onAccent
                    else ZappTheme.colors.accent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFromMe) ZappTheme.colors.onAccent
                        else ZappTheme.colors.text,
                        maxLines = 2
                    )
                    if (fileSize != null && fileSize > 0) {
                        Text(
                            text = FileUtils.formatFileSize(fileSize),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFromMe) ZappTheme.colors.onAccent.copy(alpha = 0.7f)
                            else ZappTheme.colors.textMuted
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = if (isFromMe) ZappTheme.colors.onAccent.copy(alpha = 0.7f)
                else ZappTheme.colors.textMuted
            )
        }
    }
}
