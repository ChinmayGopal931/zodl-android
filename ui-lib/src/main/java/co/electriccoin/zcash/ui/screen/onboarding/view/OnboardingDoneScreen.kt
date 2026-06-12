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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.theme.ZappTheme

@Composable
internal fun OnboardingDoneScreen(
    mode: TwoFAMode,
    onEnter: () -> Unit,
) {
    val c = ZappTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(
                    text = "✓",
                    style =
                        ZappTheme.typography.display.copy(
                            color = c.accent,
                            fontSize = 88.sp,
                            lineHeight = 92.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-4).sp,
                        ),
                )
                Spacer(Modifier.height(18.dp))
                BasicText(
                    text = stringResource(R.string.onboarding_done_title),
                    style =
                        ZappTheme.typography.display.copy(
                            color = c.text,
                            fontSize = 42.sp,
                            lineHeight = 44.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.8).sp,
                        ),
                )
                Spacer(Modifier.height(20.dp))
                AccentRule()
                Spacer(Modifier.height(20.dp))
                OnbSub(
                    text =
                        stringResource(
                            when (mode) {
                                TwoFAMode.Bio -> R.string.onboarding_done_subtitle_bio
                                TwoFAMode.Pin -> R.string.onboarding_done_subtitle_pin
                            }
                        ),
                )
            }
        }
        OnbBottomDock(cta = stringResource(R.string.onboarding_done_enter_cta), onCta = onEnter)
    }
}
