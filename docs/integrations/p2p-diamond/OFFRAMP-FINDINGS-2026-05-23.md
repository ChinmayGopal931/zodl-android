# P2P.me offramp — Android integration notes (2026-05-23)

Empirical findings from a long debug session on Base Sepolia + mainnet using
`~/dev/p2p-scripts`. End-to-end proof: mainnet order **547444** went
`placed → accepted → paid → COMPLETED` (0.4 USDC / 36 INR PAY, merchant
`0x3a28558a…`, Telegram `Yuvasri2002`).

---

## TL;DR

1. **No KYC for PAY/SELL ≤ $100.** `userTxLimit` defaults to `(buy:0, sell:100_000_000)`.
2. **Send a full `upi://pay?…&am=…&cu=INR` URI** to `setSellOrderUpi`, NOT a bare VPA.
3. **`updatedAmount` MUST be non-zero for PAY.** Zero → contract auto-cancels.
4. **Place at live `sellPrice` (not a user-typed fiat).** The contract rewrites
   `fiatAmount = usdcAmount × sellPrice(live)` regardless of what you pass.
5. **No quote API.** Read `sellPrice` from `getPriceConfig` and multiply.
   The contract already baked any spread into it — adding your own double-charges.
6. **Persist the relay identity** (separate ECDH keypair from EOA) to Keystore.
   Without persistence, you can never decrypt the merchant's UPI VPA on past orders.
7. **paid → completed takes ~5–6 min.** Set polling timeout ≥ 10 min. No user action needed.
8. **Smoke-test at 0.4 USDC / 36 INR**, not 0.99 / 85 — the bigger band routes
   to merchants that auto-cancel +4s post-setUpi for opaque reasons.
9. **Skip Sepolia E2E.** The only INR PAY merchant (`0xa8e665…`) is bot-offline
   since 2026-04-16 (on-chain `isOnline=true` but dead).

---

## 1. Network configs

```
Sepolia   Diamond  0xeb0BB8E3c014D915D9B2df03aBB130a1Fb44beb9
          USDC     0x4095fE4f1E636f11A95820BA2bB87F335Bd1040d   (p2p test token)
          Subgraph https://api.studio.thegraph.com/query/1745491/event-indexer/version/latest
          RepMgr   0xEF2E957deF0EA7dAf2D6579f0D3963a5D7A6Bd77   (BUY only)

Mainnet   Diamond  0x4cad6eC90e65baBec9335cAd728DDC610c316368
          USDC     0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913   (Circle's Base USDC)
          Subgraph https://gateway.thegraph.com/api/<key>/deployments/id/QmdGt7hftfZXHoMwBnPLLj8revQmxscrPtcGh2E2dvf3RN
```

Authoritative ABIs at `~/dev/subgraph/abis/`.

---

## 2. Order types and status codes

```
OrderType:    BUY = 0  (onramp, KYC-gated)
              SELL = 1 (offramp via VPA input;  updatedAmount=0 convention)
              PAY = 2  (offramp via QR scan;    updatedAmount MUST be non-zero)

OrderStatus:  PLACED=0  ACCEPTED=1  PAID=2  COMPLETED=3  CANCELLED=4
```

For zodl-android: use **PAY (2)** if you have a QR scanner or build the URI
from form fields; **SELL (1)** if you only collect a bare VPA.

---

## 3. Lifecycle (mainnet timings from orderId 547444)

```
0:00   placeOrder            user → contract           PLACED
+36s   assignMerchants       executor → contract       (still PLACED)
       acceptOrder           merchant → contract       ACCEPTED
+40s   setSellOrderUpi       user → contract           PAID  (merchant marks fast)
+5m28  completeOrder         merchant → contract       COMPLETED
```

Polling cadence: 10s while in PAID (mirrors `user-app-client/src/hooks/use-get-order-by-id.ts:44`).

### placeOrder signature

```
OrderFlowFacet.placeOrder(
  string pubKey, uint256 amount, address recipientAddr, uint8 orderType,
  string userUpi, string userPubKey, bytes32 currency,
  uint256 preferredPCConfigId, uint256 circleId, uint256 fiatAmountLimit
)
```

**ABI takes NO `fiatAmount` arg.** Whatever SDK param you pass is dropped at
the contract layer. The contract reads live `sellPrice` and stamps
`fiatAmount = amount × sellPrice` itself. Evidence: order 547444 placed with
`P2P_FIAT_AMOUNT=36000000` stored as `37280000` on-chain (= 0.4 × 93.20).

### Other lifecycle calls

```
OrderFlowFacet.cancelOrder(orderId)             user (PLACED only) or merchant
OrderFlowFacet.setSellOrderUpi(orderId, encUpi, updatedAmount)   user (after ACCEPTED)
OrderFlowHelper.acceptOrder(orderId, ...)       merchant-only
OrderFlowHelper.completeOrder(orderId, merchantUpi)              merchant-only
OrderProcessorFacet.autoCancelExpiredOrders([orderIds])          permissionless
OrderProcessorFacet.raiseDispute(orderId, redactTransId)         user
```

There is **no user-side `completeOrder`**. Just poll until status flips.

---

## 4. Reputation / KYC — the $100 floor

```
userTxLimit(unrep'd_wallet, "INR") → (buy: 0, sell: 100_000_000)
                                            ↑              ↑
                                            KYC-gated      $100 default cap
```

ABI corroboration: `OrderFlowFacet.json` has `DailyBuyOrderLimitExceeded` and
`MonthlyBuyOrderLimitExceeded` errors but no SELL/PAY equivalents. Daily/monthly
volume caps are a BUY-only construct. `UserHasNoReputation` (`0x071ea33c`)
fires for BUY only.

**Action**: don't gate the offramp UI on RP/KYC for sub-$100 orders. Hide the
buy flow if you don't want to ship KYC.

---

## 5. Pricing — how the canonical UI sets the quote

**There is no quote API.** No `getQuote(amount) → quoteId`. The canonical UI
reads `sellPrice` live from the Diamond and multiplies.

### 5a. The read

```typescript
// user-app-client/src/hooks/use-price-config.ts:24-31
const cfg = await prices.getPriceConfig({ currency: "INR" });
const buyPrice  = Number(formatUnits(cfg.buyPrice,  6));   // e.g. 95.50
const sellPrice = Number(formatUnits(cfg.sellPrice, 6));   // e.g. 93.20
// baseSpread and buyPriceOffset are NEVER read — they're already baked in
```

**Do NOT apply your own spread.** The protocol baked it into `sellPrice`.
Adding one double-charges the user.

### 5b. Conversion

```
fiat = floor(usdc × sellPrice, 2 decimals)
usdc = floor(fiat / sellPrice, 6 decimals)
```

Canonical client uses `truncateAmount` (floor, not round) so display ≤ on-chain.

### 5c. Fees

```typescript
// user-app-client/src/core/fees.ts:22-33
getFeeConfig(currency, "pay" | "sell" | "buy") → { smallOrderThreshold, smallOrderFixedFee }

// user-app-client/src/lib/utils.ts:611-617
fee = (amount ≤ smallOrderThreshold) ? smallOrderFixedFee : 0
```

Fee is **added on top** of USDC (escrow = `usdcAmount + fee`). Verified
empirically: order 547444 placed 0.4 USDC, fee 0.1 USDC, actualUsdc 0.5 USDC.

### 5d. Validation

```kotlin
// Balance check INCLUDES fee
totalDebit = usdc + fee
canPlace   = totalDebit ≤ balance  &&  usdc ≤ txLimits.sellLimit

// MAX button:
effectiveMax = min(balance, sellLimit) - fee
```

### 5e. Slippage

There's no quote expiry. Between preview and broadcast, `sellPrice` can move.
`placeOrder` takes `fiatAmountLimit` for slippage protection — pass
`floor(expectedFiat × 0.99)` for 1% protection. Canonical client passes `0`
(disabled); recommend ON for mobile UX.

### 5f. Refetch policy (Android)

- Refetch on screen open
- Refetch every 30s while amount input is visible
- Refetch right before broadcast; if moved >0.5%, re-show preview

### 5g. Worked example (order 547444)

```
sellPrice            93.20 INR/USDC
smallOrderThreshold  50 USDC
smallOrderFixedFee   0.1 USDC (PAY)
sellLimit            100 USDC (unrep'd)

User enters 0.4 USDC:
  fiat        = 0.4 × 93.20 = ₹37.28
  fee         = 0.1 USDC
  totalDebit  = 0.5 USDC
  
After place + complete, on-chain:
  usdcAmount  400_000  fiatAmount      37_280_000
  fixedFeePaid 100_000  actualUsdcAmount 500_000
```

---

## 6. setSellOrderUpi payload — what was wrong, what's right

### ❌ Current zodl-android (auto-cancels every order)

```kotlin
// UpiOfframpVM.kt:247        — bare VPA, no upi:// prefix
recipientUpi = "user@okhdfcbank"
// OfframpOrchestrator.kt:343 — encrypts the bare VPA
Ecies.encryptWithPublicKey(merchantPubKey, request.recipientUpi)
// DiamondCalls.kt:55-66      — zero amount
setSellOrderUpi(orderId, encryptedBareVpa, updatedAmount = BigInteger.ZERO)
```

Contract sees bare VPA + zero amount → emits `CancelledOrders` in the same tx.

### ✓ Fix (mirrors `user-app-client/src/pages/order/pay/accepted.tsx:104-148`)

1. **Build a full URI**: `upi://pay?pa=<vpa>&pn=<name>&am=<inr>&cu=INR`
   (scanner OR form fields).
2. **Parse it** with `parseQR({qrData, currency, sellPrice})` from
   `@p2pdotme/sdk/qr-parsers` → `{ paymentAddress, amount: { usdc, fiat } }`.
3. **Compute updatedAmount**:
   ```kotlin
   var updatedAmount = order.usdcAmount                  // default: keep placed
   if (parsed.amount != null) updatedAmount = truncate6(parsed.amount.usdc)
   updatedAmount = maxOf(updatedAmount, order.usdcAmount) // never reduce below placed
   ```
4. **Call setSellOrderUpi** with the FULL URI as `paymentAddress` (SDK handles
   ECIES) and the computed `updatedAmount`.
5. **Re-approve USDC if updatedAmount > placed.** Web client approves
   `MAX_UINT256` upfront; just-in-time also works.

### Why §5 (place at live rate) and §6 (cap updatedAmount) are both needed

If you place at a below-market rate but recompute updatedAmount at market,
`updatedAmount < placed` → merchant's margin disappears → merchant cancels
+4s after setUpi. We hit this today even with the URI fix in place. Placing
at the live `sellPrice` makes parsed ≈ placed → no reduction.

---

## 7. UPI encryption + relay identity (critical)

**The merchant's UPI VPA IS on-chain**, encrypted in `encMerchantUpi`
(SELL/PAY) or `encUpi` (BUY). Written by the merchant at `completeOrder`
(SELL/PAY) or `acceptOrder` (BUY). ECDH-encrypted between user's relay
pubKey (`order.userPubKey`) and merchant's relay pubkey (`order.pubkey`) —
either party can decrypt with their respective privKey.

Canonical decrypt:

```typescript
// user-app-client/src/hooks/use-receipt-share.ts:46-79
const decrypted = await orders.decryptPaymentAddress({
  encrypted: order.encMerchantUpi,
});
paymentFrom = decrypted.value;  // ← merchant's actual UPI VPA
```

### The relay identity — separate from the EOA wallet

The SDK uses a separate ECDH keypair for on-order encryption (`userPubKey` on
the order). Identity resolution
(`p2pdotme-sdk/src/orders/relay-identity/resolve.ts:49`):

1. `config.relayIdentity` (used as-is), else
2. `config.relayIdentityStore.get()` (used if non-null), else
3. fresh `createRelayIdentity()` + `store.set(it)`

Default store is `createInMemoryRelayStore()` — lost at process exit.

**Without persistence, you can never decrypt past orders.** Confirmed
empirically: today's orders 547440/547443/547444 are now permanently
undecryptable because the test rig didn't persist the identity at place time.

### Android implementation

```kotlin
class KeystoreRelayStore(ctx: Context) : RelayIdentityStore {
  private val prefs = EncryptedSharedPreferences.create(
    "p2p_relay_identity", masterKeyAlias, ctx,
    AES256_SIV, AES256_GCM,
  )
  override suspend fun get(): RelayIdentity? =
    prefs.getString("identity", null)?.let(Json::decodeFromString)
  override suspend fun set(id: RelayIdentity) {
    prefs.edit().putString("identity", Json.encodeToString(id)).apply()
  }
}

val orders = createOrders(OrdersConfig(
  publicClient = ..., diamondAddress = ..., usdcAddress = ..., subgraphUrl = ...,
  relayIdentityStore = KeystoreRelayStore(applicationContext),
))
```

The web client uses `createLocalStorageRelayStore()`. The test rig now uses
`createFileRelayStore(".relay-identity-<network>.json")`.

---

## 8. Merchant pool stratification (open mystery)

| amount band | merchants observed | outcome |
|---|---|---|
| 0.99 USDC / 85 INR | `0xE5d5f3`, `0x820958`, `0xecAfc5` | accept → cancel +4s |
| **0.4 USDC / 36 INR** | **`0x3a28558a`, `0x3cae02`** | **→ paid → COMPLETED** |

Multiple mainnet merchants in the 0.99/85 band cancel +4s after a correctly
shaped setSellOrderUpi. The central executor (`~/dev/executor`) has zero
user-side filters — the cancel logic lives in each merchant's own off-chain
bot, which we can't see.

**For QA**: always smoke at 0.4 USDC / 36 INR. **For prod**: let users pick
freely; log enough data on post-accept cancellation to correlate with merchant.

---

## 9. Subgraph access

**Field naming quirk** (cost us hours): the entity is `Orders` (plural by
accident), so The Graph generates:

- `orders(id: ID!)` — singular, takes the entity's Bytes! ID
- **`orders_collection(where:, ...)`** — plural list

Querying `orders(where:...)` returns the cryptic error
`No value provided for required argument: \`id\``. Use `orders_collection`.

Reference: `user-app-client/src/core/p2pdotme/subgraph/queries.ts:25-53`.

### Useful query for "everything about my order"

```graphql
{
  orders_collection(where: { orderId: "547444" }, first: 1) {
    orderId  type  status  circleId
    acceptedMerchantAddress  acceptedPCId
    usdcAmount  fiatAmount  actualUsdcAmount  actualFiatAmount
    fixedFeePaid  merchantRewardAmount
    placedAt  acceptedAt  paidAt  completedAt  cancelledAt  cancelledBy
    encUpi  encMerchantUpi  pubkey  userPubKey
    disputeStatus  disputeRedactTransId
    merchant { merchant  telegramId }
  }
}
```

For "why won't anyone accept my order?", query `circleMerchants(where:
{ circleId: $cid })` and check `isOnline`, `isBlacklisted`, `isOngoingOrder`,
`paymentChannels.dailyVolume/monthlyVolume`. Watch for stale `onlineAt` with
no `offlineAt` — that's the Sepolia bug (merchant bot died without toggling
off).

---

## 10. Direct asks for the Android implementer

1. **Fix `setSellOrderUpi` payload** (§6): full URI + non-zero updatedAmount.
2. **Add `KeystoreRelayStore`** (§7), pass to `createOrders(...)`. Receipt UI
   and dispute UX are dead without it.
3. **Wire the pricing pipeline** (§5):
   - Live `sellPrice` via `getPriceConfig`, refetch on screen + every 30s
   - Display `usdcAmount × sellPrice`, never the user-typed INR
   - BigDecimal scale=6 internally; floor for display
   - Fetch `getFeeConfig`, show fee as a separate line, validate
     `balance ≥ usdc + fee`
   - Pass `fiatAmountLimit = floor(expectedFiat × 0.99)` (1% slippage)
4. **Polling timeout for paid→completed ≥ 10 min.** Mainnet observed 5m 28s.
5. **No KYC gate for PAY ≤ $99.**
6. **Receipt UI**: show merchant wallet (truncated), Telegram handle
   (linkable), and decrypted UPI VPA. All available via subgraph + relay key.
7. **QA path**: `~/dev/p2p-scripts` first → Android mainnet at 0.4 / 36.
   Skip Sepolia until p2p.me restarts the merchant bot.

---

## 11. Open questions

- Why does 0.99 / 85 INR fail across multiple merchants? Ask Aash (Discord)
  with orderIds 547440 / 547443.
- Is `drashagopal@okhdfcbank` actually reachable? Order 547444 completed
  on-chain but recipient unverified.
- For thirdweb AA path (Android wraps with UserOps): does the relay-identity
  flow survive the wrapping? Need to test on Android, not just the Node rig.
- For SELL (orderType=1): does `updatedAmount=0` actually work as the
  canonical client assumes? Untested in this session.

---

## Appendix: contract selectors

From `~/dev/subgraph/abis/`:

```
OrderFlowFacet:
  placeOrder(string, uint256, address, uint8, string, string, bytes32, uint256, uint256, uint256)
  setSellOrderUpi(uint256, string, uint256)
  cancelOrder(uint256)
  assignMerchants(uint256)
  userTxLimit(address, bytes32) → (uint256 buy, uint256 sell)

OrderFlowHelper (merchant-only except getDayKey):
  acceptOrder(uint256, string, string)
  completeOrder(uint256, string)
  paidBuyOrder(uint256)

OrderProcessorFacet:
  raiseDispute(uint256, uint256)
  autoCancelExpiredOrders(uint256[])

ReputationManager (BUY only):
  userTxLimit storage backing
  socialVerify, submitAnonAadharProof, zkPassportRegister, etc.
```

### Key errors (decode in `KnownContractErrors.kt`)

```
CancelledOrders                              auto-cancel inside setUpi (bad payload)
BuyOrderAmountExceedsLimit                   BUY KYC tier hit
DailyBuyOrderLimitExceeded                   BUY only
MonthlyBuyOrderLimitExceeded                 BUY only
SellOrderAmountExceedsLimit                  > sellLimit (default 100 USDC)
UserHasNoReputation  (0x071ea33c)            BUY on unrep'd wallet
NotEnoughEligibleMerchants                   no merchant matched circle/amount
CircleNotActive / CurrencyNotSupported / ExchangeNotOperational    global state
```
