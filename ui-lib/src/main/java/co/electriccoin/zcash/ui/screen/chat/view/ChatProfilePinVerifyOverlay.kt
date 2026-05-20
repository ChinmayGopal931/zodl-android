package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.runtime.Composable
import co.electriccoin.zcash.ui.screen.chat.profile.ChatProfilePinVerifyState
import co.electriccoin.zcash.ui.screen.onboarding.view.PinVerifyScreen

@Composable
internal fun PinVerifyOverlay(state: ChatProfilePinVerifyState) {
    PinVerifyScreen(
        hasError = state.hasError,
        lockoutSecondsRemaining = state.lockoutSecondsRemaining,
        onPinSubmit = state.onPinSubmit,
        onCancel = state.onCancel,
    )
}
