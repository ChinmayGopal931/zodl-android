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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import co.electriccoin.zcash.ui.screen.chat.viewmodel.ChatViewModel

@Composable
fun ChatIdentitySetupView(
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel
) {
    val identity by viewModel.identity.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var displayNameInput by remember { mutableStateOf(TextFieldValue("")) }
    var seedPhraseInput by remember { mutableStateOf(TextFieldValue("")) }
    var restoreNameInput by remember { mutableStateOf(TextFieldValue("")) }
    var localError by remember { mutableStateOf<String?>(null) }
    var showSeedPhraseDialog by remember { mutableStateOf(false) }
    var exportedSeedPhrase by remember { mutableStateOf<String?>(null) }
    var identityJustCreated by remember { mutableStateOf(false) }

    LaunchedEffect(identity) {
        if (identity != null && identityJustCreated) {
            val phrase = viewModel.exportSeedPhraseSuspending()
            if (phrase != null) {
                exportedSeedPhrase = phrase
                showSeedPhraseDialog = true
            } else {
                onSetupComplete()
            }
        } else if (identity != null && !identityJustCreated) {
            onSetupComplete()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            if (selectedTab == 0) "Welcome to P2P Chat" else "Restore Your Identity",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            if (selectedTab == 0)
                "Create a display name to get started with peer-to-peer messaging."
            else
                "Restore your messaging identity using your wallet's 24-word seed phrase.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tab toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(0.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            listOf("Create" to 0, "Restore" to 1).forEach { (label, index) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(0.dp))
                        .background(if (selectedTab == index) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (selectedTab) {
            0 -> {
                TextField(
                    value = displayNameInput,
                    onValueChange = { displayNameInput = it },
                    placeholder = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val name = displayNameInput.text.trim()
                        if (name.isEmpty()) {
                            localError = "Display name is required"
                        } else {
                            localError = null
                            identityJustCreated = true
                            viewModel.createIdentity(name)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(0.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Create Identity", modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            1 -> {
                TextField(
                    value = restoreNameInput,
                    onValueChange = { restoreNameInput = it },
                    placeholder = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = seedPhraseInput,
                    onValueChange = { seedPhraseInput = it },
                    placeholder = { Text("Enter your wallet seed phrase (24 words)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val name = restoreNameInput.text.trim()
                        val words = seedPhraseInput.text.trim().split("\\s+".toRegex())
                        when {
                            name.isEmpty() -> localError = "Display name is required"
                            words.size != 24 -> localError = "Seed phrase must be exactly 24 words"
                            else -> {
                                localError = null
                                viewModel.restoreFromSeedPhrase(seedPhraseInput.text.trim(), name)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(0.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Restore Identity", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        val displayError = localError ?: errorMessage
        if (displayError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = displayError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showSeedPhraseDialog && exportedSeedPhrase != null) {
        SeedPhraseBackupDialog(
            seedPhrase = exportedSeedPhrase ?: return,
            onDismiss = {
                showSeedPhraseDialog = false
                onSetupComplete()
            }
        )
    }
}

@Composable
private fun SeedPhraseBackupDialog(
    seedPhrase: String,
    onDismiss: () -> Unit
) {
    val words = remember(seedPhrase) { seedPhrase.split(" ").filter { it.isNotBlank() } }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Back Up Your Seed Phrase",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Write these words down and store them safely. This is the only way to recover your messaging identity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                val leftColumn = words.take(12)
                val rightColumn = words.drop(12)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(leftColumn to 0, rightColumn to 12).forEach { (column, offset) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            column.forEachIndexed { i, word ->
                                Surface(
                                    shape = RoundedCornerShape(0.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${offset + i + 1}.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(28.dp)
                                        )
                                        Text(
                                            word,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("I've Saved My Seed Phrase", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
