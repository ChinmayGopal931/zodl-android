package co.electriccoin.zcash.ui.screen.chat.contacts

import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.screen.chat.model.ChatContact

data class ChatContactsState(
    val title: StringResource,
    val contacts: List<ChatContact>,
    val scannedPublicKey: String?,
    val scannedWalletAddress: String?,
    val showBackButton: Boolean,
    val onStartChat: (publicKey: String) -> Unit,
    val onScanPublicKey: () -> Unit,
    val onScanWalletAddress: () -> Unit,
    val onConsumeScannedPublicKey: () -> Unit,
    val onConsumeScannedWalletAddress: () -> Unit,
    val onAddContact: ChatContactsAdd,
    val onUpdateContact: ChatContactsUpdate,
    val onDeleteContact: (publicKey: String) -> Unit,
    val onBack: () -> Unit,
)

fun interface ChatContactsAdd {
    operator fun invoke(
        publicKey: String,
        name: String,
        walletAddress: String,
        walletAddresses: Map<String, String>,
    )
}

fun interface ChatContactsUpdate {
    operator fun invoke(
        publicKey: String,
        name: String,
        walletAddress: String,
        walletAddresses: Map<String, String>,
    )
}
