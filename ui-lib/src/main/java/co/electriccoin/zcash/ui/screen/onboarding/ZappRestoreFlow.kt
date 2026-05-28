@file:Suppress("TooManyFunctions")

package co.electriccoin.zcash.ui.screen.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.viewmodel.SecretState
import co.electriccoin.zcash.ui.common.viewmodel.WalletViewModel
import co.electriccoin.zcash.ui.design.theme.ProvideZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
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
    var pendingUsername by rememberSaveable { mutableStateOf("") }

    RestoreFlowEffects(
        step = step,
        onStepChange = { step = it },
        pendingUsername = pendingUsername,
        chatBootstrap = chatBootstrap,
        walletViewModel = walletViewModel,
        restoreVM = restoreVM,
        securityVM = securityVM,
    )

    RestoreStepHost(
        step = step,
        onStepChange = { step = it },
        pendingUsername = pendingUsername,
        onPendingUsernameChange = { pendingUsername = it },
        onBackToWelcome = onBackToWelcome,
        onComplete = onComplete,
        walletViewModel = walletViewModel,
        chatBootstrap = chatBootstrap,
        restoreVM = restoreVM,
        securityVM = securityVM,
    )
}

/**
 * Process-death recovery + auto-advance triggers. Lives in its own composable so
 * the LaunchedEffect conditions don't pile into ZappRestoreFlowContent's complexity.
 */
@Composable
private fun RestoreFlowEffects(
    step: RestoreStep,
    onStepChange: (RestoreStep) -> Unit,
    pendingUsername: String,
    chatBootstrap: ChatBootstrap,
    walletViewModel: WalletViewModel,
    restoreVM: ZappRestoreFlowVM,
    securityVM: OnboardingSecurityViewModel,
) {
    val secretState by walletViewModel.secretState.collectAsStateWithLifecycle()
    val chatIdentity by chatBootstrap.identity.collectAsStateWithLifecycle()
    val bioState by securityVM.bioState.collectAsStateWithLifecycle()
    val pinSaved by securityVM.pinSaved.collectAsStateWithLifecycle()

    // rememberSaveable restores `pendingUsername` across process death, but the
    // in-process `pendingDisplayName` inside ChatBootstrap dies with the process.
    LaunchedEffect(pendingUsername) {
        if (pendingUsername.isNotBlank()) {
            chatBootstrap.setPendingDisplayName(pendingUsername)
        }
    }

    // Advance only when BOTH the wallet and the chat identity are ready. A chat-derive
    // failure deliberately does NOT advance: the user stays on the RESTORING screen,
    // which surfaces the error and a retry (see RestoringStepView). Auto-advancing on
    // failure would bury the error and sail the user into the app with no chat identity.
    LaunchedEffect(secretState, chatIdentity, step) {
        val walletReady = step == RestoreStep.RESTORING && secretState == SecretState.READY
        if (walletReady && chatIdentity != null) {
            restoreVM.markRestoreCompleted()
            onStepChange(RestoreStep.SEED_CONFIRM)
        }
    }

    LaunchedEffect(bioState, step) {
        if (bioState is OnboardingSecurityViewModel.BioState.Success && step == RestoreStep.BIO_SCAN) {
            onStepChange(RestoreStep.KEEP_OPEN)
        }
    }

    LaunchedEffect(pinSaved, step) {
        if (pinSaved && step == RestoreStep.PIN_SETUP) {
            onStepChange(RestoreStep.KEEP_OPEN)
        }
    }
}

@Composable
private fun RestoreStepHost(
    step: RestoreStep,
    onStepChange: (RestoreStep) -> Unit,
    pendingUsername: String,
    onPendingUsernameChange: (String) -> Unit,
    onBackToWelcome: () -> Unit,
    onComplete: () -> Unit,
    walletViewModel: WalletViewModel,
    chatBootstrap: ChatBootstrap,
    restoreVM: ZappRestoreFlowVM,
    securityVM: OnboardingSecurityViewModel,
) {
    when (step) {
        RestoreStep.USERNAME ->
            UsernameEntryScreen(
                onBack = onBackToWelcome,
                onContinue = { name ->
                    onPendingUsernameChange(name)
                    onStepChange(RestoreStep.SEED_ENTRY)
                },
            )

        RestoreStep.SEED_ENTRY -> SeedEntryStepView(restoreVM, onStepChange)
        RestoreStep.BIRTHDAY -> BirthdayStepView(restoreVM, onStepChange)
        RestoreStep.TOR -> TorStepView(pendingUsername, restoreVM, onStepChange)
        RestoreStep.RESTORING -> RestoringStepView(pendingUsername, walletViewModel, chatBootstrap, restoreVM)
        RestoreStep.SEED_CONFIRM -> SeedConfirmStepView(walletViewModel, onStepChange)
        RestoreStep.SECURE_CHOICE -> SecureChoiceStepView(onStepChange)
        RestoreStep.BIO_SCAN -> BioStepView(securityVM, onStepChange)
        RestoreStep.PIN_SETUP -> PinStepView(securityVM, onStepChange)
        RestoreStep.KEEP_OPEN -> KeepOpenStepView(restoreVM, onComplete)
    }
}

@Composable
private fun SeedEntryStepView(restoreVM: ZappRestoreFlowVM, onStepChange: (RestoreStep) -> Unit) {
    val seedFieldState by restoreVM.seedFieldState.collectAsStateWithLifecycle()
    val validSeed by restoreVM.validSeed.collectAsStateWithLifecycle()
    val suggestionsVisible by restoreVM.suggestionsVisible.collectAsStateWithLifecycle()
    val suggestionsList by restoreVM.suggestionsList.collectAsStateWithLifecycle()
    RestoreSeedEntryScreen(
        seedState = seedFieldState,
        suggestionsVisible = suggestionsVisible,
        suggestions = suggestionsList,
        isSeedValid = validSeed != null,
        onBack = { onStepChange(RestoreStep.USERNAME) },
        onNext = { onStepChange(RestoreStep.BIRTHDAY) },
    )
}

@Composable
private fun BirthdayStepView(restoreVM: ZappRestoreFlowVM, onStepChange: (RestoreStep) -> Unit) {
    val birthdayText by restoreVM.birthdayText.collectAsStateWithLifecycle()
    val birthdayMode by restoreVM.birthdayMode.collectAsStateWithLifecycle()
    val selectedYearMonth by restoreVM.selectedYearMonth.collectAsStateWithLifecycle()
    val isEstimating by restoreVM.isEstimating.collectAsStateWithLifecycle()
    val birthdayErrorRes by restoreVM.birthdayError.collectAsStateWithLifecycle()
    RestoreBirthdayScreen(
        birthdayText = birthdayText,
        onBirthdayChange = restoreVM::onBirthdayChange,
        birthdayMode = birthdayMode,
        onBirthdayModeChange = restoreVM::onBirthdayModeChange,
        selectedYearMonth = selectedYearMonth,
        onYearMonthChange = restoreVM::onYearMonthChange,
        isEstimating = isEstimating,
        errorMessage = birthdayErrorRes?.getValue(),
        onBack = { onStepChange(RestoreStep.SEED_ENTRY) },
        onNext = {
            if (birthdayMode == BirthdayMode.DATE) {
                restoreVM.estimateFromDate()
            } else {
                onStepChange(RestoreStep.TOR)
            }
        },
        onSkip = { onStepChange(RestoreStep.TOR) },
    )
}

@Composable
private fun TorStepView(
    pendingUsername: String,
    restoreVM: ZappRestoreFlowVM,
    onStepChange: (RestoreStep) -> Unit,
) {
    val torEnabled by restoreVM.torEnabled.collectAsStateWithLifecycle()
    RestoreTorOptionScreen(
        torEnabled = torEnabled,
        onToggle = restoreVM::onTorToggle,
        onBack = { onStepChange(RestoreStep.BIRTHDAY) },
        onRestore = {
            restoreVM.startRestore(pendingUsername)
            // If the VM rejected the start (invalid birthday), birthdayError is
            // set and isRestoring stays false — bounce back so the user can fix it.
            val nextStep = if (restoreVM.isRestoring.value) RestoreStep.RESTORING else RestoreStep.BIRTHDAY
            onStepChange(nextStep)
        },
    )
}

@Composable
private fun RestoringStepView(
    pendingUsername: String,
    walletViewModel: WalletViewModel,
    chatBootstrap: ChatBootstrap,
    restoreVM: ZappRestoreFlowVM,
) {
    val secretState by walletViewModel.secretState.collectAsStateWithLifecycle()
    val walletProvisioningError by walletViewModel.walletProvisioningError.collectAsStateWithLifecycle()
    val chatIdentityFailed by chatBootstrap.chatIdentityFailed.collectAsStateWithLifecycle()
    val isDerivingChatIdentity by chatBootstrap.isDeriving.collectAsStateWithLifecycle()
    val restoreErrorRes by restoreVM.restoreError.collectAsStateWithLifecycle()

    val walletProvisionedFailedMsg = stringResource(R.string.onboarding_error_wallet_creation_failed)
    val chatDeriveFailedMsg = stringResource(R.string.chat_identity_setup_error_wallet_derive_failed)

    val errorMessage =
        rememberRestoringErrorMessage(
            walletErr = walletProvisioningError != null,
            walletErrMsg = walletProvisionedFailedMsg,
            restoreErr = restoreErrorRes?.getValue(),
            chatFailedAfterReady = chatIdentityFailed && secretState == SecretState.READY,
            chatErrMsg = chatDeriveFailedMsg,
        )
    val onRetry: (() -> Unit)? =
        when {
            restoreErrorRes != null -> ({ restoreVM.retryRestore(pendingUsername) })
            chatIdentityFailed && !isDerivingChatIdentity -> ({ chatBootstrap.retry() })
            else -> null
        }
    RestoreInProgressScreen(errorMessage = errorMessage, onRetry = onRetry)
}

@Composable
private fun rememberRestoringErrorMessage(
    walletErr: Boolean,
    walletErrMsg: String,
    restoreErr: String?,
    chatFailedAfterReady: Boolean,
    chatErrMsg: String,
): String? =
    when {
        walletErr -> walletErrMsg
        restoreErr != null -> restoreErr
        chatFailedAfterReady -> chatErrMsg
        else -> null
    }

@Composable
private fun SeedConfirmStepView(walletViewModel: WalletViewModel, onStepChange: (RestoreStep) -> Unit) {
    // Pull words from the persisted wallet (not the VM's in-memory entered words):
    // VM state dies on process death but rememberSaveable restores `step`, so a
    // rehydrated user would otherwise land on SEED_CONFIRM with 24 empty boxes.
    val walletSeed by walletViewModel.currentSeedWords.collectAsStateWithLifecycle()
    SeedRevealScreen(
        step = 2,
        title = stringResource(R.string.restore_flow_confirm_title),
        sub = stringResource(R.string.restore_flow_confirm_sub),
        words = walletSeed.orEmpty(),
        showBack = false,
        onBack = { },
        onContinue = { onStepChange(RestoreStep.SECURE_CHOICE) },
    )
}

@Composable
private fun SecureChoiceStepView(onStepChange: (RestoreStep) -> Unit) {
    TwoFAChoiceScreen(
        onBack = { onStepChange(RestoreStep.SEED_CONFIRM) },
        onPick = { mode ->
            onStepChange(
                when (mode) {
                    TwoFAMode.Bio -> RestoreStep.BIO_SCAN
                    TwoFAMode.Pin -> RestoreStep.PIN_SETUP
                }
            )
        },
    )
}

@Composable
private fun BioStepView(securityVM: OnboardingSecurityViewModel, onStepChange: (RestoreStep) -> Unit) {
    val bioState by securityVM.bioState.collectAsStateWithLifecycle()
    BioScanScreen(
        isEnrolling = bioState is OnboardingSecurityViewModel.BioState.Prompting,
        errorMessage = (bioState as? OnboardingSecurityViewModel.BioState.Error)?.message,
        onEnroll = { securityVM.triggerBiometricSetup() },
        onCancel = {
            securityVM.resetBioError()
            onStepChange(RestoreStep.SECURE_CHOICE)
        },
    )
}

@Composable
private fun PinStepView(securityVM: OnboardingSecurityViewModel, onStepChange: (RestoreStep) -> Unit) {
    PinSetupScreen(
        onBack = { onStepChange(RestoreStep.SECURE_CHOICE) },
        onPinConfirmed = { pin -> securityVM.savePin(pin) },
    )
}

@Composable
private fun KeepOpenStepView(restoreVM: ZappRestoreFlowVM, onComplete: () -> Unit) {
    val keepScreenOn by restoreVM.keepScreenOn.collectAsStateWithLifecycle()
    KeepZappOpenScreen(
        keepScreenOn = keepScreenOn,
        onToggleKeepScreenOn = restoreVM::onKeepScreenOnToggle,
        onEnterApp = {
            restoreVM.persistKeepScreenOn()
            onComplete()
        },
    )
}
