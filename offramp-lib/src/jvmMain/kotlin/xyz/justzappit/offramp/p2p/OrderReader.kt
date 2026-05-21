package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.types.Address
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
            entries.firstOrNull { it.onChain == value }
                ?: error("Unknown on-chain OrderStatus: $value")
    }
}

data class OrderRead(
    val status: OrderStatus,
    val acceptedMerchant: Address?,
    val merchantPubKey: String,
)

object OrderReader {

    fun decodeOrderSnapshot(returnData: ByteArray, orderId: BigInteger): OrderSnapshot {
        require(returnData.size >= TOP_LEVEL_OFFSET_SLOT * WORD + ORDER_TUPLE_MIN_HEAD_FULL) {
            "Order return data too short: ${returnData.size} bytes"
        }
        val tupleStart = TOP_LEVEL_OFFSET_SLOT * WORD
        val tuple = returnData.copyOfRange(tupleStart, returnData.size)

        val acceptedMerchantBytes = tuple.slice(slotRange(FIELD_ACCEPTED_MERCHANT))
            .toByteArray().copyOfRange(WORD - ADDRESS_BYTES, WORD)
        val acceptedMerchant = if (acceptedMerchantBytes.all { it == 0.toByte() }) {
            null
        } else {
            Address.fromBytes(acceptedMerchantBytes)
        }
        val userAddress = Address.fromBytes(
            tuple.slice(slotRange(FIELD_USER)).toByteArray().copyOfRange(WORD - ADDRESS_BYTES, WORD),
        )

        val statusSlot = tuple.copyOfRange(FIELD_STATUS * WORD, (FIELD_STATUS + 1) * WORD)
        val statusByte = statusSlot.last().toInt() and 0xff
        val status = OrderStatus.fromOnChain(statusByte)

        val orderTypeByte = tuple.copyOfRange(FIELD_ORDER_TYPE * WORD, (FIELD_ORDER_TYPE + 1) * WORD)
            .last().toInt() and 0xff
        val orderType = OrderType.entries.firstOrNull { it.onChain == orderTypeByte }
            ?: error("Unknown OrderType from chain: $orderTypeByte")

        val usdcAmount = Usdc6(BigInteger(1, tuple.copyOfRange(FIELD_AMOUNT * WORD, (FIELD_AMOUNT + 1) * WORD)))
        val fiatAmount = Usdc6(BigInteger(1, tuple.copyOfRange(FIELD_FIAT_AMOUNT * WORD, (FIELD_FIAT_AMOUNT + 1) * WORD)))
        val placedTimestamp = BigInteger(
            1,
            tuple.copyOfRange(FIELD_PLACED_TS * WORD, (FIELD_PLACED_TS + 1) * WORD),
        ).toLong()
        val completedTimestamp = BigInteger(
            1,
            tuple.copyOfRange(FIELD_COMPLETED_TS * WORD, (FIELD_COMPLETED_TS + 1) * WORD),
        ).toLong()
        val circleId = BigInteger(1, tuple.copyOfRange(FIELD_CIRCLE_ID * WORD, (FIELD_CIRCLE_ID + 1) * WORD))
        val currencyHex = "0x" + tuple.copyOfRange(FIELD_CURRENCY * WORD, (FIELD_CURRENCY + 1) * WORD).toHex()

        val pubKeyOffset = BigInteger(
            1,
            tuple.copyOfRange(FIELD_PUBKEY * WORD, (FIELD_PUBKEY + 1) * WORD),
        ).toInt()
        val merchantPubKey = decodeStringAt(tuple, pubKeyOffset)

        val encUpiOffset = BigInteger(
            1,
            tuple.copyOfRange(FIELD_ENC_UPI * WORD, (FIELD_ENC_UPI + 1) * WORD),
        ).toInt()
        val encryptedUserUpi = decodeStringAt(tuple, encUpiOffset)

        val encMerchantUpiOffset = BigInteger(
            1,
            tuple.copyOfRange(FIELD_ENC_MERCHANT_UPI * WORD, (FIELD_ENC_MERCHANT_UPI + 1) * WORD),
        ).toInt()
        val encryptedMerchantUpi = decodeStringAt(tuple, encMerchantUpiOffset)

        return OrderSnapshot(
            orderId = orderId,
            status = status,
            orderType = orderType,
            circleId = circleId,
            userAddress = userAddress,
            usdcAmount = usdcAmount,
            fiatAmount = fiatAmount,
            currencyHex = currencyHex,
            acceptedMerchantAddress = acceptedMerchant,
            merchantPubKey = merchantPubKey,
            encryptedUserUpi = encryptedUserUpi,
            encryptedMerchantUpi = encryptedMerchantUpi,
            placedAtEpochSeconds = placedTimestamp.takeIf { it > 0 },
            // On-chain Order tuple only carries placed + completed timestamps.
            acceptedAtEpochSeconds = null,
            paidAtEpochSeconds = null,
            completedAtEpochSeconds = completedTimestamp.takeIf { it > 0 },
            cancelledAtEpochSeconds = null,
            // actualUsdcAmount / actualFiatAmount need a separate getAdditionalOrderDetails call.
            actualUsdcAmount = null,
            actualFiatAmount = null,
            placedTxHash = null,
            placedAtBlockNumber = null,
            source = OrderSnapshot.Source.OnChain,
        )
    }

    fun decodeOrder(returnData: ByteArray): OrderRead {
        require(returnData.size >= TOP_LEVEL_OFFSET_SLOT * WORD + ORDER_TUPLE_MIN_HEAD) {
            "Order return data too short: ${returnData.size} bytes"
        }
        val tupleStart = TOP_LEVEL_OFFSET_SLOT * WORD
        val tuple = returnData.copyOfRange(tupleStart, returnData.size)

        val acceptedMerchantBytes = tuple.slice(slotRange(FIELD_ACCEPTED_MERCHANT))
            .toByteArray().copyOfRange(WORD - ADDRESS_BYTES, WORD)
        val acceptedMerchant = if (acceptedMerchantBytes.all { it == 0.toByte() }) {
            null
        } else {
            Address.fromBytes(acceptedMerchantBytes)
        }

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

    private const val WORD = 32
    private const val TOP_LEVEL_OFFSET_SLOT = 1

    // Field positions within the Order struct, ordered as in order-processor-facet.ts.
    private const val FIELD_AMOUNT = 0
    private const val FIELD_FIAT_AMOUNT = 1
    private const val FIELD_PLACED_TS = 2
    private const val FIELD_COMPLETED_TS = 3
    private const val FIELD_ACCEPTED_MERCHANT = 5
    private const val FIELD_USER = 6
    private const val FIELD_PUBKEY = 8
    private const val FIELD_ENC_UPI = 9
    private const val FIELD_STATUS = 11
    private const val FIELD_ORDER_TYPE = 12

    // Positions 13..16 hold the inline disputeInfo (4 static slots); the rest of the
    // Order tuple resumes at 17. Slot indices below already account for that.
    private const val FIELD_ENC_MERCHANT_UPI = 19
    private const val FIELD_CURRENCY = 22
    private const val FIELD_CIRCLE_ID = 24

    private const val ADDRESS_BYTES = 20

    private const val ORDER_TUPLE_MIN_HEAD = (FIELD_STATUS + 1) * WORD
    private const val ORDER_TUPLE_MIN_HEAD_FULL = (FIELD_CIRCLE_ID + 1) * WORD
}
