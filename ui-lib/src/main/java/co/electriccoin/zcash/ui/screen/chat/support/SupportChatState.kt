package co.electriccoin.zcash.ui.screen.chat.support

data class SupportUiMessage(
    val id: String,
    val content: String,
    val isFromMe: Boolean,
    val timestamp: Long,
)

sealed class SupportChatUiState {
    data object Loading : SupportChatUiState()

    /** New ticket — user has not yet selected a category. */
    data object SelectCategory : SupportChatUiState()

    /** Category has been selected; free-form chat is active. */
    data class Chat(
        val messages: List<SupportUiMessage>,
        val input: String,
    ) : SupportChatUiState()
}

data class SupportChatScreenState(
    val uiState: SupportChatUiState,
    val onCategorySelected: (SupportCategory) -> Unit,
    val onInputChange: (String) -> Unit,
    val onSend: () -> Unit,
    val onAttach: () -> Unit,
    val onLeave: () -> Unit,
    val onBack: () -> Unit,
    val leaveDialog: SupportLeaveDialogState?,
    val mediaSheet: SupportMediaSheetState?,
)

data class SupportMediaSheetState(
    val onChooseMedia: () -> Unit,
    val onAttachFile: () -> Unit,
    val onTakePhoto: () -> Unit,
    val onDismiss: () -> Unit,
)

data class SupportLeaveDialogState(
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)
