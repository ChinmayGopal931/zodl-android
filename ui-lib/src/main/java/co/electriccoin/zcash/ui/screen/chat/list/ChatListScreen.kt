@file:Suppress("ktlint:standard:filename")

package co.electriccoin.zcash.ui.screen.chat.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.screen.chat.view.ChatListView
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun ChatListScreen(showBackButton: Boolean = true) {
    val viewModel = koinViewModel<ChatListVM>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler { state.onBack() }
    ChatListView(
        state = state,
        showBackButton = showBackButton,
        modifier = Modifier.fillMaxSize(),
    )
}
