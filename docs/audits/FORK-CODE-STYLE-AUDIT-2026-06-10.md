# Fork-code style & quality audit (2026-06-10)

Multi-agent audit (32 agents: 9 area reviewers + adversarial verification of every MAJOR/BLOCKER
finding + coverage critic) of **everything the fork adds on top of upstream**: 343 fork-only Kotlin
files, the ~6.7k fork-added lines inside shared upstream files, resources, manifests, and module
infra. The question: does the fork's new code (messaging, P2P, offramp, theming) follow upstream's
coding style and the fork's own binding conventions (`.claude/skills/zodl-style/SKILL.md`)?

Fork main @ `8f239f6a4`; upstream reference `origin/main` @ `fba96432a`. Upstream-mirrored files
(RestoreBD*, restoresuccess, connectkeystone) were verified as mirrors and exempted from
fork-authored rules — their upstream-style internals are correct by definition.

## Verdict

**The fork's code splits cleanly into two generations.** The newer code — the chat architecture,
`offramp-lib`, `evm-lib`, the p2p-transactions settings sextet, balancechart, the common layer —
is strongly conformant and in places *exceeds* upstream's bar (crypto hygiene, test depth, secrets
handling). The older "Swiss design" generation — onboarding/PIN/welcome screens, the tabs wallet
home, a handful of chat sheets/bubbles — concentrates nearly all violations, and they are exactly
the categories the style skill predicted: hardcoded strings, missing values-es mirrors, public
composables, `*ViewModel` naming.

**1 BLOCKER, ~12 verified MAJOR themes, no architectural rot.** Nothing requires redesign; almost
everything is mechanical cleanup.

### Area scorecard

| Area | Files | Conformance | Verified MAJOR+ |
|---|---|---|---|
| chat-ui (a+b) | 107 | GOOD / MIXED | hardcoded strings (~61 in 12 files), 2 public Support screens, 3 silent catches in media utils |
| swap/offramp/unifiedsend UI | 27 | GOOD | UnifiedSend quad-violation, es-mirror gaps, `OfframpView` public |
| offramp-lib | 57 | **EXCELLENT** | none |
| evm-lib | 41 | GOOD | `hexToBytes` echoes private-key hex into exception messages |
| onboarding/welcome/security/settings/restore | 39 | MIXED | **BLOCKER: seed screens lack FLAG_SECURE**, ~65 hardcoded strings, 25 public composables, 2 `*ViewModel` |
| wallet-ui (tabs/home/addressbook) | 24 | MIXED | ~30 hardcoded strings, 16 es-mirror gaps, 5 public composables, nav-in-composable |
| design-lib (Zapp components) | 11 | GOOD | 1 hardcoded a11y string, app-wide |
| common layer (di/usecases/providers) | 36 | GOOD | EncryptedJsonStore secrets-in-logs edge, 4 `*ViewModel` bindings |

## BLOCKER

**Seed screens without `SecureScreen()`/FLAG_SECURE** — `SeedRevealScreen` (displays the full
24-word phrase) and `RestoreSeedEntryScreen` (collects it) never set FLAG_SECURE, so seed material
is capturable via screenshots/recents/screen-share. The convention is applied by the sibling PIN
screens (`PinSetupScreen.kt:42`, `PinVerifyScreen.kt:52`, `SecuritySettingsView.kt:398`) and every
upstream seed-touching screen. This overlaps the known Play-release blocker ("seed FLAG_SECURE",
AUDIT-2026-05-31) — same theme, these two onboarding screens are the concrete remaining instances.
*Fix: one `SecureScreen()` call per screen.*

## Verified MAJOR themes

1. **Hardcoded user-facing strings (~150 across the old generation + shared-file rewrites).**
   - Onboarding/PIN/welcome: ~65 literals in 11 files ("PINs don't match…", "Enter\nyour PIN",
     "Enable Biometrics", "Tap to reveal", WelcomeGate's entire copy…) — the skill's documented
     historical hotspot, still unfixed.
   - Chat sheets/bubbles/diagnostics: ~55 in 10 files (`AddChatContactSheet`, `EditChatContactSheet`,
     `AttachmentSheet`, NetworkStatus sheet incl. hand-rolled plurals, 6 bubbles) — while the rest
     of chat ships a perfect 239/239-key EN↔ES mirror.
   - Tabs/home: ~30 in 8 files — including hardcoding "Settings" where `R.string.settings_title`
     already exists, and chart period labels duplicating existing `home_balance_chart_period_*` keys.
   - `ReportCategory` enum: 6 English `displayLabel`s rendered in the UGC report dialog (compliance
     surface; `SupportCategory` in the same codebase shows the correct resourced pattern).
   - Design-lib: `ZappBackButton` hardcodes `contentDescription = "Go back"` — every Spanish
     TalkBack user hears English on every back button app-wide.
   - Shared-file rewrites (critic): `ReceiveView.kt:186` `BasicText("TIP")`, `:235 "Share"`,
     `:271 "←"` (no contentDescription); `RequestQrCodeView.kt:158 "Save QR to Photos"`, `:242 "NOTE"`;
     `RequestView.kt:215 "←"` — while securitysettings externalized the same glyph correctly.
2. **values-es mirror gaps: ~63 fork-added keys.** Three fork string files have **no values-es at
   all** (`offramp/` 9 keys, `peer_onramp/` 7, `unified_send/` 10); plus untranslated fork keys in
   existing mirrors: home 16 (`home_sync_*`, `home_balance_chart_*`), settings 10
   (`p2p_transactions_detail_*`, `more_cash_out`), swap 7, insufficient_funds 2, others 3. Chat's
   317-line es mirror is genuine Spanish — the discipline exists, just not uniformly.
3. **Public top-level composables (~33).** 25 in onboarding (all of `OnbComponents.kt`, every
   screen), 5 in tabs, 2 Support chat screens, `OfframpView`. All verified unreferenced outside
   their packages — `internal` is a drop-in fix. (One claimed exemplar, `SendAddressBookHint`, was
   adversarially refuted; upstream's sheet views are public idiom.)
4. **`*ViewModel` naming regression.** The four classes the skill says "had to be renamed" still
   exist and are still bound in `ViewModelModule`: `OnboardingSecurityViewModel`,
   `SecuritySettingsViewModel`, `UnifiedSendViewModel` (fork-authored) + `RestoreSuccessViewModel`
   (upstream-legacy, dead — delete with its package). Every other fork VM conforms.
5. **Secrets-in-logs edge paths (2, both fork-owned seams):**
   - `EncryptedJsonStore.decode` wraps `SerializationException.message` — kotlinx embeds a
     `JSON input:` snippet of the **decrypted** blob — into `StoreCorruptedException`, which callers
     `Twig.warn`. The store backs the relay ECDH private key, offramp checkpoints, and orderId→VPA
     maps; a decryptable-but-undeserializable blob leaks plaintext into logcat. Fix at the seam:
     stop interpolating `e.message`, log cause class + key name only.
   - `evm-lib` `String.hexToBytes()` embeds the full input (`'$this'`) in both `require()` messages
     and is called on private-key hex (`Ecies.kt:76`, `OfframpOrchestrator.kt:427`). Truncate or
     omit the value.
6. **Silent broad catches in chat media** — `FileUtils.copyUriToCache`, `ImageProcessor.compressImage`,
   `generateThumbnail`: bare `catch (e: Exception) { null }`, no Twig, no suppress; callers then
   `error(...)` with a generic message, destroying the diagnostic.
7. **UnifiedSend quad-violation** — `*ViewModel` name, `model/`+`view/` subpackage split against the
   file-triad rule, per-function `TooGenericExceptionCaught` suppress with a silently swallowed
   catch, public helper composable. Worth one dedicated cleanup pass.
8. **Navigation from composables in tabs** — `NavigationRouter` threaded into the view tree and
   `.forward()` called from onClick lambdas (`SettingsTabContent.kt:111-167`,
   `WalletTabContent.kt:88`); the rule is VM-owned routing, composables fire callbacks.

## Out-of-scope finds (coverage critic)

- **Tracked junk (MAJOR):** `.claude-flow/` (9 files of AI-agent session state, belongs in
  .gitignore); `.git-side-work/FORK-REVIEW-2026-05-28.md` — a 289-line internal security review
  committed to the repo (information-disclosure risk if public: it documents the old debug-log
  keypair leak); `ui-lib/src/main/res/ui/about/values-es/.!96846!strings.xml` — a corrupted 213-byte
  macOS extraction artifact inside the resource tree (AAPT ignores dotfiles, but it's tracked garbage).
- `design/Zapp-designs/` (1.9 MB React prototypes + raw screenshots) and `zkp2p-providers/`
  (staging data consumed by nothing in the build) — keep deliberately or relocate; currently
  unclassified.
- `network_security_config.xml:5` claims "Pins certificates for critical API endpoints" but contains
  no `<pin-set>` — misleading comment in a security-sensitive file (config itself is fine).
- Manifest hygiene conforms (location permissions have `uses-feature required=false`); only
  `ACCESS_WIFI_STATE` lacks its matching `android.hardware.wifi` uses-feature (near-zero practical
  filtering).
- CI deletions vs upstream (create-release, e2e-smoke) are deliberate (manual release posture) —
  recorded so they don't read as drift.

## What is genuinely good (verified, not flattery)

- **offramp-lib** is the cleanest area audited: zero MAJOR findings, detekt/ktlint-clean without
  baseline entries, CancellationException rethrown before every broad catch, mnemonics as CharArray
  zeroized in `finally`, 127 passing tests covering security scenarios (pubkey-tampering fails
  closed, resume idempotency, byte-for-byte viem calldata parity).
- **evm-lib** crypto hygiene: RFC 6979 + low-s, constant-time MAC compare, rejection sampling,
  golden vectors pinned against the live EntryPoint, `toString` printing only the address.
- **Chat architecture**: file triads everywhere, `runChatCall` at 20+ sites (zero try/catch
  boilerplate), 239/239 EN↔ES key parity, `ChatScanPublicKeyVM` a faithful upstream mirror.
- **Common layer**: `EncryptedPreferenceKeys` (PBKDF2-100k, constant-time compare, transparent
  legacy upgrade), ProviderModule's secret-masking HTTP logger, fail-closed `P2P_NETWORK` parsing.
- `ZappComponents.kt` is genuinely frozen (last addition 2026-04-28); new components get own files.
- gradle.properties ship-safe (all sensitive keys blank); proguard additions carry exemplary WHY
  comments; auto-backup config is deliberate hardening.

## Recommended action plan

1. **Now (security):** `SecureScreen()` on SeedReveal + RestoreSeedEntry; fix the two
   secrets-in-exception seams (EncryptedJsonStore, hexToBytes); delete
   `.git-side-work/FORK-REVIEW-2026-05-28.md` from the repo (and history if it ever goes public),
   `.claude-flow/` → .gitignore, remove the `.!96846!strings.xml` artifact.
2. **One mechanical sweep:** externalize the ~150 hardcoded strings + create the 3 missing values-es
   files + 63 translations; `internal` on the ~33 public composables; rename the 3 live `*ViewModel`s;
   delete dead `restoresuccess/`.
3. **Two structural fixes:** UnifiedSend triad/naming cleanup; move tabs routing into a VM.
4. **Polish batch (MINOR debt):** `.update{}` in the 5 flagged spots, banner-comment removal,
   `@PreviewScreens` coverage in chat, import-order debt (ties into the known 93-violation ktlint
   red CI).
