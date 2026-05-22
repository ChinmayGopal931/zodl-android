package co.electriccoin.zcash.ui.screen.chat.support

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.screen.chat.SupportChatArgs
import co.electriccoin.zcash.ui.screen.chat.common.runChatCall
import co.electriccoin.zcash.ui.screen.chat.media.FileUtils
import co.electriccoin.zcash.ui.screen.chat.media.ImageProcessor
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
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
import xyz.justzappit.zappmessaging.ZappMessagingSDK
import xyz.justzappit.zappmessaging.models.ConversationType as SdkConversationType

@Suppress("TooManyFunctions")
class SupportChatVM(
    args: SupportChatArgs,
    private val application: Application,
    private val sdk: ZappMessagingSDK,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<SupportUiMessage>>(emptyList())
    private val _input = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)
    private val _showLeaveDialog = MutableStateFlow(false)
    private val _showMediaSheet = MutableStateFlow(false)

    private val _effects = MutableSharedFlow<SupportChatEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<SupportChatEffect> = _effects.asSharedFlow()

    private var convId: String = args.conversationId
    private val isNewTicket = args.conversationId.isEmpty()

    init {
        viewModelScope.launch { initialize() }
    }

    val state: StateFlow<SupportChatScreenState> =
        combine(
            _messages,
            _input,
            combine(_isLoading, _showLeaveDialog, _showMediaSheet) { l, d, m -> Triple(l, d, m) },
        ) { messages, input, (loading, showLeave, showMedia) ->
            val uiState = when {
                loading -> SupportChatUiState.Loading
                isNewTicket && convId.isEmpty() -> SupportChatUiState.SelectCategory
                else -> SupportChatUiState.Chat(messages = messages, input = input)
            }
            SupportChatScreenState(
                uiState = uiState,
                onCategorySelected = ::onCategorySelected,
                onInputChange = ::onInputChange,
                onSend = ::onSendClick,
                onAttach = ::onAttachClick,
                onLeave = ::onLeaveClick,
                onBack = ::onBack,
                leaveDialog = if (showLeave) {
                    SupportLeaveDialogState(
                        onConfirm = ::onLeaveConfirm,
                        onDismiss = ::onLeaveDismiss,
                    )
                } else {
                    null
                },
                mediaSheet = if (showMedia) {
                    SupportMediaSheetState(
                        onChooseMedia = ::onChooseMediaClick,
                        onAttachFile = ::onAttachFileClick,
                        onTakePhoto = ::onTakePhotoClick,
                        onDismiss = ::onDismissMediaSheet,
                    )
                } else {
                    null
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = SupportChatScreenState(
                uiState = SupportChatUiState.Loading,
                onCategorySelected = ::onCategorySelected,
                onInputChange = ::onInputChange,
                onSend = ::onSendClick,
                onAttach = ::onAttachClick,
                onLeave = ::onLeaveClick,
                onBack = ::onBack,
                leaveDialog = null,
                mediaSheet = null,
            ),
        )

    // ── Initialization ────────────────────────────────────────────────────────

    private suspend fun initialize() {
        subscribeToIncomingMessages()
        if (!isNewTicket) loadMessages()
        _isLoading.value = false
    }

    private suspend fun loadMessages() {
        if (convId.isEmpty()) return
        runChatCall("SupportChatVM: loadMessages failed") {
            val list = sdk.getMessages(convId)
                .map(ChatMessage::from)
                .map { it.toSupportUiMessage() }
            _messages.value = list
        }
    }

    private fun subscribeToIncomingMessages() {
        viewModelScope.launch {
            sdk.messageReceived.collect { (incomingConvId, zmMsg) ->
                if (incomingConvId != convId || convId.isEmpty()) return@collect
                val msg = ChatMessage.from(zmMsg).toSupportUiMessage()
                _messages.update { current ->
                    if (current.any { it.id == msg.id }) current else current + msg
                }
            }
        }
    }

    // ── Category selection ────────────────────────────────────────────────────

    private fun onCategorySelected(category: SupportCategory) {
        viewModelScope.launch {
            runChatCall("SupportChatVM: create ticket failed") {
                val conv = sdk.createConversation(
                    type = SdkConversationType.GROUP,
                    participants = listOf(SupportChatConstants.SUPPORT_PUBLIC_KEY),
                    displayName = "Support: ${category.label}",
                )
                convId = conv.id

                val catMsg = sdk.sendMessage(
                    conv.id,
                    "${SupportChatConstants.CATEGORY_MARKER}${category.label}]",
                )
                _messages.update { it + ChatMessage.from(catMsg).toSupportUiMessage() }

                val greeting = SupportChatConstants.CATEGORY_GREETINGS[category] ?: return@runChatCall
                val greetMsg = sdk.sendMessage(
                    conv.id,
                    "${SupportChatConstants.BOT_PREFIX}$greeting",
                )
                _messages.update { it + ChatMessage.from(greetMsg).toSupportUiMessage() }
            }
        }
    }

    // ── Text input ───────────────────────────────────────────────────────────

    private fun onInputChange(value: String) {
        _input.value = value
    }

    private fun onSendClick() {
        val text = _input.value.trim()
        if (text.isEmpty() || convId.isEmpty()) return
        _input.value = ""
        viewModelScope.launch {
            runChatCall("SupportChatVM: send message failed") {
                val userMsg = sdk.sendMessage(convId, text)
                _messages.update { it + ChatMessage.from(userMsg).toSupportUiMessage() }
            }
        }
    }

    // ── Attachment / media ───────────────────────────────────────────────────

    private fun onAttachClick() {
        _showMediaSheet.value = true
    }

    private fun onChooseMediaClick() {
        _showMediaSheet.value = false
        _effects.tryEmit(SupportChatEffect.PickMedia)
    }

    private fun onAttachFileClick() {
        _showMediaSheet.value = false
        _effects.tryEmit(SupportChatEffect.PickFile)
    }

    private fun onTakePhotoClick() {
        _showMediaSheet.value = false
        _effects.tryEmit(SupportChatEffect.TakePhoto)
    }

    private fun onDismissMediaSheet() {
        _showMediaSheet.value = false
    }

    fun onMediaPicked(uri: Uri) {
        viewModelScope.launch { sendMediaFromUri(uri) }
    }

    fun onFilePicked(uri: Uri) {
        viewModelScope.launch { sendFileFromUri(uri) }
    }

    fun onCameraCaptured(uri: Uri) {
        viewModelScope.launch { sendCameraCapture(uri) }
    }

    private suspend fun sendMediaFromUri(uri: Uri) {
        if (convId.isEmpty()) return
        runChatCall("SupportChatVM: sendMedia failed") {
            withContext(Dispatchers.IO) {
                val mimeType = FileUtils.getMimeType(application, uri)
                val thumbnail =
                    if (mimeType.startsWith("image/")) {
                        ImageProcessor.generateThumbnail(application, uri)
                    } else {
                        null
                    }
                if (mimeType == GIF_MIME) {
                    val cached =
                        FileUtils.copyUriToCache(application, uri) ?: error("Failed to cache GIF")
                    sendMediaMessage(cached.absolutePath, GIF_MIME, thumbnail)
                } else if (mimeType.startsWith("image/")) {
                    val compressed =
                        ImageProcessor.compressImage(application, uri)
                            ?: error("Image compression failed")
                    sendMediaMessage(compressed.absolutePath, IMAGE_MIME, thumbnail)
                } else {
                    val cached =
                        FileUtils.copyUriToCache(application, uri) ?: error("Failed to cache media")
                    sendMediaMessage(cached.absolutePath, mimeType, thumbnail)
                }
            }
        }
    }

    private suspend fun sendFileFromUri(uri: Uri) {
        if (convId.isEmpty()) return
        runChatCall("SupportChatVM: sendFile failed") {
            withContext(Dispatchers.IO) {
                val cached =
                    FileUtils.copyUriToCache(application, uri) ?: error("Failed to cache file")
                val mimeType = FileUtils.getMimeType(application, uri)
                val thumbnail =
                    if (mimeType.startsWith("image/")) {
                        ImageProcessor.generateThumbnail(application, uri)
                    } else {
                        null
                    }
                sendMediaMessage(cached.absolutePath, mimeType, thumbnail)
            }
        }
    }

    private suspend fun sendCameraCapture(uri: Uri) {
        if (convId.isEmpty()) return
        runChatCall("SupportChatVM: sendCameraCapture failed") {
            withContext(Dispatchers.IO) {
                val thumbnail = ImageProcessor.generateThumbnail(application, uri)
                val compressed =
                    ImageProcessor.compressImage(application, uri)
                        ?: error("Image compression failed")
                sendMediaMessage(compressed.absolutePath, IMAGE_MIME, thumbnail)
            }
        }
    }

    private suspend fun sendMediaMessage(
        mediaPath: String,
        contentType: String,
        thumbnailData: String?,
    ) {
        runChatCall("SupportChatVM: sendMediaMessage failed") {
            val zmMessage = sdk.sendMediaMessage(convId, mediaPath, contentType, "", thumbnailData)
            _messages.update { it + ChatMessage.from(zmMessage).toSupportUiMessage() }
        }
    }

    // ── Leave / close ────────────────────────────────────────────────────────

    private fun onLeaveClick() {
        _showLeaveDialog.value = true
    }

    private fun onLeaveDismiss() {
        _showLeaveDialog.value = false
    }

    private fun onLeaveConfirm() {
        _showLeaveDialog.value = false
        viewModelScope.launch {
            if (convId.isNotEmpty()) {
                runChatCall("SupportChatVM: send leave notice failed") {
                    sdk.sendMessage(
                        convId,
                        "${SupportChatConstants.BOT_PREFIX}${SupportChatConstants.LEAVE_MESSAGE}",
                    )
                }
                runChatCall("SupportChatVM: removeConversation failed") {
                    sdk.removeConversation(convId)
                }
            }
            navigationRouter.back()
        }
    }

    private fun onBack() = navigationRouter.back()

    companion object {
        private const val IMAGE_MIME = "image/jpeg"
        private const val GIF_MIME = "image/gif"
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun ChatMessage.toSupportUiMessage(): SupportUiMessage {
    val isBot = isFromMe && content.startsWith(SupportChatConstants.BOT_PREFIX)
    return SupportUiMessage(
        id = id,
        content = if (isBot) content.removePrefix(SupportChatConstants.BOT_PREFIX) else content,
        isFromMe = if (isBot) false else isFromMe,
        timestamp = timestamp,
    )
}
