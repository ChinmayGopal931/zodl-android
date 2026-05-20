package co.electriccoin.zcash.ui.screen.chat.view.bubbles

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaBubble(message: ChatMessage, isFromMe: Boolean) {
    val isVideo = message.contentType?.startsWith("video/") == true
    val isSending = message.mediaTransferState == "sending"

    val imageModel: Any? = remember(message.mediaLocalPath, message.thumbnailData) {
        when {
            message.mediaLocalPath != null -> File(message.mediaLocalPath)
            message.thumbnailData != null -> try {
                val bytes = Base64.decode(message.thumbnailData, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { null }
            else -> null
        }
    }

    val shape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = if (isFromMe) 0.dp else 4.dp,
        bottomEnd = if (isFromMe) 4.dp else 0.dp
    )

    Surface(
        shape = shape,
        color = if (isFromMe) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.widthIn(max = 240.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 300.dp)
                    .clip(RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Media",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isVideo) "Video" else "Image",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isVideo && !isSending) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Play video",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }

                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFromMe) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
