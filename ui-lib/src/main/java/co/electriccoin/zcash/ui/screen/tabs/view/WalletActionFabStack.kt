package co.electriccoin.zcash.ui.screen.tabs.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.component.zapp.ZappFab
import co.electriccoin.zcash.ui.design.theme.colors.ZappNavBar

@Composable
internal fun PayActionFabStack(
    onPayMerchant: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    end = 18.dp,
                    bottom = ZappNavBar.FAB_BOTTOM_PADDING_DP.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.End,
    ) {
        ZappFab(
            icon = Icons.Default.Storefront,
            contentDescription = "Pay Merchant",
            onClick = onPayMerchant,
        )
        ZappFab(
            icon = Icons.AutoMirrored.Filled.CallMade,
            contentDescription = "Send",
            onClick = onSend,
        )
        ZappFab(
            icon = Icons.AutoMirrored.Filled.CallReceived,
            contentDescription = "Receive",
            onClick = onReceive,
        )
    }
}
