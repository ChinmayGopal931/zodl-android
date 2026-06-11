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
    SEED_ENTRY,
    BIRTHDAY,
    TOR,
    RESTORING,
    SEED_CONFIRM,
    USERNAME,
    DERIVING,
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
internal fun ZappRestoreFlow(
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
    val securityVM: OnboardingSecurityVM = koinViewModel()

    var step by rememberSaveable { mutableStateOf(RestoreStep.SEED_ENTRY) }
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
    securityVM: OnboardingSecurityVM,
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

    // Wallet-provisioning gate. Once the seed is persisted, move to the seed-backup
    // confirmation. The chat identity is NOT derived here: the username (and the
    // identity derived from the seed) is collected *after* the wallet exists, so the
    // wallet comes first.
    LaunchedEffect(secretState, step) {
        if (step == RestoreStep.RESTORING && secretState == SecretState.READY) {
            restoreVM.markRestoreCompleted()
            onStepChange(RestoreStep.SEED_CONFIRM)
        }
    }

    // Chat-identity gate. After the username is entered, ChatBootstrap derives the
    // identity from the now-persisted seed. A derive failure deliberately does NOT
    // advance: the user stays on DERIVING, which surfaces the error and a retry.
    LaunchedEffect(chatIdentity, step) {
        if (step == RestoreStep.DERIVING && chatIdentity != null) {
            onStepChange(RestoreStep.SECURE_CHOICE)
        }
    }

    LaunchedEffect(bioState, step) {
        if (bioState is OnboardingSecurityVM.BioState.Success && step == RestoreStep.BIO_SCAN) {
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
    onPendingUsernameChange: (String) -> Unit,
    onBackToWelcome: () -> Unit,
    onComplete: () -> Unit,
    walletViewModel: WalletViewModel,
    chatBootstrap: ChatBootstrap,
    restoreVM: ZappRestoreFlowVM,
    securityVM: OnboardingSecurityVM,
) {
    when (step) {
        RestoreStep.SEED_ENTRY -> SeedEntryStepView(restoreVM, onBackToWelcome, onStepChange)
        RestoreStep.BIRTHDAY -> BirthdayStepView(restoreVM, onStepChange)
        RestoreStep.TOR -> TorStepView(restoreVM, onStepChange)
        RestoreStep.RESTORING -> RestoringStepView(walletViewModel, restoreVM)
        RestoreStep.SEED_CONFIRM -> SeedConfirmStepView(walletViewModel, onStepChange)

        RestoreStep.USERNAME ->
            UsernameEntryScreen(
                onBack = { onStepChange(RestoreStep.SEED_CONFIRM) },
                onContinue = { name ->
                    onPendingUsernameChange(name)
                    onStepChange(RestoreStep.DERIVING)
                },
            )

        RestoreStep.DERIVING -> DerivingIdentityScreen(step = 2, chatBootstrap = chatBootstrap)
        RestoreStep.SECURE_CHOICE -> SecureChoiceStepView(onStepChange)
        RestoreStep.BIO_SCAN -> BioStepView(securityVM, onStepChange)
        RestoreStep.PIN_SETUP -> PinStepView(securityVM, onStepChange)
        RestoreStep.KEEP_OPEN -> KeepOpenStepView(restoreVM, onComplete)
    }
}

@Composable
private fun SeedEntryStepView(
    restoreVM: ZappRestoreFlowVM,
    onBack: () -> Unit,
    onStepChange: (RestoreStep) -> Unit,
) {
    val seedFieldState by restoreVM.seedFieldState.collectAsStateWithLifecycle()
    val validSeed by restoreVM.validSeed.collectAsStateWithLifecycle()
    val suggestionsVisible by restoreVM.suggestionsVisible.collectAsStateWithLifecycle()
    val suggestionsList by restoreVM.suggestionsList.collectAsStateWithLifecycle()
    RestoreSeedEntryScreen(
        seedState = seedFieldState,
        suggestionsVisible = suggestionsVisible,
        suggestions = suggestionsList,
        isSeedValid = validSeed != null,
        onBack = onBack,
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
    restoreVM: ZappRestoreFlowVM,
    onStepChange: (RestoreStep) -> Unit,
) {
    val torEnabled by restoreVM.torEnabled.collectAsStateWithLifecycle()
    RestoreTorOptionScreen(
        torEnabled = torEnabled,
        onToggle = restoreVM::onTorToggle,
        onBack = { onStepChange(RestoreStep.BIRTHDAY) },
        onRestore = {
            restoreVM.startRestore()
            // If the VM rejected the start (invalid birthday), birthdayError is
            // set and isRestoring stays false — bounce back so the user can fix it.
            val nextStep = if (restoreVM.isRestoring.value) RestoreStep.RESTORING else RestoreStep.BIRTHDAY
            onStepChange(nextStep)
        },
    )
}

@Composable
private fun RestoringStepView(
    walletViewModel: WalletViewModel,
    restoreVM: ZappRestoreFlowVM,
) {
    val walletProvisioningError by walletViewModel.walletProvisioningError.collectAsStateWithLifecycle()
    val restoreErrorRes by restoreVM.restoreError.collectAsStateWithLifecycle()

    val walletProvisionedFailedMsg = stringResource(R.string.onboarding_error_wallet_creation_failed)

    val errorMessage =
        when {
            walletProvisioningError != null -> walletProvisionedFailedMsg
            else -> restoreErrorRes?.getValue()
        }
    val onRetry: (() -> Unit)? =
        if (restoreErrorRes != null) ({ restoreVM.retryRestore() }) else null
    RestoreInProgressScreen(step = 1, errorMessage = errorMessage, onRetry = onRetry)
}

@Composable
private fun SeedConfirmStepView(walletViewModel: WalletViewModel, onStepChange: (RestoreStep) -> Unit) {
    // Pull words from the persisted wallet (not the VM's in-memory entered words):
    // VM state dies on process death but rememberSaveable restores `step`, so a
    // rehydrated user would otherwise land on SEED_CONFIRM with 24 empty boxes.
    val walletSeed by walletViewModel.currentSeedWords.collectAsStateWithLifecycle()
    SeedRevealScreen(
        step = 1,
        title = stringResource(R.string.restore_flow_confirm_title),
        sub = stringResource(R.string.restore_flow_confirm_sub),
        words = walletSeed.orEmpty(),
        showBack = false,
        onBack = { },
        onContinue = { onStepChange(RestoreStep.USERNAME) },
    )
}

@Composable
private fun SecureChoiceStepView(onStepChange: (RestoreStep) -> Unit) {
    TwoFAChoiceScreen(
        onBack = { onStepChange(RestoreStep.DERIVING) },
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
private fun BioStepView(securityVM: OnboardingSecurityVM, onStepChange: (RestoreStep) -> Unit) {
    val bioState by securityVM.bioState.collectAsStateWithLifecycle()
    BioScanScreen(
        isEnrolling = bioState is OnboardingSecurityVM.BioState.Prompting,
        errorMessage = (bioState as? OnboardingSecurityVM.BioState.Error)?.message,
        onEnroll = { securityVM.triggerBiometricSetup() },
        onCancel = {
            securityVM.resetBioError()
            onStepChange(RestoreStep.SECURE_CHOICE)
        },
    )
}

@Composable
private fun PinStepView(securityVM: OnboardingSecurityVM, onStepChange: (RestoreStep) -> Unit) {
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
