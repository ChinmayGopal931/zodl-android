package co.electriccoin.zcash.ui.screen.onboarding.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.common.UsernameRules

// ───────────────────────────────────────────────────────────────
// 02 · Phase 1 intro — Messaging account
// ───────────────────────────────────────────────────────────────

@Composable
fun MessagingPhaseIntro(
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    OnbScreen(
        step = 1,
        ghostNum = 1,
        badge = "Part 1 of 3 · Messaging account",
        cta = "Continue",
        onCta = onContinue,
        showBack = true,
        onBack = onBack,
    ) {
        OnbHero(text = "Create your\nmessaging\nidentity")
        Spacer(Modifier.height(16.dp))
        OnbSub(
            text =
                "Your identity is a username backed by your wallet's recovery phrase — " +
                    "one phrase restores your wallet and messaging identity on any device. " +
                    "Message history is stored locally and is not recoverable.",
            modifier = Modifier.fillMaxWidth(0.92f),
        )
        Spacer(Modifier.height(28.dp))
        OnbBulletRow(
            label = "Pick a username",
            sub = "How friends find and message you",
            isFirst = true,
        )
        OnbBulletRow(
            label = "One recovery phrase",
            sub = "Your wallet's 24-word phrase also restores your messaging identity.",
        )
    }
}

// ───────────────────────────────────────────────────────────────
// 03 · Username
// ───────────────────────────────────────────────────────────────

@Composable
fun UsernameEntryScreen(
    onBack: () -> Unit,
    onContinue: (username: String) -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    val isLong = username.length >= UsernameRules.MIN_LENGTH
    val isShort = username.length <= UsernameRules.MAX_LENGTH
    val isValid = UsernameRules.isValid(username)

    OnbScreen(
        step = 1,
        ghostNum = 1,
        badge = "Part 1 · Username",
        cta = "Continue",
        ctaEnabled = isValid,
        onCta = { if (isValid) onContinue(username) },
        showBack = true,
        onBack = onBack,
    ) {
        OnbHero(text = "Choose a\nusername")
        Spacer(Modifier.height(14.dp))
        OnbSub("This is how friends find you. It cannot be changed later.")
        Spacer(Modifier.height(28.dp))

        UsernameField(
            value = username,
            onChange = { username = UsernameRules.sanitize(it) },
            isValid = isValid,
        )
        Spacer(Modifier.height(12.dp))
        // `isClean` is implicit: sanitize() guarantees username only contains valid chars,
        // so the validation badge is always green once the user typed anything.
        ValidationRow(isLong = isLong, isShort = isShort, isClean = username.isNotEmpty())
        Spacer(Modifier.height(20.dp))
        InfoCallout(text = "Zapp generates a local keypair. No server ever sees your private key.")
    }
}

// ───────────────────────────────────────────────────────────────
// Shared screen scaffold (single Column owning body + dock)
// ───────────────────────────────────────────────────────────────

@Composable
internal fun OnbScreen(
    step: Int,
    ghostNum: Int,
    badge: String,
    cta: String,
    onCta: () -> Unit,
    ctaEnabled: Boolean = true,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    showCta: Boolean = true,
    body: @Composable () -> Unit,
) {
    val c = ZappTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 20.dp)) {
            OnbProgress(step = step)
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 24.dp),
        ) {
            GhostNum(
                n = ghostNum,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            Column(modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth()) {
                Eyebrow(badge)
                Spacer(Modifier.height(14.dp))
                body()
            }
        }
        OnbBottomDock(
            cta = cta,
            onCta = onCta,
            showBack = showBack,
            onBack = onBack,
            ctaEnabled = ctaEnabled,
            showCta = showCta,
        )
    }
}

@Composable
internal fun UsernameField(
    value: String,
    onChange: (String) -> Unit,
    isValid: Boolean,
) {
    val c = ZappTheme.colors
    val borderColor = if (value.isNotEmpty()) c.text else c.border
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(width = 2.dp, color = borderColor, shape = RectangleShape)
                .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicText(
            text = "@",
            style =
                ZappTheme.typography.display.copy(
                    color = c.textSubtle,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                ),
        )
        Spacer(Modifier.width(2.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            cursorBrush = SolidColor(c.accent),
            textStyle =
                ZappTheme.typography.display.copy(
                    color = c.text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        BasicText(
                            text = "your_handle",
                            style =
                                ZappTheme.typography.display.copy(
                                    color = c.textSubtle,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.8).sp,
                                ),
                        )
                    }
                    inner()
                }
            },
        )
        if (isValid) {
            BasicText(
                text = "✓",
                style =
                    ZappTheme.typography.display.copy(
                        color = c.success,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                    ),
            )
        }
    }
}

@Composable
internal fun ValidationRow(isLong: Boolean, isShort: Boolean, isClean: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Chip(label = "3+ chars", ok = isLong)
        Chip(label = "≤20 chars", ok = isShort)
        Chip(label = "a–z 0–9 _", ok = isClean)
    }
}

@Composable
private fun Chip(label: String, ok: Boolean) {
    val c = ZappTheme.colors
    val color = if (ok) c.success else c.textSubtle
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            text = if (ok) "✓" else "✕",
            style = ZappTheme.typography.chip.copy(color = color, fontSize = 11.sp, fontWeight = FontWeight.Black),
        )
        Spacer(Modifier.width(4.dp))
        BasicText(
            text = label,
            style = ZappTheme.typography.chip.copy(color = color, fontSize = 11.sp, fontWeight = FontWeight.Black),
        )
    }
}

@Composable
internal fun InfoCallout(text: String) {
    val c = ZappTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, c.border, RectangleShape)
                .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        BasicText(
            text = "🛡",
            style = ZappTheme.typography.body.copy(color = c.accent, fontSize = 13.sp),
        )
        Spacer(Modifier.width(10.dp))
        BasicText(
            text = text,
            style =
                ZappTheme.typography.body.copy(
                    color = c.textSubtle,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
        )
    }
}

// ───────────────────────────────────────────────────────────────
// Seed reveal — shared between messaging and wallet phases
// ───────────────────────────────────────────────────────────────

@Composable
fun SeedRevealScreen(
    step: Int,
    title: String,
    sub: String,
    words: List<String>,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    showBack: Boolean = true,
) {
    var revealed by rememberSaveable { mutableStateOf(false) }
    var saved by rememberSaveable { mutableStateOf(false) }
    val c = ZappTheme.colors

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 20.dp)) {
            OnbProgress(step = step)
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 24.dp),
        ) {
            BasicText(
                text = title,
                style =
                    ZappTheme.typography.display.copy(
                        color = c.text,
                        fontSize = 26.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.8).sp,
                    ),
            )
            Spacer(Modifier.height(8.dp))
            OnbSub(text = sub, modifier = Modifier.fillMaxWidth(0.92f))
            Spacer(Modifier.height(20.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                SeedGrid(
                    words = words,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, c.border, RectangleShape)
                            .blur(if (revealed) 0.dp else 14.dp),
                )
                if (!revealed) {
                    Column(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .clickable { revealed = true },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .background(c.text, RectangleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "👁",
                                style = ZappTheme.typography.body.copy(color = c.bg, fontSize = 18.sp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        BasicText(
                            text = "Tap to reveal",
                            style =
                                ZappTheme.typography.button.copy(
                                    color = c.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.2.sp,
                                ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = revealed) { saved = !saved },
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(20.dp)
                            .background(if (saved) c.accent else c.bg, RectangleShape)
                            .border(2.dp, if (saved) c.accent else c.borderStrong, RectangleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (saved) {
                        BasicText(
                            text = "✓",
                            style = ZappTheme.typography.button.copy(color = c.onAccent, fontSize = 12.sp, fontWeight = FontWeight.Black),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                BasicText(
                    text = "I've written all ${words.size} words in order. I understand this phrase cannot be recovered if lost.",
                    style =
                        ZappTheme.typography.body.copy(
                            color = c.textMuted,
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                        ),
                )
            }
        }
        OnbBottomDock(
            cta = "I've saved it",
            onCta = onContinue,
            showBack = showBack,
            onBack = onBack,
            ctaEnabled = revealed && saved,
        )
    }
}

@Composable
private fun SeedGrid(words: List<String>, modifier: Modifier = Modifier) {
    val c = ZappTheme.colors
    // 3-column grid; row count flexes with seed length (12 → 4 rows, 24 → 8 rows).
    val rows = words.chunked(3)
    Column(modifier = modifier) {
        rows.forEachIndexed { ri, row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { wi, w ->
                    val idx = ri * 3 + wi
                    val cellBg = if (ri % 2 == 0) c.bg else c.surfaceAlt
                    Row(
                        modifier =
                            Modifier
                                .weight(1f)
                                .background(cellBg, RectangleShape)
                                .padding(horizontal = 10.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = String.format("%02d", idx + 1),
                            style =
                                ZappTheme.typography.mono.copy(
                                    color = c.textSubtle,
                                    fontSize = 9.sp,
                                ),
                            modifier = Modifier.width(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        BasicText(
                            text = w,
                            style =
                                ZappTheme.typography.rowTitle.copy(
                                    color = c.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.1).sp,
                                ),
                        )
                    }
                }
                // Pad incomplete final row so cells stay aligned.
                repeat(3 - row.size) {
                    Box(modifier = Modifier.weight(1f).background(c.bg, RectangleShape))
                }
            }
        }
    }
}
