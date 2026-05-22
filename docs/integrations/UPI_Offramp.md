# UPI Offramp Integration — Architecture Brief v3

Status: **EOA path implemented; pivoting to thirdweb hybrid account abstraction**
Owner: Chinmay
Last updated: 2026-05-21

## What changed in v3

v2 was a research brief that deliberated over gas models (plain EOA vs AA) and a native-Kotlin port. Both are now settled:

- **The EOA path is built.** `evm-lib` + `offramp-lib` exist and implement the full PAY flow in native Kotlin (HD-derived EOA, EIP-1559 signing, ECIES, circle routing, order polling, checkpoint/resume). The v2 prototype tracks and "native vs JS" debate are done — removed.
- **We have decided to pivot to a thirdweb hybrid account-abstraction setup** (§3). This supersedes v2 §6 (the A/B/C gas options) and v2 §7 (plain-EOA-as-sender). The reason and mechanics are the bulk of this doc.

Everything we already verified about the p2p.me protocol (PAY lifecycle, ECIES wire format, RP gate, addresses) is now condensed to §4 with pointers to the implementing code instead of full re-explanations.

Sibling repos:
- `../../p2pdotme-sdk/` — `@p2pdotme/sdk` (reference for calldata/ECIES; we port, not import)
- `../../user-app-client/` — official p2p.me web frontend (the AA stack we're matching)
- `../../executor/` — p2p.me's event-driven contract automation (merchant assign / sweep)

---

## §1 Goal

A Zodl user holds only ZEC, scans a merchant's UPI QR, and the wallet bridges ZEC → Base USDC, places a p2p.me **PAY** order (orderType 2), ECIES-encrypts the UPI handle for the matched merchant, and settles when the merchant confirms INR receipt. No new credential, no third-party login. Gas must be invisible to the user, and a failed order must return funds to the user as ZEC.

---

## §2 Current implementation status (EOA path — built)

Native Kotlin, two pure-JVM modules (no Android imports, unit-testable):

```
evm-lib/        HD derivation, Base RPC, ABI codec, ECIES, EIP-1559 EOA signer
offramp-lib/    UPI parser, Diamond/ERC20 calldata, circle router, order readers,
                fee details, relay identity, orchestrator (state machine + checkpoint/resume)
ui-lib/.../screen/offramp/   scan → confirm → progress screens
```

The signing/account boundary is already abstracted behind `offramp-lib/account/OfframpAccountProvider.kt` — **this is the seam the AA pivot plugs into** (see §6). Today it resolves a plain EOA via `EoaSigner`; the pivot swaps the implementation, not the orchestrator.

What works end-to-end today: HD-derive EOA from the wallet seed → (bridge) → `approveUsdc` → `placeOrder` → poll to `accepted` → `setSellOrderUpi` (ECIES) → poll to `completed`. Gas is currently the EOA's own ETH.

---

## §3 Decision: thirdweb hybrid account abstraction

### §3.1 What we decided

Move the offramp account from a **plain EOA** to a **per-user ERC-4337 smart account** (thirdweb's prebuilt `Account` contract), with:

- **Owner key = self-custody, HD-derived from the wallet's BIP-39 seed.** Not a thirdweb in-app wallet, not Dynamic Labs, no social login.
- **Gas sponsored via our own thirdweb credentials** (`sponsorGas: true`). Users never hold or see ETH.
- **NEAR Intents bridges ZEC → USDC directly into the smart account address.**
- **Refunds pull funds back to ZEC via NEAR Intents 1-Click** (§3.6).

This is the same architecture p2p.me runs in production — so it's the lowest-risk shape, not a novel one.

### §3.2 What "hybrid" means

There are three pieces of ERC-4337 plumbing and one custody question. "Hybrid" = we **rent the plumbing** but **keep custody**:

| Piece | What it is | Our choice |
|---|---|---|
| **Account contract** | The user's on-chain smart wallet | Reuse thirdweb's `Account` (same contract p2p uses) |
| **Factory** | Deploys the account (EIP-1167 clone, CREATE2) | thirdweb's `AccountFactory` (our own / public instance) |
| **Bundler** | Server that submits the user's signed request (UserOp) on-chain | **Rented** (thirdweb) — not self-hosted |
| **Paymaster** | Sponsor that pays the gas so the user needs no ETH | **Rented** (thirdweb, our credentials) — not self-hosted |
| **Owner key (custody)** | The key that authorizes the account | **Ours/local** — derived from the Zcash seed, on-device |

So: managed gas infrastructure, self-custodial keys. We are **not** self-hosting a paymaster contract + bundler on day one (that's ops we don't need at launch — Base gas is cents/order; rent now, revisit only if volume makes the markup matter). We are **not** handing keys to thirdweb.

### §3.3 Why — verified against p2p.me's live stack (Base mainnet)

Confirmed on-chain and in `user-app-client` (2026-05-21):

- Every p2p.me "user" address is an **EIP-1167 minimal-proxy clone** of one implementation `0xd3d756c9a98ac8e4b642af1cc07ac51d33b80031`, whose verified source is thirdweb's prebuilt **`Account`** (`contracts/prebuilts/account/non-upgradeable/Account.sol`).
- Orders are placed via the canonical **ERC-4337 EntryPoint v0.6** `0x5FF137D4b0FDCD49DcA30c7CF57E578a026d2789` (`handleOps`). A traced UserOp's `callData` is `account.execute(Diamond, 0, placeOrder(...))`, so the Diamond sees `msg.sender = the smart account` → that's what carries reputation/KYC/limits.
- **Gas is fully sponsored**: the user account holds **0 ETH**; a paymaster contract reimburses a bundler. Frontend: `inAppWallet({ executionMode: { mode: "EIP4337", smartAccount: { sponsorGas: true, factoryAddress: VITE_THIRDWEB_CONTRACT_ADDRESS_AA_FACTORY }}})`.
- The `@p2pdotme/sdk` has **zero thirdweb dependency** — it's wallet-agnostic (`prepare() → {to,data,value}`). thirdweb is purely the frontend's wallet/gas layer. **So thirdweb is not "provided" to us; we bring our own credentials.** The SDK's fraud-engine signer interface explicitly supports both EOA and smart-wallet (`address` = smart account, `signerAddress` = admin EOA) — confirming the design is account-agnostic.

Net: the EOA path works mechanically (the Diamond accepts any `msg.sender`), but AA is p2p.me's native shape and the only way to deliver gas-free UX without funding per-user ETH dust.

### §3.4 Account & gas model

- **One persistent smart account per user**, address derived deterministically (CREATE2) from the seed-derived owner key. Computable **before deployment** (counterfactual).
- **Lazy deploy**: the clone is deployed by the factory inside the **first** sponsored UserOp (initCode), bundled with the first action. No separate deploy tx; deploy gas is also sponsored.
- **Owner key** signs UserOps (not raw txs). It must also be able to sign an **EIP-191 message** — required by p2p.me's fraud attribution (smart contracts can't sign EIP-191; the admin EOA does). A seed-derived local key does this natively.
- **Gas**: every action (deploy, `approveUsdc`, `placeOrder`, `setSellOrderUpi`, `cancel`, refund-transfer) is a sponsored UserOp through our thirdweb bundler/paymaster. We pay thirdweb (small, usage-based; Base gas ≈ cents/order). thirdweb has no first-class Kotlin SDK → we drive it over its HTTP/bundler API from `evm-lib` (see §6).

### §3.5 NEAR bridge IN

NEAR Intents delivers a plain ERC-20 USDC transfer to a destination address. We point it at the user's **smart-account address** (works even while counterfactual — an address can hold USDC before the contract exists). Flow: compute account address from owner key → request NEAR quote (ZEC → USDC, recipient = account address) → user bridges → USDC lands in the account → first sponsored UserOp deploys the account and runs `approve` + `placeOrder`.

**Pre-flight gate:** check `checkCircleEligibility` (merchant availability) **before** bridging ZEC→USDC, so we rarely bridge in only to find no route. This collapses the most common refund case.

### §3.6 Refunds OUT — NEAR Intents 1-Click pull-back (verified feasible)

When an order can't complete (no merchants, merchant cancels, or executor auto-cancels on expiry), escrowed USDC returns to the smart account. We bridge it back to ZEC via **NEAR Intents 1-Click**:

1. Backend/app requests a 1-Click quote: `originAsset` = USDC on Base, `destinationAsset` = ZEC, `recipient` = user's Zcash address, **`refundTo` = the smart account**.
2. NEAR returns a **deposit address** on Base (`depositMode: SIMPLE`).
3. The smart account does **one sponsored UserOp: `USDC.transfer(depositAddress, amount)`** — a plain transfer, paymaster pays gas. No bridge-contract integration needed.
4. NEAR solvers swap and deliver ZEC to the user's Zcash address.

**Verified (2026-05-21):** NEAR Intents 1-Click officially supports **Base + Zcash + USDC**, is bidirectional ("into and out of Zcash"), uses deposit-address + plain transfer (no EOA-only requirement → a contract can send), and exposes `recipient` + `refundTo`. The official Zcash wallet **Zashi** already uses these exact rails. (Sources in §8.)

**Failure-safety ladder** — funds are never stranded; at every rung the USDC sits in the user's own self-custodial smart account:

| Where it breaks | Outcome | Safe? |
|---|---|---|
| NEAR swap fails/expires | NEAR auto-refunds USDC to `refundTo` = the smart account | ✅ auto |
| P2P order cancelled / no merchant | Escrowed USDC returns to the smart account; run the 1-Click pull-back | ✅ |
| The pull-back transfer fails | USDC stays in the account; retry | ✅ |

Cost of a failed round-trip: NEAR swap fee (currently advertised **zero** on ZEC) + a few cents of gas (sponsored). Slippage buffer still applies.

### §3.7 Session keys — automating order lifecycle & refunds

thirdweb's `Account` has on-chain session keys (verified in source: `SignerPermission`, `approvedTargets`, `setPermissionsForSigner`, `isActiveSigner`, `getAllActiveSigners`). The owner key (signed once, locally) can grant a **scoped backend signer**:

- `approvedTargets` = `{ Diamond, NEAR 1-Click deposit/bridge target, USDC }`
- optional value caps + expiry

This lets a backend drive `setSellOrderUpi`, polling, and **automated refunds** without the user being online and **without holding user funds or the full owner key**. Without session keys, every step needs the user's device to sign (workable, worse UX). Decision pending on whether v1 ships with session-key automation or device-signed only (§7).

### §3.8 One account per user — and why not burner-per-order

v2 proposed a fresh burner per offramp for privacy. With the RP gate (§4.3) this is **counterproductive**: reputation, tx-limits, and any KYC accrue to the `msg.sender` account. A new account per order resets RP to zero every time → likely reverts or caps every order at the floor. So we use **one persistent account per user** (matches p2p.me, where a single account had ~100 orders). The privacy cost (offramps linkable on Base) is already implied by bridging at all (§5); we accept it rather than break the RP model.

---

## §4 Verified p2p.me protocol facts (condensed)

### §4.1 Addresses

**Base mainnet (production)** — confirmed by p2p.me team + on-chain:
```
DIAMOND             = 0x4cad6eC90e65baBec9335cAd728DDC610c316368
REPUTATION_MANAGER  = 0xCF613e08EE1B4c2669DdCf06A7d22c9856f6Aa1D
USDC                = 0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913
ENTRYPOINT (4337 v0.6) = 0x5FF137D4b0FDCD49DcA30c7CF57E578a026d2789
TW ACCOUNT IMPL (p2p's) = 0xd3d756c9a98ac8e4b642af1cc07ac51d33b80031
SUBGRAPH            = self-deploy (github.com/p2pdotme/subgraph)
```
Our thirdweb factory / paymaster / clientId: **TBD** — provision under our own thirdweb project (do not reuse p2p's credentials; we don't have them and shouldn't).

**Base Sepolia (testing):**
```
DIAMOND             = 0xce868398FDaDcA368EAc203222874D6888532aE2
REPUTATION_MANAGER  = 0x45919D69E2154F46b6f6eA42ae23d2e9ee21B66f
USDC                = 0xDABa329Ed949f28F64019f22c33c3B253B2Ded60
SUBGRAPH            = https://api.studio.thegraph.com/query/110312/indexer-one/version/latest
```

### §4.2 PAY lifecycle (implemented in `offramp-lib/orchestrator`)

`approveUsdc` → `placeOrder(orderType:2)` → poll to `accepted` (yields merchant addr + pubkey) → `setSellOrderUpi` (ECIES-encrypts UPI for merchant) → poll to `completed` (merchant calls `completeOrder` after INR send). **3 user-side writes** (approve, place, setUpi). There is **no `paidBuyOrder`** step in PAY (that's BUY-only).

### §4.3 Reputation Points (RP) — still the #1 open risk

`getReputationPerUsdcLimit(currency)` returns `(num, den)` → tx limit = `RP × den / num`. New addresses start with low/zero RP; BUY orders revert with `0x91da284f` until granted RP. Discord: under $100 may not need ZK verification, but baseline RP behavior for **PAY** on mainnet for a fresh address is **unconfirmed**. **AA does not change this** — RP attaches to the smart account (`msg.sender`), reinforcing §3.8 (one persistent account). Action: empirical $5 mainnet PAY from a fresh smart account + direct ask to p2p team.

### §4.4 ECIES & circle routing (implemented — reference only)

ECIES wire format (secp256k1 + AES-256-CBC + HMAC-SHA256, eth-crypto compatible) is ported in `evm-lib/crypto/Ecies.kt`, gated by golden vectors from `p2pdotme-sdk/test/orders/crypto/ecies.test.ts`. Epsilon-greedy circle routing is ported in `offramp-lib/p2p/CircleRouter.kt`. Both verified against the SDK; no further work unless the Diamond's ABIs change.

### §4.5 Fees (display)

Per-currency: `getSmallOrderThreshold`, `getSmallOrderFixedFee[Buy|Sell|Pay]`, else circle `feeRate`. Read on confirm screen to show gross/net. Not in `@p2pdotme/sdk` — ABI fragments are ours (`offramp-lib/p2p/OrderFeeDetails.kt`).

---

## §5 Privacy posture

- Bridging breaks Zcash anonymity once USDC lands on Base; the receiving (smart account) address is correlatable. Out of scope to fix; documented in UI.
- One persistent account per user (§3.8) means a user's offramps are linkable on Base — accepted trade-off for the RP model.
- ECIES protects the UPI handle on-chain (only the matched merchant decrypts).
- No fraud-engine import → no fingerprint/IP/SEON to p2p.me. **But** the fraud engine needs the owner EOA to sign an EIP-191 message (§3.4) — that signature is the only identity signal we emit, and it's local.
- Whatever subgraph URL we use sees our order queries → self-host the subgraph for mainnet.

---

## §6 Migration plan: EOA → thirdweb hybrid AA

The orchestrator, calldata builders (`DiamondCalls`, `Erc20Calls`), ECIES, circle router, order readers, fee logic, and checkpoint/resume are **all unchanged** — they operate on `{to, data, value}`, which is identical whether sent as a raw tx or wrapped in a UserOp.

What changes is behind `offramp-lib/account/OfframpAccountProvider.kt`:

1. **Account address** — replace "HD-derived EOA address" with "counterfactual thirdweb `Account` address from the owner key + factory." The bridge-in target (§3.5) becomes this address.
2. **Submit boundary** — replace `EoaSigner` (sign + `eth_sendRawTransaction`) with a UserOp path: build `account.execute(to, value, data)` calldata → request paymaster sponsorship → submit via thirdweb bundler → poll the UserOp receipt. New file e.g. `evm-lib/signer/Erc4337Submitter.kt` + a thin thirdweb bundler/paymaster JSON-RPC client.
3. **Deploy handling** — include factory `initCode` on the first UserOp (lazy deploy); empty thereafter.
4. **EIP-191 signing** — expose owner-key `signMessage` for fraud attribution (already derivable from the seed key).
5. **Refund action** — add a `pullBackToZcash` step in the orchestrator: NEAR 1-Click quote → `USDC.transfer(depositAddr)` as a sponsored UserOp (§3.6).
6. **(Optional) session keys** — `setPermissionsForSigner` grant flow if we automate refunds server-side (§3.7).

EOA `EoaSigner`/`Eip1559Tx`/`Rlp` stay in `evm-lib` (still useful for testing / fallback), but the production `OfframpAccountProvider` resolves the AA submitter.

---

## §7 Open questions

1. **PAY RP gate on mainnet** for a fresh smart account (§4.3). → $5 empirical test + ask p2p team. Blocks v1 scope (cap vs RP-onboarding).
2. **Session-key automation vs device-signed** (§3.7). Affects whether refunds/poll need the user online.
3. **thirdweb pricing/limits** for our own sponsored-gas project at expected volume; free-tier ceiling.
4. **thirdweb from Kotlin** — confirm the cleanest path to drive its bundler/paymaster from native Kotlin (direct bundler JSON-RPC with thirdweb paymaster RPC vs a thin backend). Standard ERC-4337 RPC is portable; verify thirdweb's paymaster sponsorship handshake shape.
5. **NEAR 1-Click deposit-address lifetime** — quote-bound with a deadline; orchestrator must quote→send within the window (not a static address).
6. **Counterfactual account receiving USDC then needing deploy to spend it** — confirm thirdweb bundles deploy+transfer in one UserOp for the "bridged in, never placed an order, must refund" edge.
7. Dispute UX (`raiseDispute`) — surface in-app or support link.

---

## §8 References

### Zodl (implemented)
- `evm-lib/` — `hd/EvmKeyDerivation.kt`, `rpc/BaseRpcClient.kt`, `crypto/Ecies.kt`, `signer/{EoaSigner,Eip1559Tx,EcdsaSigner,Rlp}.kt`, `abi/*`
- `offramp-lib/` — `orchestrator/OfframpOrchestrator.kt`, `account/OfframpAccountProvider.kt` (the AA seam), `p2p/{DiamondCalls,Erc20Calls,CircleRouter,Ecies use,OrderReader,OrderFeeDetails,RelayIdentity,SubgraphClient,UpiQrParser}.kt`
- `ui-lib/.../screen/offramp/` — scan / confirm / progress
- Reused: `NearSwapDataSourceImpl` (bridge), `PersistableWalletProvider` (seed), `PinAuthGate`/`BiometricRepository` (auth), `TransactionProgressView`, `ScanView`

### p2pdotme-sdk / user-app-client (AA stack we're matching)
- `user-app-client/src/core/adapters/thirdweb/chain.ts` — `inAppWallet({executionMode:"EIP4337", sponsorGas:true, factoryAddress})`
- `user-app-client/src/core/adapters/thirdweb/client.ts` — `accountAbstraction = {sponsorGas:true, factoryAddress}`
- `user-app-client/src/core/adapters/thirdweb/actions/order.ts` — `prepareXxxTx → estimatedPrepareTransaction → sendAndConfirmTransaction`
- thirdweb `Account` impl `0xd3d756c9a98ac8e4b642af1cc07ac51d33b80031` (Basescan-verified: `ContractName: Account`, session-key support)
- `@p2pdotme/sdk` — no thirdweb dep; `prepare()` calldata + ECIES golden vectors (`test/orders/crypto/ecies.test.ts`)

### NEAR Intents (refund / bridge rails — verified 2026-05-21)
- Supported chains (Base + Zcash): https://docs.near-intents.org/resources/chain-support
- 1Click API (deposit address, `recipient`/`refundTo`, SIMPLE mode): https://docs.near-intents.org/near-intents/integration/distribution-channels/1click-api
- 1Click SDK (TS): https://github.com/defuse-protocol/one-click-sdk-typescript
- Zcash cross-chain swaps live (The Defiant): https://thedefiant.io/news/defi/near-intents-100-tokens-zcash-swaps-5djz2b
- Zashi Swaps on-ramp (Electric Coin Co): https://electriccoin.co/blog/zashi-swaps-decentralized-on-ramp-is-live/

---

This document supersedes v2. The headline change is §3: we are pivoting the (already-built) EOA offramp to a thirdweb hybrid smart account — managed gas, self-custody keys, NEAR bridge in, NEAR 1-Click pull-back for refunds. §6 is the migration path; §7 lists what still needs confirming before v1.
