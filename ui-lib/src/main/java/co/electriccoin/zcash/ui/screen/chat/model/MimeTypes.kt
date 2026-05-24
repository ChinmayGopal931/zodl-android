package co.electriccoin.zcash.ui.screen.chat.model

/**
 * MIME types used as protocol markers in chat messages. The string values are part of the
 * on-wire format — changing them breaks compatibility with peers running older clients.
 */
object MimeTypes {
    const val IMAGE_PREFIX = "image/"
    const val VIDEO_PREFIX = "video/"
    const val IMAGE_JPEG = "image/jpeg"
    const val GIF = "image/gif"
    const val LOCATION = "application/location"
    const val WALLET_ADDRESS = "application/wallet-address"
}
