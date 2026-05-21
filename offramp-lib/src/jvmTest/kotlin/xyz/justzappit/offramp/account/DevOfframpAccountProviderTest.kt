package xyz.justzappit.offramp.account

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevOfframpAccountProviderTest {

    @Test
    fun `dev EOA derives a deterministic mixed-case checksummed address`() = runTest {
        val key = DevOfframpAccountProvider.nextOfframpAccount()
        assertTrue(key.address.checksumHex.startsWith("0x"))
        assertEquals(42, key.address.checksumHex.length)
        assertEquals(DevOfframpAccountProvider.address, key.address)
        println("DEV STATIC EOA ADDRESS (Sepolia): ${key.address}")
    }

    @Test
    fun `dev EOA is stable across calls`() = runTest {
        val a = DevOfframpAccountProvider.nextOfframpAccount()
        val b = DevOfframpAccountProvider.nextOfframpAccount()
        assertEquals(a.address, b.address)
    }
}
