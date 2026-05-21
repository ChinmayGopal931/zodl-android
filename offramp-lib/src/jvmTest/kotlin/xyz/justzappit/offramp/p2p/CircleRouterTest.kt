package xyz.justzappit.offramp.p2p

import kotlinx.coroutines.test.runTest
import java.math.BigInteger
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CircleRouterTest {
    private val inrCurrency = "0x494e520000000000000000000000000000000000000000000000000000000000"

    private fun circle(
        id: String,
        score: Double,
        status: String,
        active: Int = 1,
        currency: String = inrCurrency,
    ) = CircleForRouting(
        circleId = id,
        currency = currency,
        metrics = CircleMetrics(
            circleScore = score.toString(),
            circleStatus = status,
            scoreState = CircleScoreState(activeMerchantsCount = active.toString()),
        ),
    )

    @Test
    fun `circleWeight applies recovery scale for paused`() {
        val r = CircleRouter()
        assertEquals(3.0, r.circleWeight(circle("1", 10.0, "paused")), 0.0001)
    }

    @Test
    fun `circleWeight caps bootstrap by BOOTSTRAP_MAX_WEIGHT`() {
        val r = CircleRouter()
        assertEquals(25.0, r.circleWeight(circle("1", 100.0, "bootstrap")), 0.0001)
        assertEquals(5.0, r.circleWeight(circle("1", 5.0, "bootstrap")), 0.0001)
    }

    @Test
    fun `active circle weight is the raw score`() {
        val r = CircleRouter()
        assertEquals(42.0, r.circleWeight(circle("1", 42.0, "active")), 0.0001)
    }

    @Test
    fun `filterEligible drops other currencies`() {
        val r = CircleRouter()
        val list = listOf(
            circle("1", 10.0, "active", currency = inrCurrency),
            circle("2", 10.0, "active", currency = "0xdeadbeef"),
        )
        val out = r.filterEligible(list, inrCurrency)
        assertEquals(1, out.size)
        assertEquals("1", out[0].circleId)
    }

    @Test
    fun `selectCircle on empty returns null`() {
        assertNull(CircleRouter().selectCircle(emptyList()))
    }

    @Test
    fun `selectCircleForOrder returns first valid circle id`() = runTest {
        // epsilon=0 means always exploit (active-only by score).
        val router = CircleRouter(random = Random(0), epsilon = 0.0)
        val circles = listOf(
            circle("1", 100.0, "active"),
            circle("2", 1.0, "active"),
        )
        val chosen = router.selectCircleForOrder(circles, inrCurrency, validateCircle = { true })
        // Heavily-weighted #1 should win with seed 0; either way it must be a valid id.
        assertEquals(true, chosen == BigInteger.ONE || chosen == BigInteger.valueOf(2))
    }

    @Test
    fun `selectCircleForOrder retries when validation fails, then succeeds`() = runTest {
        val router = CircleRouter(random = Random(42), epsilon = 0.0)
        val circles = listOf(
            circle("1", 100.0, "active"),
            circle("2", 1.0, "active"),
        )
        var calls = 0
        val firstId = router.selectCircleForOrder(circles, inrCurrency) { id ->
            calls++
            // Reject the first attempt; accept the second.
            calls > 1
        }
        assertEquals(2, calls)
        assertEquals(true, firstId == BigInteger.ONE || firstId == BigInteger.valueOf(2))
    }

    @Test
    fun `selectCircleForOrder fails after exhausting attempts`() = runTest {
        val router = CircleRouter(random = Random(7), epsilon = 0.0, maxValidationAttempts = 2)
        val circles = listOf(circle("1", 100.0, "active"), circle("2", 1.0, "active"))
        assertFailsWith<IllegalStateException> {
            router.selectCircleForOrder(circles, inrCurrency) { false }
        }
    }

    @Test
    fun `selectCircleForOrder fails when no eligible currency match`() = runTest {
        val router = CircleRouter()
        val circles = listOf(circle("1", 10.0, "active", currency = "0xabcd"))
        assertFailsWith<IllegalStateException> {
            router.selectCircleForOrder(circles, inrCurrency) { true }
        }
    }

    @Test
    fun `epsilon = 1 explores across all statuses, not only active`() = runTest {
        val router = CircleRouter(random = Random(1), epsilon = 1.0)
        val circles = listOf(
            circle("1", 50.0, "paused"),
            circle("2", 5.0, "bootstrap"),
        )
        // Should not throw even with no active circles.
        val chosen = router.selectCircleForOrder(circles, inrCurrency) { true }
        assertEquals(true, chosen == BigInteger.ONE || chosen == BigInteger.valueOf(2))
    }
}
