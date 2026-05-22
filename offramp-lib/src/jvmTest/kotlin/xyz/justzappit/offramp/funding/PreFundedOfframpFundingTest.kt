package xyz.justzappit.offramp.funding

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.orchestrator.OfframpRequest
import xyz.justzappit.offramp.p2p.Usdc6
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PreFundedOfframpFundingTest {
    private val usdc = Address.parse("0xDABa329Ed949f28F64019f22c33c3B253B2Ded60")
    private val account = Address.parse("0xdD53a3Db48e5b69F34Abc1fA3156Dc3d0c269D5E")
    private val request = OfframpRequest(
        recipientUpi = "merchant@upi",
        usdcAmount = Usdc6(BigInteger.valueOf(1_000_000)),
    )

    private fun fundingWithBalance(micros: BigInteger): PreFundedOfframpFunding {
        val resultHex = "0x" + micros.toString(HEX).padStart(WORD_HEX, '0')
        val client = HttpClient(
            MockEngine {
                respond(
                    content = """{"jsonrpc":"2.0","id":1,"result":"$resultHex"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) { install(ContentNegotiation) { json() } }
        return PreFundedOfframpFunding(BaseRpcClient(client, "http://mock/rpc"), usdc)
    }

    @Test
    fun `passes when balance covers the order`() = runTest {
        fundingWithBalance(BigInteger.valueOf(2_000_000)).ensureFunded(account, request)
    }

    @Test
    fun `fails fast with an actionable message when balance is short`() = runTest {
        val e = assertFailsWith<IllegalStateException> {
            fundingWithBalance(BigInteger.valueOf(500_000)).ensureFunded(account, request)
        }
        assertTrue(e.message!!.contains("Fund it directly"), "expected funding hint, got: ${e.message}")
    }

    private companion object {
        const val HEX = 16
        const val WORD_HEX = 64
    }
}
