package co.electriccoin.zcash.ui.screen.chat.identity

import co.electriccoin.zcash.ui.design.util.StringResource

data class ChatIdentitySetupState(
    val title: StringResource,
    val subtitle: StringResource,
    val displayName: String,
    val displayNamePlaceholder: StringResource,
    val submitLabel: StringResource,
    val isSubmitting: Boolean,
    val error: StringResource?,
    val onDisplayNameChange: (String) -> Unit,
    val onSubmit: () -> Unit,
)
