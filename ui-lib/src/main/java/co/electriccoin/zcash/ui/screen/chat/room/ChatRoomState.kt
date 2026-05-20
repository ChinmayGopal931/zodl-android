package co.electriccoin.zcash.ui.screen.chat.room

import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.screen.chat.list.ChatListChipVariant
import co.electriccoin.zcash.ui.screen.chat.list.ChatListConnectionStatus
import co.electriccoin.zcash.ui.screen.chat.list.ChatListDhtHealth
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
import co.electriccoin.zcash.ui.screen.chat.model.ConnectionDetailsUi
import co.electriccoin.zcash.ui.screen.chat.model.ReportCategory

data class ChatRoomState(
    val title: StringResource,
    val subtitle: StringResource,
    val isTitleClickable: Boolean,
    val onTitleClick: () -> Unit,
    val onBack: () -> Unit,
    val networkChip: ChatRoomNetworkChipState,
    val messages: List<ChatMessage>,
    val isLoading: Boolean,
    val input: ChatRoomInputState,
    val attachmentSheet: ChatRoomAttachmentSheetState?,
    val mediaSheet: ChatRoomMediaSheetState?,
    val networkSheet: ChatRoomNetworkSheetState?,
    val editContactSheet: ChatRoomEditContactSheetState?,
    val blockDialog: ChatRoomBlockDialogState?,
    val reportDialog: ChatRoomReportDialogState?,
)

data class ChatRoomNetworkChipState(
    val text: StringResource,
    val variant: ChatListChipVariant,
    val onClick: () -> Unit,
)

data class ChatRoomInputState(
    val value: String,
    val placeholder: StringResource,
    val canSend: Boolean,
    val attachContentDescription: StringResource,
    val sendContentDescription: StringResource,
    val onChange: (String) -> Unit,
    val onSendClick: () -> Unit,
    val onAttachClick: () -> Unit,
)

data class ChatRoomAttachmentSheetState(
    val onShareAddress: () -> Unit,
    val onSendZec: () -> Unit,
    val onAttachMedia: () -> Unit,
    val onDismiss: () -> Unit,
)

data class ChatRoomMediaSheetState(
    val onChooseMedia: () -> Unit,
    val onAttachFile: () -> Unit,
    val onTakePhoto: () -> Unit,
    val onShareLocation: () -> Unit,
    val onDismiss: () -> Unit,
)

data class ChatRoomNetworkSheetState(
    val connectionStatus: ChatListConnectionStatus,
    val peerCount: Int,
    val dhtHealth: ChatListDhtHealth,
    val connectionDetails: ConnectionDetailsUi?,
    val onDismiss: () -> Unit,
)

data class ChatRoomEditContactSheetState(
    val currentName: String,
    val onSave: (String) -> Unit,
    val onBlock: () -> Unit,
    val onReport: () -> Unit,
    val onDismiss: () -> Unit,
)

data class ChatRoomBlockDialogState(
    val displayName: String,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)

data class ChatRoomReportDialogState(
    val displayName: String,
    val onReport: (ReportCategory, String) -> Unit,
    val onBlock: () -> Unit,
    val onDismiss: () -> Unit,
)
