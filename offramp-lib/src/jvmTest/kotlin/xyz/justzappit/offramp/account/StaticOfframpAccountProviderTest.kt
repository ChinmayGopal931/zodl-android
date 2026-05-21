package xyz.justzappit.offramp.account

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StaticOfframpAccountProviderTest {
    @Test
    fun `static provider returns the canonical account-0 address`() = runTest {
        val source = SeedPhraseSource { MNEMONIC }
        val provider = StaticOfframpAccountProvider(source, fixedAccountIndex = 0)
        val key = provider.nextOfframpAccount()
        assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", key.address)
    }

    @Test
    fun `repeated calls return the same address (dev-mode static EOA)`() = runTest {
        val source = SeedPhraseSource { MNEMONIC }
        val provider = StaticOfframpAccountProvider(source, fixedAccountIndex = 0)
        val first = provider.nextOfframpAccount()
        val second = provider.nextOfframpAccount()
        assertEquals(first.address, second.address)
    }

    @Test
    fun `different fixed index yields different address`() = runTest {
        val source = SeedPhraseSource { MNEMONIC }
        val a = StaticOfframpAccountProvider(source, fixedAccountIndex = 0).nextOfframpAccount()
        val b = StaticOfframpAccountProvider(source, fixedAccountIndex = 1).nextOfframpAccount()
        assertNotEquals(a.address, b.address)
    }

    companion object {
        const val MNEMONIC =
            "abandon abandon abandon abandon abandon abandon " +
                "abandon abandon abandon abandon abandon about"
    }
}
