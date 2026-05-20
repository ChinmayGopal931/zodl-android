package co.electriccoin.zcash.ui.screen.chat.contacts

import androidx.compose.ui.text.input.TextFieldValue

/**
 * UI state for the "Add new chat contact" bottom sheet.
 *
 * Mirrors the upstream `ABContactState` / `AddZashiABContactVM` pattern: the VM
 * owns every form field as a `MutableStateFlow`, exposes it through this data
 * class, and the View is purely declarative — no `remember { mutableStateOf(...) }`
 * for form data.
 *
 * The fork keeps `TextFieldValue` (rather than upstream's `TextFieldState`)
 * because the views render with `ZappInputField`, which preserves cursor and
 * selection state via `TextFieldValue`.
 */
data class AddChatContactState(
    val name: TextFieldValue,
    val publicKey: TextFieldValue,
    val walletAddress: TextFieldValue,
    val transparentAddr: TextFieldValue,
    val evmAddr: TextFieldValue,
    val solanaAddr: TextFieldValue,
    val showAdditionalAddresses: Boolean,
    val error: String?,
    val isValidKey: Boolean,
    val cleanedKey: String,
    val onNameChange: (TextFieldValue) -> Unit,
    val onPublicKeyChange: (TextFieldValue) -> Unit,
    val onWalletAddressChange: (TextFieldValue) -> Unit,
    val onTransparentAddrChange: (TextFieldValue) -> Unit,
    val onEvmAddrChange: (TextFieldValue) -> Unit,
    val onSolanaAddrChange: (TextFieldValue) -> Unit,
    val onToggleAdditionalAddresses: () -> Unit,
    val onScanPublicKey: () -> Unit,
    val onScanWalletAddress: () -> Unit,
    val onScanAddressField: (addrType: String) -> Unit,
    val onSave: () -> Unit,
    val onDismiss: () -> Unit,
)
