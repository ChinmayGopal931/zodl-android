package co.electriccoin.zcash.ui.common.util

import java.util.Locale

/**
 * peer.xyz availability + URL builder for the on/off-ramp flows.
 *
 * The fork ships a curated peer.xyz integration; this object centralizes the
 * URL templates and the country-code gating so the rest of the codebase
 * doesn't need to know about either. Remove a country code from
 * [UNSUPPORTED_REGIONS] once its ZKP2P provider is live on peer.xyz (see
 * `zkp2p-providers/`).
 */
object PeerXyzUtil {
    private val UNSUPPORTED_REGIONS = setOf("BR", "IN", "PH", "KE", "TZ", "UG", "GH")

    const val JUSTZAPPIT_URL = "https://justzappit.xyz/directory"
    private const val PEER_XYZ_BUY_URL = "https://www.peer.xyz/"
    private const val PEER_XYZ_SELL_URL = "https://www.peer.xyz/sell"
    private const val REFERRER = "Zapp"

    fun isPeerAvailable(): Boolean = Locale.getDefault().country !in UNSUPPORTED_REGIONS

    fun getOnrampUrl(unifiedAddress: String): String =
        if (isPeerAvailable()) {
            "$PEER_XYZ_BUY_URL?recipientAddress=$unifiedAddress&toToken=ZEC&referrer=$REFERRER"
        } else {
            JUSTZAPPIT_URL
        }

    fun getOfframpUrl(zecAmount: String): String =
        if (isPeerAvailable()) {
            "$PEER_XYZ_SELL_URL?amount=$zecAmount&referrer=$REFERRER"
        } else {
            JUSTZAPPIT_URL
        }
}
