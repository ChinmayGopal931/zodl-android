package co.electriccoin.zcash.ui.screen.chat.repository

import co.electriccoin.zcash.ui.screen.chat.model.ChatConversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for the conversation list and global connection state, owning the
 * one cache that both the list and room screens read. All conversation-level SDK events
 * (incoming message, invite, group rename/delete, member add/leave) are reduced here exactly
 * once, with the moderation block filter applied once, so the two view models can no longer
 * drift their caches or duplicate the reducer logic.
 */
interface ChatConversationsRepository {
    /** The conversation cache. `null` until the first refresh completes (loading state). */
    val conversations: StateFlow<List<ChatConversation>?>

    /** Local identity public key, or `null` before the chat identity has derived. */
    val localPublicKey: StateFlow<String?>

    val isOnline: StateFlow<Boolean>
    val peerCount: StateFlow<Int>
    val dhtHealth: StateFlow<String>

    /** Conversation ids removed upstream (group deleted) — the room uses this to navigate back. */
    val conversationDeleted: SharedFlow<String>

    /** Re-pulls the conversation list from the SDK and replaces the cache. */
    suspend fun refresh()

    /** Zeroes the unread count of [conversationId] in the cache. */
    fun markConversationRead(conversationId: String)

    /**
     * Marks [conversationId] as the on-screen room (or `null` when none) so incoming messages
     * for the open conversation don't inflate its unread count.
     */
    fun setActiveConversation(conversationId: String?)

    /** Applies a local display-name change to the cached conversation (e.g. contact rename). */
    fun renameConversation(conversationId: String, newName: String)

    /** Leaves [conversationId] upstream and removes it from the cache. */
    suspend fun leaveConversation(conversationId: String)

    /** A single cached conversation, kept live as the cache reduces events. */
    fun conversation(conversationId: String): Flow<ChatConversation?>

    companion object {
        /** Placeholder stored as a conversation's last message when it is a media attachment. */
        const val MEDIA_PLACEHOLDER_SENTINEL = "[Media]"
    }
}
