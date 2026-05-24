@file:Suppress("MatchingDeclarationName")

package co.electriccoin.zcash.ui.screen.authentication.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ProvideZappTheme
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.screen.authentication.view.AnimationConstants.ANIMATION_DURATION
import co.electriccoin.zcash.ui.screen.authentication.view.AnimationConstants.INITIAL_DELAY
import co.electriccoin.zcash.ui.screen.authentication.view.AnimationConstants.WELCOME_ANIM_TEST_TAG
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object AnimationConstants {
    const val ANIMATION_DURATION = 700
    const val INITIAL_DELAY = 1000
    const val WELCOME_ANIM_TEST_TAG = "WELCOME_ANIM_TEST_TAG"

    fun together() = (ANIMATION_DURATION + INITIAL_DELAY).toLong()

    fun durationOnly() = (ANIMATION_DURATION).toLong()
}

@Composable
fun WelcomeAnimationAutostart(
    showAuthLogo: Boolean,
    onRetry: (() -> Unit),
    modifier: Modifier = Modifier,
    delay: Duration = INITIAL_DELAY.milliseconds,
) {
    var currentAnimationState by remember { mutableStateOf(true) }

    WelcomeScreenView(
        showAuthLogo = showAuthLogo,
        animationState = currentAnimationState,
        onRetry = onRetry,
        modifier = modifier.testTag(WELCOME_ANIM_TEST_TAG)
    )

    // Let's start the animation automatically in case e.g. authentication is not involved
    LaunchedEffect(key1 = currentAnimationState) {
        kotlinx.coroutines.delay(delay)
        currentAnimationState = false
    }
}

@Composable
@Suppress("LongMethod", "MagicNumber")
fun WelcomeScreenView(
    animationState: Boolean,
    showAuthLogo: Boolean,
    onRetry: (() -> Unit),
    modifier: Modifier = Modifier,
) {
    val revealProgress by animateFloatAsState(
        targetValue = if (!animationState) 1f else 0f,
        animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = FastOutLinearInEasing),
        label = "revealProgress"
    )

    val points = remember { generateChartPoints() }
    val chartHeightDp = CHART_HEIGHT.dp

    ProvideZappTheme {
        WelcomeContent(
            modifier = modifier,
            revealProgress = revealProgress,
            points = points,
            chartHeightDp = chartHeightDp,
            showAuthLogo = showAuthLogo,
            onRetry = onRetry,
        )
    }
}

@Composable
@Suppress("MagicNumber")
private fun WelcomeContent(
    modifier: Modifier,
    revealProgress: Float,
    points: List<ChartPoint>,
    chartHeightDp: androidx.compose.ui.unit.Dp,
    showAuthLogo: Boolean,
    onRetry: () -> Unit,
) {
    val c = ZappTheme.colors

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.accent) // Zapp brand yellow/orange — the splash colour
                .drawWithContent {
                    if (revealProgress < 1f) {
                        val wavePath =
                            Path().apply {
                                val chartHeightPx = chartHeightDp.toPx()
                                val waveTopY = (size.height + chartHeightPx) * (1f - revealProgress) - chartHeightPx
                                moveTo(0f, 0f)
                                lineTo(size.width, 0f)
                                for (i in points.size - 1 downTo 0) {
                                    lineTo(size.width * points[i].x, waveTopY + chartHeightPx * points[i].y)
                                }
                                close()
                            }
                        clipPath(wavePath) {
                            this@drawWithContent.drawContent()
                        }
                    }
                }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Big Swiss "Hi." — Black weight, tight letter spacing, white on yellow.
            BasicText(
                text = "Hi.",
                style =
                    ZappTheme.typography.display.copy(
                        color = Color.White,
                        fontSize = 140.sp,
                        lineHeight = 130.sp,
                        letterSpacing = (-6).sp,
                        fontWeight = FontWeight.Black,
                    ),
            )
            Spacer(Modifier.height(20.dp))
            // 36×3 horizontal rule — same accent device used on WelcomeGate.
            Box(
                modifier =
                    Modifier
                        .width(36.dp)
                        .height(3.dp)
                        .background(c.text, RectangleShape),
            )

            AnimatedVisibility(visible = showAuthLogo) {
                // Auto-retry as soon as the lock UI becomes visible — no tap required.
                LaunchedEffect(Unit) {
                    onRetry()
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Sharp-edged tap-target — kept as a manual fallback.
                    val unlockDesc =
                        stringResource(
                            id = R.string.authentication_failed_welcome_icon_cont_desc,
                            stringResource(R.string.app_name),
                        )
                    Box(
                        modifier =
                            Modifier
                                .size(56.dp)
                                .background(c.text, RectangleShape)
                                .clickable(onClick = onRetry)
                                .semantics { contentDescription = unlockDesc },
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "🔒",
                            style =
                                ZappTheme.typography.display.copy(
                                    color = c.accent,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                ),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    BasicText(
                        text = stringResource(id = R.string.authentication_failed_welcome_title),
                        style =
                            ZappTheme.typography.display.copy(
                                color = c.text,
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.4).sp,
                                textAlign = TextAlign.Center,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        text = stringResource(id = R.string.authentication_failed_welcome_subtitle),
                        style =
                            ZappTheme.typography.body.copy(
                                color = c.text,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@PreviewScreens
@Composable
private fun WelcomeScreenPreview() {
    ZcashTheme {
        WelcomeAnimationAutostart(false, {})
    }
}

@PreviewScreens
@Composable
private fun WelcomeScreenAuthPreview() {
    ZcashTheme {
        WelcomeAnimationAutostart(true, {})
    }
}
