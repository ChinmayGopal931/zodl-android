@file:Suppress("ktlint:standard:filename")

package co.electriccoin.zcash.ui.screen.chat.newconv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.screen.chat.view.NewConversationView
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun NewConversationScreen() {
    val viewModel = koinViewModel<NewConversationVM>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val currentState = state
    if (currentState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    BackHandler { currentState.onBack() }
    NewConversationView(state = currentState, modifier = Modifier.fillMaxSize())
}
