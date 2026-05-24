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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.common.compose.SecureScreen
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import kotlinx.coroutines.delay

/**
 * Two-phase 6-digit PIN creation screen.
 *
 * Phase 1 — user enters a new PIN.
 * Phase 2 — user re-enters the same PIN to confirm.
 * Mismatch: error shown for 1.5 s, then resets to phase 1.
 * On match: [onPinConfirmed] is called with the raw 6-digit string.
 */
@Composable
fun PinSetupScreen(
    onBack: () -> Unit,
    onPinConfirmed: (String) -> Unit,
) {
    SecureScreen()

    val c = ZappTheme.colors

    var isConfirmPhase by rememberSaveable { mutableStateOf(false) }
    var firstPin by rememberSaveable { mutableStateOf("") }
    var currentInput by rememberSaveable { mutableStateOf("") }
    var mismatchError by rememberSaveable { mutableStateOf(false) }

    // Auto-advance phases when 6 digits are entered.
    LaunchedEffect(currentInput) {
        if (currentInput.length == 6) {
            if (!isConfirmPhase) {
                firstPin = currentInput
                currentInput = ""
                isConfirmPhase = true
            } else {
                if (currentInput == firstPin) {
                    onPinConfirmed(currentInput)
                } else {
                    mismatchError = true
                    currentInput = ""
                }
            }
        }
    }

    // Auto-reset after mismatch so the user starts over from phase 1.
    LaunchedEffect(mismatchError) {
        if (mismatchError) {
            delay(MISMATCH_RESET_DELAY_MS)
            isConfirmPhase = false
            firstPin = ""
            currentInput = ""
            mismatchError = false
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 20.dp)) {
            OnbProgress(step = 3)
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 24.dp),
        ) {
            GhostNum(n = 3, modifier = Modifier.align(Alignment.TopEnd))
            Column(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()) {
                Eyebrow("Part 3 of 3 · Secure Zapp")
                Spacer(Modifier.height(14.dp))
                OnbHero(
                    text = if (isConfirmPhase) "Confirm\nyour PIN" else "Create\nyour PIN"
                )
                Spacer(Modifier.height(14.dp))
                if (mismatchError) {
                    BasicText(
                        text = "PINs don't match. Please try again.",
                        style =
                            ZappTheme.typography.body.copy(
                                color = c.danger,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                            ),
                    )
                } else {
                    OnbSub(
                        text =
                            if (isConfirmPhase) {
                                "Re-enter your 6-digit PIN to confirm."
                            } else {
                                "Choose a 6-digit code you'll remember."
                            },
                    )
                }
            }
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PinDotRow(filledCount = currentInput.length, hasError = mismatchError)
                Spacer(Modifier.height(28.dp))
                PinKeypad(
                    modifier = Modifier.fillMaxWidth(),
                    onKey = { key ->
                        if (!mismatchError) {
                            when {
                                key == "⌫" -> {
                                    if (currentInput.isNotEmpty()) {
                                        currentInput = currentInput.dropLast(1)
                                    }
                                }

                                currentInput.length < 6 -> {
                                    currentInput += key
                                }
                            }
                        }
                    },
                )
            }
        }
        OnbBottomDock(
            cta = "",
            onCta = {},
            showBack = true,
            onBack = {
                if (isConfirmPhase) {
                    isConfirmPhase = false
                    currentInput = ""
                    firstPin = ""
                } else {
                    onBack()
                }
            },
            showCta = false,
        )
    }
}

private const val MISMATCH_RESET_DELAY_MS = 1_500L
