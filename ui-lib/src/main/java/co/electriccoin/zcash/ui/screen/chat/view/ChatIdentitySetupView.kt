package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.screen.chat.identity.ChatIdentitySetupBackupDialogState
import co.electriccoin.zcash.ui.screen.chat.identity.ChatIdentitySetupFormState
import co.electriccoin.zcash.ui.screen.chat.identity.ChatIdentitySetupState
import co.electriccoin.zcash.ui.screen.chat.identity.ChatIdentitySetupTab
import co.electriccoin.zcash.ui.screen.chat.identity.ChatIdentitySetupTabsState

@Composable
fun ChatIdentitySetupView(
    state: ChatIdentitySetupState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = ZappTheme.colors.accent,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = state.title.getValue(),
            style = MaterialTheme.typography.headlineSmall,
            color = ZappTheme.colors.text,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = state.subtitle.getValue(),
            style = MaterialTheme.typography.bodyMedium,
            color = ZappTheme.colors.textMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        TabToggle(state = state.tabs)

        Spacer(modifier = Modifier.height(20.dp))

        IdentityForm(state = state.form)

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error.getValue(),
                style = MaterialTheme.typography.bodySmall,
                color = ZappTheme.colors.danger,
            )
        }
    }

    state.seedBackup?.let { SeedPhraseBackupDialog(state = it) }
}

@Composable
private fun TabToggle(state: ChatIdentitySetupTabsState) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RectangleShape)
                .background(ZappTheme.colors.surfaceAlt)
                .padding(4.dp),
    ) {
        listOf(
            state.createLabel.getValue() to ChatIdentitySetupTab.CREATE,
            state.restoreLabel.getValue() to ChatIdentitySetupTab.RESTORE,
        ).forEach { (label, tab) ->
            val selected = state.selected == tab
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RectangleShape)
                        .background(if (selected) ZappTheme.colors.surface else Color.Transparent)
                        .clickable { state.onSelect(tab) }
                        .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (selected) {
                            ZappTheme.colors.accent
                        } else {
                            ZappTheme.colors.textMuted
                        },
                )
            }
        }
    }
}

@Composable
private fun IdentityForm(state: ChatIdentitySetupFormState) {
    when (state) {
        is ChatIdentitySetupFormState.Create -> {
            PlainTextField(
                value = state.displayName,
                placeholder = state.displayNamePlaceholder.getValue(),
                onValueChange = state.onDisplayNameChange,
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(20.dp))
            PrimarySubmitButton(
                label = state.submitLabel.getValue(),
                isSubmitting = state.isSubmitting,
                onClick = state.onSubmit,
            )
        }

        is ChatIdentitySetupFormState.Restore -> {
            PlainTextField(
                value = state.displayName,
                placeholder = state.displayNamePlaceholder.getValue(),
                onValueChange = state.onDisplayNameChange,
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            PlainTextField(
                value = state.seedPhrase,
                placeholder = state.seedPhrasePlaceholder.getValue(),
                onValueChange = state.onSeedPhraseChange,
                singleLine = false,
                minLines = 3,
                maxLines = 5,
            )
            Spacer(modifier = Modifier.height(20.dp))
            PrimarySubmitButton(
                label = state.submitLabel.getValue(),
                isSubmitting = state.isSubmitting,
                onClick = state.onSubmit,
            )
        }
    }
}

@Composable
private fun PlainTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors =
            TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
    )
}

@Composable
private fun PrimarySubmitButton(
    label: String,
    isSubmitting: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isSubmitting,
        shape = RectangleShape,
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = ZappTheme.colors.onAccent,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(label, modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Composable
private fun SeedPhraseBackupDialog(state: ChatIdentitySetupBackupDialogState) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(DIALOG_WIDTH_FRACTION)
                    .wrapContentHeight(),
            shape = RectangleShape,
            color = ZappTheme.colors.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = ZappTheme.colors.danger,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = state.title.getValue(),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.subtitle.getValue(),
                    style = MaterialTheme.typography.bodySmall,
                    color = ZappTheme.colors.textMuted,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))

                SeedPhraseGrid(words = state.words)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = state.onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                ) {
                    Text(state.confirmLabel.getValue(), modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SeedPhraseGrid(words: List<String>) {
    val leftColumn = words.take(SEED_HALF)
    val rightColumn = words.drop(SEED_HALF)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(leftColumn to 0, rightColumn to SEED_HALF).forEach { (column, offset) ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                column.forEachIndexed { i, word ->
                    Surface(
                        shape = RectangleShape,
                        color = ZappTheme.colors.surfaceAlt,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${offset + i + 1}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ZappTheme.colors.textMuted,
                                modifier = Modifier.width(28.dp),
                            )
                            Text(
                                text = word,
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val SEED_HALF = 12
private const val DIALOG_WIDTH_FRACTION = 0.92f
