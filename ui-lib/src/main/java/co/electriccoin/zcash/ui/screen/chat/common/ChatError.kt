package co.electriccoin.zcash.ui.screen.chat.common

import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes

sealed class ChatError {
    object CreateIdentityFailed : ChatError()

    object RestoreIdentityFailed : ChatError()

    object ExportSeedPhraseFailed : ChatError()
}

fun ChatError.toStringResource(): StringResource =
    when (this) {
        ChatError.CreateIdentityFailed -> stringRes(R.string.chat_identity_setup_error_create_failed)
        ChatError.RestoreIdentityFailed -> stringRes(R.string.chat_identity_setup_error_restore_failed)
        ChatError.ExportSeedPhraseFailed -> stringRes(R.string.chat_identity_setup_error_export_seed_failed)
    }
