package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.util.hexToBytes
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderReaderTest {

    @Test
    fun `decodeOrder picks status, acceptedMerchant, and merchant pubkey at known offsets`() {
        val merchant = "0x1234567890123456789012345678901234567890"
        val pubKey = "abcdef0123456789".repeat(8) // 128 chars, eth-crypto format

        val data = synthOrderReturnData(
            status = OrderStatus.ACCEPTED,
            acceptedMerchant = merchant,
            pubkey = pubKey,
        )
        val order = OrderReader.decodeOrder(data)
        assertEquals(OrderStatus.ACCEPTED, order.status)
        assertEquals(Address.parse(merchant), order.acceptedMerchant)
        assertEquals(pubKey, order.merchantPubKey)
    }

    @Test
    fun `decodeOrder handles each status enum value`() {
        for (s in OrderStatus.values()) {
            val data = synthOrderReturnData(status = s)
            assertEquals(s, OrderReader.decodeOrder(data).status)
        }
    }

    @Test
    fun `decodeOrder returns empty pubkey when offset is zero`() {
        val data = synthOrderReturnData(
            status = OrderStatus.PLACED,
            acceptedMerchant = "0x" + "00".repeat(20),
            pubkey = null,
        )
        val order = OrderReader.decodeOrder(data)
        assertEquals("", order.merchantPubKey)
    }

    @Test
    fun `decodeOrder rejects too-short input`() {
        kotlin.runCatching { OrderReader.decodeOrder(ByteArray(64)) }
            .fold(onSuccess = { error("expected failure") }, onFailure = { /* expected */ })
    }

    @Test
    fun `decodeAddressArrayNonEmpty returns true for length gt 0`() {
        val data = encodeAddressArray(listOf("0x" + "11".repeat(20)))
        assertTrue(OrderReader.decodeAddressArrayNonEmpty(data))
    }

    @Test
    fun `decodeAddressArrayNonEmpty returns false for length 0`() {
        val data = encodeAddressArray(emptyList())
        assertFalse(OrderReader.decodeAddressArrayNonEmpty(data))
    }

    @Test
    fun `decodeAddressArrayNonEmpty returns false for empty input`() {
        assertFalse(OrderReader.decodeAddressArrayNonEmpty(ByteArray(0)))
        assertFalse(OrderReader.decodeAddressArrayNonEmpty(ByteArray(16)))
    }

    /** Builds a synthetic Order struct return value with sentinel values for fields we don't read. */
    private fun synthOrderReturnData(
        status: OrderStatus,
        acceptedMerchant: String = "0x" + "ab".repeat(20),
        pubkey: String? = null,
    ): ByteArray {
        val word = 32
        val headSlots = 25
        val tupleHead = ByteArray(headSlots * word)

        // Slot 5: acceptedMerchant
        val mBytes = acceptedMerchant.removePrefix("0x").hexToBytes()
        System.arraycopy(mBytes, 0, tupleHead, 5 * word + (word - mBytes.size), mBytes.size)

        // Slot 11: status (uint8)
        tupleHead[12 * word - 1] = status.onChain.toByte()

        // Slot 8: pubkey offset (or 0 if null pubkey)
        val tupleTail: ByteArray
        if (pubkey != null) {
            val tailOffset = headSlots * word
            val offsetBytes = BigInteger.valueOf(tailOffset.toLong()).toByteArray()
            System.arraycopy(offsetBytes, 0, tupleHead, 9 * word - offsetBytes.size, offsetBytes.size)
            val pubKeyBytes = pubkey.toByteArray(Charsets.UTF_8)
            val pad = if (pubKeyBytes.size % word == 0) 0 else word - (pubKeyBytes.size % word)
            val tail = ByteArray(word + pubKeyBytes.size + pad)
            val lenBytes = BigInteger.valueOf(pubKeyBytes.size.toLong()).toByteArray()
            System.arraycopy(lenBytes, 0, tail, word - lenBytes.size, lenBytes.size)
            System.arraycopy(pubKeyBytes, 0, tail, word, pubKeyBytes.size)
            tupleTail = tail
        } else {
            tupleTail = ByteArray(0)
        }

        // Top-level offset = 0x20 (pointing past itself to the tuple data)
        val topOffset = ByteArray(word).also { it[word - 1] = 0x20.toByte() }
        return topOffset + tupleHead + tupleTail
    }

    private fun encodeAddressArray(addresses: List<String>): ByteArray {
        val word = 32
        val out = ByteArray(word * (2 + addresses.size))
        // offset 32
        out[word - 1] = 0x20.toByte()
        // length
        val lenBytes = BigInteger.valueOf(addresses.size.toLong()).toByteArray()
        System.arraycopy(lenBytes, 0, out, 2 * word - lenBytes.size, lenBytes.size)
        // addresses
        addresses.forEachIndexed { i, addr ->
            val addrBytes = addr.removePrefix("0x").hexToBytes()
            System.arraycopy(addrBytes, 0, out, (2 + i) * word + (word - addrBytes.size), addrBytes.size)
        }
        return out
    }
}
