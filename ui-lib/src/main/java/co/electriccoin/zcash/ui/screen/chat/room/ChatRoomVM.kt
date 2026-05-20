package co.electriccoin.zcash.ui.screen.chat.room

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.usecase.ChatSendContext
import co.electriccoin.zcash.ui.common.usecase.GetZashiAccountUseCase
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
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
import co.electriccoin.zcash.ui.screen.chat.model.ReportCategory
import co.electriccoin.zcash.ui.screen.chat.ChatRoomArgs
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
import xyz.justzappit.zappmessaging.ZappMessagingSDK

@Suppress("TooManyFunctions", "LongParameterList")
class ChatRoomVM(
    args: ChatRoomArgs,
    private val application: Application,
    private val sdk: ZappMessagingSDK,
    private val moderationRepository: ChatModerationRepository,
    private val getZashiAccount: GetZashiAccountUseCase,
    private val chatSendContext: ChatSendContext,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val conversationId: String = args.conversationId
    private val conversation = MutableStateFlow<ChatConversation?>(null)
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val isLoading = MutableStateFlow(true)

    private val connectionStatus = MutableStateFlow(ChatListConnectionStatus.CONNECTING)
    private val peerCount = MutableStateFlow(0)
    private val dhtHealth = MutableStateFlow(ChatListDhtHealth.HEALTHY)
    private val peerOnline = MutableStateFlow<Boolean?>(null)
    private val connectionDetails = MutableStateFlow<ConnectionDetailsUi?>(null)

    private val messageInput = MutableStateFlow("")
    private val showAttachmentSheet = MutableStateFlow(false)
    private val showMediaSheet = MutableStateFlow(false)
    private val showNetworkSheet = MutableStateFlow(false)
    private val showEditContact = MutableStateFlow(false)
    private val showBlockDialog = MutableStateFlow(false)
    private val showReportDialog = MutableStateFlow(false)

    private val _effects = MutableSharedFlow<ChatRoomEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<ChatRoomEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch { loadConversation() }
        viewModelScope.launch { loadMessages() }
        observeConnection()
        observeMessageEvents()
        observePeerStatus()
    }

    @Suppress("LongMethod")
    val state: StateFlow<ChatRoomState?> =
        combine(
            combine(conversation, messages, isLoading) { conv, msgs, loading ->
                Triple(conv, msgs, loading)
            },
            combine(connectionStatus, peerCount, dhtHealth, peerOnline) { cs, pc, dh, po ->
                ConnectionSnapshot(cs, pc, dh, po)
            },
            combine(messageInput, showAttachmentSheet, showMediaSheet) { input, attach, media ->
                Triple(input, attach, media)
            },
            combine(showNetworkSheet, connectionDetails) { net, details -> net to details },
            combine(showEditContact, showBlockDialog, showReportDialog) { edit, block, report ->
                Triple(edit, block, report)
            },
        ) { (conv, msgs, loading), conn, (input, attach, media), (net, details), (edit, block, report) ->
            createState(
                conversation = conv,
                messages = msgs,
                isLoading = loading,
                connection = conn,
                messageInput = input,
                showAttachmentSheet = attach,
                showMediaSheet = media,
                showNetworkSheet = net,
                connectionDetails = details,
                showEditContact = edit,
                showBlockDialog = block,
                showReportDialog = report,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = null,
        )

    private data class ConnectionSnapshot(
        val status: ChatListConnectionStatus,
        val peerCount: Int,
        val dhtHealth: ChatListDhtHealth,
        val peerOnline: Boolean?,
    )

    @Suppress("LongParameterList")
    private fun createState(
        conversation: ChatConversation?,
        messages: List<ChatMessage>,
        isLoading: Boolean,
        connection: ConnectionSnapshot,
        messageInput: String,
        showAttachmentSheet: Boolean,
        showMediaSheet: Boolean,
        showNetworkSheet: Boolean,
        connectionDetails: ConnectionDetailsUi?,
        showEditContact: Boolean,
        showBlockDialog: Boolean,
        showReportDialog: Boolean,
    ): ChatRoomState =
        ChatRoomState(
            title = conversation?.displayName.orEmpty(),
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
            ChatListConnectionStatus.CONNECTED ->
                when {
                    connection.peerOnline == true -> stringRes(R.string.chat_room_chip_online)
                    connection.dhtHealth == ChatListDhtHealth.CRITICAL -> stringRes(R.string.chat_room_chip_dht)
                    connection.peerCount > 0 -> stringRes(connection.peerCount.toString())
                    else -> stringRes(R.string.chat_list_status_connecting)
                }
            ChatListConnectionStatus.CONNECTING -> stringRes(R.string.chat_list_status_connecting)
            ChatListConnectionStatus.DISCONNECTED -> stringRes(R.string.chat_room_chip_off)
            ChatListConnectionStatus.ERROR -> stringRes(R.string.chat_room_chip_err)
        }

    private fun chipVariant(connection: ConnectionSnapshot): ChatListChipVariant =
        when (connection.status) {
            ChatListConnectionStatus.CONNECTED ->
                when {
                    connection.peerOnline == true -> ChatListChipVariant.Success
                    connection.dhtHealth == ChatListDhtHealth.CRITICAL -> ChatListChipVariant.Danger
                    connection.peerCount > 0 -> ChatListChipVariant.Success
                    else -> ChatListChipVariant.Accent
                }
            ChatListConnectionStatus.CONNECTING -> ChatListChipVariant.Accent
            ChatListConnectionStatus.DISCONNECTED,
            ChatListConnectionStatus.ERROR,
            -> ChatListChipVariant.Danger
        }

    private fun subtitleText(connection: ConnectionSnapshot): StringResource =
        when (connection.status) {
            ChatListConnectionStatus.CONNECTED ->
                when {
                    connection.peerOnline == true -> stringRes(R.string.chat_room_subtitle_peer_online)
                    connection.dhtHealth == ChatListDhtHealth.CRITICAL ->
                        stringRes(R.string.chat_room_subtitle_dht_unreachable)
                    connection.peerOnline == false -> stringRes(R.string.chat_room_subtitle_peer_offline)
                    connection.peerCount > 0 -> stringRes(R.string.chat_room_subtitle_p2p_connected)
                    connection.dhtHealth == ChatListDhtHealth.DEGRADED ->
                        stringRes(R.string.chat_room_subtitle_dht_degraded)
                    else -> stringRes(R.string.chat_room_subtitle_waiting_for_peer)
                }
            ChatListConnectionStatus.CONNECTING -> stringRes(R.string.chat_room_subtitle_connecting)
            ChatListConnectionStatus.DISCONNECTED -> stringRes(R.string.chat_room_subtitle_offline)
            ChatListConnectionStatus.ERROR -> stringRes(R.string.chat_room_subtitle_error)
        }

    // ── Sources / observers ───────────────────────────────────────────────────

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadConversation() {
        try {
            if (sdk.conversations.value.isEmpty()) sdk.refreshConversations()
        } catch (e: Exception) {
            Twig.warn(e) { "ChatRoomVM: conversations refresh failed" }
        }
        val match = sdk.conversations.value.firstOrNull { it.id == conversationId }
        conversation.value = match?.let(ChatConversation::from)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadMessages() {
        isLoading.value = true
        try {
            val list =
                sdk.getMessages(conversationId)
                    .map(ChatMessage::from)
                    .filterNot { msg -> moderationRepository.isBlocked(msg.senderName.orEmpty()) }
            messages.value = list
        } catch (e: Exception) {
            Twig.warn(e) { "ChatRoomVM: loadMessages failed" }
        } finally {
            isLoading.value = false
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

    @Suppress("CyclomaticComplexMethod")
    private fun observeMessageEvents() {
        viewModelScope.launch {
            sdk.messageReceived.collect { (incomingConvId, msg) ->
                if (incomingConvId != conversationId) return@collect
                if (moderationRepository.isBlocked(msg.senderId)) return@collect
                val mapped = ChatMessage.from(msg)
                messages.update { current -> current + mapped }
            }
        }
        viewModelScope.launch {
            sdk.messageStatus.collect { (messageId, _, status) ->
                val mapped =
                    when (status) {
                        STATUS_SENT -> MessageStatus.SENT
                        STATUS_QUEUED -> MessageStatus.QUEUED
                        STATUS_FAILED -> MessageStatus.FAILED
                        else -> null
                    } ?: return@collect
                messages.update { list ->
                    list.map { m -> if (m.id == messageId) m.copy(status = mapped) else m }
                }
            }
        }
        viewModelScope.launch {
            sdk.mediaDownloadComplete.collect { (mediaId, filePath) ->
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
        viewModelScope.launch {
            sdk.groupRenamed.collect { (renamedId, newName) ->
                if (renamedId == conversationId) {
                    conversation.update { it?.copy(displayName = newName) }
                }
            }
        }
        viewModelScope.launch {
            sdk.memberLeft.collect { (leftConvId, peer) ->
                if (leftConvId == conversationId) {
                    conversation.update { conv ->
                        conv?.copy(participantIds = conv.participantIds.filter { it != peer })
                    }
                }
            }
        }
        viewModelScope.launch {
            sdk.memberAdded.collect { (addedConvId, peer, _) ->
                if (addedConvId == conversationId) {
                    conversation.update { conv ->
                        if (conv != null && peer !in conv.participantIds) {
                            conv.copy(participantIds = conv.participantIds + peer)
                        } else {
                            conv
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            sdk.groupDeleted.collect { deletedId ->
                if (deletedId == conversationId) navigationRouter.back()
            }
        }
    }

    private fun observePeerStatus() {
        viewModelScope.launch {
            sdk.peerStatus.collect { (statusConvId, _, status) ->
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

    private fun onSendTextClick() {
        val text = messageInput.value.trim()
        if (text.isEmpty()) return
        messageInput.value = ""
        viewModelScope.launch { sendTextMessage(text) }
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
        val peerKey = conversation.value?.participantIds?.firstOrNull() ?: return
        val peerName = conversation.value?.displayName
        moderationRepository.blockUser(peerKey, peerName)
        showBlockDialog.value = false
        navigationRouter.back()
    }

    private fun onReportSubmit(category: ReportCategory, details: String) {
        val peerKey = conversation.value?.participantIds?.firstOrNull() ?: return
        val peerName = conversation.value?.displayName
        moderationRepository.submitReport(
            reportedPublicKey = peerKey,
            reportedDisplayName = peerName,
            category = category,
            details = details,
            conversationId = conversationId,
            messageId = null,
        )
    }

    private fun onReportDialogBlock() {
        val peerKey = conversation.value?.participantIds?.firstOrNull() ?: return
        val peerName = conversation.value?.displayName
        moderationRepository.blockUser(peerKey, peerName)
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

    @Suppress("TooGenericExceptionCaught")
    private suspend fun sendTextMessage(text: String) {
        try {
            val zmMessage = sdk.sendMessage(conversationId, text)
            messages.update { it + ChatMessage.from(zmMessage) }
        } catch (e: Exception) {
            Twig.warn(e) { "ChatRoomVM: sendMessage failed" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun sendMediaFromUri(uri: Uri) {
        try {
            withContext(Dispatchers.IO) {
                val mimeType = FileUtils.getMimeType(application, uri)
                val thumbnail =
                    if (mimeType.startsWith("image/")) {
                        ImageProcessor.generateThumbnail(application, uri)
                    } else {
                        null
                    }
                if (mimeType.startsWith("image/")) {
                    val compressed =
                        ImageProcessor.compressImage(application, uri)
                            ?: error("Image compression failed")
                    sendMediaMessage(compressed.absolutePath, IMAGE_MIME, thumbnailData = thumbnail)
                } else {
                    val cached =
                        FileUtils.copyUriToCache(application, uri) ?: error("Failed to cache media")
                    sendMediaMessage(cached.absolutePath, mimeType, thumbnailData = thumbnail)
                }
            }
        } catch (e: Exception) {
            Twig.warn(e) { "ChatRoomVM: sendMedia failed" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun sendFileFromUri(uri: Uri) {
        try {
            withContext(Dispatchers.IO) {
                val cached =
                    FileUtils.copyUriToCache(application, uri) ?: error("Failed to cache file")
                val mimeType = FileUtils.getMimeType(application, uri)
                val fileName = FileUtils.getFileName(application, uri) ?: FILE_FALLBACK_NAME
                val thumbnail =
                    if (mimeType.startsWith("image/")) {
                        ImageProcessor.generateThumbnail(application, uri)
                    } else {
                        null
                    }
                sendMediaMessage(cached.absolutePath, mimeType, fileName, thumbnail)
            }
        } catch (e: Exception) {
            Twig.warn(e) { "ChatRoomVM: sendFile failed" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun sendCameraCapture(uri: Uri) {
        try {
            withContext(Dispatchers.IO) {
                val thumbnail = ImageProcessor.generateThumbnail(application, uri)
                val compressed =
                    ImageProcessor.compressImage(application, uri)
                        ?: error("Image compression failed")
                sendMediaMessage(compressed.absolutePath, IMAGE_MIME, thumbnailData = thumbnail)
            }
        } catch (e: Exception) {
            Twig.warn(e) { "ChatRoomVM: sendCameraCapture failed" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun sendMediaMessage(
        mediaPath: String,
        contentType: String,
        caption: String = "",
        thumbnailData: String? = null,
    ) {
        try {
            val zmMessage = sdk.sendMediaMessage(conversationId, mediaPath, contentType, caption, thumbnailData)
            messages.update { it + ChatMessage.from(zmMessage) }
        } catch (e: Exception) {
            Twig.warn(e) { "ChatRoomVM: sendMediaMessage failed" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun sendLocationMessage(latitude: Double, longitude: Double, accuracy: Float) {
        try {
            val content =
                JSONObject()
                    .apply {
                        put("latitude", latitude)
                        put("longitude", longitude)
                        put("accuracy", accuracy.toDouble())
                    }.toString()
            val zmMessage = sdk.sendMessage(conversationId, content, LOCATION_MIME)
            messages.update { it + ChatMessage.from(zmMessage) }
        } catch (e: Exception) {
            Twig.warn(e) { "ChatRoomVM: sendLocationMessage failed" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun shareWalletAddress() {
        try {
            val address = getZashiAccount().unified.address.address
            val zmMessage = sdk.sendMessage(conversationId, address, WALLET_ADDRESS_MIME)
            messages.update { it + ChatMessage.from(zmMessage) }
        } catch (e: Exception) {
            Twig.warn(e) { "ChatRoomVM: shareWalletAddress failed" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchConnectionDetails() {
        try {
            connectionDetails.value = ConnectionDetailsUi.from(sdk.getConnectionDetails())
        } catch (e: Exception) {
            Twig.warn(e) { "ChatRoomVM: getConnectionDetails failed" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun updateContact(publicKey: String, newName: String) {
        try {
            sdk.updateContact(publicKey, newName)
            conversation.update { it?.copy(displayName = newName) }
        } catch (e: Exception) {
            Twig.warn(e) { "ChatRoomVM: updateContact failed" }
        }
    }

    private fun resolvePeerWalletAddress(): String? =
        messages.value.lastOrNull { msg ->
            msg.contentType == WALLET_ADDRESS_MIME && !msg.isFromMe
        }?.content?.takeIf { it.isNotBlank() }

    companion object {
        const val STATUS_SENT = "sent"
        const val STATUS_QUEUED = "queued"
        const val STATUS_FAILED = "failed"
        const val PEER_STATUS_ONLINE = "online"
        const val IMAGE_MIME = "image/jpeg"
        const val LOCATION_MIME = "application/location"
        const val WALLET_ADDRESS_MIME = "application/wallet-address"
        const val FILE_FALLBACK_NAME = "File"
    }
}
