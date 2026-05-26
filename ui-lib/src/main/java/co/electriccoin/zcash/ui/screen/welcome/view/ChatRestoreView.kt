package co.electriccoin.zcash.ui.screen.welcome.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.design.theme.ProvideZappTheme
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import co.electriccoin.zcash.ui.screen.onboarding.view.OnbBottomDock
import co.electriccoin.zcash.ui.screen.onboarding.view.OnbHero
import co.electriccoin.zcash.ui.screen.onboarding.view.OnbSub
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val EXPECTED_WORDS = 24

// Mirror the rules in UsernameEntryScreen so restored identities can't carry display names that
// would have been rejected on the create path (length, charset). Restored seeds are real
// identities that round-trip across devices; relaxing the rules here would only make the two
// paths produce different on-disk shapes.
private const val MIN_NAME_LENGTH = 3
private const val MAX_NAME_LENGTH = 20
private val USERNAME_REGEX = Regex("[a-z0-9_]*")

/**
 * Swiss-styled chat-identity restore — entered when the user taps "I already
 * use Zapp" on the welcome gate. The 24-word phrase + display name pair maps
 * directly onto [ChatBootstrap.restoreFromSeedPhrase]; success is observed by
 * watching the SDK's identity flow (exposed via `bootstrap.identity`) go
 * non-null.
 *
 * This is the *messaging* restore flow, not the wallet restore. The two have
 * different seeds and live separately by design.
 */
@Composable
fun ChatRestoreView(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    bootstrap: ChatBootstrap = koinInject(),
) {
    ProvideZappTheme {
        ChatRestoreContent(onBack = onBack, onSuccess = onSuccess, bootstrap = bootstrap)
    }
}

@Composable
private fun ChatRestoreContent(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    bootstrap: ChatBootstrap,
) {
    val c = ZappTheme.colors
    val scope = rememberCoroutineScope()

    var displayName by rememberSaveable { mutableStateOf("") }
    var phrase by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val identity by bootstrap.identity.collectAsStateWithLifecycle()

    val wordCount = if (phrase.isBlank()) 0 else phrase.trim().split(Regex("\\s+")).size
    val trimmedName = displayName.trim()
    val nameValid =
        trimmedName.length in MIN_NAME_LENGTH..MAX_NAME_LENGTH && trimmedName.matches(USERNAME_REGEX)
    val phraseValid = wordCount == EXPECTED_WORDS
    val isValid = nameValid && phraseValid && !isLoading

    // identity going non-null is the SDK telling us restore succeeded.
    LaunchedEffect(identity) {
        if (identity != null) onSuccess()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 24.dp),
        ) {
            OnbHero(text = "Restore your\nchat identity")
            Spacer(Modifier.height(12.dp))
            OnbSub(
                text = "Enter the display name and 24-word recovery phrase from the device where you previously used Zapp.",
                modifier = Modifier.fillMaxWidth(0.94f),
            )
            Spacer(Modifier.height(28.dp))

            FieldLabel(text = "Display name")
            Spacer(Modifier.height(6.dp))
            DisplayNameField(
                value = displayName,
                onChange = { raw ->
                    displayName = raw.lowercase().filter { it.isLetterOrDigit() || it == '_' }
                },
            )

            Spacer(Modifier.height(20.dp))

            FieldLabel(text = "Recovery phrase")
            Spacer(Modifier.height(6.dp))
            PhraseField(value = phrase, onChange = { phrase = it }, wordCount = wordCount)

            val displayError = errorMessage
            if (displayError != null) {
                Spacer(Modifier.height(12.dp))
                BasicText(
                    text = displayError,
                    style =
                        ZappTheme.typography.body.copy(
                            color = c.danger,
                            fontSize = 12.sp,
                        ),
                )
            }
        }
        OnbBottomDock(
            cta = if (isLoading) "Restoring…" else "Restore",
            onCta = {
                if (isValid) {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        runCatching {
                            bootstrap.restoreFromSeedPhrase(
                                seedPhrase = phrase.trim(),
                                displayName = displayName.trim(),
                            )
                        }.onFailure { e ->
                            errorMessage = e.message ?: "Restore failed"
                        }
                        isLoading = false
                    }
                }
            },
            ctaEnabled = isValid,
            showBack = true,
            onBack = onBack,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    val c = ZappTheme.colors
    BasicText(
        text = text.uppercase(),
        style =
            ZappTheme.typography.eyebrow.copy(
                color = c.textSubtle,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Black,
            ),
    )
}

@Composable
private fun DisplayNameField(
    value: String,
    onChange: (String) -> Unit,
) {
    val c = ZappTheme.colors
    val borderColor = if (value.isNotEmpty()) c.text else c.border
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(width = 2.dp, color = borderColor, shape = RectangleShape)
                .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            cursorBrush = SolidColor(c.accent),
            textStyle =
                ZappTheme.typography.display.copy(
                    color = c.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp,
                ),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    BasicText(
                        text = "your_handle",
                        style =
                            ZappTheme.typography.display.copy(
                                color = c.textSubtle,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.4).sp,
                            ),
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun PhraseField(
    value: String,
    onChange: (String) -> Unit,
    wordCount: Int,
) {
    val c = ZappTheme.colors
    val borderColor = if (value.isNotEmpty()) c.text else c.border
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(width = 2.dp, color = borderColor, shape = RectangleShape)
                .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.lowercase()) },
            cursorBrush = SolidColor(c.accent),
            textStyle =
                ZappTheme.typography.body.copy(
                    color = c.text,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontFamily = FontFamily.Monospace,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(110.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    BasicText(
                        text = "Paste or type your $EXPECTED_WORDS words, separated by spaces.",
                        style =
                            ZappTheme.typography.body.copy(
                                color = c.textSubtle,
                                fontSize = 13.sp,
                                lineHeight = 22.sp,
                            ),
                    )
                }
                inner()
            },
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "$wordCount / $EXPECTED_WORDS words",
                style =
                    ZappTheme.typography.body.copy(
                        color = if (wordCount == EXPECTED_WORDS) c.success else c.textSubtle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.2.sp,
                    ),
            )
        }
    }
}
