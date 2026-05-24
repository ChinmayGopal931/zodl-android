package co.electriccoin.zcash.ui.screen.chat.profile

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.preference.EncryptedPreferenceProvider
import co.electriccoin.zcash.preference.StandardPreferenceProvider
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.repository.BiometricRepository
import co.electriccoin.zcash.ui.common.repository.BiometricRequest
import co.electriccoin.zcash.ui.common.repository.BiometricsCancelledException
import co.electriccoin.zcash.ui.common.repository.BiometricsFailureException
import co.electriccoin.zcash.ui.common.security.PinAuthGate
import co.electriccoin.zcash.ui.common.usecase.ObserveSelectedWalletAccountUseCase
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.preference.StandardPreferenceKeys
import co.electriccoin.zcash.ui.screen.chat.common.runChatCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.justzappit.zappmessaging.ZappMessagingSDK

@Suppress("TooManyFunctions")
class ChatProfileVM(
    private val application: Application,
    private val sdk: ZappMessagingSDK,
    observeSelectedWalletAccount: ObserveSelectedWalletAccountUseCase,
    private val biometricRepository: BiometricRepository,
    private val standardPreferenceProvider: StandardPreferenceProvider,
    private val encryptedPreferenceProvider: EncryptedPreferenceProvider,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val activeTab = MutableStateFlow(ChatProfileTab.MESSAGING_ID)
    private val walletSubTab = MutableStateFlow(ChatProfileWalletSubTab.SHIELDED)
    private val isKeyCopied = MutableStateFlow(false)
    private val isAddressCopied = MutableStateFlow(false)
    private val showDeleteDialog = MutableStateFlow(false)
    private val showEditNameDialog = MutableStateFlow(false)
    private val editNameInput = MutableStateFlow("")
    private val pinVerifyMode = MutableStateFlow<PinVerifyMode>(PinVerifyMode.Idle)
    private val pendingSeedPhrase = MutableStateFlow<String?>(null)

    private val walletAccount =
        observeSelectedWalletAccount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
                initialValue = null,
            )

    private val identity =
        sdk.identity
            .map { id -> id?.let { ChatProfileIdentity(displayName = it.displayName, publicKey = it.publicKey) } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
                initialValue = null,
            )

    private var pinLockoutTickerJob: Job? = null
    private var copyKeyResetJob: Job? = null
    private var copyAddressResetJob: Job? = null

    val state: StateFlow<ChatProfileState> =
        combine(
            combine(activeTab, walletSubTab, identity) { tab, sub, id -> Triple(tab, sub, id) },
            walletAccount,
            combine(isKeyCopied, isAddressCopied) { key, addr -> key to addr },
            combine(showDeleteDialog, showEditNameDialog, editNameInput) { del, edit, input ->
                Triple(del, edit, input)
            },
            combine(pinVerifyMode, pendingSeedPhrase) { pin, seed -> pin to seed },
        ) { (tab, sub, id), wallet, (keyCopied, addrCopied), (delDlg, editDlg, editInput), (pinMode, seed) ->
            createState(tab, sub, id, wallet, keyCopied, addrCopied, delDlg, editDlg, editInput, pinMode, seed)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue =
                createState(
                    tab = ChatProfileTab.MESSAGING_ID,
                    sub = ChatProfileWalletSubTab.SHIELDED,
                    id = null,
                    wallet = null,
                    keyCopied = false,
                    addrCopied = false,
                    delDlg = false,
                    editDlg = false,
                    editInput = "",
                    pinMode = PinVerifyMode.Idle,
                    seed = null,
                ),
        )

    private fun createState(
        tab: ChatProfileTab,
        sub: ChatProfileWalletSubTab,
        id: ChatProfileIdentity?,
        wallet: WalletAccount?,
        keyCopied: Boolean,
        addrCopied: Boolean,
        delDlg: Boolean,
        editDlg: Boolean,
        editInput: String,
        pinMode: PinVerifyMode,
        seed: String?,
    ): ChatProfileState =
        ChatProfileState(
            title = stringRes(R.string.chat_profile_title),
            activeTab = tab,
            walletSubTab = sub,
            displayName = id?.displayName,
            publicKey = id?.publicKey,
            shieldedAddress = wallet?.unified?.address?.address,
            transparentAddress = wallet?.transparent?.address?.address,
            isKeyCopied = keyCopied,
            isAddressCopied = addrCopied,
            onMainTabSelected = ::onMainTabSelected,
            onWalletSubTabSelected = ::onWalletSubTabSelected,
            onEditDisplayNameClick = ::onEditDisplayNameClick,
            onCopyPublicKeyClick = ::onCopyPublicKeyClick,
            onCopyAddressClick = ::onCopyAddressClick,
            onSeedPhraseClick = ::onSeedPhraseClick,
            onDeleteClick = ::onDeleteClick,
            onBack = ::onBack,
            editNameDialog =
                if (editDlg) {
                    ChatProfileEditNameDialogState(
                        value = editInput,
                        canSave = editInput.isNotBlank(),
                        onValueChange = ::onEditNameInputChange,
                        onSave = ::onEditNameSave,
                        onDismiss = ::dismissEditNameDialog,
                    )
                } else {
                    null
                },
            deleteDialog =
                if (delDlg) {
                    ChatProfileDeleteDialogState(
                        onConfirm = ::onDeleteConfirm,
                        onDismiss = ::dismissDeleteDialog,
                    )
                } else {
                    null
                },
            seedPhraseDialog =
                seed?.let { phrase ->
                    ChatProfileSeedPhraseDialogState(
                        words = phrase.split(" ").filter { it.isNotBlank() },
                        onDismiss = ::dismissSeedPhraseDialog,
                    )
                },
            pinVerify = pinMode.toState(),
        )

    private fun PinVerifyMode.toState(): ChatProfilePinVerifyState? =
        when (this) {
            PinVerifyMode.Idle -> {
                null
            }

            PinVerifyMode.Required -> {
                ChatProfilePinVerifyState(
                    hasError = false,
                    lockoutSecondsRemaining = 0,
                    onPinSubmit = ::onPinSubmitted,
                    onCancel = ::onPinEntryDismissed,
                )
            }

            PinVerifyMode.Error -> {
                ChatProfilePinVerifyState(
                    hasError = true,
                    lockoutSecondsRemaining = 0,
                    onPinSubmit = ::onPinSubmitted,
                    onCancel = ::onPinEntryDismissed,
                )
            }

            is PinVerifyMode.Locked -> {
                ChatProfilePinVerifyState(
                    hasError = false,
                    lockoutSecondsRemaining = secondsRemaining,
                    onPinSubmit = ::onPinSubmitted,
                    onCancel = ::onPinEntryDismissed,
                )
            }
        }

    // ── Click handlers ─────────────────────────────────────────────────

    private fun onBack() = navigationRouter.back()

    private fun onMainTabSelected(tab: ChatProfileTab) {
        activeTab.value = tab
    }

    private fun onWalletSubTabSelected(tab: ChatProfileWalletSubTab) {
        walletSubTab.value = tab
    }

    private fun onEditDisplayNameClick() {
        editNameInput.value = identity.value?.displayName.orEmpty()
        showEditNameDialog.value = true
    }

    private fun onEditNameInputChange(value: String) {
        editNameInput.value = value
    }

    private fun onEditNameSave() {
        val trimmed = editNameInput.value.trim()
        if (trimmed.isEmpty()) return
        showEditNameDialog.value = false
        viewModelScope.launch { updateDisplayName(trimmed) }
    }

    private suspend fun updateDisplayName(name: String) {
        runChatCall("ChatProfileVM: updateDisplayName failed") {
            sdk.updateDisplayName(name)
        }
    }

    private fun dismissEditNameDialog() {
        showEditNameDialog.value = false
    }

    private fun onDeleteClick() {
        showDeleteDialog.value = true
    }

    private fun dismissDeleteDialog() {
        showDeleteDialog.value = false
    }

    private fun onDeleteConfirm() {
        showDeleteDialog.value = false
        viewModelScope.launch { performDeleteIdentity() }
    }

    private suspend fun performDeleteIdentity() {
        runChatCall("ChatProfileVM: sdk.shutdown failed") {
            sdk.shutdown()
        }
        navigationRouter.backToRoot()
    }

    private fun onCopyPublicKeyClick() {
        val pk = identity.value?.publicKey ?: return
        copyToClipboard(label = "Public Key", value = pk)
        isKeyCopied.value = true
        copyKeyResetJob?.cancel()
        copyKeyResetJob =
            viewModelScope.launch {
                delay(COPY_FEEDBACK_MS)
                isKeyCopied.value = false
            }
    }

    private fun onCopyAddressClick() {
        val address = currentWalletAddress() ?: return
        copyToClipboard(label = "Wallet Address", value = address)
        isAddressCopied.value = true
        copyAddressResetJob?.cancel()
        copyAddressResetJob =
            viewModelScope.launch {
                delay(COPY_FEEDBACK_MS)
                isAddressCopied.value = false
            }
    }

    private fun currentWalletAddress(): String? {
        val account = walletAccount.value ?: return null
        return when (walletSubTab.value) {
            ChatProfileWalletSubTab.SHIELDED -> account.unified.address.address
            ChatProfileWalletSubTab.TRANSPARENT -> account.transparent.address.address
        }
    }

    private fun copyToClipboard(label: String, value: String) {
        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
    }

    // ── Seed phrase reveal (PIN / biometric gate) ───────────────────────

    private fun onSeedPhraseClick() {
        viewModelScope.launch { initiateSeedReveal() }
    }

    private suspend fun initiateSeedReveal() {
        val authMethod =
            StandardPreferenceKeys.AUTH_METHOD.getValue(standardPreferenceProvider())
        when (authMethod) {
            AUTH_METHOD_BIOMETRIC -> {
                try {
                    biometricRepository.requestBiometrics(
                        BiometricRequest(message = stringRes(R.string.chat_profile_seed_phrase_biometric_prompt)),
                    )
                    exportAndEmitSeedPhrase()
                } catch (_: BiometricsFailureException) {
                    // user dismissed / hardware failed — silent
                } catch (_: BiometricsCancelledException) {
                    // explicit cancel — silent
                }
            }

            AUTH_METHOD_PIN -> {
                pinVerifyMode.value = PinVerifyMode.Required
            }

            else -> {
                exportAndEmitSeedPhrase()
            }
        }
    }

    private fun onPinSubmitted(pin: String) {
        viewModelScope.launch {
            val result =
                PinAuthGate.tryVerify(pin, encryptedPreferenceProvider, standardPreferenceProvider)
            when (result) {
                PinAuthGate.Result.Success -> {
                    pinVerifyMode.value = PinVerifyMode.Idle
                    exportAndEmitSeedPhrase()
                }

                PinAuthGate.Result.Wrong -> {
                    pinVerifyMode.value = PinVerifyMode.Error
                    delay(PIN_ERROR_FEEDBACK_MS)
                    pinVerifyMode.value = PinVerifyMode.Required
                }

                is PinAuthGate.Result.Locked -> {
                    startPinLockoutTicker(result.msUntilUnlock)
                }
            }
        }
    }

    private fun onPinEntryDismissed() {
        pinVerifyMode.value = PinVerifyMode.Idle
    }

    private fun startPinLockoutTicker(initialMs: Long) {
        pinLockoutTickerJob?.cancel()
        pinLockoutTickerJob =
            viewModelScope.launch {
                var remaining = initialMs
                while (remaining > 0) {
                    pinVerifyMode.value =
                        PinVerifyMode.Locked(((remaining + MS_ROUND_UP) / MS_PER_SECOND).toInt())
                    delay(MS_PER_SECOND)
                    remaining -= MS_PER_SECOND
                }
                pinVerifyMode.value = PinVerifyMode.Required
            }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun exportAndEmitSeedPhrase() {
        val phrase =
            try {
                sdk.exportSeedPhrase()
            } catch (e: Exception) {
                Twig.warn(e) { "ChatProfileVM: exportSeedPhrase failed" }
                null
            }
        if (phrase != null) pendingSeedPhrase.value = phrase
    }

    private fun dismissSeedPhraseDialog() {
        pendingSeedPhrase.value = null
    }

    private sealed class PinVerifyMode {
        object Idle : PinVerifyMode()

        object Required : PinVerifyMode()

        object Error : PinVerifyMode()

        data class Locked(
            val secondsRemaining: Int
        ) : PinVerifyMode()
    }

    private data class ChatProfileIdentity(
        val displayName: String,
        val publicKey: String
    )

    companion object {
        private const val COPY_FEEDBACK_MS = 2_000L
        private const val PIN_ERROR_FEEDBACK_MS = 1_500L
        private const val MS_PER_SECOND = 1_000L
        private const val MS_ROUND_UP = 999L
        private const val AUTH_METHOD_BIOMETRIC = "biometric"
        private const val AUTH_METHOD_PIN = "pin"
    }

    override fun onCleared() {
        super.onCleared()
        pinLockoutTickerJob?.cancel()
        copyKeyResetJob?.cancel()
        copyAddressResetJob?.cancel()
    }
}
