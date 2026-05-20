@file:Suppress("TooManyFunctions")

package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.component.zapp.initialsOf
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.screen.chat.model.ChatContact
import co.electriccoin.zcash.ui.screen.chat.newconv.NewConversationContactItem
import co.electriccoin.zcash.ui.screen.chat.newconv.NewConversationParticipantChip
import co.electriccoin.zcash.ui.screen.chat.newconv.NewConversationPrimaryAction
import co.electriccoin.zcash.ui.screen.chat.newconv.NewConversationState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewConversationView(state: NewConversationState, modifier: Modifier = Modifier) {
    val c = ZappTheme.colors

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding(),
    ) {
        ZappScreenHeader(title = state.title.getValue())

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            if (state.showEmptyState) {
                EmptyState(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 28.dp),
                )
            } else {
                ConversationBody(state = state)
            }
        }

        SearchField(
            value = state.searchInput,
            onChange = state.onSearchInputChange,
            onClear = state.onClearSearch,
        )

        BottomDock(state = state)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConversationBody(state: NewConversationState) {
    val c = ZappTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
    ) {
        if (state.selectedParticipants.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.selectedParticipants.forEach { chip -> ParticipantChip(chip = chip) }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (state.isPublicKeyDetected) {
            Spacer(Modifier.height(12.dp))
            PublicKeyDetectedBanner(
                detectedKey = state.detectedPublicKey,
                onAdd = state.onAddDetectedKey,
            )
            Spacer(Modifier.height(12.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items = state.contacts, key = { it.contact.publicKey }) { item ->
                ContactSelectRow(item = item)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp)
                            .height(1.dp)
                            .background(c.border, RectangleShape),
                )
            }
        }
    }
}

@Composable
private fun ParticipantChip(chip: NewConversationParticipantChip) {
    val c = ZappTheme.colors
    val description =
        stringResource(R.string.chat_new_conversation_remove_participant_content_description_fmt, chip.displayName)
    Row(
        modifier =
            Modifier
                .background(c.accentSoft, RectangleShape)
                .border(BorderStroke(1.dp, c.border), RectangleShape)
                .clickable(onClick = chip.onRemove)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                }.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(
            text = chip.displayName,
            style = ZappTheme.typography.chip.copy(color = c.accentText),
        )
        Icon(
            Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = c.accentText,
        )
    }
}

@Composable
private fun PublicKeyDetectedBanner(detectedKey: String, onAdd: () -> Unit) {
    val c = ZappTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(c.accentSoft, RectangleShape)
                .border(BorderStroke(1.dp, c.border), RectangleShape)
                .clickable(onClick = onAdd)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = c.accentText,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = stringResource(R.string.chat_new_conversation_public_key_detected),
                style = ZappTheme.typography.caption.copy(color = c.accentText),
            )
            BasicText(
                text = "${detectedKey.take(KEY_PREVIEW_HEAD)}...${detectedKey.takeLast(KEY_PREVIEW_TAIL)}",
                style = ZappTheme.typography.mono.copy(color = c.accentText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicText(
            text = stringResource(R.string.chat_new_conversation_add_action),
            style = ZappTheme.typography.button.copy(color = c.accentText),
        )
    }
}

@Composable
private fun ContactSelectRow(item: NewConversationContactItem) {
    val c = ZappTheme.colors
    val contact: ChatContact = item.contact
    val initials = remember(contact.name) { initialsOf(contact.name) }
    val selectedLabel = stringResource(R.string.chat_new_conversation_selected_content_description)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = item.onToggle)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(c.accent, RectangleShape),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(initials, style = ZappTheme.typography.rowTitle.copy(color = c.onAccent))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                contact.name,
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
            )
            BasicText(
                "${contact.publicKey.take(CONTACT_KEY_HEAD)}...${contact.publicKey.takeLast(CONTACT_KEY_TAIL)}",
                style = ZappTheme.typography.mono.copy(color = c.textMuted),
            )
        }

        if (item.isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = selectedLabel,
                tint = c.accent,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, onClear: () -> Unit) {
    val c = ZappTheme.colors
    val clearLabel = stringResource(R.string.chat_new_conversation_clear_content_description)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .background(c.surfaceInput, RectangleShape)
                .border(
                    BorderStroke(
                        width = if (value.isNotEmpty()) 2.dp else 1.dp,
                        color = if (value.isNotEmpty()) c.borderStrong else c.border,
                    ),
                    RectangleShape,
                ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 0.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = c.textSubtle,
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = ZappTheme.typography.body.copy(color = c.text),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (value.isEmpty()) {
                    BasicText(
                        text = stringResource(R.string.chat_new_conversation_search_placeholder),
                        style = ZappTheme.typography.body.copy(color = c.textSubtle),
                    )
                }
            }
            if (value.isNotEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clickable(onClick = onClear)
                            .semantics {
                                contentDescription = clearLabel
                                role = Role.Button
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = c.textSubtle,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomDock(state: NewConversationState) {
    val c = ZappTheme.colors
    val backLabel = stringResource(R.string.chat_new_conversation_back_content_description)
    val (label, description) =
        when (state.primaryAction) {
            is NewConversationPrimaryAction.StartChat ->
                stringResource(R.string.chat_new_conversation_start_chat_label) to
                    stringResource(R.string.chat_new_conversation_start_chat_content_description)
            is NewConversationPrimaryAction.ScanQr ->
                stringResource(R.string.chat_new_conversation_scan_qr_label) to
                    stringResource(R.string.chat_new_conversation_scan_qr_content_description)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(c.surface)
                .border(BorderStroke(1.dp, c.border), RectangleShape)
                .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 72.dp, height = 52.dp)
                    .border(BorderStroke(1.dp, c.border), RectangleShape)
                    .clickable(onClick = state.onBack)
                    .semantics {
                        contentDescription = backLabel
                        role = Role.Button
                    },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                "←",
                style =
                    ZappTheme.typography.button.copy(
                        color = c.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    ),
            )
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .background(c.accent, RectangleShape)
                    .clickable(onClick = state.primaryAction.onClick)
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                    },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.primaryAction is NewConversationPrimaryAction.ScanQr) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = c.onAccent,
                    )
                }
                BasicText(
                    text = label,
                    style =
                        ZappTheme.typography.button.copy(
                            color = c.onAccent,
                            fontWeight = FontWeight.Black,
                            letterSpacing = BUTTON_LETTER_SPACING,
                        ),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val c = ZappTheme.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(100.dp)
                    .background(c.surfaceAlt, RectangleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = c.textSubtle,
            )
        }

        Spacer(Modifier.height(24.dp))

        BasicText(
            text = stringResource(R.string.chat_new_conversation_empty_title),
            style =
                ZappTheme.typography.sectionTitle.copy(
                    color = c.text,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                ),
        )

        Spacer(Modifier.height(8.dp))

        BasicText(
            text = stringResource(R.string.chat_new_conversation_empty_body),
            style =
                ZappTheme.typography.body.copy(
                    color = c.textMuted,
                    textAlign = TextAlign.Center,
                ),
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(c.accentSoft, RectangleShape)
                    .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(c.accent, RectangleShape),
            )
            Spacer(Modifier.width(12.dp))
            BasicText(
                text = stringResource(R.string.chat_new_conversation_privacy_callout),
                style = ZappTheme.typography.body.copy(color = c.accentText),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val KEY_PREVIEW_HEAD = 12
private const val KEY_PREVIEW_TAIL = 6
private const val CONTACT_KEY_HEAD = 8
private const val CONTACT_KEY_TAIL = 4
private val BUTTON_LETTER_SPACING = 0.6.sp
