package xyz.justzappit.offramp.orchestrator

/**
 * Enumerated p2p.me revert reasons the offramp flow recognises. The UI layer maps each variant
 * to a localised `R.string.*` resource; this module stays free of English copy.
 */
enum class KnownRevertReason {
    /**
     * BUY orders (and PAY orders from fresh addresses) require an RP grant from the p2p.me team
     * on Sepolia, or social/KYC verification on mainnet. Selector `0x91da284f`.
     */
    InsufficientReputation,

    /**
     * No merchant has fiat liquidity for this order in the selected circle. Selector `0x5d04ff4c`.
     */
    NoMerchantLiquidity,
}
