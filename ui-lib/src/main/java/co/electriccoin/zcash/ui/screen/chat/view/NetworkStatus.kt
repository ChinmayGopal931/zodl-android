package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.list.ChatListConnectionStatus
import co.electriccoin.zcash.ui.screen.chat.list.ChatListDhtHealth
import co.electriccoin.zcash.ui.screen.chat.model.ConnectionDetailsUi

@Composable
internal fun ConnectionPill(
    connectionStatus: ChatListConnectionStatus,
    peerCount: Int,
    dhtHealth: ChatListDhtHealth,
    onClick: (() -> Unit)? = null
) {
    val errorColor = ZappTheme.colors.danger
    val okColor = ZappTheme.colors.success
    val warnColor = ZappTheme.colors.accent
    val statusColor =
        when (connectionStatus) {
            ChatListConnectionStatus.CONNECTED -> {
                when {
                    dhtHealth == ChatListDhtHealth.CRITICAL -> errorColor
                    peerCount > 0 -> okColor
                    dhtHealth == ChatListDhtHealth.DEGRADED -> warnColor
                    else -> okColor
                }
            }

            ChatListConnectionStatus.CONNECTING -> {
                warnColor
            }

            else -> {
                errorColor
            }
        }
    val label =
        when (connectionStatus) {
            ChatListConnectionStatus.CONNECTED -> {
                when {
                    dhtHealth == ChatListDhtHealth.CRITICAL -> "DHT unreachable"
                    peerCount > 0 -> if (peerCount == 1) "1 peer" else "$peerCount peers"
                    dhtHealth == ChatListDhtHealth.DEGRADED -> "DHT degraded"
                    else -> "Online"
                }
            }

            ChatListConnectionStatus.CONNECTING -> {
                "Connecting"
            }

            ChatListConnectionStatus.DISCONNECTED -> {
                "Offline"
            }

            ChatListConnectionStatus.ERROR -> {
                "Error"
            }
        }

    Surface(
        shape = RectangleShape,
        color = statusColor.copy(alpha = 0.12f),
        modifier =
            Modifier
                .height(26.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier.size(7.dp),
                shape = CircleShape,
                color = statusColor
            ) {}
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetworkDetailsSheet(
    connectionStatus: ChatListConnectionStatus,
    peerCount: Int,
    dhtHealth: ChatListDhtHealth,
    connectionDetails: ConnectionDetailsUi?,
    onDismiss: () -> Unit
) {
    val errorColor = ZappTheme.colors.danger
    val okColor = ZappTheme.colors.success
    val warnColor = ZappTheme.colors.accent
    val secondaryColor = ZappTheme.colors.textMuted

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ZappTheme.colors.bg
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Network",
                style = MaterialTheme.typography.titleMedium,
                color = ZappTheme.colors.text,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            NetworkDetailRow(
                icon = Icons.Default.Wifi,
                label = "Connection",
                value =
                    when (connectionStatus) {
                        ChatListConnectionStatus.CONNECTED -> "Connected"
                        ChatListConnectionStatus.CONNECTING -> "Connecting"
                        ChatListConnectionStatus.DISCONNECTED -> "Disconnected"
                        ChatListConnectionStatus.ERROR -> "Error"
                    },
                valueColor =
                    when (connectionStatus) {
                        ChatListConnectionStatus.CONNECTED -> okColor
                        ChatListConnectionStatus.CONNECTING -> warnColor
                        else -> errorColor
                    }
            )

            NetworkDetailRow(
                icon = Icons.Default.Hub,
                label = "DHT",
                value =
                    when (dhtHealth) {
                        ChatListDhtHealth.HEALTHY -> "Healthy"
                        ChatListDhtHealth.DEGRADED -> "Degraded"
                        ChatListDhtHealth.CRITICAL -> "Critical"
                    },
                valueColor =
                    when (dhtHealth) {
                        ChatListDhtHealth.HEALTHY -> okColor
                        ChatListDhtHealth.DEGRADED -> warnColor
                        ChatListDhtHealth.CRITICAL -> errorColor
                    }
            )

            NetworkDetailRow(
                icon = Icons.Default.People,
                label = "Peers",
                value = peerCount.toString(),
                valueColor = if (peerCount > 0) okColor else secondaryColor
            )

            connectionDetails?.let { details ->
                HorizontalDivider(color = ZappTheme.colors.border.copy(alpha = 0.3f))

                NetworkDetailRow(
                    icon = Icons.Default.Cable,
                    label = "TCP connections",
                    value = details.globalConnections.toString()
                )

                NetworkDetailRow(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = "Conversations",
                    value = "${details.directConversations} direct · ${details.groupConversations} group"
                )

                NetworkDetailRow(
                    icon = Icons.Default.Schedule,
                    label = "Pending",
                    value =
                        if (details.pendingMessageCount > 0) {
                            "${details.pendingMessageCount} msgs in ${details.pendingQueues} queues"
                        } else {
                            "None"
                        },
                    valueColor = if (details.pendingMessageCount > 0) warnColor else okColor
                )

                HorizontalDivider(color = ZappTheme.colors.border.copy(alpha = 0.3f))

                NetworkDetailRow(
                    icon = Icons.Default.Security,
                    label = "Protocol",
                    value = "Zapp Messaging P2P"
                )

                NetworkDetailRow(
                    icon = Icons.Default.Lock,
                    label = "Encryption",
                    value = "End-to-end (Noise XX)"
                )
            }
        }
    }
}

@Composable
private fun NetworkDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    val effectiveValueColor =
        if (valueColor == Color.Unspecified) {
            ZappTheme.colors.text
        } else {
            valueColor
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = ZappTheme.colors.textMuted
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = ZappTheme.colors.textMuted
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = effectiveValueColor
        )
    }
}
