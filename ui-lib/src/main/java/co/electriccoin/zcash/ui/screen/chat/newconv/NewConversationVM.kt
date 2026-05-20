package co.electriccoin.zcash.ui.screen.chat.newconv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.usecase.NavigateToScanPublicKeyUseCase
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.chat.ChatRoomArgs
import co.electriccoin.zcash.ui.screen.chat.model.ChatContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.justzappit.zappmessaging.ZappMessagingSDK
import xyz.justzappit.zappmessaging.models.ConversationType as SdkConversationType

@Suppress("TooManyFunctions")
class NewConversationVM(
    private val sdk: ZappMessagingSDK,
    private val navigateToScanPublicKey: NavigateToScanPublicKeyUseCase,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val searchInput = MutableStateFlow("")
    private val selectedParticipants = MutableStateFlow<List<SelectedParticipant>>(emptyList())
    private val isCreating = MutableStateFlow(false)
    private val contacts = MutableStateFlow<List<ChatContact>>(emptyList())

    init {
        viewModelScope.launch { refreshContacts() }
    }

    val state: StateFlow<NewConversationState> =
        combine(
            searchInput,
            selectedParticipants,
            contacts,
            isCreating,
        ) { input, participants, contactList, creating ->
            createState(input, participants, contactList, creating)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue =
                createState(
                    input = "",
                    participants = emptyList(),
                    contactList = emptyList(),
                    creating = false,
                ),
        )

    private fun createState(
        input: String,
        participants: List<SelectedParticipant>,
        contactList: List<ChatContact>,
        creating: Boolean,
    ): NewConversationState {
        val trimmed = input.trim()
        val cleanedSearch = trimmed.removePrefix("0x")
        val isPublicKey =
            cleanedSearch.length == PUBLIC_KEY_HEX_LENGTH &&
                cleanedSearch.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

        val filtered =
            if (trimmed.isEmpty()) {
                contactList
            } else {
                contactList.filter {
                    it.name.contains(trimmed, ignoreCase = true) ||
                        it.publicKey.contains(trimmed, ignoreCase = true)
                }
            }

        val showEmptyState = trimmed.isEmpty() && participants.isEmpty()
        val canStartChat = participants.isNotEmpty() && !creating

        val primaryAction =
            if (canStartChat) {
                NewConversationPrimaryAction.StartChat(
                    isCreating = creating,
                    onClick = ::onStartChatClick,
                )
            } else {
                NewConversationPrimaryAction.ScanQr(onClick = ::onScanQrClick)
            }

        return NewConversationState(
            title = stringRes(R.string.chat_new_conversation_title),
            searchInput = input,
            onSearchInputChange = ::onSearchInputChange,
            onClearSearch = ::onClearSearch,
            isPublicKeyDetected =
                isPublicKey && participants.none { it.publicKey == cleanedSearch },
            detectedPublicKey = cleanedSearch,
            onAddDetectedKey = ::onAddDetectedKey,
            selectedParticipants =
                participants.map { p ->
                    NewConversationParticipantChip(
                        publicKey = p.publicKey,
                        displayName = p.displayName,
                        onRemove = { removeParticipant(p.publicKey) },
                    )
                },
            contacts =
                filtered
                    .sortedBy { it.name.lowercase() }
                    .map { c ->
                        val selected = participants.any { it.publicKey == c.publicKey }
                        NewConversationContactItem(
                            contact = c,
                            isSelected = selected,
                            onToggle = { onContactToggle(c, selected) },
                        )
                    },
            showEmptyState = showEmptyState,
            primaryAction = primaryAction,
            onBack = ::onBack,
        )
    }

    private fun onSearchInputChange(value: String) {
        searchInput.value = value
    }

    private fun onClearSearch() {
        searchInput.value = ""
    }

    private fun onAddDetectedKey() {
        val cleaned = searchInput.value.trim().removePrefix("0x")
        if (cleaned.length != PUBLIC_KEY_HEX_LENGTH) return
        val existing = contacts.value.firstOrNull { it.publicKey == cleaned }
        val displayName = existing?.name ?: "${cleaned.take(KEY_PREVIEW_HEAD)}..."
        addParticipant(cleaned, displayName)
        searchInput.value = ""
    }

    private fun addParticipant(publicKey: String, displayName: String) {
        if (selectedParticipants.value.any { it.publicKey == publicKey }) return
        selectedParticipants.value =
            selectedParticipants.value + SelectedParticipant(publicKey, displayName)
    }

    private fun removeParticipant(publicKey: String) {
        selectedParticipants.value = selectedParticipants.value.filter { it.publicKey != publicKey }
    }

    private fun onContactToggle(contact: ChatContact, isSelected: Boolean) {
        if (isSelected) {
            removeParticipant(contact.publicKey)
        } else {
            addParticipant(contact.publicKey, contact.name)
        }
    }

    private fun onStartChatClick() {
        if (isCreating.value) return
        val first = selectedParticipants.value.firstOrNull() ?: return
        viewModelScope.launch { createDirectChat(first.publicKey, first.displayName) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun createDirectChat(publicKey: String, displayName: String) {
        isCreating.value = true
        try {
            val conv =
                sdk.createConversation(
                    type = SdkConversationType.DIRECT,
                    participants = listOf(publicKey),
                    displayName = displayName,
                )
            navigationRouter.replace(ChatRoomArgs(conv.id))
        } catch (e: Exception) {
            Twig.warn(e) { "NewConversationVM: createConversation failed" }
        } finally {
            isCreating.value = false
        }
    }

    private fun onScanQrClick() {
        viewModelScope.launch {
            val key = navigateToScanPublicKey()
            if (!key.isNullOrBlank()) searchInput.value = key
        }
    }

    private fun onBack() = navigationRouter.back()

    @Suppress("TooGenericExceptionCaught")
    private suspend fun refreshContacts() {
        try {
            sdk.refreshContacts()
            contacts.value = sdk.contacts.value.map(ChatContact::from)
        } catch (e: Exception) {
            Twig.warn(e) { "NewConversationVM: refreshContacts failed" }
        }
    }

    private data class SelectedParticipant(val publicKey: String, val displayName: String)

    companion object {
        private const val PUBLIC_KEY_HEX_LENGTH = 64
        private const val KEY_PREVIEW_HEAD = 8
    }
}
