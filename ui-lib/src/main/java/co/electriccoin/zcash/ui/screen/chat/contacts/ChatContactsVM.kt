package co.electriccoin.zcash.ui.screen.chat.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.repository.AddressBookRepository
import co.electriccoin.zcash.ui.common.usecase.NavigateToScanGenericAddressUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToScanPublicKeyUseCase
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.chat.ChatRoomArgs
import co.electriccoin.zcash.ui.screen.chat.common.runChatCall
import co.electriccoin.zcash.ui.screen.chat.model.ChatContact
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.justzappit.zappmessaging.ZappMessagingSDK
import xyz.justzappit.zappmessaging.models.ConversationType as SdkConversationType

@Suppress("TooManyFunctions")
class ChatContactsVM(
    private val sdk: ZappMessagingSDK,
    private val addressBookRepository: AddressBookRepository,
    private val navigateToScanPublicKey: NavigateToScanPublicKeyUseCase,
    private val navigateToScanGenericAddress: NavigateToScanGenericAddressUseCase,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val contacts = MutableStateFlow<List<ChatContact>>(emptyList())
    private val scannedPublicKey = MutableStateFlow<String?>(null)
    private val scannedWalletAddress = MutableStateFlow<String?>(null)

    // Per-sheet VMs. The parent owns these because the sheets share its scan
    // bridge and contact list — see AddChatContactVM / EditChatContactVM kdoc.
    private val addSheet = MutableStateFlow<AddChatContactVM?>(null)
    private val editSheet = MutableStateFlow<EditChatContactVM?>(null)

    init {
        viewModelScope.launch { refreshContacts() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val addSheetState: StateFlow<AddChatContactState?> =
        addSheet
            .flatMapLatest { it?.state ?: flowOf(null) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
                initialValue = null,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val editSheetState: StateFlow<EditChatContactState?> =
        editSheet
            .flatMapLatest { it?.state ?: flowOf(null) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
                initialValue = null,
            )

    val state: StateFlow<ChatContactsState> =
        combine(
            contacts,
            addSheetState,
            editSheetState,
        ) { list, add, edit ->
            createState(list, add, edit)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = createState(emptyList(), null, null),
        )

    private fun createState(
        list: List<ChatContact>,
        add: AddChatContactState?,
        edit: EditChatContactState?,
    ): ChatContactsState =
        ChatContactsState(
            title = stringRes(R.string.chat_contacts_title),
            contacts = list,
            onStartChat = ::onStartChat,
            onAddSheetOpen = ::openAddSheet,
            onEditSheetOpen = ::openEditSheet,
            onBack = ::onBack,
            addSheet = add,
            editSheet = edit,
        )

    private fun onBack() = navigationRouter.back()

    private fun onStartChat(publicKey: String) {
        viewModelScope.launch { createDirectChat(publicKey) }
    }

    private suspend fun createDirectChat(publicKey: String) {
        val cleaned = publicKey.trim().removePrefix("0x")
        runChatCall("ChatContactsVM: createConversation failed") {
            val conv =
                sdk.createConversation(
                    type = SdkConversationType.DIRECT,
                    participants = listOf(cleaned),
                    displayName = null,
                )
            navigationRouter.forward(ChatRoomArgs(conv.id))
        }
    }

    private fun openAddSheet() {
        if (addSheet.value != null) return
        addSheet.value =
            AddChatContactVM(
                scope = viewModelScope,
                existingKeysProvider = { contacts.value.map { it.publicKey }.toSet() },
                scannedPublicKeyFlow = scannedPublicKey.asStateFlow(),
                scannedWalletAddressFlow = scannedWalletAddress.asStateFlow(),
                onConsumeScannedPublicKey = ::consumeScannedPublicKey,
                onConsumeScannedWalletAddress = ::consumeScannedWalletAddress,
                onScanPublicKeyRequest = ::onScanPublicKey,
                onScanWalletAddressRequest = ::onScanWalletAddress,
                onSaveContact = ::addContactFromSheet,
                onDismissRequest = ::closeAddSheet,
            )
    }

    private fun closeAddSheet() {
        addSheet.value = null
    }

    private fun openEditSheet(contact: ChatContact) {
        if (editSheet.value != null) return
        editSheet.value =
            EditChatContactVM(
                contact = contact,
                scope = viewModelScope,
                scannedWalletAddressFlow = scannedWalletAddress.asStateFlow(),
                onConsumeScannedWalletAddress = ::consumeScannedWalletAddress,
                onScanWalletAddressRequest = ::onScanWalletAddress,
                onSaveContact = ::updateContactFromSheet,
                onDeleteContact = ::deleteContactFromSheet,
                onDismissRequest = ::closeEditSheet,
            )
    }

    private fun closeEditSheet() {
        editSheet.value = null
    }

    private fun onScanPublicKey() {
        viewModelScope.launch {
            val key = navigateToScanPublicKey()
            if (!key.isNullOrBlank()) scannedPublicKey.value = key
        }
    }

    private fun onScanWalletAddress() {
        viewModelScope.launch {
            val result = navigateToScanGenericAddress()
            if (result != null) scannedWalletAddress.value = result.address
        }
    }

    private fun consumeScannedPublicKey() {
        scannedPublicKey.value = null
    }

    private fun consumeScannedWalletAddress() {
        scannedWalletAddress.value = null
    }

    private fun addContactFromSheet(
        publicKey: String,
        name: String,
        walletAddress: String,
        walletAddresses: Map<String, String>,
    ) {
        val cleaned = publicKey.trim().removePrefix("0x")
        viewModelScope.launch {
            performAddContact(cleaned, name, walletAddress, walletAddresses)
            consumeScannedPublicKey()
            consumeScannedWalletAddress()
            closeAddSheet()
        }
    }

    private suspend fun performAddContact(
        publicKey: String,
        name: String,
        walletAddress: String,
        walletAddresses: Map<String, String>,
    ) {
        runChatCall("ChatContactsVM: addContact failed") {
            sdk.addContact(publicKey, name)
            val wallet = walletAddress.trim()
            if (wallet.isNotEmpty() || walletAddresses.isNotEmpty()) {
                addressBookRepository.saveContact(
                    name = name,
                    address = wallet,
                    chain = null,
                    walletAddresses = walletAddresses,
                )
            }
            refreshContacts()
        }
    }

    private fun updateContactFromSheet(
        publicKey: String,
        name: String,
        walletAddress: String,
        walletAddresses: Map<String, String>,
    ) {
        viewModelScope.launch {
            performUpdateContact(publicKey, name, walletAddress, walletAddresses)
            closeEditSheet()
        }
    }

    private suspend fun performUpdateContact(
        publicKey: String,
        name: String,
        walletAddress: String,
        walletAddresses: Map<String, String>,
    ) {
        runChatCall("ChatContactsVM: updateContact failed") {
            sdk.updateContact(publicKey, name)
            val wallet = walletAddress.trim()
            if (wallet.isNotEmpty() || walletAddresses.isNotEmpty()) {
                addressBookRepository.saveContact(
                    name = name,
                    address = wallet,
                    chain = null,
                    walletAddresses = walletAddresses,
                )
            }
            refreshContacts()
        }
    }

    private fun deleteContactFromSheet(publicKey: String) {
        viewModelScope.launch {
            performDeleteContact(publicKey)
            closeEditSheet()
        }
    }

    private suspend fun performDeleteContact(publicKey: String) {
        runChatCall("ChatContactsVM: deleteContact failed") {
            sdk.deleteContact(publicKey)
            refreshContacts()
        }
    }

    private suspend fun refreshContacts() {
        runChatCall("ChatContactsVM: refreshContacts failed") {
            sdk.refreshContacts()
            contacts.value = sdk.contacts.value.map(ChatContact::from)
        }
    }
}
