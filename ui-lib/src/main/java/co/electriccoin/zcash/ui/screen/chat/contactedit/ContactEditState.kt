package co.electriccoin.zcash.ui.screen.chat.contactedit

import co.electriccoin.zcash.ui.design.util.StringResource

data class ContactEditState(
    val title: StringResource,
    val publicKey: String,
    val nameInput: String,
    val isContactFound: Boolean,
    val onNameChange: (String) -> Unit,
    val onSave: () -> Unit,
    val canSave: Boolean,
    val onDeleteClick: () -> Unit,
    val onBack: () -> Unit,
    val deleteDialog: ContactEditDeleteDialogState?,
)

data class ContactEditDeleteDialogState(
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)
