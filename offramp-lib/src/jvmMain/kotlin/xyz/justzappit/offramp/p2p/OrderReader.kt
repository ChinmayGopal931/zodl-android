package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.util.toHex
import java.math.BigInteger

enum class OrderStatus(val onChain: Int) {
    PLACED(0),
    ACCEPTED(1),
    PAID(2),
    COMPLETED(3),
    CANCELLED(4);

    companion object {
        fun fromOnChain(value: Int): OrderStatus =
            values().firstOrNull { it.onChain == value }
                ?: error("Unknown on-chain OrderStatus: $value")
    }
}

data class OrderRead(
    val status: OrderStatus,
    val acceptedMerchant: String,
    val merchantPubKey: String,
)

object OrderReader {

    typealias Order = OrderRead
    private const val WORD = 32
    private const val TOP_LEVEL_OFFSET_SLOT = 1

    fun decodeOrder(returnData: ByteArray): OrderRead {
        require(returnData.size >= TOP_LEVEL_OFFSET_SLOT * WORD + ORDER_TUPLE_MIN_HEAD) {
            "Order return data too short: ${returnData.size} bytes"
        }
        val tupleStart = TOP_LEVEL_OFFSET_SLOT * WORD
        val tuple = returnData.copyOfRange(tupleStart, returnData.size)

        val acceptedMerchant = "0x" + tuple.slice(slotRange(FIELD_ACCEPTED_MERCHANT))
            .toByteArray().copyOfRange(WORD - ADDRESS_BYTES, WORD).toHex()

        val statusSlot = tuple.copyOfRange(FIELD_STATUS * WORD, (FIELD_STATUS + 1) * WORD)
        val statusByte = statusSlot.last().toInt() and 0xff
        val status = OrderStatus.fromOnChain(statusByte)

        val pubKeyOffset = BigInteger(
            1,
            tuple.copyOfRange(FIELD_PUBKEY * WORD, (FIELD_PUBKEY + 1) * WORD),
        ).toInt()
        val pubKey = decodeStringAt(tuple, pubKeyOffset)

        return OrderRead(status, acceptedMerchant, pubKey)
    }

    fun decodeAddressArrayNonEmpty(returnData: ByteArray): Boolean {
        if (returnData.size < 2 * WORD) return false
        val offset = BigInteger(1, returnData.copyOfRange(0, WORD)).toInt()
        if (offset + WORD > returnData.size) return false
        val length = BigInteger(1, returnData.copyOfRange(offset, offset + WORD)).toInt()
        return length > 0
    }

    private fun decodeStringAt(tuple: ByteArray, offset: Int): String {
        if (offset < 0 || offset + WORD > tuple.size) return ""
        val length = BigInteger(1, tuple.copyOfRange(offset, offset + WORD)).toInt()
        if (length == 0) return ""
        val dataStart = offset + WORD
        val dataEnd = dataStart + length
        if (dataEnd > tuple.size) return ""
        return tuple.copyOfRange(dataStart, dataEnd).toString(Charsets.UTF_8)
    }

    private fun slotRange(index: Int): IntRange = (index * WORD) until ((index + 1) * WORD)

    /** Field positions within the Order struct, as ordered in `order-processor-facet.ts`. */
    private const val FIELD_ACCEPTED_MERCHANT = 5
    private const val FIELD_PUBKEY = 8
    private const val FIELD_STATUS = 11

    private const val ADDRESS_BYTES = 20

    /** At minimum we read up to slot 11 (status). Anything shorter is malformed. */
    private const val ORDER_TUPLE_MIN_HEAD = (FIELD_STATUS + 1) * WORD
}
