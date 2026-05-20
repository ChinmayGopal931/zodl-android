@file:Suppress("TooManyFunctions")

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.zapp.ZappBackButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappChipVariant
import co.electriccoin.zcash.ui.design.component.zapp.ZappFab
import co.electriccoin.zcash.ui.design.component.zapp.ZappRowDivider
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.component.zapp.ZappStatusChip
import co.electriccoin.zcash.ui.design.component.zapp.initialsOf
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZappNavBar
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.chat.list.ChatListChipVariant
import co.electriccoin.zcash.ui.screen.chat.list.ChatListItemState
import co.electriccoin.zcash.ui.screen.chat.list.ChatListLeaveDialogState
import co.electriccoin.zcash.ui.screen.chat.list.ChatListNetworkChipState
import co.electriccoin.zcash.ui.screen.chat.list.ChatListNetworkSheetState
import co.electriccoin.zcash.ui.screen.chat.list.ChatListState
import co.electriccoin.zcash.ui.screen.chat.list.ChatListTosDialogState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ChatListView(
    state: ChatListState,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val c = ZappTheme.colors

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ZappScreenHeader(
                title = state.title.getValue(),
                right = { NetworkChip(state = state.networkChip) },
            )

            if (state.items.isEmpty() && state.isLoading) {
                LoadingState()
            } else if (state.items.isEmpty()) {
                EmptyState(
                    title = state.emptyTitle.getValue(),
                    subtitle = state.emptySubtitle.getValue(),
                )
            } else {
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            top = 4.dp,
                            bottom = navBarBottom + ZappNavBar.CLEARANCE_DP.dp,
                        ),
                ) {
                    items(items = state.items, key = { it.id }) { item ->
                        SwipeToLeaveRow(
                            item = item,
                            onLeave = item.onLeaveSwipe,
                        ) {
                            ConversationItem(item = item)
                        }
                        ZappRowDivider(inset = true)
                    }
                }
            }
        }

        ZappFab(
            icon = Icons.Default.Add,
            contentDescription = state.newConversationContentDescription.getValue(),
            onClick = state.onNewConversationClick,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(
                        end = 20.dp,
                        bottom = ZappNavBar.FAB_BOTTOM_PADDING_DP.dp,
                    ),
        )

        if (showBackButton) {
            ZappBackButton(
                onClick = state.onBack,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(
                            start = 20.dp,
                            bottom = ZappNavBar.FAB_BOTTOM_PADDING_DP.dp,
                        ),
            )
        }

        state.leaveDialog?.let { LeaveConfirmationDialog(state = it) }
    }

    state.networkSheet?.let { sheetState ->
        NetworkDetailsSheet(
            connectionStatus = sheetState.connectionStatus,
            peerCount = sheetState.peerCount,
            dhtHealth = sheetState.dhtHealth,
            connectionDetails = sheetState.connectionDetails,
            onDismiss = sheetState.onDismiss,
        )
    }

    state.tosDialog?.let { ChatTermsDialog(onAccept = it.onAccept, onDecline = it.onDecline) }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ZappTheme.colors.accent)
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    val c = ZappTheme.colors
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
                text = title,
                style = ZappTheme.typography.sectionTitle.copy(color = c.text),
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = subtitle,
                style = ZappTheme.typography.body.copy(color = c.textMuted),
            )
        }
    }
}

/**
 * The pointerInput uses PointerEventPass.Initial so it sees MOVE events before
 * the inner clickable; consume() then cancels the clickable's tap tracking.
 * Delta is read from change.position - change.previousPosition, immune to
 * positionChangeConsumed, and rawOffset is a local var so there's no
 * coroutine race with offsetX.
 */
@Suppress("CyclomaticComplexMethod", "LoopWithTooManyJumpStatements", "MagicNumber")
@Composable
private fun SwipeToLeaveRow(
    item: ChatListItemState,
    onLeave: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = ZappTheme.colors
    val scope = rememberCoroutineScope()
    var offsetX by remember { mutableFloatStateOf(0f) }
    val revealThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .pointerInput(item.id) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)

                        var rawOffset = 0f
                        var hAccum = 0f
                        var vAccum = 0f
                        var isHorizontalDrag = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break

                            if (!change.pressed) {
                                if (isHorizontalDrag) {
                                    val capturedOffset = rawOffset
                                    scope.launch {
                                        if (-capturedOffset >= revealThresholdPx) onLeave()
                                        Animatable(capturedOffset).animateTo(0f, tween(200)) {
                                            offsetX = value
                                        }
                                    }
                                }
                                break
                            }

                            val dx = (change.position - change.previousPosition).x
                            val dy = (change.position - change.previousPosition).y

                            if (!isHorizontalDrag) {
                                hAccum += dx
                                vAccum += dy
                                val absH = kotlin.math.abs(hAccum)
                                val absV = kotlin.math.abs(vAccum)
                                val slop = viewConfiguration.touchSlop

                                when {
                                    absH > slop && absH >= absV && hAccum < 0f -> {
                                        isHorizontalDrag = true
                                        rawOffset = hAccum.coerceIn(-revealThresholdPx * 2f, 0f)
                                        offsetX = rawOffset
                                        change.consume()
                                    }

                                    absV > slop || (absH > slop && hAccum >= 0f) -> {
                                        break
                                    }
                                }
                            } else {
                                change.consume()
                                rawOffset = (rawOffset + dx).coerceIn(-revealThresholdPx * 2f, 0f)
                                offsetX = rawOffset
                            }
                        }
                    }
                },
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(c.danger, RectangleShape)
                    .padding(end = 20.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            BasicText(
                text = stringRes(R.string.chat_list_leave_action).getValue(),
                style =
                    ZappTheme.typography.button.copy(
                        color = c.bg,
                        fontWeight = FontWeight.Black,
                    ),
            )
        }

        Box(
            modifier =
                Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .background(c.bg)
                    .fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Composable
private fun LeaveConfirmationDialog(state: ChatListLeaveDialogState) {
    val c = ZappTheme.colors
    val message =
        stringRes(R.string.chat_list_leave_dialog_message, state.conversationName).getValue()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.overlay)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = state.onDismiss,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .background(c.surface, RectangleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    ).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText(
                text = stringRes(R.string.chat_list_leave_dialog_title).getValue(),
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
            )
            BasicText(
                text = message,
                style = ZappTheme.typography.body.copy(color = c.textMuted),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DialogButton(
                    text = stringRes(R.string.chat_list_leave_dialog_cancel).getValue(),
                    background = c.surfaceAlt,
                    textColor = c.text,
                    onClick = state.onDismiss,
                    modifier = Modifier.weight(1f),
                )
                DialogButton(
                    text = stringRes(R.string.chat_list_leave_dialog_confirm).getValue(),
                    background = c.danger,
                    textColor = c.bg,
                    onClick = state.onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    background: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(48.dp)
                .background(background, RectangleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style =
                ZappTheme.typography.button.copy(
                    color = textColor,
                    fontWeight = FontWeight.Black,
                ),
        )
    }
}

@Composable
private fun NetworkChip(state: ChatListNetworkChipState) {
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
private fun ConversationItem(item: ChatListItemState) {
    val c = ZappTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = item.onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConversationAvatar(name = item.displayName, group = item.isGroup)

        Spacer(Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = item.displayName,
                    style = ZappTheme.typography.rowTitle.copy(color = c.text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                item.timeLabel?.let { ts ->
                    BasicText(
                        text = ts.getValue(),
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
                    text = item.lastMessage.getValue(),
                    style = ZappTheme.typography.rowSubtitle.copy(color = c.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (item.unreadCount > 0) {
                    Box(
                        modifier =
                            Modifier
                                .padding(start = 8.dp)
                                .background(c.accent, RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        BasicText(
                            text = "${item.unreadCount}",
                            style = ZappTheme.typography.chip.copy(color = c.onAccent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationAvatar(name: String, group: Boolean) {
    val c = ZappTheme.colors
    val initials = remember(name) { initialsOf(name) }
    Box(
        modifier =
            Modifier
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

