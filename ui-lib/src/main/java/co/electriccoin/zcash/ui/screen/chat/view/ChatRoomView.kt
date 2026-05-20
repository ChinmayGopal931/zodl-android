package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.component.zapp.ZappBackButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.screen.chat.room.ChatRoomState
import kotlinx.coroutines.launch

@Composable
fun ChatRoomView(
    state: ChatRoomState,
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
                title = state.title.getValue(),
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
