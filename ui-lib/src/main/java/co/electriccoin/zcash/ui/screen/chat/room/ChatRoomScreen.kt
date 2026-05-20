@file:Suppress("ktlint:standard:filename")

package co.electriccoin.zcash.ui.screen.chat.room

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.screen.chat.ChatRoomArgs
import co.electriccoin.zcash.ui.screen.chat.view.ChatRoomView
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun ChatRoomScreen(args: ChatRoomArgs) {
    val viewModel = koinViewModel<ChatRoomVM> { parametersOf(args) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChatRoomEffectsHandler(viewModel)
    BackHandler { state.onBack() }
    ChatRoomView(state = state, modifier = Modifier.fillMaxSize())
}
