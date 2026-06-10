# Upstream parity audit — new-delta sweep (2026-06-10)

Multi-agent audit (63 agents: 5 topic analysts + prior-audit reconciliation + 2-lens adversarial
verification of every actionable finding + completeness critic) covering everything upstream merged
**since the 2026-06-07 audits**, plus a re-verification of every open item those audits left behind.

## Scope & method

| | |
|---|---|
| Fork HEAD | `8088ab070` (main) |
| Upstream reference | `origin/main` = `fba96432a` (zodl-inc/zodl-android, **3.5.3 backmerge**, fetched 2026-06-10) |
| Prior audited point | `95762b4fc` (the 2026-06-07 VS-ORIGIN-MAIN reference) |
| Fork merge-base | `2409c4d71` = upstream **release 3.3.1** (2026-04-13) |
| New delta audited | **45 commits / 30 files** (`git diff 95762b4fc..origin/main`) |
| Sibling checkout | `../zodl-android-real` fast-forwarded to `fba96432a` (was 13 behind) — now a valid reference |

Method: every changed file was blob-hash classified (fork = old-upstream / new-upstream / diverged /
absent), then topic agents did three-way hunk-level analysis honoring intentional divergences
(ZappTheme, P2P messaging, P2P.me offramp reusing the swap plumbing, voting out of scope, MOB-1144
deferred). Every actionable finding survived two adversarial verifiers (existence + applicability);
a completeness critic confirmed all 31 non-merge commits and all merge-resolution edits map to a
covered topic.

## Verdict

**The contract still holds — zero security or data-flow regressions in inherited code.** The fork is
at parity with the previously audited upstream point; the new 45-commit delta contains **two
port-now items (both security-relevant)**, two scheduled items, and a pile of confirmed-N/A CI/voting
infra. Of the 2026-06-07 carried-over list, the big clusters (resync, FLEX_INPUT swap, Keystone
birthday + QR-decoder-shape, KeepOpen, MOB-987, biometric) are **verified RESOLVED** by the parity PR
train (#50–#57, #60); a short tail remains open (below).

## P1 — port now (new delta)

| Area | Upstream change | Fork state | Action |
|---|---|---|---|
| **MOB-1346 ktor body-logging redaction** — `HttpClientProvider.kt` (`e9a118c6f`) | Logging plugin gated `if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE`; header sanitization extended from `Authorization`-only to also `X-CMC_PRO_API_KEY` + `X-Helper-Token` (new `SANITIZED_HEADERS` set). | Fork still logs `LogLevel.ALL` unconditionally and sanitizes only `Authorization`. The shared client carries the **CMC API key** and **every NEAR quote / offramp-funding body**. Today the only protections are `IS_MINIFY_ENABLED` + the Zcash SDK's *transitive* proguard rule — both out-of-repo accidents. With a manual Play release pending and a prior log-leak incident, this is the **top-priority port in the delta**. | Hunk-level port into the fork's diverged file (3 hunks, listed in commit `e9a118c6f`). Adapt the comment's payload list (fork: swap + offramp bridge, no voting). Fork-only `evm-lib` RpcHttpClient verified already body-safe (optional release gate). Chat/zappMessaging has no Android-side HTTP clients — N/A there. |
| **MOB-1346 proguard `android.util.Log` strip** — `spackle-android-lib/proguard-consumer.txt` | New 13-line `-assumenosideeffects class android.util.Log` rule so release builds strip direct `Log` calls via the app's *own* rule instead of the SDK's transitive one. | Fork file is byte-identical to old upstream (Twig-only block). Precondition already satisfied: `proguard-android-optimize.txt` in use (`app/build.gradle.kts:151`), `IS_MINIFY_ENABLED=true`. | Append the upstream hunk verbatim. |
| **MOB-1340/1345 swap-quote validation hardening** — `model/SwapQuote.kt`, `model/SwapQuoteStatus.kt`, new `model/near/NearSwapQuote.kt` + `near/NearSwapQuoteStatus.kt`, `near/QuoteResponseDto.kt`, `NearSwapDataSourceImpl.kt`, `RequestSwapQuoteUseCase.kt`, `GetSwapStatusUseCase.kt` + 3 new test files (PR #2299: `4d9b59792`…`4022784dd`) | Concrete quote classes moved to `near/` with **fail-closed `init` require()s**: asset-id echo match, positive formatted amount, raw-vs-formatted consistency at asset decimals, client-snapshotted slippage-tolerance echo, slippage floor/ceiling vs the server's worst-case guarantee. Use-case `validateQuote` layer: user-amount echo, asset-snapshot, destination/refund **address echo** (`requestNextShieldedAddress` hoisted pre-suspension). `isTerminal` fix stops status polling on FAILED/EXPIRED. `4022784dd` fixes an off-by-one (DECIMAL128 floor/ceiling vs integer-truncated server guarantee) without which **every real quote is rejected**. | Fork has **none** of it: 4 files byte-identical to pre-change upstream; diverged `RequestSwapQuoteUseCase`/`NearSwapDataSourceImpl` lack all validation. Fork trusts the 1-Click server's echo completely on an irreversible-send money path — and the **offramp funding bridge rides the same plumbing**. | Port as **one unit from final upstream blobs** (never `84439d098` without `4022784dd`). Rename blast radius verified tiny: concrete classes referenced only in `NearSwapDataSourceImpl`; all offramp/chat consumers bind the unmoved `SwapQuote`/`SwapQuoteStatus` interfaces. **Keep-fork hunks:** `referral = "zapp"`, `flexInput` param, merged error-asset branches. **Fork extras required:** (1) the fork-only third entry point `RequestSwapQuoteUseCase.requestExactInputIntoZec` (`:81`, caller `SwapVM.kt:358`) needs its own `validateQuote` block — a verbatim port leaves the USDC→ZEC reverse-swap unvalidated; (2) `OfframpBridgeWallet` calls `swapDataSource.requestQuote` directly, bypassing the use-case layer — add equivalent address/amount echo checks at `openBridge` (before `sendZecDeposit`, `:194`) and `pullbackTarget`; (3) port the 3 test files together with the main sources. |

## P2 — scheduled (new delta)

| Area | Upstream change | Action |
|---|---|---|
| **`ef77228a2` fastestEndpoints coroutines rewrite** — `WalletRepository.kt` | Replaces the two-coroutine `channelFlow` + intermediate `MutableStateFlow` pipeline with a single linear flow chain; hoists the previous-result cache to a class field so it survives `WhileSubscribed` restarts; `refreshFastestServers()` uses `.value` instead of `first()`. **Distinct from** the older fastestEndpoints rewrite already ported via PR #60 — do not confuse the two (the 06-07 reconciliation line "fastestEndpoints rewrite RESOLVED" refers to the old one). | Hunk-level port into the fork's diverged file (fork `WalletRepository.kt:129-198`), keeping fork `walletProvisioningError` hooks. Skip the upstream-sibling `SynchronizerProvider.getSynchronizerOrNull()` — verified dead code (zero callers both trees), provenance is the deferred MOB-1144 cluster; record in the deferral ledger. |
| **SDK 2.6.2-SNAPSHOT bump** — `gradle.properties` (`9cb518f3c`) | Upstream main now builds against SDK `2.6.2-SNAPSHOT` = SDK branch `release/snapshot-v2.6.2` (tip `216ae477`), **17 commits ahead of the fork's pin** `27af78d3`: new-wallet tree-state fast-sync + its reorg-safety fix (`15313529`), 5 new checkpoints, SDK-side MOB-1124 multi-currency scaffolding. Nothing consensus/NU-critical; no app code in the audited delta uses new SDK API. | Coordinated two-part bump as upstream-sync work (per CLAUDE.md, not unilateral): `ZCASH_SDK_VERSION=2.6.2-SNAPSHOT` + `.zapp-deps` pin → `216ae477` with comment-block update. Not release-blocking — includeBuild substitution ignores the Maven version. |

## Confirmed N/A (new delta)

- **Voting (MOB-1358 `1702f8bf6`, layout fix `0663b5450`)** — touch only `VoteConfirmSubmissionComponents.kt`; fork has zero voting code (grep-verified). Out of scope by standing decision.
- **MOB-1336 decommissioned servers (`216369a5d`) + the 3-line `ZcashApplication.kt` delta** — *already in the fork* (ported 2026-06-07 as `09cc1cb15` from the release/3.5.3 branch before upstream backmerged it; includes `getDecommissionedHosts()`, `migrateDecommissionedEndpointIfNeeded()`, `walletRepository.init()`). Only looked new because the backmerge post-dates the prior audit point.
- **CI/release infra** — `create-release.yml`, `e2e-smoke.yml` (absent in fork), `release.yaml` overhaul (AWS GPG signing, F-Droid publish), F-Droid README rewrite: fork releases are manual, fork CI is separate. Action SHA-pinning "gap" was **adversarially refuted** — upstream pinned only two actions in a workflow the fork doesn't have, and still tag-pins the same actions the fork tag-pins. The e2e USDC→ZEC swap-verification rig (`cadb40346`) is an FYI pattern for future offramp testing.
- **`SkipRemainingKeystoneBundlesUseCaseTest` 2-line delta** — merge-resolution edit keeping a **voting** test compiling (the use case is part of the voting feature despite its name); fork lacks the feature. Fork does **not** systematically lack shared ui-lib unit tests: upstream's 25 vs fork's count differ almost entirely by 21 voting tests + the 3 new swap tests (ported with P1).
- **Docs/changelogs** (`CHANGELOG.md`, `README.md`, whatsNew EN/ES, fastlane `1745.txt`, `ZCASH_VERSION_NAME=3.5.3`) — intentional fork divergence; 3.5.3's sole user-facing change (MOB-1336) is already in fork code.

## Carried-over items from 2026-06-07 — reconciled against fork HEAD

**Verified RESOLVED by the parity PR train (#50–#57, #60)** — each adversarially re-checked:
MOB-1336 servers, memtagMode, HttpTimeout/log-chunking, exchange-rate `?: true` revert, the full
MOB-1139 resync cluster + UI entry, MOB-1122 FLEX_INPUT (incl. SWAP_INTO_ZEC), Keystone birthday
import + sub-flow screens, KeepOpen flow + home-nav switch, MOB-987 locale formatter (+ ZatoshiExt
orphans gone), biometric cancel-mapping, old fastestEndpoints rewrite + suspend
`updateWalletEndpoint`, ParseKeystonePCZT→KeystoneSDKProvider, LceState/VmHelpers infra,
WalletAccount `requireNotNull`, and both SUPPLEMENT §4 false-positives (Flexa submit-success and
proposal log redaction — upstream *reverted* the server-broadcast feature in `e0c9cf154`, so those
files are byte-identical to upstream again).

**Correction from adversarial review:** *Keystone per-scan-session QR decoder reset is NOT resolved*
— the initial reconciliation mistook the long-present MOB-1100 error-path reset for it. The real
feature (`scanSessionId` / `resetDecoderForNewSession`, second reset call site at upstream
`ParseKeystonePCZTUseCase.kt:97`) arrived in upstream's **coinholder-polling** commit (`2f204beed`)
and remains deferred-with-voting.

**STILL OPEN (actionable tail, unchanged recommendations):**

1. **MOB-1190 `OptInExchangeRateAndTorUseCase`** — fork still force-disables currency conversion on
   Tor opt-out. Smallest, highest-value behavioral fix; no DI churn. *(MEDIUM)*
2. **Keystone connect tutorial entry point** — explainer registered but unreachable; 2-file fix
   (`ConnectKeystoneScreen.kt` no-op → `forward(KeystoneExplainerScreenArgs)`, `ic_info` app-bar
   button in `ConnectKeystoneView.kt`). *(MEDIUM)*
3. **Restore-date LceState migration** — estimate errors still swallowed, no spinner; siblings
   already migrated. *(MEDIUM)*
4. **SelectKeystoneAccount cleanup** + **DeriveKeystoneAccountUnifiedAddressUseCase** full-address /
   middle-ellipsize (use case still head-truncates at `:18`). *(LOW)*
5. **Delete dead `restoresuccess/` package** — superseded by KeepOpen, still shipped + registered
   (`ViewModelModule.kt:118`, `WalletNavGraph.kt:349`), no caller. *(LOW)*
6. **LOW polish batch:** swap-slippage `ZashiDisclaimer` migration (component already in fork),
   `testTagsAsResourceId` instrumentation, `InfoBottomSheetView` dedup (5 sheets), hardcoded
   `0xFF34C759` in `RestoreTorView.kt:154`.
7. **PARTIAL:** RestoreTor failure surfacing landed (`1c3bea48a`); remaining LOW = no `isLoading`
   spinner on the primary button + missing test tag.

**DEFERRED BY STANDING DECISION (unchanged):** Coinholder Voting (whole feature incl. the QR
scan-session reset and `SkipRemainingKeystoneBundles*`), MOB-1144 automatic server selection
(+ ChooseServer rewrite + `getSynchronizerOrNull`), restore-subtree verbatim renames, design-lib
widget batch, M3 tooltip, MOB-1315, settings.gradle zcash-sdk-backend catalog entry.

## Recommended action plan

1. **Port MOB-1346 now** (ktor gate + headers + proguard rule) — release-blocking-adjacent given the
   pending Play upload.
2. **Port MOB-1340/1345 swap hardening now** as one unit with the fork extras (third entry point,
   offramp bypass checks, keep-fork hunks, 3 tests).
3. **Fold in MOB-1190 + Keystone tutorial entry + restore-date LceState** (carried-over tail) in the
   same parity train.
4. **Schedule** the `ef77228a2` fastestEndpoints rewrite and the coordinated SDK 2.6.2 pin bump.
5. Keep the deferral ledger (Voting, MOB-1144) recorded in `docs/FORK_MAINTENANCE_GUIDE.md` so future
   sweeps stop re-flagging.
