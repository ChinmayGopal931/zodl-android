# P2P.me Diamond — Reconstructed Facet Sources

**These are NOT the team's original source.** They are logic-equivalent reconstructions hand-refactored from `../facets/f*/decompiled.sol` (heimdall-rs output), cross-referenced with `~/dev/p2pdotme-sdk` ABIs, `KnownContractErrors.kt`, and on-chain storage access patterns.

## What's accurate

- **Function signatures** when matched against SDK ABIs or OpenChain — these are canonical.
- **Storage slots accessed** per function — heimdall's tracing of SLOAD/SSTORE is reliable.
- **Custom error selectors** — verified against KnownContractErrors (sourced from p2pdotme-sdk).
- **Cross-facet staticcalls** — the selector being called and the slot holding the target address are reliable.
- **Numeric constants** — magic numbers like the 180s deadline (`0xb4`), 86400s/day (`0x015180`), 1000-cap (`0x03e8`) are byte-exact.

## What's reconstructed (not original)

- **Struct field names and layouts** — only field count and packing positions are recovered; semantic names are inferred from usage.
- **Variable names** — synthesized from context (`var_a`, `storage_map_c` → `orders`, `_user`, etc.).
- **Modifier extraction** — heimdall inlines all modifiers; we re-extract `onlySuperAdmin`, `nonReentrant`, etc. based on the repeated patterns.
- **Control flow cleanup** — heimdall produces `require(0, X)` patterns for unreachable branches; we re-flow into if/else.
- **Event field names** — events without canonical names are inferred from emit-site arguments.

## What's lost

- **Internal/private helper function boundaries** — the compiler inlines these; we cannot recover them.
- **Inheritance hierarchy** — these were `OrderFlowFacet is Modifiers, EventEmitters, ...` upstream; we collapse to a single contract.
- **Original comments** — gone.
- **Optimizer-rewritten control flow** — short-circuiting and loop unrolling are not undone.

## Files

- `f09_OrderFlowFacet.sol` — 7 selectors (`acceptOrder`, `paidBuyOrder`, `completeOrder`, 4 date helpers)
- `f12_AdminConfigFacet.sol` — 49 admin setters (most thin, ~5 substantial)
- `f15_GettersFacet.sol` — 82 view functions (most read a single storage slot)

Each file annotates selectors as `/// @custom:selector 0xXXXXXXXX` so you can cross-reference back to `../facets/f*/decompiled.sol` (the raw heimdall output) and `../selectors/master.json` (the canonical signature table).

## Caveat: the Diamond is upgradeable

If `DiamondCut` has fired since this snapshot, facet addresses and selector routing may have changed. Re-run the pipeline in `../README.md` before relying on these.
