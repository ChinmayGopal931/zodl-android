package xyz.justzappit.evm.crypto

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class Encrypted(
    val iv: String,
    val ephemPublicKey: String,
    val ciphertext: String,
    val mac: String,
)

object Ecies {
    private const val MIN_CIPHER_BYTES = 82
    private const val IV_BYTES = 16
    private const val COMPRESSED_PUBKEY_BYTES = 33
    private const val MAC_BYTES = 32
    private const val FIELD_BYTES = 32

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    private val curve: ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
    private val random = SecureRandom()

    fun encryptWithPublicKey(publicKeyHex: String, message: String): Encrypted {
        val pubBytes = ("04$publicKeyHex").hexToBytes()
        val pubPoint = curve.curve.decodePoint(pubBytes)

        val ephemPriv = generateScalar()
        val ephemPubPoint = curve.g.multiply(ephemPriv).normalize()
        val ephemPubUncompressed = ephemPubPoint.getEncoded(false)

        val sharedSecret =
            pubPoint
                .multiply(ephemPriv)
                .normalize()
                .affineXCoord.encoded
        val (encKey, macKey) = deriveKeys(sharedSecret)

        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val plaintext = message.toByteArray(Charsets.UTF_8)

        val ciphertext =
            Cipher
                .getInstance("AES/CBC/PKCS5Padding")
                .apply {
                    init(Cipher.ENCRYPT_MODE, SecretKeySpec(encKey, "AES"), IvParameterSpec(iv))
                }.doFinal(plaintext)

        val mac = hmacSha256(macKey, iv + ephemPubUncompressed + ciphertext)

        return Encrypted(
            iv = iv.toHex(),
            ephemPublicKey = ephemPubUncompressed.toHex(),
            ciphertext = ciphertext.toHex(),
            mac = mac.toHex(),
        )
    }

    fun decryptWithPrivateKey(privateKeyHex: String, encrypted: Encrypted): String {
        val priv = BigInteger(1, privateKeyHex.removePrefix("0x").hexToBytes())
        val ephemPubBytes = encrypted.ephemPublicKey.hexToBytes()
        val iv = encrypted.iv.hexToBytes()
        val ciphertext = encrypted.ciphertext.hexToBytes()
        val macBytes = encrypted.mac.hexToBytes()

        val ephemPubPoint = curve.curve.decodePoint(ephemPubBytes)
        val sharedSecret =
            ephemPubPoint
                .multiply(priv)
                .normalize()
                .affineXCoord.encoded
        val (encKey, macKey) = deriveKeys(sharedSecret)

        val computedMac = hmacSha256(macKey, iv + ephemPubBytes + ciphertext)
        check(MessageDigest.isEqual(computedMac, macBytes)) {
            "MAC mismatch — ciphertext may be corrupted or tampered with"
        }

        val plaintext =
            Cipher
                .getInstance("AES/CBC/PKCS5Padding")
                .apply {
                    init(Cipher.DECRYPT_MODE, SecretKeySpec(encKey, "AES"), IvParameterSpec(iv))
                }.doFinal(ciphertext)

        return plaintext.toString(Charsets.UTF_8)
    }

    fun cipherStringify(encrypted: Encrypted): String {
        val ephemPubPoint = curve.curve.decodePoint(encrypted.ephemPublicKey.hexToBytes())
        val compressed = ephemPubPoint.getEncoded(true)
        val out =
            encrypted.iv.hexToBytes() +
                compressed +
                encrypted.mac.hexToBytes() +
                encrypted.ciphertext.hexToBytes()
        return out.toHex()
    }

    fun cipherParse(s: String): Encrypted {
        val buf = s.hexToBytes()
        require(buf.size >= MIN_CIPHER_BYTES) {
            "cipherParse: input too short (${buf.size} bytes, need at least $MIN_CIPHER_BYTES)"
        }
        val ivEnd = IV_BYTES
        val compEnd = ivEnd + COMPRESSED_PUBKEY_BYTES
        val macEnd = compEnd + MAC_BYTES

        val iv = buf.copyOfRange(0, ivEnd)
        val compressed = buf.copyOfRange(ivEnd, compEnd)
        val mac = buf.copyOfRange(compEnd, macEnd)
        val ciphertext = buf.copyOfRange(macEnd, buf.size)

        val uncompressed = curve.curve.decodePoint(compressed).getEncoded(false)

        return Encrypted(
            iv = iv.toHex(),
            ephemPublicKey = uncompressed.toHex(),
            ciphertext = ciphertext.toHex(),
            mac = mac.toHex(),
        )
    }

    private fun generateScalar(): BigInteger {
        while (true) {
            val bytes = ByteArray(FIELD_BYTES).also { random.nextBytes(it) }
            val k = BigInteger(1, bytes)
            if (k > BigInteger.ZERO && k < curve.n) return k
        }
    }

    /**
     * Key derivation deliberately uses plain SHA-512(sharedSecret) instead of HKDF, matching the
     * [eth-crypto](https://github.com/pubkey/eth-crypto/blob/master/src/encrypt-with-public-key.ts)
     * convention that p2p.me's relays interoperate with. Replacing this with NIST SP 800-56 / SEC1
     * standard ECIES (HKDF + info string) will break wire-format compatibility with every existing
     * counterparty — do NOT "fix" this without coordinating a hard fork of the relay protocol.
     */
    private fun deriveKeys(sharedSecret: ByteArray): Pair<ByteArray, ByteArray> {
        val hash = MessageDigest.getInstance("SHA-512").digest(sharedSecret)
        return hash.copyOfRange(0, FIELD_BYTES) to hash.copyOfRange(FIELD_BYTES, hash.size)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data)
}
