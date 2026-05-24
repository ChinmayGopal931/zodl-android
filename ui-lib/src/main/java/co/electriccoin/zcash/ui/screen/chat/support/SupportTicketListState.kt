package co.electriccoin.zcash.ui.screen.chat.support

data class SupportTicketListState(
    val tickets: List<SupportTicketItem>,
    val isLoading: Boolean,
    val onNewTicket: () -> Unit,
    val onBack: () -> Unit,
    val closeDialog: SupportLeaveDialogState?,
)

data class SupportTicketItem(
    val conversationId: String,
    val categoryLabel: String,
    val lastMessage: String?,
    val timeLabel: String?,
    val unreadCount: Int,
    val onClick: () -> Unit,
    val onCloseSwipe: () -> Unit,
)
