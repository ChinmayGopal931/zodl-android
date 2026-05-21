package co.electriccoin.zcash.di

import co.electriccoin.zcash.ui.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.justzappit.evm.hd.EvmKey
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.signer.EoaSigner
import xyz.justzappit.offramp.account.DevOfframpAccountProvider
import xyz.justzappit.offramp.account.OfframpAccountProvider
import xyz.justzappit.offramp.config.P2pConfigProvider
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.config.P2pNetworks
import co.electriccoin.zcash.spackle.Twig
import xyz.justzappit.offramp.orchestrator.OfframpOrchestrator
import xyz.justzappit.offramp.p2p.CircleRouter
import xyz.justzappit.offramp.p2p.FallbackOrderReader
import xyz.justzappit.offramp.p2p.OnChainOrderReader
import xyz.justzappit.offramp.p2p.OrderReadSource
import xyz.justzappit.offramp.p2p.SubgraphClient
import xyz.justzappit.offramp.p2p.SubgraphOrderReader

private const val HTTP_CLIENT_QUALIFIER = "offramp_http"

val offrampModule = module {
    single<HttpClient>(named(HTTP_CLIENT_QUALIFIER)) {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json() }
        }
    }
    single<P2pConfigProvider> {
        when (BuildConfig.P2P_NETWORK.lowercase()) {
            "mainnet" -> P2pConfigProvider(
                networkName = "mainnet",
                rpcUrlOverride = BuildConfig.P2P_RPC_URL_BASE_MAINNET.takeIf { it.isNotBlank() },
                subgraphUrlOverride = BuildConfig.P2P_SUBGRAPH_URL_MAINNET.takeIf { it.isNotBlank() },
            )
            else -> P2pConfigProvider(
                networkName = "sepolia",
                rpcUrlOverride = BuildConfig.P2P_RPC_URL_BASE_SEPOLIA.takeIf { it.isNotBlank() }
                    ?: P2pNetworks.SEPOLIA.rpcUrl,
                subgraphUrlOverride = BuildConfig.P2P_SUBGRAPH_URL_SEPOLIA.takeIf { it.isNotBlank() }
                    ?: P2pNetworks.SEPOLIA.subgraphUrl,
            )
        }
    }
    single<P2pNetworkConfig> { get<P2pConfigProvider>().current() }
    single<BaseRpcClient> {
        val cfg = get<P2pNetworkConfig>()
        BaseRpcClient(httpClient = get(named(HTTP_CLIENT_QUALIFIER)), rpcUrl = cfg.rpcUrl)
    }
    single<SubgraphClient> {
        val cfg = get<P2pNetworkConfig>()
        SubgraphClient(httpClient = get(named(HTTP_CLIENT_QUALIFIER)), subgraphUrl = cfg.subgraphUrl)
    }
    single<OfframpAccountProvider> { DevOfframpAccountProvider }
    single<EvmKey> { DevOfframpAccountProvider.key }
    single<EoaSigner> {
        EoaSigner(
            rpc = get(),
            chainId = get<P2pNetworkConfig>().chainId,
            account = get(),
        )
    }
    single<CircleRouter> { CircleRouter() }
    single { SubgraphOrderReader(subgraph = get()) }
    single { OnChainOrderReader(rpc = get(), network = get()) }
    single<OrderReadSource> {
        FallbackOrderReader(
            primary = get<SubgraphOrderReader>(),
            fallback = get<OnChainOrderReader>(),
            logger = { msg, cause ->
                if (cause != null) Twig.warn(cause) { msg } else Twig.warn { msg }
            },
        )
    }
    factory {
        OfframpOrchestrator(
            rpc = get(),
            signer = get(),
            account = get(),
            network = get(),
            subgraph = get(),
            orderReader = get(),
            router = get(),
        )
    }
}
