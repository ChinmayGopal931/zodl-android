package co.electriccoin.zcash.ui.common.provider

import java.util.concurrent.atomic.AtomicReference

/**
 * Process-wide latch that records when a ZEC send originates from a chat
 * conversation. The chat room sets the conversation id before navigating to
 * the send flow; the submit-proposal use case consumes it after a successful
 * submission to send an auto-notification message back to the peer.
 */
class ChatSendContextProvider {
    private val ref = AtomicReference<String?>(null)

    fun set(conversationId: String) {
        ref.set(conversationId)
    }

    fun clear() {
        ref.set(null)
    }

    fun consume(): String? = ref.getAndSet(null)
}
