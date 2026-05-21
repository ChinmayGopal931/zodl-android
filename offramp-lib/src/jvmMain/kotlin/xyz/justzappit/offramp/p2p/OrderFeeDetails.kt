package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.abi.AbiDecoder

/**
 * Post-execution detail bundle the p2p.me Diamond returns from `getAdditionalOrderDetails(uint256)`.
 * Authoritative source for the "you sent / fee / you received" breakdown — subgraph snapshots
 * have the amounts but never `fixedFeePaid` (the contract's per-order fixed fee, distinct from
 * any circle fee rate).
 *
 * Fields populate progressively as the order moves: [acceptedAtEpochSeconds] flips non-null on
 * ACCEPTED, [paidAtEpochSeconds] on PAID, and [fixedFeePaid] / [actualUsdcAmount] /
 * [actualFiatAmount] settle to their final values on COMPLETED. Reading earlier is safe but
 * gives zeros for fields not yet written by the contract.
 */
data class OrderFeeDetails(
    val fixedFeePaid: Usdc6,
    val tipsPaid: Usdc6,
    val acceptedAtEpochSeconds: Long?,
    val paidAtEpochSeconds: Long?,
    val actualUsdcAmount: Usdc6,
    val actualFiatAmount: Usdc6,
)

object OrderFeeDetailsDecoder {
    /**
     * Decodes the `getAdditionalOrderDetails` return — a top-level tuple with all-static fields,
     * so no leading offset pointer (cf. `getOrdersById`, whose tuple is dynamic). The 7 words
     * land back-to-back: uint64(fixedFeePaid), uint64(tipsPaid), uint128(acceptedTimestamp),
     * uint128(paidTimestamp), uint128(reserved2), uint256(actualUsdtAmount), uint256(actualFiatAmount).
     * Each is left-padded to 32 bytes per ABI spec.
     */
    fun decode(returnData: ByteArray): OrderFeeDetails {
        val d = AbiDecoder(returnData)
        d.requireWords(TUPLE_FIELDS)
        return OrderFeeDetails(
            fixedFeePaid = Usdc6(d.uint(FIELD_FIXED_FEE_PAID)),
            tipsPaid = Usdc6(d.uint(FIELD_TIPS_PAID)),
            acceptedAtEpochSeconds = d.uint(FIELD_ACCEPTED_TS).toLong().takeIf { it > 0 },
            paidAtEpochSeconds = d.uint(FIELD_PAID_TS).toLong().takeIf { it > 0 },
            // FIELD_RESERVED2 intentionally skipped — contract reserves it for future use.
            actualUsdcAmount = Usdc6(d.uint(FIELD_ACTUAL_USDC)),
            actualFiatAmount = Usdc6(d.uint(FIELD_ACTUAL_FIAT)),
        )
    }

    private const val FIELD_FIXED_FEE_PAID = 0
    private const val FIELD_TIPS_PAID = 1
    private const val FIELD_ACCEPTED_TS = 2
    private const val FIELD_PAID_TS = 3
    private const val FIELD_ACTUAL_USDC = 5
    private const val FIELD_ACTUAL_FIAT = 6
    private const val TUPLE_FIELDS = 7
}
