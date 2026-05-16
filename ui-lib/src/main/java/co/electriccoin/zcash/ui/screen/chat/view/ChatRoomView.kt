package co.electriccoin.zcash.ui.screen.chat.view

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import co.electriccoin.zcash.ui.design.component.zapp.ZappBackButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.component.zapp.ZappStatusChip
import co.electriccoin.zcash.ui.design.component.zapp.ZappChipVariant
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.media.rememberCameraCaptureState
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
import co.electriccoin.zcash.ui.screen.chat.model.ConversationType
import co.electriccoin.zcash.ui.screen.chat.model.MessageStatus
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.FileBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.LocationBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.MediaBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.PaymentRequestBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.TransactionBubble
import co.electriccoin.zcash.ui.screen.chat.view.bubbles.WalletAddressBubble
import co.electriccoin.zcash.ui.screen.chat.viewmodel.ChatViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatRoomView(
    conversationId: String,
    onNavigateBack: () -> Unit,
    onSendZec: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel
) {
    val c = ZappTheme.colors
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val currentConversation by viewModel.currentConversation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val peerCount by viewModel.peerCount.collectAsState()
    val dhtHealth by viewModel.dhtHealth.collectAsState()
    val conversationPeerOnline by viewModel.conversationPeerOnline.collectAsState()

    var messageInput by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showMediaSheet by remember { mutableStateOf(false) }
    var showNetworkSheet by remember { mutableStateOf(false) }
    var showEditContact by remember { mutableStateOf(false) }
    val connectionDetails by viewModel.connectionDetails.collectAsState()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.processAndSendMedia(conversationId, it) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.processAndSendFile(conversationId, it) }
    }

    val cameraCaptureState = rememberCameraCaptureState(context) { uri ->
        viewModel.processAndSendCameraCapture(conversationId, uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraCaptureState.launch()
        else Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            coroutineScope.launch { shareLocation(context, viewModel, conversationId) }
        } else {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    // Map connection status to Zapp chip variants
    val (chipVariant, chipDot, chipText) = when (connectionStatus) {
        ChatViewModel.ConnectionStatus.CONNECTED -> when {
            conversationPeerOnline == true -> Triple(ZappChipVariant.Success, c.success, "Online")
            dhtHealth == ChatViewModel.DhtHealth.CRITICAL -> Triple(ZappChipVariant.Danger, c.danger, "DHT")
            peerCount > 0 -> Triple(ZappChipVariant.Success, c.success, "$peerCount")
            else -> Triple(ZappChipVariant.Accent, c.accent, "...")
        }
        ChatViewModel.ConnectionStatus.CONNECTING -> Triple(ZappChipVariant.Accent, c.accent, "...")
        ChatViewModel.ConnectionStatus.DISCONNECTED -> Triple(ZappChipVariant.Danger, c.danger, "Off")
        ChatViewModel.ConnectionStatus.ERROR -> Triple(ZappChipVariant.Danger, c.danger, "Err")
    }

    val subtitleText = when (connectionStatus) {
        ChatViewModel.ConnectionStatus.CONNECTED -> when {
            conversationPeerOnline == true -> "Peer online"
            dhtHealth == ChatViewModel.DhtHealth.CRITICAL -> "DHT unreachable"
            conversationPeerOnline == false -> "Peer offline — messages queued"
            peerCount > 0 -> "P2P connected"
            dhtHealth == ChatViewModel.DhtHealth.DEGRADED -> "DHT degraded"
            else -> "Waiting for peer..."
        }
        ChatViewModel.ConnectionStatus.CONNECTING -> "Connecting..."
        ChatViewModel.ConnectionStatus.DISCONNECTED -> "Offline"
        ChatViewModel.ConnectionStatus.ERROR -> "Connection error"
    }

    Scaffold(
        topBar = {
            ZappScreenHeader(
                title = currentConversation?.displayName ?: "Chat",
                subtitle = subtitleText,
                onTitleClick = if (currentConversation?.type == ConversationType.DIRECT) {
                    { showEditContact = true }
                } else null,
                left = { ZappBackButton(onClick = onNavigateBack) },
                right = {
                    ZappStatusChip(
                        text = chipText,
                        variant = chipVariant,
                        dotColor = chipDot,
                        onClick = {
                            viewModel.fetchConnectionDetails()
                            showNetworkSheet = true
                        },
                    )
                },
            )
        },
        containerColor = c.bg,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isLoading && messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = c.accent)
                        }
                    }
                }
                items(items = messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }

            // Hairline divider above input row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(c.border),
            )

            // Message input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Attach button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(c.surfaceAlt, RectangleShape)
                        .border(androidx.compose.foundation.BorderStroke(1.dp, c.border), RectangleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = c.accent),
                            onClick = { showAttachmentSheet = true },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Attach",
                        tint = c.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 36.dp),
                    placeholder = {
                        BasicText(
                            "Message",
                            style = ZappTheme.typography.body.copy(color = c.textSubtle),
                        )
                    },
                    maxLines = 4,
                    shape = RectangleShape,
                    colors = TextFieldDefaults.colors(
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

                // Send button
                val canSend = messageInput.text.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (canSend) c.accent else c.surfaceAlt, RectangleShape)
                        .border(androidx.compose.foundation.BorderStroke(1.dp, c.border), RectangleShape)
                        .clickable(
                            enabled = canSend,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = c.onAccent),
                            onClick = {
                                val text = messageInput.text.trim()
                                if (text.isNotEmpty()) {
                                    viewModel.sendMessage(conversationId, text)
                                    messageInput = TextFieldValue("")
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = if (canSend) c.onAccent else c.textSubtle,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    if (showAttachmentSheet) {
        AttachmentSheet(
            onShareAddress = {
                showAttachmentSheet = false
                viewModel.shareMyAddress(conversationId)
            },
            onSendZec = {
                showAttachmentSheet = false
                onSendZec()
            },
            onAttachMedia = {
                showAttachmentSheet = false
                showMediaSheet = true
            },
            onDismiss = { showAttachmentSheet = false },
        )
    }

    if (showNetworkSheet) {
        NetworkDetailsSheet(
            connectionStatus = connectionStatus,
            peerCount = peerCount,
            dhtHealth = dhtHealth,
            connectionDetails = connectionDetails,
            onDismiss = { showNetworkSheet = false },
        )
    }

    if (showEditContact) {
        val peerPublicKey = currentConversation?.participantIds?.firstOrNull() ?: ""
        ContactEditSheet(
            currentName = currentConversation?.displayName ?: "",
            onDismiss = { showEditContact = false },
            onSave = { newName ->
                if (peerPublicKey.isNotEmpty()) {
                    viewModel.updateContact(peerPublicKey, newName)
                }
                showEditContact = false
            },
        )
    }

    if (showMediaSheet) {
        MediaAttachmentSheet(
            onChooseMedia = {
                showMediaSheet = false
                mediaPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onAttachFile = {
                showMediaSheet = false
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            onTakePhoto = {
                showMediaSheet = false
                val hasPerm = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) cameraCaptureState.launch()
                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onShareLocation = {
                showMediaSheet = false
                val hasPerm = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    coroutineScope.launch { shareLocation(context, viewModel, conversationId) }
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                }
            },
            onDismiss = { showMediaSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactEditSheet(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val c = ZappTheme.colors
    var nameInput by remember { mutableStateOf(TextFieldValue(currentName)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        shape = RectangleShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText(
                text = "Edit Contact",
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
            )
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(c.surfaceAlt, RectangleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Cancel",
                        style = ZappTheme.typography.button.copy(
                            color = c.text,
                            fontWeight = FontWeight.Black,
                        ),
                    )
                }
                val canSave = nameInput.text.isNotBlank()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(if (canSave) c.accent else c.surfaceAlt, RectangleShape)
                        .clickable(enabled = canSave, onClick = { onSave(nameInput.text.trim()) }),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Save",
                        style = ZappTheme.typography.button.copy(
                            color = if (canSave) c.onAccent else c.textMuted,
                            fontWeight = FontWeight.Black,
                        ),
                    )
                }
            }
        }
    }
}

@Suppress("MissingPermission")
private suspend fun shareLocation(
    context: android.content.Context,
    viewModel: ChatViewModel,
    conversationId: String
) {
    try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
            cont.invokeOnCancellation { cts.cancel() }
        }
        if (location != null) {
            viewModel.sendLocationMessage(
                conversationId,
                location.latitude,
                location.longitude,
                location.accuracy
            )
        } else {
            Toast.makeText(context, "Could not get location", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Location error: ${e.message ?: ""}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isFromMe = message.isFromMe
    val c = ZappTheme.colors
    val contentType = run {
        val declared = message.contentType
        if (!declared.isNullOrEmpty() && declared != "text/plain") {
            declared
        } else {
            try { JSONObject(message.content).optString("contentType", "").takeIf { it.isNotEmpty() } }
            catch (_: Exception) { null }
        } ?: "text/plain"
    }

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
            contentType == "application/payment-request" ->
                PaymentRequestBubble(message = message, isFromMe = isFromMe)
            contentType == "application/wallet-address" ->
                WalletAddressBubble(message = message, isFromMe = isFromMe)
            contentType == "application/zec-transaction" ->
                TransactionBubble(message = message, isFromMe = isFromMe)
            contentType == "application/location" ->
                LocationBubble(message = message, isFromMe = isFromMe)
            contentType.startsWith("image/") ->
                MediaBubble(message = message, isFromMe = isFromMe)
            contentType.startsWith("video/") ->
                MediaBubble(message = message, isFromMe = isFromMe)
            message.mediaId != null ->
                FileBubble(message = message, isFromMe = isFromMe)
            else ->
                TextMessageBubble(message = message, isFromMe = isFromMe)
        }
    }
}

@Composable
private fun TextMessageBubble(message: ChatMessage, isFromMe: Boolean) {
    val c = ZappTheme.colors
    Box(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .background(if (isFromMe) c.accent else c.surfaceAlt, RectangleShape)
            .padding(12.dp),
    ) {
        Column {
            BasicText(
                text = message.content,
                style = ZappTheme.typography.body.copy(
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
                    style = ZappTheme.typography.caption.copy(
                        color = if (isFromMe) c.onAccent.copy(alpha = 0.7f) else c.textMuted,
                    ),
                )
                if (isFromMe) {
                    DeliveryIndicator(
                        status = message.status,
                        tintColor = c.onAccent.copy(alpha = 0.7f),
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
        MessageStatus.SENT -> Icon(
            Icons.Default.Done,
            contentDescription = "Sent",
            modifier = Modifier.size(13.dp),
            tint = tintColor,
        )
        MessageStatus.QUEUED -> Icon(
            Icons.Default.Schedule,
            contentDescription = "Queued",
            modifier = Modifier.size(13.dp),
            tint = tintColor,
        )
        MessageStatus.FAILED -> Icon(
            Icons.Default.ErrorOutline,
            contentDescription = "Failed",
            modifier = Modifier.size(13.dp),
            tint = c.danger,
        )
        MessageStatus.SENDING -> Icon(
            Icons.Default.MoreHoriz,
            contentDescription = "Sending",
            modifier = Modifier.size(13.dp),
            tint = tintColor,
        )
        null -> {}
    }
}

private fun formatMessageTime(epochMillis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))
