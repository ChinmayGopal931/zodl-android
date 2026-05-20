@file:Suppress("ktlint:standard:filename")

package co.electriccoin.zcash.ui.screen.chat.contacts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.screen.chat.view.ChatContactsView
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun ChatContactsScreen(showBackButton: Boolean = true) {
    val viewModel = koinViewModel<ChatContactsVM>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(showBackButton) { viewModel.setShowBackButton(showBackButton) }

    val currentState = state
    if (currentState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    BackHandler { currentState.onBack() }
    ChatContactsView(state = currentState, modifier = Modifier.fillMaxSize())
}
