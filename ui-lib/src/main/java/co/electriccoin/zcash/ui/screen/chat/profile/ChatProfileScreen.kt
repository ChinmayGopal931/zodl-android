@file:Suppress("ktlint:standard:filename")

package co.electriccoin.zcash.ui.screen.chat.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.screen.chat.view.ChatProfileView
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun ChatProfileScreen() {
    val viewModel = koinViewModel<ChatProfileVM>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val currentState = state
    if (currentState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ZappTheme.colors.accent)
        }
        return
    }

    BackHandler { currentState.onBack() }
    ChatProfileView(state = currentState, modifier = Modifier.fillMaxSize())
}
