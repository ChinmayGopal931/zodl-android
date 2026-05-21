package xyz.justzappit.offramp.p2p

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RelayIdentityTest {
    @Test
    fun `generate produces valid eth-crypto formatted keys`() {
        val id = RelayIdentities.generate()
        assertTrue(id.privateKeyHex.startsWith("0x"))
        assertEquals(66, id.privateKeyHex.length, "private key hex must be 0x + 64 chars")
        assertEquals(128, id.publicKeyHex.length, "public key must be 128 chars (X || Y, no 04)")
        assertTrue(id.publicKeyHex.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `successive identities are different`() {
        val a = RelayIdentities.generate()
        val b = RelayIdentities.generate()
        assertNotEquals(a.privateKeyHex, b.privateKeyHex)
        assertNotEquals(a.publicKeyHex, b.publicKeyHex)
    }
}
