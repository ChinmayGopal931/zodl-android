# Zodl-android (Zapp fork)

Fork of upstream `zodl-inc/zodl-android` with Zapp's P2P messaging integrated. The upstream is the golden standard for style.

**Before writing or editing any `.kt` file, load `.claude/skills/zodl-style/SKILL.md`** — it carries the fork-specific conventions (ZappTheme vs ZashiTheme, the chat error helper, the dead Detekt rules, the file triad). Skip for one-line edits or pure documentation.

## Running locally

- Build/install: `./gradlew :app:installZcashtestnetFossDebug`, or `:app:installZcashmainnetFossDebug` for mainnet (side-by-side packages; launcher names "Zapp [DFT]" / "Zapp [DFM]").
- Package on device: `xyz.justzappit.zapp.testnet.foss.debug` (testnet) / `xyz.justzappit.zapp.foss.debug` (mainnet).
- Testnet lightwalletd is currently offline → home shows "Offline — reconnecting". Expected, not a regression.
- After pulling JS changes in `../zappMessaging/`: run `npm run build:android` there before `./gradlew`.

## Upstream coupling

`gradle.properties` sets `SDK_INCLUDED_BUILD_PATH=../zcash-android-wallet-sdk`; the SHA is pinned in `.zapp-deps`. Don't bump without coordinating with upstream-sync work.

## Release config — what NOT to commit to `gradle.properties`

Build properties that resolve through the `local.properties` → `gradle.properties` → env-var fallback chain (defined per-property in the relevant `build.gradle.kts`) belong in `local.properties` when set to a developer-only value. The committed default in `gradle.properties` must be safe to ship.

- `ZAPP_MESSAGING_LOG_LEVEL` — keep blank or `info` in `gradle.properties`. `debug` ships keypair dumps and verbose stream lifecycle into release `BuildConfig`. Put `debug` in your `local.properties` if you need it locally.
- `ZCASH_NETWORK` — keep blank for CI / side-by-side installs. Set to `mainnet` / `testnet` in `local.properties` for a single-flavor dev build.
- `BLIND_PEER_KEYS`, `BLIND_PEER_BOOTSTRAP` — public values, safe to commit.

Before tagging a release: diff `gradle.properties` against the last release tag and confirm no `*_LOG_LEVEL=debug` or `*_NETWORK=<flavor>` overrides slipped in.

## Android manifest hygiene

When adding a hardware-implying permission (`CAMERA`, `ACCESS_*_LOCATION`, `RECORD_AUDIO`, etc.) to `ui-lib/src/main/AndroidManifest.xml`, also declare the matching `<uses-feature>` with `android:required="false"`. Otherwise Google Play infers a hardware requirement from the permission and filters the eligible-device set.

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-feature android:name="android.hardware.location" android:required="false" />
<uses-feature android:name="android.hardware.location.gps" android:required="false" />
```
