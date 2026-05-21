package xyz.justzappit.offramp.config

data class P2pNetworkConfig(
    val name: String,
    val chainId: Long,
    val rpcUrl: String,
    val diamondAddress: String,
    val usdcAddress: String,
    val reputationManagerAddress: String,
    val subgraphUrl: String,
    val baseExplorerUrl: String,
)

object P2pNetworks {
    val SEPOLIA = P2pNetworkConfig(
        name = "sepolia",
        chainId = SEPOLIA_CHAIN_ID,
        rpcUrl = "https://sepolia.base.org",
        diamondAddress = "0xce868398FDaDcA368EAc203222874D6888532aE2",
        usdcAddress = "0xDABa329Ed949f28F64019f22c33c3B253B2Ded60",
        reputationManagerAddress = "0x45919D69E2154F46b6f6eA42ae23d2e9ee21B66f",
        subgraphUrl = "https://api.studio.thegraph.com/query/110312/indexer-one/version/latest",
        baseExplorerUrl = "https://sepolia.basescan.org",
    )

    val MAINNET = P2pNetworkConfig(
        name = "mainnet",
        chainId = MAINNET_CHAIN_ID,
        rpcUrl = "",
        diamondAddress = "0x4cad6eC90e65baBec9335cAd728DDC610c316368",
        usdcAddress = "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913",
        reputationManagerAddress = "0xCF613e08EE1B4c2669DdCf06A7d22c9856f6Aa1D",
        subgraphUrl = "",
        baseExplorerUrl = "https://basescan.org",
    )

    private const val SEPOLIA_CHAIN_ID = 84_532L
    private const val MAINNET_CHAIN_ID = 8_453L
}
