# Zodl-android (Zapp fork)

Fork of upstream `zodl-inc/zodl-android` with Zapp's P2P messaging integrated. The upstream is the golden standard for style.

**Before writing or editing any `.kt` file, load `.claude/skills/zodl-style/SKILL.md`** — it carries the fork-specific conventions (ZappTheme vs ZashiTheme, the chat error helper, the dead Detekt rules, the file triad). Skip for one-line edits or pure documentation.

## Running locally

- Build/install: `./gradlew :app:installZcashtestnetFossDebug` (mainnet has no debug install task).
- Package on device: `xyz.justzappit.zapp.testnet.foss.debug`.
- Testnet lightwalletd is currently offline → home shows "Offline — reconnecting". Expected, not a regression.
- After pulling JS changes in `../zappMessaging/`: run `npm run build:android` there before `./gradlew`.

## Upstream coupling

`gradle.properties` sets `SDK_INCLUDED_BUILD_PATH=../zcash-android-wallet-sdk`; the SHA is pinned in `.zapp-deps`. Don't bump without coordinating with upstream-sync work.
