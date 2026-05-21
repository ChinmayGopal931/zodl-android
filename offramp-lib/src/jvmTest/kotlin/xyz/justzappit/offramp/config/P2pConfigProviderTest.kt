package xyz.justzappit.offramp.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class P2pConfigProviderTest {
    @Test
    fun `sepolia preset uses public RPC by default`() {
        val cfg = P2pConfigProvider(networkName = "sepolia").current()
        assertEquals(P2pNetworks.SEPOLIA.chainId, cfg.chainId)
        assertEquals(P2pNetworks.SEPOLIA.rpcUrl, cfg.rpcUrl)
        assertEquals(P2pNetworks.SEPOLIA.diamondAddress, cfg.diamondAddress)
    }

    @Test
    fun `mainnet requires explicit RPC and subgraph overrides`() {
        assertFailsWith<IllegalStateException> {
            P2pConfigProvider(networkName = "mainnet").current()
        }
        assertFailsWith<IllegalStateException> {
            P2pConfigProvider(networkName = "mainnet", rpcUrlOverride = "https://x").current()
        }
    }

    @Test
    fun `mainnet with both overrides resolves`() {
        val cfg = P2pConfigProvider(
            networkName = "mainnet",
            rpcUrlOverride = "https://mainnet.example",
            subgraphUrlOverride = "https://graph.example",
        ).current()
        assertEquals(P2pNetworks.MAINNET_CHAIN_ID, cfg.chainId)
        assertEquals("https://mainnet.example", cfg.rpcUrl)
        assertEquals("https://graph.example", cfg.subgraphUrl)
        assertEquals(P2pNetworks.MAINNET_USDC_ADDRESS, cfg.usdcAddress)
    }

    @Test
    fun `unknown network name fails`() {
        assertFailsWith<IllegalStateException> {
            P2pConfigProvider(networkName = "goerli").current()
        }
    }

    @Test
    fun `network name is case-insensitive`() {
        val cfg = P2pConfigProvider(networkName = "SEPOLIA").current()
        assertEquals(P2pNetworks.SEPOLIA.chainId, cfg.chainId)
    }

    @Test
    fun `P2pNetworkConfig rejects blank rpcUrl`() {
        assertFailsWith<IllegalArgumentException> {
            P2pNetworkConfig(
                name = "x",
                chainId = 1,
                rpcUrl = "",
                diamondAddress = "0x0",
                usdcAddress = "0x0",
                reputationManagerAddress = "0x0",
                subgraphUrl = "https://s",
                baseExplorerUrl = "https://e",
            )
        }
    }

    @Test
    fun `P2pNetworkConfig rejects blank subgraphUrl`() {
        assertFailsWith<IllegalArgumentException> {
            P2pNetworkConfig(
                name = "x",
                chainId = 1,
                rpcUrl = "https://r",
                diamondAddress = "0x0",
                usdcAddress = "0x0",
                reputationManagerAddress = "0x0",
                subgraphUrl = "",
                baseExplorerUrl = "https://e",
            )
        }
    }
}
