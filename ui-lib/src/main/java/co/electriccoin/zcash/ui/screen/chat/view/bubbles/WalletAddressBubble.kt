package co.electriccoin.zcash.ui.screen.chat.view.bubbles

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.AndroidQrCodeImageGenerator
import co.electriccoin.zcash.ui.design.util.JvmQrCodeGenerator
import co.electriccoin.zcash.ui.screen.chat.model.ChatMessage
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val QR_PX = 400

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WalletAddressBubble(message: ChatMessage, isFromMe: Boolean) {
    val address = remember(message.content) {
        try { JSONObject(message.content).optString("content", message.content) }
        catch (_: Exception) { message.content }
    }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val qrBitmap = remember(address) {
        val pixels = JvmQrCodeGenerator.generate(address, QR_PX)
        AndroidQrCodeImageGenerator.generate(
            bitArray = pixels,
            sizePixels = QR_PX,
            background = Color.White.toArgb(),
            foreground = Color.Black.toArgb()
        ).asImageBitmap()
    }

    Surface(
        shape = RectangleShape,
        color = ZappTheme.colors.accent.copy(alpha = 0.08f),
        modifier = Modifier.widthIn(min = 200.dp, max = 280.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = ZappTheme.colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isFromMe) "Wallet address shared" else "Wallet address",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ZappTheme.colors.accent
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RectangleShape,
                color = Color.White,
            ) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "Wallet QR",
                    modifier = Modifier
                        .size(160.dp)
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall,
                color = ZappTheme.colors.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("address", address))
                            Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                        },
                        onClick = {}
                    )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = ZappTheme.colors.textMuted,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
