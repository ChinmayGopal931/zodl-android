package co.electriccoin.zcash.ui.screen.tabs.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.design.theme.ZappTheme

enum class ZappTab(
    val title: String
) {
    WALLET("Wallet"),
    CHATS("Chats"),
    CONTACTS("Contacts"),
    SETTINGS("Settings"),
}

@Composable
fun FloatingPillNavBar(
    currentTab: ZappTab,
    chatUnreadCount: Int,
    onTabSelected: (ZappTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ZappTheme.colors

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RectangleShape, clip = false)
                    .background(c.navPill, RectangleShape)
                    .border(BorderStroke(1.dp, c.border), RectangleShape)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ZappTab.entries.forEach { tab ->
                val selected = tab == currentTab
                val icon: ImageVector = iconFor(tab, selected)
                val showBadge = tab == ZappTab.CHATS && chatUnreadCount > 0

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .background(
                                color = if (selected) c.accent else Color.Transparent,
                                shape = RectangleShape,
                            ).clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication =
                                    ripple(
                                        color = if (selected) c.onAccent else c.accent,
                                        bounded = true,
                                    ),
                                onClick = { onTabSelected(tab) },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.title,
                        tint = if (selected) c.onAccent else c.textMuted,
                        modifier = Modifier.size(22.dp),
                    )

                    if (showBadge) {
                        val badgeText = if (chatUnreadCount > 99) "99+" else chatUnreadCount.toString()
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-6).dp, y = 4.dp)
                                    .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                    .background(c.danger, RectangleShape)
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = badgeText,
                                style =
                                    ZappTheme.typography.chip.copy(
                                        color = c.onAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun iconFor(
    tab: ZappTab,
    selected: Boolean,
): ImageVector =
    when (tab) {
        ZappTab.WALLET -> if (selected) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
        ZappTab.CHATS -> if (selected) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat
        ZappTab.CONTACTS -> if (selected) Icons.Filled.Contacts else Icons.Outlined.Contacts
        ZappTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    }
