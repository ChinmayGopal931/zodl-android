package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.util.toHex
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class Erc20CallsTest {
    @Test
    fun `approve calldata matches the well-known reference`() {
        // approve(diamond=0xce868398..., amount=1_000_000) — same hex any ERC-20 client emits.
        val calldata = Erc20Calls.approveCalldata(
            spender = "0xce868398FDaDcA368EAc203222874D6888532aE2",
            amount = BigInteger.valueOf(1_000_000),
        ).toHex()
        assertEquals(
            "095ea7b3" +
                "000000000000000000000000ce868398fdadca368eac203222874d6888532ae2" +
                "00000000000000000000000000000000000000000000000000000000000f4240",
            calldata,
        )
    }
}
