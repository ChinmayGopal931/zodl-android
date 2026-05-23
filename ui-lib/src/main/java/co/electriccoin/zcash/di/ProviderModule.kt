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
import co.electriccoin.zcash.ui.common.provider.OfframpCheckpointStorageProvider
import co.electriccoin.zcash.ui.common.provider.OfframpCheckpointStorageProviderImpl
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
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.rpc.BundlerClient
import xyz.justzappit.evm.rpc.RpcHttpClient
import co.electriccoin.zcash.ui.common.provider.WalletSeedPhraseSource
import xyz.justzappit.offramp.account.DevOfframpAccountProvider
import xyz.justzappit.offramp.account.OfframpAccountProvider
import xyz.justzappit.offramp.account.SeedPhraseSource
import xyz.justzappit.offramp.account.SmartOfframpAccountProvider
import xyz.justzappit.offramp.account.StaticOfframpAccountProvider
import xyz.justzappit.offramp.config.P2pConfigProvider
import co.electriccoin.zcash.ui.common.provider.NearBridgeOfframpFunding
import co.electriccoin.zcash.ui.common.provider.NearPullbackOfframpRefund
import co.electriccoin.zcash.ui.common.provider.OfframpBridgeWallet
import co.electriccoin.zcash.ui.common.provider.RealOfframpBridgeWallet
import xyz.justzappit.offramp.funding.NoRouteOfframpRefund
import xyz.justzappit.offramp.funding.OfframpFunding
import xyz.justzappit.offramp.funding.OfframpRefund
import xyz.justzappit.offramp.funding.PreFundedOfframpFunding
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
        singleOf(::OfframpCheckpointStorageProviderImpl) bind OfframpCheckpointStorageProvider::class
        single<HttpClient>(named(OFFRAMP_HTTP_CLIENT_QUALIFIER)) {
            // Pipe ktor's Logging plugin output through Twig so subgraph + RPC errors land in
            // logcat under our "Twig" tag with the OfframpHttp prefix. Without this, transport
            // failures (ConnectException, SSL handshake, etc.) emit no logcat trace and the only
            // signal is the orchestrator's Failed status emission — which gets rotated out of
            // the buffer before we can grab it.
            val twigLogger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    Twig.debug { "OfframpHttp $message" }
                }
            }
            RpcHttpClient.create(
                config = RpcHttpClient.Config(
                    logger = twigLogger,
                    logLevel = io.ktor.client.plugins.logging.LogLevel.INFO,
                ),
            )
        }
        single<P2pConfigProvider> {
            // Recognised values are exactly "", "sepolia", "mainnet"; blank defaults to Sepolia
            // for CI / side-by-side installs. A typo like "mainet" must not silently boot the
            // testnet build into the wrong network — fail closed instead.
            when (val net = BuildConfig.P2P_NETWORK.lowercase(Locale.ROOT)) {
                P2pNetworks.MAINNET_NAME -> P2pConfigProvider(
                    networkName = P2pNetworks.MAINNET_NAME,
                    rpcUrlOverride = BuildConfig.P2P_RPC_URL_BASE_MAINNET.takeIf { it.isNotBlank() },
                    subgraphUrlOverride = BuildConfig.P2P_SUBGRAPH_URL_MAINNET.takeIf { it.isNotBlank() },
                )
                P2pNetworks.SEPOLIA_NAME, "" -> P2pConfigProvider(
                    networkName = P2pNetworks.SEPOLIA_NAME,
                    rpcUrlOverride = BuildConfig.P2P_RPC_URL_BASE_SEPOLIA.takeIf { it.isNotBlank() }
                        ?: P2pNetworks.SEPOLIA.rpcUrl,
                    subgraphUrlOverride = BuildConfig.P2P_SUBGRAPH_URL_SEPOLIA.takeIf { it.isNotBlank() }
                        ?: P2pNetworks.SEPOLIA.subgraphUrl,
                )
                else -> error(
                    "Unknown P2P_NETWORK build flag value '$net' — expected '${P2pNetworks.SEPOLIA_NAME}', " +
                        "'${P2pNetworks.MAINNET_NAME}', or blank for the default.",
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
        single<SeedPhraseSource> { WalletSeedPhraseSource(persistableWalletProvider = get()) }
        single<OfframpAccountProvider> {
            val cfg = get<P2pNetworkConfig>()
            // Testnet keeps the committed dev key (its smart account is pre-funded for QA). Mainnet
            // derives the ERC-4337 owner key from the user's wallet seed, so they sign with their own
            // self-custodial account — never the shared dev key.
            if (cfg.chainId == P2pNetworks.MAINNET_CHAIN_ID) {
                StaticOfframpAccountProvider(seedPhraseSource = get())
            } else {
                DevOfframpAccountProvider
            }
        }
        single<BundlerClient> {
            val cfg = get<P2pNetworkConfig>()
            BundlerClient(
                httpClient = get(named(OFFRAMP_HTTP_CLIENT_QUALIFIER)),
                bundlerUrl = BundlerClient.urlFor(cfg.chainId),
                clientId = BuildConfig.THIRDWEB_CLIENT_ID,
                entryPoint = cfg.entryPointAddress,
                chainId = cfg.chainId,
            )
        }
        single<OfframpBridgeWallet> {
            RealOfframpBridgeWallet(
                accountDataSource = get(),
                zashiProposalRepository = get(),
                keystoneProposalRepository = get(),
                submitProposal = get(),
                synchronizerProvider = get(),
            )
        }
        single<OfframpFunding> {
            val cfg = get<P2pNetworkConfig>()
            // Network toggle: mainnet bridges ZEC→USDC via NEAR (reusing the swap SwapDataSource);
            // testnet expects a pre-funded account.
            if (cfg.chainId == P2pNetworks.MAINNET_CHAIN_ID) {
                NearBridgeOfframpFunding(
                    rpc = get(),
                    usdc = cfg.usdcAddress,
                    swapDataSource = get(),
                    wallet = get(),
                )
            } else {
                PreFundedOfframpFunding(rpc = get(), usdc = cfg.usdcAddress)
            }
        }
        single<OfframpRefund> {
            val cfg = get<P2pNetworkConfig>()
            // Network toggle: mainnet pulls USDC→ZEC via NEAR; testnet keeps the USDC in the account.
            if (cfg.chainId == P2pNetworks.MAINNET_CHAIN_ID) {
                NearPullbackOfframpRefund(usdc = cfg.usdcAddress, swapDataSource = get(), wallet = get())
            } else {
                NoRouteOfframpRefund()
            }
        }
        single {
            val cfg = get<P2pNetworkConfig>()
            SmartOfframpAccountProvider(
                accountProvider = get(),
                rpc = get(),
                accountFactory = cfg.accountFactoryAddress,
            )
        }

        // Twig-shaped logger fn pulled out as a reusable single, used by FallbackOrderReader.
        single<(String, Throwable?) -> Unit>(named("offramp_warn")) {
            { msg, cause ->
                if (cause != null) Twig.warn(cause) { msg } else Twig.warn { msg }
            }
        }
    }
