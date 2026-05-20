package co.electriccoin.zcash.ui.screen.chat.contacts

import androidx.compose.ui.text.input.TextFieldValue

/**
 * UI state for the "Edit chat contact" bottom sheet.
 *
 * The VM owns all form state; the View is purely declarative. Same shape as
 * [AddChatContactState] but with an immutable `publicKey` (cannot be edited
 * once a contact is created) and a delete-confirmation flag.
 */
data class EditChatContactState(
    val publicKey: String,
    val originalName: String,
    val originalWalletAddress: String,
    val name: TextFieldValue,
    val walletAddress: TextFieldValue,
    val transparentAddr: TextFieldValue,
    val evmAddr: TextFieldValue,
    val solanaAddr: TextFieldValue,
    val showAdditionalAddresses: Boolean,
    val showDeleteConfirm: Boolean,
    val error: String?,
    val isSaveEnabled: Boolean,
    val onNameChange: (TextFieldValue) -> Unit,
    val onWalletAddressChange: (TextFieldValue) -> Unit,
    val onTransparentAddrChange: (TextFieldValue) -> Unit,
    val onEvmAddrChange: (TextFieldValue) -> Unit,
    val onSolanaAddrChange: (TextFieldValue) -> Unit,
    val onToggleAdditionalAddresses: () -> Unit,
    val onScanWalletAddress: () -> Unit,
    val onScanAddressField: (addrType: String) -> Unit,
    val onSave: () -> Unit,
    val onRequestDelete: () -> Unit,
    val onCancelDelete: () -> Unit,
    val onConfirmDelete: () -> Unit,
    val onDismiss: () -> Unit,
)
