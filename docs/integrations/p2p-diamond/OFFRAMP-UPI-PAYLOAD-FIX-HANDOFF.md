# UPI offramp — payload-format fix handoff

**Branch context:** `feat/upi-offramp` (tip: `b826dcc0` at handoff time).
**Status:** **ship-blocker** for the in-app UPI offramp. The Android orchestrator sends a malformed `setSellOrderUpi` payload; the p2p.me Diamond auto-cancels every order inside the user's own setUpi tx. Verified empirically against the production Diamond on Base mainnet on 2026-05-23.

The fix is a localized payload-shape change in two Kotlin files plus a UI capture step. No contract/SDK changes — the official p2p web client (`~/dev/user-app-client`) already does it correctly; we were modelling on the SDK's `example/make-pay-order.ts`, which is misleading.

---

## 1. Evidence

External Node/bun test rig at `~/dev/p2p-scripts/` (mirrors the orchestrator end-to-end with the same SDK and an EOA signer) reproduced the bug across four mainnet runs and confirmed the fix.

| # | order id | payload | merchant | outcome | tx |
|---|----------|---------|----------|---------|----|
| 1 | 546545 | bare VPA + `updatedAmount=0` | `0xecAfc581…` | accepted → **cancelled in same tx as setUpi** | [setUpi 0x92ae25…](https://basescan.org/tx/0x92ae255af23859a0848643b7dbe2657b8315d35d3f6f1a4fab7eaea89a201ed8) |
| 2 | (placeOrder reverted) | — | — | reverted via merchant-race | [0x9c391d…](https://basescan.org/tx/0x9c391dc6581fe4f7ad31b9ec8f17df872f667056c893c57712a0f0709a2f6074) |
| 3 | 546563 | bare VPA + `updatedAmount=0` | `0xE5d5f3…` | accepted → **cancelled in same tx as setUpi** | [setUpi 0x0e0728…](https://basescan.org/tx/0x0e0728f87dae0784be7423ec7e8535a77913319d19aa38e0ad4201c235ee244b) |
| 4 | 546578 | **full `upi://pay?…&am=36.00&cu=INR` + parsed `updatedAmount=387847`** | `0x3cae02…` | accepted → **paid** → cancelled +82s (off-chain UPI failure) | [setUpi 0xba0077…](https://basescan.org/tx/0xba00770c205c313994bf84ec86bdcde0794e90ca54d635baecafeccc19ee60a9) |

Run #4 is the proof: same orchestrator flow, only the setSellOrderUpi payload shape changed, and the order made it to `paid` for the first time. The terminal cancel after that was a separate tx originated by the merchant — an off-chain UPI delivery failure unrelated to the contract path (the VPA used was a guess; provenance unverified).

Decoded event in runs #1 and #3 setUpi tx logs: `CancelledOrders` (topic0 `0x24e0e750e9b0658d9179ad1662912205ec2f1b2dc00bcbda15d801da1bb5a35a`) emitted in the same block, same tx, same call — i.e. the contract executed our setSellOrderUpi *and* cancelled the order atomically. Not a merchant-side decision; a contract-side invariant rejection.

---

## 2. Root cause

The Diamond's `setSellOrderUpi(uint256 orderId, string encUpi, uint256 updatedAmount)` is invoked with two malformed args:

1. **`encUpi` is an ECIES-ciphered bare VPA** (`name@bank`) instead of a ciphered full UPI URI (`upi://pay?pa=…&pn=…&am=…&cu=INR`). The merchant decrypts and runs `parseQR` from `@p2pdotme/sdk/qr-parsers` over the plaintext. A bare VPA passes the regex but produces a `ParsedQR` with no `amount` field — the merchant bot rejects.
2. **`updatedAmount` is hardcoded to `BigInteger.ZERO`** instead of the USDC-equivalent of the QR's `am=` field (6-decimal, e.g. `parseUnits("0.387847", 6)`). The contract appears to require `updatedAmount > 0` for PAY orders (the official client always passes a non-zero value).

When either invariant fails the contract emits `CancelledOrders` synchronously inside the user's setSellOrderUpi call. The user's escrowed USDC is refunded — no funds lost — but the order is dead and the user has paid gas for nothing.

### Exact loci in the codebase

- `ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/swap/upi/UpiOfframpVM.kt:242-251`
  ```kotlin
  val upi = upiText.value
  if (upi.isBlank() || !UpiQrParser.validateUpiId(upi)) return  // ← validates bare VPA, not URI
  // …
  navigationRouter.forward(
      UpiOfframpProgressArgs(
          recipientUpi = upi,                                    // ← bare VPA propagated forward
          usdcAmountMicro = usdcMicro.toString(),
          currency = CURRENCY,
      ),
  )
  ```

- `offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/orchestrator/OfframpOrchestrator.kt:341-353` (the setUpi broadcast inside `awaitMerchantAndComplete`)
  ```kotlin
  val cipherHex = Ecies.cipherStringify(
      Ecies.encryptWithPublicKey(verifiedMerchantPubKey(orderId, accepted), request.recipientUpi),
                                                                  // ↑ bare VPA going straight into ECIES
  )
  submitter.sendTransaction(
      to = network.diamondAddress,
      data = DiamondCalls.setSellOrderUpiCalldata(
          orderId = orderId,
          encryptedUpiHex = cipherHex,
                                                                  // ↑ no updatedAmount passed — defaults to 0
      ),
  )
  ```

- `offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/p2p/DiamondCalls.kt:55-66`
  ```kotlin
  fun setSellOrderUpiCalldata(
      orderId: BigInteger,
      encryptedUpiHex: String,
      updatedAmount: BigInteger = BigInteger.ZERO,           // ← default-zero is the wrong default for PAY
  ): ByteArray = …
  ```

### Reference implementation (the right shape)

`~/dev/user-app-client/src/pages/order/pay/accepted.tsx:104-149` — the official web client:

```ts
const parseResult = await parseQRData(qrString, order.currency, priceConfig.sellPrice, order.id);
// parseResult.value = { paymentAddress: "name@bank", amount: { usdc: 0.387, fiat: 36 } }

let updatedAmount = Number(order.amount);
if (amount) updatedAmount = truncate6(Number(amount.usdc));   // ← parsed USDC, not zero

await setSellOrderUpiMutation.mutateAsync({
    orderId: BigInt(order.id),
    paymentAddress: qrString,                                  // ← FULL URI, not parsed VPA
    merchantPublicKey: order.pubkey,
    updatedAmount: parseUnits(updatedAmount.toString(), 6),    // ← 6-decimal USDC bigint
});
```

`~/dev/user-app-client/src/hooks/use-order-flow.ts:70-100, 206-211` — `ensureUSDCApproval()` runs before *both* placeOrder and setSellOrderUpi (it approves `MAX_UINT256` on first hit and short-circuits if allowance ≥ MAX/2), because a non-zero `updatedAmount` may trigger an additional `transferFrom` when the QR amount exceeds the originally-placed principal.

---

## 3. The fix

Three changes, smallest blast-radius first:

### 3a. Plumb a full URI through `OfframpRequest.recipientUpi`

Change the contract of `recipientUpi` from "bare VPA" to "scanned/constructed UPI URI (`upi://pay?…`)". Update the `require(recipientUpi.isNotBlank())` in `OfframpRequest.kt:19` to also validate URI shape — reuse `UpiQrParser.parseQr(...)` (already in the codebase at `offramp-lib/.../UpiQrParser.kt`) to assert that the URI parses and yields both `pa` and `am`.

### 3b. UI: build (or scan) a real URI before forwarding

In `UpiOfframpVM.kt:227-252`, replace the bare-VPA path with one of:

- **Form approach (simpler, no new screens):** keep the existing VPA text field, add an "amount in INR" text field (or compute INR from the existing USDC field × live `sellPrice` from `GetUpiOfframpRateUseCase`), and construct the URI client-side:
  ```kotlin
  val qr = "upi://pay?" + listOf(
      "pa" to vpa,
      "pn" to (payeeName.ifBlank { "Recipient" }),
      "am" to inrAmount.toPlainString(),
      "cu" to "INR",
  ).joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
  ```
- **Scanner approach (matches the web client UX):** wire up a UPI QR scanner (CameraX + ML Kit), capture the raw payload, validate with `UpiQrParser.parseQr`, forward the raw payload (not the parsed VPA) to `UpiOfframpProgressArgs.recipientUpi`. There's already a `UpiQrParseResult` sum type ready for this in `offramp-lib`.

Either way, the value passed to `UpiOfframpProgressArgs.recipientUpi` must be a string that `UpiQrParser.parseQr` accepts and that contains an `am=` param.

### 3c. Orchestrator: derive and pass `updatedAmount`

In `OfframpOrchestrator.awaitMerchantAndComplete` (around `OfframpOrchestrator.kt:336-368`), after the merchant accepts and before encrypting, parse the URI and compute the USDC delta:

```kotlin
val parsedQr = UpiQrParser.parseQr(request.recipientUpi).requireSuccess()
val sellPriceInrPerUsdc = priceConfigReader.sellPrice(request.currency)  // existing GetUpiOfframpRateUseCase signal
val updatedAmountMicros = parsedQr.amount?.let { fiat ->
    Usdc6.fromBigDecimal(fiat.divide(sellPriceInrPerUsdc, 6, HALF_UP))
} ?: error("URI must contain am=")  // 3a guarantees this; tighten the type if you can

// existing encrypt call unchanged — the plaintext is the full URI, not a bare VPA
val cipherHex = Ecies.cipherStringify(
    Ecies.encryptWithPublicKey(verifiedMerchantPubKey(orderId, accepted), request.recipientUpi),
)

submitter.sendTransaction(
    to = network.diamondAddress,
    data = DiamondCalls.setSellOrderUpiCalldata(
        orderId = orderId,
        encryptedUpiHex = cipherHex,
        updatedAmount = updatedAmountMicros.micros,
    ),
)
```

Remove the `= BigInteger.ZERO` default on `DiamondCalls.setSellOrderUpiCalldata` (`DiamondCalls.kt:55-66`) so future callers can't fall into the same hole — make the third arg required, force every call site to think about it.

### 3d. Approval cadence

If the parsed `updatedAmountMicros > request.usdcAmount`, the contract pulls the delta during setSellOrderUpi. Today the orchestrator only approves once (`OfframpOrchestrator.kt:142-149`, `Erc20Calls.approveCalldata(diamond, request.usdcAmount)`). Either:

- Approve `Usdc6.MAX` once and skip on resume (matches the web client's MAX_UINT256), or
- Re-call approve in `awaitMerchantAndComplete` immediately before setSellOrderUpi when `updatedAmountMicros > request.usdcAmount`.

The first is simpler and matches the production reference. Adds a one-time minor security note (infinite allowance) — call it out in the PR.

### 3e. Tests

- `OfframpOrchestratorTest`: extend with a case where `setSellOrderUpi` is called with `recipientUpi = "upi://pay?pa=foo@bar&am=42&cu=INR"`; assert the encoded calldata contains `updatedAmount = round(42 / sellPrice * 1e6)`. Reject in test setup if a future change passes a bare VPA.
- `UpiQrParserTest`: already has the URI-parsing coverage; add a case for the assembled URI shape that `UpiOfframpVM` produces.
- `KnownRevertsTest`: nothing to change; the `UpiAlreadySent` and friends are unaffected.

---

## 4. Validation plan

After the change lands:

1. Build the SDK once: `cd ~/dev/p2pdotme-sdk && bun run build`.
2. Mirror the orchestrator's behaviour in the Node rig:
   ```
   cd ~/dev/p2p-scripts
   # .env: MNEMONIC, P2P_NETWORK=mainnet, P2P_RPC_URL_BASE_MAINNET, P2P_SUBGRAPH_URL_MAINNET,
   # P2P_USDC_AMOUNT=400000, P2P_FIAT_AMOUNT=36000000, P2P_PAYEE_ADDRESS=<known-good-VPA>
   bun src/offramp.ts
   ```
   The script already uses the corrected payload shape (full URI + parsed `updatedAmount`); the merchant should transition `placed → accepted → paid → completed` end-to-end if the destination VPA is valid. Successful path was achieved in test run #4 above through `paid`; only the off-chain UPI step failed.
3. Install the Android build (`./gradlew :app:installZcashtestnetFossDebug` for testnet; for the in-app mainnet flow follow `gradle.properties` overrides per `CLAUDE.md`) and run a small-amount offramp against a VPA you control. Watch the orchestrator's `WaitingForCompletion` status — `paid → completed` is the proof.

The wallet `0x6149e18ED284511DcB8FD0218D40Ad41B08a1b57` (mnemonic in `~/dev/p2p-scripts/.env`) still has ~1 USDC on mainnet and 10 test-USDC on Sepolia from this round of testing — reuse it.

---

## 5. Open questions / known unknowns

- **Was test run #4's terminal cancel actually a VPA-resolution failure, or is there a *third* payload invariant we're missing?** The merchant transitioned the order to `paid` then cancelled 82s later in a separate tx. That's consistent with an off-chain UPI delivery failure (NPCI lookup miss, bank reject, daily limit), and inconsistent with another contract-level rejection (those fire synchronously in our setUpi tx, as in runs #1 and #3). Validate by running the script against a *known-good* VPA — if it still cancels at the same +60-90s window, there's more work to do.

- **Is the SDK example wrong, or am I missing context?** `~/dev/p2pdotme-sdk/example/make-pay-order.ts` ships the same bare-VPA + 0 pattern that broke us. It's plausible that the SDK example was written against a permissive test merchant that didn't enforce the URI invariant, and the production Diamond does. Worth flagging upstream — file an issue on the p2pdotme-sdk repo asking them to either update the example or document the requirement in `@p2pdotme/sdk/orders`' `setSellOrderUpi` JSDoc.

- **Should `setSellOrderUpiCalldata`'s default-zero be removed entirely, or relaxed to `null` for SELL orders?** PAY needs non-zero. SELL might still accept zero (the function name predates PAY). Check the contract behaviour with a SELL order before tightening the Kotlin signature — if SELL still wants zero, keep the param optional but document the PAY constraint.

- **Approval strategy:** infinite vs. just-in-time. Web client does infinite. Mobile users may prefer just-in-time for the security signal. Either works for the contract.

---

## 6. Where to look (in this repo)

| concern | file |
|---|---|
| User-facing screen | `ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/swap/upi/{UpiOfframpVM, UpiOfframpView, UpiOfframpState}.kt` |
| Orchestrator | `offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/orchestrator/OfframpOrchestrator.kt` |
| Contract calldata | `offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/p2p/DiamondCalls.kt` |
| Request shape | `offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/orchestrator/OfframpRequest.kt` |
| Existing URI parser | `offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/p2p/UpiQrParser.kt` |
| Sell-price source for the INR ↔ USDC conversion | `ui-lib/.../GetUpiOfframpRateUseCase.kt` + `offramp-lib/.../PriceConfigDecoderTest.kt` for the decode shape |
| Tests to extend | `offramp-lib/src/jvmTest/kotlin/xyz/justzappit/offramp/orchestrator/OfframpOrchestratorTest.kt` |

Decompile + selector tables live alongside this file (`README.md` in the same directory). The Node rig at `~/dev/p2p-scripts/src/offramp.ts` has the working reference implementation if you want a side-by-side.

---

## 7. PR shape suggestion

- Title: `fix(offramp): build proper UPI URI + pass parsed updatedAmount to setSellOrderUpi`
- Body: TL;DR ≈ first paragraph of this doc. Link [setUpi tx 0xba0077…](https://basescan.org/tx/0xba00770c205c313994bf84ec86bdcde0794e90ca54d635baecafeccc19ee60a9) as the "after" evidence and one of the same-block cancel txs (e.g. `0x0e0728…`) as the "before".
- Don't bundle the approval-cadence change with the payload fix unless you go with infinite approval (which is a slightly larger conversation). If just-in-time is preferred, do that as a follow-up.
- Test plan checklist: unit test diff + a single mainnet `bun src/offramp.ts` run with a known-good VPA, screenshotted.

Good luck.
