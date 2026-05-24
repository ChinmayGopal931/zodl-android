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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.design.theme.ZappTheme

/**
 * Real biometric enrollment screen.
 *
 * @param isEnrolling True while the system biometric prompt is in flight — disables CTA.
 * @param errorMessage Non-null when the last attempt failed; shown in danger color.
 * @param onEnroll Called when the user taps the CTA (or "Retry" on error).
 * @param onCancel Called when the user taps back — returns to the choice screen.
 */
@Composable
fun BioScanScreen(
    isEnrolling: Boolean,
    errorMessage: String?,
    onEnroll: () -> Unit,
    onCancel: () -> Unit,
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
            OnbProgress(step = 3)
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 28.dp),
            ) {
                BasicText(
                    text = "◉",
                    style =
                        ZappTheme.typography.display.copy(
                            color =
                                when {
                                    isEnrolling -> c.accent
                                    errorMessage != null -> c.danger
                                    else -> c.text
                                },
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                        ),
                )
                Spacer(Modifier.height(20.dp))
                BasicText(
                    text =
                        when {
                            isEnrolling -> "Verifying…"
                            else -> "Biometric unlock"
                        },
                    style =
                        ZappTheme.typography.display.copy(
                            color = c.text,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.8).sp,
                        ),
                )
                Spacer(Modifier.height(8.dp))
                if (errorMessage != null) {
                    BasicText(
                        text = errorMessage,
                        style =
                            ZappTheme.typography.body.copy(
                                color = c.danger,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                            ),
                        modifier = Modifier.fillMaxWidth(0.9f),
                    )
                } else {
                    OnbSub(
                        text = "Zapp will use your fingerprint or face to lock and unlock the app.",
                        modifier = Modifier.fillMaxWidth(0.9f),
                    )
                }
            }
        }
        OnbBottomDock(
            cta =
                when {
                    isEnrolling -> "Verifying…"
                    errorMessage != null -> "Retry"
                    else -> "Enable Biometrics"
                },
            onCta = onEnroll,
            ctaEnabled = !isEnrolling,
            showBack = true,
            onBack = onCancel,
        )
    }
}
