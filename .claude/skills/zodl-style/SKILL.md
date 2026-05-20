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

Applies to **both VMs and views** — every user-facing string round-trips through `strings.xml`. The rule is loudest in error toasts, dialog titles, button labels, and PIN/onboarding flows, which is where past audits found the most violations.

- VM holds `error: StringResource?` (not `String?`). Set via `stringRes(R.string.foo)`. Import from `co.electriccoin.zcash.ui.design.util`.
- View renders via `.getValue()` for `StringResource`, or `stringResource(R.string.foo)` for pure-view text. **Never** `BasicText(text = "Confirm")`, `Text("Retry")`, etc.
- New strings go in `ui-lib/src/main/res/ui/<feature>/values/strings.xml`, not the common file. Match existing `<feature>_<noun>_<purpose>` naming. Mirror to `values-es/strings.xml` in the same commit.
- Clipboard labels and accessibility content-descriptions count as user-facing.

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

- `FooState.kt` — `data class`. State lives in its own file; **do not** declare it inside `FooView.kt`.
- `FooVM.kt` — VM, not `ViewModel`. Extends `androidx.lifecycle.ViewModel`. Exposes `val state: StateFlow<FooState?>`.
- `FooView.kt` — single `internal fun FooView(state: FooState, ...)` composable.

### Two rules that get missed at scale

**Rule 1 — VM file and class are `FooVM`, not `FooViewModel`.**
Audit history: `OnboardingSecurityViewModel.kt`, `RestoreSuccessViewModel.kt`, `SecuritySettingsViewModel.kt`, `UnifiedSendViewModel.kt` all violated this and had to be renamed. When you create a VM, the class name ends in `VM` and the filename matches.

**Rule 2 — top-level screen composables are `internal`, not public.**
The route-entry composable in `Android<Foo>.kt` stays public (it's referenced by `MainActivity`'s nav graph). Everything in `FooView.kt`, `ChatRoomView.kt`, every dialog and bottom-sheet composable in `chat/view/` etc. is `internal`. A `fun FooView(...)` without `internal` is a fork-style violation even though Kotlin and Detekt are happy.

```kotlin
// FooView.kt
@Composable
internal fun FooView(state: FooState, ...) { ... }     // ✓
// fun FooView(state: FooState, ...) { ... }            // ✗ public by default
```

Variants: prefer `sealed interface`. Use cases: `GetXUseCase`, `NavigateToXUseCase`, `CreateXUseCase`.

Anti-pattern: class names like `OldHomeViewModel`, `LegacyChatScreen`. If something is genuinely deprecated, use `@Deprecated`. If it's still active code, name it for what it does.

## VM & screen wiring

Conventions that aren't obvious from reading one file but are pervasive across upstream. Match these in new fork code.

**StateFlow updates.** Use `MutableStateFlow.update { it.copy(foo = bar) }`, not `state.value = state.value.copy(...)`. Upstream uses `.update { }` everywhere — it's atomic, it composes, and Detekt-stable. Grep upstream's `SwapVM`, `AddSwapABContactVM` etc. for the shape.

**Koin DI module placement.** Don't make a new module for one binding. Put new things in their topic module:

| What you wrote | Module file |
|---|---|
| `ViewModel` | `di/ViewModelModule.kt` |
| `GetXUseCase` / `NavigateToXUseCase` / `CreateXUseCase` | `di/UseCaseModule.kt` |
| `Repository` (anything that owns state or a data source) | `di/RepositoryModule.kt` |
| `Provider` (anything that wraps `Context`, prefs, SDK access) | `di/ProviderModule.kt` |
| `Mapper` (DTO → domain conversions) | `di/MapperModule.kt` |

A new module file is only justified when you bring in a whole new domain (e.g. `ZappMessagingModule` for the JS worklet stack). New module files must also be added to the `startKoin { modules(...) }` list in `ZcashApplication.onCreate`.

**Navigation.** VMs inject `NavigationRouter` and call `.forward(args)`, `.back()`, `.replaceAll(...)`. Don't pass `NavController` into composables or VMs. Composables stay navigation-agnostic — they fire callbacks; the VM routes.

**Typed nav Args.** Each navigable screen has a `@Serializable data class FooArgs(...)` co-located with the screen (either its own `FooArgs.kt` file or inside `FooScreen.kt`). `Args` is the navigation handle *and* the typed parameter carrier. No `Bundle` extras, no `String?` URL params.

**Previews.** Use `@PreviewScreens` (the fork/upstream multi-config preview annotation), not bare `@Preview`. Preview functions are `private fun`. Wrap the body in `ZcashTheme(forceDarkMode = …) { … }`. For data, prefer fixtures from `cash.z.ecc.android.sdk.fixture.*` over hand-built mocks — they round-trip the real types.

## Design components — one component per file

`ui-design-lib/.../component/zapp/` mirrors upstream's Zashi layer: **one composable (or one tightly-related family) per `.kt` file**, named for the component (`ZappButton.kt`, `ZappFab.kt`, `ZappEyebrowTopAppBar.kt`, …). Upstream ships ~74 such files; the fork should match.

The legacy `ZappComponents.kt` aggregates ~17 components for historical reasons and is being broken up. **Do not add new components to it.** New design components get their own file. Enums and helpers tightly bound to one component (e.g. `ZappButtonVariant` with `ZappButton`) live alongside that component.

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
