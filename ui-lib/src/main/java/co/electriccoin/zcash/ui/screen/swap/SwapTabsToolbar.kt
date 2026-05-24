package co.electriccoin.zcash.ui.screen.swap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.zapp.ZappBackButton
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.ZcashTheme

@Composable
internal fun SwapTabsToolbar(
    selected: SwapTab,
    onSelect: (SwapTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ZappTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(c.bg)
                .padding(horizontal = TOOLBAR_HORIZONTAL_PADDING.dp, vertical = TOOLBAR_VERTICAL_PADDING.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZappBackButton(onClick = onBack)
        Box(
            modifier = Modifier.weight(1f).padding(horizontal = SEGMENT_HORIZONTAL_INSET.dp),
            contentAlignment = Alignment.Center,
        ) {
            SegmentedRow(selected, onSelect)
        }
        // Mirrors the back button width so the segmented row stays centered.
        Box(modifier = Modifier.padding(start = BACK_BUTTON_MIRROR_PADDING.dp))
    }
}

@Composable
private fun SegmentedRow(
    selected: SwapTab,
    onSelect: (SwapTab) -> Unit,
) {
    val c = ZappTheme.colors
    Row(
        modifier =
            Modifier
                .background(c.surfaceAlt),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        SegmentCell(
            label = stringResource(R.string.swap_tab_swap),
            isSelected = selected == SwapTab.SWAP,
            onClick = { onSelect(SwapTab.SWAP) },
        )
        SegmentCell(
            label = stringResource(R.string.swap_tab_offramp),
            isSelected = selected == SwapTab.OFFRAMP,
            onClick = { onSelect(SwapTab.OFFRAMP) },
        )
    }
}

@Composable
private fun SegmentCell(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val c = ZappTheme.colors
    val t = ZappTheme.typography
    Box(
        modifier =
            Modifier
                .background(if (isSelected) c.surface else c.surfaceAlt)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = c.text),
                    onClick = onClick,
                ).padding(horizontal = CELL_HORIZONTAL_PADDING.dp, vertical = CELL_VERTICAL_PADDING.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style =
                t.button.copy(
                    color = if (isSelected) c.text else c.textMuted,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                ),
        )
    }
}

private const val TOOLBAR_HORIZONTAL_PADDING = 12
private const val TOOLBAR_VERTICAL_PADDING = 8
private const val SEGMENT_HORIZONTAL_INSET = 12
private const val BACK_BUTTON_MIRROR_PADDING = 40
private const val CELL_HORIZONTAL_PADDING = 24
private const val CELL_VERTICAL_PADDING = 10

@PreviewScreens
@Composable
private fun PreviewSwapSelected() {
    ZcashTheme {
        SwapTabsToolbar(selected = SwapTab.SWAP, onSelect = {}, onBack = {})
    }
}

@PreviewScreens
@Composable
private fun PreviewOfframpSelected() {
    ZcashTheme {
        SwapTabsToolbar(selected = SwapTab.OFFRAMP, onSelect = {}, onBack = {})
    }
}
