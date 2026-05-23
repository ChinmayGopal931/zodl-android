package xyz.justzappit.offramp.orchestrator

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.rpc.BundlerClient
import xyz.justzappit.evm.signer.Erc4337Submitter
import xyz.justzappit.offramp.account.SmartOfframpAccountProvider
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.funding.OfframpFunding
import xyz.justzappit.offramp.funding.OfframpRefund
import java.math.BigInteger
import xyz.justzappit.offramp.p2p.CircleRouter
import xyz.justzappit.offramp.p2p.OrderReadSource
import xyz.justzappit.offramp.p2p.SubgraphClient

/**
 * Production [OfframpDriver] for the ERC-4337 path. The smart-account address needs an async
 * `factory.getAddress` call, which Koin's synchronous factories can't await — so resolution and
 * submitter construction happen lazily inside the flow (a suspend context), then delegate to a
 * plain [OfframpOrchestrator]. Tests drive the orchestrator directly with an EOA [TxSubmitter].
 */
class AaOfframpDriver(
    private val rpc: BaseRpcClient,
    private val bundler: BundlerClient,
    private val network: P2pNetworkConfig,
    private val accountProvider: SmartOfframpAccountProvider,
    private val subgraph: SubgraphClient,
    private val orderReader: OrderReadSource,
    private val funding: OfframpFunding,
    private val refund: OfframpRefund,
    private val router: CircleRouter = CircleRouter(),
) : OfframpDriver {
    override fun run(request: OfframpRequest): Flow<OfframpStatus> = flow {
        emitAll(buildOrchestrator().run(request))
    }

    override fun resume(checkpoint: OfframpCheckpoint): Flow<OfframpStatus> = flow {
        emitAll(buildOrchestrator().resume(checkpoint))
    }

    override fun bridgeFundsBackToZec(orderId: BigInteger?): Flow<OfframpStatus> = flow {
        emitAll(buildOrchestrator().bridgeFundsBackToZec(orderId))
    }

    private suspend fun buildOrchestrator(): OfframpOrchestrator {
        val account = accountProvider.resolve()
        val submitter = Erc4337Submitter(
            rpc = rpc,
            bundler = bundler,
            entryPoint = network.entryPointAddress,
            accountFactory = network.accountFactoryAddress,
            owner = account.owner,
            smartAccount = account.address,
            chainId = network.chainId,
        )
        return OfframpOrchestrator(
            rpc = rpc,
            submitter = submitter,
            accountAddress = account.address,
            network = network,
            subgraph = subgraph,
            orderReader = orderReader,
            funding = funding,
            refund = refund,
            router = router,
        )
    }
}
