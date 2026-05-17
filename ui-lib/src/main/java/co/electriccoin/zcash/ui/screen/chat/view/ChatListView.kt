package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.component.zapp.ZappBackButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappChipVariant
import co.electriccoin.zcash.ui.design.component.zapp.ZappFab
import co.electriccoin.zcash.ui.design.component.zapp.ZappRowDivider
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.component.zapp.ZappStatusChip
import co.electriccoin.zcash.ui.design.component.zapp.initialsOf
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZappNavBar
import co.electriccoin.zcash.ui.screen.chat.model.ChatConversation
import co.electriccoin.zcash.ui.screen.chat.model.ConversationType
import co.electriccoin.zcash.ui.screen.chat.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ChatListView(
    onConversationClick: (ChatConversation) -> Unit,
    onNewMessage: () -> Unit,
    onNavigateBack: () -> Unit = {},
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel,
) {
    val c = ZappTheme.colors
    val conversations by viewModel.conversations.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val peerCount by viewModel.peerCount.collectAsState()
    val dhtHealth by viewModel.dhtHealth.collectAsState()
    val connectionDetails by viewModel.connectionDetails.collectAsState()
    var showNetworkSheet by remember { mutableStateOf(false) }
    var leaveTargetConversation by remember { mutableStateOf<ChatConversation?>(null) }
    val showChatTosDialog by viewModel.showChatTosDialog.collectAsState()

    // Check ToS acceptance on first view
    LaunchedEffect(Unit) {
        viewModel.checkChatTosAccepted()
    }

    val blockedKeys by viewModel.blockedKeys.collectAsState()
    val sortedConversations =
        remember(conversations, blockedKeys) {
            conversations
                .filter { conv ->
                    // Hide direct conversations with blocked users
                    conv.type != co.electriccoin.zcash.ui.screen.chat.model.ConversationType.DIRECT ||
                        conv.participantIds.none { it in blockedKeys }
                }
                .sortedByDescending { it.lastMessageTimestamp ?: 0L }
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ZappScreenHeader(
                title = "Chats",
                right = {
                    NetworkChip(
                        connectionStatus = connectionStatus,
                        peerCount = peerCount,
                        onClick = {
                            viewModel.fetchConnectionDetails()
                            showNetworkSheet = true
                        },
                    )
                },
            )

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = c.textSubtle,
                        )
                        Spacer(Modifier.height(12.dp))
                        BasicText(
                            "No conversations yet",
                            style = ZappTheme.typography.sectionTitle.copy(color = c.text),
                        )
                        Spacer(Modifier.height(6.dp))
                        BasicText(
                            "Tap + to start a new P2P chat",
                            style = ZappTheme.typography.body.copy(color = c.textMuted),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = ZappNavBar.CLEARANCE_DP.dp,
                    ),
                ) {
                    items(
                        items = sortedConversations,
                        key = { it.id },
                    ) { conversation ->
                        // SwipeToLeaveRow wraps only the row content; the divider stays fixed.
                        SwipeToLeaveRow(
                            conversation = conversation,
                            onLeave = { leaveTargetConversation = conversation },
                        ) {
                            ConversationItem(
                                conversation = conversation,
                                onClick = { onConversationClick(conversation) },
                            )
                        }
                        ZappRowDivider(inset = true)
                    }
                }
            }
        }

        ZappFab(
            icon = Icons.Default.Add,
            contentDescription = "New conversation",
            onClick = onNewMessage,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 20.dp,
                    bottom = (ZappNavBar.CLEARANCE_DP + 12).dp,
                ),
        )

        if (showBackButton) {
            ZappBackButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = 20.dp,
                        bottom = (ZappNavBar.CLEARANCE_DP + 12).dp,
                    ),
            )
        }

        // Leave confirmation dialog — overlays everything within this Box
        leaveTargetConversation?.let { conv ->
            LeaveConfirmationDialog(
                conversationName = conv.displayName,
                onDismiss = { leaveTargetConversation = null },
                onConfirm = {
                    viewModel.leaveConversation(conv.id)
                    leaveTargetConversation = null
                },
            )
        }
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

    if (showChatTosDialog) {
        ChatTermsDialog(
            onAccept = { viewModel.acceptChatTos() },
            onDecline = {
                viewModel.declineChatTos()
                onNavigateBack()
            },
        )
    }
}

/**
 * Swipe-to-reveal container for conversation rows.
 *
 * The pointerInput sits on the outer Box (parent of ConversationItem in the layout tree).
 * Using PointerEventPass.Initial means we intercept MOVE events before the inner clickable
 * sees them. Once left-swipe is confirmed:
 *   - change.consume() marks the event consumed → clickable's waitForUpOrCancellation
 *     sees isConsumed = true and cancels tap tracking, preventing phantom taps.
 *   - Delta is always read as (change.position - change.previousPosition) rather than
 *     positionChange(), which returns Offset.Zero when positionChangeConsumed is true.
 *   - rawOffset is a local var updated synchronously — no coroutine race condition.
 */
@Composable
private fun SwipeToLeaveRow(
    conversation: ChatConversation,
    onLeave: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = ZappTheme.colors
    val scope = rememberCoroutineScope()
    // mutableFloatStateOf: plain state, no suspend needed — avoids restricted-scope errors.
    // Animatable is only created on release for the snap-back animation.
    var offsetX by remember { mutableFloatStateOf(0f) }
    val revealThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            // Gesture on the outer Box — parent of content's clickable in the layout tree.
            // Initial pass processes parent before child, giving us first access to events.
            .pointerInput(conversation.id) {
                awaitEachGesture {
                    // DOWN: nothing consumes this before us, so Main pass (default) is fine.
                    awaitFirstDown(requireUnconsumed = false)

                    var rawOffset = 0f
                    var hAccum = 0f
                    var vAccum = 0f
                    var isHorizontalDrag = false

                    while (true) {
                        // Initial pass: we see MOVE before the inner clickable does.
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break

                        if (!change.pressed) {
                            if (isHorizontalDrag) {
                                val capturedOffset = rawOffset
                                scope.launch {
                                    if (-capturedOffset >= revealThresholdPx) onLeave()
                                    // Fresh Animatable for release animation — unrestricted scope.
                                    Animatable(capturedOffset).animateTo(0f, tween(200)) {
                                        offsetX = value
                                    }
                                }
                            }
                            break
                        }

                        // Raw positional delta — immune to positionChangeConsumed flag.
                        val dx = (change.position - change.previousPosition).x
                        val dy = (change.position - change.previousPosition).y

                        if (!isHorizontalDrag) {
                            hAccum += dx
                            vAccum += dy
                            val absH = kotlin.math.abs(hAccum)
                            val absV = kotlin.math.abs(vAccum)
                            val slop = viewConfiguration.touchSlop

                            when {
                                // Left-swipe with at least as much horizontal as vertical
                                absH > slop && absH >= absV && hAccum < 0f -> {
                                    isHorizontalDrag = true
                                    rawOffset = hAccum.coerceIn(-revealThresholdPx * 2f, 0f)
                                    offsetX = rawOffset  // direct state write — no suspend
                                    change.consume()
                                }
                                // Vertical scroll or rightward — yield to LazyColumn
                                absV > slop || (absH > slop && hAccum >= 0f) -> break
                                // Still within slop — keep watching
                            }
                        } else {
                            // Active drag: consume so LazyColumn doesn't scroll vertically
                            change.consume()
                            rawOffset = (rawOffset + dx).coerceIn(-revealThresholdPx * 2f, 0f)
                            offsetX = rawOffset  // direct state write — no suspend
                        }
                    }
                }
            },
    ) {
        // Reveal layer — always behind the sliding row
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(c.danger, RectangleShape)
                .padding(end = 20.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            BasicText(
                text = "Leave",
                style = ZappTheme.typography.button.copy(
                    color = c.bg,
                    fontWeight = FontWeight.Black,
                ),
            )
        }

        // Foreground content — slides left on drag
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .background(c.bg)
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}

/**
 * Full-screen confirmation overlay — no MaterialTheme AlertDialog,
 * pure Box + BasicText + clickable per the Zapp design system.
 */
@Composable
private fun LeaveConfirmationDialog(
    conversationName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val c = ZappTheme.colors

    // Scrim — tapping it dismisses
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.overlay)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Dialog card — absorbs clicks so they don't fall through to the scrim
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .background(c.surface, RectangleShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText(
                text = "Leave conversation?",
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
            )
            BasicText(
                text = "You'll leave \"$conversationName\" and stop receiving its messages.",
                style = ZappTheme.typography.body.copy(color = c.textMuted),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Cancel
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
                // Leave — danger
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(c.danger, RectangleShape)
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Leave",
                        style = ZappTheme.typography.button.copy(
                            color = c.bg,
                            fontWeight = FontWeight.Black,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkChip(
    connectionStatus: ChatViewModel.ConnectionStatus,
    peerCount: Int,
    onClick: () -> Unit,
) {
    val c = ZappTheme.colors
    val (variant, dotColor, text) =
        when (connectionStatus) {
            ChatViewModel.ConnectionStatus.CONNECTED ->
                Triple(ZappChipVariant.Success, c.success, "$peerCount")
            ChatViewModel.ConnectionStatus.CONNECTING ->
                Triple(ZappChipVariant.Accent, c.accent, "...")
            ChatViewModel.ConnectionStatus.DISCONNECTED ->
                Triple(ZappChipVariant.Danger, c.danger, "off")
            ChatViewModel.ConnectionStatus.ERROR ->
                Triple(ZappChipVariant.Danger, c.danger, "err")
        }
    ZappStatusChip(
        text = text,
        variant = variant,
        dotColor = dotColor,
        onClick = onClick,
    )
}

@Composable
private fun ConversationItem(
    conversation: ChatConversation,
    onClick: () -> Unit,
) {
    val c = ZappTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConversationAvatar(
            name = conversation.displayName,
            group = conversation.type == ConversationType.GROUP,
        )

        Spacer(Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = conversation.displayName,
                    style = ZappTheme.typography.rowTitle.copy(color = c.text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                conversation.lastMessageTimestamp?.let { ts ->
                    BasicText(
                        formatRelativeTime(ts),
                        style = ZappTheme.typography.caption.copy(color = c.textSubtle),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = conversation.lastMessage ?: "No messages yet",
                    style = ZappTheme.typography.rowSubtitle.copy(color = c.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(c.accent, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        BasicText(
                            text = "${conversation.unreadCount}",
                            style = ZappTheme.typography.chip.copy(color = c.onAccent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationAvatar(
    name: String,
    group: Boolean,
) {
    val c = ZappTheme.colors
    val initials = remember(name) { initialsOf(name) }
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(c.accent, RectangleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (group || initials.isBlank()) {
            Icon(
                imageVector = if (group) Icons.Default.Group else Icons.Default.Person,
                contentDescription = null,
                tint = c.onAccent,
                modifier = Modifier.size(20.dp),
            )
        } else {
            BasicText(
                text = initials,
                style = ZappTheme.typography.rowTitle.copy(color = c.onAccent),
            )
        }
    }
}

private fun formatRelativeTime(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - epochMillis
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))
        diff < 604_800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(epochMillis))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMillis))
    }
}
