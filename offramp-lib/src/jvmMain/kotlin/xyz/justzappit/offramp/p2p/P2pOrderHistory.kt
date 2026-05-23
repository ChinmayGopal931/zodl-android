package xyz.justzappit.offramp.p2p

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import xyz.justzappit.evm.crypto.Ecies
import xyz.justzappit.evm.types.Address
import java.math.BigInteger

/**
 * Display-ready row for the P2P transactions screen. Fields all derive from the subgraph snapshot
 * + ECIES decryption against the persisted relay identity; no contract reads are required for the
 * list view (the subgraph already indexes `actualUsdc/FiatAmount` and the encrypted UPIs).
 *
 *  - [recipientUpiPlain]: decrypted `encUpi`. For PAY/SELL this is the user-supplied destination
 *    (full URI for PAY, bare VPA for SELL); for BUY it's the merchant's pay-to VPA.
 *  - [merchantUpiPlain]: decrypted `encMerchantUpi`. Populated only after the merchant calls
 *    completeOrder on SELL/PAY (empty for BUY and for orders that haven't completed).
 */
data class P2pOrderHistoryItem(
    val orderId: BigInteger,
    val orderType: OrderType,
    val status: OrderStatus,
    val usdcAmount: Usdc6,
    val fiatAmount: Usdc6,
    val currencyHex: String,
    val placedAtEpochSeconds: Long?,
    val completedAtEpochSeconds: Long?,
    val cancelledAtEpochSeconds: Long?,
    val acceptedMerchantAddress: Address?,
    val recipientUpiPlain: String?,
    val merchantUpiPlain: String?,
)

/**
 * Paginates the subgraph and decrypts the per-order UPI ciphertexts using the persisted relay
 * identity. Stateless and idempotent — the screen VM owns the fetch lifecycle.
 *
 * Strategy: one subgraph round-trip per page (up to [PAGE_SIZE] rows), N parallel CPU-only ECIES
 * decryptions per page. No on-chain reads for the list view — `actualUsdc/FiatAmount` are already
 * indexed by the subgraph.
 */
class P2pOrderHistorySource(
    private val subgraph: SubgraphClient,
    private val relayIdentityStore: RelayIdentityStore,
) {
    suspend fun fetchAll(userAddress: Address, maxOrders: Int = MAX_ORDERS): List<P2pOrderHistoryItem> {
        val relay = relayIdentityStore.get()
        val snapshots = paginateUserOrders(userAddress, maxOrders)
        return coroutineScope {
            snapshots.map { snapshot -> async { decryptItem(snapshot, relay) } }.awaitAll()
        }
    }

    private suspend fun paginateUserOrders(userAddress: Address, maxOrders: Int): List<OrderSnapshot> {
        val out = mutableListOf<OrderSnapshot>()
        var skip = 0
        while (out.size < maxOrders) {
            val pageSize = minOf(PAGE_SIZE, maxOrders - out.size)
            val rows = subgraph.ordersForUser(userAddress.lowercaseHex, first = pageSize, skip = skip)
            if (rows.isEmpty()) break
            out += rows.map { SubgraphOrderParser.parse(it) }
            if (rows.size < pageSize) break
            skip += pageSize
        }
        return out
    }

    private fun decryptItem(snapshot: OrderSnapshot, relay: RelayIdentity?): P2pOrderHistoryItem {
        val recipientUpi = decryptUpi(snapshot.encryptedUserUpi, relay)
        val merchantUpi = decryptUpi(snapshot.encryptedMerchantUpi, relay)
        // The subgraph leaves actualUsdcAmount/actualFiatAmount null until the merchant completes
        // the order; pre-completion the placed amounts are still the right thing to show.
        return P2pOrderHistoryItem(
            orderId = snapshot.orderId,
            orderType = snapshot.orderType,
            status = snapshot.status,
            usdcAmount = snapshot.actualUsdcAmount ?: snapshot.usdcAmount,
            fiatAmount = snapshot.actualFiatAmount ?: snapshot.fiatAmount,
            currencyHex = snapshot.currencyHex,
            placedAtEpochSeconds = snapshot.placedAtEpochSeconds,
            completedAtEpochSeconds = snapshot.completedAtEpochSeconds,
            cancelledAtEpochSeconds = snapshot.cancelledAtEpochSeconds,
            acceptedMerchantAddress = snapshot.acceptedMerchantAddress,
            recipientUpiPlain = recipientUpi,
            merchantUpiPlain = merchantUpi,
        )
    }

    private fun decryptUpi(cipherHex: String, relay: RelayIdentity?): String? {
        if (cipherHex.isBlank() || relay == null) return null
        return runCatching {
            Ecies.decryptWithPrivateKey(relay.privateKeyHex, Ecies.cipherParse(cipherHex))
        }.getOrNull()
    }

    companion object {
        private const val PAGE_SIZE = 100
        private const val MAX_ORDERS = 500
    }
}

/**
 * If [plain] is a `upi://pay?…` URI, returns the `pa=` VPA; otherwise returns the input verbatim
 * (the SELL flow seals a bare VPA into encUpi, not a URI). Thin delegate over [UpiQrParser.extractPa]
 * to keep a single canonical UPI parser.
 */
fun extractUpiVpa(plain: String): String = UpiQrParser.extractPa(plain)
