package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.chat.list.ChatListItemState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The pointerInput uses PointerEventPass.Initial so it sees MOVE events before
 * the inner clickable; consume() then cancels the clickable's tap tracking.
 * Delta is read from change.position - change.previousPosition, immune to
 * positionChangeConsumed, and rawOffset is a local var so there's no
 * coroutine race with offsetX.
 */
@Suppress("CyclomaticComplexMethod", "LoopWithTooManyJumpStatements", "MagicNumber")
@Composable
internal fun SwipeToLeaveRow(
    item: ChatListItemState,
    onLeave: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = ZappTheme.colors
    val scope = rememberCoroutineScope()
    var offsetX by remember { mutableFloatStateOf(0f) }
    val revealThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .pointerInput(item.id) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)

                        var rawOffset = 0f
                        var hAccum = 0f
                        var vAccum = 0f
                        var isHorizontalDrag = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break

                            if (!change.pressed) {
                                if (isHorizontalDrag) {
                                    val capturedOffset = rawOffset
                                    scope.launch {
                                        if (-capturedOffset >= revealThresholdPx) onLeave()
                                        Animatable(capturedOffset).animateTo(0f, tween(200)) {
                                            offsetX = value
                                        }
                                    }
                                }
                                break
                            }

                            val dx = (change.position - change.previousPosition).x
                            val dy = (change.position - change.previousPosition).y

                            if (!isHorizontalDrag) {
                                hAccum += dx
                                vAccum += dy
                                val absH = kotlin.math.abs(hAccum)
                                val absV = kotlin.math.abs(vAccum)
                                val slop = viewConfiguration.touchSlop

                                when {
                                    absH > slop && absH >= absV && hAccum < 0f -> {
                                        isHorizontalDrag = true
                                        rawOffset = hAccum.coerceIn(-revealThresholdPx * 2f, 0f)
                                        offsetX = rawOffset
                                        change.consume()
                                    }

                                    absV > slop || (absH > slop && hAccum >= 0f) -> {
                                        break
                                    }
                                }
                            } else {
                                change.consume()
                                rawOffset = (rawOffset + dx).coerceIn(-revealThresholdPx * 2f, 0f)
                                offsetX = rawOffset
                            }
                        }
                    }
                },
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(c.danger, RectangleShape)
                    .padding(end = 20.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            BasicText(
                text = stringRes(R.string.chat_list_leave_action).getValue(),
                style =
                    ZappTheme.typography.button.copy(
                        color = c.bg,
                        fontWeight = FontWeight.Black,
                    ),
            )
        }

        Box(
            modifier =
                Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .background(c.bg)
                    .fillMaxWidth(),
        ) {
            content()
        }
    }
}
