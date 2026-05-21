# UPI Offramp — v1 Implementation Plan

Status: **plan locked, ready to start coding**
Companion doc: [UPI_Offramp.md](./UPI_Offramp.md) (research/architecture background)
Last updated: 2026-05-20

This is the working plan. Read [UPI_Offramp.md](./UPI_Offramp.md) for the research that led here.

---

## §1 Locked decisions

| Decision | Choice | Why |
|---|---|---|
| Order type | **PAY (orderType 2)** | User's USDC → merchant's third-party UPI. Matches scan-QR-and-pay UX. |
| Gas | **Plain EOA + bridged ETH dust** | Simplest. NEAR Intents already supports Base ETH. AA/paymaster is a future-pivot, not v1. |
| Implementation | **Native Kotlin** | ~450 LOC across `evm-lib` + `offramp-lib`. No JS runtime. |
| Subgraph | **Use p2p's hosted URL for Sepolia; ask for mainnet URL, fallback to self-deploy** | See §4 |
| Per-order limit | **$99 USDC cap** | Skips ZK KYC per Bucky's confirmation |
| Networks supported in build | **Sepolia + Mainnet, runtime-switchable via config** | See §3 |
| First milestone | **End-to-end PAY on Base Sepolia with manually-funded USDC** | Bypass bridge initially; isolate p2p protocol bugs |

---

## §2 Sepolia is messy — what the Discord taught us

Before we start coding we should internalize the operational state of the testnet, because half of the failure modes are environmental, not code bugs.

**Things known to be broken or finicky on Sepolia (as of 2026-04-23):**

1. **BUY orders revert with `0x91da284f`** for any address that hasn't been manually granted Reputation Points. SELL/PAY are believed to bypass this — but **needs empirical confirmation for PAY** before we trust it. Action: ask Bucky directly + run a test.
2. **`0x5d04ff4c`** = "no merchant has enough fiat liquidity for this order." This is a contract-level revert when `getAssignableMerchantsFromCircle` returns empty. It means the SDK's circle routing picked a valid circle but on-chain eligibility failed. Cure: pick a different currency or wait for merchants to fund.
3. **Merchant bots are intermittent.** meshjs.dev placed three valid SELL INR orders (`orderId 17, 18, 19`) that sat in `pending` for hours with `acceptedMerchant = 0x0...`. The INR merchant bot was effectively offline. Implication for us: **integration tests need coordination with team — ask before testing, confirm the INR PAY bot is running.**
4. **New currencies need explicit enabling.** VEN was disabled until Bucky manually flipped it on. INR is presumed always-on.
5. **Faucets:** Base Sepolia ETH = Chainlink/Alchemy faucet. Base Sepolia USDC = Circle's testnet faucet. RP grants = ask Bucky.

**The implication:** "it doesn't work on Sepolia" can mean six different things, only one of which is "our code is wrong." Build logging early, surface revert selectors in our error type, and tolerate the team-coordination overhead.

---

## §3 Network configuration strategy

The user said: *"we wan tproper maininabel configs."* Here's the shape.

### 3.1 One config object, two presets

```kotlin
// offramp-lib/src/main/kotlin/xyz/zapp/offramp/config/P2pNetworkConfig.kt
data class P2pNetworkConfig(
    val name: String,                 // "sepolia" | "mainnet" — for logs
    val chainId: Long,                // 84532 | 8453
    val rpcUrl: String,
    val diamondAddress: String,
    val usdcAddress: String,
    val reputationManagerAddress: String,
    val subgraphUrl: String,
    val baseExplorerUrl: String,      // for deeplinks in TransactionProgressView
)

object P2pNetworks {
    val SEPOLIA = P2pNetworkConfig(
        name = "sepolia",
        chainId = 84532L,
        rpcUrl = "https://sepolia.base.org",
        diamondAddress = "0xce868398FDaDcA368EAc203222874D6888532aE2",
        usdcAddress = "0xDABa329Ed949f28F64019f22c33c3B253B2Ded60",
        reputationManagerAddress = "0x45919D69E2154F46b6f6eA42ae23d2e9ee21B66f",
        subgraphUrl = "https://api.studio.thegraph.com/query/110312/indexer-one/version/latest",
        baseExplorerUrl = "https://sepolia.basescan.org",
    )

    val MAINNET = P2pNetworkConfig(
        name = "mainnet",
        chainId = 8453L,
        rpcUrl = "",                  // see §3.2
        diamondAddress = "0x4cad6eC90e65baBec9335cAd728DDC610c316368",
        usdcAddress = "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913",
        reputationManagerAddress = "0xCF613e08EE1B4c2669DdCf06A7d22c9856f6Aa1D",
        subgraphUrl = "",             // see §4
        baseExplorerUrl = "https://basescan.org",
    )
}
```

### 3.2 Where values come from (per existing Zodl convention)

Following the same `local.properties` → `gradle.properties` → env-var fallback pattern documented in the project root `CLAUDE.md`:

| Property | gradle.properties default | local.properties / env override | Notes |
|---|---|---|---|
| `P2P_NETWORK` | `sepolia` | `mainnet` for prod builds | Build-time. Selects the preset above. |
| `P2P_RPC_URL_BASE_MAINNET` | _(empty)_ | Set to Alchemy/Infura URL | Only consulted when `P2P_NETWORK=mainnet`. |
| `P2P_RPC_URL_BASE_SEPOLIA` | `https://sepolia.base.org` | _(usually default)_ | Public RPC is fine for dev. |
| `P2P_SUBGRAPH_URL_MAINNET` | _(empty)_ | Set after Graph Studio deploy | See §4. |

Exposed to runtime via `BuildConfig.P2P_NETWORK` etc. — same pattern as `ZAPP_MESSAGING_LOG_LEVEL` etc.

### 3.3 Selecting the active config at runtime

```kotlin
// offramp-lib/src/main/kotlin/xyz/zapp/offramp/config/P2pConfigProvider.kt
class P2pConfigProvider(
    private val networkName: String,                // BuildConfig.P2P_NETWORK
    private val rpcUrlOverride: String?,            // BuildConfig.P2P_RPC_URL_*
    private val subgraphUrlOverride: String?,
) {
    fun current(): P2pNetworkConfig = when (networkName) {
        "mainnet" -> P2pNetworks.MAINNET.copy(
            rpcUrl = rpcUrlOverride ?: error("P2P_RPC_URL_BASE_MAINNET must be set"),
            subgraphUrl = subgraphUrlOverride ?: error("P2P_SUBGRAPH_URL_MAINNET must be set"),
        )
        "sepolia" -> P2pNetworks.SEPOLIA.copy(
            rpcUrl = rpcUrlOverride ?: P2pNetworks.SEPOLIA.rpcUrl,
        )
        else -> error("Unknown P2P_NETWORK: $networkName")
    }
}
```

Wired via Koin. The orchestrator and all other services depend on `P2pNetworkConfig`, not on hardcoded addresses.

### 3.4 Debug-build network switcher (optional, v1.1)

For testing UX with mainnet contracts but Sepolia funds, we *could* add a hidden settings toggle that overrides the BuildConfig value at runtime. Defer to v1.1; v1 ships with build-time selection only.

---

## §4 Subgraph — what to do

**Short answer to your question: yes, for now we just use the URL Bucky shared. We don't need to deploy our own for Sepolia. For mainnet we need to figure it out — see below.**

### 4.1 Sepolia

`https://api.studio.thegraph.com/query/110312/indexer-one/version/latest` works. It's the dev-time shared URL Bucky maintains.

### 4.2 Mainnet — open question

The `.env.example` in `user-app-client` says "Deploy the P2P.me subgraph indexer and paste the endpoint here," and Bucky's Discord message reinforced that: *"You can deploy subgraph by yourself. https://github.com/p2pdotme/subgraph"*.

But this seems weird for a production protocol — surely there's a canonical mainnet subgraph somewhere (the official frontend at app.p2p.me has to read from *something*). Two scenarios:
- (a) There's a public mainnet subgraph URL we just don't know about. **Ask Bucky.**
- (b) p2p.me genuinely expects every integrator to self-deploy. In that case, we fork `github.com/p2pdotme/subgraph`, deploy to The Graph Studio (free hosted tier, ~1hr setup), publish, and put the URL in `gradle.properties`.

**Action: ask before mainnet launch. Self-deploy is the fallback. Doesn't block v1 development.**

### 4.3 Why we need it at all

Two queries:

| Query | Required by | What it returns |
|---|---|---|
| `CirclesForRouting($currency)` | `placeOrder.prepare` — circle selection | List of active INR circles + their scores |
| `OrdersByUser($user, $skip, $first)` | Order history UI (optional) | User's past orders |

The Diamond contract doesn't expose a "list all active circles" view function. The subgraph is the indexed cache of `CircleRegistered` events that lets us do that query cheaply.

For v1 we hit `CirclesForRouting` on every offramp (~once per session — we can cache for an hour). `OrdersByUser` we skip entirely for v1 — we hold the active orderId in memory and poll `getOrder` until terminal.

---

## §5 Module layout (concrete)

Following the existing Zodl `*-api-lib` / `*-impl-android-lib` split convention is overkill for v1. We use two single modules:

```
evm-lib/                                       NEW
├── build.gradle.kts                           pure Kotlin module, web3j + BouncyCastle + OkHttp + kotlinx-serialization
└── src/main/kotlin/xyz/zapp/evm/
    ├── hd/
    │   └── EvmKeyDerivation.kt               BIP-32/44, m/44'/60'/0'/0/n from BIP-39 seed
    ├── rpc/
    │   ├── BaseRpcClient.kt                  eth_call, eth_sendRawTransaction, eth_getTransactionReceipt, eth_chainId
    │   └── RpcError.kt
    ├── signer/
    │   ├── EoaSigner.kt                      web3j RawTransactionManager; signs + broadcasts
    │   └── SignedTx.kt
    └── crypto/
        ├── Ecies.kt                          eth-crypto wire-compatible, BouncyCastle
        └── EciesTypes.kt

offramp-lib/                                   NEW
├── build.gradle.kts                           depends on evm-lib
└── src/main/kotlin/xyz/zapp/offramp/
    ├── config/
    │   ├── P2pNetworkConfig.kt               (§3.1)
    │   └── P2pConfigProvider.kt              (§3.3)
    ├── upi/
    │   └── UpiQrParser.kt                    upi://pay?pa=…&am=… regex parser
    ├── p2p/
    │   ├── DiamondAbi.kt                     web3j-generated bindings from p2pdotme-sdk/src/contracts/abis/
    │   ├── Subgraph.kt                       CirclesForRouting (only; OrdersByUser skipped for v1)
    │   ├── CircleRouter.kt                   epsilon-greedy port — direct from routing.ts
    │   ├── Orders.kt                         prepareApproveCalldata, preparePlaceOrderCalldata, prepareSetSellOrderUpiCalldata, parseOrderIdFromReceipt
    │   ├── OrderReader.kt                    getOrder via eth_call (matches readOrderMulticall)
    │   └── RelayIdentity.kt                  ephemeral secp256k1 keypair (in-memory only, never persisted)
    ├── orchestrator/
    │   ├── OfframpOrchestrator.kt            sealed state machine
    │   ├── OfframpStatus.kt                  sealed class
    │   └── OfframpError.kt                   typed errors, including raw revert selectors for debugging
    └── di/
        └── offrampModule.kt                  Koin

ui-lib/.../screen/offramp/                     NEW UI surface (uses existing primitives)
├── scan/
│   ├── OfframpScanScreen.kt                  reuses ScanView; only parser differs
│   ├── OfframpScanViewModel.kt
│   └── OfframpScanState.kt
├── confirm/
│   ├── OfframpConfirmScreen.kt               amount + fees + "Send" button
│   ├── OfframpConfirmViewModel.kt
│   └── OfframpConfirmState.kt
├── progress/
│   ├── OfframpProgressScreen.kt              thin wrapper over TransactionProgressView
│   └── OfframpProgressViewModel.kt           maps OfframpStatus → TransactionProgressState
└── OfframpNavGraph.kt                        registered inside WalletNavGraph (chat-style)
```

Pure Kotlin modules can be unit-tested on JVM (no Robolectric needed). That's a deliberate constraint — ECIES, ABI encoding, and the orchestrator are all framework-independent.

---

## §6 Phased implementation

Each phase is a checkpoint with a verifiable success criterion. Each phase ships only after the previous one passes.

### Phase 0 — Pre-flight (1 day)

Pure research/coordination, no code:

- [ ] Ask Bucky on Discord: *"Do PAY orders (orderType 2) bypass the RP gate the same way SELL orders do, for fresh addresses?"*
- [ ] Ask Bucky: *"Is the INR PAY merchant bot running on Sepolia? Can we coordinate timing for a test?"*
- [ ] Ask Bucky/Git Chad: *"Is there a public mainnet subgraph URL, or do all integrators self-deploy?"*
- [ ] Generate Java/Kotlin bindings from the SDK's ABIs: `web3j` codegen pass over `p2pdotme-sdk/src/contracts/abis/`.

**Success:** clarity on the 3 questions + ABI bindings checked in.

### Phase 1 — ECIES (1-2 days)

The cryptographic go/no-go.

- [ ] Create `evm-lib` module, add BouncyCastle dependency.
- [ ] Implement `Ecies.kt`:
  - `encryptWithPublicKey(pubKeyHex: String, message: String): Encrypted`
  - `decryptWithPrivateKey(privKeyHex: String, encrypted: Encrypted): String`
  - `cipherStringify(encrypted: Encrypted): String`
  - `cipherParse(s: String): Encrypted`
- [ ] Copy test vectors from `p2pdotme-sdk/test/orders/crypto/ecies.test.ts` into a Kotlin unit test.
- [ ] Add a bidirectional round-trip test: encrypt in Kotlin, decrypt via a tiny `bun` script that imports the SDK; encrypt with the SDK, decrypt in Kotlin.

**Success:** all SDK golden vectors pass. The bidirectional round-trip is the actual go/no-go gate — if either direction fails, the integration is dead until we fix the wire format.

**Failure modes to expect:**
- Compressed/uncompressed pubkey mismatch in HMAC input (§4.4 of [UPI_Offramp.md](./UPI_Offramp.md)).
- AES-256-CBC padding scheme drift (BouncyCastle defaults to PKCS#7; verify the SDK matches).
- secp256k1 point compression endianness.

### Phase 2 — EVM primitives (2-3 days)

Everything we need to send a transaction to Base.

- [ ] `EvmKeyDerivation.kt`: from a BIP-39 mnemonic + index `n`, derive `m/44'/60'/0'/0/n` → (privKey, pubKey, address). Use web3j's `Bip32ECKeyPair`.
- [ ] `BaseRpcClient.kt`: OkHttp + JSON-RPC. Methods: `ethCall`, `ethSendRawTransaction`, `ethGetTransactionReceipt`, `ethEstimateGas`, `ethGasPrice`, `ethChainId`, `ethGetTransactionCount` (nonce).
- [ ] `EoaSigner.kt`: takes a `BaseRpcClient` + `ECKeyPair` + chainId, exposes `sendTransaction(to, data, value): Hash` that handles nonce, gas estimation, signing, broadcast, receipt wait. Use web3j's `RawTransactionManager` if it fits cleanly; otherwise hand-roll (it's ~80 LOC).
- [ ] `P2pNetworkConfig.kt` + `P2pConfigProvider.kt` from §3.

**Success:** Unit test that derives an address from a fixed mnemonic + index, signs an arbitrary call, and the hex matches viem's output for the same inputs.

**Smoke test:** integration test that sends a no-op transaction (e.g., `value=0, to=self, data=0x`) on Sepolia and confirms the receipt.

### Phase 3 — P2P calldata + reads (3-4 days)

The SDK-equivalent surface, native Kotlin.

- [ ] `DiamondAbi.kt`: web3j-generated bindings for the relevant facet ABIs. Functions we need: `placeOrder`, `setSellOrderUpi`, `getOrder` (multicall), `getPriceConfig`, `getAssignableMerchantsFromCircle`, ERC-20 `approve`.
- [ ] `RelayIdentity.kt`: `generateRelayIdentity()` returns a fresh secp256k1 keypair. **In-memory only, never persisted.** Different per offramp.
- [ ] `Subgraph.kt`: OkHttp POST to `subgraphUrl`, body `{query: CIRCLES_FOR_ROUTING_QUERY, variables: {currency: <hex32>}}`. Cache last successful response for 1 hour.
- [ ] `CircleRouter.kt`: direct port of `routing.ts`. `EPSILON = 0.25` etc. Calls `getAssignableMerchantsFromCircle` via `BaseRpcClient` for on-chain eligibility.
- [ ] `Orders.kt`:
  - `prepareApproveCalldata(amount: BigInteger): Calldata`
  - `preparePlaceOrderCalldata(params: PlaceOrderParams, circleId: BigInteger, relayIdentity: RelayIdentity): Calldata`
  - `prepareSetSellOrderUpiCalldata(orderId, paymentAddress, merchantPublicKey, updatedAmount, relayIdentity): Calldata` — does the ECIES encryption internally
  - `parseOrderIdFromReceipt(receipt): BigInteger?` — decodes `OrderPlaced` event
- [ ] `OrderReader.kt`: `getOrder(orderId): Order` via `eth_call`. Returns normalized order with status enum.

**Success:** offline unit tests that show our calldata bytes match what `viem`'s `encodeFunctionData` produces for the same inputs. (Use `bun` to capture viem output once, save as fixtures.)

### Phase 4 — Orchestrator (2-3 days)

The state machine that drives the full flow.

```kotlin
sealed class OfframpStatus {
    data object Idle : OfframpStatus()
    data class QuotingBridge(val zecAmount: Zatoshi) : OfframpStatus()
    data class BridgingToBase(val nearDepositTxId: String) : OfframpStatus()
    data class WaitingForUsdcArrival(val derivedAddress: String, val expectedUsdc: BigInteger) : OfframpStatus()
    data class ApprovingUsdc(val txHash: String) : OfframpStatus()
    data class PlacingOrder(val txHash: String) : OfframpStatus()
    data class WaitingForMerchantAcceptance(val orderId: BigInteger) : OfframpStatus()
    data class SendingEncryptedUpi(val orderId: BigInteger, val txHash: String) : OfframpStatus()
    data class WaitingForCompletion(val orderId: BigInteger) : OfframpStatus()
    data class Completed(val orderId: BigInteger, val actualFiat: BigInteger) : OfframpStatus()
    data class Failed(val reason: OfframpError, val orderId: BigInteger?) : OfframpStatus()
}
```

For v1, the bridge step is **skipped during Sepolia testing** — Phase 5 tests fund the EOA directly from Circle's faucet.

- [ ] `OfframpOrchestrator.kt`: takes a `OfframpRequest` (recipientUpi, fiatAmount, currency), emits a `Flow<OfframpStatus>`.
- [ ] Idempotency: if killed mid-flow, must be resumable from the last known status. Persist `orderId` + `derivedEoaIndex` to encrypted prefs as soon as `placeOrder` returns a hash.
- [ ] Poll cadence: `getOrder` every 3 seconds while waiting for status changes. 5-min timeout on each waiting state with a "still waiting" UI affordance.

**Success:** unit-tested state machine + a runnable integration test that drives the orchestrator against a Sepolia EOA we pre-fund with USDC and reach `Completed` with team-side merchant coordination.

### Phase 5 — UI + Sepolia E2E (3-4 days)

The visible product.

- [ ] `OfframpScanScreen` — reuses existing `ScanView`. UPI parser triggers on `upi://pay?` prefix.
- [ ] `OfframpConfirmScreen` — amount input (capped at $99 worth), fee preview from `getSmallOrderFixedFeePay` + circle fee, "Send ₹X" button.
- [ ] `OfframpProgressScreen` — wraps `TransactionProgressView`, observes `OfframpStatus` flow, maps each status to a `TransactionProgressState`.
- [ ] `OfframpNavGraph` registered inside `WalletNavGraph`.
- [ ] Entry point: add an "Offramp" action to wherever feels natural in the existing UI (likely Send tab or a new top-bar action). Defer the entry-point design to a small AskUserQuestion when we get there.

**Success:** real PAY order placed and completed on Base Sepolia from a Zodl debug APK with manually-funded USDC.

### Phase 6 — Bridge integration (2-3 days)

Wire the existing NEAR Intents flow into the orchestrator.

- [ ] Extend `NearSwapDataSourceImpl` (or wrap it) to request two quotes in parallel: ZEC → Base USDC + ZEC → Base ETH. Net out as a single ZEC debit shown to the user.
- [ ] Orchestrator gains the `QuotingBridge`/`BridgingToBase`/`WaitingForUsdcArrival` states.
- [ ] Add a "low gas balance" pre-flight check that triggers a top-up bridge if the derived EOA's ETH balance is < 0.0002 ETH (~$0.50).

**Success:** end-to-end mainnet test with a real ZEC→INR PAY for $5. This is also the Track C test from the research doc — confirms PAY's RP behavior on mainnet for fresh addresses.

### Phase 7 — Hardening (1-2 days)

Pre-release sweep.

- [ ] Mainnet config values confirmed in `gradle.properties`.
- [ ] Subgraph URL for mainnet sorted (§4.2).
- [ ] Logging: BuildConfig flag `OFFRAMP_LOG_LEVEL` defaults to `info`; release builds never include order IDs or addresses in non-error logs.
- [ ] Error messages: revert selectors mapped to human strings where known (`0x91da284f` → "Need reputation points first"; `0x5d04ff4c` → "No merchant available right now, try again later").
- [ ] Manifest hygiene per project root `CLAUDE.md` (no new permissions needed — we already have INTERNET).
- [ ] Release-config diff against last tag (per project root `CLAUDE.md`).

---

## §7 Test plan

### 7.1 Sepolia test matrix

Run before declaring Phase 5 complete:

| Scenario | Currency | Amount | Expected |
|---|---|---|---|
| Happy path | INR | $5 | Reaches `Completed` |
| Below small-order threshold | INR | $1 | Reaches `Completed`, fixed fee applied |
| Above $99 cap | INR | $100 | Blocked client-side, never reaches contract |
| Merchant offline | INR | $5 | Reaches `WaitingForMerchantAcceptance`, 5-min timeout fires gracefully |
| Invalid UPI string | — | — | Rejected at QR parse time |
| Bridge fails mid-flow | INR | $5 | (Phase 6 only) — recoverable error, no orphan state |

### 7.2 Pre-test coordination

Before each Sepolia test session, message Bucky:
- "Running PAY INR test on `<our test address>` in ~5 min. Is the INR merchant bot up?"
- If we hit `0x91da284f` despite Bucky's expectation that PAY skips RP, share the tx hash and ask for decode.

### 7.3 Mainnet smoke test (Phase 6)

One $5 PAY from a fresh derived address. Single criterion: does PAY revert with `0x91da284f` for fresh sub-$99 addresses?
- **If revert:** PAY has the same RP gate as BUY. v1 ships with an RP onboarding flow (manual grant, or Reclaim social verify) or doesn't ship.
- **If success:** v1 ships with the $99 cap and no RP onboarding needed.

This single test answers the one remaining big unknown.

---

## §8 Engineering hygiene

Per project root `CLAUDE.md` and the `zodl-style` skill:

- Load `.claude/skills/zodl-style/SKILL.md` before any `.kt` edits.
- Strings to `Res.string.*`, not inline.
- Visual styling via `ZappTheme`, never `ZashiColors/Typography/Dimensions`.
- Screens stay thin — composition happens in `*View.kt`, state in `*ViewModel.kt`, sealed `*State.kt` per screen.
- New modules registered through Koin only; no manual singletons.

For the new pure-Kotlin modules (`evm-lib`, `offramp-lib`):
- No Android imports. Unit-testable on JVM.
- All public functions return either a typed result (`Result<T, OfframpError>` or `Either<E, T>` — pick one and stay consistent) or throw only well-typed exceptions.
- No `println`; route logs through a `Logger` interface (matches the SDK's pattern).
- All numeric amounts are `BigInteger` at the wire level; only the UI layer converts to user-facing decimals.

---

## §9 Open questions tracker

These need answers, but only the first blocks v1:

| # | Question | For | Blocks |
|---|---|---|---|
| 1 | Does PAY (orderType 2) skip the RP gate that BUY has? | Bucky | Phase 6 / mainnet ship |
| 2 | Is there a canonical mainnet subgraph URL or do we self-deploy? | Bucky / Git Chad | Phase 7 |
| 3 | Is the Sepolia INR PAY merchant bot reliably running? If not, when? | Bucky | Phase 5 tests |
| 4 | What does the contract return for `getSmallOrderFixedFeePay` on Sepolia for INR? | self (eth_call) | Phase 5 UI |
| 5 | What's the 72h auto-cancel behavior? Does the USDC return to the original payer, or to the smart account, or stuck? | Bucky / docs | Phase 7 polish |

---

## §10 What gets deferred to v1.1+

Explicitly out of scope for v1:

- BUY and SELL order types (PAY only)
- Order history UI (no `OrdersByUser` subgraph query)
- Dispute UX (`raiseDispute`)
- ZK KYC (Reclaim / Anon Aadhaar / ZK Passport)
- Orders > $99
- Currencies other than INR
- ERC-4337 / AA / gas sponsorship
- Self-hosted subgraph (only if §9 #2 forces it)
- Hot network switching (debug-build override)
- Order cancellation UX
- Bridge slippage refund handling (whatever USDC dust is left in the EOA stays there)

---

## §11 Starting the work

Next session (Phase 1) I'll:

1. Create the `evm-lib` module skeleton with BouncyCastle.
2. Write `Ecies.kt`.
3. Port the golden test vectors from `p2pdotme-sdk/test/orders/crypto/ecies.test.ts`.
4. Get the bidirectional round-trip passing.

If Phase 1 passes cleanly, Phase 2-3 are mechanical from there. The whole v1 is ~14-18 working days of engineering, ~3-4 calendar weeks accounting for Discord-coordination delays.
