package co.electriccoin.zcash.ui.screen.chat.model

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
 */
enum class ReportCategory(val displayLabel: String) {
    SPAM("Spam or unwanted messages"),
    HARASSMENT("Harassment or bullying"),
    ILLEGAL_CONTENT("Illegal content"),
    IMPERSONATION("Impersonation"),
    SCAM("Scam or fraud"),
    OTHER("Other")
}

/**
 * A user-submitted content report stored locally.
 */
data class ContentReport(
    val id: String = java.util.UUID.randomUUID().toString(),
    val reportedPublicKey: String,
    val reportedDisplayName: String?,
    val category: ReportCategory,
    val details: String = "",
    val conversationId: String? = null,
    val messageId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
