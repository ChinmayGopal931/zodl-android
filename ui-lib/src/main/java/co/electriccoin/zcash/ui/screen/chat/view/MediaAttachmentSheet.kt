package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.design.theme.ZappTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaAttachmentSheet(
    onChooseMedia: () -> Unit,
    onAttachFile: () -> Unit,
    onTakePhoto: () -> Unit,
    onShareLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ZappTheme.colors.surface,
        scrimColor = ZappTheme.colors.overlay,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Attach",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = ZappTheme.colors.text
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MediaOption(
                    icon = Icons.Default.Image,
                    label = "Media",
                    onClick = onChooseMedia,
                    modifier = Modifier.weight(1f)
                )
                MediaOption(
                    icon = Icons.Default.AttachFile,
                    label = "File",
                    onClick = onAttachFile,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MediaOption(
                    icon = Icons.Default.CameraAlt,
                    label = "Camera",
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f)
                )
                MediaOption(
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    onClick = onShareLocation,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MediaOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        color = ZappTheme.colors.surfaceAlt
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = ZappTheme.colors.accent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ZappTheme.colors.text
            )
        }
    }
}
