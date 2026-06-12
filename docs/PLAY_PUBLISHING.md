# Zapp — Google Play publishing handoff

Everything needed to build a signed release bundle and ship it to Google Play
for the `xyz.justzappit.zapp` app. Written for whoever (human or agent) picks
up publishing next.

> This file deliberately contains NO raw secrets. The keystore password is not
> written here. See "Where the secrets live" below.

---

## 1. Current status (2026-06-11)

- App is **Zapp** (`xyz.justzappit.zapp`), in **Internal testing**, still a
  **Draft / unreviewed** app. Not in production.
- The build currently live on the Internal testing track is **versionCode 1839
  / 4.0.0** (release name "Internal 0.1"), and it is BROKEN (crashes on launch +
  new-wallet onboarding loops). The ProGuard keep rules that fix both bugs are
  on `main` (commits `eb107241` + `08a11824`), so a fresh build from `main` is
  the fix.
- **Signing is resolved.** The 2026-05-30 key documented in older revisions of
  this file (`3F:D5:DE…`, `zapp-upload-keystore.jks`) had its password lost and
  is dead. A second reset was filed 2026-06-08 and **approved ~2026-06-10/11**;
  the active upload key is now **v2** (`zapp-upload-v2.keystore`, SHA-1
  `1F:D6:DC…`). Full key history, fingerprints, and the how-a-reset-works
  explainer live in `docs/RELEASE_SIGNING.md` — that file is now the source of
  truth for signing; section 3 below is kept only as a quick reference.
- Next upload: build from `main` with `-PZCASH_VERSION_CODE=1841` (1839 is
  consumed on Play; 1840 was burned on a stale AAB from old `main` — confirm in
  App bundle explorer if you want to reuse it).

---

## 2. Play Console access

- Console: https://play.google.com/console
- Sign in with the Google account that owns the **JustZappIt** developer account
  (the project owner's email). Developer/account ID `5733186641146432299`.
- App ID `4974483008811516631`. Package `xyz.justzappit.zapp`.
- Useful deep links (swap nothing, these are this app):
  - App signing: https://play.google.com/console/u/0/developers/5733186641146432299/app/4974483008811516631/app-signing
  - The "Protected with Play" / integrity page is separate and is NOT where the
    upload key lives.
- To reach things by menu: pick the **Zapp** app first (the account-level menu
  with Policy status / Financial / etc. is the wrong level), then use the
  per-app left menu (Dashboard, Test and release, etc.).

---

## 3. Signing / the keystore (Play App Signing)

This app uses **Play App Signing**. That means:
- Google holds the real **app signing key** (cannot be lost, never changes).
- We sign uploads with an **upload key**. A lost upload key is recoverable by
  the account owner via an upload key reset.

**CURRENT upload key (v2, generated 2026-06-08, reset approved ~2026-06-10/11):**
- Keystore: `~/keys/justzappit/zapp-upload-v2.keystore`
- Alias: `zapp-upload`
- SHA-1: `1F:D6:DC:3A:55:C4:41:57:41:E3:48:C4:39:BE:EB:57:2E:5D:76:34`
- Full details, retired-key history, and the reset walkthrough:
  **`docs/RELEASE_SIGNING.md`** (source of truth).

> The key previously listed here (`3F:D5:DE…` in `zapp-upload-keystore.jks`,
> alias `upload`, generated 2026-05-30) is **DEAD** — its password was lost and
> Google rejects re-registration of old certificates. Do not use the `.jks`
> still sitting in `~/keys/justzappit/`.

### Upload key reset (quick reference)
Google never *sends* you a key — you generate the keystore locally, upload only
its exported certificate via App signing → **Request upload key reset**, and
Google approves silently within ~48h. To check approval, compare the *Upload key
certificate* fingerprints on the App signing page against
`keytool -list -v -keystore <keystore> | grep SHA1`. When they match, sign
uploads with that keystore. (Full walkthrough in `docs/RELEASE_SIGNING.md`.)

### Where the secrets live
- The keystore path/password are wired into the repo's **`local.properties`**
  (git-ignored) under the four `ZCASH_RELEASE_*` keys, so Gradle signs
  automatically on this machine. Keystore path must be **absolute** (no `~`).
- The raw password is also in the project owner's password manager. It is NOT in
  this repo or this doc on purpose.
- Do NOT move these into the repo's `gradle.properties` (that file is committed).
- Back up the keystore file AND its password. Losing either means another reset
  and ~48h of downtime.

---

## 4. Build the signed bundle (AAB)

The Play artifact is the **Store** flavor on the **mainnet** network, release
build type: variant `zcashmainnetStoreRelease`. This is the one whose
applicationId is exactly `xyz.justzappit.zapp` (foss = `.foss`, internal =
`.internal`).

```bash
cd ~/dev/zapp/zodl-android
./gradlew clean :app:bundleZcashmainnetStoreRelease -PZCASH_VERSION_CODE=<N>
```

Output:
`app/build/outputs/bundle/zcashmainnetStoreRelease/app-zcashmainnet-store-release.aab`

### versionCode rule (important)
- `versionCode` is computed in `app/build.gradle.kts`. When `ZCASH_VERSION_CODE`
  is `1` (the default in `gradle.properties`) it uses the **git commit count**.
  On this checkout the commit count is only ~497, which is LOWER than the 1839
  already on Play, so a default build would be rejected.
- Always pass `-PZCASH_VERSION_CODE=<N>` with **N greater than the highest
  versionCode already uploaded** (currently 1839, so use 1840, then 1841, ...).
- Confirm the highest uploaded code in Play Console under Test and release ->
  App bundle explorer before picking N.

### Firebase
- The Store build applies Firebase/Crashlytics only if
  `app/src/release/google-services.json` exists; if it's absent, the build still
  succeeds and just skips Crashlytics. It is not required to ship.

### Building locally with the debug key (for sideload testing only)
To test a minified release on a device without the real upload key, build a
`foss` release signed with the debug key (installs side-by-side as
`xyz.justzappit.zapp.foss`, NOT uploadable to Play):
```bash
./gradlew :app:assembleZcashmainnetFossRelease -PIS_SIGN_RELEASE_BUILD_WITH_DEBUG_KEY=true
```

---

## 5. Upload to Play (manual)

CI does not push to Play (see item 7), so uploading is manual:
1. Play Console -> Zapp -> **Test and release -> Internal testing -> Create new
   release**.
2. Upload the `.aab` from step 4.
3. Add a short release note, **Review release**, then **Start rollout to Internal
   testing**. Tester links update automatically.

For production later you must first run a **closed test with at least 12 testers
opted in for at least 14 days** (Play requirement, shown on the dashboard).

---

## 6. main vs PR #42

`main` was reset back to commit `4513293e` (PR #37) on 2026-05-30, then the two
ProGuard keep rules were cherry-picked back on top (commits `eb107241` +
`08a11824`). So **`main` now builds a working minified release**: it has the
Bare/chat stack AND the keep rules that stop R8 breaking it.

0xVampirot's UX redesign (Phases 1-4 + SEND/SWAP) plus the nav-bar inset fixes
and the wallet-encrypting animation are parked off `main` in:
- Branch: `feature/ux-redesign`
- Draft PR: https://github.com/JustZappIt/zodl-android-fork/pull/42

The keep rules (now on `main`, also in #42):
- `to.holepunch.bare.kit.**` — without it the app SIGABRTs on launch
  ("JNI DETECTED ERROR ... mid == null").
- `xyz.justzappit.zappmessaging.**` — without it new-wallet onboarding hangs
  (chat identity never derives in the minified build).

Note: a Store AAB built earlier at versionCode 1840 came from the OLD main
(redesign + fixes) and represents PR #42, not current `main`. Build a fresh AAB
from current `main` for upload.

---

## 7. CI (does not auto-publish)

- Workflow: `.github/workflows/release.yaml`, triggers on a **GitHub Release
  being published**.
- It builds `:app:bundleZcashmainnetStoreRelease` and a universal APK, signs with
  an upload keystore decoded from repo secrets, and attaches artifacts to the
  GitHub Release. It does **not** upload to Google Play (the Play publisher step
  is disabled, see TODO #1033 in the workflow).
- As of 2026-05-30 the `JustZappIt/zodl-android-fork` repo has **no Actions
  secrets configured** (no `UPLOAD_KEYSTORE_BASE_64`, etc.), so CI cannot
  currently produce a signed bundle. To use CI for signing later, add the new
  upload keystore (base64) and its passwords as repo/environment secrets matching
  the names referenced in `release.yaml`.
- Bottom line today: build locally per item 4 and upload per item 5.

---

## 8. One-page checklist to ship a fixed Internal testing build

1. Make sure the two ProGuard keep rules (item 6) are in the working tree
   (`app/proguard-project.txt`) — on `main` since 2026-05-30.
2. Confirm the upload key reset is approved in Play Console (item 3) and the
   four `ZCASH_RELEASE_*` props in `local.properties` point at the **v2**
   keystore.
3. **Preflight `local.properties`** — dev overrides silently bake into the AAB
   (`settings.gradle.kts` makes `local.properties` shadow `gradle.properties`):
   - `P2P_NETWORK=mainnet` — **`sepolia` AND blank both select Base Sepolia.**
     A store build must say `mainnet` explicitly or the offramp runs on testnet.
   - `OFFRAMP_USE_DEV_KEY=false` — `true` derives every user's smart account
     from the shared dev key instead of their wallet seed.
   - `PIMLICO_API_KEY` non-blank — blank crashes the offramp progress screen
     and Settings → P2P transactions at runtime (fail-fast by design).
   - `P2P_RPC_URL_BASE_MAINNET` + `P2P_SUBGRAPH_URL_MAINNET` non-blank —
     required when `P2P_NETWORK=mainnet`, or offramp DI throws.
   - No `ZAPP_MESSAGING_LOG_LEVEL=debug` (ships keypair dumps) and no stray
     `ZCASH_NETWORK=` override.
4. Pick `N` greater than the latest uploaded versionCode (currently 1839; 1840
   was burned on a stale AAB — next is 1841).
5. `./gradlew clean :app:bundleZcashmainnetStoreRelease -PZCASH_VERSION_CODE=N`
6. Verify before upload:
   - signature: `keytool -printcert -jarfile <aab> | grep SHA1` → must be
     `1F:D6:DC…` (the v2 upload key);
   - network: `ui-lib/build/generated/source/buildConfig/zcashmainnet/store/release/.../BuildConfig.java`
     shows `P2P_NETWORK = "mainnet"` and `OFFRAMP_USE_DEV_KEY = false`.
7. Upload the `.aab` to Internal testing and roll out (item 5).
