package co.electriccoin.zcash.ui.screen.swap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.ZcashTheme

@Composable
internal fun SwapTabSwitcher(
    selected: SwapTab,
    onSelect: (SwapTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ZappTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = TAB_HORIZONTAL_PADDING.dp, vertical = TAB_VERTICAL_PADDING.dp)
                .background(c.surface, RectangleShape)
                .border(BorderStroke(1.dp, c.border), RectangleShape)
                .padding(TAB_INNER_PADDING.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SwapTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val label =
                when (tab) {
                    SwapTab.SWAP -> stringResource(R.string.swap_tab_swap)
                    SwapTab.OFFRAMP -> stringResource(R.string.swap_tab_offramp)
                }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = TAB_CELL_MIN_HEIGHT.dp)
                        .background(
                            if (isSelected) c.accent else Color.Transparent,
                            RectangleShape,
                        ).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = c.accent),
                            onClick = { onSelect(tab) },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = label,
                    style =
                        ZappTheme.typography.caption.copy(
                            color = if (isSelected) c.onAccent else c.textMuted,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                        ),
                )
            }
        }
    }
}

private const val TAB_HORIZONTAL_PADDING = 18
private const val TAB_VERTICAL_PADDING = 8
private const val TAB_INNER_PADDING = 3
private const val TAB_CELL_MIN_HEIGHT = 34

@PreviewScreens
@Composable
private fun PreviewSwapSelected() {
    ZcashTheme {
        SwapTabSwitcher(selected = SwapTab.SWAP, onSelect = {})
    }
}

@PreviewScreens
@Composable
private fun PreviewOfframpSelected() {
    ZcashTheme {
        SwapTabSwitcher(selected = SwapTab.OFFRAMP, onSelect = {})
    }
}
