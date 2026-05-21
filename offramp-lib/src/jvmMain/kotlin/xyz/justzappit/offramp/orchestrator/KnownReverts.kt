package xyz.justzappit.offramp.orchestrator

import xyz.justzappit.evm.abi.Selector4
import xyz.justzappit.evm.rpc.RpcException

/**
 * Two-layer revert decoder:
 *
 * 1. [explain] returns a [KnownRevertReason] for selectors we want to surface with a
 *    user-actionable, localised message. Curated against the PAY flow.
 * 2. [sdkName] falls through to the wholesale [KnownContractErrors] table (generated from
 *    `user-app-client/src/lib/errors.ts`) — returns the SDK's canonical error constant string
 *    so the UI can render "Contract error: BUY_ORDER_AMOUNT_EXCEEDS_LIMIT" for anything outside
 *    the curated set instead of dumping a raw 4-byte selector.
 *
 * The orchestrator populates both fields on [OfframpStatus.Failed]; the VM prefers the curated
 * `knownRevertReason` over `sdkErrorName` over `solidityErrorString` over the raw `message`.
 */
object KnownReverts {
    private val CURATED: Map<Selector4, KnownRevertReason> = mapOf(
        Selector4.fromHex("0x91da284f") to KnownRevertReason.BuyOrderAmountExceedsLimit,
        Selector4.fromHex("0x412dd2b1") to KnownRevertReason.InsufficientReputation,
        Selector4.fromHex("0x65f577de") to KnownRevertReason.ZkVerificationRequired,
        Selector4.fromHex("0xf42e41a1") to KnownRevertReason.OrderAmountExceedsLimit,
        Selector4.fromHex("0xbba2edf9") to KnownRevertReason.SellAmountExceedsFiatLimit,
        Selector4.fromHex("0x02a6fdd2") to KnownRevertReason.CurrencyNotSupported,
        Selector4.fromHex("0xebb6f34b") to KnownRevertReason.UserIsBlacklisted,
        Selector4.fromHex("0x4bbac5de") to KnownRevertReason.ExchangeNotOperational,
        Selector4.fromHex("0x5d04ff4c") to KnownRevertReason.NotEnoughEligibleMerchants,
        Selector4.fromHex("0xc56873ba") to KnownRevertReason.OrderExpired,
        Selector4.fromHex("0xc1654697") to KnownRevertReason.UpiAlreadySent,
        Selector4.fromHex("0xaa60ec26") to KnownRevertReason.InvalidOrderUpi,
        Selector4.fromHex("0x6b1b90b4") to KnownRevertReason.OrderNotAccepted,
        // Three Diamond variants for "USDC transferFrom failed" collapse to one user-facing reason.
        Selector4.fromHex("0x149f9fca") to KnownRevertReason.UsdcTransferFailed,
        Selector4.fromHex("0x47bfece5") to KnownRevertReason.UsdcTransferFailed,
        Selector4.fromHex("0x279bbc0c") to KnownRevertReason.UsdcTransferFailed,
    )

    fun explain(reverted: RpcException.ExecutionReverted): KnownRevertReason? =
        reverted.selector?.let { CURATED[it] }

    fun explain(selector: Selector4?): KnownRevertReason? = selector?.let { CURATED[it] }

    /** Long-tail SDK error name for any selector the wholesale table knows. */
    fun sdkName(reverted: RpcException.ExecutionReverted): String? =
        KnownContractErrors.nameFor(reverted.selector)

    fun sdkName(selector: Selector4?): String? = KnownContractErrors.nameFor(selector)
}
