package xyz.justzappit.evm.signer

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger

data class EcdsaSignature(val r: BigInteger, val s: BigInteger, val yParity: Byte)

object EcdsaSigner {
    private val curve = ECNamedCurveTable.getParameterSpec("secp256k1")
    private val domain = ECDomainParameters(curve.curve, curve.g, curve.n, curve.h)
    private val halfN: BigInteger = curve.n.shiftRight(1)
    private const val HASH_LEN = 32
    private const val FIELD_BYTES = 32

    fun sign(messageHash: ByteArray, privateKey: BigInteger): EcdsaSignature {
        require(messageHash.size == HASH_LEN) { "messageHash must be $HASH_LEN bytes" }
        require(privateKey > BigInteger.ZERO && privateKey < curve.n) { "private key out of range" }

        val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        signer.init(true, ECPrivateKeyParameters(privateKey, domain))
        val sig = signer.generateSignature(messageHash)
        val r = sig[0]
        val s = if (sig[1] > halfN) curve.n.subtract(sig[1]) else sig[1]

        val expectedPub = curve.g.multiply(privateKey).normalize()
        for (recId in 0..1) {
            val recovered = recoverPublicKey(recId, r, s, messageHash) ?: continue
            if (recovered.affineXCoord.toBigInteger() == expectedPub.affineXCoord.toBigInteger() &&
                recovered.affineYCoord.toBigInteger() == expectedPub.affineYCoord.toBigInteger()
            ) {
                return EcdsaSignature(r, s, recId.toByte())
            }
        }
        error("Failed to derive recovery ID — should not happen for a valid signature")
    }

    fun recoverPublicKey(recId: Int, r: BigInteger, s: BigInteger, messageHash: ByteArray): ECPoint? {
        require(recId == 0 || recId == 1) { "recId must be 0 or 1" }
        if (r.signum() <= 0 || s.signum() <= 0 || r >= curve.n || s >= curve.n) return null
        if (r >= curve.curve.field.characteristic) return null

        val xBytes = padTo32(r.toByteArray())
        val compressed = byteArrayOf((0x02 + recId).toByte()) + xBytes
        val rPoint = try {
            curve.curve.decodePoint(compressed)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val e = BigInteger(1, messageHash)
        val rInv = r.modInverse(curve.n)
        val negE = curve.n.subtract(e.mod(curve.n))
        val q = rPoint.multiply(s).add(curve.g.multiply(negE)).multiply(rInv).normalize()
        if (q.isInfinity) return null
        return q
    }

    private fun padTo32(b: ByteArray): ByteArray = when {
        b.size == FIELD_BYTES -> b
        b.size > FIELD_BYTES -> b.copyOfRange(b.size - FIELD_BYTES, b.size)
        else -> ByteArray(FIELD_BYTES).also { System.arraycopy(b, 0, it, FIELD_BYTES - b.size, b.size) }
    }
}
