package co.electriccoin.zcash.ui.screen.chat.view.bubbles

import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun MediaBubble(
    message: ChatMessage,
    isFromMe: Boolean,
    onImageClick: ((ChatMessage) -> Unit)? = null,
) {
    val isVideo = message.contentType?.startsWith("video/") == true
    val isGif = message.contentType == "image/gif"
    val isSending = message.mediaTransferState == "sending"

    val context = LocalContext.current
    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val hasLocalFile = message.mediaLocalPath != null &&
        File(message.mediaLocalPath).exists()

    val imageModel: Any? = remember(message.mediaLocalPath, message.thumbnailData) {
        when {
            hasLocalFile -> ImageRequest.Builder(context)
                .data(File(message.mediaLocalPath))
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build()
            message.thumbnailData != null -> try {
                val bytes = Base64.decode(message.thumbnailData, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { null }
            else -> null
        }
    }

    val aspectRatio = remember(message.mediaWidth, message.mediaHeight) {
        val w = message.mediaWidth
        val h = message.mediaHeight
        if (w != null && h != null && w > 0 && h > 0) {
            w.toFloat() / h.toFloat()
        } else {
            null
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
        color = if (isFromMe) ZappTheme.colors.accent
        else ZappTheme.colors.surfaceAlt,
        modifier = Modifier.widthIn(max = 260.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (aspectRatio != null) {
                            Modifier.aspectRatio(aspectRatio.coerceIn(MIN_ASPECT, MAX_ASPECT))
                        } else {
                            Modifier.heightIn(min = 120.dp, max = 300.dp)
                        }
                    )
                    .clip(RectangleShape)
                    .then(
                        if (onImageClick != null && !isVideo && !isSending) {
                            Modifier.clickable { onImageClick(message) }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        imageLoader = if (isGif) gifImageLoader else ImageLoader(context),
                        contentDescription = when {
                            isGif -> stringResource(R.string.chat_room_media_content_description_gif)
                            isVideo -> stringResource(R.string.chat_room_media_content_description_video)
                            else -> stringResource(R.string.chat_room_media_content_description_image)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = if (hasLocalFile) ContentScale.Fit else ContentScale.Fit,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isVideo) {
                                stringResource(R.string.chat_room_media_placeholder_video)
                            } else {
                                stringResource(R.string.chat_room_media_placeholder_image)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFromMe) ZappTheme.colors.onAccent.copy(alpha = 0.5f)
                            else ZappTheme.colors.textMuted
                        )
                    }
                }

                if (isVideo && !isSending) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = stringResource(R.string.chat_room_media_play_video),
                        modifier = Modifier.size(48.dp),
                        tint = ZappTheme.colors.onAccent.copy(alpha = 0.85f)
                    )
                }

                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = ZappTheme.colors.onAccent,
                        strokeWidth = 2.dp
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFromMe) ZappTheme.colors.onAccent
                        else ZappTheme.colors.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFromMe) ZappTheme.colors.onAccent.copy(alpha = 0.7f)
                    else ZappTheme.colors.textMuted
                )
            }
        }
    }
}

private const val MIN_ASPECT = 0.4f
private const val MAX_ASPECT = 2.5f
