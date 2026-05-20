package co.electriccoin.zcash.ui.screen.chat.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.repository.AddressBookRepository
import co.electriccoin.zcash.ui.common.usecase.NavigateToScanGenericAddressUseCase
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

@Suppress("TooManyFunctions", "LongParameterList")
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
    private val showBackButton = MutableStateFlow(true)

    init {
        viewModelScope.launch { refreshContacts() }
    }

    /**
     * Hosted contexts can opt out of the back chevron (used by the tabs scaffold
     * which routes navigation through the tab bar instead).
     */
    fun setShowBackButton(value: Boolean) {
        showBackButton.value = value
    }

    val state: StateFlow<ChatContactsState?> =
        combine(
            contacts,
            scannedPublicKey,
            scannedWalletAddress,
            showBackButton,
        ) { list, scannedKey, scannedAddress, showBack ->
            ChatContactsState(
                title = stringRes(R.string.chat_contacts_title),
                contacts = list,
                scannedPublicKey = scannedKey,
                scannedWalletAddress = scannedAddress,
                showBackButton = showBack,
                onStartChat = ::onStartChat,
                onScanPublicKey = ::onScanPublicKey,
                onScanWalletAddress = ::onScanWalletAddress,
                onConsumeScannedPublicKey = ::consumeScannedPublicKey,
                onConsumeScannedWalletAddress = ::consumeScannedWalletAddress,
                onAddContact = ::addContact,
                onUpdateContact = ::updateContact,
                onDeleteContact = ::deleteContact,
                onBack = ::onBack,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = null,
        )

    private fun onBack() = navigationRouter.back()

    private fun onStartChat(publicKey: String) {
        viewModelScope.launch { createDirectChat(publicKey) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun createDirectChat(publicKey: String) {
        val cleaned = publicKey.trim().removePrefix("0x")
        try {
            val conv =
                sdk.createConversation(
                    type = SdkConversationType.DIRECT,
                    participants = listOf(cleaned),
                    displayName = null,
                )
            navigationRouter.forward(ChatRoomArgs(conv.id))
        } catch (e: Exception) {
            Twig.warn(e) { "ChatContactsVM: createConversation failed" }
        }
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

    private fun addContact(
        publicKey: String,
        name: String,
        walletAddress: String,
        walletAddresses: Map<String, String>,
    ) {
        val cleaned = publicKey.trim().removePrefix("0x")
        viewModelScope.launch { performAddContact(cleaned, name, walletAddress, walletAddresses) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun performAddContact(
        publicKey: String,
        name: String,
        walletAddress: String,
        walletAddresses: Map<String, String>,
    ) {
        try {
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
        } catch (e: Exception) {
            Twig.warn(e) { "ChatContactsVM: addContact failed" }
        }
    }

    private fun updateContact(
        publicKey: String,
        name: String,
        walletAddress: String,
        walletAddresses: Map<String, String>,
    ) {
        viewModelScope.launch { performUpdateContact(publicKey, name, walletAddress, walletAddresses) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun performUpdateContact(
        publicKey: String,
        name: String,
        walletAddress: String,
        walletAddresses: Map<String, String>,
    ) {
        try {
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
        } catch (e: Exception) {
            Twig.warn(e) { "ChatContactsVM: updateContact failed" }
        }
    }

    private fun deleteContact(publicKey: String) {
        viewModelScope.launch { performDeleteContact(publicKey) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun performDeleteContact(publicKey: String) {
        try {
            sdk.deleteContact(publicKey)
            refreshContacts()
        } catch (e: Exception) {
            Twig.warn(e) { "ChatContactsVM: deleteContact failed" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun refreshContacts() {
        try {
            sdk.refreshContacts()
            contacts.value = sdk.contacts.value.map(ChatContact::from)
        } catch (e: Exception) {
            Twig.warn(e) { "ChatContactsVM: refreshContacts failed" }
        }
    }
}
