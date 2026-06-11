package co.electriccoin.zcash.ui.screen.chat.model

import androidx.annotation.StringRes
import co.electriccoin.zcash.ui.R

/**
 * Represents a blocked user in the local blocklist.
 */
data class BlockedUser(
    val publicKey: String,
    val displayName: String?,
    val blockedAt: Long = System.currentTimeMillis()
)

/**
 * Report categories for UGC compliance.
 *
 * The enum [name] is the persisted value — must NEVER be localized.
 * [displayNameRes] is the localized label rendered on the report dialog.
 */
enum class ReportCategory(
    @param:StringRes val displayNameRes: Int
) {
    SPAM(R.string.chat_report_category_spam),
    HARASSMENT(R.string.chat_report_category_harassment),
    ILLEGAL_CONTENT(R.string.chat_report_category_illegal_content),
    IMPERSONATION(R.string.chat_report_category_impersonation),
    SCAM(R.string.chat_report_category_scam),
    OTHER(R.string.chat_report_category_other)
}

/**
 * A user-submitted content report stored locally.
 */
data class ContentReport(
    val id: String =
        java.util.UUID
            .randomUUID()
            .toString(),
    val reportedPublicKey: String,
    val reportedDisplayName: String?,
    val category: ReportCategory,
    val details: String = "",
    val conversationId: String? = null,
    val messageId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
