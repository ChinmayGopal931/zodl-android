package co.electriccoin.zcash.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.SeedPhrase
import co.electriccoin.zcash.ui.common.provider.IsKeepScreenOnDuringRestoreProvider
import co.electriccoin.zcash.ui.common.usecase.RestoreWalletUseCase
import co.electriccoin.zcash.ui.common.usecase.ValidateSeedUseCase
import co.electriccoin.zcash.ui.design.component.SeedTextFieldState
import co.electriccoin.zcash.ui.design.component.SeedWordInnerTextFieldState
import co.electriccoin.zcash.ui.design.component.SeedWordTextFieldState
import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Suppress("TooManyFunctions")
class ZappRestoreFlowVM(
    private val validateSeed: ValidateSeedUseCase,
    private val restoreWallet: RestoreWalletUseCase,
    private val chatBootstrap: ChatBootstrap,
    private val isKeepScreenOnDuringRestoreProvider: IsKeepScreenOnDuringRestoreProvider,
) : ViewModel() {

    // ── Seed words ──────────────────────────────────────────────

    private val seedWords =
        MutableStateFlow(
            (0..23).map { index ->
                SeedWordTextFieldState(
                    innerState = SeedWordInnerTextFieldState(""),
                    onValueChange = { onSeedWordChange(index, it) },
                    isError = false,
                )
            }
        )

    private val bip39Suggestions =
        flow {
            val result = withContext(Dispatchers.IO) { Mnemonics.getCachedWords(Locale.ENGLISH.language) }
            emit(result)
        }.stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val seedValidations =
        combine(seedWords, bip39Suggestions) { words, suggestions ->
            words to suggestions.orEmpty()
        }.mapLatest { (words, suggestions) ->
            withContext(Dispatchers.Default) {
                words.map { field ->
                    val trimmed = field.innerState.value.lowercase().trim()
                    val autocomplete = suggestions.filter { it.startsWith(trimmed) }
                    val valid = when {
                        trimmed.isBlank() -> suggestions
                        suggestions.contains(trimmed) && autocomplete.size == 1 -> suggestions
                        else -> autocomplete
                    }
                    valid.isNotEmpty()
                }
            }
        }

    val validSeed: StateFlow<SeedPhrase?> =
        seedWords
            .map { fields -> validateSeed(fields.map { it.innerState.value.trim() }) }
            .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    val seedFieldState: StateFlow<SeedTextFieldState> =
        combine(seedWords, seedValidations) { words, validations ->
            SeedTextFieldState(
                values = words.mapIndexed { index, word ->
                    word.copy(isError = !validations[index])
                }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SeedTextFieldState(values = seedWords.value),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val suggestionsVisible: StateFlow<Boolean> =
        combine(validSeed, bip39Suggestions) { seed, suggestions ->
            seed == null && suggestions != null
        }.mapLatest { it }
            .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = false)

    val suggestionsList: StateFlow<List<String>> =
        bip39Suggestions
            .map { it.orEmpty() }
            .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList())

    private fun onSeedWordChange(index: Int, newState: SeedWordInnerTextFieldState) {
        seedWords.update { list ->
            list.toMutableList().also {
                it[index] = it[index].copy(innerState = newState.copy(value = newState.value.trim()))
            }
        }
    }

    // ── Birthday height ─────────────────────────────────────────

    private val _birthdayText = MutableStateFlow("")
    val birthdayText: StateFlow<String> = _birthdayText.asStateFlow()

    fun onBirthdayChange(value: String) {
        _birthdayText.value = value.filter { it.isDigit() }
    }

    // ── Tor toggle ──────────────────────────────────────────────

    private val _torEnabled = MutableStateFlow(false)
    val torEnabled: StateFlow<Boolean> = _torEnabled.asStateFlow()

    fun onTorToggle() {
        _torEnabled.update { !it }
    }

    // ── Keep screen on ──────────────────────────────────────────

    private val _keepScreenOn = MutableStateFlow(false)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    fun onKeepScreenOnToggle() {
        _keepScreenOn.update { !it }
    }

    fun persistKeepScreenOn() {
        viewModelScope.launch {
            isKeepScreenOnDuringRestoreProvider.store(_keepScreenOn.value)
        }
    }

    // ── Restore action ──────────────────────────────────────────

    private val _restoreError = MutableStateFlow<String?>(null)
    val restoreError: StateFlow<String?> = _restoreError.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    fun startRestore(displayName: String) {
        val seed = validSeed.value ?: return
        if (_isRestoring.value) return
        _isRestoring.value = true
        _restoreError.value = null

        chatBootstrap.setPendingDisplayName(displayName)

        val birthday = _birthdayText.value.toLongOrNull()
        val blockHeight = if (birthday != null && birthday > 0) {
            BlockHeight.new(birthday)
        } else {
            BlockHeight.new(419_200L) // sapling activation height (testnet safe default)
        }

        viewModelScope.launch {
            runCatching {
                restoreWallet(
                    seedPhrase = seed,
                    enableTor = _torEnabled.value,
                    birthday = blockHeight,
                )
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _restoreError.value = e.message ?: e.toString()
                _isRestoring.value = false
            }
        }
    }

    fun retryRestore(displayName: String) {
        _isRestoring.update { false }
        startRestore(displayName)
    }

    fun enteredSeedWords(): List<String> =
        seedWords.value.map { it.innerState.value.trim() }
}
