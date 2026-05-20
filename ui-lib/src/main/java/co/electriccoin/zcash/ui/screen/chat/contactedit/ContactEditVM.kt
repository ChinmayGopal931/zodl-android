package co.electriccoin.zcash.ui.screen.chat.contactedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.util.stringRes
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
    private val publicKey: String,
    private val sdk: ZappMessagingSDK,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val nameInput = MutableStateFlow("")
    private val showDeleteDialog = MutableStateFlow(false)
    private val contact = MutableStateFlow<ChatContact?>(null)
    private val contactLoaded = MutableStateFlow(false)

    init {
        viewModelScope.launch { loadContact() }
    }

    val state: StateFlow<ContactEditState?> =
        combine(
            contact,
            contactLoaded,
            nameInput,
            showDeleteDialog,
        ) { c, loaded, name, showDelete ->
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
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = null,
        )

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadContact() {
        try {
            if (sdk.contacts.value.isEmpty()) sdk.refreshContacts()
            val found = sdk.contacts.value.firstOrNull { it.publicKey == publicKey }
            contact.value = found?.let(ChatContact::from)
            nameInput.value = contact.value?.name.orEmpty()
        } catch (e: Exception) {
            Twig.warn(e) { "ContactEditVM: loadContact failed" }
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

    @Suppress("TooGenericExceptionCaught")
    private suspend fun updateContact(name: String) {
        try {
            sdk.updateContact(publicKey, name)
            navigationRouter.back()
        } catch (e: Exception) {
            Twig.warn(e) { "ContactEditVM: updateContact failed" }
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

    @Suppress("TooGenericExceptionCaught")
    private suspend fun deleteContact() {
        try {
            sdk.deleteContact(publicKey)
            navigationRouter.back()
        } catch (e: Exception) {
            Twig.warn(e) { "ContactEditVM: deleteContact failed" }
        }
    }

    private fun onBack() = navigationRouter.back()
}
