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
import androidx.compose.runtime.rememberCoroutineScope
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
import co.electriccoin.zcash.ui.common.provider.IsTorEnabledStorageProvider
import co.electriccoin.zcash.ui.common.viewmodel.SecretState
import co.electriccoin.zcash.ui.common.viewmodel.WalletViewModel
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import co.electriccoin.zcash.ui.screen.onboarding.view.TorOptionScreen
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
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** All steps the Swiss onboarding flow walks the user through. */
private enum class Step {
    WALLET_INTRO,
    WALLET_CHOICE,
    TOR_OPTION,
    WALLET_SEED,
    MSG_INTRO,
    MSG_USERNAME,
    DERIVING,
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
 * - **Part 1 — Wallet** (intro, create/restore, seed)
 * - **Part 2 — Messaging account** (intro, username)
 * - **Part 3 — Secure Zapp** (biometric/PIN, scan, done)
 *
 * The wallet comes first because the messaging identity is *derived from* its
 * 24-word BIP-39 seed. Only once the wallet is provisioned ([Step.WALLET_SEED] for
 * create, or the restore sub-flow returning READY) do we collect the username in
 * [Step.MSG_USERNAME] and hand it to [ChatBootstrap.setPendingDisplayName]; the
 * reactive coroutine inside [ChatBootstrap] then derives the identity from the
 * now-present seed while [Step.DERIVING] shows a spinner.
 */
@Composable
internal fun ZappOnboardingFlow(
    onComplete: () -> Unit,
    onBackToWelcome: () -> Unit,
    walletViewModel: WalletViewModel,
    chatBootstrap: ChatBootstrap,
    navigationRouter: NavigationRouter,
) {
    var step by rememberSaveable { mutableStateOf(Step.WALLET_INTRO) }
    var twoFAMode by rememberSaveable { mutableStateOf(TwoFAMode.Bio) }
    var pendingUsername by rememberSaveable { mutableStateOf("") }
    var torEnabled by rememberSaveable { mutableStateOf(false) }

    val walletSeed by walletViewModel.currentSeedWords.collectAsStateWithLifecycle()
    val secretState by walletViewModel.secretState.collectAsStateWithLifecycle()
    val walletProvisioningError by walletViewModel.walletProvisioningError.collectAsStateWithLifecycle()
    val chatIdentity by chatBootstrap.identity.collectAsStateWithLifecycle()

    val securityVM: OnboardingSecurityVM = koinViewModel()
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

    // Wallet-ready transitions. The username (and the identity derived from the seed) is
    // collected AFTER the wallet exists, so READY routes into the messaging phase rather
    // than straight to security.
    // - WALLET_CHOICE: the restore sub-flow persisted a wallet and popped back here
    //   (navigationRouter.backToRoot); move on to the messaging phase.
    // - TOR_OPTION: process-death safety — if we died between createNewWallet() and the
    //   step transition, don't strand the user on the Tor screen with a wallet already
    //   persisted underneath (tapping Continue again would overwrite it).
    LaunchedEffect(secretState, step) {
        if (secretState == SecretState.READY) {
            when (step) {
                Step.WALLET_CHOICE -> step = Step.MSG_INTRO
                Step.TOR_OPTION -> step = Step.WALLET_SEED
                else -> Unit
            }
        }
    }

    // Chat-identity gate. Once the username is set, ChatBootstrap derives the identity
    // from the persisted seed; advance when it lands. A derive failure keeps the user on
    // DERIVING (which shows the error + retry) instead of advancing.
    LaunchedEffect(chatIdentity, step) {
        if (step == Step.DERIVING && chatIdentity != null) {
            step = Step.SECURE_CHOICE
        }
    }

    // Advance to Done once biometric enrollment succeeds.
    LaunchedEffect(bioState) {
        if (bioState is OnboardingSecurityVM.BioState.Success && step == Step.BIO_SCAN) {
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
        Step.WALLET_INTRO -> {
            WalletPhaseIntro(
                onBack = onBackToWelcome,
                onContinue = { step = Step.WALLET_CHOICE },
            )
        }

        Step.WALLET_CHOICE -> {
            if (secretState == SecretState.READY) {
                // The restore sub-flow persisted a wallet and popped back here; the
                // wallet-ready effect is about to advance to MSG_INTRO. Render a spinner
                // rather than flash the create/restore chooser for a frame.
                SeedLoadingPlaceholder(sdkError = null, onRetry = null)
            } else {
                WalletChoiceScreen(
                    onBack = { step = Step.WALLET_INTRO },
                    // Create path defers wallet creation until after the Tor opt-in so the
                    // preference is persisted before the Synchronizer is wired up.
                    onCreate = { step = Step.TOR_OPTION },
                    onRestore = { navigationRouter.forward(RestoreSeedArgs) },
                )
            }
        }

        Step.TOR_OPTION -> {
            val torProvider: IsTorEnabledStorageProvider = koinInject()
            val scope = rememberCoroutineScope()
            TorOptionScreen(
                torEnabled = torEnabled,
                onToggle = { torEnabled = !torEnabled },
                onBack = { step = Step.WALLET_CHOICE },
                onContinue = {
                    // Persist the Tor preference BEFORE kicking off wallet creation so the
                    // Synchronizer reads the up-to-date value when it spins up. store() is
                    // suspend; running it sequentially in a single coroutine eliminates the
                    // race that the previous structure (concurrent scope.launch + fire-and-
                    // forget createNewWallet) had.
                    scope.launch {
                        torProvider.store(torEnabled)
                        walletViewModel.createNewWallet()
                        step = Step.WALLET_SEED
                    }
                },
                badge = stringResource(R.string.onboarding_tor_badge),
                ctaText = stringResource(R.string.onboarding_continue),
                step = 1,
                ghostNum = 1,
            )
        }

        Step.WALLET_SEED -> {
            val words = walletSeed
            val errorMessage =
                if (walletProvisioningError != null) {
                    stringResource(R.string.onboarding_error_wallet_creation_failed)
                } else {
                    null
                }
            when {
                errorMessage != null -> SeedLoadingPlaceholder(sdkError = errorMessage, onRetry = null)
                words == null -> SeedLoadingPlaceholder(sdkError = null, onRetry = null)
                else ->
                    WalletSeedPhraseScreen(
                        words = words,
                        onBack = { step = Step.WALLET_CHOICE },
                        onContinue = { step = Step.MSG_INTRO },
                    )
            }
        }

        Step.MSG_INTRO -> {
            // No back: the wallet is already committed at this point.
            MessagingPhaseIntro(
                onBack = {},
                onContinue = { step = Step.MSG_USERNAME },
                showBack = false,
            )
        }

        Step.MSG_USERNAME -> {
            UsernameEntryScreen(
                onBack = { step = Step.MSG_INTRO },
                onContinue = { name ->
                    pendingUsername = name
                    step = Step.DERIVING
                },
            )
        }

        Step.DERIVING -> DerivingIdentityScreen(step = 2, chatBootstrap = chatBootstrap)

        Step.SECURE_CHOICE -> {
            TwoFAChoiceScreen(
                onBack = { step = Step.DERIVING },
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
                isEnrolling = bioState is OnboardingSecurityVM.BioState.Prompting,
                errorMessage = (bioState as? OnboardingSecurityVM.BioState.Error)?.message,
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
    val timeoutMessage = stringResource(R.string.onboarding_seed_loading_timeout)

    LaunchedEffect(sdkError) {
        if (sdkError == null) {
            kotlinx.coroutines.delay(SEED_LOAD_TIMEOUT_MS)
            timedOut = true
        }
    }

    val displayError =
        sdkError
            ?: timeoutMessage.takeIf { timedOut }

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
                        if (onRetry != null) {
                            stringResource(R.string.onboarding_seed_loading_retry_hint)
                        } else {
                            stringResource(R.string.onboarding_seed_loading_no_retry_hint)
                        },
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
                            text = stringResource(R.string.onboarding_seed_loading_retry),
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
