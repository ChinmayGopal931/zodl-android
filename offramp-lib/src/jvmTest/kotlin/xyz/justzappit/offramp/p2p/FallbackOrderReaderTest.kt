package xyz.justzappit.offramp.p2p

import kotlinx.coroutines.test.runTest
import xyz.justzappit.evm.types.Address
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FallbackOrderReaderTest {

    @Test
    fun `returns primary's result when primary succeeds`() = runTest {
        val primaryResult = snapshot(OrderSnapshot.Source.Subgraph)
        val fallback = neverCalled()
        val composite = FallbackOrderReader(
            primary = constant(primaryResult),
            fallback = fallback,
        )
        assertSame(primaryResult, composite.fetchOrder(BigInteger.ONE))
    }

    @Test
    fun `falls through to secondary when primary throws`() = runTest {
        val secondaryResult = snapshot(OrderSnapshot.Source.OnChain)
        val warnings = mutableListOf<String>()
        val composite = FallbackOrderReader(
            primary = throwing(IllegalStateException("subgraph 503")),
            fallback = constant(secondaryResult),
            logger = { msg, _ -> warnings += msg },
        )
        assertSame(secondaryResult, composite.fetchOrder(BigInteger.ONE))
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("orderId=1"))
    }

    @Test
    fun `falls through when primary returns null`() = runTest {
        val secondaryResult = snapshot(OrderSnapshot.Source.OnChain)
        val composite = FallbackOrderReader(
            primary = constant(null),
            fallback = constant(secondaryResult),
        )
        assertSame(secondaryResult, composite.fetchOrder(BigInteger.ONE))
    }

    @Test
    fun `propagates secondary exception if primary also threw`() = runTest {
        val composite = FallbackOrderReader(
            primary = throwing(IllegalStateException("subgraph 503")),
            fallback = throwing(IllegalStateException("rpc 502")),
        )
        val ex = assertFailsWith<IllegalStateException> {
            composite.fetchOrder(BigInteger.ONE)
        }
        assertTrue(ex.message!!.contains("rpc 502"))
    }

    private fun snapshot(source: OrderSnapshot.Source) = OrderSnapshot(
        orderId = BigInteger.ONE,
        status = OrderStatus.PLACED,
        orderType = OrderType.PAY,
        circleId = BigInteger.ONE,
        userAddress = Address.ZERO,
        usdcAmount = BigInteger.ZERO,
        fiatAmount = BigInteger.ZERO,
        currencyHex = "0x" + "00".repeat(32),
        acceptedMerchantAddress = null,
        merchantPubKey = "",
        encryptedUserUpi = "",
        encryptedMerchantUpi = "",
        placedAtEpochSeconds = 1L,
        acceptedAtEpochSeconds = null,
        paidAtEpochSeconds = null,
        completedAtEpochSeconds = null,
        cancelledAtEpochSeconds = null,
        actualUsdcAmount = null,
        actualFiatAmount = null,
        placedTxHash = null,
        placedAtBlockNumber = null,
        source = source,
    )

    private fun constant(snapshot: OrderSnapshot?) = OrderReadSource { _ -> snapshot }
    private fun throwing(cause: Throwable) = OrderReadSource { _ -> throw cause }
    private fun neverCalled() = OrderReadSource { _ -> error("fallback should not be called") }

    private fun OrderReadSource(block: suspend (BigInteger) -> OrderSnapshot?) = object : OrderReadSource {
        override suspend fun fetchOrder(orderId: BigInteger): OrderSnapshot? = block(orderId)
    }
}
