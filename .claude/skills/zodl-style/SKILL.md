---
name: zodl-style
description: Fork-specific Kotlin/Compose conventions for zodl-android. Loads non-obvious style rules that aren't generic Kotlin/Compose knowledge — the ZappTheme/ZashiTheme split, the chat error helper, the dead Detekt rules, and the file triad pattern. MUST BE USED PROACTIVELY before creating new .kt files or making non-trivial edits in this repo. Skip for one-line edits or pure documentation.
---

The upstream `zodl-inc/zodl-android` is the golden standard. Match it for everything not called out here. When in doubt, find a sibling file in the original (cloned at `../zodl-android-real/` if available) and copy its shape.

## Theming — the most-violated rule

In any new fork-authored file:

- **Use** `ZappTheme.colors.*`, `ZappTheme.typography.*`, `ZappTheme.spacing.*` for every visual value.
- **Reject** direct use of `ZashiColors.*`, `ZashiTypography.*`, `ZashiDimensions.*`. They belong to the upstream layer; the fork's `ZappTheme` overlay is the visual source of truth.
- Structural wrappers stay: `ZashiTheme`, `ZashiScreenScaffold`, `ZashiSmallTopAppBar`, `ZashiButton`, `ZashiTextField`. Keep for upgradability.
- New design components live in `ui-design-lib/.../component/zapp/`.

When migrating an existing Zashi-token call site, the t-shirt scale maps cleanly: `8.dp` → `ZappTheme.spacing.md`, `12.dp` → `.lg`, `16.dp` → `.xl`, etc. Off-scale values (`14.dp`, `18.dp` for Swiss-design padding) stay raw.

## Strings — never hardcoded

- VM holds `error: StringResource?` (not `String?`). Set via `stringRes(R.string.foo)`. Import from `co.electriccoin.zcash.ui.design.util`.
- View renders via `.getValue()` for `StringResource`, or `stringResource(R.string.foo)` for pure-view text.
- New strings go in `ui-lib/src/main/res/ui/<feature>/values/strings.xml`, not the common file. Match existing `<feature>_<noun>_<purpose>` naming.

## Error handling

**Chat code** — use the existing helper:
```kotlin
runChatCall("ChatRoomVM: sendMessage failed") {
    sdk.sendMessage(conversationId, text)
}
```
Helper: `ui/screen/chat/common/ChatErrorHandling.kt`. Re-throws `CancellationException`, logs other `Exception`s via `Twig.warn`. Do **not** write per-call-site `try { … } catch (e: Exception) { Twig.warn(e) { … } }` + `@Suppress("TooGenericExceptionCaught")` — that boilerplate is what the helper exists to absorb.

**Outside chat** — the original idiom is:
```kotlin
runCatching { … }.onFailure { Twig.warn(it) { "..." } }
```
Don't catch broadly unless there's a concrete reason. Prefer letting exceptions propagate to the coroutine boundary.

## @Suppress discipline (the killer non-obvious rule)

`tools/detekt.yml` has:

```
LongMethod:          active: false
LongParameterList:   active: false
```

These rules **don't run**. Never add `@Suppress("LongMethod")` or `@Suppress("LongParameterList")` — they're no-ops. The fork accumulated dozens of these; the sweep removed them. New ones are forbidden.

Rules that ARE active and worth knowing:

| Rule | What to do when it fires |
|---|---|
| `TooGenericExceptionCaught` | Use `runChatCall` (chat) or catch specifically. Last resort: per-`catch` suppression, never per-function. |
| `CyclomaticComplexMethod` | Extract named helpers. Don't annotate. |
| `TooManyFunctions` | Class-level `@Suppress("TooManyFunctions")` on genuinely large VMs is the **one** annotation the original uses freely. Match that bar. |

Before adding any `@Suppress`: confirm the rule is active in `tools/detekt.yml`.

## File triad per screen

Three files in the same package, no `view/` subdir at the screen level (only chat does that for the many small composables — copy that pattern only there):

- `FooState.kt` — `data class`.
- `FooVM.kt` — VM, not `ViewModel`. Extends `androidx.lifecycle.ViewModel`. Exposes `val state: StateFlow<FooState?>`.
- `FooView.kt` — single `internal fun FooView(state: FooState, ...)` composable. **`internal`, not public.**

Variants: prefer `sealed interface`. Use cases: `GetXUseCase`, `NavigateToXUseCase`, `CreateXUseCase`.

Anti-pattern: class names like `OldHomeViewModel`, `LegacyChatScreen`. If something is genuinely deprecated, use `@Deprecated`. If it's still active code, name it for what it does.

## Comments

Default to no comments. Add one only when the WHY is non-obvious:

- ❌ "This file implements the chat room view model."
- ❌ "// affinity — the legacy ChatViewModel ran initialize() inside viewModelScope"
- ❌ Multi-paragraph KDoc explaining the upstream pattern this VM mirrors.
- ✅ "// Bare-kit's native IPC init has main-thread affinity; running off-main null-derefs in `bare_ipc_poll_init`."

Rule of thumb: if removing the comment wouldn't confuse a future reader, don't write it. Historical context, refactor narratives, and "used by X flow" notes belong in commit messages.

## Verify

After non-trivial Kotlin edits:
```
./gradlew :ui-lib:compileZcashmainnetFossDebugKotlin
```

For UI changes, install and drive:
```
./gradlew :app:installZcashtestnetFossDebug
adb shell monkey -p xyz.justzappit.zapp.testnet.foss.debug -c android.intent.category.LAUNCHER 1
```

Mainnet has no debug install task — only testnet. Testnet lightwalletd is currently offline; home will show "Offline — reconnecting" — that's expected, not a regression.
