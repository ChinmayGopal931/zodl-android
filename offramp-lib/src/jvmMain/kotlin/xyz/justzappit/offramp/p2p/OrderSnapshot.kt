package xyz.justzappit.offramp.p2p

import java.math.BigInteger

/**
 * Strict-typed read model for a single offramp order. The union of fields the subgraph
 * and the on-chain Diamond can provide — nullable where one source can supply a value
 * and the other can't, or where the field is semantically "unset" (e.g. zero timestamp).
 *
 * Authoritative for [status], [acceptedMerchantAddress], and [merchantPubKey]. The
 * "actual*" amounts come from the SDK's `AdditionalOrderDetails` (only the subgraph
 * exposes them in a single read).
 */
data class OrderSnapshot(
    val orderId: BigInteger,
    val status: OrderStatus,
    val orderType: OrderType,
    val circleId: BigInteger,
    val userAddress: String,
    val usdcAmount: BigInteger,
    val fiatAmount: BigInteger,
    val currencyHex: String,
    val acceptedMerchantAddress: String?,
    val merchantPubKey: String,
    val encryptedUserUpi: String,
    val encryptedMerchantUpi: String,
    val placedAtEpochSeconds: Long?,
    val acceptedAtEpochSeconds: Long?,
    val paidAtEpochSeconds: Long?,
    val completedAtEpochSeconds: Long?,
    val cancelledAtEpochSeconds: Long?,
    val actualUsdcAmount: BigInteger?,
    val actualFiatAmount: BigInteger?,
    val placedTxHash: String?,
    val placedAtBlockNumber: Long?,
    val source: Source,
) {
    enum class Source { Subgraph, OnChain }

    val isAccepted: Boolean get() = status.onChain >= OrderStatus.ACCEPTED.onChain &&
        !acceptedMerchantAddress.isNullOrBlank() &&
        merchantPubKey.isNotBlank()
}
