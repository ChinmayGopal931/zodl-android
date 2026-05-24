package co.electriccoin.zcash.ui.screen.chat.model

import xyz.justzappit.zappmessaging.models.ZMContact
import xyz.justzappit.zappmessaging.models.ZMConversation
import xyz.justzappit.zappmessaging.models.ZMIdentity
import xyz.justzappit.zappmessaging.models.ZMMessage

enum class ConversationType {
    DIRECT,
    GROUP
}

data class ChatIdentity(
    val publicKey: String,
    val displayName: String
) {
    companion object {
        fun from(zmIdentity: ZMIdentity) =
            ChatIdentity(
                publicKey = zmIdentity.publicKey,
                displayName = zmIdentity.displayName
            )
    }
}

data class ChatConversation(
    val id: String,
    val type: ConversationType,
    val displayName: String,
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long? = null,
    val participantIds: List<String> = emptyList(),
    val isOwner: Boolean = false,
    val unreadCount: Int = 0
) {
    companion object {
        fun from(zmConv: ZMConversation) =
            ChatConversation(
                id = zmConv.id,
                type =
                    when (zmConv.type) {
                        xyz.justzappit.zappmessaging.models.ConversationType.GROUP -> ConversationType.GROUP
                        else -> ConversationType.DIRECT
                    },
                displayName = zmConv.displayName,
                lastMessage = zmConv.lastMessage,
                lastMessageTimestamp = zmConv.lastMessageTimestamp,
                participantIds = zmConv.participantIds,
                isOwner = zmConv.isOwner ?: false,
                unreadCount = zmConv.unreadCount ?: 0
            )
    }
}

enum class MessageStatus {
    SENDING,
    SENT,
    QUEUED,
    FAILED
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val content: String,
    val contentType: String? = "text/plain",
    val senderName: String? = null,
    val isFromMe: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val mediaId: String? = null,
    val mediaSize: Int? = null,
    val mediaWidth: Int? = null,
    val mediaHeight: Int? = null,
    val thumbnailData: String? = null,
    val mediaLocalPath: String? = null,
    val mediaTransferState: String? = null,
    val status: MessageStatus? = null,
    val replyToId: String? = null,
    val replyToSenderName: String? = null,
    val replyToContent: String? = null
) {
    companion object {
        fun from(zmMsg: ZMMessage) =
            ChatMessage(
                id = zmMsg.id,
                conversationId = zmMsg.conversationId,
                content = zmMsg.content,
                contentType = zmMsg.contentType,
                senderName = zmMsg.senderName,
                isFromMe = zmMsg.isFromMe,
                timestamp = zmMsg.timestamp,
                mediaId = zmMsg.mediaId,
                mediaSize = zmMsg.mediaSize,
                mediaWidth = zmMsg.mediaWidth,
                mediaHeight = zmMsg.mediaHeight,
                thumbnailData = zmMsg.thumbnailData,
                mediaLocalPath = zmMsg.mediaLocalPath,
                mediaTransferState = zmMsg.mediaTransferState?.name?.lowercase(),
                status = if (zmMsg.isFromMe) MessageStatus.SENT else null,
                // TODO: restore reply fields once zappMessaging ZMMessage exposes replyTo* (not in
                // the currently-pinned SHA in .zapp-deps). Defaults are null, so unwired for now.
            )
    }
}

data class ChatContact(
    val publicKey: String,
    val name: String,
    val walletAddress: String? = null
) {
    companion object {
        fun from(zmContact: ZMContact) =
            ChatContact(
                publicKey = zmContact.publicKey,
                name = zmContact.name,
                walletAddress = zmContact.walletAddress
            )
    }
}
