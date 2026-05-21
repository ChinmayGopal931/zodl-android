package xyz.justzappit.evm.signer

import org.bouncycastle.crypto.digests.KeccakDigest
import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger

data class Eip1559Tx(
    val chainId: Long,
    val nonce: BigInteger,
    val maxPriorityFeePerGas: BigInteger,
    val maxFeePerGas: BigInteger,
    val gasLimit: BigInteger,
    val to: String,
    val value: BigInteger,
    val data: ByteArray,
) {
    init {
        require(to.startsWith("0x") && to.length == ADDRESS_HEX_LEN) {
            "to must be a 0x-prefixed 20-byte address, got '$to'"
        }
    }

    fun signingPayload(): ByteArray = TX_TYPE_EIP1559 + Rlp.encode(toRlpList(includeSignature = false))

    fun encodeSigned(sig: EcdsaSignature): ByteArray {
        val items = toRlpList(includeSignature = false) as RlpItem.L
        val signedItems = items.items + listOf(
            rlpInt(sig.yParity.toLong()),
            rlpInt(sig.r),
            rlpInt(sig.s),
        )
        return TX_TYPE_EIP1559 + Rlp.encode(RlpItem.L(signedItems))
    }

    fun signingHash(): ByteArray = keccak256(signingPayload())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Eip1559Tx) return false
        return chainId == other.chainId &&
            nonce == other.nonce &&
            maxPriorityFeePerGas == other.maxPriorityFeePerGas &&
            maxFeePerGas == other.maxFeePerGas &&
            gasLimit == other.gasLimit &&
            to == other.to &&
            value == other.value &&
            data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var h = chainId.hashCode()
        h = 31 * h + nonce.hashCode()
        h = 31 * h + maxPriorityFeePerGas.hashCode()
        h = 31 * h + maxFeePerGas.hashCode()
        h = 31 * h + gasLimit.hashCode()
        h = 31 * h + to.hashCode()
        h = 31 * h + value.hashCode()
        h = 31 * h + data.contentHashCode()
        return h
    }

    private fun toRlpList(@Suppress("SameParameterValue") includeSignature: Boolean): RlpItem {
        check(!includeSignature) { "Use encodeSigned to attach a signature" }
        return rlpList(
            rlpInt(chainId),
            rlpInt(nonce),
            rlpInt(maxPriorityFeePerGas),
            rlpInt(maxFeePerGas),
            rlpInt(gasLimit),
            rlpBytes(to.removePrefix("0x").hexToBytes()),
            rlpInt(value),
            rlpBytes(data),
            rlpList(emptyList()),
        )
    }

    companion object {
        private val TX_TYPE_EIP1559 = byteArrayOf(0x02)
        private const val ADDRESS_HEX_LEN = 42

        private fun keccak256(data: ByteArray): ByteArray {
            val d = KeccakDigest(256)
            d.update(data, 0, data.size)
            return ByteArray(d.digestSize).also { d.doFinal(it, 0) }
        }
    }
}

fun ByteArray.asHex0x(): String = "0x" + toHex()
