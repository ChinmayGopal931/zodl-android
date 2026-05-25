package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.FileBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.LocationBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.MediaBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.PaymentRequestBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.TransactionBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.WalletAddressBubble
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun MessageBubble(
    message: ChatMessage,
    onReplyToMessage: ((ChatMessage) -> Unit)? = null,
    onImageClick: ((ChatMessage) -> Unit)? = null,
) {
    val c = ZappTheme.colors
    val isFromMe = message.isFromMe
    val contentType = resolveContentType(message)
    val haptic = LocalHapticFeedback.current

    var offsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 80f

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        if ((offsetX > SWIPE_ICON_APPEAR_THRESHOLD && !isFromMe) || (offsetX < -SWIPE_ICON_APPEAR_THRESHOLD && isFromMe)) {
            Icon(
                Icons.AutoMirrored.Filled.Reply,
                contentDescription = stringResource(R.string.chat_room_reply_action),
                tint = c.textSubtle,
                modifier =
                    Modifier
                        .align(if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart)
                        .padding(horizontal = 8.dp)
                        .size(20.dp),
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .pointerInput(onReplyToMessage) {
                        if (onReplyToMessage == null) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if ((isFromMe && offsetX < -swipeThreshold) ||
                                    (!isFromMe && offsetX > swipeThreshold)
                                ) {
                                    runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                    onReplyToMessage(message)
                                }
                                offsetX = 0f
                            },
                            onDragCancel = { offsetX = 0f },
                        ) { _, dragAmount ->
                            val newOffset = offsetX + dragAmount
                            offsetX =
                                if (isFromMe) {
                                    newOffset.coerceIn(-SWIPE_MAX_OFFSET, 0f)
                                } else {
                                    newOffset.coerceIn(0f, SWIPE_MAX_OFFSET)
                                }
                        }
                    },
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
        ) {
            if (!isFromMe && message.senderName != null) {
                BasicText(
                    text = message.senderName,
                    style = ZappTheme.typography.chip.copy(color = c.accent),
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
            }

            if (message.replyToId != null) {
                Box(
                    modifier =
                        Modifier
                            .widthIn(max = 280.dp)
                            .background(if (isFromMe) c.accent else c.surfaceAlt, RectangleShape)
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                ) {
                    QuotedReplyBlock(
                        senderName = message.replyToSenderName,
                        content = message.replyToContent,
                        isFromMe = isFromMe,
                    )
                }
            }

            when {
                contentType == CONTENT_TYPE_PAYMENT_REQUEST -> {
                    PaymentRequestBubble(message = message, isFromMe = isFromMe)
                }

                contentType == CONTENT_TYPE_WALLET_ADDRESS -> {
                    WalletAddressBubble(message = message, isFromMe = isFromMe)
                }

                contentType == CONTENT_TYPE_ZEC_TRANSACTION -> {
                    TransactionBubble(message = message, isFromMe = isFromMe)
                }

                contentType == CONTENT_TYPE_LOCATION -> {
                    LocationBubble(message = message, isFromMe = isFromMe)
                }

                contentType.startsWith(IMAGE_MIME_PREFIX) -> {
                    MediaBubble(message = message, isFromMe = isFromMe, onImageClick = onImageClick)
                }

                contentType.startsWith(VIDEO_MIME_PREFIX) -> {
                    MediaBubble(message = message, isFromMe = isFromMe)
                }

                message.mediaId != null -> {
                    FileBubble(message = message, isFromMe = isFromMe)
                }

                else -> {
                    TextMessageBubble(message = message, isFromMe = isFromMe)
                }
            }
        }
    }
}

private fun resolveContentType(message: ChatMessage): String {
    val declared = message.contentType
    if (!declared.isNullOrEmpty() && declared != CONTENT_TYPE_TEXT_PLAIN) return declared
    return try {
        JSONObject(message.content).optString("contentType", "").takeIf { it.isNotEmpty() }
    } catch (_: JSONException) {
        null
    } ?: CONTENT_TYPE_TEXT_PLAIN
}

@Composable
private fun TextMessageBubble(message: ChatMessage, isFromMe: Boolean) {
    val c = ZappTheme.colors
    Box(
        modifier =
            Modifier
                .widthIn(max = 280.dp)
                .background(if (isFromMe) c.accent else c.surfaceAlt, RectangleShape)
                .padding(12.dp),
    ) {
        Column {
            BasicText(
                text = message.content,
                style =
                    ZappTheme.typography.body.copy(
                        color = if (isFromMe) c.onAccent else c.text,
                    ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            BasicText(
                text = formatMessageTime(message.timestamp),
                style =
                    ZappTheme.typography.caption.copy(
                        color = if (isFromMe) c.onAccent.copy(alpha = OUTGOING_META_ALPHA) else c.textMuted,
                    ),
            )
        }
    }
}

private fun formatMessageTime(epochMillis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))

@Composable
private fun QuotedReplyBlock(
    senderName: String?,
    content: String?,
    isFromMe: Boolean,
) {
    val c = ZappTheme.colors
    val barColor = if (isFromMe) c.onAccent.copy(alpha = 0.5f) else c.accent
    val nameColor = if (isFromMe) c.onAccent else c.accent
    val textColor = if (isFromMe) c.onAccent.copy(alpha = OUTGOING_META_ALPHA) else c.textMuted

    Row {
        Box(
            modifier =
                Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .background(barColor),
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            BasicText(
                text = senderName ?: stringResource(R.string.chat_room_reply_unknown_sender),
                style = ZappTheme.typography.chip.copy(color = nameColor),
                maxLines = 1,
            )
            BasicText(
                text = content ?: "",
                style = ZappTheme.typography.caption.copy(color = textColor),
                maxLines = 1,
            )
        }
    }
}

private const val CONTENT_TYPE_TEXT_PLAIN = "text/plain"
private const val CONTENT_TYPE_PAYMENT_REQUEST = "application/payment-request"
private const val CONTENT_TYPE_WALLET_ADDRESS = "application/wallet-address"
private const val CONTENT_TYPE_ZEC_TRANSACTION = "application/zec-transaction"
private const val CONTENT_TYPE_LOCATION = "application/location"
private const val IMAGE_MIME_PREFIX = "image/"
private const val VIDEO_MIME_PREFIX = "video/"
private const val OUTGOING_META_ALPHA = 0.7f
private const val SWIPE_MAX_OFFSET = 120f
private const val SWIPE_ICON_APPEAR_THRESHOLD = 20f
