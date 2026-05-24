package co.electriccoin.zcash.ui.screen.chat.common

import co.electriccoin.zcash.spackle.Twig
import kotlin.coroutines.cancellation.CancellationException

internal inline fun runChatCall(message: String, block: () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught") e: Exception
    ) {
        Twig.warn(e) { message }
    }
}
