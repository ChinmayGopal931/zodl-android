package xyz.justzappit.offramp.p2p

import java.math.BigInteger

interface OrderReadSource {
    suspend fun fetchOrder(orderId: BigInteger): OrderSnapshot?
}
