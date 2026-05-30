# Zapp — Google Play publishing handoff

Everything needed to build a signed release bundle and ship it to Google Play
for the `xyz.justzappit.zapp` app. Written for whoever (human or agent) picks
up publishing next.

> This file deliberately contains NO raw secrets. The keystore password is not
> written here. See "Where the secrets live" below.

---

## 1. Current status (2026-05-30)

- App is **Zapp** (`xyz.justzappit.zapp`), in **Internal testing**, still a
  **Draft / unreviewed** app. Not in production.
- The build currently live on the Internal testing track is **versionCode 1839
  / 4.0.0**, and it is BROKEN (crashes on launch + new-wallet onboarding loops).
- As of 2026-05-30 the two ProGuard keep rules that fix both bugs ARE now on
  `main` (cherry-picked, commits `eb107241` + `08a11824`), so a minified release
  built from `main` launches and onboards correctly. Only 0xVampirot's redesign
  UI remains parked off `main` in PR #42 (item 6).
- The original upload key was on another laptop and is not available. A **new
  upload key was generated** and an **upload key reset was requested** in Play
  Console. Until Google approves that reset, Play will REJECT any bundle signed
  with the new key. Confirm approval before trying to upload.

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

New upload key (generated 2026-05-30):
- Keystore: `~/keys/justzappit/zapp-upload-keystore.jks`
- Certificate (the .pem uploaded to Google for the reset): `~/keys/justzappit/zapp-upload-certificate.pem`
- Alias: `upload`
- Algorithm: RSA 2048, valid until 2053
- SHA-256: `F2:12:5F:F4:98:17:2D:2E:3E:AF:62:A3:56:DB:C2:E5:A5:8E:4A:5B:52:11:E0:00:1F:F9:A1:CA:B4:E9:F6:95`
- SHA-1: `3F:D5:DE:55:AF:D5:19:57:5B:9C:57:E7:67:82:DC:30:2F:DC:88:42`

### Upload key reset (do this once, then it's done)
If Play rejects the bundle with "signed with the wrong key", the reset hasn't
been approved yet. To (re)request it:
1. Open the App signing deep link above.
2. Under **Upload key certificate**, choose **Request upload key reset**.
3. Reason: lost upload key. Upload `~/keys/justzappit/zapp-upload-certificate.pem`.
4. Wait for Google's confirmation email (can take up to ~48h). Installed testers
   are unaffected because Google holds the app signing key.

### Where the secrets live
- The keystore password (store password = key password) is **already wired** into
  `~/.gradle/gradle.properties` under the `ZCASH_RELEASE_*` keys, so Gradle reads
  it automatically on this machine. You do not need to type it to build here.
- The raw password is also in the project owner's password manager. It is NOT in
  this repo or this doc on purpose.
- The `~/.gradle/gradle.properties` keys: `ZCASH_RELEASE_KEYSTORE_PATH`,
  `ZCASH_RELEASE_KEYSTORE_PASSWORD`, `ZCASH_RELEASE_KEY_ALIAS`,
  `ZCASH_RELEASE_KEY_ALIAS_PASSWORD`. Do NOT move these into the repo's
  `gradle.properties` (that file is committed).
- Back up the `.jks` file somewhere safe. If it is lost again, it means another
  reset.

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
   (`app/proguard-project.txt`) — i.e. build from PR #42 or cherry-pick them.
2. Confirm the upload key reset is approved in Play Console (item 3).
3. Pick `N` greater than the latest uploaded versionCode (currently 1839).
4. `./gradlew clean :app:bundleZcashmainnetStoreRelease -PZCASH_VERSION_CODE=N`
5. Upload the `.aab` to Internal testing and roll out (item 5).
