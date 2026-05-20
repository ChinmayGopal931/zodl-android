package co.electriccoin.zcash.ui.screen.chat.identity

import co.electriccoin.zcash.ui.design.util.StringResource

data class ChatIdentitySetupState(
    val title: StringResource,
    val subtitle: StringResource,
    val tabs: ChatIdentitySetupTabsState,
    val form: ChatIdentitySetupFormState,
    val error: StringResource?,
    val seedBackup: ChatIdentitySetupBackupDialogState?,
)

data class ChatIdentitySetupTabsState(
    val selected: ChatIdentitySetupTab,
    val createLabel: StringResource,
    val restoreLabel: StringResource,
    val onSelect: (ChatIdentitySetupTab) -> Unit,
)

enum class ChatIdentitySetupTab { CREATE, RESTORE }

sealed interface ChatIdentitySetupFormState {
    val isSubmitting: Boolean

    data class Create(
        val displayName: String,
        val displayNamePlaceholder: StringResource,
        override val isSubmitting: Boolean,
        val submitLabel: StringResource,
        val onDisplayNameChange: (String) -> Unit,
        val onSubmit: () -> Unit,
    ) : ChatIdentitySetupFormState

    data class Restore(
        val displayName: String,
        val displayNamePlaceholder: StringResource,
        val seedPhrase: String,
        val seedPhrasePlaceholder: StringResource,
        override val isSubmitting: Boolean,
        val submitLabel: StringResource,
        val onDisplayNameChange: (String) -> Unit,
        val onSeedPhraseChange: (String) -> Unit,
        val onSubmit: () -> Unit,
    ) : ChatIdentitySetupFormState
}

data class ChatIdentitySetupBackupDialogState(
    val title: StringResource,
    val subtitle: StringResource,
    val confirmLabel: StringResource,
    val words: List<String>,
    val onConfirm: () -> Unit,
)
