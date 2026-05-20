@file:Suppress("TooManyFunctions")

package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.zapp.ZappBackButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappChipVariant
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.component.zapp.ZappStatusChip
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.screen.chat.list.ChatListChipVariant
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
import co.electriccoin.zcash.ui.screen.chat.model.MessageStatus
import co.electriccoin.zcash.ui.screen.chat.room.ChatRoomEditContactSheetState
import co.electriccoin.zcash.ui.screen.chat.room.ChatRoomInputState
import co.electriccoin.zcash.ui.screen.chat.room.ChatRoomNetworkChipState
import co.electriccoin.zcash.ui.screen.chat.room.ChatRoomState
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.FileBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.LocationBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.MediaBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.PaymentRequestBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.TransactionBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.WalletAddressBubble
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun ChatRoomView(
    state: ChatRoomState,
    fallbackTitle: String,
    modifier: Modifier = Modifier,
) {
    val c = ZappTheme.colors
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            coroutineScope.launch { listState.animateScrollToItem(state.messages.size - 1) }
        }
    }

    Scaffold(
        topBar = {
            ZappScreenHeader(
                title = state.title.ifBlank { fallbackTitle },
                subtitle = state.subtitle.getValue(),
                onTitleClick = if (state.isTitleClickable) state.onTitleClick else null,
                left = { ZappBackButton(onClick = state.onBack) },
                right = { NetworkChip(state = state.networkChip) },
            )
        },
        containerColor = c.bg,
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.isLoading && state.messages.isEmpty()) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = c.accent)
                        }
                    }
                }
                items(items = state.messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(c.border),
            )

            InputRow(state = state.input)
        }
    }

    state.attachmentSheet?.let {
        AttachmentSheet(
            onShareAddress = it.onShareAddress,
            onSendZec = it.onSendZec,
            onAttachMedia = it.onAttachMedia,
            onDismiss = it.onDismiss,
        )
    }

    state.networkSheet?.let { sheet ->
        NetworkDetailsSheet(
            connectionStatus = sheet.connectionStatus,
            peerCount = sheet.peerCount,
            dhtHealth = sheet.dhtHealth,
            connectionDetails = sheet.connectionDetails,
            onDismiss = sheet.onDismiss,
        )
    }

    state.editContactSheet?.let { ContactEditSheet(state = it) }

    state.mediaSheet?.let {
        MediaAttachmentSheet(
            onChooseMedia = it.onChooseMedia,
            onAttachFile = it.onAttachFile,
            onTakePhoto = it.onTakePhoto,
            onShareLocation = it.onShareLocation,
            onDismiss = it.onDismiss,
        )
    }

    state.blockDialog?.let {
        BlockUserDialog(
            displayName = it.displayName,
            onConfirm = it.onConfirm,
            onDismiss = it.onDismiss,
        )
    }

    state.reportDialog?.let {
        ReportAndBlockDialog(
            displayName = it.displayName,
            onReport = { category, details -> it.onReport(category, details) },
            onBlock = it.onBlock,
            onDismiss = it.onDismiss,
        )
    }
}

@Composable
private fun NetworkChip(state: ChatRoomNetworkChipState) {
    val c = ZappTheme.colors
    val (variant, dotColor) =
        when (state.variant) {
            ChatListChipVariant.Success -> ZappChipVariant.Success to c.success
            ChatListChipVariant.Accent -> ZappChipVariant.Accent to c.accent
            ChatListChipVariant.Danger -> ZappChipVariant.Danger to c.danger
        }
    ZappStatusChip(
        text = state.text.getValue(),
        variant = variant,
        dotColor = dotColor,
        onClick = state.onClick,
    )
}

@Composable
private fun InputRow(state: ChatRoomInputState) {
    val c = ZappTheme.colors
    val attachContentDescription = state.attachContentDescription.getValue()
    val sendContentDescription = state.sendContentDescription.getValue()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(c.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .background(c.surfaceAlt, RectangleShape)
                    .border(BorderStroke(1.dp, c.border), RectangleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = c.accent),
                        onClick = state.onAttachClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = attachContentDescription,
                tint = c.accent,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        TextField(
            value = state.value,
            onValueChange = state.onChange,
            modifier =
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 36.dp),
            placeholder = {
                BasicText(
                    text = state.placeholder.getValue(),
                    style = ZappTheme.typography.body.copy(color = c.textSubtle),
                )
            },
            maxLines = 4,
            shape = RectangleShape,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = c.surfaceInput,
                    unfocusedContainerColor = c.surfaceInput,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = c.text,
                    unfocusedTextColor = c.text,
                    cursorColor = c.accent,
                ),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .background(if (state.canSend) c.accent else c.surfaceAlt, RectangleShape)
                    .border(BorderStroke(1.dp, c.border), RectangleShape)
                    .clickable(
                        enabled = state.canSend,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = c.onAccent),
                        onClick = state.onSendClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.ArrowUpward,
                contentDescription = sendContentDescription,
                tint = if (state.canSend) c.onAccent else c.textSubtle,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactEditSheet(state: ChatRoomEditContactSheetState) {
    val c = ZappTheme.colors
    var nameInput by remember(state.currentName) { mutableStateOf(TextFieldValue(state.currentName)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = state.onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        shape = RectangleShape,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText(
                text = stringResource(R.string.chat_room_edit_contact_title),
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
            )
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text(stringResource(R.string.chat_room_edit_contact_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ContactEditButton(
                    label = stringResource(R.string.chat_room_edit_contact_cancel),
                    background = c.surfaceAlt,
                    textColor = c.text,
                    onClick = state.onDismiss,
                    modifier = Modifier.weight(1f),
                )
                val canSave = nameInput.text.isNotBlank()
                ContactEditButton(
                    label = stringResource(R.string.chat_room_edit_contact_save),
                    background = if (canSave) c.accent else c.surfaceAlt,
                    textColor = if (canSave) c.onAccent else c.textMuted,
                    enabled = canSave,
                    onClick = { state.onSave(nameInput.text.trim()) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(c.border),
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ContactEditButton(
                    label = stringResource(R.string.chat_room_edit_contact_report),
                    background = c.surfaceAlt,
                    textColor = c.text,
                    onClick = state.onReport,
                    modifier = Modifier.weight(1f),
                )
                ContactEditButton(
                    label = stringResource(R.string.chat_room_edit_contact_block),
                    background = c.danger.copy(alpha = DANGER_BG_ALPHA),
                    textColor = c.danger,
                    onClick = state.onBlock,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ContactEditButton(
    label: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .height(48.dp)
                .background(background, RectangleShape)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = ZappTheme.typography.button.copy(color = textColor, fontWeight = FontWeight.Black),
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val c = ZappTheme.colors
    val isFromMe = message.isFromMe
    val contentType = resolveContentType(message)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
    ) {
        if (!isFromMe && message.senderName != null) {
            BasicText(
                text = message.senderName,
                style = ZappTheme.typography.chip.copy(color = c.accent),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
        }

        when {
            contentType == CONTENT_TYPE_PAYMENT_REQUEST ->
                PaymentRequestBubble(message = message, isFromMe = isFromMe)
            contentType == CONTENT_TYPE_WALLET_ADDRESS ->
                WalletAddressBubble(message = message, isFromMe = isFromMe)
            contentType == CONTENT_TYPE_ZEC_TRANSACTION ->
                TransactionBubble(message = message, isFromMe = isFromMe)
            contentType == CONTENT_TYPE_LOCATION ->
                LocationBubble(message = message, isFromMe = isFromMe)
            contentType.startsWith(IMAGE_MIME_PREFIX) ->
                MediaBubble(message = message, isFromMe = isFromMe)
            contentType.startsWith(VIDEO_MIME_PREFIX) ->
                MediaBubble(message = message, isFromMe = isFromMe)
            message.mediaId != null ->
                FileBubble(message = message, isFromMe = isFromMe)
            else -> TextMessageBubble(message = message, isFromMe = isFromMe)
        }
    }
}

@Suppress("TooGenericExceptionCaught")
private fun resolveContentType(message: ChatMessage): String {
    val declared = message.contentType
    if (!declared.isNullOrEmpty() && declared != CONTENT_TYPE_TEXT_PLAIN) return declared
    return try {
        JSONObject(message.content).optString("contentType", "").takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    } ?: CONTENT_TYPE_TEXT_PLAIN
}

@Composable
private fun TextMessageBubble(message: ChatMessage, isFromMe: Boolean) {
    val c = ZappTheme.colors
    Box(
        modifier =
            Modifier
                .widthIn(max = 280.dp)
                .background(if (isFromMe) c.accent else c.surfaceAlt, RectangleShape)
                .padding(12.dp),
    ) {
        Column {
            BasicText(
                text = message.content,
                style =
                    ZappTheme.typography.body.copy(
                        color = if (isFromMe) c.onAccent else c.text,
                    ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BasicText(
                    text = formatMessageTime(message.timestamp),
                    style =
                        ZappTheme.typography.caption.copy(
                            color = if (isFromMe) c.onAccent.copy(alpha = OUTGOING_META_ALPHA) else c.textMuted,
                        ),
                )
                if (isFromMe) {
                    DeliveryIndicator(
                        status = message.status,
                        tintColor = c.onAccent.copy(alpha = OUTGOING_META_ALPHA),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryIndicator(status: MessageStatus?, tintColor: Color) {
    val c = ZappTheme.colors
    when (status) {
        MessageStatus.SENT ->
            Icon(
                Icons.Default.Done,
                contentDescription = stringResource(R.string.chat_room_delivery_sent),
                modifier = Modifier.size(13.dp),
                tint = tintColor,
            )
        MessageStatus.QUEUED ->
            Icon(
                Icons.Default.Schedule,
                contentDescription = stringResource(R.string.chat_room_delivery_queued),
                modifier = Modifier.size(13.dp),
                tint = tintColor,
            )
        MessageStatus.FAILED ->
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = stringResource(R.string.chat_room_delivery_failed),
                modifier = Modifier.size(13.dp),
                tint = c.danger,
            )
        MessageStatus.SENDING ->
            Icon(
                Icons.Default.MoreHoriz,
                contentDescription = stringResource(R.string.chat_room_delivery_sending),
                modifier = Modifier.size(13.dp),
                tint = tintColor,
            )
        null -> {}
    }
}

private fun formatMessageTime(epochMillis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))

private const val CONTENT_TYPE_TEXT_PLAIN = "text/plain"
private const val CONTENT_TYPE_PAYMENT_REQUEST = "application/payment-request"
private const val CONTENT_TYPE_WALLET_ADDRESS = "application/wallet-address"
private const val CONTENT_TYPE_ZEC_TRANSACTION = "application/zec-transaction"
private const val CONTENT_TYPE_LOCATION = "application/location"
private const val IMAGE_MIME_PREFIX = "image/"
private const val VIDEO_MIME_PREFIX = "video/"
private const val OUTGOING_META_ALPHA = 0.7f
private const val DANGER_BG_ALPHA = 0.1f
