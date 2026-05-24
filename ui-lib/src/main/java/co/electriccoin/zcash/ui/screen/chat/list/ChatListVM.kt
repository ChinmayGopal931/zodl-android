package co.electriccoin.zcash.ui.screen.chat.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.preference.StandardPreferenceProvider
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.preference.StandardPreferenceKeys
import co.electriccoin.zcash.ui.screen.chat.ChatRoomArgs
import co.electriccoin.zcash.ui.screen.chat.NewConversationArgs
import co.electriccoin.zcash.ui.screen.chat.SupportTicketListArgs
import co.electriccoin.zcash.ui.screen.chat.support.SupportChatConstants
import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import co.electriccoin.zcash.ui.screen.chat.common.formatRelativeTime
import co.electriccoin.zcash.ui.screen.chat.common.runChatCall
import co.electriccoin.zcash.ui.screen.chat.model.ChatConversation
import co.electriccoin.zcash.ui.screen.chat.model.ConnectionDetailsUi
import co.electriccoin.zcash.ui.screen.chat.model.ConversationType
import co.electriccoin.zcash.ui.screen.chat.repository.ChatModerationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.justzappit.zappmessaging.ZappMessagingSDK

@Suppress("TooManyFunctions")
class ChatListVM(
    private val sdk: ZappMessagingSDK,
    private val moderationRepository: ChatModerationRepository,
    private val standardPreferenceProvider: StandardPreferenceProvider,
    private val navigationRouter: NavigationRouter,
    private val chatBootstrap: ChatBootstrap,
) : ViewModel() {
    private val conversations = MutableStateFlow<List<ChatConversation>?>(null)
    private val localPublicKey = MutableStateFlow<String?>(null)
    private val connectionStatus = MutableStateFlow(ChatListConnectionStatus.CONNECTING)
    private val peerCount = MutableStateFlow(0)
    private val dhtHealth = MutableStateFlow(ChatListDhtHealth.HEALTHY)
    private val connectionDetails = MutableStateFlow<ConnectionDetailsUi?>(null)
    private val showNetworkSheet = MutableStateFlow(false)
    private val showTosDialog = MutableStateFlow(false)
    private val leaveTarget = MutableStateFlow<ChatConversation?>(null)
    private val activeConversationId = MutableStateFlow<String?>(null)

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch { observeIdentityAndRefresh() }
        observeConnection()
        observeConversationUpdates()
        viewModelScope.launch { checkTosAccepted() }
    }

    private fun buildSupportRow(supportConvs: List<ChatConversation>): ChatListSupportRowState? {
        if (supportConvs.isEmpty()) return null
        val latestSupportMsg = supportConvs
            .maxByOrNull { it.lastMessageTimestamp ?: 0L }
            ?.lastMessage
            ?.removePrefix(SupportChatConstants.BOT_PREFIX)
        return ChatListSupportRowState(
            ticketCount = supportConvs.size,
            lastMessage = latestSupportMsg?.let { stringRes(it) },
            totalUnreadCount = supportConvs.sumOf { it.unreadCount },
            onClick = ::onSupportClick,
        )
    }

    val state: StateFlow<ChatListState> =
        combine(
            combine(conversations, moderationRepository.blockedKeys, localPublicKey) { c, b, pk ->
                Triple(c, b, pk)
            },
            combine(connectionStatus, peerCount, dhtHealth) { cs, pc, dh -> Triple(cs, pc, dh) },
            combine(showTosDialog, showNetworkSheet, leaveTarget) { tos, sheet, leave ->
                Triple(tos, sheet, leave)
            },
            connectionDetails,
        ) { (convs, blocked, pk), (cs, pc, dh), (tos, sheet, leave), details ->
            createState(
                conversations = convs,
                blockedKeys = blocked,
                localPublicKey = pk,
                connectionStatus = cs,
                peerCount = pc,
                dhtHealth = dh,
                connectionDetails = details,
                showNetworkSheet = sheet,
                showTosDialog = tos,
                leaveTarget = leave,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue =
                createState(
                    conversations = null,
                    blockedKeys = emptySet(),
                    localPublicKey = null,
                    connectionStatus = ChatListConnectionStatus.CONNECTING,
                    peerCount = 0,
                    dhtHealth = ChatListDhtHealth.HEALTHY,
                    connectionDetails = null,
                    showNetworkSheet = false,
                    showTosDialog = false,
                    leaveTarget = null,
                ),
        )


    private fun createState(
        conversations: List<ChatConversation>?,
        blockedKeys: Set<String>,
        localPublicKey: String?,
        connectionStatus: ChatListConnectionStatus,
        peerCount: Int,
        dhtHealth: ChatListDhtHealth,
        connectionDetails: ConnectionDetailsUi?,
        showNetworkSheet: Boolean,
        showTosDialog: Boolean,
        leaveTarget: ChatConversation?,
    ): ChatListState {
        // Pin the aggregate "Zapp Support" row above the timestamp-sorted list.
        // [isSupportConversation] handles the side-asymmetry: user device requires the
        // support agent's key in participantIds; the support agent's device falls back
        // to the displayName prefix because its own key is excluded from the participant list.
        val supportConvs = conversations?.filter { conv ->
            SupportChatConstants.isSupportConversation(
                displayName = conv.displayName,
                participantIds = conv.participantIds,
                localPublicKey = localPublicKey,
            )
        }.orEmpty()

        val visibleConversations =
            conversations
                ?.filter { conv ->
                    !SupportChatConstants.isSupportConversation(
                        displayName = conv.displayName,
                        participantIds = conv.participantIds,
                        localPublicKey = localPublicKey,
                    ) && (
                        conv.type != ConversationType.DIRECT ||
                            conv.participantIds.none { it in blockedKeys }
                    )
                }?.sortedByDescending { it.lastMessageTimestamp ?: 0L }
                .orEmpty()

        val supportRow = buildSupportRow(supportConvs)

        return ChatListState(
            title = stringRes(R.string.chat_list_title),
            isLoading = conversations == null,
            items = visibleConversations.map(::toItemState),
            emptyTitle = stringRes(R.string.chat_list_empty_title),
            emptySubtitle = stringRes(R.string.chat_list_empty_subtitle),
            newConversationContentDescription =
                stringRes(R.string.chat_list_new_conversation_content_description),
            onBack = ::onBack,
            onNewConversationClick = ::onNewConversationClick,
            networkChip =
                ChatListNetworkChipState(
                    text = networkChipText(connectionStatus, peerCount),
                    variant = networkChipVariant(connectionStatus),
                    onClick = ::onNetworkChipClick,
                ),
            networkSheet =
                if (showNetworkSheet) {
                    ChatListNetworkSheetState(
                        connectionStatus = connectionStatus,
                        peerCount = peerCount,
                        dhtHealth = dhtHealth,
                        connectionDetails = connectionDetails,
                        onDismiss = ::onNetworkSheetDismiss,
                    )
                } else {
                    null
                },
            tosDialog =
                if (showTosDialog) {
                    ChatListTosDialogState(
                        onAccept = ::onAcceptTos,
                        onDecline = ::onDeclineTos,
                    )
                } else {
                    null
                },
            leaveDialog =
                leaveTarget?.let { conv ->
                    ChatListLeaveDialogState(
                        conversationName = conv.displayName,
                        onConfirm = { onLeaveConfirm(conv) },
                        onDismiss = ::onLeaveDismiss,
                    )
                },
            supportRow = supportRow,
        )
    }

    private fun toItemState(conv: ChatConversation): ChatListItemState =
        ChatListItemState(
            id = conv.id,
            displayName = conv.displayName,
            isGroup = conv.type == ConversationType.GROUP,
            lastMessage = lastMessageText(conv.lastMessage),
            timeLabel = conv.lastMessageTimestamp?.let { ts -> formatRelativeTime(ts) },
            unreadCount = conv.unreadCount,
            onClick = { onConversationClick(conv) },
            onLeaveSwipe = { onLeaveRequest(conv) },
        )

    private fun lastMessageText(value: String?): StringResource =
        when {
            value == null -> stringRes(R.string.chat_list_no_messages)
            value == MEDIA_PLACEHOLDER_SENTINEL -> stringRes(R.string.chat_list_media_placeholder)
            else -> stringRes(value)
        }

    private fun networkChipText(
        status: ChatListConnectionStatus,
        peerCount: Int,
    ): StringResource =
        when (status) {
            ChatListConnectionStatus.CONNECTED -> stringRes(peerCount.toString())
            ChatListConnectionStatus.CONNECTING -> stringRes(R.string.chat_list_status_connecting)
            ChatListConnectionStatus.DISCONNECTED -> stringRes(R.string.chat_list_status_disconnected)
            ChatListConnectionStatus.ERROR -> stringRes(R.string.chat_list_status_error)
        }

    private fun networkChipVariant(status: ChatListConnectionStatus): ChatListChipVariant =
        when (status) {
            ChatListConnectionStatus.CONNECTED -> ChatListChipVariant.Success

            ChatListConnectionStatus.CONNECTING -> ChatListChipVariant.Accent

            ChatListConnectionStatus.DISCONNECTED,
            ChatListConnectionStatus.ERROR,
            -> ChatListChipVariant.Danger
        }

    private fun onBack() = navigationRouter.back()

    private fun onNewConversationClick() = navigationRouter.forward(NewConversationArgs)

    private fun onSupportClick() = navigationRouter.forward(SupportTicketListArgs)

    private fun onConversationClick(conv: ChatConversation) {
        activeConversationId.value = conv.id
        conversations.update { current ->
            current?.map { c -> if (c.id == conv.id) c.copy(unreadCount = 0) else c }
        }
        chatBootstrap.markConversationRead(conv.id)
        navigationRouter.forward(ChatRoomArgs(conv.id))
    }

    private fun onLeaveRequest(conv: ChatConversation) {
        leaveTarget.value = conv
    }

    private fun onLeaveDismiss() {
        leaveTarget.value = null
    }

    private fun onLeaveConfirm(conv: ChatConversation) {
        leaveTarget.value = null
        viewModelScope.launch { leaveConversation(conv) }
    }

    private fun onNetworkChipClick() {
        viewModelScope.launch { refreshConnectionDetails() }
        showNetworkSheet.value = true
    }

    private fun onNetworkSheetDismiss() {
        showNetworkSheet.value = false
    }

    private fun onAcceptTos() {
        viewModelScope.launch {
            StandardPreferenceKeys.IS_CHAT_TOS_ACCEPTED
                .putValue(standardPreferenceProvider(), true)
            showTosDialog.value = false
        }
    }

    private fun onDeclineTos() {
        showTosDialog.value = false
    }

    private suspend fun observeIdentityAndRefresh() {
        sdk.identity.collect { id ->
            localPublicKey.value = id?.publicKey
            if (id != null) startConversationRefresh()
        }
    }

    private fun startConversationRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { refreshConversations() }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun refreshConversations() {
        try {
            sdk.refreshConversations()
            val list =
                sdk.conversations.value
                    .map(ChatConversation::from)
                    .sortedByDescending { it.lastMessageTimestamp ?: 0L }
            conversations.value = list
        } catch (e: Exception) {
            Twig.warn(e) { "ChatListVM: failed to refresh conversations" }
            // Surface an empty list so the View leaves the loading state.
            if (conversations.value == null) conversations.value = emptyList()
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            sdk.isOnline.collect { online ->
                connectionStatus.value =
                    if (online) {
                        ChatListConnectionStatus.CONNECTED
                    } else {
                        ChatListConnectionStatus.DISCONNECTED
                    }
            }
        }
        viewModelScope.launch { sdk.peerCount.collect { peerCount.value = it } }
        viewModelScope.launch { sdk.dhtHealth.collect { dhtHealth.value = mapDhtHealth(it) } }
    }

    private fun observeConversationUpdates() {
        viewModelScope.launch {
            sdk.messageReceived.collect { (conversationId, msg) ->
                if (moderationRepository.isBlocked(msg.senderId)) return@collect
                val isViewingConversation = conversationId == activeConversationId.value
                conversations.update { current ->
                    current?.map { conv ->
                        if (conv.id == conversationId) {
                            conv.copy(
                                lastMessage = msg.content.ifEmpty { MEDIA_PLACEHOLDER_SENTINEL },
                                lastMessageTimestamp = msg.timestamp,
                                unreadCount =
                                    when {
                                        msg.isFromMe -> conv.unreadCount
                                        isViewingConversation -> conv.unreadCount
                                        else -> conv.unreadCount + 1
                                    },
                            )
                        } else {
                            conv
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            sdk.inviteReceived.collect {
                delay(CONVERSATION_RELOAD_DEBOUNCE_MS)
                refreshConversations()
            }
        }
        viewModelScope.launch {
            sdk.groupDeleted.collect { conversationId ->
                conversations.update { it?.filter { conv -> conv.id != conversationId } }
            }
        }
        viewModelScope.launch {
            sdk.groupRenamed.collect { (conversationId, newName) ->
                conversations.update { list ->
                    list?.map { conv ->
                        if (conv.id == conversationId) conv.copy(displayName = newName) else conv
                    }
                }
            }
        }
        viewModelScope.launch {
            sdk.memberLeft.collect { (conversationId, peerKey) ->
                conversations.update { list ->
                    list?.map { conv ->
                        if (conv.id == conversationId) {
                            conv.copy(participantIds = conv.participantIds.filter { it != peerKey })
                        } else {
                            conv
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            sdk.memberAdded.collect { (conversationId, peerKey, _) ->
                conversations.update { list ->
                    list?.map { conv ->
                        if (conv.id == conversationId && peerKey !in conv.participantIds) {
                            conv.copy(participantIds = conv.participantIds + peerKey)
                        } else {
                            conv
                        }
                    }
                }
            }
        }
    }

    private suspend fun leaveConversation(conv: ChatConversation) {
        runChatCall("ChatListVM: leave conversation failed") {
            sdk.removeConversation(conv.id)
            conversations.update { it?.filter { c -> c.id != conv.id } }
        }
    }

    private suspend fun refreshConnectionDetails() {
        runChatCall("ChatListVM: failed to fetch connection details") {
            connectionDetails.value = ConnectionDetailsUi.from(sdk.getConnectionDetails())
        }
    }

    private suspend fun checkTosAccepted() {
        val accepted =
            StandardPreferenceKeys.IS_CHAT_TOS_ACCEPTED.getValue(standardPreferenceProvider())
        if (!accepted) showTosDialog.value = true
    }

    companion object {
        const val MEDIA_PLACEHOLDER_SENTINEL = "[Media]"
        private const val CONVERSATION_RELOAD_DEBOUNCE_MS = 500L
    }
}
