package co.electriccoin.zcash.ui.screen.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import co.electriccoin.zcash.ui.screen.onboarding.view.RestoreInProgressScreen

/**
 * Spinner shown while [ChatBootstrap] derives the messaging identity from the
 * persisted wallet seed (after the username step). Surfaces a derive failure with
 * a retry, gated on `isDeriving` so a spammed retry can't queue redundant PBKDF2
 * round-trips. Shared by the create and restore onboarding flows so both render the
 * identical Part-2 progress UI.
 */
@Composable
internal fun DerivingIdentityScreen(
    step: Int,
    chatBootstrap: ChatBootstrap,
) {
    val chatIdentityFailed by chatBootstrap.chatIdentityFailed.collectAsStateWithLifecycle()
    val isDeriving by chatBootstrap.isDeriving.collectAsStateWithLifecycle()

    val errorMessage =
        if (chatIdentityFailed) stringResource(R.string.chat_identity_setup_error_wallet_derive_failed) else null
    val onRetry: (() -> Unit)? =
        if (chatIdentityFailed && !isDeriving) ({ chatBootstrap.retry() }) else null
    RestoreInProgressScreen(step = step, errorMessage = errorMessage, onRetry = onRetry)
}
