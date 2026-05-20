package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.component.zapp.ZappBackButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappFab
import co.electriccoin.zcash.ui.design.component.zapp.ZappRowDivider
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZappNavBar
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.screen.chat.list.ChatListState

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
