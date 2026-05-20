package co.electriccoin.zcash.ui.screen.chat.contactedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.chat.ContactEditArgs
import co.electriccoin.zcash.ui.screen.chat.common.runChatCall
import co.electriccoin.zcash.ui.screen.chat.model.ChatContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.justzappit.zappmessaging.ZappMessagingSDK

class ContactEditVM(
    args: ContactEditArgs,
    private val sdk: ZappMessagingSDK,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val publicKey: String = args.publicKey
    private val nameInput = MutableStateFlow("")
    private val showDeleteDialog = MutableStateFlow(false)
    private val contact = MutableStateFlow<ChatContact?>(null)
    private val contactLoaded = MutableStateFlow(false)

    init {
        viewModelScope.launch { loadContact() }
    }

    val state: StateFlow<ContactEditState> =
        combine(
            contact,
            contactLoaded,
            nameInput,
            showDeleteDialog,
        ) { c, loaded, name, showDelete ->
            createState(c, loaded, name, showDelete)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = createState(c = null, loaded = false, name = "", showDelete = false),
        )

    private fun createState(
        c: ChatContact?,
        loaded: Boolean,
        name: String,
        showDelete: Boolean,
    ): ContactEditState =
        ContactEditState(
            title = stringRes(R.string.chat_contact_edit_title),
            publicKey = publicKey,
            nameInput = name,
            isContactFound = c != null || !loaded,
            onNameChange = ::onNameChange,
            onSave = ::onSave,
            canSave = name.isNotBlank() && c != null,
            onDeleteClick = ::onDeleteClick,
            onBack = ::onBack,
            deleteDialog =
                if (showDelete) {
                    ContactEditDeleteDialogState(
                        onConfirm = ::onDeleteConfirm,
                        onDismiss = ::dismissDeleteDialog,
                    )
                } else {
                    null
                },
        )

    private suspend fun loadContact() {
        try {
            runChatCall("ContactEditVM: loadContact failed") {
                if (sdk.contacts.value.isEmpty()) sdk.refreshContacts()
                val found = sdk.contacts.value.firstOrNull { it.publicKey == publicKey }
                contact.value = found?.let(ChatContact::from)
                nameInput.value = contact.value?.name.orEmpty()
            }
        } finally {
            contactLoaded.value = true
        }
    }

    private fun onNameChange(value: String) {
        nameInput.value = value
    }

    private fun onSave() {
        val trimmed = nameInput.value.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { updateContact(trimmed) }
    }

    private suspend fun updateContact(name: String) {
        runChatCall("ContactEditVM: updateContact failed") {
            sdk.updateContact(publicKey, name)
            navigationRouter.back()
        }
    }

    private fun onDeleteClick() {
        showDeleteDialog.value = true
    }

    private fun dismissDeleteDialog() {
        showDeleteDialog.value = false
    }

    private fun onDeleteConfirm() {
        showDeleteDialog.value = false
        viewModelScope.launch { deleteContact() }
    }

    private suspend fun deleteContact() {
        runChatCall("ContactEditVM: deleteContact failed") {
            sdk.deleteContact(publicKey)
            navigationRouter.back()
        }
    }

    private fun onBack() = navigationRouter.back()
}
