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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.QrState
import co.electriccoin.zcash.ui.design.component.ZashiQr
import co.electriccoin.zcash.ui.design.component.zapp.ZappRow
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.component.zapp.initialsOf
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.screen.chat.profile.ChatProfileDeleteDialogState
import co.electriccoin.zcash.ui.screen.chat.profile.ChatProfileEditNameDialogState
import co.electriccoin.zcash.ui.screen.chat.profile.ChatProfilePinVerifyState
import co.electriccoin.zcash.ui.screen.chat.profile.ChatProfileSeedPhraseDialogState
import co.electriccoin.zcash.ui.screen.chat.profile.ChatProfileState
import co.electriccoin.zcash.ui.screen.chat.profile.ChatProfileTab
import co.electriccoin.zcash.ui.screen.chat.profile.ChatProfileWalletSubTab
import co.electriccoin.zcash.ui.screen.onboarding.view.PinVerifyScreen

@Composable
fun ChatProfileView(state: ChatProfileState, modifier: Modifier = Modifier) {
    val c = ZappTheme.colors

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        ZappScreenHeader(title = state.title.getValue())

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state.activeTab) {
                ChatProfileTab.MESSAGING_ID -> MessagingIdTabContent(state = state)
                ChatProfileTab.WALLET_ADDRESS -> WalletAddressTabContent(state = state)
            }
        }

        SeedPhraseRow(onClick = state.onSeedPhraseClick)

        Spacer(Modifier.height(4.dp))

        if (state.activeTab == ChatProfileTab.WALLET_ADDRESS && state.shieldedAddress != null) {
            ProfileSegmentedRow(
                items =
                    listOf(
                        SegmentItem(
                            label = stringResource(R.string.chat_profile_subtab_shielded),
                            icon = Icons.Default.Security,
                            isSelected = state.walletSubTab == ChatProfileWalletSubTab.SHIELDED,
                        ),
                        SegmentItem(
                            label = stringResource(R.string.chat_profile_subtab_transparent),
                            icon = Icons.Default.CreditCard,
                            isSelected = state.walletSubTab == ChatProfileWalletSubTab.TRANSPARENT,
                        ),
                    ),
                onSelect = { idx ->
                    state.onWalletSubTabSelected(
                        if (idx == 0) ChatProfileWalletSubTab.SHIELDED else ChatProfileWalletSubTab.TRANSPARENT,
                    )
                },
            )
            Spacer(Modifier.height(4.dp))
        }

        ProfileSegmentedRow(
            items =
                listOf(
                    SegmentItem(
                        label = stringResource(R.string.chat_profile_tab_messaging_id),
                        icon = null,
                        isSelected = state.activeTab == ChatProfileTab.MESSAGING_ID,
                    ),
                    SegmentItem(
                        label = stringResource(R.string.chat_profile_tab_wallet_address),
                        icon = null,
                        isSelected = state.activeTab == ChatProfileTab.WALLET_ADDRESS,
                    ),
                ),
            onSelect = { idx ->
                state.onMainTabSelected(
                    if (idx == 0) ChatProfileTab.MESSAGING_ID else ChatProfileTab.WALLET_ADDRESS,
                )
            },
        )

        Spacer(Modifier.height(12.dp))

        BottomDock(onBack = state.onBack, onDelete = state.onDeleteClick)
    }

    state.editNameDialog?.let { EditDisplayNameDialog(state = it) }
    state.deleteDialog?.let { DeleteIdentityDialog(state = it) }
    state.pinVerify?.let { PinVerifyOverlay(state = it) }
    state.seedPhraseDialog?.let { SeedPhraseDialog(state = it) }
}

@Composable
private fun MessagingIdTabContent(state: ChatProfileState) {
    val c = ZappTheme.colors
    val initials = remember(state.displayName) { state.displayName?.let { initialsOf(it) } ?: "?" }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .background(c.accent, RectangleShape),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = initials,
                style = ZappTheme.typography.sectionTitle.copy(color = c.onAccent),
            )
        }
        val editLabel = stringResource(R.string.chat_profile_edit_display_name_content_description)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicText(
                text = "@${state.displayName.orEmpty()}",
                style = ZappTheme.typography.sectionTitle.copy(color = c.text),
            )
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false),
                            onClick = state.onEditDisplayNameClick,
                        ).semantics {
                            contentDescription = editLabel
                            role = Role.Button
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = c.textMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }

    state.publicKey?.let { pk ->
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(c.surface, RectangleShape)
                    .border(BorderStroke(1.dp, c.border), RectangleShape)
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ZashiQr(state = QrState(qrData = pk), qrSize = 160.dp)
                BasicText(
                    text = stringResource(R.string.chat_profile_qr_caption),
                    style = ZappTheme.typography.caption.copy(color = c.textSubtle),
                )
            }
        }

        PublicKeyCard(
            publicKey = pk,
            showCopiedFeedback = state.isKeyCopied,
            onCopy = state.onCopyPublicKeyClick,
        )
    }
}

@Composable
private fun WalletAddressTabContent(state: ChatProfileState) {
    val c = ZappTheme.colors
    val address =
        when (state.walletSubTab) {
            ChatProfileWalletSubTab.SHIELDED -> state.shieldedAddress.orEmpty()
            ChatProfileWalletSubTab.TRANSPARENT -> state.transparentAddress.orEmpty()
        }
    val addressLabel =
        when (state.walletSubTab) {
            ChatProfileWalletSubTab.SHIELDED ->
                stringResource(R.string.chat_profile_address_shielded_label)
            ChatProfileWalletSubTab.TRANSPARENT ->
                stringResource(R.string.chat_profile_address_transparent_label)
        }
    val caption =
        when (state.walletSubTab) {
            ChatProfileWalletSubTab.SHIELDED ->
                stringResource(R.string.chat_profile_address_shielded_caption)
            ChatProfileWalletSubTab.TRANSPARENT ->
                stringResource(R.string.chat_profile_address_transparent_caption)
        }

    if (address.isNotEmpty()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(c.surface, RectangleShape)
                    .border(BorderStroke(1.dp, c.border), RectangleShape)
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ZashiQr(state = QrState(qrData = address), qrSize = 200.dp)
                BasicText(
                    text = caption,
                    style = ZappTheme.typography.caption.copy(color = c.textSubtle),
                )
            }
        }

        AddressCard(
            label = addressLabel,
            address = address,
            showCopiedFeedback = state.isAddressCopied,
            onCopy = state.onCopyAddressClick,
        )
    }
}

@Composable
private fun SeedPhraseRow(onClick: () -> Unit) {
    val c = ZappTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .background(c.surface, RectangleShape)
                .border(BorderStroke(1.dp, c.border), RectangleShape),
    ) {
        ZappRow(
            title = stringResource(R.string.chat_profile_seed_phrase_title),
            subtitle = stringResource(R.string.chat_profile_seed_phrase_subtitle),
            icon = Icons.Default.Key,
            iconBackground = c.accentSoft,
            iconTint = c.accentText,
            onClick = onClick,
        )
    }
}

@Composable
private fun BottomDock(onBack: () -> Unit, onDelete: () -> Unit) {
    val c = ZappTheme.colors
    val backLabel = stringResource(R.string.chat_profile_back_content_description)
    val deleteLabel = stringResource(R.string.chat_profile_delete_identity_content_description)
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
                    .clickable(onClick = onBack)
                    .semantics {
                        contentDescription = backLabel
                        role = Role.Button
                    },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "←",
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
                    .background(c.danger, RectangleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = c.onAccent),
                        onClick = onDelete,
                    ).semantics {
                        contentDescription = deleteLabel
                        role = Role.Button
                    },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = stringResource(R.string.chat_profile_delete_button),
                style =
                    ZappTheme.typography.button.copy(
                        color = c.onAccent,
                        fontWeight = FontWeight.Black,
                        letterSpacing = LETTER_SPACING_DELETE,
                    ),
            )
        }
    }
}

@Composable
private fun PublicKeyCard(publicKey: String, showCopiedFeedback: Boolean, onCopy: () -> Unit) {
    val c = ZappTheme.colors
    val copyLabel =
        if (showCopiedFeedback) {
            stringResource(R.string.chat_profile_copied_content_description)
        } else {
            stringResource(R.string.chat_profile_copy_public_key_content_description)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(c.surfaceAlt, RectangleShape)
                .border(BorderStroke(1.dp, c.border), RectangleShape)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = stringResource(R.string.chat_profile_public_key_label),
                style = ZappTheme.typography.caption.copy(color = c.textMuted),
            )
            Spacer(Modifier.height(4.dp))
            BasicText(
                text = publicKey,
                style = ZappTheme.typography.mono.copy(color = c.text),
                maxLines = 3,
            )
        }
        Spacer(Modifier.width(8.dp))
        CopyIconButton(showCopiedFeedback = showCopiedFeedback, contentLabel = copyLabel, onClick = onCopy)
    }
}

@Composable
private fun AddressCard(
    label: String,
    address: String,
    showCopiedFeedback: Boolean,
    onCopy: () -> Unit,
) {
    val c = ZappTheme.colors
    val copyLabel =
        if (showCopiedFeedback) {
            stringResource(R.string.chat_profile_copied_content_description)
        } else {
            stringResource(R.string.chat_profile_copy_address_content_description)
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(c.surfaceAlt, RectangleShape)
                .border(BorderStroke(1.dp, c.border), RectangleShape)
                .padding(16.dp),
    ) {
        BasicText(
            text = label,
            style = ZappTheme.typography.caption.copy(color = c.textMuted),
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = address,
                style = ZappTheme.typography.mono.copy(color = c.text),
                modifier = Modifier.weight(1f),
                maxLines = 3,
            )
            Spacer(Modifier.width(8.dp))
            CopyIconButton(showCopiedFeedback = showCopiedFeedback, contentLabel = copyLabel, onClick = onCopy)
        }
    }
}

@Composable
private fun CopyIconButton(
    showCopiedFeedback: Boolean,
    contentLabel: String,
    onClick: () -> Unit,
) {
    val c = ZappTheme.colors
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = contentLabel
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (showCopiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = null,
            tint = if (showCopiedFeedback) c.success else c.textMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

private data class SegmentItem(val label: String, val icon: ImageVector?, val isSelected: Boolean)

@Composable
private fun ProfileSegmentedRow(items: List<SegmentItem>, onSelect: (Int) -> Unit) {
    val c = ZappTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .background(c.surfaceAlt, RectangleShape)
                .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEachIndexed { index, item ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .background(
                            if (item.isSelected) c.surface else Color.Transparent,
                            RectangleShape,
                        ).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = c.accent),
                            onClick = { onSelect(index) },
                        ).semantics {
                            contentDescription = item.label
                            role = Role.Button
                        },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (item.isSelected) c.accentText else c.textMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    BasicText(
                        text = item.label,
                        style =
                            ZappTheme.typography.rowSubtitle.copy(
                                color = if (item.isSelected) c.text else c.textMuted,
                                fontWeight = if (item.isSelected) FontWeight.Black else FontWeight.Normal,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditDisplayNameDialog(state: ChatProfileEditNameDialogState) {
    val c = ZappTheme.colors
    AlertDialog(
        onDismissRequest = state.onDismiss,
        containerColor = c.surface,
        titleContentColor = c.text,
        textContentColor = c.textMuted,
        shape = RectangleShape,
        title = {
            BasicText(
                text = stringResource(R.string.chat_profile_edit_name_title),
                style = ZappTheme.typography.sectionTitle.copy(color = c.text),
            )
        },
        text = {
            OutlinedTextField(
                value = state.value,
                onValueChange = state.onValueChange,
                singleLine = true,
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            DialogTextButton(
                label = stringResource(R.string.chat_profile_edit_name_save),
                color = if (state.canSave) c.accent else c.textSubtle,
                enabled = state.canSave,
                onClick = state.onSave,
            )
        },
        dismissButton = {
            DialogTextButton(
                label = stringResource(R.string.chat_profile_edit_name_cancel),
                color = c.textMuted,
                onClick = state.onDismiss,
            )
        },
    )
}

@Composable
private fun DeleteIdentityDialog(state: ChatProfileDeleteDialogState) {
    val c = ZappTheme.colors
    AlertDialog(
        onDismissRequest = state.onDismiss,
        containerColor = c.surface,
        titleContentColor = c.text,
        textContentColor = c.textMuted,
        shape = RectangleShape,
        title = {
            BasicText(
                text = stringResource(R.string.chat_profile_delete_dialog_title),
                style = ZappTheme.typography.sectionTitle.copy(color = c.text),
            )
        },
        text = {
            BasicText(
                text = stringResource(R.string.chat_profile_delete_dialog_message),
                style = ZappTheme.typography.body.copy(color = c.textMuted),
            )
        },
        confirmButton = {
            DialogTextButton(
                label = stringResource(R.string.chat_profile_delete_dialog_confirm),
                color = c.danger,
                onClick = state.onConfirm,
            )
        },
        dismissButton = {
            DialogTextButton(
                label = stringResource(R.string.chat_profile_delete_dialog_cancel),
                color = c.textMuted,
                onClick = state.onDismiss,
            )
        },
    )
}

@Composable
private fun PinVerifyOverlay(state: ChatProfilePinVerifyState) {
    PinVerifyScreen(
        hasError = state.hasError,
        lockoutSecondsRemaining = state.lockoutSecondsRemaining,
        onPinSubmit = state.onPinSubmit,
        onCancel = state.onCancel,
    )
}

@Composable
private fun SeedPhraseDialog(state: ChatProfileSeedPhraseDialogState) {
    val c = ZappTheme.colors
    AlertDialog(
        onDismissRequest = state.onDismiss,
        containerColor = c.surface,
        titleContentColor = c.text,
        textContentColor = c.textMuted,
        shape = RectangleShape,
        title = {
            BasicText(
                text = stringResource(R.string.chat_profile_seed_phrase_dialog_title),
                style = ZappTheme.typography.sectionTitle.copy(color = c.text),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicText(
                    text = stringResource(R.string.chat_profile_seed_phrase_dialog_message),
                    style = ZappTheme.typography.body.copy(color = c.textMuted),
                )
                SeedWordGrid(words = state.words)
            }
        },
        confirmButton = {
            DialogTextButton(
                label = stringResource(R.string.chat_profile_seed_phrase_dialog_done),
                color = c.accent,
                onClick = state.onDismiss,
            )
        },
    )
}

@Composable
private fun SeedWordGrid(words: List<String>) {
    val c = ZappTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(words.take(SEED_HALF) to 0, words.drop(SEED_HALF) to SEED_HALF).forEach { (col, offset) ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                col.forEachIndexed { i, word ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        BasicText(
                            text = "${offset + i + 1}".padStart(2, '0'),
                            style = ZappTheme.typography.mono.copy(color = c.textSubtle),
                        )
                        BasicText(
                            text = word,
                            style = ZappTheme.typography.rowTitle.copy(color = c.text),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogTextButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            Modifier
                .clickable(enabled = enabled, onClick = onClick)
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = ZappTheme.typography.button.copy(color = color),
        )
    }
}

private const val SEED_HALF = 12
private val LETTER_SPACING_DELETE = 0.6.sp
