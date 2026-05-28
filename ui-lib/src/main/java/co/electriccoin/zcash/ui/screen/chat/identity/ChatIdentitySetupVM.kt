package co.electriccoin.zcash.ui.screen.chat.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.usecase.CreateChatIdentityUseCase
import co.electriccoin.zcash.ui.common.usecase.ObserveChatIdentityUseCase
import co.electriccoin.zcash.ui.common.usecase.RestoreChatIdentityUseCase
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import co.electriccoin.zcash.ui.screen.chat.common.ChatResult
import co.electriccoin.zcash.ui.screen.chat.common.toStringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class ChatIdentitySetupVM(
    observeChatIdentity: ObserveChatIdentityUseCase,
    private val createChatIdentity: CreateChatIdentityUseCase,
    private val restoreChatIdentity: RestoreChatIdentityUseCase,
    chatBootstrap: ChatBootstrap,
) : ViewModel() {
    // If auto-derivation already failed (e.g. the user came through the restore
    // flow but the SDK call failed), land on RESTORE so they can retry with their
    // seed rather than accidentally creating a new identity.
    private val selectedTab =
        MutableStateFlow(
            if (chatBootstrap.chatIdentityFailed.value) {
                ChatIdentitySetupTab.RESTORE
            } else {
                ChatIdentitySetupTab.CREATE
            }
        )
    private val createName = MutableStateFlow("")
    // Pre-fill the display name from the pending name captured during onboarding
    // so the user doesn't have to retype it after a failed auto-derive.
    private val restoreName = MutableStateFlow(chatBootstrap.pendingDisplayName.value ?: "")
    private val restoreSeed = MutableStateFlow("")
    private val isSubmitting = MutableStateFlow(false)
    private val error = MutableStateFlow<StringResource?>(null)
    private val seedBackup = MutableStateFlow<String?>(null)

    val isSetupComplete: StateFlow<Boolean> =
        combine(observeChatIdentity(), seedBackup) { identity, backup ->
            identity != null && backup == null
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = false,
        )

    val state: StateFlow<ChatIdentitySetupState> =
        combine(
            selectedTab,
            combine(createName, restoreName, restoreSeed) { c, r, s -> Triple(c, r, s) },
            combine(isSubmitting, error, seedBackup) { sub, err, bak -> Triple(sub, err, bak) },
        ) { tab, (createN, restoreN, restoreS), (submitting, err, backup) ->
            createState(tab, createN, restoreN, restoreS, submitting, err, backup)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue =
                createState(
                    tab = ChatIdentitySetupTab.CREATE,
                    createName = "",
                    restoreName = "",
                    restoreSeed = "",
                    isSubmitting = false,
                    error = null,
                    backup = null,
                ),
        )

    private fun createState(
        tab: ChatIdentitySetupTab,
        createName: String,
        restoreName: String,
        restoreSeed: String,
        isSubmitting: Boolean,
        error: StringResource?,
        backup: String?,
    ): ChatIdentitySetupState =
        ChatIdentitySetupState(
            title =
                when (tab) {
                    ChatIdentitySetupTab.CREATE -> stringRes(R.string.chat_identity_setup_welcome_title)
                    ChatIdentitySetupTab.RESTORE -> stringRes(R.string.chat_identity_setup_restore_title)
                },
            subtitle =
                when (tab) {
                    ChatIdentitySetupTab.CREATE -> stringRes(R.string.chat_identity_setup_welcome_subtitle)
                    ChatIdentitySetupTab.RESTORE -> stringRes(R.string.chat_identity_setup_restore_subtitle)
                },
            tabs =
                ChatIdentitySetupTabsState(
                    selected = tab,
                    createLabel = stringRes(R.string.chat_identity_setup_tab_create),
                    restoreLabel = stringRes(R.string.chat_identity_setup_tab_restore),
                    onSelect = ::onTabSelected,
                ),
            form =
                when (tab) {
                    ChatIdentitySetupTab.CREATE -> {
                        ChatIdentitySetupFormState.Create(
                            displayName = createName,
                            displayNamePlaceholder = stringRes(R.string.chat_identity_setup_display_name_placeholder),
                            isSubmitting = isSubmitting,
                            submitLabel = stringRes(R.string.chat_identity_setup_create_button),
                            onDisplayNameChange = ::onCreateNameChange,
                            onSubmit = ::onCreateClick,
                        )
                    }

                    ChatIdentitySetupTab.RESTORE -> {
                        ChatIdentitySetupFormState.Restore(
                            displayName = restoreName,
                            displayNamePlaceholder = stringRes(R.string.chat_identity_setup_display_name_placeholder),
                            seedPhrase = restoreSeed,
                            seedPhrasePlaceholder = stringRes(R.string.chat_identity_setup_seed_phrase_placeholder),
                            isSubmitting = isSubmitting,
                            submitLabel = stringRes(R.string.chat_identity_setup_restore_button),
                            onDisplayNameChange = ::onRestoreNameChange,
                            onSeedPhraseChange = ::onSeedPhraseChange,
                            onSubmit = ::onRestoreClick,
                        )
                    }
                },
            error = error,
            seedBackup =
                backup?.let { phrase ->
                    ChatIdentitySetupBackupDialogState(
                        title = stringRes(R.string.chat_identity_setup_backup_title),
                        subtitle = stringRes(R.string.chat_identity_setup_backup_subtitle),
                        confirmLabel = stringRes(R.string.chat_identity_setup_backup_confirm),
                        words = phrase.split(" ").filter { it.isNotBlank() },
                        onConfirm = ::onSeedBackupConfirm,
                    )
                },
        )

    private fun onTabSelected(tab: ChatIdentitySetupTab) {
        selectedTab.value = tab
        error.value = null
    }

    private fun onCreateNameChange(value: String) {
        createName.value = value
    }

    private fun onRestoreNameChange(value: String) {
        restoreName.value = value
    }

    private fun onSeedPhraseChange(value: String) {
        restoreSeed.value = value
    }

    private fun onCreateClick() {
        val name = createName.value.trim()
        if (name.isBlank()) {
            error.value = stringRes(R.string.chat_identity_setup_error_name_required)
            return
        }
        error.value = null
        viewModelScope.launch { createIdentity(name) }
    }

    private fun onRestoreClick() {
        val name = restoreName.value.trim()
        val seed = restoreSeed.value.trim()
        val words = seed.split("\\s+".toRegex()).filter { it.isNotBlank() }
        when {
            name.isBlank() -> {
                error.value = stringRes(R.string.chat_identity_setup_error_name_required)
            }

            words.size != SEED_WORD_COUNT -> {
                error.value = stringRes(R.string.chat_identity_setup_error_seed_word_count)
            }

            else -> {
                error.value = null
                viewModelScope.launch { restoreIdentity(seed, name) }
            }
        }
    }

    private fun onSeedBackupConfirm() {
        seedBackup.value = null
    }

    private suspend fun createIdentity(name: String) {
        isSubmitting.value = true
        try {
            when (val result = createChatIdentity(name)) {
                is ChatResult.Success -> seedBackup.value = result.value
                is ChatResult.Failure -> error.value = result.error.toStringResource()
            }
        } finally {
            isSubmitting.value = false
        }
    }

    private suspend fun restoreIdentity(seed: String, name: String) {
        isSubmitting.value = true
        try {
            when (val result = restoreChatIdentity(seed, name)) {
                is ChatResult.Success -> Unit
                is ChatResult.Failure -> error.value = result.error.toStringResource()
            }
        } finally {
            isSubmitting.value = false
        }
    }

    companion object {
        private const val SEED_WORD_COUNT = 24
    }
}
