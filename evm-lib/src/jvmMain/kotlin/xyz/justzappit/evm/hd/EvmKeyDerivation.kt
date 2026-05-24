package xyz.justzappit.evm.hd

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECParameterSpec
import xyz.justzappit.evm.abi.keccak256
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.util.padLeftToWord
import java.math.BigInteger
import java.text.Normalizer
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class EvmKey(
    val privateKey: ByteArray,
    val publicKey: ByteArray,
    val address: Address,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EvmKey) return false
        return privateKey.contentEquals(other.privateKey) &&
            publicKey.contentEquals(other.publicKey) &&
            address == other.address
    }

    override fun hashCode(): Int {
        var h = privateKey.contentHashCode()
        h = 31 * h + publicKey.contentHashCode()
        h = 31 * h + address.hashCode()
        return h
    }
}

object EvmKeyDerivation {
    private const val HARDENED_BIT: Int = 0x80000000.toInt()
    private const val PBKDF2_ITERATIONS = 2048
    private const val SEED_BITS = 512
    private const val FIELD_BYTES = 32
    private const val ADDRESS_BYTES = 20

    private val curve: ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")

    fun derive(mnemonic: String, accountIndex: Int = 0, passphrase: String = ""): EvmKey {
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

    private fun mnemonicToSeed(mnemonic: String, passphrase: String): ByteArray {
        val normMnemonic = Normalizer.normalize(mnemonic.trim(), Normalizer.Form.NFKD)
        val normPass = Normalizer.normalize("mnemonic$passphrase", Normalizer.Form.NFKD)
        val spec =
            PBEKeySpec(
                normMnemonic.toCharArray(),
                normPass.toByteArray(Charsets.UTF_8),
                PBKDF2_ITERATIONS,
                SEED_BITS,
            )
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).encoded
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
