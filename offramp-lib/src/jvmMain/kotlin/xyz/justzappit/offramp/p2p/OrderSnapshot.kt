package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.types.Address
import java.math.BigInteger

data class OrderSnapshot(
    val orderId: BigInteger,
    val status: OrderStatus,
    val orderType: OrderType,
    val circleId: BigInteger,
    val userAddress: Address,
    val usdcAmount: BigInteger,
    val fiatAmount: BigInteger,
    val currencyHex: String,
    val acceptedMerchantAddress: Address?,
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
        acceptedMerchantAddress != null &&
        merchantPubKey.isNotBlank()
}
