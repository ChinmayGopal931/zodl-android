package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.design.theme.ZappTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttachmentSheet(
    onShareAddress: () -> Unit,
    onSendZec: () -> Unit,
    onAttachMedia: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ZappTheme.colors.surface,
        scrimColor = ZappTheme.colors.overlay,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
        ) {
            AttachmentRow(
                icon = Icons.Default.QrCode2,
                label = "Share Address",
                onClick = onShareAddress
            )
            HorizontalDivider(
                color = ZappTheme.colors.border,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            AttachmentRow(
                icon = Icons.AutoMirrored.Filled.Send,
                label = "Send ZEC",
                onClick = onSendZec
            )
            HorizontalDivider(
                color = ZappTheme.colors.border,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            AttachmentRow(
                icon = Icons.Default.AttachFile,
                label = "Attach Media",
                onClick = onAttachMedia
            )
        }
    }
}

@Composable
private fun AttachmentRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RectangleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = ZappTheme.colors.accent,
            modifier = Modifier.size(24.dp)
        )
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = ZappTheme.colors.text
        )
    }
}
