package xyz.justzappit.evm.signer

import xyz.justzappit.evm.abi.keccak256
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.ChainId
import xyz.justzappit.evm.types.Wei
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger

data class Eip1559Tx(
    val chainId: ChainId,
    val nonce: BigInteger,
    val maxPriorityFeePerGas: Wei,
    val maxFeePerGas: Wei,
    val gasLimit: BigInteger,
    val to: Address,
    val value: Wei,
    val data: ByteArray,
) {
    fun signingPayload(): ByteArray = TX_TYPE_EIP1559 + Rlp.encode(toRlpList())

    fun encodeSigned(sig: EcdsaSignature): ByteArray {
        val items = (toRlpList() as RlpItem.L).items + listOf(
            rlpInt(sig.yParity.toLong()),
            rlpInt(sig.r),
            rlpInt(sig.s),
        )
        return TX_TYPE_EIP1559 + Rlp.encode(RlpItem.L(items))
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

    private fun toRlpList(): RlpItem = rlpList(
        rlpInt(chainId.value),
        rlpInt(nonce),
        rlpInt(maxPriorityFeePerGas.value),
        rlpInt(maxFeePerGas.value),
        rlpInt(gasLimit),
        rlpBytes(to.bytes),
        rlpInt(value.value),
        rlpBytes(data),
        rlpList(emptyList()),
    )

    companion object {
        private val TX_TYPE_EIP1559 = byteArrayOf(0x02)
    }
}

fun ByteArray.asHex0x(): String = "0x" + toHex()
