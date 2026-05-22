package co.electriccoin.zcash.ui.screen.chat.support

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.chat.SupportChatArgs
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun SupportChatScreen(args: SupportChatArgs) {
    val viewModel = koinViewModel<SupportChatVM> { parametersOf(args) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    SupportChatEffectsHandler(viewModel)
    BackHandler { state.onBack() }
    SupportChatView(state = state)
}

// ── View ──────────────────────────────────────────────────────────────────────

@Composable
private fun SupportChatView(state: SupportChatScreenState) {
    val c = ZappTheme.colors
    val uiState = state.uiState

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SupportTopBar(
                title = stringRes(R.string.support_chat_title).getValue(),
                onBack = state.onBack,
                onLeave = state.onLeave,
                showOverflow = uiState is SupportChatUiState.Chat,
            )

            when (uiState) {
                SupportChatUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = c.accent)
                    }
                }

                SupportChatUiState.SelectCategory -> {
                    CategoryPickerFullScreen(
                        onSelected = state.onCategorySelected,
                        modifier = Modifier.weight(1f),
                    )
                }

                is SupportChatUiState.Chat -> {
                    MessageList(
                        messages = uiState.messages,
                        modifier = Modifier.weight(1f),
                    )
                    SupportInputBar(
                        input = uiState.input,
                        onInputChange = state.onInputChange,
                        onSend = state.onSend,
                        onAttach = state.onAttach,
                    )
                }
            }
        }

        state.leaveDialog?.let { dialog ->
            SupportLeaveDialog(state = dialog)
        }
    }

    state.mediaSheet?.let { sheet ->
        SupportMediaSheet(
            onChooseMedia = sheet.onChooseMedia,
            onAttachFile = sheet.onAttachFile,
            onTakePhoto = sheet.onTakePhoto,
            onDismiss = sheet.onDismiss,
        )
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun SupportTopBar(
    title: String,
    onBack: () -> Unit,
    onLeave: () -> Unit,
    showOverflow: Boolean,
) {
    var showMenu by remember { mutableStateOf(false) }

    ZappScreenHeader(
        title = title,
        left = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = ZappTheme.colors.text,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        right = {
            if (showOverflow) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false),
                                onClick = { showMenu = true },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = ZappTheme.colors.text,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                BasicText(
                                    text = stringRes(R.string.support_chat_overflow_close).getValue(),
                                    style = ZappTheme.typography.body.copy(
                                        color = ZappTheme.colors.danger,
                                    ),
                                )
                            },
                            onClick = {
                                showMenu = false
                                onLeave()
                            },
                        )
                    }
                }
            }
        },
    )
}

// ── Category picker (bottom-anchored, thumb-reachable) ──────────────────────

@Composable
private fun CategoryPickerFullScreen(
    onSelected: (SupportCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ZappTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 48.dp),
        ) {
            BasicText(
                text = stringRes(R.string.support_chat_pick_topic).getValue(),
                style = ZappTheme.typography.sectionTitle.copy(color = c.text),
            )
            Spacer(Modifier.height(16.dp))
            SupportCategory.entries.forEach { category ->
                val label = when (category) {
                    SupportCategory.PROBLEM ->
                        stringRes(R.string.support_chat_category_problem).getValue()
                    SupportCategory.FEEDBACK ->
                        stringRes(R.string.support_chat_category_feedback).getValue()
                    SupportCategory.OTHER ->
                        stringRes(R.string.support_chat_category_other).getValue()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(c.surfaceAlt, RectangleShape)
                        .border(BorderStroke(1.dp, c.border), RectangleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = c.accent),
                            onClick = { onSelected(category) },
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = label,
                        style = ZappTheme.typography.button.copy(color = c.text),
                    )
                }
            }
        }
    }
}

// ── Message list ──────────────────────────────────────────────────────────────

@Composable
private fun MessageList(
    messages: List<SupportUiMessage>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size.coerceAtLeast(0))
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(top = 8.dp, bottom = navBarBottom + 8.dp),
    ) {
        items(items = messages, key = { it.id }) { msg ->
            SupportMessageBubble(message = msg)
        }
    }
}

// ── Message bubble ────────────────────────────────────────────────────────────

@Composable
private fun SupportMessageBubble(message: SupportUiMessage) {
    // Hide category marker messages from the visible list.
    if (message.content.startsWith(SupportChatConstants.CATEGORY_MARKER)) return

    val c = ZappTheme.colors
    val isFromMe = message.isFromMe

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isFromMe) c.accent else c.surface,
                    RectangleShape,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            BasicText(
                text = message.content,
                style = ZappTheme.typography.body.copy(
                    color = if (isFromMe) c.onAccent else c.text,
                ),
            )
        }
    }
}

// ── Input bar ─────────────────────────────────────────────────────────────────

@Composable
private fun SupportInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
) {
    val c = ZappTheme.colors
    val canSend = input.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(c.surfaceAlt, RectangleShape)
                .border(BorderStroke(1.dp, c.border), RectangleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = c.accent),
                    onClick = onAttach,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringRes(R.string.chat_room_attach_content_description).getValue(),
                tint = c.accent,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        TextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 36.dp),
            placeholder = {
                BasicText(
                    text = stringRes(R.string.support_chat_input_placeholder).getValue(),
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

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(if (canSend) c.accent else c.surfaceAlt, RectangleShape)
                .border(BorderStroke(1.dp, c.border), RectangleShape)
                .clickable(
                    enabled = canSend,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = c.onAccent),
                    onClick = onSend,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = if (canSend) c.onAccent else c.textSubtle,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Media sheet ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupportMediaSheet(
    onChooseMedia: () -> Unit,
    onAttachFile: () -> Unit,
    onTakePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = ZappTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        scrimColor = c.overlay,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            BasicText(
                text = stringRes(R.string.support_chat_media_sheet_title).getValue(),
                style = ZappTheme.typography.sectionTitle.copy(color = c.text),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MediaOption(
                    icon = Icons.Default.Image,
                    label = stringRes(R.string.support_chat_media_option_media).getValue(),
                    onClick = onChooseMedia,
                    modifier = Modifier.weight(1f),
                )
                MediaOption(
                    icon = Icons.Default.AttachFile,
                    label = stringRes(R.string.support_chat_media_option_file).getValue(),
                    onClick = onAttachFile,
                    modifier = Modifier.weight(1f),
                )
                MediaOption(
                    icon = Icons.Default.CameraAlt,
                    label = stringRes(R.string.support_chat_media_option_camera).getValue(),
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MediaOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ZappTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RectangleShape,
        color = c.surfaceAlt,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = c.accent,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(4.dp))
            BasicText(
                text = label,
                style = ZappTheme.typography.chip.copy(color = c.text),
            )
        }
    }
}

// ── Leave dialog ──────────────────────────────────────────────────────────────

@Composable
private fun SupportLeaveDialog(state: SupportLeaveDialogState) {
    val c = ZappTheme.colors

    Box(
        modifier = Modifier
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
                text = stringRes(R.string.support_chat_leave_dialog_title).getValue(),
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
            )
            BasicText(
                text = stringRes(R.string.support_chat_leave_dialog_message).getValue(),
                style = ZappTheme.typography.body.copy(color = c.textMuted),
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
                        .clickable(onClick = state.onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = stringRes(R.string.support_chat_leave_dialog_cancel).getValue(),
                        style = ZappTheme.typography.button.copy(
                            color = c.text,
                            fontWeight = FontWeight.Black,
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(c.danger, RectangleShape)
                        .clickable(onClick = state.onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = stringRes(R.string.support_chat_leave_dialog_confirm).getValue(),
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
