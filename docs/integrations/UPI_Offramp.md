# UPI Offramp Integration — Research & Architecture Brief v2

Status: **research / not yet implemented** (supersedes v1)
Owner: Chinmay
Last updated: 2026-05-20

## How to use this document

Read top to bottom on first pass — the strategy questions in §6–§8 depend on the protocol facts in §4–§5. If you're just looking for the recommendation: jump to §8 (path), §13 (prototype), §15 (next steps).

Sibling repos:
- `../../p2pdotme-sdk/` — `@p2pdotme/sdk` v1.1.7
- `../../user-app-client/` — official p2p.me web frontend (open source: github.com/p2pdotme/user-app-client)
- `../zappMessaging/` — JS messaging stack already shipped via BareKit

---

## §1 Goal

Unchanged from v1. A Zodl user holds only ZEC, scans a merchant's UPI QR, and the wallet bridges ZEC → Base USDC, places a p2p.me PAY order, ECIES-encrypts the UPI handle for the matched merchant, and settles when the merchant confirms INR receipt. No new credential, no third-party login.

---

## §2 Verified addresses & endpoints

**Base mainnet (production)** — confirmed by p2p.me core team on Discord 2026-04-28:
```
DIAMOND   = 0x4cad6eC90e65baBec9335cAd728DDC610c316368
REPUTATION_MANAGER = 0xCF613e08EE1B4c2669DdCf06A7d22c9856f6Aa1D
USDC      = 0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913  (Circle's canonical Base USDC)
SUBGRAPH  = self-deploy required (github.com/p2pdotme/subgraph)
```

**Base Sepolia (testing)** — confirmed by Bucky 2026-04-23:
```
DIAMOND   = 0xce868398FDaDcA368EAc203222874D6888532aE2
REPUTATION_MANAGER = 0x45919D69E2154F46b6f6eA42ae23d2e9ee21B66f
USDC      = 0xDABa329Ed949f28F64019f22c33c3B253B2Ded60
SUBGRAPH  = https://api.studio.thegraph.com/query/110312/indexer-one/version/latest
```

**Open issues from Discord:**
- INR SELL merchant bot was not accepting orders on Sepolia as of 2026-04-23 (`acceptedMerchant` stayed `0x0...` indefinitely on orderIds 17/18/19). Verify before integration tests.
- BUY orders revert globally with custom error `0x91da284f` until the calling address has enough RP. RP is granted manually by team on testnet; on mainnet it accrues via verification (§4.6).

---

## §3 Zodl primitives we reuse

(Unchanged from v1 — code-verified.)

- **NEAR Intents bridge** — `NearSwapDataSourceImpl` already supports ZEC → Base USDC. We just hand it the destination address.
- **Seed access** — `PersistableWalletProvider.persistableWallet.first()` — gated by `BiometricRepository` / `PinAuthGate`. Precedent: `ChatViewModel.restoreFromWalletSeed`.
- **QR scanner** — `ScanView` (CameraX + ML Kit/ZXing). UPI parser is trivial.
- **Progress UI** — `TransactionProgressView` + `TransactionProgressState`. Already Swiss-design success/pending/error driven by an enum.
- **Auth** — biometric + PIN already wired; same gate covers EVM key derivation.

No code in `evm-lib` shape exists today. Everything below is new.

---

## §4 p2p.me protocol — what the SDK actually does

### 4.1 SDK shape (`@p2pdotme/sdk` v1.1.7)

Subpath exports per the architecture doc:

```
@p2pdotme/sdk
├── /orders       reads + write actions (prepare/execute pairs)
├── /prices       getPriceConfig, getReputationPerUsdcLimit
├── /profile      USDC balance/allowance/tx-limits/fiat balances
├── /qr-parsers   UPI / PIX / QRIS / MercadoPago / Pago Móvil
├── /fraud-engine (we skip)
├── /zkkyc        (we skip for v1; revisit for >$100 orders)
├── /country      currency metadata + payment-field validators
└── /react        provider + hooks (we don't use)
```

Two design properties from `architecture.md` that materially affect our integration:

1. **Wallet-agnostic.** Every write has `prepare(params) → {to, data, value}` (pure, no wallet) AND `execute({walletClient, ...params})` (signs + submits via viem). We can use `prepare()` from anywhere — Kotlin/native, no JS runtime needed for calldata construction (modulo ECIES).
2. **No environment variables read.** All config passed via factory args. Means the SDK can be reused in any host.

### 4.2 The PAY order lifecycle (verified against `example/make-pay-order.ts`)

```
1. approveUsdc.execute({amount})              ← user signs ERC-20 approve
2. placeOrder.execute({orderType:2, ...})     ← user signs; emits OrderPlaced
3. poll getOrder(id) until status == "accepted"
   → contains accepted merchant address + their pubkey
4. setSellOrderUpi.execute({orderId, paymentAddress: "merchant@upi",
                            merchantPublicKey, updatedAmount:0})
   ← ECIES-encrypts UPI; user signs
5. poll getOrder(id) until status == "completed"
   ← merchant pays UPI off-chain then calls completeOrder
```

**3 user-side write transactions** total for a PAY: approve, place, setUpi.

Correction to v1 doc: there is **no `paidBuyOrder` step in the PAY flow**. `paidBuyOrder` is for BUY orders only (where the buyer confirms they sent fiat). PAY merchants call `completeOrder` themselves once their UPI sends are confirmed. (v1 §4.4 had this wrong.)

### 4.3 External call surface (audited)

| Module | Network surface | Notes |
|---|---|---|
| orders (read) | `eth_call` to Diamond on Base | Read order, fee config |
| orders (write) | `eth_sendTransaction` to Diamond | Or `eth_sendUserOperation` if going AA |
| orders (routing) | 1 GraphQL POST to subgraph (`CirclesForRouting`) | Per `placeOrder.prepare` |
| orders (history) | 1 GraphQL POST to subgraph (`OrdersByUser`) | Optional |
| prices | `eth_call` to Diamond | `getPriceConfig`, `getRpPerUsdtLimitRational` |
| profile | `eth_call` to USDC + Diamond | Balance, allowance, tx-limits |
| qr-parsers (INR) | Pure regex on `upi://` URI | No network |
| fraud-engine | 3 proprietary POSTs to p2p.me REST + ipify + FingerprintJS + SEON | **We skip.** |
| zkkyc | Reclaim REST + 3rd-party SDKs | **We skip for v1.** |

**Conclusion:** outside fraud-engine and zkkyc, the SDK's only external dependencies are (a) Base RPC and (b) one subgraph URL. No proprietary p2p.me REST API.

### 4.4 ECIES wire format (verified in `src/orders/crypto/ecies.ts`)

Confirmed implementation:
- secp256k1 ephemeral keypair (`@noble/curves`)
- ECDH x-coordinate (32 bytes) → KDF: `SHA-512(shared) → first 32 = AES key, last 32 = MAC key`
- AES-256-CBC with random 16-byte IV
- HMAC-SHA256 over `iv || ephemPubKeyUNCOMPRESSED(65 bytes) || ciphertext`
- Wire format: `iv(16) || ephemPubKeyCOMPRESSED(33) || mac(32) || ciphertext`

**Two porting gotchas:**
1. Wire stores **compressed** pubkey (33 bytes); HMAC is over **uncompressed** (65 bytes). A Kotlin port must decompress before MAC recomputation.
2. The `cipherStringify` output is hex-encoded, single string — this is what goes on-chain as `encMerchantUpi`.

Golden vectors live at `p2pdotme-sdk/test/orders/crypto/ecies.test.ts`. Port these into a Kotlin unit test as the go/no-go gate.

### 4.5 Epsilon-greedy circle routing (verified in `src/orders/internal/routing/routing.ts`)

- `EPSILON = 0.25`, `RECOVERY_SCALE = 0.3`, `BOOTSTRAP_MAX_WEIGHT = 25`, `MAX_VALIDATION_ATTEMPTS = 3`.
- 75% exploit (active circles, weighted by `circleScore`), 25% explore (status-weighted across all eligible).
- Calls subgraph `CirclesForRouting($currency)` → filters by status `["active", "bootstrap", "paused"]`.
- For each candidate, calls `checkCircleEligibility` on-chain via `eth_call`. Retries up to 3× with failed circles removed.
- ~100 LOC pure logic.

### 4.6 Reputation Points — the hidden v1 blocker

This was not in v1 doc. It's now the #1 risk.

**What it is:** `getReputationPerUsdcLimit(currency)` returns `(numerator, denominator)` → "your USDC tx limit is `RP × denominator / numerator`" per currency. INR uses a custom multiplier; default elsewhere is 1 RP = 2 USDC.

**What the Discord shows:**
- 2026-04-23: meshjs.dev's BUY order reverts with `0x91da284f` regardless of amount, currency, addresses. Bucky: *"Buy order not working because it doesn't have enough RP — Share user address I'll give."* After manual grant, BUY works.
- Bucky: *"For amount under $100 user can pay without doing ZK verification."* (This is necessary but not sufficient — you still need baseline RP.)
- Bucky: *"User will have to do social verify to increase the RP. https://app.p2p.me/limits"*

**What we don't know empirically yet:**
- Does PAY (orderType 2) hit the same RP gate as BUY? `example/make-pay-order.ts` doesn't mention RP, but that example targets a placeholder contract.
- Is there a small-order bypass? The contract has `getSmallOrderThreshold`, `getSmallOrderFixedFeePay` — suggesting orders below a threshold use a different fee path. **Whether they bypass RP entirely is unverified.**
- Where does mainnet "initial RP" come from for a brand-new address? On testnet the team grants it manually. On mainnet the only documented path is the ZK verifications (Reclaim social, Anon Aadhaar, ZK Passport) via `app.p2p.me/limits`.

**v1 design implication:** we cannot ship offramp until we know how PAY orders behave for new users on mainnet. Two paths to clarity:
1. Empirical test on mainnet with a $5 PAY order from a fresh address. Cheapest signal.
2. Direct ask to Bucky/Git Chad: "what's the minimum RP for a PAY order, and how does a new user get there for orders <$100?"

We should do both.

### 4.7 Fees (informational)

The contract exposes per-currency fixed fees per-order-type:
- `getSmallOrderThreshold(currency)` — below this USDC amount, fixed fee applies
- `getSmallOrderFixedFee(currency)`, `getSmallOrderFixedFeeBuy/Sell/Pay(currency)` — fixed fee per order type
- Above the threshold, fee = `circle`'s `feeRate` (varies)

For UPI/INR PAY orders we should read these on app start and display the gross/net to the user. None of these are in `@p2pdotme/sdk`; we'd need to add the ABI fragments ourselves (they live in `src/contracts/abis/`).

---

## §5 Privacy posture (largely unchanged)

Same as v1:
- Bridge breaks Zcash anonymity — the moment USDC lands on Base, the receiving address is correlatable. Out of scope for this module.
- ECIES protects the UPI handle on-chain — only the matched merchant can decrypt.
- No fraud-engine import → no fingerprints/IP/SEON sent to p2p.me backend.
- One small refinement: **whatever subgraph URL we use sees every order query (orderId, user address) we make.** Self-hosting our own subgraph (or proxying through our own node) closes that leak.

---

## §6 Gas model — the truth

**Marketing:** "P2P.me DAO covers all gas costs on Base."

**Implementation (from `user-app-client/src/core/adapters/thirdweb/chain.ts:86-95`):**
```ts
inAppWallet({
  executionMode: {
    mode: "EIP4337",
    smartAccount: {
      sponsorGas: true,
      chain,
      factoryAddress: VITE_THIRDWEB_CONTRACT_ADDRESS_AA_FACTORY,
    },
  },
  ...
})
```

And `user-app-client/.env.example:82-87` confirms it's overridable:
```
# GAS SPONSORSHIP  (optional — users pay their own gas if not set)
# Coinbase CDP paymaster URL, or any EIP-4337 compatible paymaster.
VITE_BUNDLER_PAYMASTER_URL=
```

So:
- The official frontend uses **Thirdweb's ERC-4337 stack** with `sponsorGas: true`. Thirdweb's billing account (presumably funded by the DAO) pays Base gas for every UserOp.
- If the `VITE_BUNDLER_PAYMASTER_URL` env var is set, that custom paymaster is used instead (e.g. CDP).
- **The SDK itself has no gas-sponsorship logic.** It's a wallet-layer concern.

### 6.1 Our three gas options

| Option | Implementation | Pro | Con |
|---|---|---|---|
| **A. Plain EOA + bridged ETH** | HD-derive EOA `m/44'/60'/0'/0/n`. NEAR bridge delivers $0.20 of ETH alongside the USDC. Sign txs directly. | Simplest. ~600 LOC. No 4337, no paymaster, no smart-account contract. | Visible ETH dust on Base. Slight cognitive cost in UI ("we'll bridge some ETH for fees"). Marketing-misaligned with p2p's "gas-free" pitch. |
| **B. AA + our own paymaster (Pimlico / CDP)** | LightAccount on Base. Owner = HD-derived EOA. UserOps via Pimlico's bundler+paymaster. | Gas-free UX. Vendor-neutral. We control sponsorship policy. | We pay gas (~$0.05/order). ~1300 extra LOC for 4337 + paymaster. Pimlico/CDP billing account needed. |
| **C. AA + DAO-sponsored Thirdweb paymaster** | Same smart account on Base, but route UserOps through Thirdweb's bundler with their clientId. | Truly gas-free for users AND for us. Closest to "official" UX. | Depends on DAO agreement to extend sponsorship to our clientId. Couples us to Thirdweb's billing. Same ~1300 LOC cost as B. |

### 6.2 Recommendation: **start with A, plan migration to C**

Rationale:
- **A ships in ~1 week** and proves the entire offramp end-to-end on real money. No ERC-4337 engineering needed.
- The "$0.20 of ETH dust on Base" is invisible to the user if we hide it in the bridge step. The UI shows "1 ZEC → ₹X to merchant"; the bridge does ZEC → USDC + small ETH atomically.
- **C is the right end state** if we want to match p2p's UX promise — but it requires (i) implementing 4337, and (ii) a partnership conversation with the DAO. Neither is hard, but both add ~3 weeks calendar time.
- **B is a fallback** if the DAO sponsorship conversation stalls. We absorb gas (~$0.05 × order volume) as cost of business; this is bounded.

Migration cost A→C is low: the SDK calldata + ECIES + orchestrator are identical. Only the "sign + submit" boundary changes. Refactor it behind one interface (`OfframpSigner`) from day one.

### 6.3 Why I changed my mind from the v1 doc

v1 said: "skip AA entirely, plain EOA wins on simplicity, AA gives nothing for privacy." That's still true mechanically. But I underweighted: (a) the marketing alignment ("gas-free" is in p2p.me's pitch and users will expect it), (b) the fact that DAO sponsorship is a real path, and (c) the future-proofing argument. A→C migration is real, but it's a clear roadmap, not throwaway work.

---

## §7 Wallet & auth — HD from existing seed

(Unchanged conclusion, refined detail.)

### 7.1 Decision

HD-derive `m/44'/60'/0'/0/n` from the BIP-39 phrase in `PersistableWallet`. Use this EOA as either:
- (Option A) the direct sender of Base transactions, OR
- (Option B/C) the owner-signer of a LightAccount smart contract wallet

The seed already exists. Adding another derivation path costs nothing. Recovery story is unchanged (24 words restore everything: Zcash + messaging + offramp).

### 7.2 Burner per offramp

Increment `n` per offramp; store `next-n` counter in encrypted prefs. For Option A this means a new EOA address per order; for B/C a new smart account per order (LightAccount CREATE2 salt = `n`). Either way, cluster heuristics can't trivially link two offramps from the same user.

For Option A specifically, "burner per offramp" also means: don't reuse the ETH dust across offramps. Bridge fresh ETH for each new address. This is wasteful (~$0.05 leftover per address × N orders), but the dust is a function of order count, not order value, so it's bounded.

### 7.3 What we do NOT do

- No Thirdweb `inAppWallet` (Google/email/phone/passkey login). Conflicts with seed model.
- No Dynamic Labs.
- No Android Credential Manager / passkey-derived seed.
- No new app password.

---

## §8 Implementation strategy — native Kotlin port

### 8.1 The framing

Three forms of work scope:
1. **`offramp-lib` (always needed)** — UPI parsing, ECIES, ABI encoding, subgraph queries, circle routing, getOrder polling, orchestrator state machine.
2. **`evm-lib` (needed for B/C, skipped for A)** — ERC-4337 UserOp construction, LightAccount factory + execute, paymaster + bundler JSON-RPC client.
3. **Plain EOA RPC (needed for A only)** — web3j-based RawTransactionManager, single signer.

Option A skips #2. Option B/C ship #1 + #2 + drop #3.

### 8.2 Why native Kotlin (not JS bridge)

Re-evaluated for Option A:

| Concern | Native Kotlin (A) | JS via BareKit/WebView |
|---|---|---|
| ECIES | ~150 LOC BouncyCastle (golden vectors gate it) | Free, bundled in SDK |
| ABI encoding | web3j auto-generates from JSON ABIs | viem in SDK |
| Subgraph query | ~40 LOC OkHttp + JSON | Free, in SDK |
| Circle routing | ~40 LOC pure port | Free, in SDK |
| Sign+submit | web3j RawTransactionManager, ~50 LOC | Need wallet shim |
| Total new code | ~600 LOC | ~200 LOC Kotlin + JS bundle |
| Runtime cost | Negligible | +5-15MB APK; +cold-start time |
| Debug surface | Stack trace stays in JVM | Cross-language tracing |
| SDK upgrades | Manual port of changed ABIs | `npm update` (modulo shims) |

For Option A, native wins on every axis except SDK-upgrade ergonomics. SDK upgrades will hit our ABI bindings, not our logic — and `@p2pdotme/sdk`'s ABIs are public Diamond facet ABIs that change on a contract-version cadence, not a code cadence.

For Option C (with AA), the JS bridge gets more attractive because we'd otherwise hand-roll ERC-4337 + Thirdweb's bundler-paymaster wire format. But even then, `permissionless` (viem-native ERC-4337 lib) is small enough that we could port the UserOp construction in Kotlin too. The bundler/paymaster JSON-RPC is standard and stable.

**Decision: native Kotlin for both phases. Option A first, evolve to C.**

---

## §9 Module layout (revised)

```
evm-lib/                                          NEW — pure Kotlin EVM primitives
├── hd/EvmKeyDerivation.kt                        BIP-32/44 from BIP-39 seed
├── rpc/BaseRpcClient.kt                          eth_call, eth_sendRawTransaction, eth_getLogs
├── crypto/Ecies.kt                               eth-crypto wire-compatible (BouncyCastle)
└── signer/
    └── EoaSigner.kt                              Option A: web3j RawTransactionManager
    └── (later) Erc4337Signer.kt                  Option B/C: UserOp packing + send

offramp-lib/                                      NEW — pure Kotlin offramp logic
├── upi/UpiQrParser.kt                            upi:// URI regex
├── p2p/
│   ├── DiamondAbi.kt                             web3j-generated bindings
│   ├── Subgraph.kt                               CirclesForRouting + OrdersByUser
│   ├── CircleRouter.kt                           epsilon-greedy port (40 LOC)
│   ├── Orders.kt                                 prepare calldata for approve, placeOrder, setSellOrderUpi
│   ├── PriceConfig.kt                            getPriceConfig + getRpPerUsdtLimitRational
│   ├── ReputationGate.kt                         pre-flight RP check + small-order-threshold logic
│   ├── RelayIdentity.kt                          ephemeral secp256k1 keypair (in-memory only)
│   └── OrderPoller.kt                            polling readOrderMulticall (or via subgraph)
├── orchestrator/
│   ├── OfframpOrchestrator.kt                    state machine: scan → quote → bridge → approve → place → setUpi → wait
│   └── OfframpStatus.kt                          sealed class consumed by UI
└── di/offrampModule.kt                           Koin wiring

ui-lib/.../screen/offramp/                        NEW screens
├── scan/                                          OfframpScanScreen + VM (subclass of ScanView)
├── confirm/                                       OfframpConfirmScreen + VM (amount + fees + RP check)
├── progress/                                      OfframpProgressScreen — wraps TransactionProgressView
```

`offramp-lib` depends only on `evm-lib`. Both pure Kotlin (no Android imports). Unit-testable on JVM. UI lives in `ui-lib`, registered via Koin and a new `OfframpNavGraph` inside `WalletNavGraph` for back-stack continuity.

---

## §10 Subgraph

### 10.1 What it's used for

The SDK uses the subgraph for exactly two queries:
- `CirclesForRouting($currency)` — required inside `placeOrder.prepare` (for epsilon-greedy)
- `OrdersByUser($user, $skip, $first)` — optional, for order history UI

The Discord exchange noted: *"Subgraph URL is mainly for showing all transactions on your frontend"* — partially true. It's also required for routing.

### 10.2 Options

| Approach | Cost | When |
|---|---|---|
| Use p2p's mainnet subgraph (when they expose one) | Free | Currently they tell integrators to self-deploy on mainnet |
| Use p2p's Sepolia URL for dev | Free | `https://api.studio.thegraph.com/query/110312/indexer-one/version/latest` |
| Self-deploy on The Graph Studio | Free | Recommended for mainnet — fork github.com/p2pdotme/subgraph, deploy via Studio dashboard. ~1hr setup. |
| Self-host (own node) | $$ + ops | Not needed unless we have privacy or rate-limit concerns |

**Plan:** use p2p's Sepolia subgraph for dev, deploy our own to Graph Studio for mainnet before launch.

### 10.3 Fallback

The subgraph is "just an indexer". If we lose it, we can:
- For `CirclesForRouting`: read the Diamond's circle registry directly (more on-chain calls, slower)
- For `OrdersByUser`: replay `OrderPlaced` events via `eth_getLogs` from a known block

Both are feasible but slower and chatty on RPC. Keep them as a degraded mode.

---

## §11 Open questions

Ranked by blast radius:

1. **PAY order RP requirements on mainnet.** Empirical test needed before promising users it works without ZK KYC for sub-$100 orders. → Action: ask Bucky + run a $5 PAY test on mainnet from a fresh address.
2. **DAO sponsorship eligibility for our app's Thirdweb clientId** (only matters for path C). → Action: open conversation with @Git Chad once we have a working prototype.
3. **Does our subgraph need approval to index the mainnet Diamond?** Probably not (Graph Studio is permissionless), but worth confirming the github.com/p2pdotme/subgraph repo doesn't have hardcoded keys.
4. **NEAR Intents to a counterfactual address.** For Option A this is moot (the EOA is just an address). For Option C we want to bridge USDC to the not-yet-deployed smart account. Almost certainly fine, but confirm with one $0.50 test bridge on Sepolia.
5. **Slippage + fee buffer UX.** The user sees "₹500" but actual debited ZEC depends on (bridge rate + circle fee + small-order fixed fee). Product decision: show range vs single number? Pre-quote vs post-quote?
6. **Dispute UX in v1.** Surface `raiseDispute` in-app or link to a support email? Affects whether we port the 5th action.
7. **What happens if a PAY order auto-cancels after 72h** (timeout-based refund — needs verification)? UI must handle "got USDC back, here's what to do next."

---

## §12 Risks

| Risk | Likelihood | Severity | Mitigation |
|---|---|---|---|
| **PAY orders fail for new users due to insufficient RP** | High (BUY already does) | High — blocks v1 | Test on mainnet with $5. If confirmed: either gate offramp behind a one-time RP grant flow, or implement Reclaim social verify (zkkyc), or wait for DAO to add baseline RP. |
| Subgraph downtime / endpoint change | Medium | Medium (only blocks routing) | Self-deploy on Graph Studio. Cache last successful `CirclesForRouting` result for 60s. |
| ECIES port byte-incompatibility | Medium | Critical (merchant can't decrypt UPI) | Port golden vectors first. Round-trip test against SDK fixtures before integration. |
| Diamond contract upgrade changes calldata layout | Low | High | The Diamond is EIP-2535 — facet upgrades. Worst case: we re-port the ABI fragments. Monitor `contractVersion` in user-app-client/package.json (currently 0.0.6). |
| Bridge delivers less USDC than quoted | Medium | Medium | NEAR Intents quote includes slippage. Build a 3-5% buffer. Show "you'll receive between ₹X and ₹Y" UI. |
| ZEC anonymity broken at bridge step | Certain | Out of scope | Document in UI ("Base tx is public"). |
| Fraud-engine enforcement on backend rejects orders we submit | Unknown | High | Empirical — first mainnet test will tell us. Have not seen evidence the backend enforces fingerprints; happy path on Sepolia for the meshjs.dev engineer didn't require fraud-engine. |
| Path A → Path C migration is painful | Low | Low | Design `OfframpSigner` interface from day one. The migration touches only the signer; everything else is identical. |

---

## §13 Suggested next prototype (revised)

Two tracks, sequential.

### Track A — ECIES go/no-go (1–2 days)

Same as v1 doc:
1. Copy `p2pdotme-sdk/test/orders/crypto/ecies.test.ts` vectors into a Kotlin unit test.
2. Implement `evm-lib/crypto/Ecies.kt` with BouncyCastle.
3. Round-trip: `cipherStringify` output from Kotlin must match SDK output byte-for-byte for the same inputs.

If this passes, we're unblocked on the cryptographic part. If it fails, the most likely culprit is compressed/uncompressed pubkey handling in the HMAC step (§4.4).

### Track B — PAY order on Base Sepolia, plain EOA (3–5 days)

1. Implement `EvmKeyDerivation` (m/44'/60'/0'/0/0).
2. Fund the derived EOA with Sepolia ETH (chainlink faucet) + Sepolia USDC (Circle faucet).
3. **Ask Bucky to grant RP to the test address** — explicitly cite Discord precedent. Without this, BUY/SELL/PAY all revert with `0x91da284f`.
4. Implement bare-minimum `Orders.kt`: `approveUsdc.prepareData`, `placeOrder.prepareData` (with a hardcoded `circleId` to skip subgraph routing), `setSellOrderUpi.prepareData`.
5. Implement `BaseRpcClient` + web3j signer.
6. Send the 3 transactions sequentially. Poll `readOrderMulticall` between each.
7. Have a test merchant on Sepolia complete the order off-chain (coordinate via Discord).

Success criteria: USDC moves from our EOA to the Diamond, then out to the recipient. Sepolia BaseScan confirms.

### Track C — Mainnet $5 PAY order (1 day, after A+B pass)

1. Same EOA flow but on mainnet.
2. Fresh address. **Don't** ask for an RP grant — we need to see what happens by default for a PAY order under $100.
3. If it reverts with the same `0x91da284f` selector: PAY has the same RP gate as BUY. We need RP onboarding before v1 ships.
4. If it succeeds: small-orders bypass RP. We can ship v1 with a $99 cap.

This single test ends the §11 question 1 ambiguity.

---

## §14 References (verified, file:line)

### Zodl
Unchanged from v1. Key reused paths:
- `ui-lib/.../screen/unifiedsend/` (Send form, bridge integration)
- `ui-lib/.../common/datasource/NearSwapDataSourceImpl.kt`
- `ui-lib/.../common/repository/{BiometricRepository, SwapRepository}.kt`
- `ui-lib/.../common/security/PinAuthGate.kt`
- `ui-lib/.../common/provider/PersistableWalletProvider.kt`
- `ui-lib/.../screen/transactionprogress/{TransactionProgressView, State}.kt`
- `ui-lib/.../screen/scan/ScanView.kt`
- `ui-lib/.../screen/chat/viewmodel/ChatViewModel.kt:restoreFromWalletSeed` (HD derivation precedent)

### p2pdotme-sdk
- `architecture.md`, `CLAUDE.md` — design principles
- `src/orders/client.ts:97-227` — `createOrders` factory
- `src/orders/crypto/ecies.ts:67-165` — ECIES encrypt/decrypt + wire format
- `src/orders/actions/place-order.ts:57-150` — calldata + circle routing wiring
- `src/orders/actions/set-sell-order-upi.ts:27-77` — ECIES + calldata
- `src/orders/actions/approve-usdc.ts:19-56` — ERC-20 approve
- `src/orders/tx.ts:12-76` — `walletClient.sendTransaction` shim (wallet-agnostic boundary)
- `src/orders/internal/routing/{routing.ts, client.ts, subgraph/queries.ts}` — epsilon-greedy
- `src/orders/subgraph/queries.ts` — `OrdersByUser`
- `src/prices/client.ts:25-33` + `src/contracts/p2p-config/index.ts:48-82` — `getReputationPerUsdcLimit` (the RP gate)
- `example/make-pay-order.ts` — canonical PAY reference flow
- `test/orders/crypto/ecies.test.ts` — **golden vectors for the Kotlin port**

### user-app-client
- `.env.example:29-31` — mainnet addresses (Diamond / USDC / RM)
- `.env.example:82-87` — `VITE_BUNDLER_PAYMASTER_URL`
- `.env.example:53-60` — subgraph deploy guidance
- `package.json:27` — `@p2pdotme/sdk@1.1.7` (latest used by official FE)
- `package.json:63` — `thirdweb@5.119.0`
- `src/core/adapters/thirdweb/chain.ts:86-112` — `inAppWallet({executionMode:"EIP4337", sponsorGas:true})`
- `src/core/adapters/thirdweb/client.ts:34-38` — `accountAbstraction = {sponsorGas: true, factoryAddress: ...}`
- `src/core/adapters/thirdweb/client.ts:63-142` — `estimatedPrepareTransaction`
- `src/core/adapters/thirdweb/actions/order.ts:617-655` — `placeOrder` integration (full pattern: `prepareXxxTx → estimatedPrepareTransaction → sendAndConfirmTransaction`)
- `src/core/adapters/thirdweb/actions/order.ts:73-296` — reads NOT in `@p2pdotme/sdk` (getTxLimit, getUserBuyLimit, getRPPerUsdLimit, getSmallOrderThreshold/FixedFee*, …)

### Discord (p2p.me server, confirmed messages)
- 2026-04-23 (Bucky): Sepolia addresses, RP grant policy, BUY/SELL/PAY revert selectors
- 2026-04-24 (Bucky): RP increases via social verify on `app.p2p.me/limits`
- 2026-04-28 (Bucky): mainnet addresses; "deploy subgraph yourself"
- 2026-04-29 (Bucky): user-app-client open-sourced
- 2026-04-30 (Bucky): "amount under $100 user can pay without doing ZK verification"

---

## §15 Concrete next steps for me/you

In order:

1. **You:** message Bucky / Git Chad on Discord:
   - "Do PAY orders (orderType 2) have the same RP gate as BUY?"
   - "What's the default RP for a new mainnet address — is there any baseline for sub-$100 orders, or does every new user need to run a social verify before they can pay anyone?"
   - "Can we discuss DAO gas sponsorship for our integration's Thirdweb clientId once we have a prototype?"
2. **Me, next session:** write the Kotlin ECIES module + golden-vector unit test (Track A).
3. **Me, after Track A passes:** scaffold `evm-lib` (HD derivation, BaseRpcClient, EoaSigner) and `offramp-lib` (UpiQrParser, DiamondAbi via web3j codegen, Orders prepareData functions).
4. **You + me, after #3:** Track B — submit a real PAY order on Sepolia with a real INR merchant. This is the end-to-end smoke test.
5. **You, after Track B:** $5 mainnet PAY (Track C) to resolve RP ambiguity.
6. **Based on Track C result:** either ship v1 with a $99 cap (RP bypass works) or design an RP onboarding flow (RP bypass doesn't work) before v1.

That sequence ships V1 in ~3 weeks of engineering effort spread across a 4-5 week calendar.

---

## §16 Appendix — what's in `@p2pdotme/sdk` vs what's not

For each capability we need, where to get it:

| Need | In `@p2pdotme/sdk`? | Notes |
|---|---|---|
| `placeOrder` calldata | ✅ orders.placeOrder.prepare | Includes circle routing |
| `approveUsdc` calldata | ✅ orders.approveUsdc.prepare | |
| `setSellOrderUpi` calldata + ECIES | ✅ orders.setSellOrderUpi.prepare | |
| `getOrder` read | ✅ orders.getOrder | |
| Order events | ✅ orders.watchEvents | |
| Subgraph queries | ✅ internal | We re-implement in Kotlin (1 query) |
| Circle routing | ✅ internal | Pure logic, easy port |
| `getPriceConfig`, `getReputationPerUsdcLimit` | ✅ prices.* | |
| `getUsdcBalance`, `getUsdcAllowance`, tx-limits | ✅ profile.* | |
| **`getTxLimit`, `getUserBuyLimit`, `getUserSellLimit`** | ❌ | FE uses internal `@p2pdotme/contracts`. We add the ABI fragments ourselves if we need them for UI. |
| **`getSmallOrderThreshold`, `getSmallOrderFixedFee*`** | ❌ | Same. Needed for fee display + RP-bypass logic. |
| **RP grant / social verify / Anon Aadhaar** | Partially (zkkyc) | We skip for v1; revisit for >$100. |
| ERC-4337 UserOp construction | ❌ | Wallet layer concern; not SDK's job. |
| Paymaster JSON-RPC | ❌ | Same. |

The "❌" rows are the surface we'd write ourselves regardless of whether we use the SDK or port it.

---

This document supersedes the v1 brief. If anything in §6 (gas model) or §4.6 (RP) reads differently from your understanding, push back — those two sections are where I made the biggest revisions from v1, and they drive most of the strategy.
