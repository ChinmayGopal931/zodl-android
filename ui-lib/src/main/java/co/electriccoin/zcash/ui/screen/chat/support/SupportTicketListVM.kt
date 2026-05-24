package co.electriccoin.zcash.ui.screen.chat.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.screen.chat.SupportChatArgs
import co.electriccoin.zcash.ui.screen.chat.common.runChatCall
import co.electriccoin.zcash.ui.screen.chat.model.ChatConversation
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.justzappit.zappmessaging.ZappMessagingSDK
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupportTicketListVM(
    private val sdk: ZappMessagingSDK,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {

    private val tickets = MutableStateFlow<List<TicketSnapshot>?>(null)
    private val closeTarget = MutableStateFlow<TicketSnapshot?>(null)
    private val categoryCache = mutableMapOf<String, String?>()

    init {
        refresh()
        observeConversations()
        observeUpdates()
    }

    val state: StateFlow<SupportTicketListState> =
        combine(tickets, closeTarget) { list, target ->
            SupportTicketListState(
                tickets = list.orEmpty().map { it.toItem() },
                isLoading = list == null,
                onNewTicket = ::onNewTicket,
                onBack = ::onBack,
                closeDialog = target?.let { t ->
                    SupportLeaveDialogState(
                        onConfirm = { onCloseConfirm(t) },
                        onDismiss = ::onCloseDismiss,
                    )
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = SupportTicketListState(
                tickets = emptyList(),
                isLoading = true,
                onNewTicket = ::onNewTicket,
                onBack = ::onBack,
                closeDialog = null,
            ),
        )

    // ── Data loading ─────────────────────────────────────────────────────────

    fun refresh() {
        viewModelScope.launch {
            runChatCall("SupportTicketListVM: refresh failed") {
                sdk.refreshConversations()
            }
        }
    }

    private fun observeConversations() {
        viewModelScope.launch {
            sdk.conversations.collect { _ -> buildTicketList() }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun buildTicketList() {
        try {
            val supportConvs = sdk.conversations.value
                .map(ChatConversation::from)
                .filter {
                    SupportChatConstants.isSupportConversation(it.displayName, it.participantIds)
                }
                .sortedByDescending { it.lastMessageTimestamp ?: 0L }

            val snapshots = supportConvs.map { conv ->
                val category = categoryCache.getOrPut(conv.id) { fetchCategory(conv.id) }
                TicketSnapshot(
                    conversationId = conv.id,
                    categoryLabel = category ?: "Ticket",
                    lastMessage = stripBotPrefix(conv.lastMessage),
                    lastMessageTimestamp = conv.lastMessageTimestamp,
                    unreadCount = conv.unreadCount,
                )
            }
            tickets.value = snapshots
        } catch (e: Exception) {
            Twig.warn(e) { "SupportTicketListVM: buildTicketList failed" }
            if (tickets.value == null) tickets.value = emptyList()
        }
    }

    private suspend fun fetchCategory(conversationId: String): String? {
        return try {
            val msgs = sdk.getMessages(conversationId).map(ChatMessage::from)
            msgs.firstOrNull { it.content.startsWith(SupportChatConstants.CATEGORY_MARKER) }
                ?.content
                ?.removePrefix(SupportChatConstants.CATEGORY_MARKER)
                ?.removeSuffix("]")
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Twig.warn(e) { "SupportTicketListVM: fetchCategory failed for $conversationId" }
            null
        }
    }

    private fun stripBotPrefix(message: String?): String? =
        message?.removePrefix(SupportChatConstants.BOT_PREFIX)

    private fun observeUpdates() {
        viewModelScope.launch {
            sdk.messageReceived.collect { _ -> buildTicketList() }
        }
        viewModelScope.launch {
            sdk.inviteReceived.collect { buildTicketList() }
        }
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private fun onNewTicket() {
        navigationRouter.forward(SupportChatArgs())
    }

    private fun onBack() = navigationRouter.back()

    private fun onCloseRequest(ticket: TicketSnapshot) {
        closeTarget.value = ticket
    }

    private fun onCloseDismiss() {
        closeTarget.value = null
    }

    private fun onCloseConfirm(ticket: TicketSnapshot) {
        closeTarget.value = null
        viewModelScope.launch {
            runChatCall("SupportTicketListVM: send leave notice failed") {
                sdk.sendMessage(
                    ticket.conversationId,
                    "${SupportChatConstants.BOT_PREFIX}${SupportChatConstants.LEAVE_MESSAGE}",
                )
            }
            runChatCall("SupportTicketListVM: removeConversation failed") {
                sdk.removeConversation(ticket.conversationId)
            }
            tickets.value = tickets.value?.filter { it.conversationId != ticket.conversationId }
        }
    }

    // ── Snapshot model ───────────────────────────────────────────────────────

    private data class TicketSnapshot(
        val conversationId: String,
        val categoryLabel: String,
        val lastMessage: String?,
        val lastMessageTimestamp: Long?,
        val unreadCount: Int,
    )

    private fun TicketSnapshot.toItem() = SupportTicketItem(
        conversationId = conversationId,
        categoryLabel = categoryLabel,
        lastMessage = lastMessage,
        timeLabel = lastMessageTimestamp?.let { formatTime(it) },
        unreadCount = unreadCount,
        onClick = { navigationRouter.forward(SupportChatArgs(conversationId = conversationId)) },
        onCloseSwipe = { onCloseRequest(this) },
    )

    companion object {
        private fun formatTime(epochMillis: Long): String {
            val diff = System.currentTimeMillis() - epochMillis
            return when {
                diff < 60_000L -> "now"
                diff < 3_600_000L -> "${diff / 60_000L}m"
                diff < 86_400_000L ->
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))
                diff < 604_800_000L ->
                    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(epochMillis))
                else ->
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMillis))
            }
        }
    }
}
