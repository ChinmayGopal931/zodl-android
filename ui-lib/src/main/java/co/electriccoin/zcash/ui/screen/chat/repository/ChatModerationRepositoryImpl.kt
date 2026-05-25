package co.electriccoin.zcash.ui.screen.chat.repository

import android.content.Context
import co.electriccoin.zcash.ui.screen.chat.model.BlockedUser
import co.electriccoin.zcash.ui.screen.chat.model.ContentReport
import co.electriccoin.zcash.ui.screen.chat.model.ReportCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ChatModerationRepositoryImpl(
    context: Context,
) : ChatModerationRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _blockedUsers = MutableStateFlow<Set<BlockedUser>>(emptySet())
    override val blockedUsers: StateFlow<Set<BlockedUser>> = _blockedUsers.asStateFlow()

    private val _blockedKeys = MutableStateFlow<Set<String>>(emptySet())
    override val blockedKeys: StateFlow<Set<String>> = _blockedKeys.asStateFlow()

    private val _reports = MutableStateFlow<List<ContentReport>>(emptyList())
    override val reports: StateFlow<List<ContentReport>> = _reports.asStateFlow()

    init {
        loadBlockedUsers()
        loadReports()
    }

    override fun isBlocked(publicKey: String): Boolean = _blockedKeys.value.contains(publicKey)

    override fun blockUser(publicKey: String, displayName: String?) {
        val user = BlockedUser(publicKey = publicKey, displayName = displayName)
        val updated = _blockedUsers.value + user
        _blockedUsers.value = updated
        _blockedKeys.value = updated.map { it.publicKey }.toSet()
        persistBlockedUsers(updated)
    }

    override fun unblockUser(publicKey: String) {
        val updated = _blockedUsers.value.filter { it.publicKey != publicKey }.toSet()
        _blockedUsers.value = updated
        _blockedKeys.value = updated.map { it.publicKey }.toSet()
        persistBlockedUsers(updated)
    }

    override fun submitReport(
        reportedPublicKey: String,
        reportedDisplayName: String?,
        category: ReportCategory,
        details: String,
        conversationId: String?,
        messageId: String?,
    ): ContentReport {
        val report =
            ContentReport(
                reportedPublicKey = reportedPublicKey,
                reportedDisplayName = reportedDisplayName,
                category = category,
                details = details,
                conversationId = conversationId,
                messageId = messageId,
            )
        _reports.value = _reports.value + report
        persistReports(_reports.value)
        return report
    }

    private fun loadBlockedUsers() {
        val json = prefs.getString(KEY_BLOCKED_USERS, null) ?: return
        try {
            val arr = JSONArray(json)
            val users = mutableSetOf<BlockedUser>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                users.add(
                    BlockedUser(
                        publicKey = obj.getString("publicKey"),
                        displayName = obj.optString("displayName").takeIf { it.isNotEmpty() },
                        blockedAt = obj.optLong("blockedAt", 0L)
                    )
                )
            }
            _blockedUsers.value = users
            _blockedKeys.value = users.map { it.publicKey }.toSet()
        } catch (_: org.json.JSONException) {
            // Corrupted data — start fresh
        }
    }

    private fun persistBlockedUsers(users: Set<BlockedUser>) {
        val arr = JSONArray()
        users.forEach { user ->
            arr.put(
                JSONObject().apply {
                    put("publicKey", user.publicKey)
                    put("displayName", user.displayName ?: "")
                    put("blockedAt", user.blockedAt)
                }
            )
        }
        prefs.edit().putString(KEY_BLOCKED_USERS, arr.toString()).apply()
    }

    private fun loadReports() {
        val json = prefs.getString(KEY_REPORTS, null) ?: return
        try {
            val arr = JSONArray(json)
            val reports = mutableListOf<ContentReport>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                reports.add(
                    ContentReport(
                        id = obj.getString("id"),
                        reportedPublicKey = obj.getString("reportedPublicKey"),
                        reportedDisplayName = obj.optString("reportedDisplayName").takeIf { it.isNotEmpty() },
                        category = ReportCategory.valueOf(obj.getString("category")),
                        details = obj.optString("details"),
                        conversationId = obj.optString("conversationId").takeIf { it.isNotEmpty() },
                        messageId = obj.optString("messageId").takeIf { it.isNotEmpty() },
                        timestamp = obj.optLong("timestamp", 0L)
                    )
                )
            }
            _reports.value = reports
        } catch (_: org.json.JSONException) {
            // Corrupted data — start fresh
        }
    }

    private fun persistReports(reports: List<ContentReport>) {
        val arr = JSONArray()
        reports.forEach { report ->
            arr.put(
                JSONObject().apply {
                    put("id", report.id)
                    put("reportedPublicKey", report.reportedPublicKey)
                    put("reportedDisplayName", report.reportedDisplayName ?: "")
                    put("category", report.category.name)
                    put("details", report.details)
                    put("conversationId", report.conversationId ?: "")
                    put("messageId", report.messageId ?: "")
                    put("timestamp", report.timestamp)
                }
            )
        }
        prefs.edit().putString(KEY_REPORTS, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "chat_moderation"
        private const val KEY_BLOCKED_USERS = "blocked_users"
        private const val KEY_REPORTS = "content_reports"
    }
}
