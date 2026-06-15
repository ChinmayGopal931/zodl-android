package co.electriccoin.zcash.ui.screen.chat.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.preference.StandardPreferenceProvider
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.usecase.CopyToClipboardUseCase
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.preference.StandardPreferenceKeys
import co.electriccoin.zcash.ui.screen.chat.ChatContactsArgs
import co.electriccoin.zcash.ui.screen.chat.ChatProfileArgs
import co.electriccoin.zcash.ui.screen.chat.common.runChatCall
import co.electriccoin.zcash.ui.screen.chat.list.ChatListConnectionStatus
import co.electriccoin.zcash.ui.screen.chat.list.ChatListDhtHealth
import co.electriccoin.zcash.ui.screen.chat.list.mapDhtHealth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.justzappit.zappmessaging.ZappMessagingSDK

@Suppress("TooManyFunctions")
class ChatSettingsVM(
    private val copyToClipboard: CopyToClipboardUseCase,
    private val sdk: ZappMessagingSDK,
    private val navigationRouter: NavigationRouter,
    private val standardPreferenceProvider: StandardPreferenceProvider,
) : ViewModel() {
    private val connectionStatus = MutableStateFlow(ChatListConnectionStatus.CONNECTING)
    private val peerCount = MutableStateFlow(0)
    private val dhtHealth = MutableStateFlow(ChatListDhtHealth.HEALTHY)

    private val showEditNameDialog = MutableStateFlow(false)
    private val editNameInput = MutableStateFlow("")
    private val showDeleteConfirm = MutableStateFlow(false)
    private val isPublicKeyCopied = MutableStateFlow(false)
    private var copyResetJob: Job? = null

    private val identity =
        sdk.identity
            .map { id -> id?.let { ChatSettingsIdentity(displayName = it.displayName, publicKey = it.publicKey) } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
                initialValue = null,
            )

    private val notificationsEnabled: StateFlow<Boolean> =
        flow {
            emitAll(StandardPreferenceKeys.IS_CHAT_NOTIFICATIONS_ENABLED.observe(standardPreferenceProvider()))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = true,
        )

    init {
        observeConnection()
    }

    val state: StateFlow<ChatSettingsState> =
        combine(
            combine(identity, isPublicKeyCopied) { id, copied -> id to copied },
            combine(connectionStatus, peerCount, dhtHealth) { cs, pc, dh -> Triple(cs, pc, dh) },
            combine(showEditNameDialog, editNameInput, showDeleteConfirm) { edit, input, del ->
                Triple(edit, input, del)
            },
            notificationsEnabled,
        ) { (id, copied), (cs, pc, dh), (editDlg, editInput, delDlg), notifEnabled ->
            createState(id, copied, cs, pc, dh, editDlg, editInput, delDlg, notifEnabled)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue =
                createState(
                    id = null,
                    copied = false,
                    cs = ChatListConnectionStatus.CONNECTING,
                    pc = 0,
                    dh = ChatListDhtHealth.HEALTHY,
                    editDlg = false,
                    editInput = "",
                    delDlg = false,
                    notifEnabled = true,
                ),
        )

    private fun createState(
        id: ChatSettingsIdentity?,
        copied: Boolean,
        cs: ChatListConnectionStatus,
        pc: Int,
        dh: ChatListDhtHealth,
        editDlg: Boolean,
        editInput: String,
        delDlg: Boolean,
        notifEnabled: Boolean,
    ): ChatSettingsState =
        ChatSettingsState(
            title = stringRes(R.string.chat_settings_title),
            deleteLabel = stringRes(R.string.chat_settings_delete_button),
            displayName = id?.displayName,
            publicKey = id?.publicKey,
            isPublicKeyCopied = copied,
            connectionStatus = cs,
            dhtHealth = dh,
            peerCount = pc,
            notificationsEnabled = notifEnabled,
            onProfileClick = ::onProfileClick,
            onContactsClick = ::onContactsClick,
            onEditDisplayNameClick = ::onEditDisplayNameClick,
            onCopyPublicKeyClick = ::onCopyPublicKeyClick,
            onDeleteClick = ::onDeleteClick,
            onNotificationsToggle = ::onNotificationsToggle,
            onBack = ::onBack,
            editNameDialog =
                if (editDlg) {
                    ChatSettingsEditNameDialogState(
                        value = editInput,
                        canSave = editInput.isNotBlank(),
                        onValueChange = ::onEditNameInputChange,
                        onSave = ::onEditNameSave,
                        onDismiss = ::dismissEditNameDialog,
                    )
                } else {
                    null
                },
            deleteDialog =
                if (delDlg) {
                    ChatSettingsDeleteDialogState(
                        onConfirm = ::onDeleteConfirm,
                        onDismiss = ::dismissDeleteDialog,
                    )
                } else {
                    null
                },
        )

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

    private fun onBack() = navigationRouter.back()

    private fun onProfileClick() = navigationRouter.forward(ChatProfileArgs)

    private fun onContactsClick() = navigationRouter.forward(ChatContactsArgs)

    private fun onEditDisplayNameClick() {
        editNameInput.value = identity.value?.displayName.orEmpty()
        showEditNameDialog.value = true
    }

    private fun onEditNameInputChange(value: String) {
        editNameInput.value = value
    }

    private fun onEditNameSave() {
        val trimmed = editNameInput.value.trim()
        if (trimmed.isEmpty()) return
        showEditNameDialog.value = false
        viewModelScope.launch { updateDisplayName(trimmed) }
    }

    private suspend fun updateDisplayName(name: String) {
        runChatCall("ChatSettingsVM: updateDisplayName failed") {
            sdk.updateDisplayName(name)
        }
    }

    private fun dismissEditNameDialog() {
        showEditNameDialog.value = false
    }

    private fun onDeleteClick() {
        showDeleteConfirm.value = true
    }

    private fun dismissDeleteDialog() {
        showDeleteConfirm.value = false
    }

    private fun onDeleteConfirm() {
        showDeleteConfirm.value = false
        viewModelScope.launch { performDeleteIdentity() }
    }

    private suspend fun performDeleteIdentity() {
        runChatCall("ChatSettingsVM: sdk.shutdown failed") {
            sdk.shutdown()
        }
        navigationRouter.backToRoot()
    }

    private fun onCopyPublicKeyClick() {
        val pk = identity.value?.publicKey ?: return
        copyToClipboard(pk)
        isPublicKeyCopied.value = true
        copyResetJob?.cancel()
        copyResetJob =
            viewModelScope.launch {
                delay(COPY_FEEDBACK_MS)
                isPublicKeyCopied.value = false
            }
    }

    private fun onNotificationsToggle() {
        viewModelScope.launch {
            StandardPreferenceKeys.IS_CHAT_NOTIFICATIONS_ENABLED.putValue(
                preferenceProvider = standardPreferenceProvider(),
                newValue = !notificationsEnabled.value,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        copyResetJob?.cancel()
    }

    private data class ChatSettingsIdentity(
        val displayName: String,
        val publicKey: String
    )

    companion object {
        private const val COPY_FEEDBACK_MS = 2_000L
    }
}
