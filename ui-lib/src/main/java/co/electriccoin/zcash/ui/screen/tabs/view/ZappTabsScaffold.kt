package co.electriccoin.zcash.ui.screen.tabs.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.viewmodel.WalletViewModel
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import co.electriccoin.zcash.ui.screen.chat.identity.ChatIdentitySetupScreen
import co.electriccoin.zcash.ui.screen.chat.identity.ChatIdentitySetupVM
import co.electriccoin.zcash.ui.screen.chat.list.ChatListScreen
import co.electriccoin.zcash.ui.screen.onboarding.ZappOnboardingFlow
import co.electriccoin.zcash.ui.screen.onboarding.ZappRestoreFlow
import co.electriccoin.zcash.ui.screen.tabs.TabsVM
import co.electriccoin.zcash.ui.screen.welcome.WelcomeGateVM
import co.electriccoin.zcash.ui.screen.welcome.view.WelcomeGateView
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun ZappTabsScaffold(
    navigationRouter: NavigationRouter,
) {
    val welcomeGateVM: WelcomeGateVM = koinViewModel()
    val walletViewModel: WalletViewModel = koinViewModel()
    val isWelcomeDismissed by welcomeGateVM.isWelcomeDismissed.collectAsState()
    val isOnboardingCompleted by welcomeGateVM.isOnboardingCompleted.collectAsState()

    // True while the user is filling out the chat-restore form (entered via
    // WelcomeGate's "I already use Zapp"). Held locally so cancelling drops
    // them back at the welcome gate without persisting any state.
    var restoreMode by rememberSaveable { mutableStateOf(false) }

    when {
        isWelcomeDismissed == null || isOnboardingCompleted == null -> {
            Box(modifier = Modifier.fillMaxSize()) // brief blank while prefs load
        }

        restoreMode -> {
            ZappRestoreFlow(
                onComplete = {
                    welcomeGateVM.dismissWelcome()
                    welcomeGateVM.completeOnboarding()
                    restoreMode = false
                },
                onBackToWelcome = { restoreMode = false },
                walletViewModel = walletViewModel,
                chatBootstrap = koinInject(),
            )
        }

        isWelcomeDismissed == false -> {
            WelcomeGateView(
                onGetStarted = { welcomeGateVM.dismissWelcome() },
                onRestoreExisting = { restoreMode = true },
            )
        }

        isOnboardingCompleted == false -> {
            ZappOnboardingFlow(
                onComplete = { welcomeGateVM.completeOnboarding() },
                onBackToWelcome = { welcomeGateVM.undoDismissWelcome() },
                walletViewModel = walletViewModel,
                chatBootstrap = koinInject(),
                navigationRouter = navigationRouter,
            )
        }

        else -> {
            ZappTabsScaffoldContent()
        }
    }
}

@Composable
private fun ZappTabsScaffoldContent() {
    val tabsVM: TabsVM = koinViewModel()
    var currentTab by rememberSaveable { mutableStateOf(ZappTab.CHATS) }
    // Set by tab content when it pushes a fullscreen sub-screen that owns its
    // own bottom CTA (e.g. wallet seed-reveal). Hides the floating nav pill so
    // the two don't overlap.
    var hideNavPill by rememberSaveable { mutableStateOf(false) }

    val bootstrap: ChatBootstrap = koinInject()
    val unreadCount by bootstrap.totalUnreadCount.collectAsState()
    val c = ZappTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        when (currentTab) {
            ZappTab.PAY -> {
                WalletTabContent(
                    onRestoreWallet = tabsVM::onRestoreWalletClick,
                    onFullscreenChange = { hideNavPill = it },
                )
            }

            ZappTab.CHATS -> {
                ChatsTabContent()
            }

            ZappTab.YOU -> {
                SettingsTabContent(
                    onChatProfileClick = tabsVM::onChatProfileClick,
                    onContactsClick = tabsVM::onContactsClick,
                    onAppLockClick = tabsVM::onAppLockClick,
                    onChooseServerClick = tabsVM::onChooseServerClick,
                    onSwapClick = tabsVM::onSwapClick,
                    onTorClick = tabsVM::onTorClick,
                    onP2pTransactionsClick = tabsVM::onP2pTransactionsClick,
                    onSupportClick = tabsVM::onSupportClick,
                )
            }
        }

        if (!hideNavPill) {
            FloatingPillNavBar(
                currentTab = currentTab,
                chatUnreadCount = unreadCount,
                onTabSelected = { currentTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ChatsTabContent() {
    val bootstrap: ChatBootstrap = koinInject()
    val identitySetupVm: ChatIdentitySetupVM = koinViewModel()
    val isInitializing by bootstrap.isInitializing.collectAsState()
    val isSetupComplete by identitySetupVm.isSetupComplete.collectAsState()

    val c = ZappTheme.colors
    when {
        isInitializing -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.accent)
            }
        }

        !isSetupComplete -> {
            ChatIdentitySetupScreen()
        }

        else -> {
            ChatListScreen(
                showBackButton = false,
            )
        }
    }
}
