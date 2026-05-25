package co.electriccoin.zcash.ui.screen.chat.repository

import co.electriccoin.zcash.ui.screen.chat.model.BlockedUser
import co.electriccoin.zcash.ui.screen.chat.model.ContentReport
import co.electriccoin.zcash.ui.screen.chat.model.ReportCategory
import kotlinx.coroutines.flow.StateFlow

interface ChatModerationRepository {
    val blockedUsers: StateFlow<Set<BlockedUser>>
    val blockedKeys: StateFlow<Set<String>>
    val reports: StateFlow<List<ContentReport>>

    fun isBlocked(publicKey: String): Boolean

    fun blockUser(publicKey: String, displayName: String?)

    fun unblockUser(publicKey: String)

    fun submitReport(
        reportedPublicKey: String,
        reportedDisplayName: String?,
        category: ReportCategory,
        details: String = "",
        conversationId: String? = null,
        messageId: String? = null,
    ): ContentReport
}
