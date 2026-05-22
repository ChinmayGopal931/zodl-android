package xyz.justzappit.offramp.funding

import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.p2p.Usdc6

/**
 * Mainnet refund-out (doc §3.6): resolve a NEAR Intents 1-Click deposit address so the orchestrator
 * can transfer the refunded USDC to it, and NEAR delivers ZEC to the user. The quote leg
 * (originAsset=USDC on Base, destinationAsset=ZEC, recipient=user's Zcash address, refundTo=account)
 * is the remaining mainnet integration point; gated behind mainnet billing today.
 */
class NearPullbackOfframpRefund : OfframpRefund {
    override suspend fun pullbackTarget(account: Address, amount: Usdc6): Address {
        // TODO(mainnet): request a NEAR 1-Click quote (USDC→ZEC, recipient=user's Zcash address,
        //  refundTo=account) and return its deposit address (depositMode=SIMPLE). Needs the wallet's
        //  Zcash address (PersistableWalletProvider). Reuse NearSwapDataSourceImpl.
        error("NEAR pull-back to ZEC for mainnet offramp is not yet implemented")
    }
}
