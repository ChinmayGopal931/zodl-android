package co.electriccoin.zcash.ui.common.provider

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import co.electriccoin.zcash.ui.MainActivity
import co.electriccoin.zcash.ui.R

/** Intent extra carrying the conversation id that a notification tap should open. */
const val CHAT_CONVERSATION_ID_EXTRA = "chat_conversation_id"

/**
 * Posts local notifications for incoming chat messages. This is the single
 * presentation seam reused by both the foreground path and the later push wake;
 * suppression (own messages, on-screen conversation, blocked sender, user toggle)
 * is decided by the caller, not here.
 */
interface ChatNotifier {
    fun post(
        conversationId: String,
        conversationName: String?,
        senderName: String?,
        content: String,
    )
}

class ChatNotifierImpl(
    private val context: Context,
) : ChatNotifier {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.chat_notifications_channel_name))
                .setDescription(context.getString(R.string.chat_notifications_channel_description))
                .build(),
        )
    }

    // areNotificationsEnabled() is the real guard (false on 13+ when POST_NOTIFICATIONS
    // is denied or the channel is off); lint can't see it as a permission check.
    @SuppressLint("MissingPermission")
    override fun post(
        conversationId: String,
        conversationName: String?,
        senderName: String?,
        content: String,
    ) {
        if (!notificationManager.areNotificationsEnabled()) return

        val title =
            conversationName
                ?: senderName
                ?: context.getString(R.string.chat_notifications_fallback_title)
        val body = content.ifEmpty { context.getString(R.string.chat_notifications_media_body) }

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_chat)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent(conversationId))
                .build()

        // Tag by conversationId (collision-free, unlike an int id) so repeat messages
        // from one conversation collapse onto one notification and never overwrite another's.
        notificationManager.notify(conversationId, NOTIFICATION_ID, notification)
    }

    private fun contentIntent(conversationId: String): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(CHAT_CONVERSATION_ID_EXTRA, conversationId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        // Per-conversation requestCode keeps each conversation's PendingIntent (and its
        // extra) distinct, so simultaneous notifications each route to the right room.
        return PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val CHANNEL_ID = "chat_messages"
        const val NOTIFICATION_ID = 1
    }
}
