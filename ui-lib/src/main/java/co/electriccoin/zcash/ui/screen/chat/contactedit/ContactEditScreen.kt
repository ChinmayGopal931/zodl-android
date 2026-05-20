@file:Suppress("ktlint:standard:filename")

package co.electriccoin.zcash.ui.screen.chat.contactedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.screen.chat.ContactEditArgs
import co.electriccoin.zcash.ui.screen.chat.view.ContactEditView
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun ContactEditScreen(args: ContactEditArgs) {
    val viewModel = koinViewModel<ContactEditVM> { parametersOf(args) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler { state.onBack() }
    ContactEditView(state = state, modifier = Modifier.fillMaxSize())
}
