package xyz.justzappit.offramp.funding

import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.p2p.Usdc6

/**
 * Decides where a cancelled order's refunded USDC should go to return to the user as ZEC. The
 * orchestrator does the sponsored `USDC.transfer` itself; this seam only resolves the destination,
 * so the NEAR-specific logic stays isolated and network-toggled.
 *
 * Mainnet resolves a NEAR Intents 1-Click deposit address (USDC→ZEC, §3.6). Testnet returns null —
 * no NEAR route — and the refunded USDC simply stays in the self-custodial smart account (safe).
 */
fun interface OfframpRefund {
    /** Base address to send the refunded USDC to, or null to leave it in the account (testnet). */
    suspend fun pullbackTarget(account: Address, amount: Usdc6): Address?
}

/** Testnet/dev: no NEAR route. The refunded USDC remains in the smart account (already self-custodial). */
class NoRouteOfframpRefund : OfframpRefund {
    override suspend fun pullbackTarget(account: Address, amount: Usdc6): Address? = null
}
