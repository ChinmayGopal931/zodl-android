package co.electriccoin.zcash.di

import co.electriccoin.zcash.ui.common.provider.ApplicationStateProvider
import co.electriccoin.zcash.ui.common.provider.ApplicationStateProviderImpl
import co.electriccoin.zcash.ui.common.provider.BlockchainProvider
import co.electriccoin.zcash.ui.common.provider.BlockchainProviderImpl
import co.electriccoin.zcash.ui.common.provider.ChatSendContextProvider
import co.electriccoin.zcash.ui.common.provider.CMCApiProvider
import co.electriccoin.zcash.ui.common.provider.CMCApiProviderImpl
import co.electriccoin.zcash.ui.common.provider.CrashReportingStorageProvider
import co.electriccoin.zcash.ui.common.provider.CrashReportingStorageProviderImpl
import co.electriccoin.zcash.ui.common.provider.EphemeralAddressStorageProvider
import co.electriccoin.zcash.ui.common.provider.EphemeralAddressStorageProviderImpl
import co.electriccoin.zcash.ui.common.provider.GetVersionInfoProvider
import co.electriccoin.zcash.ui.common.provider.GetZcashCurrencyProvider
import co.electriccoin.zcash.ui.common.provider.HttpClientProvider
import co.electriccoin.zcash.ui.common.provider.HttpClientProviderImpl
import co.electriccoin.zcash.ui.common.provider.IsExchangeRateEnabledStorageProvider
import co.electriccoin.zcash.ui.common.provider.IsExchangeRateEnabledStorageProviderImpl
import co.electriccoin.zcash.ui.common.provider.IsKeepScreenOnDuringRestoreProvider
import co.electriccoin.zcash.ui.common.provider.IsKeepScreenOnDuringRestoreProviderImpl
import co.electriccoin.zcash.ui.common.provider.IsTorEnabledStorageProvider
import co.electriccoin.zcash.ui.common.provider.IsTorEnabledStorageProviderImpl
import co.electriccoin.zcash.ui.common.provider.KeystoneSDKProvider
import co.electriccoin.zcash.ui.common.provider.KeystoneSDKProviderImpl
import co.electriccoin.zcash.ui.common.provider.KtorNearApiProvider
import co.electriccoin.zcash.ui.common.provider.LightWalletEndpointProvider
import co.electriccoin.zcash.ui.common.provider.NearApiProvider
import co.electriccoin.zcash.ui.common.provider.PersistableWalletProvider
import co.electriccoin.zcash.ui.common.provider.PersistableWalletProviderImpl
import co.electriccoin.zcash.ui.common.provider.RestoreTimestampStorageProvider
import co.electriccoin.zcash.ui.common.provider.RestoreTimestampStorageProviderImpl
import co.electriccoin.zcash.ui.common.provider.SelectedAccountUUIDProvider
import co.electriccoin.zcash.ui.common.provider.SelectedAccountUUIDProviderImpl
import co.electriccoin.zcash.ui.common.provider.ShieldFundsInfoProvider
import co.electriccoin.zcash.ui.common.provider.ShieldFundsInfoProviderImpl
import co.electriccoin.zcash.ui.common.provider.SimpleSwapAssetProvider
import co.electriccoin.zcash.ui.common.provider.SimpleSwapAssetProviderImpl
import co.electriccoin.zcash.ui.common.provider.SwapAssetProvider
import co.electriccoin.zcash.ui.common.provider.SwapAssetProviderImpl
import co.electriccoin.zcash.ui.common.provider.SynchronizerProvider
import co.electriccoin.zcash.ui.common.provider.SynchronizerProviderImpl
import co.electriccoin.zcash.ui.common.provider.TokenIconProvider
import co.electriccoin.zcash.ui.common.provider.TokenIconProviderImpl
import co.electriccoin.zcash.ui.common.provider.TokenNameProvider
import co.electriccoin.zcash.ui.common.provider.TokenNameProviderImpl
import co.electriccoin.zcash.ui.common.provider.WalletBackupConsentStorageProvider
import co.electriccoin.zcash.ui.common.provider.WalletBackupConsentStorageProviderImpl
import co.electriccoin.zcash.ui.common.provider.WalletBackupFlagStorageProvider
import co.electriccoin.zcash.ui.common.provider.WalletBackupFlagStorageProviderImpl
import co.electriccoin.zcash.ui.common.provider.WalletBackupRemindMeCountStorageProvider
import co.electriccoin.zcash.ui.common.provider.WalletBackupRemindMeCountStorageProviderImpl
import co.electriccoin.zcash.ui.common.provider.WalletBackupRemindMeTimestampStorageProvider
import co.electriccoin.zcash.ui.common.provider.WalletBackupRemindMeTimestampStorageProviderImpl
import co.electriccoin.zcash.ui.common.provider.WalletRestoringStateProvider
import co.electriccoin.zcash.ui.common.provider.WalletRestoringStateProviderImpl
import co.electriccoin.zcash.ui.BuildConfig
import co.electriccoin.zcash.spackle.Twig
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.justzappit.evm.hd.EvmKey
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.rpc.RpcHttpClient
import xyz.justzappit.evm.signer.EoaSigner
import xyz.justzappit.offramp.account.DevOfframpAccountProvider
import xyz.justzappit.offramp.account.OfframpAccountProvider
import xyz.justzappit.offramp.config.P2pConfigProvider
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.config.P2pNetworks
import xyz.justzappit.offramp.p2p.SubgraphClient
import java.util.Locale

const val OFFRAMP_HTTP_CLIENT_QUALIFIER = "offramp_http"

val providerModule =
    module {
        factoryOf(::LightWalletEndpointProvider)
        singleOf(::GetVersionInfoProvider)
        singleOf(::GetZcashCurrencyProvider)
        singleOf(::SelectedAccountUUIDProviderImpl) bind SelectedAccountUUIDProvider::class
        singleOf(::PersistableWalletProviderImpl) bind PersistableWalletProvider::class
        singleOf(::SynchronizerProviderImpl) bind SynchronizerProvider::class
        singleOf(::ApplicationStateProviderImpl) bind ApplicationStateProvider::class
        singleOf(::RestoreTimestampStorageProviderImpl) bind RestoreTimestampStorageProvider::class
        singleOf(::WalletBackupRemindMeCountStorageProviderImpl) bind
            WalletBackupRemindMeCountStorageProvider::class
        singleOf(::WalletBackupRemindMeTimestampStorageProviderImpl) bind
            WalletBackupRemindMeTimestampStorageProvider::class
        singleOf(::WalletBackupFlagStorageProviderImpl) bind WalletBackupFlagStorageProvider::class
        singleOf(::WalletBackupConsentStorageProviderImpl) bind WalletBackupConsentStorageProvider::class
        singleOf(::WalletRestoringStateProviderImpl) bind WalletRestoringStateProvider::class
        singleOf(::CrashReportingStorageProviderImpl) bind CrashReportingStorageProvider::class
        singleOf(::ShieldFundsInfoProviderImpl) bind ShieldFundsInfoProvider::class
        singleOf(::IsExchangeRateEnabledStorageProviderImpl) bind IsExchangeRateEnabledStorageProvider::class
        singleOf(::IsTorEnabledStorageProviderImpl) bind IsTorEnabledStorageProvider::class
        singleOf(::BlockchainProviderImpl) bind BlockchainProvider::class
        singleOf(::TokenIconProviderImpl) bind TokenIconProvider::class
        singleOf(::TokenNameProviderImpl) bind TokenNameProvider::class
        singleOf(::KtorNearApiProvider) bind NearApiProvider::class
        factoryOf(::HttpClientProviderImpl) bind HttpClientProvider::class
        factoryOf(::SimpleSwapAssetProviderImpl) bind SimpleSwapAssetProvider::class
        factoryOf(::SwapAssetProviderImpl) bind SwapAssetProvider::class
        factoryOf(::IsKeepScreenOnDuringRestoreProviderImpl) bind IsKeepScreenOnDuringRestoreProvider::class
        singleOf(::EphemeralAddressStorageProviderImpl) bind EphemeralAddressStorageProvider::class
        singleOf(::CMCApiProviderImpl) bind CMCApiProvider::class
        factoryOf(::KeystoneSDKProviderImpl) bind KeystoneSDKProvider::class
        singleOf(::ChatSendContextProvider)

        // UPI offramp infrastructure (evm-lib + offramp-lib config wiring).
        single<HttpClient>(named(OFFRAMP_HTTP_CLIENT_QUALIFIER)) { RpcHttpClient.create() }
        single<P2pConfigProvider> {
            when (BuildConfig.P2P_NETWORK.lowercase(Locale.ROOT)) {
                P2pNetworks.MAINNET_NAME -> P2pConfigProvider(
                    networkName = P2pNetworks.MAINNET_NAME,
                    rpcUrlOverride = BuildConfig.P2P_RPC_URL_BASE_MAINNET.takeIf { it.isNotBlank() },
                    subgraphUrlOverride = BuildConfig.P2P_SUBGRAPH_URL_MAINNET.takeIf { it.isNotBlank() },
                )
                else -> P2pConfigProvider(
                    networkName = P2pNetworks.SEPOLIA_NAME,
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
            BaseRpcClient(httpClient = get(named(OFFRAMP_HTTP_CLIENT_QUALIFIER)), rpcUrl = cfg.rpcUrl)
        }
        single<SubgraphClient> {
            val cfg = get<P2pNetworkConfig>()
            SubgraphClient(httpClient = get(named(OFFRAMP_HTTP_CLIENT_QUALIFIER)), subgraphUrl = cfg.subgraphUrl)
        }
        single<OfframpAccountProvider> {
            val cfg = get<P2pNetworkConfig>()
            check(cfg.chainId != P2pNetworks.MAINNET_CHAIN_ID) {
                "UPI offramp is not wired for mainnet — refusing to expose DevOfframpAccountProvider" +
                    " (would sign mainnet txs with the committed dev key)."
            }
            DevOfframpAccountProvider
        }
        single<EvmKey> {
            // Resolve the provider first so its mainnet-safety check runs before the key escapes.
            get<OfframpAccountProvider>()
            DevOfframpAccountProvider.key
        }
        single<EoaSigner> {
            EoaSigner(
                rpc = get(),
                chainId = get<P2pNetworkConfig>().chainId,
                account = get(),
            )
        }

        // Twig-shaped logger fn pulled out as a reusable single, used by FallbackOrderReader.
        single<(String, Throwable?) -> Unit>(named("offramp_warn")) {
            { msg, cause ->
                if (cause != null) Twig.warn(cause) { msg } else Twig.warn { msg }
            }
        }
    }
