package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
import co.electriccoin.zcash.ui.screen.chat.model.MessageStatus
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.FileBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.LocationBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.MediaBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.PaymentRequestBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.TransactionBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.WalletAddressBubble
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

@Composable
internal fun MessageBubble(message: ChatMessage) {
    val c = ZappTheme.colors
    val isFromMe = message.isFromMe
    val contentType = resolveContentType(message)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
    ) {
        if (!isFromMe && message.senderName != null) {
            BasicText(
                text = message.senderName,
                style = ZappTheme.typography.chip.copy(color = c.accent),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
        }

        when {
            contentType == CONTENT_TYPE_PAYMENT_REQUEST ->
                PaymentRequestBubble(message = message, isFromMe = isFromMe)
            contentType == CONTENT_TYPE_WALLET_ADDRESS ->
                WalletAddressBubble(message = message, isFromMe = isFromMe)
            contentType == CONTENT_TYPE_ZEC_TRANSACTION ->
                TransactionBubble(message = message, isFromMe = isFromMe)
            contentType == CONTENT_TYPE_LOCATION ->
                LocationBubble(message = message, isFromMe = isFromMe)
            contentType.startsWith(IMAGE_MIME_PREFIX) ->
                MediaBubble(message = message, isFromMe = isFromMe)
            contentType.startsWith(VIDEO_MIME_PREFIX) ->
                MediaBubble(message = message, isFromMe = isFromMe)
            message.mediaId != null ->
                FileBubble(message = message, isFromMe = isFromMe)
            else -> TextMessageBubble(message = message, isFromMe = isFromMe)
        }
    }
}

@Suppress("TooGenericExceptionCaught")
private fun resolveContentType(message: ChatMessage): String {
    val declared = message.contentType
    if (!declared.isNullOrEmpty() && declared != CONTENT_TYPE_TEXT_PLAIN) return declared
    return try {
        JSONObject(message.content).optString("contentType", "").takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BasicText(
                    text = formatMessageTime(message.timestamp),
                    style =
                        ZappTheme.typography.caption.copy(
                            color = if (isFromMe) c.onAccent.copy(alpha = OUTGOING_META_ALPHA) else c.textMuted,
                        ),
                )
                if (isFromMe) {
                    DeliveryIndicator(
                        status = message.status,
                        tintColor = c.onAccent.copy(alpha = OUTGOING_META_ALPHA),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryIndicator(status: MessageStatus?, tintColor: Color) {
    val c = ZappTheme.colors
    when (status) {
        MessageStatus.SENT ->
            Icon(
                Icons.Default.Done,
                contentDescription = stringResource(R.string.chat_room_delivery_sent),
                modifier = Modifier.size(13.dp),
                tint = tintColor,
            )
        MessageStatus.QUEUED ->
            Icon(
                Icons.Default.Schedule,
                contentDescription = stringResource(R.string.chat_room_delivery_queued),
                modifier = Modifier.size(13.dp),
                tint = tintColor,
            )
        MessageStatus.FAILED ->
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = stringResource(R.string.chat_room_delivery_failed),
                modifier = Modifier.size(13.dp),
                tint = c.danger,
            )
        MessageStatus.SENDING ->
            Icon(
                Icons.Default.MoreHoriz,
                contentDescription = stringResource(R.string.chat_room_delivery_sending),
                modifier = Modifier.size(13.dp),
                tint = tintColor,
            )
        null -> {}
    }
}

private fun formatMessageTime(epochMillis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))

private const val CONTENT_TYPE_TEXT_PLAIN = "text/plain"
private const val CONTENT_TYPE_PAYMENT_REQUEST = "application/payment-request"
private const val CONTENT_TYPE_WALLET_ADDRESS = "application/wallet-address"
private const val CONTENT_TYPE_ZEC_TRANSACTION = "application/zec-transaction"
private const val CONTENT_TYPE_LOCATION = "application/location"
private const val IMAGE_MIME_PREFIX = "image/"
private const val VIDEO_MIME_PREFIX = "video/"
private const val OUTGOING_META_ALPHA = 0.7f
