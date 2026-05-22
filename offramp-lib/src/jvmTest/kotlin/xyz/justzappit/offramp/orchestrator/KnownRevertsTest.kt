package xyz.justzappit.offramp.orchestrator

import xyz.justzappit.evm.abi.Selector4
import xyz.justzappit.evm.abi.SolidityErrors
import xyz.justzappit.evm.rpc.RpcException
import xyz.justzappit.evm.util.hexToBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KnownRevertsTest {

    // -- Curated KnownRevertReason mappings --------------------------------------------------

    @Test
    fun `0x91da284f maps to BuyOrderAmountExceedsLimit (was misnamed InsufficientReputation)`() {
        // Regression: prior to the wholesale-port refactor, this selector was labelled
        // InsufficientReputation. The SDK's canonical name is BuyOrderAmountExceedsLimit; the
        // _functional_ "you need more RP" effect comes from txLimit = RP × multiplier, so the
        // user copy is similar — but the enum + sdkErrorName must match the SDK.
        assertEquals(KnownRevertReason.BuyOrderAmountExceedsLimit, KnownReverts.explain(revertedWith("0x91da284f")))
        assertEquals("BUY_ORDER_AMOUNT_EXCEEDS_LIMIT", KnownReverts.sdkName(revertedWith("0x91da284f")))
    }

    @Test
    fun `0x412dd2b1 maps to InsufficientReputation (the real RP=0 case)`() {
        assertEquals(KnownRevertReason.InsufficientReputation, KnownReverts.explain(revertedWith("0x412dd2b1")))
        assertEquals("INSUFFICIENT_RP", KnownReverts.sdkName(revertedWith("0x412dd2b1")))
    }

    @Test
    fun `0x5d04ff4c maps to NotEnoughEligibleMerchants`() {
        assertEquals(KnownRevertReason.NotEnoughEligibleMerchants, KnownReverts.explain(revertedWith("0x5d04ff4c")))
    }

    @Test
    fun `all three USDC-transfer-failed selectors collapse to one curated reason`() {
        val selectors = listOf("0x149f9fca", "0x47bfece5", "0x279bbc0c")
        for (s in selectors) {
            assertEquals(
                KnownRevertReason.UsdcTransferFailed,
                KnownReverts.explain(revertedWith(s)),
                "selector $s should map to UsdcTransferFailed",
            )
        }
    }

    @Test
    fun `setSellOrderUpi-phase selectors are curated`() {
        assertEquals(KnownRevertReason.UpiAlreadySent, KnownReverts.explain(revertedWith("0xc1654697")))
        assertEquals(KnownRevertReason.InvalidOrderUpi, KnownReverts.explain(revertedWith("0xaa60ec26")))
        assertEquals(KnownRevertReason.OrderNotAccepted, KnownReverts.explain(revertedWith("0x6b1b90b4")))
        assertEquals(KnownRevertReason.OrderExpired, KnownReverts.explain(revertedWith("0xc56873ba")))
    }

    @Test
    fun `placeOrder-phase guardrail selectors are curated`() {
        assertEquals(KnownRevertReason.OrderAmountExceedsLimit, KnownReverts.explain(revertedWith("0xf42e41a1")))
        assertEquals(KnownRevertReason.SellAmountExceedsFiatLimit, KnownReverts.explain(revertedWith("0xbba2edf9")))
        assertEquals(KnownRevertReason.CurrencyNotSupported, KnownReverts.explain(revertedWith("0x02a6fdd2")))
        assertEquals(KnownRevertReason.UserIsBlacklisted, KnownReverts.explain(revertedWith("0xebb6f34b")))
        assertEquals(KnownRevertReason.ExchangeNotOperational, KnownReverts.explain(revertedWith("0x4bbac5de")))
    }

    @Test
    fun `selector-only overload also resolves curated reasons`() {
        val selector = Selector4.fromHex("0x91da284f")
        assertEquals(KnownRevertReason.BuyOrderAmountExceedsLimit, KnownReverts.explain(selector))
        assertNull(KnownReverts.explain(null))
    }

    // -- Wholesale KnownContractErrors long-tail (uncurated but still labelled) --------------

    @Test
    fun `uncurated selector still resolves to an SDK name and message via the wholesale tables`() {
        // OrderAlreadyCompleted is one of the ~175 selectors we don't curate — it should not produce
        // a KnownRevertReason but must produce a non-null sdkName + sdkMessage so the UI can show
        // "Contract error: Order already marked completed" instead of a raw 4-byte selector.
        val r = revertedWith("0x03683687")
        assertNull(KnownReverts.explain(r))
        assertEquals("ORDER_ALREADY_COMPLETED", KnownReverts.sdkName(r))
        assertEquals("Order already marked completed", KnownReverts.sdkMessage(r))
    }

    @Test
    fun `ORDER_NOT_ACCEPTED and ORDER_NOT_PLACED are distinct (selector-collision regression)`() {
        // Regression for the hand-merged table that mapped BOTH 0x6b1b90b4 and 0x58db8ed6 to
        // "ORDER_NOT_PLACED_TO_BE_ACCEPTED" (and 0x7f61b868 / 0xc1654697 to the same string). The
        // SDK's errors.ts is now the single source of truth, so every code is distinct.
        assertEquals("ORDER_NOT_ACCEPTED", KnownContractErrors.nameFor(Selector4.fromHex("0x6b1b90b4")))
        assertEquals("ORDER_NOT_PLACED", KnownContractErrors.nameFor(Selector4.fromHex("0x58db8ed6")))
        assertEquals("ORDER_ALREADY_PAID", KnownContractErrors.nameFor(Selector4.fromHex("0x7f61b868")))
        assertEquals("UPI_ALREADY_SENT", KnownContractErrors.nameFor(Selector4.fromHex("0xc1654697")))
    }

    @Test
    fun `KnownContractErrors covers every curated selector`() {
        // Every curated selector must also exist in the wholesale SDK table. If this fails, the
        // curated map drifted from the SDK and a re-run of generate-revert-selectors.ts is overdue.
        val curatedSelectors = listOf(
            "0x91da284f", "0x412dd2b1", "0xf42e41a1", "0xbba2edf9",
            "0x02a6fdd2", "0xebb6f34b", "0x4bbac5de", "0x5d04ff4c", "0xc56873ba",
            "0xc1654697", "0xaa60ec26", "0x6b1b90b4",
            "0x149f9fca", "0x47bfece5", "0x279bbc0c",
        )
        for (s in curatedSelectors) {
            assertNotNull(
                KnownContractErrors.nameFor(Selector4.fromHex(s)),
                "Curated selector $s missing from KnownContractErrors — regenerate the wholesale table",
            )
        }
    }

    @Test
    fun `sdkMessage renders human-readable SDK copy for the long tail`() {
        assertEquals("Order expired", KnownReverts.sdkMessage(revertedWith("0xc56873ba")))
        assertEquals("Order not placed to be accepted", KnownReverts.sdkMessage(revertedWith("0x6b1b90b4")))
        assertEquals("USDC transfer failed", KnownReverts.sdkMessage(revertedWith("0x149f9fca")))
        assertNull(KnownReverts.sdkMessage(revertedWith("0xdeadbeef")))
    }

    @Test
    fun `every SDK error code has a message (generators stay in lock-step)`() {
        // Both tables are generated 1:1 from the same SDK source; equal non-trivial size is the
        // parity guard. If a future regen adds a selector without a message (or vice-versa), the
        // counts diverge and this fails.
        assertEquals(KnownContractErrors.size, KnownContractErrorMessages.size)
        assertTrue(KnownContractErrorMessages.size >= 120, "expected ≥120 messages, got ${KnownContractErrorMessages.size}")
    }

    @Test
    fun `KnownContractErrors table is non-trivially populated`() {
        // Guards against a future bad regen of the generator producing an empty table.
        assertTrue(KnownContractErrors.size >= 120, "expected ≥120 mapped selectors, got ${KnownContractErrors.size}")
    }

    @Test
    fun `KnownContractErrors returns null for genuinely unknown selectors`() {
        assertNull(KnownContractErrors.nameFor(Selector4.fromHex("0xdeadbeef")))
        assertNull(KnownContractErrors.nameFor(null))
    }

    @Test
    fun `explain returns null for selectors outside the curated set`() {
        // 0x03683687 = ORDER_ALREADY_COMPLETED — known by SDK but not actionable enough
        // to be in KnownRevertReason. explain() must say null; sdkName() must still resolve.
        assertNull(KnownReverts.explain(revertedWith("0x03683687")))
    }

    @Test
    fun `explain returns null for a completely unknown selector`() {
        assertNull(KnownReverts.explain(revertedWith("0xdeadbeef")))
        assertNull(KnownReverts.sdkName(revertedWith("0xdeadbeef")))
    }

    // -- ERC-4337 bundler error decoding -----------------------------------------------------

    @Test
    fun `0xea8e4eb5 maps to NotAuthorized`() {
        assertEquals(KnownRevertReason.NotAuthorized, KnownReverts.explain(revertedWith("0xea8e4eb5")))
        assertEquals("NOT_AUTHORIZED", KnownReverts.sdkName(revertedWith("0xea8e4eb5")))
    }

    @Test
    fun `selectorFromMessage recovers a selector from a bundler revert message`() {
        val msg = "UserOperation reverted during simulation with reason: 0xea8e4eb5"
        val selector = KnownReverts.selectorFromMessage(msg)
        assertEquals(Selector4.fromHex("0xea8e4eb5"), selector)
        assertEquals(KnownRevertReason.NotAuthorized, KnownReverts.explain(selector))
    }

    @Test
    fun `selectorFromMessage ignores non-selector input`() {
        assertNull(KnownReverts.selectorFromMessage(null))
        assertNull(KnownReverts.selectorFromMessage("AA25 invalid account nonce"))
        // A full 20-byte address must not be mistaken for a 4-byte selector.
        assertNull(KnownReverts.selectorFromMessage("sender 0xdD53a3Db48e5b69F34Abc1fA3156Dc3d0c269D5E rejected"))
    }

    // -- SolidityErrors regression preserved -------------------------------------------------

    @Test
    fun `SolidityErrors decodes an Error(string) payload`() {
        // 0x08c379a0 || offset(0x20) || length(0x05) || "hello" || padding
        val payload = (
            "0x08c379a0" +
                "0000000000000000000000000000000000000000000000000000000000000020" +
                "0000000000000000000000000000000000000000000000000000000000000005" +
                "68656c6c6f000000000000000000000000000000000000000000000000000000"
            ).hexToBytes()
        assertEquals("hello", SolidityErrors.decodeErrorString(payload))
    }

    @Test
    fun `SolidityErrors returns null for non-Error payloads`() {
        assertNull(SolidityErrors.decodeErrorString("0x91da284f".hexToBytes()))
        assertNull(SolidityErrors.decodeErrorString(byteArrayOf()))
    }

    private fun revertedWith(selectorHex: String): RpcException.ExecutionReverted = RpcException.ExecutionReverted(
        method = "eth_call",
        selector = Selector4.fromHex(selectorHex),
        data = selectorHex.hexToBytes(),
        solidityErrorString = null,
        rawMessage = "execution reverted",
    )
}
