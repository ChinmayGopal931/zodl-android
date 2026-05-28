package co.electriccoin.zcash.ui.screen.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.common.viewmodel.SecretState
import co.electriccoin.zcash.ui.common.viewmodel.WalletViewModel
import androidx.compose.ui.res.stringResource
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.theme.ProvideZappTheme
import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import co.electriccoin.zcash.ui.screen.onboarding.view.BioScanScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.KeepZappOpenScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.PinSetupScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.RestoreBirthdayScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.RestoreInProgressScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.RestoreSeedEntryScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.RestoreTorOptionScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.SeedRevealScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.TwoFAChoiceScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.TwoFAMode
import co.electriccoin.zcash.ui.screen.onboarding.view.UsernameEntryScreen
import org.koin.androidx.compose.koinViewModel

private enum class RestoreStep {
    USERNAME,
    SEED_ENTRY,
    BIRTHDAY,
    TOR,
    RESTORING,
    SEED_CONFIRM,
    SECURE_CHOICE,
    BIO_SCAN,
    PIN_SETUP,
    KEEP_OPEN,
}

/**
 * Unified restore flow for "I already use Zapp". Restores both the wallet
 * and the messaging identity from a single 24-word seed phrase, then walks
 * the user through PIN/biometrics and the sync screen.
 */
@Composable
fun ZappRestoreFlow(
    onComplete: () -> Unit,
    onBackToWelcome: () -> Unit,
    walletViewModel: WalletViewModel,
    chatBootstrap: ChatBootstrap,
) {
    ProvideZappTheme {
        ZappRestoreFlowContent(
            onComplete = onComplete,
            onBackToWelcome = onBackToWelcome,
            walletViewModel = walletViewModel,
            chatBootstrap = chatBootstrap,
        )
    }
}

@Composable
private fun ZappRestoreFlowContent(
    onComplete: () -> Unit,
    onBackToWelcome: () -> Unit,
    walletViewModel: WalletViewModel,
    chatBootstrap: ChatBootstrap,
) {
    val restoreVM: ZappRestoreFlowVM = koinViewModel()
    val securityVM: OnboardingSecurityViewModel = koinViewModel()

    var step by rememberSaveable { mutableStateOf(RestoreStep.USERNAME) }
    var twoFAMode by rememberSaveable { mutableStateOf(TwoFAMode.Bio) }
    var pendingUsername by rememberSaveable { mutableStateOf("") }

    val secretState by walletViewModel.secretState.collectAsStateWithLifecycle()
    val chatIdentity by chatBootstrap.identity.collectAsStateWithLifecycle()
    val chatIdentityFailed by chatBootstrap.chatIdentityFailed.collectAsStateWithLifecycle()
    val isDerivingChatIdentity by chatBootstrap.isDeriving.collectAsStateWithLifecycle()
    val walletProvisioningError by walletViewModel.walletProvisioningError.collectAsStateWithLifecycle()

    val seedFieldState by restoreVM.seedFieldState.collectAsStateWithLifecycle()
    val validSeed by restoreVM.validSeed.collectAsStateWithLifecycle()
    val suggestionsVisible by restoreVM.suggestionsVisible.collectAsStateWithLifecycle()
    val suggestionsList by restoreVM.suggestionsList.collectAsStateWithLifecycle()
    val birthdayText by restoreVM.birthdayText.collectAsStateWithLifecycle()
    val birthdayMode by restoreVM.birthdayMode.collectAsStateWithLifecycle()
    val selectedYearMonth by restoreVM.selectedYearMonth.collectAsStateWithLifecycle()
    val isEstimating by restoreVM.isEstimating.collectAsStateWithLifecycle()
    val torEnabled by restoreVM.torEnabled.collectAsStateWithLifecycle()
    val isRestoring by restoreVM.isRestoring.collectAsStateWithLifecycle()
    val restoreError by restoreVM.restoreError.collectAsStateWithLifecycle()
    val keepScreenOn by restoreVM.keepScreenOn.collectAsStateWithLifecycle()

    val bioState by securityVM.bioState.collectAsStateWithLifecycle()
    val pinSaved by securityVM.pinSaved.collectAsStateWithLifecycle()

    // Process-death recovery: rememberSaveable restores `pendingUsername`, but the
    // in-process `pendingDisplayName` inside ChatBootstrap dies with the process.
    // Re-publish once per rehydration.
    LaunchedEffect(pendingUsername) {
        if (pendingUsername.isNotBlank()) {
            chatBootstrap.setPendingDisplayName(pendingUsername)
        }
    }

    // Auto-advance from RESTORING → SEED_CONFIRM when wallet + chat identity are ready.
    LaunchedEffect(secretState, chatIdentity, chatIdentityFailed, step) {
        if (step == RestoreStep.RESTORING && secretState == SecretState.READY) {
            when {
                chatIdentity != null -> step = RestoreStep.SEED_CONFIRM
                chatIdentityFailed -> {
                    // Chat identity derive failed — still advance (wallet is restored).
                    // The user can set up messaging later.
                    step = RestoreStep.SEED_CONFIRM
                }
            }
        }
    }

    // Auto-advance from BIO_SCAN → KEEP_OPEN on success.
    LaunchedEffect(bioState) {
        if (bioState is OnboardingSecurityViewModel.BioState.Success && step == RestoreStep.BIO_SCAN) {
            step = RestoreStep.KEEP_OPEN
        }
    }

    // Auto-advance from PIN_SETUP → KEEP_OPEN once saved.
    LaunchedEffect(pinSaved) {
        if (pinSaved && step == RestoreStep.PIN_SETUP) {
            step = RestoreStep.KEEP_OPEN
        }
    }

    when (step) {
        RestoreStep.USERNAME -> {
            UsernameEntryScreen(
                onBack = onBackToWelcome,
                onContinue = { name ->
                    pendingUsername = name
                    step = RestoreStep.SEED_ENTRY
                },
            )
        }

        RestoreStep.SEED_ENTRY -> {
            RestoreSeedEntryScreen(
                seedState = seedFieldState,
                suggestionsVisible = suggestionsVisible,
                suggestions = suggestionsList,
                isSeedValid = validSeed != null,
                onBack = { step = RestoreStep.USERNAME },
                onNext = { step = RestoreStep.BIRTHDAY },
            )
        }

        RestoreStep.BIRTHDAY -> {
            LaunchedEffect(Unit) {
                restoreVM.estimationDone.collect {
                    if (step == RestoreStep.BIRTHDAY) step = RestoreStep.TOR
                }
            }
            RestoreBirthdayScreen(
                birthdayText = birthdayText,
                onBirthdayChange = restoreVM::onBirthdayChange,
                birthdayMode = birthdayMode,
                onBirthdayModeChange = restoreVM::onBirthdayModeChange,
                selectedYearMonth = selectedYearMonth,
                onYearMonthChange = restoreVM::onYearMonthChange,
                isEstimating = isEstimating,
                onBack = { step = RestoreStep.SEED_ENTRY },
                onNext = {
                    if (birthdayMode == BirthdayMode.DATE) {
                        restoreVM.estimateFromDate()
                    } else {
                        step = RestoreStep.TOR
                    }
                },
                onSkip = { step = RestoreStep.TOR },
            )
        }

        RestoreStep.TOR -> {
            RestoreTorOptionScreen(
                torEnabled = torEnabled,
                onToggle = restoreVM::onTorToggle,
                onBack = { step = RestoreStep.BIRTHDAY },
                onRestore = {
                    restoreVM.startRestore(pendingUsername)
                    step = RestoreStep.RESTORING
                },
            )
        }

        RestoreStep.RESTORING -> {
            val errorMsg = when {
                walletProvisioningError != null -> walletProvisioningError?.message
                restoreError != null -> restoreError
                chatIdentityFailed && secretState == SecretState.READY ->
                    stringResource(R.string.chat_identity_setup_error_wallet_derive_failed)
                else -> null
            }
            val onRetry: (() -> Unit)? = when {
                restoreError != null -> {{ restoreVM.retryRestore(pendingUsername) }}
                chatIdentityFailed && !isDerivingChatIdentity -> {{ chatBootstrap.retry() }}
                else -> null
            }
            RestoreInProgressScreen(
                isRestoring = isRestoring,
                errorMessage = errorMsg,
                onRetry = onRetry,
            )
        }

        RestoreStep.SEED_CONFIRM -> {
            val words = restoreVM.enteredSeedWords()
            SeedRevealScreen(
                step = 2,
                title = stringResource(R.string.restore_flow_confirm_title),
                sub = stringResource(R.string.restore_flow_confirm_sub),
                words = words,
                onBack = { }, // No going back from here — wallet already restored
                onContinue = { step = RestoreStep.SECURE_CHOICE },
            )
        }

        RestoreStep.SECURE_CHOICE -> {
            TwoFAChoiceScreen(
                onBack = { step = RestoreStep.SEED_CONFIRM },
                onPick = { mode ->
                    twoFAMode = mode
                    step = when (mode) {
                        TwoFAMode.Bio -> RestoreStep.BIO_SCAN
                        TwoFAMode.Pin -> RestoreStep.PIN_SETUP
                    }
                },
            )
        }

        RestoreStep.BIO_SCAN -> {
            BioScanScreen(
                isEnrolling = bioState is OnboardingSecurityViewModel.BioState.Prompting,
                errorMessage = (bioState as? OnboardingSecurityViewModel.BioState.Error)?.message,
                onEnroll = { securityVM.triggerBiometricSetup() },
                onCancel = {
                    securityVM.resetBioError()
                    step = RestoreStep.SECURE_CHOICE
                },
            )
        }

        RestoreStep.PIN_SETUP -> {
            PinSetupScreen(
                onBack = { step = RestoreStep.SECURE_CHOICE },
                onPinConfirmed = { pin -> securityVM.savePin(pin) },
            )
        }

        RestoreStep.KEEP_OPEN -> {
            KeepZappOpenScreen(
                keepScreenOn = keepScreenOn,
                onToggleKeepScreenOn = restoreVM::onKeepScreenOnToggle,
                onEnterApp = {
                    restoreVM.persistKeepScreenOn()
                    onComplete()
                },
            )
        }
    }
}
