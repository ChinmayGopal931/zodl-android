package xyz.justzappit.evm.hd

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECParameterSpec
import xyz.justzappit.evm.abi.keccak256
import xyz.justzappit.evm.signer.EcdsaSignature
import xyz.justzappit.evm.signer.EcdsaSigner
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.util.padLeftToWord
import java.math.BigInteger
import java.nio.CharBuffer
import java.text.Normalizer
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * A secp256k1 EVM key.
 *
 * The 32-byte private key is held as a private field — callers sign through
 * [signRecoverable] rather than reaching for the raw bytes, so the scalar
 * doesn't leak across module boundaries (and into any incidental logging or
 * serialisation along the way). [exportPrivateKeyBytes] is the controlled
 * escape hatch for legitimate persistence paths (e.g. the relay identity);
 * it returns a defensive copy so the canonical buffer can still be zeroised
 * via [zeroize] without affecting persisted hex.
 */
class EvmKey internal constructor(
    internal val privateKey: ByteArray,
    val publicKey: ByteArray,
    val address: Address,
) {
    /** Sign a 32-byte message hash with this key's secp256k1 scalar. */
    fun signRecoverable(messageHash: ByteArray): EcdsaSignature =
        EcdsaSigner.sign(messageHash, BigInteger(1, privateKey))

    /** Defensive copy of the raw private key. Caller owns the copy; call [ByteArray.fill] when done. */
    fun exportPrivateKeyBytes(): ByteArray = privateKey.copyOf()

    /**
     * Overwrite the canonical private-key buffer with zeros. Defence in depth: a
     * [BigInteger] derived from these bytes (e.g. inside the signer) carries its
     * own internal arrays that this can't reach.
     */
    fun zeroize() {
        privateKey.fill(0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EvmKey) return false
        return privateKey.contentEquals(other.privateKey) &&
            publicKey.contentEquals(other.publicKey) &&
            address == other.address
    }

    // hashCode intentionally omits `privateKey` so the secret scalar isn't distributed across
    // hash-table buckets if a caller ever drops EvmKey into a HashMap/HashSet. The (publicKey,
    // address) pair already uniquely identifies a valid secp256k1 keypair.
    override fun hashCode(): Int = 31 * publicKey.contentHashCode() + address.hashCode()

    override fun toString(): String = "EvmKey(address=$address)"
}

@Suppress("TooManyFunctions")
object EvmKeyDerivation {
    private const val HARDENED_BIT: Int = 0x80000000.toInt()
    private const val PBKDF2_ITERATIONS = 2048
    private const val SEED_BITS = 512
    private const val FIELD_BYTES = 32
    private const val ADDRESS_BYTES = 20
    private const val WIPE_CHAR: Char = '\u0000'

    private val curve: ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")

    /**
     * Derives the BIP-44 EVM key at m/44'/60'/0'/0/[accountIndex]. Primary entry point.
     *
     * [mnemonic] is not zeroed here — caller-owned-wipe contract, so the same array can be reused
     * across accounts before the caller clears it. PBEKeySpec's internal copy IS cleared via
     * `clearPassword()`. The transient `String` from `Normalizer.normalize` is unavoidable (JDK
     * API) but dies with the stack frame.
     */
    fun derive(mnemonic: CharArray, accountIndex: Int = 0, passphrase: String = ""): EvmKey {
        require(accountIndex >= 0) { "accountIndex must be non-negative" }
        val seed = mnemonicToSeed(mnemonic, passphrase)
        val master = masterFromSeed(seed)
        val derived =
            listOf(
                44 or HARDENED_BIT,
                60 or HARDENED_BIT,
                0 or HARDENED_BIT,
                0,
                accountIndex,
            ).fold(master) { parent, index -> ckdPrivWithRetry(parent, index) }
        return fromPrivateKey(derived.priv)
    }

    /** Convenience overload for test vectors / dev tools. Production callers should use the [CharArray] overload. */
    fun derive(mnemonic: String, accountIndex: Int = 0, passphrase: String = ""): EvmKey =
        derive(mnemonic.toCharArray(), accountIndex, passphrase)

    fun fromPrivateKey(privBytes: ByteArray): EvmKey {
        require(privBytes.size == FIELD_BYTES) { "private key must be 32 bytes" }
        val priv = BigInteger(1, privBytes)
        require(priv > BigInteger.ZERO && priv < curve.n) { "private key out of range" }
        val point = curve.g.multiply(priv).normalize()
        val pubXY = point.affineXCoord.encoded + point.affineYCoord.encoded
        return EvmKey(
            privateKey = privBytes.copyOf(),
            publicKey = pubXY,
            address = addressFromPub(pubXY),
        )
    }

    private data class ExtKey(
        val priv: ByteArray,
        val chainCode: ByteArray
    )

    private fun mnemonicToSeed(mnemonic: CharArray, passphrase: String): ByteArray {
        // String.trim() semantics over a CharBuffer view so we don't allocate a copy of the input.
        var start = 0
        var endExclusive = mnemonic.size
        while (start < endExclusive && mnemonic[start].isWhitespace()) start++
        while (endExclusive > start && mnemonic[endExclusive - 1].isWhitespace()) endExclusive--

        val normalizedMnemonic =
            Normalizer.normalize(CharBuffer.wrap(mnemonic, start, endExclusive - start), Normalizer.Form.NFKD)
        val normalizedMnemonicChars = normalizedMnemonic.toCharArray()
        val normalizedPassBytes =
            Normalizer.normalize("mnemonic$passphrase", Normalizer.Form.NFKD).toByteArray(Charsets.UTF_8)

        val spec = PBEKeySpec(normalizedMnemonicChars, normalizedPassBytes, PBKDF2_ITERATIONS, SEED_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            normalizedMnemonicChars.fill(WIPE_CHAR)
            normalizedPassBytes.fill(0)
        }
    }

    private fun masterFromSeed(seed: ByteArray): ExtKey {
        val i = hmacSha512("Bitcoin seed".toByteArray(Charsets.UTF_8), seed)
        return ExtKey(priv = i.copyOfRange(0, FIELD_BYTES), chainCode = i.copyOfRange(FIELD_BYTES, i.size))
    }

    /**
     * BIP-32 §"Private parent → private child" requires that if the candidate IL is ≥ n or the
     * resulting child key is zero, we MUST advance to the next sibling index and retry. Both
     * conditions have probability < 2^-127, but skipping the retry would diverge from every other
     * BIP-32 implementation on that one-in-2^127 input and produce a different address than the
     * user's other wallets.
     */
    private fun ckdPrivWithRetry(parent: ExtKey, startIndex: Int): ExtKey {
        var index = startIndex
        while (true) {
            val candidate = ckdPrivOnce(parent, index)
            if (candidate != null) return candidate
            // Hardened bits never collide with their non-hardened neighbours; incrementing by 1
            // stays within the same range.
            val next = index + 1
            check(next != startIndex) { "BIP-32 ckdPriv: exhausted all 2^32 child indices" }
            index = next
        }
    }

    private fun ckdPrivOnce(parent: ExtKey, index: Int): ExtKey? {
        val hardened = (index.toLong() and 0xffff_ffffL) >= 0x8000_0000L
        val data =
            if (hardened) {
                byteArrayOf(0x00) + parent.priv + intToBytes(index)
            } else {
                compressedPub(parent.priv) + intToBytes(index)
            }
        val i = hmacSha512(parent.chainCode, data)
        val il = i.copyOfRange(0, FIELD_BYTES)
        val ir = i.copyOfRange(FIELD_BYTES, i.size)
        val ilNum = BigInteger(1, il)
        if (ilNum >= curve.n) return null
        val parentNum = BigInteger(1, parent.priv)
        val childNum = (ilNum + parentNum).mod(curve.n)
        if (childNum == BigInteger.ZERO) return null
        return ExtKey(priv = childNum.toByteArray().padLeftToWord(), chainCode = ir)
    }

    private fun compressedPub(privBytes: ByteArray): ByteArray =
        curve.g
            .multiply(BigInteger(1, privBytes))
            .normalize()
            .getEncoded(true)

    private fun addressFromPub(pubXY: ByteArray): Address {
        val hash = keccak256(pubXY)
        return Address.fromBytes(hash.copyOfRange(hash.size - ADDRESS_BYTES, hash.size))
    }

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA512").apply { init(SecretKeySpec(key, "HmacSHA512")) }.doFinal(data)

    private fun intToBytes(i: Int): ByteArray =
        byteArrayOf(
            (i ushr 24 and 0xff).toByte(),
            (i ushr 16 and 0xff).toByte(),
            (i ushr 8 and 0xff).toByte(),
            (i and 0xff).toByte(),
        )
}
