package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.TxHash
import java.math.BigInteger

data class OrderSnapshot(
    val orderId: BigInteger,
    val status: OrderStatus,
    val orderType: OrderType,
    val circleId: BigInteger,
    val userAddress: Address,
    val usdcAmount: Usdc6,
    val fiatAmount: Usdc6,
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
    val actualUsdcAmount: Usdc6?,
    val actualFiatAmount: Usdc6?,
    val placedTxHash: TxHash?,
    val placedAtBlockNumber: Long?,
    val source: Source,
) {
    enum class Source { Subgraph, OnChain }

    val isAccepted: Boolean get() = status.onChain >= OrderStatus.ACCEPTED.onChain &&
        acceptedMerchantAddress != null &&
        merchantPubKey.isNotBlank()
}
