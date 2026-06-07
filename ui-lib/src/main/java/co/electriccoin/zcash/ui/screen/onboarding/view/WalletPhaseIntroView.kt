package co.electriccoin.zcash.ui.screen.onboarding.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.theme.ZappTheme

// ───────────────────────────────────────────────────────────────
// Phase 1 intro — Wallet
// ───────────────────────────────────────────────────────────────

@Composable
fun WalletPhaseIntro(
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    OnbScreen(
        step = 1,
        ghostNum = 1,
        badge = "Part 1 of 3 · Wallet & Messaging",
        cta = "Continue",
        onCta = onContinue,
        showBack = true,
        onBack = onBack,
    ) {
        OnbHero(text = "Wallet &\nmessaging\nsetup")
        Spacer(Modifier.height(16.dp))
        OnbSub(
            text = "Your wallet's 24-word recovery phrase also seeds your messaging identity — one phrase covers both.",
            modifier = Modifier.fillMaxWidth(0.94f),
        )
        Spacer(Modifier.height(28.dp))
        OnbBulletRow(
            label = "Create or restore a wallet",
            sub = "Your choice here also sets your messaging identity",
            isFirst = true,
        )
        OnbBulletRow(
            label = "One phrase for everything",
            sub = "24 BIP-39 words restore your funds and your chats — back it up offline.",
        )
    }
}

// ───────────────────────────────────────────────────────────────
// 06 · Wallet choice — Create / Restore (skip via dock)
// ───────────────────────────────────────────────────────────────

@Composable
fun WalletChoiceScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onRestore: () -> Unit,
) {
    val c = ZappTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        // Progress bar
        Box(modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 20.dp)) {
            OnbProgress(step = 1)
        }
        // Body — hero at top, action card pinned to bottom (thumb zone)
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 24.dp),
        ) {
            GhostNum(n = 1, modifier = Modifier.align(Alignment.TopEnd))
            Column(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()) {
                Eyebrow("Part 1 · Wallet & Messaging")
                Spacer(Modifier.height(14.dp))
                OnbHero(text = "Set up\nyour wallet")
                Spacer(Modifier.height(14.dp))
                OnbSub("Your wallet seed also creates your messaging identity. One backup for everything.")
            }
            OnbActionListCard(
                actions =
                    listOf(
                        OnbAction(
                            icon = "✦",
                            label = "Create new wallet",
                            sub = "New wallet + fresh messaging identity",
                            onClick = onCreate,
                            highlight = true,
                        ),
                        OnbAction(
                            icon = "⚿",
                            label = "Restore from phrase",
                            sub = "Restores wallet and derives your messaging ID from it",
                            onClick = onRestore,
                        ),
                    ),
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
            )
        }
        // Bottom dock — back button only (bottom-left, per SKILL.md Pattern A)
        OnbBottomDock(
            cta = "",
            onCta = {},
            showBack = true,
            onBack = onBack,
            showCta = false,
        )
    }
}

// ───────────────────────────────────────────────────────────────
// 07 · Wallet recovery phrase
// ───────────────────────────────────────────────────────────────

@Composable
fun WalletSeedPhraseScreen(
    words: List<String>,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    SeedRevealScreen(
        step = 1,
        title = "Wallet &\nmessaging phrase",
        sub = "These 24 words restore both your wallet funds and your messaging identity. Back them up offline — they cover everything.",
        words = words,
        onBack = onBack,
        onContinue = onContinue,
    )
}
