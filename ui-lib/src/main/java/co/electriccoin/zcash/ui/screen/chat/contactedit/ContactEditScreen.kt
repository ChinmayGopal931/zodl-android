@file:Suppress("ktlint:standard:filename")

package co.electriccoin.zcash.ui.screen.chat.contactedit

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
import co.electriccoin.zcash.ui.screen.chat.ContactEditArgs
import co.electriccoin.zcash.ui.screen.chat.view.ContactEditView
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun ContactEditScreen(args: ContactEditArgs) {
    val viewModel = koinViewModel<ContactEditVM> { parametersOf(args) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val currentState = state
    if (currentState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ZappTheme.colors.accent)
        }
        return
    }

    BackHandler { currentState.onBack() }
    ContactEditView(state = currentState, modifier = Modifier.fillMaxSize())
}
