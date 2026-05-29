# Zapp fork — deep review vs upstream zodl-android

**Date:** 2026-05-28 · **Branch reviewed:** `main` (HEAD `8f8906a4`) · **Fork point:** `2409c4d7` (upstream zodl v3.3.1)
**Method:** 13 domain reviewers → adversarial verification of every medium+ finding → cross-cutting synthesis (90 agents). Each finding was re-checked against the actual code and the golden-standard upstream checkout at `../zodl-android-real`.

**Tally:** 157 findings kept (5 refuted and dropped). **1 critical · 13 high · 40 medium · 87 low · 16 nit.** 70 high-confidence findings were confirmed by a second agent re-reading the code; low/nit (mostly evm-lib parsing smells) were not adversarially verified and are lower priority.

---

## ⚠️ Read this first — `main` is behind your fix branch

`main` is **0 commits ahead / 15 behind** `fix/signup-identity-derivation`. None of the identity-audit commits (`4dd8f099`, `45c44512`, `620990a5`, `ec16262a`, `8d879f6c`, `b567b012`, `894eef27`) are in `main`. So a cluster of findings below — the non-atomic identity creation, the messenger-only/arbitrary-seed restore path, `EvmKey` encapsulation, `ChatIdentitySetupView` using `MaterialTheme.typography`, the `*ViewModel` naming — describe code that **is already fixed on `fix/signup-identity-derivation` but not merged to `main`.** Those are tagged **[fixed-on-branch]**. Merging that branch resolves them; the rest need separate work and affect both branches.

---

## Overall verdict

The fork's **net-new money/crypto core (`evm-lib`, `offramp-lib`) is the strongest addition** — RFC-6979 deterministic + low-S signing, ABI calldata byte-for-byte parity-tested against viem fixtures, userOpHash pinned to a live EntryPoint v0.6, decimals centralized in a `Usdc6` value class, the allowance/fee atomic-cancel bug correctly handled, dev-key fails closed on mainnet. This layer was merged at the upstream bar.

The **UI / onboarding / build layers were merged at a markedly lower bar** than upstream, and that's where the risk concentrates:

1. **A release-config regression that ships secrets** (the one critical).
2. **A systemic i18n collapse** — the entire new surface ships English-only and some conversions *re-broke* already-translated upstream screens.
3. **A handful of genuinely user-harming behavioral bugs** on PIN/send/seed/moderation/contact paths.

---

## 🔴 CRITICAL

### build-config-1 — `gradle.properties` ships P2P keypair dumps to release builds
`gradle.properties:113` → `ZAPP_MESSAGING_LOG_LEVEL=debug`. CLAUDE.md states verbatim that `debug` "ships keypair dumps and verbose stream lifecycle into release `BuildConfig`." The merge-base did not have this line — it's a fork regression. Verified: the consumer (`../zappMessaging/android/build.gradle.kts:36-51`) writes it into `BuildConfig`.
**Fix (one line):** set it blank/`info` in the committed file; put `debug` in `local.properties`.

---

## 🟠 HIGH (13)

### Release / build hygiene
- **build-config-2** — `gradle.properties:71` `ZCASH_NETWORK=testnet` contradicts the file's own comment and **disables all mainnet variants** at config time (`app/build.gradle.kts:267-282` `beforeVariants` disables flavors != requested), breaking CI / side-by-side. Revert to blank; dev value → `local.properties`.
- **build-config-3** — `ui-lib/src/main/AndroidManifest.xml:5-6` adds `ACCESS_FINE/COARSE_LOCATION` with **no matching `<uses-feature required="false">`**. Per CLAUDE.md, Play then infers a hardware requirement and filters install-eligible devices. Add `android.hardware.location` + `.location.gps` features.

### PIN-lockout traps (same class, two most sensitive screens)
- **modified-upstream-views-1** — `ReviewTransactionVM.kt` (send confirmation): `onSendAuthDismissed` does **not** cancel the lockout ticker, so it flips state back to `PinLocked` within 1s and re-shows the full-screen PIN over the screen the user just dismissed — user is trapped until the lockout elapses. Cancel `sendLockoutTickerJob` on dismiss.
- **modified-upstream-views-2** — `WalletBackupViewModel.kt:71-83 / 267-269` (seed reveal): identical bug. Cancel `pinLockoutTickerJob` in `onPinEntryDismissed`. (Both VMs also write `state.value =` instead of `.update{}`, so ticker + handler can lose writes.)

### Chat-identity / onboarding architecture (wallet-is-single-root violated)
- **onboarding-tabs-1** — `welcome/view/ChatRestoreView.kt:45-160` restores a **chat-only identity from an arbitrary phrase with no wallet**, drops into the tabs shell at `SecretState.NONE`; a later "Create new wallet" yields a *different* seed, permanently breaking the "one phrase restores everything" promise. Its KDoc still documents the obsolete two-seed model. **[partially fixed-on-branch]**
- **identity-domain-1** — `usecase/CreateChatIdentityUseCase.kt:11-18` is **non-atomic**: `createIdentity()` then `exportSeedPhrase()` are two IPC calls; if the second throws, the caller gets `Failure` but an identity *exists that was never shown for backup* — silently un-backed-up. Make creation atomic at the SDK boundary or surface a distinct "created but backup unavailable" state. **[fixed-on-branch]**
- **identity-domain-6** *(medium, arch)* — `ChatBootstrap.kt:67-77` exposes both `restoreFromWalletSeed` and `restoreFromSeedPhrase`, keeping the removed arbitrary-seed entry point alive. **[fixed-on-branch]**

### Chat correctness / data integrity
- **chat-core-1** — `room/ChatRoomVM.kt:420-432,463` blocked-user filter on load compares `senderName` against a **public-key** blocklist → never matches, so a blocked peer's full history reappears on every room reload. The incoming-message path keys correctly on `senderId`; the two paths disagree. `ChatMessage` doesn't even carry `senderId`. Add it and filter on it.
- **chat-contacts-2** — `contacts/ChatContactsVM.kt:234-247` contact edit calls `saveContact` (append) instead of `updateContact` → **duplicate address-book rows on every edit**. Mirror `AddressBookVM.onUpdateContact`.
- **chat-contacts-3** *(medium)* — `contacts/EditChatContactVM.kt:41-43,195-207` can't pre-load existing per-chain (transparent/EVM/Solana) addresses → they're **silently wiped on save**.

### Offramp money-math
- **offramp-lib-1** — `orchestrator/OfframpCheckpoint.kt:63-70` **drops the 1% slippage floor** (`fiatAmountLimit`) on a funding/bridge-stage resume — `placeOrder` runs with zero slippage protection on exactly the highest-drift path. Persist `fiatAmountLimit` in the checkpoint, or recompute it in `toRequest()`.

### Secret leak
- **evm-lib-1** — `rpc/BundlerClient.kt:250-255` puts the Pimlico `?apikey=` **in the URL** and is wired with a logging `HttpClient` (`ProviderModule.kt:198-212`), so the gas-sponsorship key can leak to logcat / bug reports → budget drain. The class KDoc even warns "do not log the bundler URL verbatim." Move the key to a header or give the bundler a logging-off `HttpClient`.

### Privacy via duplication
- **dup-reuse-1** — `chat/profile/ChatProfileVM.kt:295-298`, `chat/settings/ChatSettingsVM.kt:202-203`, `bubbles/WalletAddressBubble.kt:130-132` hand-roll clipboard copy, **dropping `EXTRA_IS_SENSITIVE`** so wallet addresses & public keys land in clipboard history / keyboard suggestions — a privacy regression vs every other copy site. Route through `CopyToClipboardUseCase`.

### i18n (representative high spots; full collapse below)
- **strings-i18n-9** — `onboarding/view/WalletPhaseIntroView.kt` hardcodes every wallet-phase string.
- **strings-i18n-18** — `chat/view/EditChatContactSheet.kt` hardcodes the header, the whole delete-confirm dialog, CTAs, and 4 content-descriptions (12 literals).

---

## Cross-cutting themes (where the medium/low volume lives)

### 1. Systemic i18n collapse (53 i18n findings)
The entire **~248-string chat feature** plus new **offramp**, **unified_send**, **onboarding/PIN/security**, and **home balance-chart** surfaces ship **English-only — no `values-es` mirror at all**. Worse, the Zashi→ZappTheme conversion of **WalletBackup / Receive / RequestQrCode** *replaced already-translated upstream string lookups with English literals* — an active regression of Spanish. Dozens of view/VM/clipboard/content-description literals are hardcoded straight into source.
**Fix:** generate `values-es` mirrors for every new feature dir; sweep hardcoded text through `StringResource`; add a CI lint that fails when an EN key has no ES counterpart.

### 2. ZappTheme / fork-style drift (15 style findings)
`ChatIdentitySetupView` uses `MaterialTheme.typography` not `ZappTheme.typography` **[fixed-on-branch]**; `OnboardingSecurityViewModel` / `SecuritySettingsViewModel` were **not renamed to `*VM`** (SKILL.md lists these exact names as prior violations) **[fixed-on-branch]**; top-level composables left **public** instead of `internal` across onboarding/chat; new components added to the **legacy `ZappComponents.kt` aggregate** (`theme-design-1`, 578 lines) instead of one-file-per-component.

### 3. Duplication (13 findings)
`NetworkChip` + state duplicated byte-for-byte across chat list/room (`dup-reuse-2`); the chat-message JSON envelope parsed ad-hoc in **5 bubble files** (`dup-reuse-4`); hand-rolled clipboard (above); duplicated ellipsize/timestamp helpers; `ChatBootstrap` duplicate restore entry points. Several reinvent things deleted utils used to centralize (`ZecAmountExt`, `ClipboardManagerUtil`).

### 4. Incomplete deletions / dead code (22 findings)
- **deletions-nav-2** — `androidTest` still references deleted `send/`onboarding symbols → **the androidTest source set no longer compiles**.
- **deletions-nav-1** — removing the `SecretState.NONE` nav branch (`RootNavGraph.kt:86-95`) **strands the user on a dead screen after wallet reset** with no create/restore entry.
- **modified-upstream-views-3** — Request QR screen lost its "close flow" button; `onClose` is now dead code; back goes to the amount step instead of exiting.
- Plus dead state/no-op buttons (`AddressBookVM` scan/manual no-ops, `validateUnifiedAddress`), empty-catch error swallowing, stale KDoc referencing a non-existent wallet "skip" path.

### 5. Other confirmed money/UI bugs
- **swap-offramp-ui-1** — `GetBalanceHistoryUseCase.kt:23-53` running sum **counts failed/pending sends**, so the chart + footer balance diverge from the real wallet balance.
- **chat-core-10** — `ChatRoomVM.kt:580-587,719-725` clears the typed text **before** send succeeds; on failure the message is silently lost (no FAILED bubble / draft restore).
- **build-config-4** *(security)* — `buildSrc` dependency **lockfiles deleted while `dependencyLocking{lockAllConfigurations()}` stays enabled** — defeats the supply-chain guarantee while looking like it's still enforced.

---

## What's strong (don't touch)
- `evm-lib` signing/ABI/RLP/userOpHash — fixture- and on-chain-vetted; helpers correctly shared with `offramp-lib` (no duplication).
- `offramp-lib` decimal handling, allowance/fee math, relay-identity encrypted persistence, dev-key fails-closed, merchant-tamper / UPI-idempotency / cancellation test coverage.
- Chat-core structure: file-triad, `ZappTheme` colors, `NavigationRouter` + `StateFlow.update{}`, `runChatCall` used correctly in chat VMs.

---

## Recommended sequencing

**Quick wins (minutes, high impact):**
1. `build-config-1` — blank the debug log level *(secret leak — do first)*
2. `build-config-2` — blank `ZCASH_NETWORK`
3. `build-config-3` — add the two `<uses-feature>` lines
4. `evm-lib-1` — Pimlico key to header / logging off
5. `dup-reuse-1` — route chat clipboard through `CopyToClipboardUseCase` *(privacy)*

**Then (real fixes):**
6. PIN-lockout ticker cancel on both screens (`muv-1`, `muv-2`)
7. Blocked-user filter on public key (`chat-core-1`); contact-edit `updateContact` + seed addresses (`chat-contacts-2/-3`)
8. Offramp slippage floor on resume (`offramp-lib-1`)
9. **Decide on the branch:** merge / rebase `fix/signup-identity-derivation` to clear the `[fixed-on-branch]` cluster, then re-review the delta.

**Sweeps (own PRs):**
10. i18n: `values-es` mirrors + hardcoded-string sweep + CI mirror-check
11. Style pass with the `zodl-style` skill (rename `*VM`, `internal` composables, break up `ZappComponents.kt`)
12. Dead-code cleanup + fix androidTest compile (`deletions-nav-2`)

---

*Full machine-readable findings: `/tmp/zapp_review.json`. Appendix below: every finding.*

---

## Appendix — all 157 findings

| sev | id | cat | verdict | file:line | title |
|---|---|---|---|---|---|
| critical | build-config-1 | security | confirmed | gradle.properties:113 | ZAPP_MESSAGING_LOG_LEVEL=debug committed in gradle.properties ships keypair dumps to release builds |
| high | build-config-2 | bug | confirmed | gradle.properties:71 | ZCASH_NETWORK=testnet committed default disables mainnet variants and breaks CI/side-by-side |
| high | build-config-3 | bug | confirmed | ui-lib/src/main/AndroidManifest.xml:5-6 | ACCESS_FINE/COARSE_LOCATION permissions added without matching <uses-feature required=false> |
| high | chat-contacts-2 | bug | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/contacts/ChatContactsVM.kt:234-247 | Chat-contact edit persists via saveContact (append) instead of updateContact → duplicate AB rows |
| high | chat-core-1 | bug | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/room/ChatRoomVM.kt:420-432, 463 | Blocked-user filter on message load uses senderName against a public-key blocklist — never matches |
| high | dup-reuse-1 | duplication | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/profile/ChatProfileVM.kt:295-298 | Chat VMs/bubble hand-roll clipboard copy, bypassing CopyToClipboardUseCase (drops sensitive flag + hardcodes l |
| high | evm-lib-1 | security | confirmed | evm-lib/src/jvmMain/kotlin/xyz/justzappit/evm/rpc/BundlerClient.kt:250-255 | Pimlico API key leaks to logcat: bundler URL carries ?apikey= and is wired with a logging HttpClient |
| high | identity-domain-1 | bug | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/common/usecase/CreateChatIdentityUseCase.kt:11-18 | CreateChatIdentityUseCase is non-atomic: identity is created but a failed exportSeedPhrase reports failure, st |
| high | modified-upstream-views-1 | bug | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/reviewtransaction/ReviewTransactionVM.kt:startSendLockoutTicker ~ lines 80-95 and onSendAuthDismissed ~ lines around 365 | PIN lockout ticker is not cancelled on dismiss in send-funds path (re-shows PIN over a submitted/abandoned tra |
| high | modified-upstream-views-2 | bug | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/walletbackup/WalletBackupViewModel.kt:71-83 (startPinLockoutTicker) and 267-269 (onPinEntryDismissed) | PIN lockout ticker is not cancelled on dismiss in wallet-backup seed reveal |
| high | offramp-lib-1 | bug | confirmed | offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/orchestrator/OfframpCheckpoint.kt:63-70 | Slippage floor (fiatAmountLimit) is dropped on a funding/bridge-stage resume — placeOrder runs with no slippag |
| high | onboarding-tabs-1 | architecture | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/welcome/view/ChatRestoreView.kt:45-160 | ChatRestoreView restores a chat-only identity with no wallet, breaking the single-root architecture |
| high | strings-i18n-18 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/EditChatContactSheet.kt:81, 155, 223, 230, 250, 256, 286, 295, 311, 317, 334, 340 | EditChatContactSheet hardcodes header, delete-confirm dialog, CTAs, and content-descriptions |
| high | strings-i18n-9 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/view/WalletPhaseIntroView.kt:32-53, 89-110, 142-143 | WalletPhaseIntroView hardcodes all wallet-phase onboarding strings |
| medium | build-config-4 | security | confirmed | buildSrc/build.gradle.kts:8-21, 25-38 | buildSrc dependency lockfiles deleted while dependencyLocking{lockAllConfigurations()} stays enabled |
| medium | chat-contacts-3 | bug | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/contacts/EditChatContactVM.kt:41-43, 195-207 | EditChatContactVM cannot load existing per-chain addresses → silent data loss on save |
| medium | chat-contacts-4 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/addressbook/AddContactSheet.kt:175, 191, 258, 324, 334 | Hardcoded user-facing strings in addressbook Add/Edit sheets and WalletAddressField |
| medium | chat-contacts-6 | style-inconsistency | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/ChatIdentitySetupView.kt:71, 79, 96, 132, 269, 277, 324, 331 | ChatIdentitySetupView uses MaterialTheme.typography instead of ZappTheme.typography |
| medium | chat-core-10 | bug | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/room/ChatRoomVM.kt:580-587, 719-725 | Sent text is cleared before the send succeeds; on failure the message is silently lost |
| medium | chat-core-2 | i18n | confirmed | ui-lib/src/main/res/ui/chat/values/strings.xml: | Entire chat feature has no values-es translation mirror |
| medium | chat-core-3 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/NetworkStatus.kt:70-92, 148-236 | NetworkStatus.kt hardcodes ~25 user-facing English strings (ConnectionPill + NetworkDetailsSheet) |
| medium | chat-core-5 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/bubbles/LocationBubble.kt:62, 98 (Location); PaymentRequestBubble.kt:56; TransactionBubble.kt:55; WalletAddressBubble.kt:96,110,132; FileBubble.kt:31 | Message bubbles hardcode user-facing labels (Location/Payment/Transaction/WalletAddress/File) |
| medium | chat-core-7 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/AddChatContactSheet.kt:75, 106, 122-123, 187, 238, 244 (Add); EditChatContactSheet.kt:81,155,223,230,250,256,286,295,311,317,334,340 | AddChatContactSheet / EditChatContactSheet hardcode many strings (titles, buttons, placeholders, content-descr |
| medium | chat-core-8 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/AttachmentSheet.kt:54, 63, 72, 97-106 | AttachmentSheet hardcodes attachment-option labels |
| medium | deletions-nav-1 | bug | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/RootNavGraph.kt:86-95 | Wallet reset strands the user: SecretState.NONE navigation branch was removed with no replacement |
| medium | deletions-nav-2 | dead-code | confirmed | ui-lib/src/androidTest/java/co/electriccoin/zcash/ui/screen/send/integration/SendViewIntegrationTest.kt:8-14 | Instrumented-test (androidTest) source set references deleted send/onboarding symbols and will not compile |
| medium | deletions-nav-3 | dead-code | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/WalletNavGraph.kt:211-213 | Legacy HomeArgs/AndroidHome route is registered but unreachable; justifying comment is factually wrong |
| medium | dup-reuse-2 | duplication | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/ChatRoomNetworkChip.kt:11-26 | NetworkChip composable and its state class duplicated byte-for-byte across chat list and room |
| medium | dup-reuse-4 | duplication | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/bubbles/TransactionBubble.kt:31-38 | Chat-message JSON content envelope parsed ad-hoc in five bubble files |
| medium | identity-domain-6 | architecture | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/common/ChatBootstrap.kt:67-77 | ChatBootstrap exposes both restoreFromWalletSeed and restoreFromSeedPhrase, duplicating SDK-restore entry poin |
| medium | modified-upstream-views-3 | bug | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/request/view/RequestView.kt:167-176 (QrCode branch of RequestBottomBar) | Request QR screen lost its 'Close flow' button; onClose callback is now dead code |
| medium | modified-upstream-views-6 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/walletbackup/WalletBackupView.kt:198, 215, 227, 387 | WalletBackup screen re-hardcodes English strings that already exist in strings.xml + values-es, breaking Spani |
| medium | modified-upstream-views-7 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/receive/ReceiveView.kt:186 (TIP), 235 (Share), 271 (←), 323 (Shielded/Transparent) | Hardcoded user-facing strings introduced throughout the Receive screen rewrite |
| medium | modified-upstream-views-8 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/request/view/RequestQrCodeView.kt:156, 173, 219, 239 | Hardcoded strings in RequestQrCodeView (Save QR to Photos, address labels, NOTE, copy CD) |
| medium | onboarding-tabs-2 | style-inconsistency | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/OnboardingSecurityViewModel.kt:26 | OnboardingSecurityViewModel and SecuritySettingsViewModel were NOT renamed to the *VM convention |
| medium | onboarding-tabs-3 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/welcome/view/WelcomeGateView.kt:79,90,104,116,138,159,168,178 | Pervasive hardcoded user-facing strings across the entire onboarding/welcome flow |
| medium | onboarding-tabs-4 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/securitysettings/SecuritySettingsViewModel.kt:190-192,216,227 | Error/PIN/UI strings hardcoded inside the security VMs |
| medium | strings-i18n-10 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/view/PinSetupScreen.kt:99-122 | PinSetupScreen hardcodes PIN-creation titles, error, and subtitles |
| medium | strings-i18n-11 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/view/PinVerifyScreen.kt:89-117 | PinVerifyScreen hardcodes PIN-entry title, lockout, and error strings |
| medium | strings-i18n-12 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/view/BioScanScreen.kt:78-114 | BioScanScreen hardcodes biometric-enrollment strings |
| medium | strings-i18n-14 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/view/TwoFAChoiceScreen.kt:48-67 | TwoFAChoiceScreen hardcodes security-choice labels and subtitles |
| medium | strings-i18n-17 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/AddChatContactSheet.kt:75, 106, 122, 186, 238, 244 | AddChatContactSheet hardcodes title, placeholder, CTA, and content-descriptions |
| medium | strings-i18n-19 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/AttachmentSheet.kt:54, 63, 72 | AttachmentSheet hardcodes all attachment-action labels |
| medium | strings-i18n-20 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/NetworkStatus.kt:149-236 | NetworkStatus sheet hardcodes ~17 diagnostic labels and status values |
| medium | strings-i18n-21 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/bubbles/WalletAddressBubble.kt:110, 131, 132 | WalletAddressBubble hardcodes QR content-description, clipboard label, and copied toast |
| medium | strings-i18n-23 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/bubbles/PaymentRequestBubble.kt:56 | PaymentRequestBubble hardcodes payment-request status labels |
| medium | strings-i18n-5 | i18n | confirmed | ui-lib/src/main/res/ui/onboarding/values/strings.xml: | Fork added onboarding EN keys without mirroring to existing values-es |
| medium | strings-i18n-7 | i18n | confirmed | ui-lib/src/main/res/ui/settings/values/strings.xml: | Fork added settings/p2p EN keys without mirroring to existing values-es |
| medium | strings-i18n-8 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/view/MessagingIdentityView.kt:49-114, 380, 418, 429 | MessagingIdentityView hardcodes all onboarding identity/username/seed strings |
| medium | swap-offramp-ui-1 | bug | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/common/usecase/GetBalanceHistoryUseCase.kt:23-53 | Balance-history running sum counts failed and pending sends, corrupting the chart |
| medium | swap-offramp-ui-3 | i18n | confirmed | ui-lib/src/main/res/ui/swap/values-es/strings.xml: | Seven fork-added swap strings are missing from values-es |
| medium | swap-offramp-ui-4 | i18n | confirmed | ui-lib/src/main/res/ui/home/values-es/strings.xml: | Entire balance-chart string set is missing from home values-es |
| medium | theme-design-1 | architecture | confirmed | ui-design-lib/src/main/java/co/electriccoin/zcash/ui/design/component/zapp/ZappComponents.kt:1-578 | ZappComponents.kt aggregate keeps growing — violates one-component-per-file rule |
| medium | theme-design-2 | i18n | confirmed | ui-design-lib/src/main/java/co/electriccoin/zcash/ui/design/component/zapp/ZappComponents.kt:456-472 | Hardcoded English content-description "Go back" in ZappBackButton |
| low | build-config-5 | style-inconsistency | unverified | settings.gradle.kts:298-300 | coil and play-services-location declared with inline hardcoded versions, breaking the gradle.properties versio |
| low | build-config-6 | architecture | unverified | gradle.properties:191 | ANDROID_TARGET_SDK_VERSION downgraded to 35 while compileSdk stays 36 and upstream targets 36 |
| low | build-config-7 | architecture | unverified | settings.gradle.kts:3-14 | local.properties is loaded into project extra for the whole build, broadly overriding any gradle.properties ke |
| low | chat-contacts-1 | i18n | confirmed | ui-lib/src/main/res/ui/chat/values/strings.xml: | Entire chat feature has no values-es mirror (systemic i18n gap) |
| low | chat-contacts-11 | architecture | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/settings/ChatSettingsVM.kt:47-54 | ChatSettingsVM reads sdk.identity directly while ChatProfileVM uses ObserveChatIdentityUseCase |
| low | chat-contacts-12 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/addressbook/AddContactSheet.kt:90-112 | AddContactSheet primary-vs-field scan routing relies on two concurrent LaunchedEffects keyed on scannedAddress |
| low | chat-contacts-14 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/repository/ChatModerationRepositoryImpl.kt:34-47, 66 | ChatModerationRepository mutates StateFlow via .value read-modify-write (non-atomic) |
| low | chat-contacts-5 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/profile/ChatProfileVM.kt:265, 277 | Hardcoded clipboard labels in ChatProfileVM and ChatSettingsVM |
| low | chat-contacts-7 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/addressbook/AddressBookVM.kt:90-99 | AddressBookVM scanButton/manualButton are dead no-op state |
| low | chat-contacts-8 | bug | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/addressbook/AddressBookVM.kt:170-178 | AddressBookVM.onSaveNewContact swallows all exceptions (incl. CancellationException) with empty catch |
| low | chat-contacts-9 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/addressbook/WalletAddressField.kt:137-141 | Unused validateUnifiedAddress validator (dead code) |
| low | chat-core-11 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/room/ChatRoomVM.kt:459-466, 722-724 | Incoming messages are appended without dedup-by-id or timestamp ordering |
| low | chat-core-12 | duplication | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/ChatListNetworkChip.kt:11-26 (and ChatRoomNetworkChip.kt:11-26) | Two identical NetworkChip overloads duplicated across files |
| low | chat-core-13 | duplication | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/ChatMessageBubble.kt:251-257 | Content-type protocol constants duplicated and diverge from MimeTypes |
| low | chat-core-14 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/bubbles/MediaBubble.kt:156-158 | MediaBubble allocates a new ImageLoader on every recomposition for non-GIF media |
| low | chat-core-15 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/bubbles/PaymentRequestBubble.kt:37, 63 (and TransactionBubble.kt:37,62) | Numeric amounts in Payment/Transaction bubbles use raw Double interpolation, not locale-aware formatting |
| low | chat-core-16 | style-inconsistency | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/room/ChatRoomEffectsHandler.kt:92-126 | shareLocation uses raw try/catch(Exception) + @Suppress("TooGenericExceptionCaught") — the anti-pattern runCha |
| low | chat-core-17 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/common/ChatBootstrap.kt:53-61 | ChatBootstrap.observeMessagesForUnread filters senderId but never excludes the active conversation |
| low | chat-core-18 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/bubbles/WalletAddressBubble.kt:110, 131-132 | WalletAddressBubble copy-to-clipboard uses non-localized clipboard label and toast |
| low | chat-core-19 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/ChatMessageBubble.kt:121-135 | MessageBubble swallows reply swipe behind unwired replyTo fields (dead UI affordance) |
| low | chat-core-4 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/NetworkStatus.kt:41-119 | ConnectionPill is dead code |
| low | chat-core-6 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/model/BlockedUser.kt:15-24 | ReportCategory display labels are hardcoded English in the model layer |
| low | chat-core-9 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/ChatContactsView.kt:247 | ChatContactsView hardcodes 'Start chat' content-description |
| low | deletions-nav-4 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/RootNavGraph.kt:30 | Unused walletViewModel parameter left in RootNavGraph after onboardingNavGraph removal |
| low | deletions-nav-5 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/common/usecase/NavigateToNearPayUseCase.kt:10-32 | Dead commented-out PayArgs/ephemeral-lock block left in NavigateToNearPayUseCase |
| low | deletions-nav-6 | architecture | unverified | crash-android-lib/build.gradle.kts:65-67 | Firebase removal is incomplete at the build layer: Crashlytics gradle plugins/classpath remain while the crash |
| low | dup-reuse-3 | dead-code | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/NetworkStatus.kt:42-119 | Dead ConnectionPill composable plus connection-status→color/label mapping duplicated in four places |
| low | dup-reuse-5 | duplication | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/bubbles/TransactionBubble.kt:76 | Bubble timestamp formatting copy-pasted across six chat files instead of a shared helper |
| low | dup-reuse-6 | duplication | unverified | ui-design-lib/src/main/java/co/electriccoin/zcash/ui/design/component/zapp/ZappComponents.kt:62-63 | Three parallel address/key ellipsize helpers in fork code (two are functionally identical) |
| low | dup-reuse-8 | duplication | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/common/ChatBootstrap.kt:67-73 | Wallet-seed-to-string done two ways with inconsistent zeroization posture |
| low | evm-lib-3 | dead-code | unverified | evm-lib/src/jvmMain/kotlin/xyz/justzappit/evm/rpc/BaseRpcClient.kt:124-129 | ethGetTransactionReceipt has dead/incorrect JSON-null handling |
| low | evm-lib-4 | bug | unverified | evm-lib/src/jvmMain/kotlin/xyz/justzappit/evm/abi/SolidityErrors.kt:17-19 | SolidityErrors.decodeErrorString length fold truncates to 32 bits (Int wrap) |
| low | evm-lib-5 | bug | unverified | evm-lib/src/jvmMain/kotlin/xyz/justzappit/evm/util/Hex.kt:10-13 | hexToBigInteger does not strip an uppercase 0X prefix |
| low | evm-lib-6 | bug | unverified | evm-lib/src/jvmMain/kotlin/xyz/justzappit/evm/rpc/BaseRpcClient.kt:51-52 | ethChainId silently narrows BigInteger to Long |
| low | identity-domain-10 | architecture | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/common/usecase/NavigateToScanPublicKeyUseCase.kt:28-31 | Scan use cases call navigationRouter.back() in onScanned, diverging from upstream NavigateToScanGenericAddress |
| low | identity-domain-11 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/common/provider/IsExchangeRateEnabledStorageProvider.kt:19, 31 | IsExchangeRateEnabledStorageProvider now defaults unset to true (network-emitting feature on by default) |
| low | identity-domain-2 | dead-code | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/di/ProviderModule.kt:191-197 | CachingOfframpAccountProvider is never wired into DI; StaticOfframpAccountProvider re-runs PBKDF2 + crosses th |
| low | identity-domain-3 | duplication | uncertain | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/identity/ChatIdentitySetupVM.kt:152-156, 161-179 | Chat username validation drifts between create (blank-only) and restore (24-word + blank-only) paths; no share |
| low | identity-domain-4 | security | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/common/viewmodel/WalletViewModel.kt:34-43 | WalletViewModel.currentSeedWords keeps the 24 raw seed words in a long-lived StateFlow |
| low | identity-domain-5 | security | confirmed | evm-lib/src/jvmMain/kotlin/xyz/justzappit/evm/hd/EvmKeyDerivation.kt:16-34 | EvmKey has no secret encapsulation and mixes the private scalar into hashCode (evm-lib, fed by in-scope Wallet |
| low | identity-domain-7 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/di/UseCaseModule.kt:322-324 | Dead/unreachable chat-identity use cases and DI bindings retained on main (CreateChatIdentityUseCase, RestoreC |
| low | identity-domain-8 | security | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/common/security/PinAuthGate.kt:46-51, 65-82 | PIN lockout uses user-manipulable wall-clock time while the auth-timeout was deliberately moved to monotonic e |
| low | modified-upstream-views-10 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/transactionprogress/TransactionProgressView.kt:TitleBlock eyebrowText when-expression (the SUCCESS/ERROR -> "RESULT", PENDING -> "STATUS" block) | Hardcoded eyebrow labels 'RESULT'/'STATUS' in TransactionProgressView |
| low | modified-upstream-views-12 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/request/view/RequestView.kt:203-223 (SwissBackBox); also ReceiveView ReceiveBottomDock back box ~262-279; TransactionProgressView TopBar close ~Icon contentDescription = null | Accessibility regression: back/close controls lost content descriptions during conversion |
| low | modified-upstream-views-13 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/request/viewmodel/RequestVM.kt:173-183 (onNextClick) with RequestAmountView note field always shown | Transparent addresses can now embed a ZIP-321 memo via the inline note field |
| low | modified-upstream-views-14 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/request/viewmodel/RequestVM.kt:63-64 (decimal getter) | Request amount decimal-separator regex switched from monetaryDecimalSeparator to decimalSeparator |
| low | modified-upstream-views-15 | style-inconsistency | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/walletbackup/WalletBackupViewModel.kt:77, 81, 220, 249, 250, 254, 256, 268 | Non-atomic state writes (state.value =) instead of update {} in PIN flows |
| low | modified-upstream-views-16 | duplication | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/reviewtransaction/ReviewTransactionVM.kt:305, 324 (and WalletBackupViewModel.kt 200, 219) | Magic-string auth method comparisons duplicated instead of using shared constants |
| low | modified-upstream-views-17 | style-inconsistency | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/about/util/WebBrowserUtil.kt:openBrandedUrl toolbarColor line | Hardcoded toolbar color '#FF9417' in WebBrowserUtil instead of a theme reference |
| low | modified-upstream-views-19 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/receive/ReceiveView.kt:70-74 | ReceiveView returns nothing for the empty-non-loading state (potential blank screen) |
| low | modified-upstream-views-4 | dead-code | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/request/viewmodel/RequestVM.kt:173-183 (onNextClick), 331-355 (onAmountDone), 357-375 (onMemoDone) | Request Memo stage is now unreachable; onAmountDone/onMemoDone and RequestState.Memo branch are dead code |
| low | modified-upstream-views-5 | bug | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/request/view/RequestQrCodeView.kt:208-224 | 'Copy address' button in RequestQrCodeView does nothing (empty clickable shipped with admitting comment) |
| low | modified-upstream-views-9 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/request/view/RequestAmountView.kt:353 | Hardcoded strings in RequestAmountView note field |
| low | offramp-lib-2 | dead-code | unverified | offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/account/OfframpAccountProvider.kt:31-43 | CachingOfframpAccountProvider is implemented and unit-tested but never wired in production DI; its stated inva |
| low | offramp-lib-3 | bug | unverified | offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/orchestrator/OfframpOrchestrator.kt:253-280 | bridgeFundsBackToZec recovery flow is not resumable/idempotent for the NEAR pull-back direction |
| low | offramp-lib-4 | bug | unverified | offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/orchestrator/OfframpOrchestrator.kt:574-593 | validateCircleOnChain queries getAssignableMerchantsFromCircle with fiatAmount = ZERO |
| low | offramp-lib-5 | bug | unverified | offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/orchestrator/OfframpOrchestrator.kt:425-445 | encryptUpiEnvelopeForMerchant signs over a raw keccak256(uri), not an EIP-191 prefixed message, with no parity |
| low | onboarding-tabs-10 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/ZappOnboardingFlow.kt:176-188 | ZappOnboardingFlow SECURE_CHOICE back button jumps to WALLET_INTRO, skipping the seed/choice steps |
| low | onboarding-tabs-11 | i18n | unverified | ui-lib/src/main/res/ui/onboarding/values/strings.xml: | New strings present in values/ but not mirrored to values-es/ |
| low | onboarding-tabs-12 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/ZappOnboardingFlow.kt:166,228-269 | SeedLoadingPlaceholder dead error branch — sdkError is always null at the only call site |
| low | onboarding-tabs-5 | style-inconsistency | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/ZappOnboardingFlow.kt:76 | Top-level onboarding/welcome/tabs composables are public instead of internal |
| low | onboarding-tabs-6 | style-inconsistency | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/securitysettings/SecuritySettingsViewModel.kt:78,110,127,131,136,160,164,183,211,220,259 | SecuritySettingsViewModel uses state.value = ... assignment instead of update {} |
| low | onboarding-tabs-7 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/tabs/view/FloatingPillNavBar.kt:44-51,107 | FloatingPillNavBar tab content-descriptions use hardcoded English ZappTab.title |
| low | onboarding-tabs-8 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/tabs/view/SettingsTabContent.kt:90,104-157,213 | SettingsTabContent hardcodes section/row labels and uses commented-out dead code blocks |
| low | onboarding-tabs-9 | doc | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/ZappOnboardingFlow.kt:65 | Stale KDoc/comments describing a non-existent wallet "skip" path and obsolete optional-wallet model |
| low | strings-i18n-1 | i18n | confirmed | ui-lib/src/main/res/ui/chat/values/strings.xml: | Fork-new chat string feature dir has no values-es mirror (248 strings) |
| low | strings-i18n-13 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/view/OnboardingDoneScreen.kt:58-81 | OnboardingDoneScreen hardcodes completion strings with concatenation |
| low | strings-i18n-15 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/view/OnbComponents.kt:158 | OnbComponents hardcodes the 'back' button label |
| low | strings-i18n-16 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/onboarding/ZappOnboardingFlow.kt:241, 262 | ZappOnboardingFlow hardcodes seed-load timeout/error strings |
| low | strings-i18n-2 | i18n | confirmed | ui-lib/src/main/res/ui/offramp/values/strings.xml: | Fork-new offramp string dir has no values-es mirror |
| low | strings-i18n-22 | i18n | confirmed | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/bubbles/TransactionBubble.kt:55 | TransactionBubble hardcodes sent/received status labels |
| low | strings-i18n-24 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/bubbles/LocationBubble.kt:98 | LocationBubble hardcodes 'Open in Maps' label |
| low | strings-i18n-25 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/ChatContactsView.kt:247 | ChatContactsView hardcodes 'Start chat' content-description |
| low | strings-i18n-26 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/addressbook/WalletAddressField.kt:64, 212 | WalletAddressField hardcodes 'Additional addresses' and 'Scan $label QR' content-descriptions |
| low | strings-i18n-27 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/profile/ChatProfileVM.kt:265, 277 | ChatProfileVM hardcodes clipboard labels for public key and wallet address |
| low | strings-i18n-28 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/settings/ChatSettingsVM.kt:203 | ChatSettingsVM hardcodes 'Public Key' clipboard label |
| low | strings-i18n-4 | i18n | confirmed | ui-lib/src/main/res/ui/unified_send/values/strings.xml: | Fork-new unified_send string dir has no values-es mirror |
| low | strings-i18n-6 | i18n | confirmed | ui-lib/src/main/res/ui/swap/values/strings.xml: | Fork added swap EN keys without mirroring to existing values-es |
| low | swap-offramp-ui-5 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/swap/upi/progress/UpiOfframpProgressVM.kt:79-149 | UpiOfframpProgressVM accumulates an unbounded status list that no consumer reads beyond .last() |
| low | swap-offramp-ui-6 | bug | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/swap/upi/UpiOfframpVM.kt:182-214 | Unguarded division by the live rate can crash the rate poller if sellPrice is ever zero |
| low | swap-offramp-ui-7 | duplication | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/swap/ExactInputVMMapper.kt:78-86 | headerBalanceFiat reinvents the in-file getTotalSpendableFiatBalance() helper |
| low | swap-offramp-ui-8 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/swap/SwapView.kt:95-127 | SwapView keeps a now-unused required appBarState parameter |
| low | theme-design-3 | duplication | unverified | ui-design-lib/src/main/java/co/electriccoin/zcash/ui/design/component/zapp/ZappComponents.kt:62-63 | Duplicated head/tail ellipsize helpers: ellipsizeAddress vs ellipsizeMiddle |
| low | theme-design-4 | dead-code | unverified | ui-design-lib/build.gradle.kts:46 | Unused sdkExtLib dependency added to ui-design-lib/build.gradle.kts |
| low | theme-design-5 | i18n | unverified | ui-design-lib/src/main/java/co/electriccoin/zcash/ui/design/util/StringResource.kt:264-277 | Balance/Zatoshi formatting uses plain DecimalFormatSymbols, not upstream ZcashDecimalFormatSymbols (monetary s |
| low | theme-design-6 | bug | unverified | ui-design-lib/src/main/java/co/electriccoin/zcash/ui/design/theme/ZappTheme.kt:21-34 | ProvideZappTheme decides dark/light independently of ZcashTheme's forceDarkMode |
| low | theme-design-9 | dead-code | unverified | design/Zapp-designs/: | 1.9MB React/HTML design-prototype dump committed into the Android repo under design/Zapp-designs |
| nit | chat-contacts-13 | style-inconsistency | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/contacts/AddChatContactVM.kt:20-32 | Refactor-narrative KDoc on AddChatContactVM/EditChatContactVM (comment-noise) |
| nit | deletions-nav-7 | doc | unverified | crash-android-lib/src/zcashtestnetStoreDebug/kotlin/co/electriccoin/zcash/crash/android/internal/ListCrashReportersImpl.kt:8-12 | Stale FirebaseCrashReporter race-condition comment remains in Debug ListCrashReportersImpl after Firebase dele |
| nit | dup-reuse-7 | duplication | unverified | offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/p2p/SubgraphOrderParser.kt:65-82 | SubgraphOrderParser re-implements hex-digit validation that Address.parseOrNull already performs |
| nit | evm-lib-7 | architecture | unverified | evm-lib/src/jvmMain/kotlin/xyz/justzappit/evm/types/Address.kt:23 | Address.bytes recomputes hexToBytes on every access (per-RLP-field hot path) |
| nit | identity-domain-9 | style-inconsistency | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/common/viewmodel/AuthenticationViewModel.kt:217-225 | persistGoToBackgroundTime keeps a dead millis parameter guarded by @Suppress("UNUSED_PARAMETER") |
| nit | modified-upstream-views-18 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/advancedsettings/AdvancedSettingsVM.kt:resync ListItemState comment block in buildState + // private fun onResyncWalletClick() | Commented-out dead code left in AdvancedSettingsVM |
| nit | modified-upstream-views-20 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/receive/ReceiveView.kt:39 | Unused semantics.contentDescription import in ReceiveView |
| nit | modified-upstream-views-21 | i18n | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/authentication/view/WelcomeAnimation.kt:159 (Hi.), plus comments at 158, 169, 184 | Hardcoded decorative 'Hi.' greeting and refactor-narrative comments in WelcomeAnimation |
| nit | offramp-lib-6 | style-inconsistency | unverified | offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/orchestrator/OfframpOrchestrator.kt:364, 427, 428, 447, 474 | Inline fully-qualified java.math.* references despite an existing import |
| nit | offramp-lib-7 | bug | unverified | offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/p2p/OrderSnapshot.kt:34-36 | OrderSnapshot.isAccepted returns true for COMPLETED/PAID, relying on caller ordering |
| nit | swap-offramp-ui-10 | style-inconsistency | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/swap/upi/UpiOfframpVM.kt:145,59,72-73 | Findings-doc section references and refactor-narrative comments violate the minimal-comment rule |
| nit | swap-offramp-ui-11 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/home/balancechart/BalanceChartWidget.kt:165,209 | No-op RoundedCornerShape(0.dp) clips in balance-chart widget |
| nit | swap-offramp-ui-12 | style-inconsistency | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/swap/SwapView.kt:136-256 | SwapView uses scattered inline dp literals while sibling fork views hoist named constants |
| nit | swap-offramp-ui-9 | dead-code | unverified | ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/swap/upi/progress/UpiOfframpProgressView.kt:11,33,130-131 | Unused imports and dead locals in UpiOfframpProgressView |
| nit | theme-design-7 | style-inconsistency | unverified | ui-design-lib/src/main/java/co/electriccoin/zcash/ui/design/component/zapp/ZappStackedActionBar.kt:33-41 | ZappStackedActionBar and ZappBottomActionBar disagree on surface/border tokens for the same bottomBar slot |
| nit | theme-design-8 | style-inconsistency | unverified | ui-design-lib/src/main/java/co/electriccoin/zcash/ui/design/component/zapp/ZappComponents.kt:367-373 | ZappToggle thumb uses hardcoded Color.White instead of a theme token |
