package co.electriccoin.zcash.ui.common.usecase

/**
 * Singleton that tracks when a ZEC send originates from a chat conversation.
 * Set before navigating to the send flow; consumed after successful submission
 * to send an auto-notification message back to the chat peer.
 */
class ChatSendContext {
    private val ref = java.util.concurrent.atomic.AtomicReference<String?>(null)

    fun set(conversationId: String) {
        ref.set(conversationId)
    }

    fun clear() {
        ref.set(null)
    }

    fun consume(): String? = ref.getAndSet(null)
}
