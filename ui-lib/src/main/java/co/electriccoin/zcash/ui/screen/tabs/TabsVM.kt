package co.electriccoin.zcash.ui.screen.tabs

import androidx.lifecycle.ViewModel
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.screen.chat.ChatProfileArgs
import co.electriccoin.zcash.ui.screen.chat.SupportTicketListArgs
import co.electriccoin.zcash.ui.screen.chooseserver.ChooseServerArgs
import co.electriccoin.zcash.ui.screen.restore.seed.RestoreSeedArgs
import co.electriccoin.zcash.ui.screen.securitysettings.SecuritySettingsArgs
import co.electriccoin.zcash.ui.screen.settings.p2p.P2pTransactionsArgs

class TabsVM(
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    fun onRestoreWalletClick() = navigationRouter.forward(RestoreSeedArgs)

    fun onChatProfileClick() = navigationRouter.forward(ChatProfileArgs)

    fun onAppLockClick() = navigationRouter.forward(SecuritySettingsArgs)

    fun onChooseServerClick() = navigationRouter.forward(ChooseServerArgs)

    fun onP2pTransactionsClick() = navigationRouter.forward(P2pTransactionsArgs)

    fun onSupportClick() = navigationRouter.forward(SupportTicketListArgs)
}
