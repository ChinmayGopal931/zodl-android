package co.electriccoin.zcash.ui.screen.chat.list

enum class ChatListConnectionStatus { CONNECTED, CONNECTING, DISCONNECTED, ERROR }

enum class ChatListDhtHealth { HEALTHY, DEGRADED, CRITICAL }

internal fun mapDhtHealth(value: String): ChatListDhtHealth =
    when (value) {
        "degraded" -> ChatListDhtHealth.DEGRADED
        "critical" -> ChatListDhtHealth.CRITICAL
        else -> ChatListDhtHealth.HEALTHY
    }
