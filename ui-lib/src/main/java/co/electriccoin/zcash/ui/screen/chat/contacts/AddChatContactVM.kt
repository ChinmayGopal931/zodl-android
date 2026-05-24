package co.electriccoin.zcash.ui.screen.chat.contacts

import androidx.compose.ui.text.input.TextFieldValue
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.model.AddressBookContact
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Per-sheet ViewModel for the "Add new chat contact" flow. Owns every form
 * field as a `MutableStateFlow`, exposing them through [AddChatContactState].
 *
 * Follows the upstream `AddZashiABContactVM` pattern (state.stateIn with
 * `SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT)`), adapted for
 * this fork:
 *  - hosted inside [ChatContactsVM] (not via Koin), because the sheet needs
 *    direct access to the parent's scan bridge and contact list;
 *  - uses `TextFieldValue` for fields (the views render with `ZappInputField`).
 *
 * Save and scan actions are wired by the parent VM via constructor callbacks.
 */
class AddChatContactVM(
    private val scope: CoroutineScope,
    private val existingKeysProvider: () -> Set<String>,
    private val scannedPublicKeyFlow: StateFlow<String?>,
    private val scannedWalletAddressFlow: StateFlow<String?>,
    private val onConsumeScannedPublicKey: () -> Unit,
    private val onConsumeScannedWalletAddress: () -> Unit,
    private val onScanPublicKeyRequest: () -> Unit,
    private val onScanWalletAddressRequest: () -> Unit,
    private val onSaveContact: (publicKey: String, name: String, walletAddress: String, walletAddresses: Map<String, String>) -> Unit,
    private val onDismissRequest: () -> Unit,
) {
    private val name = MutableStateFlow(TextFieldValue(""))
    private val publicKey = MutableStateFlow(TextFieldValue(""))
    private val walletAddress = MutableStateFlow(TextFieldValue(""))
    private val transparentAddr = MutableStateFlow(TextFieldValue(""))
    private val evmAddr = MutableStateFlow(TextFieldValue(""))
    private val solanaAddr = MutableStateFlow(TextFieldValue(""))
    private val showAdditionalAddresses = MutableStateFlow(false)
    private val error = MutableStateFlow<StringResource?>(null)

    // When the user taps a per-field scan icon (transparent / evm / solana),
    // we record which field is awaiting the result. `null` means the next
    // scanned wallet address goes to the primary `walletAddress` field.
    private val scanTargetField = MutableStateFlow<String?>(null)

    init {
        // Bridge scanned public key -> publicKey field, then consume.
        scannedPublicKeyFlow
            .onEach { key ->
                if (!key.isNullOrEmpty()) {
                    publicKey.value = TextFieldValue(key)
                    error.value = null
                    onConsumeScannedPublicKey()
                }
            }.launchIn(scope)

        // Bridge scanned wallet address. Route to either the per-field address
        // (transparent/evm/solana) or the primary wallet address field.
        scannedWalletAddressFlow
            .onEach { addr ->
                if (!addr.isNullOrEmpty()) {
                    val target = scanTargetField.value
                    if (target != null) {
                        val tfv = TextFieldValue(addr)
                        when (target) {
                            AddressBookContact.ADDR_TYPE_TRANSPARENT -> transparentAddr.value = tfv
                            AddressBookContact.ADDR_TYPE_EVM -> evmAddr.value = tfv
                            AddressBookContact.ADDR_TYPE_SOLANA -> solanaAddr.value = tfv
                        }
                        showAdditionalAddresses.value = true
                        scanTargetField.value = null
                    } else {
                        walletAddress.value = TextFieldValue(addr)
                        error.value = null
                    }
                    onConsumeScannedWalletAddress()
                }
            }.launchIn(scope)
    }

    val state: StateFlow<AddChatContactState> =
        combine(
            combine(name, publicKey, walletAddress) { n, p, w -> Triple(n, p, w) },
            combine(transparentAddr, evmAddr, solanaAddr) { t, e, s -> Triple(t, e, s) },
            showAdditionalAddresses,
            error,
        ) { primary, extras, showExtras, err ->
            val (nameVal, pkVal, walletVal) = primary
            val (tAddr, eAddr, sAddr) = extras
            val cleanedKey = pkVal.text.trim().removePrefix("0x")
            val isValidKey =
                cleanedKey.length == 64 &&
                    cleanedKey.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
            AddChatContactState(
                name = nameVal,
                publicKey = pkVal,
                walletAddress = walletVal,
                transparentAddr = tAddr,
                evmAddr = eAddr,
                solanaAddr = sAddr,
                showAdditionalAddresses = showExtras,
                error = err,
                isValidKey = isValidKey,
                cleanedKey = cleanedKey,
                onNameChange = ::onNameChange,
                onPublicKeyChange = ::onPublicKeyChange,
                onWalletAddressChange = ::onWalletAddressChange,
                onTransparentAddrChange = ::onTransparentAddrChange,
                onEvmAddrChange = ::onEvmAddrChange,
                onSolanaAddrChange = ::onSolanaAddrChange,
                onToggleAdditionalAddresses = ::onToggleAdditionalAddresses,
                onScanPublicKey = onScanPublicKeyRequest,
                onScanWalletAddress = ::onScanPrimaryWalletAddress,
                onScanAddressField = ::onScanAddressField,
                onSave = ::onSave,
                onDismiss = onDismissRequest,
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = initialState(),
        )

    private fun initialState() =
        AddChatContactState(
            name = TextFieldValue(""),
            publicKey = TextFieldValue(""),
            walletAddress = TextFieldValue(""),
            transparentAddr = TextFieldValue(""),
            evmAddr = TextFieldValue(""),
            solanaAddr = TextFieldValue(""),
            showAdditionalAddresses = false,
            error = null,
            isValidKey = false,
            cleanedKey = "",
            onNameChange = ::onNameChange,
            onPublicKeyChange = ::onPublicKeyChange,
            onWalletAddressChange = ::onWalletAddressChange,
            onTransparentAddrChange = ::onTransparentAddrChange,
            onEvmAddrChange = ::onEvmAddrChange,
            onSolanaAddrChange = ::onSolanaAddrChange,
            onToggleAdditionalAddresses = ::onToggleAdditionalAddresses,
            onScanPublicKey = onScanPublicKeyRequest,
            onScanWalletAddress = ::onScanPrimaryWalletAddress,
            onScanAddressField = ::onScanAddressField,
            onSave = ::onSave,
            onDismiss = onDismissRequest,
        )

    private fun onNameChange(value: TextFieldValue) {
        name.value = value
        error.value = null
    }

    private fun onPublicKeyChange(value: TextFieldValue) {
        publicKey.value = value
        error.value = null
    }

    private fun onWalletAddressChange(value: TextFieldValue) {
        walletAddress.value = value
        error.value = null
    }

    private fun onTransparentAddrChange(value: TextFieldValue) {
        transparentAddr.value = value
    }

    private fun onEvmAddrChange(value: TextFieldValue) {
        evmAddr.value = value
    }

    private fun onSolanaAddrChange(value: TextFieldValue) {
        solanaAddr.value = value
    }

    private fun onToggleAdditionalAddresses() {
        showAdditionalAddresses.update { !it }
    }

    private fun onScanPrimaryWalletAddress() {
        scanTargetField.value = null
        onScanWalletAddressRequest()
    }

    private fun onScanAddressField(addrType: String) {
        scanTargetField.value = addrType
        onScanWalletAddressRequest()
    }

    private fun onSave() {
        val pk =
            publicKey.value.text
                .trim()
                .removePrefix("0x")
        val nameVal = name.value.text.trim()
        val wallet = walletAddress.value.text.trim()
        val isValidHex =
            pk.length == 64 &&
                pk.all {
                    it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
                }
        val addrs =
            buildMap {
                if (transparentAddr.value.text.isNotBlank()) {
                    put(AddressBookContact.ADDR_TYPE_TRANSPARENT, transparentAddr.value.text.trim())
                }
                if (evmAddr.value.text.isNotBlank()) {
                    put(AddressBookContact.ADDR_TYPE_EVM, evmAddr.value.text.trim())
                }
                if (solanaAddr.value.text.isNotBlank()) {
                    put(AddressBookContact.ADDR_TYPE_SOLANA, solanaAddr.value.text.trim())
                }
            }
        when {
            nameVal.isEmpty() -> error.value = stringRes(R.string.chat_contact_error_name_required)
            pk.isEmpty() -> error.value = stringRes(R.string.chat_contact_error_messaging_key_required)
            !isValidHex -> error.value = stringRes(R.string.chat_contact_error_invalid_messaging_key)
            existingKeysProvider().contains(pk) -> error.value = stringRes(R.string.chat_contact_error_already_exists)
            else -> onSaveContact(pk, nameVal, wallet, addrs)
        }
    }
}
