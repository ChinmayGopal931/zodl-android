package co.electriccoin.zcash.ui.screen.chat.support

import android.content.Context
import co.electriccoin.zcash.ui.R

enum class SupportCategory(val label: String) {
    PROBLEM("Problem"),
    FEEDBACK("Feedback"),
    OTHER("Other"),
}

object SupportChatConstants {
    const val SUPPORT_PUBLIC_KEY = "15984cb52f224e3883d866825fb427770939dea73f14403d5c23397eeffebf07"
    const val SUPPORT_DISPLAY_NAME = "Zapp Support"

    /**
     * Prefix set on every support-ticket conversation's displayName.
     * Present on BOTH the user's and the support agent's device (the JS core
     * propagates it via `groupName` in the group invite), so it is the only
     * reliable signal for identifying support conversations regardless of
     * which side is running the filter.
     *
     * `participantIds` cannot be used alone because the SDK excludes the
     * local user's own key — on the support agent's device the list contains
     * only the user's key, not `SUPPORT_PUBLIC_KEY`.
     */
    const val DISPLAY_NAME_PREFIX = "Support: "

    /**
     * Prefix applied to all automated messages before they are sent via the SDK.
     * Visible to the support agent so automated messages are clearly distinguished
     * from user-typed ones.
     */
    const val BOT_PREFIX = "[Zapp]: "

    /**
     * Prefix of the category-selection marker sent when the user picks a topic.
     * Used to detect whether the user has already selected a category on reload.
     */
    const val CATEGORY_MARKER = "[Category: "

    /** Localised greeting shown after the user picks a topic. */
    fun categoryGreeting(context: Context, category: SupportCategory): String =
        context.getString(
            when (category) {
                SupportCategory.PROBLEM -> R.string.support_chat_greeting_problem
                SupportCategory.FEEDBACK -> R.string.support_chat_greeting_feedback
                SupportCategory.OTHER -> R.string.support_chat_greeting_other
            },
        )

    /** Sent to the support peer when the user closes the chat. */
    fun leaveNotice(context: Context): String =
        context.getString(R.string.support_chat_leave_notice)

    /**
     * Returns true when the conversation is a support ticket.
     * Works on both the user's device (where `participantIds` contains
     * [SUPPORT_PUBLIC_KEY]) and the support agent's device (where
     * `participantIds` contains the user's key instead, but `displayName`
     * still carries the [DISPLAY_NAME_PREFIX]).
     */
    fun isSupportConversation(
        displayName: String,
        participantIds: List<String>,
    ): Boolean =
        displayName.startsWith(DISPLAY_NAME_PREFIX) ||
            participantIds.contains(SUPPORT_PUBLIC_KEY)
}
