package xyz.justzappit.offramp.config

import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.ChainId

data class P2pNetworkConfig(
    val name: String,
    val chainId: ChainId,
    val rpcUrl: String,
    val diamondAddress: Address,
    val usdcAddress: Address,
    val reputationManagerAddress: Address,
    val subgraphUrl: String,
    val baseExplorerUrl: String,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(rpcUrl.isNotBlank()) { "rpcUrl must not be blank for '$name'" }
        require(subgraphUrl.isNotBlank()) { "subgraphUrl must not be blank for '$name'" }
        require(baseExplorerUrl.isNotBlank()) { "baseExplorerUrl must not be blank for '$name'" }
    }
}

object P2pNetworks {
    val SEPOLIA = P2pNetworkConfig(
        name = SEPOLIA_NAME,
        chainId = ChainId.BASE_SEPOLIA,
        rpcUrl = "https://sepolia.base.org",
        diamondAddress = Address.parse("0xce868398FDaDcA368EAc203222874D6888532aE2"),
        usdcAddress = Address.parse("0xDABa329Ed949f28F64019f22c33c3B253B2Ded60"),
        reputationManagerAddress = Address.parse("0x45919D69E2154F46b6f6eA42ae23d2e9ee21B66f"),
        subgraphUrl = "https://api.studio.thegraph.com/query/110312/indexer-one/version/latest",
        baseExplorerUrl = "https://sepolia.basescan.org",
    )

    fun mainnet(rpcUrl: String, subgraphUrl: String): P2pNetworkConfig = P2pNetworkConfig(
        name = MAINNET_NAME,
        chainId = ChainId.BASE_MAINNET,
        rpcUrl = rpcUrl,
        diamondAddress = Address.parse(MAINNET_DIAMOND_ADDRESS),
        usdcAddress = Address.parse(MAINNET_USDC_ADDRESS),
        reputationManagerAddress = Address.parse(MAINNET_REPUTATION_MANAGER_ADDRESS),
        subgraphUrl = subgraphUrl,
        baseExplorerUrl = MAINNET_BASE_EXPLORER_URL,
    )

    const val SEPOLIA_NAME = "sepolia"
    const val MAINNET_NAME = "mainnet"

    val MAINNET_CHAIN_ID: ChainId = ChainId.BASE_MAINNET
    const val MAINNET_DIAMOND_ADDRESS = "0x4cad6eC90e65baBec9335cAd728DDC610c316368"
    const val MAINNET_USDC_ADDRESS = "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913"
    const val MAINNET_REPUTATION_MANAGER_ADDRESS = "0xCF613e08EE1B4c2669DdCf06A7d22c9856f6Aa1D"
    const val MAINNET_BASE_EXPLORER_URL = "https://basescan.org"
}
