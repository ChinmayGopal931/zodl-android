package co.electriccoin.zcash.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.viewmodel.SecretState
import co.electriccoin.zcash.ui.common.viewmodel.WalletViewModel
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import co.electriccoin.zcash.ui.screen.onboarding.view.BioScanScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.MessagingPhaseIntro
import co.electriccoin.zcash.ui.screen.onboarding.view.OnboardingDoneScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.PinSetupScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.TwoFAChoiceScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.TwoFAMode
import co.electriccoin.zcash.ui.screen.onboarding.view.UsernameEntryScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.WalletChoiceScreen
import co.electriccoin.zcash.ui.screen.onboarding.view.WalletPhaseIntro
import co.electriccoin.zcash.ui.screen.onboarding.view.WalletSeedPhraseScreen
import co.electriccoin.zcash.ui.screen.restore.seed.RestoreSeedArgs
import org.koin.androidx.compose.koinViewModel

/** All steps the Swiss onboarding flow walks the user through. */
private enum class Step {
    MSG_INTRO,
    MSG_USERNAME,
    WALLET_INTRO,
    WALLET_CHOICE,
    WALLET_SEED,
    SECURE_CHOICE,
    BIO_SCAN,
    PIN_SETUP,
    DONE,
}

/**
 * Swiss-design post-welcome onboarding orchestrator.
 *
 * Runs after [co.electriccoin.zcash.ui.screen.welcome.view.WelcomeGateView] is
 * dismissed and before the user reaches the tabs shell. Three phases mirror the
 * design canvas:
 * - **Part 1 — Messaging account** (intro, username)
 * - **Part 2 — Wallet** (intro, create/restore/skip, seed)
 * - **Part 3 — Secure Zapp** (biometric/PIN, scan, done)
 *
 * The wallet's 24-word BIP-39 phrase seeds the messaging identity. The username
 * picked in [Step.MSG_USERNAME] is handed to [ChatBootstrap.setPendingDisplayName];
 * a reactive coroutine inside [ChatBootstrap] derives the identity from the wallet
 * seed as soon as both the SDK and the wallet are ready. This keeps both the
 * Create and the Restore paths converging on the same code, and survives the
 * navigation jump into the wallet-restore sub-flow.
 */
@Composable
fun ZappOnboardingFlow(
    onComplete: () -> Unit,
    onBackToWelcome: () -> Unit,
    walletViewModel: WalletViewModel,
    chatBootstrap: ChatBootstrap,
    navigationRouter: NavigationRouter,
) {
    var step by rememberSaveable { mutableStateOf(Step.MSG_INTRO) }
    var twoFAMode by rememberSaveable { mutableStateOf(TwoFAMode.Bio) }
    var pendingUsername by rememberSaveable { mutableStateOf("") }

    val walletSeed by walletViewModel.currentSeedWords.collectAsStateWithLifecycle()
    val secretState by walletViewModel.secretState.collectAsStateWithLifecycle()
    val walletProvisioningError by walletViewModel.walletProvisioningError.collectAsStateWithLifecycle()
    val chatIdentityFailed by chatBootstrap.chatIdentityFailed.collectAsStateWithLifecycle()
    val isDerivingChatIdentity by chatBootstrap.isDeriving.collectAsStateWithLifecycle()
    val chatIdentity by chatBootstrap.identity.collectAsStateWithLifecycle()

    val securityVM: OnboardingSecurityViewModel = koinViewModel()
    val bioState by securityVM.bioState.collectAsStateWithLifecycle()
    val pinSaved by securityVM.pinSaved.collectAsStateWithLifecycle()

    // Process-death recovery: rememberSaveable restores `pendingUsername`, but the
    // in-process `pendingDisplayName` inside ChatBootstrap dies with the process.
    // Re-publish once per rehydration. Keyed on `pendingUsername` alone (NOT `step`)
    // so step transitions don't re-fire this; `setPendingDisplayName` is idempotent
    // for the same name, but re-firing on every step change risked masking transient
    // state if its semantics ever drift.
    LaunchedEffect(pendingUsername) {
        if (pendingUsername.isNotBlank()) {
            chatBootstrap.setPendingDisplayName(pendingUsername)
        }
    }

    // Auto-advance from WALLET_CHOICE → SECURE_CHOICE only when the wallet AND the
    // chat identity are both ready. If chat-derive failed, bounce to WALLET_SEED so
    // the user sees the error and can retry — otherwise the restore path would jump
    // silently past every error surface we have. If derivation is still in flight,
    // hold at WALLET_CHOICE.
    LaunchedEffect(secretState, chatIdentity, chatIdentityFailed, step) {
        if (secretState == SecretState.READY && step == Step.WALLET_CHOICE) {
            when {
                chatIdentity != null -> step = Step.SECURE_CHOICE
                chatIdentityFailed -> step = Step.WALLET_SEED
            }
        }
    }

    // Advance to Done once biometric enrollment succeeds.
    LaunchedEffect(bioState) {
        if (bioState is OnboardingSecurityViewModel.BioState.Success && step == Step.BIO_SCAN) {
            step = Step.DONE
        }
    }

    // Advance to Done once PIN is saved.
    LaunchedEffect(pinSaved) {
        if (pinSaved && step == Step.PIN_SETUP) {
            step = Step.DONE
        }
    }

    when (step) {
        Step.MSG_INTRO -> {
            MessagingPhaseIntro(
                onBack = onBackToWelcome,
                onContinue = { step = Step.MSG_USERNAME },
            )
        }

        Step.MSG_USERNAME -> {
            UsernameEntryScreen(
                onBack = { step = Step.MSG_INTRO },
                onContinue = { name ->
                    pendingUsername = name
                    step = Step.WALLET_INTRO
                },
            )
        }

        Step.WALLET_INTRO -> {
            WalletPhaseIntro(
                onBack = { step = Step.MSG_USERNAME },
                onContinue = { step = Step.WALLET_CHOICE },
            )
        }

        Step.WALLET_CHOICE -> {
            WalletChoiceScreen(
                onBack = { step = Step.WALLET_INTRO },
                onCreate = {
                    chatBootstrap.setPendingDisplayName(pendingUsername)
                    walletViewModel.createNewWallet()
                    step = Step.WALLET_SEED
                },
                onRestore = {
                    chatBootstrap.setPendingDisplayName(pendingUsername)
                    navigationRouter.forward(RestoreSeedArgs)
                },
            )
        }

        Step.WALLET_SEED -> {
            val words = walletSeed
            // Wallet-creation failure takes precedence: without a wallet, there's nothing for
            // chat-identity derivation to operate on, so its error (if any) is a downstream
            // symptom. Once wallet exists, surface chat-derivation failures alone.
            val errorMessage =
                when {
                    walletProvisioningError != null ->
                        stringResource(R.string.onboarding_error_wallet_creation_failed)
                    chatIdentityFailed ->
                        stringResource(R.string.chat_identity_setup_error_wallet_derive_failed)
                    else -> null
                }
            // Only chat-identity failures are retryable from here; a wallet-creation failure
            // means there's no seed to derive from, so a retry of the chat path would just
            // fail again. The user has to go back to WALLET_CHOICE. Gate on `isDeriving` so a
            // spammed button doesn't queue redundant PBKDF2 round-trips.
            val onRetry: (() -> Unit)? =
                if (walletProvisioningError == null && chatIdentityFailed && !isDerivingChatIdentity) {
                    { chatBootstrap.retry() }
                } else {
                    null
                }
            when {
                // An error suppresses the seed-phrase display: on the restore path `words` is
                // the phrase the user just typed, so re-showing it adds nothing and risks
                // burying the actionable error.
                errorMessage != null -> SeedLoadingPlaceholder(sdkError = errorMessage, onRetry = onRetry)
                words == null -> SeedLoadingPlaceholder(sdkError = null, onRetry = null)
                else ->
                    WalletSeedPhraseScreen(
                        words = words,
                        onBack = { step = Step.WALLET_CHOICE },
                        onContinue = { step = Step.SECURE_CHOICE },
                    )
            }
        }

        Step.SECURE_CHOICE -> {
            TwoFAChoiceScreen(
                onBack = { step = Step.WALLET_INTRO },
                onPick = { mode ->
                    twoFAMode = mode
                    step =
                        when (mode) {
                            TwoFAMode.Bio -> Step.BIO_SCAN
                            TwoFAMode.Pin -> Step.PIN_SETUP
                        }
                },
            )
        }

        Step.BIO_SCAN -> {
            BioScanScreen(
                isEnrolling = bioState is OnboardingSecurityViewModel.BioState.Prompting,
                errorMessage = (bioState as? OnboardingSecurityViewModel.BioState.Error)?.message,
                onEnroll = { securityVM.triggerBiometricSetup() },
                onCancel = {
                    securityVM.resetBioError()
                    step = Step.SECURE_CHOICE
                },
            )
        }

        Step.PIN_SETUP -> {
            PinSetupScreen(
                onBack = { step = Step.SECURE_CHOICE },
                onPinConfirmed = { pin -> securityVM.savePin(pin) },
            )
        }

        Step.DONE -> {
            OnboardingDoneScreen(
                mode = twoFAMode,
                onEnter = onComplete,
            )
        }
    }
}

private const val SEED_LOAD_TIMEOUT_MS = 15_000L

/**
 * Brief skeleton shown while the SDK finishes generating the recovery phrase
 * (chat) or persisting the new wallet (wallet). On the happy path this flashes
 * for under a second; if [sdkError] is non-null we surface it immediately. If
 * the SDK doesn't error but also doesn't finish within [SEED_LOAD_TIMEOUT_MS],
 * we fall back to a generic message so the user isn't trapped on a spinner.
 */
@Composable
private fun SeedLoadingPlaceholder(sdkError: String?, onRetry: (() -> Unit)?) {
    val c = ZappTheme.colors
    var timedOut by remember { mutableStateOf(false) }

    LaunchedEffect(sdkError) {
        if (sdkError == null) {
            kotlinx.coroutines.delay(SEED_LOAD_TIMEOUT_MS)
            timedOut = true
        }
    }

    val displayError =
        sdkError
            ?: "Taking longer than expected.".takeIf { timedOut }

    Box(
        modifier = Modifier.fillMaxSize().background(c.bg),
        contentAlignment = Alignment.Center,
    ) {
        if (displayError != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BasicText(
                    text = displayError,
                    style =
                        ZappTheme.typography.body.copy(
                            color = c.danger,
                            fontSize = 13.sp,
                        ),
                )
                Spacer(Modifier.height(12.dp))
                BasicText(
                    text =
                        if (onRetry != null) "Tap retry, or go back and submit again."
                        else "Try going back and submitting again.",
                    style =
                        ZappTheme.typography.body.copy(
                            color = c.textMuted,
                            fontSize = 12.sp,
                        ),
                )
                if (onRetry != null) {
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier =
                            Modifier
                                .border(width = 2.dp, color = c.text, shape = RectangleShape)
                                .clickable(onClick = onRetry)
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                    ) {
                        BasicText(
                            text = "Retry",
                            style =
                                ZappTheme.typography.body.copy(
                                    color = c.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp,
                                ),
                        )
                    }
                }
            }
        } else {
            CircularProgressIndicator(color = c.accent)
        }
    }
}
