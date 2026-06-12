package co.electriccoin.zcash.ui.screen.chat.room

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.provider.ChatSendContextProvider
import co.electriccoin.zcash.ui.common.usecase.GetChatConnectionDetailsUseCase
import co.electriccoin.zcash.ui.common.usecase.GetChatMessagesUseCase
import co.electriccoin.zcash.ui.common.usecase.GetZashiAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.ObserveChatMediaDownloadCompleteUseCase
import co.electriccoin.zcash.ui.common.usecase.ObserveChatMessageReceivedUseCase
import co.electriccoin.zcash.ui.common.usecase.ObserveChatMessageStatusUseCase
import co.electriccoin.zcash.ui.common.usecase.ObserveChatPeerStatusUseCase
import co.electriccoin.zcash.ui.common.usecase.SendChatMediaMessageUseCase
import co.electriccoin.zcash.ui.common.usecase.SendChatMessageUseCase
import co.electriccoin.zcash.ui.common.usecase.UpdateChatContactUseCase
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.chat.ChatRoomArgs
import co.electriccoin.zcash.ui.screen.chat.common.runChatCall
import co.electriccoin.zcash.ui.screen.chat.list.ChatListChipVariant
import co.electriccoin.zcash.ui.screen.chat.list.ChatListConnectionStatus
import co.electriccoin.zcash.ui.screen.chat.list.ChatListDhtHealth
import co.electriccoin.zcash.ui.screen.chat.list.mapDhtHealth
import co.electriccoin.zcash.ui.screen.chat.media.FileUtils
import co.electriccoin.zcash.ui.screen.chat.media.ImageProcessor
import co.electriccoin.zcash.ui.screen.chat.model.ChatConversation
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
import co.electriccoin.zcash.ui.screen.chat.model.ConnectionDetailsUi
import co.electriccoin.zcash.ui.screen.chat.model.ConversationType
import co.electriccoin.zcash.ui.screen.chat.model.MessageStatus
import co.electriccoin.zcash.ui.screen.chat.model.MimeTypes
import co.electriccoin.zcash.ui.screen.chat.model.ReportCategory
import co.electriccoin.zcash.ui.screen.chat.repository.ChatConversationsRepository
import co.electriccoin.zcash.ui.screen.chat.repository.ChatModerationRepository
import co.electriccoin.zcash.ui.screen.unifiedsend.UnifiedSendArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Suppress("TooManyFunctions")
class ChatRoomVM(
    args: ChatRoomArgs,
    private val application: Application,
    private val moderationRepository: ChatModerationRepository,
    private val chatConversationsRepository: ChatConversationsRepository,
    private val getZashiAccount: GetZashiAccountUseCase,
    private val chatSendContext: ChatSendContextProvider,
    private val navigationRouter: NavigationRouter,
    private val observeChatMessageReceived: ObserveChatMessageReceivedUseCase,
    private val observeChatMessageStatus: ObserveChatMessageStatusUseCase,
    private val observeChatMediaDownloadComplete: ObserveChatMediaDownloadCompleteUseCase,
    private val observeChatPeerStatus: ObserveChatPeerStatusUseCase,
    private val getChatMessages: GetChatMessagesUseCase,
    private val sendChatMessage: SendChatMessageUseCase,
    private val sendChatMediaMessage: SendChatMediaMessageUseCase,
    private val getChatConnectionDetails: GetChatConnectionDetailsUseCase,
    private val updateChatContact: UpdateChatContactUseCase,
) : ViewModel() {
    private val conversationId: String = args.conversationId
    private val conversation: StateFlow<ChatConversation?> =
        chatConversationsRepository
            .conversation(conversationId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT), null)
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val isLoading = MutableStateFlow(true)

    private val connectionStatus = MutableStateFlow(ChatListConnectionStatus.CONNECTING)
    private val peerCount = MutableStateFlow(0)
    private val dhtHealth = MutableStateFlow(ChatListDhtHealth.HEALTHY)
    private val peerOnline = MutableStateFlow<Boolean?>(null)
    private val connectionDetails = MutableStateFlow<ConnectionDetailsUi?>(null)

    private val messageInput = MutableStateFlow("")
    private val replyingTo = MutableStateFlow<ChatMessage?>(null)
    private val showAttachmentSheet = MutableStateFlow(false)
    private val showMediaSheet = MutableStateFlow(false)
    private val showNetworkSheet = MutableStateFlow(false)
    private val showEditContact = MutableStateFlow(false)
    private val showBlockDialog = MutableStateFlow(false)
    private val showReportDialog = MutableStateFlow(false)

    private val _effects = MutableSharedFlow<ChatRoomEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<ChatRoomEffect> = _effects.asSharedFlow()

    init {
        chatConversationsRepository.setActiveConversation(conversationId)
        viewModelScope.launch { loadConversation() }
        viewModelScope.launch { loadMessages() }
        observeConnection()
        observeMessageEvents()
        observePeerStatus()
    }

    override fun onCleared() {
        chatConversationsRepository.setActiveConversation(null)
        super.onCleared()
    }

    val state: StateFlow<ChatRoomState> =
        combine(
            combine(conversation, messages, isLoading) { conv, msgs, loading ->
                Triple(conv, msgs, loading)
            },
            combine(connectionStatus, peerCount, dhtHealth, peerOnline) { cs, pc, dh, po ->
                ConnectionSnapshot(cs, pc, dh, po)
            },
            combine(messageInput, replyingTo, showAttachmentSheet, showMediaSheet) { input, reply, attach, media ->
                InputSnapshot(input, reply, attach, media)
            },
            combine(showNetworkSheet, connectionDetails) { net, details -> net to details },
            combine(showEditContact, showBlockDialog, showReportDialog) { edit, block, report ->
                Triple(edit, block, report)
            },
        ) { (conv, msgs, loading), conn, inputSnap, (net, details), (edit, block, report) ->
            createState(
                conversation = conv,
                messages = msgs,
                isLoading = loading,
                connection = conn,
                messageInput = inputSnap.input,
                replyingTo = inputSnap.reply,
                showAttachmentSheet = inputSnap.showAttach,
                showMediaSheet = inputSnap.showMedia,
                showNetworkSheet = net,
                connectionDetails = details,
                showEditContact = edit,
                showBlockDialog = block,
                showReportDialog = report,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue =
                createState(
                    conversation = null,
                    messages = emptyList(),
                    isLoading = true,
                    connection =
                        ConnectionSnapshot(
                            status = ChatListConnectionStatus.CONNECTING,
                            peerCount = 0,
                            dhtHealth = ChatListDhtHealth.HEALTHY,
                            peerOnline = null,
                        ),
                    messageInput = "",
                    replyingTo = null,
                    showAttachmentSheet = false,
                    showMediaSheet = false,
                    showNetworkSheet = false,
                    connectionDetails = null,
                    showEditContact = false,
                    showBlockDialog = false,
                    showReportDialog = false,
                ),
        )

    private data class ConnectionSnapshot(
        val status: ChatListConnectionStatus,
        val peerCount: Int,
        val dhtHealth: ChatListDhtHealth,
        val peerOnline: Boolean?,
    )

    private data class InputSnapshot(
        val input: String,
        val reply: ChatMessage?,
        val showAttach: Boolean,
        val showMedia: Boolean,
    )

    private fun createState(
        conversation: ChatConversation?,
        messages: List<ChatMessage>,
        isLoading: Boolean,
        connection: ConnectionSnapshot,
        messageInput: String,
        replyingTo: ChatMessage?,
        showAttachmentSheet: Boolean,
        showMediaSheet: Boolean,
        showNetworkSheet: Boolean,
        connectionDetails: ConnectionDetailsUi?,
        showEditContact: Boolean,
        showBlockDialog: Boolean,
        showReportDialog: Boolean,
    ): ChatRoomState =
        ChatRoomState(
            title =
                conversation
                    ?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { stringRes(it) }
                    ?: stringRes(R.string.chat_room_title_fallback),
            subtitle = subtitleText(connection),
            isTitleClickable = conversation?.type == ConversationType.DIRECT,
            onTitleClick = ::onTitleClick,
            onBack = ::onBack,
            networkChip =
                ChatRoomNetworkChipState(
                    text = chipText(connection),
                    variant = chipVariant(connection),
                    onClick = ::onChipClick,
                ),
            messages = messages,
            isLoading = isLoading,
            input =
                ChatRoomInputState(
                    value = messageInput,
                    placeholder = stringRes(R.string.chat_room_input_placeholder),
                    canSend = messageInput.isNotBlank() && !isLoading,
                    attachContentDescription = stringRes(R.string.chat_room_attach_content_description),
                    sendContentDescription = stringRes(R.string.chat_room_send_content_description),
                    onChange = ::onInputChange,
                    onSendClick = ::onSendTextClick,
                    onAttachClick = ::onAttachClick,
                    replyPreview =
                        replyingTo?.let { msg ->
                            val fallbackRes =
                                if (msg.isFromMe) {
                                    R.string.chat_room_reply_sender_self
                                } else {
                                    R.string.chat_room_reply_sender_unknown
                                }
                            ChatRoomReplyPreviewState(
                                senderName = msg.senderName ?: application.getString(fallbackRes),
                                content = msg.content.take(REPLY_PREVIEW_MAX_LENGTH),
                                onDismiss = ::dismissReply,
                            )
                        },
                ),
            attachmentSheet =
                if (showAttachmentSheet) {
                    ChatRoomAttachmentSheetState(
                        onShareAddress = ::onShareAddressClick,
                        onSendZec = ::onSendZecClick,
                        onAttachMedia = ::onAttachMediaClick,
                        onDismiss = ::dismissAttachmentSheet,
                    )
                } else {
                    null
                },
            mediaSheet =
                if (showMediaSheet) {
                    ChatRoomMediaSheetState(
                        onChooseMedia = ::onChooseMediaClick,
                        onAttachFile = ::onAttachFileClick,
                        onTakePhoto = ::onTakePhotoClick,
                        onShareLocation = ::onShareLocationClick,
                        onDismiss = ::dismissMediaSheet,
                    )
                } else {
                    null
                },
            networkSheet =
                if (showNetworkSheet) {
                    ChatRoomNetworkSheetState(
                        connectionStatus = connection.status,
                        peerCount = connection.peerCount,
                        dhtHealth = connection.dhtHealth,
                        connectionDetails = connectionDetails,
                        onDismiss = ::dismissNetworkSheet,
                    )
                } else {
                    null
                },
            editContactSheet =
                if (showEditContact && conversation?.type == ConversationType.DIRECT) {
                    ChatRoomEditContactSheetState(
                        currentName = conversation.displayName,
                        onSave = ::onEditContactSave,
                        onBlock = ::onEditContactBlock,
                        onReport = ::onEditContactReport,
                        onDismiss = ::dismissEditContact,
                    )
                } else {
                    null
                },
            blockDialog =
                if (showBlockDialog && conversation != null) {
                    ChatRoomBlockDialogState(
                        displayName = conversation.displayName,
                        onConfirm = ::onBlockConfirm,
                        onDismiss = ::dismissBlockDialog,
                    )
                } else {
                    null
                },
            reportDialog =
                if (showReportDialog && conversation != null) {
                    ChatRoomReportDialogState(
                        displayName = conversation.displayName,
                        onReport = ::onReportSubmit,
                        onBlock = ::onReportDialogBlock,
                        onDismiss = ::dismissReportDialog,
                    )
                } else {
                    null
                },
        )

    private fun chipText(connection: ConnectionSnapshot): StringResource =
        when (connection.status) {
            ChatListConnectionStatus.CONNECTED -> {
                when {
                    connection.peerOnline == true -> stringRes(R.string.chat_room_chip_online)
                    connection.dhtHealth == ChatListDhtHealth.CRITICAL -> stringRes(R.string.chat_room_chip_dht)
                    connection.peerCount > 0 -> stringRes(connection.peerCount.toString())
                    else -> stringRes(R.string.chat_list_status_connecting)
                }
            }

            ChatListConnectionStatus.CONNECTING -> {
                stringRes(R.string.chat_list_status_connecting)
            }

            ChatListConnectionStatus.DISCONNECTED -> {
                stringRes(R.string.chat_room_chip_off)
            }

            ChatListConnectionStatus.ERROR -> {
                stringRes(R.string.chat_room_chip_err)
            }
        }

    private fun chipVariant(connection: ConnectionSnapshot): ChatListChipVariant =
        when (connection.status) {
            ChatListConnectionStatus.CONNECTED -> {
                when {
                    connection.peerOnline == true -> ChatListChipVariant.Success
                    connection.dhtHealth == ChatListDhtHealth.CRITICAL -> ChatListChipVariant.Danger
                    connection.peerCount > 0 -> ChatListChipVariant.Success
                    else -> ChatListChipVariant.Accent
                }
            }

            ChatListConnectionStatus.CONNECTING -> {
                ChatListChipVariant.Accent
            }

            ChatListConnectionStatus.DISCONNECTED,
            ChatListConnectionStatus.ERROR,
            -> {
                ChatListChipVariant.Danger
            }
        }

    private fun subtitleText(connection: ConnectionSnapshot): StringResource =
        when (connection.status) {
            ChatListConnectionStatus.CONNECTED -> {
                when {
                    connection.peerOnline == true -> {
                        stringRes(R.string.chat_room_subtitle_peer_online)
                    }

                    connection.dhtHealth == ChatListDhtHealth.CRITICAL -> {
                        stringRes(R.string.chat_room_subtitle_dht_unreachable)
                    }

                    connection.peerOnline == false -> {
                        stringRes(R.string.chat_room_subtitle_peer_offline)
                    }

                    connection.peerCount > 0 -> {
                        stringRes(R.string.chat_room_subtitle_p2p_connected)
                    }

                    connection.dhtHealth == ChatListDhtHealth.DEGRADED -> {
                        stringRes(R.string.chat_room_subtitle_dht_degraded)
                    }

                    else -> {
                        stringRes(R.string.chat_room_subtitle_waiting_for_peer)
                    }
                }
            }

            ChatListConnectionStatus.CONNECTING -> {
                stringRes(R.string.chat_room_subtitle_connecting)
            }

            ChatListConnectionStatus.DISCONNECTED -> {
                stringRes(R.string.chat_room_subtitle_offline)
            }

            ChatListConnectionStatus.ERROR -> {
                stringRes(R.string.chat_room_subtitle_error)
            }
        }

    // ── Sources / observers ───────────────────────────────────────────────────

    private suspend fun loadConversation() {
        // The repository owns the conversation cache; ensure it is populated, then [conversation]
        // (derived from it) emits this room's conversation and tracks member/rename/delete edits.
        if (chatConversationsRepository.conversations.value.isNullOrEmpty()) {
            chatConversationsRepository.refresh()
        }
    }

    private suspend fun loadMessages() {
        isLoading.value = true
        try {
            getChatMessages(conversationId).onSuccess { zmList ->
                messages.value =
                    zmList
                        .map(ChatMessage::from)
                        .filterNot { msg -> moderationRepository.isBlocked(msg.senderId.orEmpty()) }
            }
        } finally {
            isLoading.value = false
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            chatConversationsRepository.isOnline.collect { online ->
                connectionStatus.value =
                    if (online) {
                        ChatListConnectionStatus.CONNECTED
                    } else {
                        ChatListConnectionStatus.DISCONNECTED
                    }
            }
        }
        viewModelScope.launch { chatConversationsRepository.peerCount.collect { peerCount.value = it } }
        viewModelScope.launch {
            chatConversationsRepository.dhtHealth.collect { dhtHealth.value = mapDhtHealth(it) }
        }
    }

    private fun observeMessageEvents() {
        observeIncomingMessages()
        observeMessageStatus()
        observeMediaDownloads()
        observeGroupDeletion()
    }

    private fun observeIncomingMessages() =
        viewModelScope.launch {
            observeChatMessageReceived().collect { (incomingConvId, msg) ->
                if (incomingConvId != conversationId) return@collect
                if (moderationRepository.isBlocked(msg.senderId)) return@collect
                messages.update { it + ChatMessage.from(msg) }
            }
        }

    private fun observeMessageStatus() =
        viewModelScope.launch {
            observeChatMessageStatus().collect { (messageId, _, status) ->
                val mapped = mapMessageStatus(status) ?: return@collect
                messages.update { list ->
                    list.map { m -> if (m.id == messageId) m.copy(status = mapped) else m }
                }
            }
        }

    private fun mapMessageStatus(status: String): MessageStatus? =
        when (status) {
            STATUS_SENT -> MessageStatus.SENT
            STATUS_QUEUED -> MessageStatus.QUEUED
            STATUS_FAILED -> MessageStatus.FAILED
            else -> null
        }

    private fun observeMediaDownloads() =
        viewModelScope.launch {
            observeChatMediaDownloadComplete().collect { (mediaId, filePath) ->
                messages.update { list ->
                    list.map { m ->
                        if (m.mediaId == mediaId && m.mediaLocalPath == null) {
                            m.copy(mediaLocalPath = filePath)
                        } else {
                            m
                        }
                    }
                }
            }
        }

    private fun observeGroupDeletion() =
        viewModelScope.launch {
            chatConversationsRepository.conversationDeleted.collect { deletedId ->
                if (deletedId == conversationId) navigationRouter.back()
            }
        }

    private fun observePeerStatus() {
        viewModelScope.launch {
            observeChatPeerStatus().collect { (statusConvId, _, status) ->
                if (statusConvId == conversationId) {
                    peerOnline.value = status == PEER_STATUS_ONLINE
                }
            }
        }
    }

    // ── Click handlers / state mutators ───────────────────────────────────────

    private fun onBack() = navigationRouter.back()

    private fun onTitleClick() {
        if (conversation.value?.type == ConversationType.DIRECT) {
            showEditContact.value = true
        }
    }

    private fun onChipClick() {
        viewModelScope.launch { fetchConnectionDetails() }
        showNetworkSheet.value = true
    }

    private fun onInputChange(value: String) {
        messageInput.value = value
    }

    fun onReplyToMessage(message: ChatMessage) {
        replyingTo.value = message
    }

    private fun dismissReply() {
        replyingTo.value = null
    }

    private fun onSendTextClick() {
        val text = messageInput.value.trim()
        if (text.isEmpty()) return
        val reply = replyingTo.value
        messageInput.value = ""
        replyingTo.value = null
        viewModelScope.launch { sendTextMessage(text, reply) }
    }

    private fun onAttachClick() {
        showAttachmentSheet.value = true
    }

    private fun onShareAddressClick() {
        showAttachmentSheet.value = false
        viewModelScope.launch { shareWalletAddress() }
    }

    private fun onSendZecClick() {
        showAttachmentSheet.value = false
        val peerAddress = resolvePeerWalletAddress()
        chatSendContext.set(conversationId)
        navigationRouter.forward(UnifiedSendArgs(recipientAddress = peerAddress))
    }

    private fun onAttachMediaClick() {
        showAttachmentSheet.value = false
        showMediaSheet.value = true
    }

    private fun onChooseMediaClick() {
        showMediaSheet.value = false
        _effects.tryEmit(ChatRoomEffect.PickMedia)
    }

    private fun onAttachFileClick() {
        showMediaSheet.value = false
        _effects.tryEmit(ChatRoomEffect.PickFile)
    }

    private fun onTakePhotoClick() {
        showMediaSheet.value = false
        _effects.tryEmit(ChatRoomEffect.TakePhoto)
    }

    private fun onShareLocationClick() {
        showMediaSheet.value = false
        _effects.tryEmit(ChatRoomEffect.ShareLocation)
    }

    private fun dismissAttachmentSheet() {
        showAttachmentSheet.value = false
    }

    private fun dismissMediaSheet() {
        showMediaSheet.value = false
    }

    private fun dismissNetworkSheet() {
        showNetworkSheet.value = false
    }

    private fun dismissEditContact() {
        showEditContact.value = false
    }

    private fun dismissBlockDialog() {
        showBlockDialog.value = false
    }

    private fun dismissReportDialog() {
        showReportDialog.value = false
    }

    private fun onEditContactSave(newName: String) {
        val peerKey = conversation.value?.participantIds?.firstOrNull() ?: return
        showEditContact.value = false
        viewModelScope.launch { updateContact(peerKey, newName) }
    }

    private fun onEditContactBlock() {
        showEditContact.value = false
        showBlockDialog.value = true
    }

    private fun onEditContactReport() {
        showEditContact.value = false
        showReportDialog.value = true
    }

    private fun onBlockConfirm() {
        // Read the conversation once: it is a WhileSubscribed flow the repository can update off
        // the main thread, so re-reading .value per field could pair a key with a stale name.
        val conv = conversation.value ?: return
        val peerKey = conv.participantIds.firstOrNull() ?: return
        moderationRepository.blockUser(peerKey, conv.displayName)
        showBlockDialog.value = false
        navigationRouter.back()
    }

    private fun onReportSubmit(category: ReportCategory, details: String) {
        val conv = conversation.value ?: return
        val peerKey = conv.participantIds.firstOrNull() ?: return
        moderationRepository.submitReport(
            reportedPublicKey = peerKey,
            reportedDisplayName = conv.displayName,
            category = category,
            details = details,
            conversationId = conversationId,
            messageId = null,
        )
    }

    private fun onReportDialogBlock() {
        val conv = conversation.value ?: return
        val peerKey = conv.participantIds.firstOrNull() ?: return
        moderationRepository.blockUser(peerKey, conv.displayName)
        showReportDialog.value = false
        navigationRouter.back()
    }

    // ── External-effect entry points (View calls these after launchers fire) ─

    fun onMediaPicked(uri: Uri) {
        viewModelScope.launch { sendMediaFromUri(uri) }
    }

    fun onFilePicked(uri: Uri) {
        viewModelScope.launch { sendFileFromUri(uri) }
    }

    fun onCameraCaptured(uri: Uri) {
        viewModelScope.launch { sendCameraCapture(uri) }
    }

    fun onLocationObtained(latitude: Double, longitude: Double, accuracy: Float) {
        viewModelScope.launch { sendLocationMessage(latitude, longitude, accuracy) }
    }

    // ── SDK calls ────────────────────────────────────────────────────────────

    private suspend fun sendTextMessage(text: String, replyTo: ChatMessage? = null) {
        // TODO: thread replyTo through once zappMessaging sdk.sendMessage accepts replyTo*
        // params (not in the currently-pinned SHA in .zapp-deps). Local-only echo for now.
        sendChatMessage(conversationId = conversationId, content = text)
            .onSuccess { zmMessage -> messages.update { it + ChatMessage.from(zmMessage) } }
            .onFailure {
                // Send failed (logged by runChatCallResult); restore the draft that the optimistic
                // clear wiped so the user can retry instead of silently losing their message.
                if (messageInput.value.isEmpty()) messageInput.value = text
            }
    }

    private suspend fun sendMediaFromUri(uri: Uri) {
        runChatCall("ChatRoomVM: sendMedia failed") {
            withContext(Dispatchers.IO) {
                val mimeType = FileUtils.getMimeType(application, uri)
                val thumbnail =
                    if (mimeType.startsWith(MimeTypes.IMAGE_PREFIX)) {
                        ImageProcessor.generateThumbnail(application, uri)
                    } else {
                        null
                    }
                if (mimeType == MimeTypes.GIF) {
                    val cached =
                        FileUtils.copyUriToCache(application, uri) ?: error("Failed to cache GIF")
                    sendMediaMessage(cached.absolutePath, MimeTypes.GIF, thumbnailData = thumbnail)
                } else if (mimeType.startsWith(MimeTypes.IMAGE_PREFIX)) {
                    val compressed =
                        ImageProcessor.compressImage(application, uri)
                            ?: error("Image compression failed")
                    sendMediaMessage(compressed.absolutePath, MimeTypes.IMAGE_JPEG, thumbnailData = thumbnail)
                } else {
                    val cached =
                        FileUtils.copyUriToCache(application, uri) ?: error("Failed to cache media")
                    sendMediaMessage(cached.absolutePath, mimeType, thumbnailData = thumbnail)
                }
            }
        }
    }

    private suspend fun sendFileFromUri(uri: Uri) {
        runChatCall("ChatRoomVM: sendFile failed") {
            withContext(Dispatchers.IO) {
                val cached =
                    FileUtils.copyUriToCache(application, uri) ?: error("Failed to cache file")
                val mimeType = FileUtils.getMimeType(application, uri)
                val fileName = FileUtils.getFileName(application, uri) ?: FILE_FALLBACK_NAME
                val thumbnail =
                    if (mimeType.startsWith(MimeTypes.IMAGE_PREFIX)) {
                        ImageProcessor.generateThumbnail(application, uri)
                    } else {
                        null
                    }
                sendMediaMessage(cached.absolutePath, mimeType, fileName, thumbnail)
            }
        }
    }

    private suspend fun sendCameraCapture(uri: Uri) {
        runChatCall("ChatRoomVM: sendCameraCapture failed") {
            withContext(Dispatchers.IO) {
                val thumbnail = ImageProcessor.generateThumbnail(application, uri)
                val compressed =
                    ImageProcessor.compressImage(application, uri)
                        ?: error("Image compression failed")
                sendMediaMessage(compressed.absolutePath, MimeTypes.IMAGE_JPEG, thumbnailData = thumbnail)
            }
        }
    }

    private suspend fun sendMediaMessage(
        mediaPath: String,
        contentType: String,
        caption: String = "",
        thumbnailData: String? = null,
    ) {
        sendChatMediaMessage(conversationId, mediaPath, contentType, caption, thumbnailData).onSuccess { zmMessage ->
            messages.update { it + ChatMessage.from(zmMessage) }
        }
    }

    private suspend fun sendLocationMessage(latitude: Double, longitude: Double, accuracy: Float) {
        val content =
            JSONObject()
                .apply {
                    put("latitude", latitude)
                    put("longitude", longitude)
                    put("accuracy", accuracy.toDouble())
                }.toString()
        sendChatMessage(conversationId, content, MimeTypes.LOCATION).onSuccess { zmMessage ->
            messages.update { it + ChatMessage.from(zmMessage) }
        }
    }

    private suspend fun shareWalletAddress() {
        val address = getZashiAccount().unified.address.address
        sendChatMessage(conversationId, address, MimeTypes.WALLET_ADDRESS).onSuccess { zmMessage ->
            messages.update { it + ChatMessage.from(zmMessage) }
        }
    }

    private suspend fun fetchConnectionDetails() {
        getChatConnectionDetails().onSuccess { details ->
            connectionDetails.value = ConnectionDetailsUi.from(details)
        }
    }

    private suspend fun updateContact(publicKey: String, newName: String) {
        updateChatContact(publicKey, newName).onSuccess {
            chatConversationsRepository.renameConversation(conversationId, newName)
        }
    }

    private fun resolvePeerWalletAddress(): String? =
        messages.value
            .lastOrNull { msg ->
                msg.contentType == MimeTypes.WALLET_ADDRESS && !msg.isFromMe
            }?.content
            ?.takeIf { it.isNotBlank() }

    companion object {
        const val STATUS_SENT = "sent"
        const val STATUS_QUEUED = "queued"
        const val STATUS_FAILED = "failed"
        const val PEER_STATUS_ONLINE = "online"
        const val FILE_FALLBACK_NAME = "File"
        const val REPLY_PREVIEW_MAX_LENGTH = 100
    }
}
