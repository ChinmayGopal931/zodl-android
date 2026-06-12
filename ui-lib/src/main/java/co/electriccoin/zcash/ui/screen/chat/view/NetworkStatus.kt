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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.R
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
                    dhtHealth == ChatListDhtHealth.CRITICAL ->
                        stringResource(R.string.chat_room_subtitle_dht_unreachable)

                    peerCount > 0 ->
                        pluralStringResource(R.plurals.chat_network_peers_count, peerCount, peerCount)

                    dhtHealth == ChatListDhtHealth.DEGRADED ->
                        stringResource(R.string.chat_room_subtitle_dht_degraded)

                    else -> stringResource(R.string.chat_room_chip_online)
                }
            }

            ChatListConnectionStatus.CONNECTING -> {
                stringResource(R.string.chat_settings_connection_connecting)
            }

            ChatListConnectionStatus.DISCONNECTED -> {
                stringResource(R.string.chat_room_subtitle_offline)
            }

            ChatListConnectionStatus.ERROR -> {
                stringResource(R.string.chat_settings_connection_error)
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
                stringResource(R.string.chat_settings_section_network),
                style = MaterialTheme.typography.titleMedium,
                color = ZappTheme.colors.text,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            NetworkDetailRow(
                icon = Icons.Default.Wifi,
                label = stringResource(R.string.chat_settings_label_connection),
                value =
                    when (connectionStatus) {
                        ChatListConnectionStatus.CONNECTED ->
                            stringResource(R.string.chat_settings_connection_connected)

                        ChatListConnectionStatus.CONNECTING ->
                            stringResource(R.string.chat_settings_connection_connecting)

                        ChatListConnectionStatus.DISCONNECTED ->
                            stringResource(R.string.chat_settings_connection_disconnected)

                        ChatListConnectionStatus.ERROR ->
                            stringResource(R.string.chat_settings_connection_error)
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
                label = stringResource(R.string.chat_room_chip_dht),
                value =
                    when (dhtHealth) {
                        ChatListDhtHealth.HEALTHY -> stringResource(R.string.chat_settings_dht_healthy)
                        ChatListDhtHealth.DEGRADED -> stringResource(R.string.chat_settings_dht_degraded)
                        ChatListDhtHealth.CRITICAL -> stringResource(R.string.chat_settings_dht_critical)
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
                label = stringResource(R.string.chat_settings_label_peers),
                value = peerCount.toString(),
                valueColor = if (peerCount > 0) okColor else secondaryColor
            )

            connectionDetails?.let { details ->
                HorizontalDivider(color = ZappTheme.colors.border.copy(alpha = 0.3f))

                NetworkDetailRow(
                    icon = Icons.Default.Cable,
                    label = stringResource(R.string.chat_network_label_tcp_connections),
                    value = details.globalConnections.toString()
                )

                NetworkDetailRow(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = stringResource(R.string.chat_network_label_conversations),
                    value =
                        stringResource(
                            R.string.chat_network_value_conversations_fmt,
                            details.directConversations,
                            details.groupConversations
                        )
                )

                NetworkDetailRow(
                    icon = Icons.Default.Schedule,
                    label = stringResource(R.string.chat_network_label_pending),
                    value =
                        if (details.pendingMessageCount > 0) {
                            stringResource(
                                R.string.chat_network_value_pending_fmt,
                                details.pendingMessageCount,
                                details.pendingQueues
                            )
                        } else {
                            stringResource(R.string.chat_network_value_pending_none)
                        },
                    valueColor = if (details.pendingMessageCount > 0) warnColor else okColor
                )

                HorizontalDivider(color = ZappTheme.colors.border.copy(alpha = 0.3f))

                NetworkDetailRow(
                    icon = Icons.Default.Security,
                    label = stringResource(R.string.chat_settings_label_protocol),
                    value = stringResource(R.string.chat_network_value_protocol)
                )

                NetworkDetailRow(
                    icon = Icons.Default.Lock,
                    label = stringResource(R.string.chat_settings_label_encryption),
                    value = stringResource(R.string.chat_network_value_encryption)
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
