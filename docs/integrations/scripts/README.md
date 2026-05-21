# Offramp integration scripts

These scripts produce fixture data that's committed back into Kotlin test files.
They depend on `viem`/`@noble/*` from `p2pdotme-sdk`, so they run via:

```sh
# from inside a p2pdotme-sdk clone with `bun install` already run:
bun /path/to/zodl-android/docs/integrations/scripts/<script>.ts
```

Neither script touches the SDK source tree — they're read by bun, imports resolve
against the SDK's `node_modules`, and output goes to stdout. Paste output back
into the matching `*Test.kt` constants.

## `generate-ecies-fixture.ts`

Produces frozen ECIES test vectors that prove the Kotlin port in
`evm-lib/.../crypto/Ecies.kt` is wire-compatible with `@p2pdotme/sdk`.

The script imports from the SDK's source, so it must be run from inside a clone
of `p2pdotme-sdk` (private to us — the @noble/* deps live in that tree).

```sh
# from inside p2pdotme-sdk/
cp /path/to/zodl-android/docs/integrations/scripts/generate-ecies-fixture.ts scripts/
bun install   # if not already
bun run scripts/generate-ecies-fixture.ts
```

Paste the four `ciphertextHex` strings back into `evm-lib`'s
`EciesTest.kt` (the `SDK_FIXTURES` companion list).

Only re-run when the SDK's ECIES wire format changes — fixtures are
committed and travel with the test.

## `generate-revert-selectors.ts`

Regenerates `offramp-lib/.../orchestrator/KnownContractErrors.kt` — the wholesale
selector → SDK error-name table — by regex-parsing `user-app-client/src/lib/errors.ts`.
No node_modules required; the script is self-contained.

```sh
bun /path/to/zodl-android/docs/integrations/scripts/generate-revert-selectors.ts \
  /path/to/user-app-client \
  > /path/to/zodl-android/offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/orchestrator/KnownContractErrors.kt
```

The path arg defaults to `/Users/chinmaygopal/dev/user-app-client` if omitted.
Selectors are emitted in stable (alphabetical) order so a re-run with no source
change produces zero diff. Re-run whenever you sync to a newer `user-app-client`
release — `KnownRevertsTest` asserts the table stays ≥120 entries and that every
*curated* selector still exists in the generated table.

## `generate-calldata-fixtures.ts`

Emits viem-encoded calldata for `approve`, `placeOrder`, `setSellOrderUpi`,
`getOrdersById`, `getAssignableMerchantsFromCircle`. The ABI fragments are
inlined into the script (kept in sync manually with `p2pdotme-sdk/src/contracts/abis/`),
so it only needs viem in the cwd's `node_modules`.

These fixtures aren't yet wired into Kotlin tests — the encoder currently
self-tests against the well-known `approve` reference vector + ABI-spec
invariants. Run this script + paste output into a future
`AbiCalldataFixturesTest.kt` before the first mainnet broadcast for a
strong cross-language check.
