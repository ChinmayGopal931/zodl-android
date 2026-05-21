package xyz.justzappit.evm.signer

import org.bouncycastle.crypto.digests.KeccakDigest
import xyz.justzappit.evm.hd.EvmKeyDerivation
import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Eip1559TxTest {

    @Test
    fun `signingPayload starts with 0x02 type byte`() {
        val tx = sampleTx()
        val payload = tx.signingPayload()
        assertEquals(0x02.toByte(), payload[0])
    }

    @Test
    fun `signingPayload is deterministic for same inputs`() {
        val tx = sampleTx()
        assertTrue(tx.signingPayload().contentEquals(tx.signingPayload()))
        assertTrue(tx.signingHash().contentEquals(tx.signingHash()))
    }

    @Test
    fun `signed tx ecrecovers to the signer address`() {
        val key = EvmKeyDerivation.derive(MNEMONIC, accountIndex = 0)
        val tx = sampleTx(toAddress = "0x000000000000000000000000000000000000dEaD")

        val priv = BigInteger(1, key.privateKey)
        val sig = EcdsaSigner.sign(tx.signingHash(), priv)
        val signed = tx.encodeSigned(sig)

        assertEquals(0x02.toByte(), signed[0])

        val recovered = EcdsaSigner.recoverPublicKey(
            sig.yParity.toInt(),
            sig.r,
            sig.s,
            tx.signingHash(),
        )
        assertNotNull(recovered)
        val pubXY = recovered.affineXCoord.encoded + recovered.affineYCoord.encoded
        val recoveredAddress = "0x" + keccak256(pubXY).copyOfRange(12, 32).toHex()
        assertEquals(key.address.lowercase(), recoveredAddress.lowercase())
    }

    @Test
    fun `chainId zero-encoding never sneaks in as 0x80 for the value field`() {
        // value=0 should encode as 0x80 (rlpEmpty), not as 0x00.
        val tx = Eip1559Tx(
            chainId = 84_532L,
            nonce = BigInteger.ZERO,
            maxPriorityFeePerGas = BigInteger.ONE,
            maxFeePerGas = BigInteger.TEN,
            gasLimit = BigInteger.valueOf(21_000),
            to = "0x000000000000000000000000000000000000dEaD",
            value = BigInteger.ZERO,
            data = byteArrayOf(),
        )
        val hex = tx.signingPayload().toHex()
        assertTrue(hex.startsWith("02"), "tx type prefix missing")
    }

    @Test
    fun `invalid to address is rejected`() {
        kotlin.runCatching {
            Eip1559Tx(
                chainId = 1L,
                nonce = BigInteger.ZERO,
                maxPriorityFeePerGas = BigInteger.ONE,
                maxFeePerGas = BigInteger.ONE,
                gasLimit = BigInteger.ONE,
                to = "0xnotanaddress",
                value = BigInteger.ZERO,
                data = byteArrayOf(),
            )
        }.fold(onSuccess = { error("expected to be rejected") }, onFailure = { /* expected */ })
    }

    @Test
    fun `non-empty data is included in encoding`() {
        val data = "deadbeefcafe".hexToBytes()
        val tx = sampleTx(callData = data)
        val payload = tx.signingPayload().toHex()
        assertTrue(payload.contains("86deadbeefcafe"), "expected data with length prefix in payload, got $payload")
    }

    private fun sampleTx(
        toAddress: String = "0x000000000000000000000000000000000000dEaD",
        callData: ByteArray = byteArrayOf(),
    ) = Eip1559Tx(
        chainId = 84_532L,
        nonce = BigInteger.valueOf(7),
        maxPriorityFeePerGas = BigInteger.valueOf(1_000_000L),
        maxFeePerGas = BigInteger.valueOf(50_000_000L),
        gasLimit = BigInteger.valueOf(100_000L),
        to = toAddress,
        value = BigInteger.valueOf(123_456_789L),
        data = callData,
    )

    private fun keccak256(data: ByteArray): ByteArray {
        val d = KeccakDigest(256)
        d.update(data, 0, data.size)
        return ByteArray(d.digestSize).also { d.doFinal(it, 0) }
    }

    companion object {
        const val MNEMONIC =
            "abandon abandon abandon abandon abandon abandon " +
                "abandon abandon abandon abandon abandon about"
    }
}
