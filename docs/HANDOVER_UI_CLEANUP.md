# Handover: Zapp UI Cleanup — Remove Dead Routes, Expose Hidden Screens

**Date:** 2026-05-19
**Repo:** `zodl-android-fork`
**Plan file:** `.claude/plans/velvety-mapping-map.md`
**Design system spec:** `.claude/skills/zapp-android-ui/SKILL.md`

---

## What This Is

Zapp inherited upstream features (Feedback, WhatsNew, Integrations) that serve no purpose in the fork, and has useful tools (Export, Tax, Hotfixes, Debug, About) hidden behind dead-code blocks or secret gestures. This cleanup removes the dead routes and surfaces the useful screens through discoverable UX, all styled with the Zapp Swiss design system.

---

## Progress

| Step | Description | Status |
|------|-------------|--------|
| **1** | Remove Feedback, WhatsNew, Integrations routes | **Done** |
| **2** | Build Advanced Settings screen (new View + VM enhancements) | **Done** |
| **3** | Add Settings Tab rows (Advanced + About) | **Done** |
| **4** | Restyle About screen (Zashi -> Zapp) | **Done** |

---

## What Was Done

### Step 1: Remove Dead Routes

**Files changed (4):**

| File | What changed |
|------|-------------|
| `ui-lib/.../WalletNavGraph.kt` | Removed 3 route registrations (`FeedbackArgs`, `WHATS_NEW`, `IntegrationsArgs`) and 5 imports |
| `ui-lib/.../di/ViewModelModule.kt` | Removed 3 VM registrations (`WhatsNewViewModel`, `IntegrationsVM`, `FeedbackVM`) and 3 imports |
| `ui-lib/.../screen/more/MoreVM.kt` | Removed WhatsNew + Feedback list items, their handlers (`onWhatsNewClick`, `onSendUsFeedbackClick`), and 2 imports (`WHATS_NEW`, `FeedbackArgs`) |
| `ui-lib/.../common/appbar/ZashiTopAppBarVM.kt` | Replaced Integrations routing with direct `MoreArgs` navigation. Removed unused constructor params (`getVersionInfo`, `configurationRepository`), simplified `onInfoClick()` to no-arg, removed `DistributionDimension` import |

**Dead source files still on disk (harmless, no references):**
- `screen/feedback/FeedbackVM.kt`, `FeedbackScreen.kt`
- `screen/whatsnew/viewmodel/WhatsNewViewModel.kt`, `AndroidWhatsNew.kt`, model/view files
- `screen/integrations/IntegrationsVM.kt`, `IntegrationsScreen.kt`

These can be deleted in a follow-up. They compile but are unreachable (no route, no DI registration).

### Step 2: Build Advanced Settings Screen

**Files changed/created (4):**

| File | What changed |
|------|-------------|
| `screen/advancedsettings/AdvancedSettingsState.kt` | **Rewritten** — Replaced Zashi `ListItemState` with new `AdvancedSettingsItem(title: StringResource, icon: ImageVector, isEnabled: Boolean, onClick)`. State now has `onDeleteWallet: () -> Unit` instead of `deleteButton: ButtonState` |
| `screen/advancedsettings/AdvancedSettingsVM.kt` | **Rewritten** — Added 2 constructor params (`NavigateToExportPrivateDataUseCase`, `NavigateToTaxExportUseCase`). Items now include: Recovery, Export, Tax (disabled while restoring), Discover funds, Refresh transaction data, Disconnect HW (conditional Keystone), Developer tools (conditional DEBUG). All items use `ImageVector` icons |
| `screen/advancedsettings/AdvancedSettingsView.kt` | **New file** — Full Zapp-styled screen. Raw `Column` layout (no Scaffold), `ZappScreenHeader`, `ZappRow` items in bordered card, `ZappButton(Danger)` for delete, `ZappBottomActionBar` with back at bottom-left, `statusBars.union(displayCutout)` insets, scrollable body |
| `screen/advancedsettings/AdvancesSettingsScreen.kt` | **Rewritten** — Was a redirect stub (`back() + forward(ChatProfileArgs)`). Now hosts the real screen via `koinViewModel<AdvancedSettingsVM>` + `collectAsStateWithLifecycle` + `BackHandler` |

**Koin DI note:** No changes were needed to `ViewModelModule.kt` because `NavigateToExportPrivateDataUseCase` and `NavigateToTaxExportUseCase` are already registered as `factoryOf` in `UseCaseModule.kt`. Koin auto-resolves them.

**Filename note:** `AdvancesSettingsScreen.kt` still has the original typo. It has `@file:Suppress("ktlint:standard:filename")` to allow the mismatch. Renaming it would also require updating `WalletNavGraph.kt`'s import — deferred to avoid churn.

---

## What Remains

### Step 3: Add Settings Tab Rows

**File:** `ui-lib/.../screen/tabs/view/SettingsTabContent.kt`

This file already uses Zapp components correctly. Two additions needed:

**3a. Add "Advanced" row in the Wallet group** (after the existing Server row, around line 123):

```kotlin
ZappRowDivider(inset = true)
ZappRow(
    title = "Advanced",
    subtitle = "Recovery, export, developer tools",
    icon = Icons.Default.Tune,
    onClick = { navigationRouter.forward(AdvancedSettingsArgs) },
)
```

**3b. Add new "About" group** (after the Wallet group, before Support group, around line 126):

```kotlin
SettingsGroup(title = "About") {
    ZappRow(
        title = "About Zapp",
        subtitle = "Version, privacy policy, terms of use",
        icon = Icons.Default.Info,
        iconTint = c.accentText,
        iconBackground = c.accentSoft,
        onClick = { navigationRouter.forward(AboutArgs) },
    )
}
```

**Imports to add:** `AdvancedSettingsArgs`, `AboutArgs`, `Icons.Default.Tune`, `Icons.Default.Info`.

The existing `SettingsGroup` composable (defined at line 178) already wraps content in a bordered card with `c.surface` bg, `RectangleShape`, `1.dp c.border`. No changes needed to the wrapper.

### Step 4: Restyle About Screen

**File:** `ui-lib/.../screen/about/view/AboutView.kt`

This is a full Zashi-to-Zapp migration. The current file uses 11 Zashi-specific components/tokens that must all be replaced.

**Import replacement table:**

| Remove | Replace with |
|--------|-------------|
| `androidx.compose.material3.Scaffold` | Not needed (use raw `Column`) |
| `androidx.compose.material3.Text` | `androidx.compose.foundation.text.BasicText` |
| `ZashiSmallTopAppBar` | `ZappScreenHeader` |
| `ZashiTopAppBarBackNavigation` | Remove (back moves to `ZappBottomActionBar`) |
| `ZashiVersion` | Inline `BasicText` with `ZappTheme.typography.rowSubtitle` |
| `ZashiListItem` | `ZappRow` |
| `ZashiHorizontalDivider` | `ZappRowDivider(inset = true)` |
| `ZashiColors.*` | `ZappTheme.colors.*` |
| `ZashiTypography.*` | `ZappTheme.typography.*` |
| `ZashiDimensions.Spacing.*` | Hardcoded `18.dp` per Zapp spacing spec |
| `ZcashTheme` (in preview) | `ZappTheme` |
| `scaffoldScrollPadding(paddingValues)` | Remove (no Scaffold = no padding values) |
| `ListItemState` / `imageRes(R.drawable.*)` | Not needed (ZappRow uses `icon: ImageVector`) |
| `SettingsListItemLeadingIcon` | Not needed (ZappRow renders icons internally) |

**Structural change:**

```
BEFORE:                          AFTER:
Scaffold                         Column (fillMaxSize, c.bg, statusBars+displayCutout insets)
  topBar: ZashiSmallTopAppBar      ZappScreenHeader (title, right = debug menu if debug build)
    back in top-right corner       (no back here)
  body: Column                     Column (weight 1f, verticalScroll)
    Text (header6)                   BasicText (ZappTheme.typography.rowTitle)
    Text (textSm)                    BasicText (ZappTheme.typography.body, c.textMuted)
    ZashiListItem (privacy)          ZappRow in bordered card
    ZashiHorizontalDivider           ZappRowDivider(inset = true)
    ZashiListItem (terms)            ZappRow
    ZashiVersion                     BasicText (rowSubtitle, c.textSubtle, centered)
                                   ZappBottomActionBar(onBack = onBack)
```

**Debug menu:** Keep `DropdownMenu` / `DropdownMenuItem` (Material3, no Zapp equivalent). Move trigger from top app bar to `ZappScreenHeader`'s `right` slot. Replace inner `Text(...)` with `BasicText(...)` using `ZappTheme.typography.rowSubtitle.copy(color = c.text)`.

**Preview:** Change `ZcashTheme { ... }` to `ZappTheme { ... }`.

**Reference implementation:** Look at `AdvancedSettingsView.kt` (created in Step 2) for the exact pattern — it's the same layout structure (header + scrollable body + bottom action bar).

---

## Build & Verification

**Build command:**
```bash
./gradlew :app:assembleZcashtestnetStoreDebug
```

**JDK requirement:** JDK 17 (not 21+, not 25). The current dev machine has JDK 25 installed which causes Gradle to fail with a cryptic error message of just "25". Switch to JDK 17 before building:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

**Lint:**
```bash
./gradlew detektAll && ./gradlew ktlintFormat
```

**Zashi remnant check** (run after Step 4):
```bash
grep -rn "ZashiColors\|ZashiTypography\|ZashiSmallTopAppBar\|ZashiListItem\|ZashiHorizontalDivider\|ZashiVersion\|ZcashTheme" \
  ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/advancedsettings/ \
  ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/about/view/ \
  ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/tabs/view/SettingsTabContent.kt
```
Expected: zero hits.

**Manual test checklist:**
- [ ] Settings tab shows 4 groups: Security, Wallet, About, Support
- [ ] Settings > Wallet > Advanced opens Advanced Settings with all items
- [ ] Advanced > Recovery phrase navigates correctly
- [ ] Advanced > Export data navigates correctly
- [ ] Advanced > Tax export is dimmed/disabled while wallet is restoring
- [ ] Advanced > Discover funds opens hotfix screen
- [ ] Advanced > Refresh transaction data opens enhancement hotfix screen
- [ ] Advanced > Disconnect HW wallet appears only when Keystone account exists
- [ ] Advanced > Developer tools appears only in debug builds
- [ ] Advanced > Reset Zapp triggers delete/reset flow
- [ ] Advanced > Back button is bottom-left, navigates back
- [ ] Settings > About > About Zapp shows restyled About screen
- [ ] About > Privacy policy opens https://justzappit.xyz/privacy
- [ ] About > Terms of use opens https://justzappit.xyz/terms
- [ ] About > Back button is bottom-left
- [ ] About > Debug menu (overflow icon in header) appears only in debug builds
- [ ] More screen no longer shows Feedback or What's New
- [ ] More > version long-press still triggers Discover funds hotfix
- [ ] More > version double-tap still triggers Refresh transaction data hotfix
- [ ] All screens render correctly in dark mode
- [ ] TalkBack announces all interactive elements

---

## Design System Quick Reference

These rules apply to every screen in this cleanup. Violating any of them means Play Store rejection or design review failure.

| Rule | Implementation |
|------|---------------|
| Sharp corners only | `RectangleShape` on every border/bg — never `RoundedCornerShape` |
| ZappTheme tokens only | `val c = ZappTheme.colors` at top of every composable. Never `ZashiColors`, `MaterialTheme`, hardcoded hex |
| BasicText only | Never `Text(...)` from Material3 |
| Box + clickable | Never `Button`, `OutlinedButton`, `TextButton` |
| Back at bottom-left | `ZappBottomActionBar(onBack)` — never in top app bar |
| No Scaffold (unless snackbar) | Raw `Column` with `windowInsetsPadding(statusBars.union(displayCutout))` |
| Touch targets >= 48dp | `ZappRow` = 56dp, `ZappBackButton` = 48dp, `ZappButton` = 52dp |
| In-app screen gutters | `18.dp` horizontal padding (not `28.dp` which is for onboarding) |
| Card groups | `14.dp` horizontal padding, `c.surface` bg, `1.dp c.border`, `RectangleShape` |

**Component mapping (Zashi -> Zapp):**

| Zashi | Zapp |
|-------|------|
| `ZashiSmallTopAppBar` | `ZappScreenHeader` |
| `ZashiTopAppBarBackNavigation` | `ZappBottomActionBar` (back moves to bottom) |
| `ZashiListItem` + `ListItemState` | `ZappRow(title, icon, onClick)` |
| `ZashiHorizontalDivider` | `ZappRowDivider(inset = true)` |
| `ZashiButton` / `ZashiButtonDefaults` | `ZappButton(text, variant, onClick)` |
| `ZashiVersion` | `BasicText` with `ZappTheme.typography.rowSubtitle` |
| `BlankBgScaffold` | `Column` with `c.bg` background + manual insets |
| `scaffoldScrollPadding` | Not needed (no Scaffold) |

**Key source files:**
- Design system components: `ui-design-lib/.../component/zapp/ZappComponents.kt`
- Theme tokens: `ui-design-lib/.../theme/ZappTheme.kt`
- Full design system spec: `.claude/skills/zapp-android-ui/SKILL.md`
- Reference Zapp-styled screen: `screen/tabs/view/SettingsTabContent.kt`
