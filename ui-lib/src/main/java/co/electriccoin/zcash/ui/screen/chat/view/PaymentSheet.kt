package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.design.theme.ZappTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentSheet(
    onSendZec: () -> Unit,
    onRequestZec: () -> Unit,
    onPayMerchant: () -> Unit,
    onShareAddress: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ZappTheme.colors.surface,
        scrimColor = ZappTheme.colors.overlay,
        shape = RectangleShape,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
        ) {
            PaymentSheetRow(
                icon = Icons.AutoMirrored.Filled.Send,
                label = "Send ZEC",
                onClick = onSendZec,
            )
            HorizontalDivider(
                color = ZappTheme.colors.border,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            PaymentSheetRow(
                icon = Icons.AutoMirrored.Filled.CallReceived,
                label = "Request ZEC",
                onClick = onRequestZec,
            )
            HorizontalDivider(
                color = ZappTheme.colors.border,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            PaymentSheetRow(
                icon = Icons.Default.Storefront,
                label = "Pay Merchant",
                onClick = onPayMerchant,
            )
            HorizontalDivider(
                color = ZappTheme.colors.border,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            PaymentSheetRow(
                icon = Icons.Default.QrCode2,
                label = "Share Address",
                onClick = onShareAddress,
            )
        }
    }
}

@Composable
private fun PaymentSheetRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RectangleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    role = Role.Button
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ZappTheme.colors.accent,
            modifier = Modifier.size(24.dp),
        )
        BasicText(
            label,
            style =
                ZappTheme.typography.rowTitle.copy(
                    color = ZappTheme.colors.text,
                    fontWeight = FontWeight.Black,
                ),
        )
    }
}
