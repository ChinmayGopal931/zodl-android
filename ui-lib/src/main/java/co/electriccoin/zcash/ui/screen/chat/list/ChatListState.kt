package co.electriccoin.zcash.ui.screen.chat.list

import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.screen.chat.model.ConnectionDetailsUi

data class ChatListState(
    val title: StringResource,
    val isLoading: Boolean,
    val items: List<ChatListItemState>,
    val emptyTitle: StringResource,
    val emptySubtitle: StringResource,
    val newConversationContentDescription: StringResource,
    val onBack: () -> Unit,
    val onNewConversationClick: () -> Unit,
    val networkChip: ChatListNetworkChipState,
    val networkSheet: ChatListNetworkSheetState?,
    val tosDialog: ChatListTosDialogState?,
    val leaveDialog: ChatListLeaveDialogState?,
    /** Non-null once the feature is wired; [ChatListSupportRowState.isActive] drives placement. */
    val supportRow: ChatListSupportRowState,
)

data class ChatListItemState(
    val id: String,
    val displayName: String,
    val isGroup: Boolean,
    val lastMessage: StringResource,
    val timeLabel: StringResource?,
    val unreadCount: Int,
    val onClick: () -> Unit,
    val onLeaveSwipe: () -> Unit,
)

data class ChatListNetworkChipState(
    val text: StringResource,
    val variant: ChatListChipVariant,
    val onClick: () -> Unit,
)

enum class ChatListChipVariant { Success, Accent, Danger }

data class ChatListNetworkSheetState(
    val connectionStatus: ChatListConnectionStatus,
    val peerCount: Int,
    val dhtHealth: ChatListDhtHealth,
    val connectionDetails: ConnectionDetailsUi?,
    val onDismiss: () -> Unit,
)

data class ChatListTosDialogState(
    val onAccept: () -> Unit,
    val onDecline: () -> Unit,
)

data class ChatListLeaveDialogState(
    val conversationName: String,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * Aggregate state for the "Zapp Support" parent row in the chat list.
 *
 * When [isActive] is true at least one support ticket exists and the row is pinned at
 * the top of the list. Tapping it opens the ticket list, not an individual chat.
 */
data class ChatListSupportRowState(
    val isActive: Boolean,
    val ticketCount: Int,
    val lastMessage: StringResource?,
    val totalUnreadCount: Int,
    val onClick: () -> Unit,
)
