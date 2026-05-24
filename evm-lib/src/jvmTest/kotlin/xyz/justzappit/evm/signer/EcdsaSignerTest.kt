package xyz.justzappit.evm.signer

import org.bouncycastle.jce.ECNamedCurveTable
import xyz.justzappit.evm.hd.EvmKeyDerivation
import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EcdsaSignerTest {
    private val curve = ECNamedCurveTable.getParameterSpec("secp256k1")

    @Test
    fun `signature is deterministic per RFC 6979`() {
        val priv = BigInteger("1ab42cc412b618bdea3a599e3c9bae199ebf030895b039e9db1e30dafb12b727", 16)
        val hash = ByteArray(32) { 0x11 }
        val a = EcdsaSigner.sign(hash, priv)
        val b = EcdsaSigner.sign(hash, priv)
        assertEquals(a.r, b.r)
        assertEquals(a.s, b.s)
        assertEquals(a.yParity, b.yParity)
    }

    @Test
    fun `signature is low-S (less than n div 2)`() {
        val priv = BigInteger("1ab42cc412b618bdea3a599e3c9bae199ebf030895b039e9db1e30dafb12b727", 16)
        val halfN = curve.n.shiftRight(1)
        val sig = EcdsaSigner.sign(ByteArray(32) { 0x22 }, priv)
        assertTrue(sig.s <= halfN, "s=${sig.s.toString(16)} > n/2=${halfN.toString(16)}")
    }

    @Test
    fun `sign then recover yields the signer's public point`() {
        val derived = EvmKeyDerivation.derive(MNEMONIC, accountIndex = 0)
        val priv = BigInteger(1, derived.privateKey)
        val hash = ByteArray(32) { 0x33 }

        val sig = EcdsaSigner.sign(hash, priv)
        val recovered = EcdsaSigner.recoverPublicKey(sig.yParity.toInt(), sig.r, sig.s, hash)
        assertNotNull(recovered)

        val expected = curve.g.multiply(priv).normalize()
        assertEquals(expected.affineXCoord.toBigInteger(), recovered.affineXCoord.toBigInteger())
        assertEquals(expected.affineYCoord.toBigInteger(), recovered.affineYCoord.toBigInteger())
    }

    @Test
    fun `recovered point matches the EOA address`() {
        val derived = EvmKeyDerivation.derive(MNEMONIC, accountIndex = 0)
        val priv = BigInteger(1, derived.privateKey)
        val hash = "abcd".padEnd(64, '0').hexToBytes()

        val sig = EcdsaSigner.sign(hash, priv)
        val recovered = EcdsaSigner.recoverPublicKey(sig.yParity.toInt(), sig.r, sig.s, hash)!!
        val pubXY = recovered.affineXCoord.encoded + recovered.affineYCoord.encoded
        val rebuilt = EvmKeyDerivation.fromPrivateKey(derived.privateKey)
        assertTrue(pubXY.contentEquals(rebuilt.publicKey))
    }

    @Test
    fun `wrong recovery id yields a different point`() {
        val priv = BigInteger("ab".repeat(32), 16).mod(curve.n)
        val hash = ByteArray(32) { 0x44 }
        val sig = EcdsaSigner.sign(hash, priv)
        val correct = EcdsaSigner.recoverPublicKey(sig.yParity.toInt(), sig.r, sig.s, hash)!!
        val wrong = EcdsaSigner.recoverPublicKey(1 - sig.yParity.toInt(), sig.r, sig.s, hash)
        if (wrong != null) {
            assertTrue(
                correct.affineYCoord.toBigInteger() != wrong.affineYCoord.toBigInteger(),
                "Recovery with wrong recId returned the same point",
            )
        }
    }

    @Test
    fun `recovery returns null for out-of-range r or s`() {
        val hash = ByteArray(32)
        assertNull(EcdsaSigner.recoverPublicKey(0, BigInteger.ZERO, BigInteger.ONE, hash))
        assertNull(EcdsaSigner.recoverPublicKey(0, BigInteger.ONE, BigInteger.ZERO, hash))
        assertNull(EcdsaSigner.recoverPublicKey(0, curve.n, BigInteger.ONE, hash))
    }

    companion object {
        const val MNEMONIC =
            "abandon abandon abandon abandon abandon abandon " +
                "abandon abandon abandon abandon abandon about"
    }
}
