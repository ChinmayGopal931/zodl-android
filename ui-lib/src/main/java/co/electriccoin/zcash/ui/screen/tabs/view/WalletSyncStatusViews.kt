package co.electriccoin.zcash.ui.screen.tabs.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.design.component.zapp.ZappChipVariant
import co.electriccoin.zcash.ui.design.component.zapp.ZappStatusChip
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.tabs.viewmodel.WalletSyncChipState
import co.electriccoin.zcash.ui.screen.tabs.viewmodel.WalletSyncStatus

@Composable
internal fun SyncProgressRow(state: WalletSyncChipState) {
    val label = when (state.status) {
        WalletSyncStatus.SYNCING -> "Syncing"
        WalletSyncStatus.RESTORING -> "Restoring"
        WalletSyncStatus.INITIALIZING -> "Connecting"
        WalletSyncStatus.DISCONNECTED -> "Offline — reconnecting"
        WalletSyncStatus.ERROR -> "Sync error"
        WalletSyncStatus.SYNCED -> return
    }
    val c = ZappTheme.colors
    val isError = state.status == WalletSyncStatus.DISCONNECTED || state.status == WalletSyncStatus.ERROR
    val fillColor = if (isError) c.danger else c.accent
    val fraction = (state.progressPercent.coerceIn(0, 100)) / 100f
    val showPercent = state.status == WalletSyncStatus.SYNCING || state.status == WalletSyncStatus.RESTORING

    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = label,
                style = ZappTheme.typography.rowSubtitle.copy(
                    color = if (isError) c.danger else c.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                ),
            )
            if (showPercent) {
                BasicText(
                    text = "${state.progressPercent}%",
                    style = ZappTheme.typography.rowSubtitle.copy(
                        color = c.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(c.surfaceAlt, RectangleShape),
        ) {
            if (showPercent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                        .height(3.dp)
                        .background(fillColor, RectangleShape),
                )
            } else if (isError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(fillColor, RectangleShape),
                )
            }
        }
    }
}

@Composable
internal fun SyncStatusChip(state: WalletSyncChipState) {
    val c = ZappTheme.colors
    when (state.status) {
        WalletSyncStatus.SYNCED ->
            ZappStatusChip("Synced", variant = ZappChipVariant.Success, dotColor = c.success)
        WalletSyncStatus.SYNCING ->
            ZappStatusChip("Syncing ${state.progressPercent}%", variant = ZappChipVariant.Accent, dotColor = c.accent)
        WalletSyncStatus.RESTORING ->
            ZappStatusChip("Restoring ${state.progressPercent}%", variant = ZappChipVariant.Accent, dotColor = c.accent)
        WalletSyncStatus.DISCONNECTED ->
            ZappStatusChip("Offline", variant = ZappChipVariant.Danger, dotColor = c.danger)
        WalletSyncStatus.ERROR ->
            ZappStatusChip("Sync error", variant = ZappChipVariant.Danger, dotColor = c.danger)
        WalletSyncStatus.INITIALIZING ->
            ZappStatusChip("Connecting", variant = ZappChipVariant.Muted, dotColor = c.textSubtle)
    }
}
