# Zapp UX/UI Redesign Plan v2

> **Purpose:** Master reference for the Zapp Android UX overhaul. Every phase, task, screen spec, and audit checklist lives here. Update this doc as tasks complete.

---

## Table of Contents

- [Design System Rules](#design-system-rules)
- [Current State Summary](#current-state-summary)
- [Phase 1 — Quick Wins](#phase-1--quick-wins-ui-reshuffles)
- [Phase 2 — Merchant Pay Flow](#phase-2--merchant-pay-flow-new-screens)
- [Phase 3 — Onboarding Simplification](#phase-3--onboarding-simplification)
- [Phase 4 — Unified Contacts](#phase-4--unified-contacts-data-layer-refactor)
- [Master Audit Protocol](#master-audit-protocol)

---

## Design System Rules

Non-negotiable. Every screen must satisfy all of these. Source: `.claude/skills/zapp-android-ui/SKILL.md`.

| Rule | Spec |
|------|------|
| **Sharp corners** | `RectangleShape` everywhere. Zero `RoundedCornerShape`, zero `CircleShape`. |
| **Tokens only** | `val c = ZappTheme.colors`, `ZappTheme.typography`. No hex, no `MaterialTheme`. |
| **Black headings** | `FontWeight.Black` on all display, button, and label text. |
| **Box + clickable** | No `Button`, `OutlinedButton`, `TextButton`. All taps via `Box`/`Row` + `.clickable`. `ZappButton` wrapper is OK. |
| **Manual insets** | `windowInsetsPadding(WindowInsets.statusBars)` top, `windowInsetsPadding(WindowInsets.navigationBars)` bottom dock. No Scaffold padding. |
| **Back = bottom-left** | Never in top bar. Always `OnbBottomDock` (flows) or bottom dock `Row` (in-app). |
| **Actions in thumb zone** | All CTAs, back buttons, FABs at screen bottom. Primary actions within one-thumb reach. |
| **FABs at BottomEnd** | Square `ZappFab`, `RectangleShape`, accent bg, stacked `Column` at `Alignment.BottomEnd`, cleared above nav pill by `ZappNavBar.CLEARANCE_DP`. |
| **48dp touch targets** | Every tappable element >= 48x48dp. |
| **BasicText** | Not `Text`. |
| **28dp gutter** (onboarding) / **18dp gutter** (in-app) | Screen edge padding. |
| **Accessibility** | `semantics { role = Role.Button }` on tappable Box/Row. `contentDescription` on FABs. `invisibleToUser()` on decorative. |
| **imePadding()** | On every screen with a `BasicTextField`. |
| **displayCutout** | Union with `statusBars` on all fullscreen screens. |

### Anti-Patterns (grep check after every task)

```
RoundedCornerShape  CircleShape  MaterialTheme  Color(0x
^import.*Button$  OutlinedButton  TextButton
```

---

## Current State Summary

### Tab Structure (today): 4 tabs

| Tab | Icon | Default | Content |
|-----|------|---------|---------|
| Wallet | AccountBalanceWallet | **Yes** | Balance card + chart, activity, Send/Receive/Swap FABs |
| Chats | Chat | No | Identity setup wall → conversation list |
| Contacts | Contacts | No | Identity setup wall → contact list |
| Settings | Settings | No | Profile, security, wallet server, P2P txns, support |

### UPI Offramp (today) — buried path

```
Wallet tab → Swap FAB → SwapScreen → tap "OFFRAMP" tab → UpiOfframpView
  → enter USDC amount + UPI ID → 9-step on-chain progress
```

5 actions before the user sees the input form. "OFFRAMP" label meaningless to normal users.

### Key Problems

1. P2P payments invisible — buried 3 taps deep in chat attachment menu
2. App opens on Wallet, not Chats — contradicts "Chat privately" marketing
3. Contacts tab is dead weight — duplicates new-chat flow
4. Two disconnected contact systems (Chat Contacts vs Address Book)
5. UPI pay-a-merchant nested inside Swap with DeFi jargon
6. Onboarding is 10 steps with complexity front-loaded
7. Swap FAB competes with core Send/Receive actions

---

## Phase 1 — Quick Wins (UI reshuffles)

No data layer changes. Purely presentational — moving composables, changing defaults, renaming.

### Task 1.1 — Change default tab to CHATS

**Status:** [x] DONE

**File:** `ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/tabs/view/ZappTabsScaffold.kt`

**Change:** Line 89: `mutableStateOf(ZappTab.WALLET)` → `mutableStateOf(ZappTab.CHATS)`

**Audit:**
- [x] App opens on Chats tab
- [x] Other tabs still accessible
- [x] No other behavior changed

---

### Task 1.2 — Split chat attachment button into clip (media) + Z (payment)

**Status:** [x] DONE

**Files to change:**
- `ui-lib/.../screen/chat/view/ChatRoomView.kt` — input row: replace single `[+]` with two buttons
- `ui-lib/.../screen/chat/view/AttachmentSheet.kt` → rename to `MediaSheet.kt`, keep only Photo/File/Camera
- New: `ui-lib/.../screen/chat/view/PaymentSheet.kt` — Send ZEC, Request ZEC, Pay Merchant, Share Address

**Input row layout (thumb zone):**
```
[ message input...            ] [clip] [Z] [send]
```

Both buttons in the bottom input row — already in thumb zone. Each >= 48x48dp.

**Z button spec:**
```kotlin
Box(
    modifier = Modifier
        .size(48.dp)
        .background(c.accent, RectangleShape)
        .clickable(onClick = onPaymentSheet)
        .semantics { contentDescription = "Payment options"; role = Role.Button },
    contentAlignment = Alignment.Center,
) {
    BasicText("Z", style = ZappTheme.typography.button.copy(
        color = c.onAccent, fontWeight = FontWeight.Black, fontSize = 18.sp
    ))
}
```

**PaymentSheet spec:**
```kotlin
ModalBottomSheet(
    containerColor = c.surface,
    scrimColor = c.overlay,
    shape = RectangleShape,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, bottom = 24.dp)
    ) {
        PaymentRow(icon = Icons.AutoMirrored.Filled.Send, label = "Send ZEC", onClick = onSendZec)
        HorizontalDivider(color = c.border)
        PaymentRow(icon = Icons.Default.CallReceived, label = "Request ZEC", onClick = onRequestZec)
        HorizontalDivider(color = c.border)
        PaymentRow(icon = Icons.Default.Storefront, label = "Pay Merchant", onClick = onPayMerchant)
        HorizontalDivider(color = c.border)
        PaymentRow(icon = Icons.Default.QrCode2, label = "Share Address", onClick = onShareAddress)
    }
}
```

Each `PaymentRow`: `Row` with `.clickable`, `semantics { role = Role.Button }`, icon `c.accent` 24dp, label `BasicText` `.rowTitle` `FontWeight.Black`. Touch target >= 48dp (16dp padding * 2 + content).

**Audit:**
- [ ] Z button: 48x48dp, `RectangleShape`, `c.accent` bg, `c.onAccent` text
- [ ] Clip button: 48x48dp, no rounded corners
- [ ] PaymentSheet: `c.surface` bg, `RectangleShape`, `c.overlay` scrim
- [ ] All rows: `BasicText`, `.rowTitle`, `FontWeight.Black`
- [ ] All rows: `semantics { role = Role.Button }`
- [ ] `navigationBarsPadding()` on sheet content
- [ ] Each row touch target >= 48dp height
- [ ] No `Text`, `Button`, `OutlinedButton`, `MaterialTheme`

---

### Task 1.3 — Rename Wallet → Pay, Settings → You, remove Contacts tab from enum

**Status:** [x] DONE

**File:** `ui-lib/.../screen/tabs/view/FloatingPillNavBar.kt`

**Changes:**
```kotlin
enum class ZappTab(val title: String) {
    PAY("Pay"),       // was WALLET
    CHATS("Chats"),   // unchanged
    YOU("You"),       // was SETTINGS
    // CONTACTS removed
}
```

Icon mapping:
```kotlin
ZappTab.PAY -> if (selected) Icons.Filled.Payment else Icons.Outlined.Payment
ZappTab.CHATS -> // unchanged
ZappTab.YOU -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
```

**Also:** increase `defaultMinSize(minHeight = 48.dp)` on each tab cell (currently 40dp — below minimum).

**Audit:**
- [ ] Nav pill: 3 tabs — Pay / Chats / You
- [ ] Icons: Filled (selected) / Outlined (unselected)
- [ ] Each tab cell >= 48dp height
- [ ] `RectangleShape` on backgrounds/borders
- [ ] `ZappTheme.typography.chip` for labels

---

### Task 1.4 — Remove Contacts tab content, merge into You

**Status:** [x] DONE

**Files to change:**
- `ZappTabsScaffold.kt` — remove `ZappTab.CONTACTS ->` branch, remove `ContactsTabContent()` composable
- `SettingsTabContent.kt` — add "People" group with "Contacts" row

**New row in SettingsTabContent:**
```kotlin
SettingsGroup(title = "People") {
    ZappRow(
        title = "Contacts",
        subtitle = "Manage your chat contacts",
        icon = Icons.Default.Contacts,
        iconTint = c.accentText,
        iconBackground = c.accentSoft,
        onClick = { navigationRouter.forward(ChatContactsArgs) },
    )
}
```

**Audit:**
- [ ] Only 3 tabs in nav pill
- [ ] Contacts accessible from You → People → Contacts
- [ ] No `ContactsTabContent` composable remaining
- [ ] `ZappRow` uses `c.accentText` tint, `c.accentSoft` bg
- [ ] Row touch target >= 48dp

---

### Task 1.5 — Rename Settings → You in screen header

**Status:** [x] DONE

**File:** `ui-lib/.../screen/tabs/view/SettingsTabContent.kt`

**Change:** `ZappScreenHeader(title = "You")` (was `"Settings"`)

**Audit:**
- [ ] Tab pill says "You"
- [ ] Screen header says "You"
- [ ] Person icon on tab

---

### Task 1.6 — Restructure Pay tab — add Pay Merchant FAB, demote Swap

**Status:** [x] DONE

**Files to change:**
- `WalletHomeView.kt` — replace `WalletActionFabStack` with `PayActionFabStack`, rename header to "Pay"
- `WalletActionFabStack.kt` → rename to `PayActionFabStack.kt`, change FABs
- `SettingsTabContent.kt` — add Swap row under Wallet group

**New FAB stack (BottomEnd, thumb zone):**
```kotlin
@Composable
internal fun PayActionFabStack(
    onPayMerchant: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(end = 18.dp, bottom = ZappNavBar.FAB_BOTTOM_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.End,
    ) {
        ZappFab(icon = Icons.Default.Storefront, contentDescription = "Pay Merchant", onClick = onPayMerchant)
        ZappFab(icon = Icons.AutoMirrored.Filled.CallMade, contentDescription = "Send", onClick = onSend)
        ZappFab(icon = Icons.AutoMirrored.Filled.CallReceived, contentDescription = "Receive", onClick = onReceive)
    }
}
```

Pay Merchant is topmost FAB (closest to natural thumb position).

**Swap moves to You tab** under Wallet group:
```kotlin
ZappRow(
    title = "Swap",
    subtitle = "Exchange crypto assets",
    icon = Icons.Default.SwapHoriz,
    onClick = { navigationRouter.forward(SwapArgs) },
)
```

**Audit:**
- [ ] 3 FABs: Pay Merchant (top), Send, Receive
- [ ] Each: `ZappFab` — 56x56dp, `RectangleShape`, `c.accent` bg, `c.onAccent` icon
- [ ] Each: non-null `contentDescription`
- [ ] Anchored `Alignment.BottomEnd`, cleared above nav pill
- [ ] Swap FAB removed from Pay tab
- [ ] Swap in You → Wallet → Swap row
- [ ] Screen header says "Pay"

---

### Phase 1 Completion Audit

Run after ALL 1.x tasks done:

- [ ] **3 tabs only**: Chats (default), Pay, You
- [ ] **Default = Chats**
- [ ] **Nav pill min height**: each cell >= 48dp
- [ ] **No Contacts tab**: reachable from You → People
- [ ] **Chat input**: two buttons (clip + Z) + send
- [ ] **Z button**: 48dp, `RectangleShape`, accent, `contentDescription`
- [ ] **PaymentSheet**: `c.surface`, sharp corners, 4 options
- [ ] **MediaSheet**: `c.surface`, sharp corners, 3 options
- [ ] **Pay tab FABs**: 3 square FABs at BottomEnd
- [ ] **Swap demoted**: You → Wallet → Swap
- [ ] **All CTAs in thumb zone**
- [ ] **No `Button`/`OutlinedButton`/`TextButton`**
- [ ] **No `MaterialTheme`**
- [ ] **No `RoundedCornerShape`**
- [ ] **`BasicText` only** (not `Text`)
- [ ] **`FontWeight.Black`** on headings/labels/buttons
- [ ] **Semantics**: `role = Role.Button` on all tappable
- [ ] **Insets**: `statusBars + displayCutout` top, `navigationBars` bottom

**Grep check:**
```bash
grep -rn "RoundedCornerShape\|CircleShape\|MaterialTheme\|Color(0x" <changed-files>
grep -rn "^import.*Button$\|OutlinedButton\|TextButton" <changed-files>
```

**Build:** `./gradlew :app:assembleZcashtestnetFossDebug`

---

## Phase 2 — Merchant Pay Flow (new screens)

The on-chain orchestrator (`OfframpDriver`, `OfframpCheckpointPersister`) stays unchanged. New UI screens wrap the same backend.

### Task 2.1 — MerchantPayScreen (camera-first QR + manual entry + recents)

**Status:** [ ] Not started

**New files:**
- `ui-lib/.../screen/merchantpay/MerchantPayScreen.kt`
- `ui-lib/.../screen/merchantpay/MerchantPayView.kt`
- `ui-lib/.../screen/merchantpay/MerchantPayVM.kt`
- `ui-lib/.../screen/merchantpay/MerchantPayArgs.kt`

**Reuses:** `UpiQrParser`, `ScanUpiVM` logic, `RecentMerchantRepository` (task 2.5)

**Screen layout — B2 dock pattern (back bottom-left, CTA bottom-right):**
```
┌─────────────────────────────────────────────────┐
│  (statusBars + displayCutout)                   │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌─────────────────────────────────────────┐    │  18dp gutter
│  │       [ Camera Viewfinder ]             │    │  c.surface bg, 1dp c.border
│  │    Scan a UPI QR code to pay            │    │  RectangleShape
│  └─────────────────────────────────────────┘    │
│                                                 │
│  --- or enter manually ---                      │  SentenceFragment
│                                                 │
│  UPI ID                                         │  .eyebrow, 10sp, Black
│  ┌──────────────────────────────── [scan]──┐    │  2dp border when non-empty
│  │ merchant@upi                            │    │  ZashiAddressTextField
│  └─────────────────────────────────────────┘    │
│                                                 │
│  --- Recent merchants ---                       │
│                                                 │
│  ┌─────────────────────────────────────────┐    │  1dp border card
│  │  Chai Point    merchant@upi       R85   │    │  ZappRow pattern, >= 48dp
│  ├─────────────────────────────────────────┤    │  1dp divider
│  │  DMart         dmart@axis       R1200   │    │
│  └─────────────────────────────────────────┘    │
│                                                 │
├─────────────────────────────────────────────────┤
│ [<-] [          CONTINUE          ]            │  B2 dock: 72x52 back + accent CTA
│       (navBars)                                 │
└─────────────────────────────────────────────────┘
```

**Key specs:**
- Camera viewfinder: `Box`, `c.surface` bg, 1dp `c.border`, `RectangleShape`
- UPI field: `ZashiAddressTextField`, 2dp border active, 1dp empty
- Scan icon: 48x48dp `Box + clickable`, `c.accent` icon
- Recent merchants: `OnbActionListCard`-style bordered card, rows >= 48dp, `clickable + semantics`
- Bottom dock: B2 pattern — 72x52 back left, accent CTA right, in thumb zone
- `.imePadding()` on scrollable column
- `windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))` top

**Audit:**
- [ ] Camera: `RectangleShape`, `c.surface`, 1dp `c.border`
- [ ] UPI field: `ZashiAddressTextField`, 2dp border non-empty
- [ ] Scan button: >= 48x48dp, `semantics`
- [ ] Merchant rows: >= 48dp, `clickable`, `semantics`
- [ ] B2 dock: back 72x52 left, CTA accent right
- [ ] `.imePadding()` present
- [ ] `statusBars.union(displayCutout)` top, `navigationBars` dock
- [ ] `BasicText` + `ZappTheme.typography` only
- [ ] No `RoundedCornerShape`, no `MaterialTheme`

---

### Task 2.2 — MerchantAmountScreen (INR-primary amount entry)

**Status:** [ ] Not started

**New files:**
- `ui-lib/.../screen/merchantpay/MerchantAmountScreen.kt`
- `ui-lib/.../screen/merchantpay/MerchantAmountView.kt`
- `ui-lib/.../screen/merchantpay/MerchantAmountVM.kt`
- `ui-lib/.../screen/merchantpay/MerchantAmountArgs.kt`

**Reuses:** exchange rate from `UpiOfframpVM` pattern, `ExchangeRateRepository`

**Screen layout — B2 dock:**
```
┌─────────────────────────────────────────────────┐
│  (statusBars + displayCutout)                   │
├─────────────────────────────────────────────────┤
│                                                 │
│  Paying                                         │  .eyebrow, accent
│  Chai Point                                     │  .display, 28sp, Black
│  merchant@upi                                   │  .body, textMuted
│                                                 │
│        R [ 85.00 ]                              │  Large input, 28sp Black
│        ~ 0.23 ZEC                               │  .body, textMuted
│                                                 │
│  Available: R4,560 (12.34 ZEC)                  │  .caption, textMuted
│                                                 │
│  ┌──────────────────────────────────────────┐   │
│  │ 3dp accent | HOW IT WORKS               │   │  Swiss tip block
│  │   stripe   | Your ZEC converts           │   │  (ReceiveView pattern)
│  │            | automatically. Merchant gets│   │
│  │            | INR via UPI. ~1-3 min.      │   │
│  └──────────────────────────────────────────┘   │
│                                                 │
├─────────────────────────────────────────────────┤
│ [<-] [          PAY R85            ]           │  B2 dock, dynamic CTA text
│       (navBars)                                 │
└─────────────────────────────────────────────────┘
```

**Key specs:**
- Amount input: INR primary (R prefix), ZEC as subtitle
- Swiss tip block: 3dp accent left stripe + "HOW IT WORKS" eyebrow + body (matches ReceiveView)
- CTA: "PAY R{amount}" uppercase, `FontWeight.Black`, accent bg
- `.imePadding()` for amount input

**Audit:**
- [ ] INR-primary (not USDC)
- [ ] ZEC subtitle in `c.textMuted`
- [ ] Swiss tip block matches ReceiveView pattern
- [ ] CTA: "PAY R{amount}", uppercase, Black
- [ ] B2 dock: back 72x52 + accent CTA
- [ ] `.imePadding()`
- [ ] All insets correct

---

### Task 2.3 — MerchantProgressScreen (3 human-readable steps)

**Status:** [ ] Not started

**New files:**
- `ui-lib/.../screen/merchantpay/MerchantProgressScreen.kt`
- `ui-lib/.../screen/merchantpay/MerchantProgressView.kt`
- `ui-lib/.../screen/merchantpay/MerchantProgressVM.kt`
- `ui-lib/.../screen/merchantpay/MerchantProgressArgs.kt`

**Delegates to:** existing `OfframpDriver`, `OfframpCheckpointPersister`

**Screen layout — B1 dock (ZappBackButton + ZappButton):**
```
┌─────────────────────────────────────────────────┐
│  (statusBars + displayCutout)                   │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────────────────────────────────────┐   │
│  │           R85                            │   │  Amount card
│  │        to merchant@upi                   │   │  c.surface, 1dp c.border
│  │        Chai Point                        │   │  RectangleShape
│  └──────────────────────────────────────────┘   │
│                                                 │
│  ┌──────────────────────────────────────────┐   │
│  │ [accent] Processing payment...       ... │   │  Step 1: in progress
│  │ [border] Sending to merchant             │   │  Step 2: pending
│  │ [border] Confirming receipt              │   │  Step 3: pending
│  └──────────────────────────────────────────┘   │
│                                                 │
│  Usually takes 1-3 minutes                      │  .caption, textMuted
│                                                 │
├─────────────────────────────────────────────────┤
│  [<-]  [       CANCEL / DONE       ]           │  B1 dock
│         (navBars)                               │
└─────────────────────────────────────────────────┘
```

**Status mapping (OfframpStatus → user step):**

| OfframpStatus | User step | Label |
|---|---|---|
| `Idle`, `BridgingFunds`, `FundedFromBase`, `ApprovingUsdc`, `PlacingOrder` | 1 IN_PROGRESS | "Processing payment..." |
| `WaitingForMerchantAcceptance`, `SendingEncryptedUpi` | 2 IN_PROGRESS | "Sending to merchant..." |
| `WaitingForCompletion` | 3 IN_PROGRESS | "Confirming receipt..." |
| `Completed` | All COMPLETED | "Paid" |
| `Cancelled` | — | "No merchant available" |
| `Failed` | — | "Payment couldn't be completed" |

**Step indicator spec:**
```kotlin
@Composable
private fun StepRow(label: String, status: StepStatus) {
    val c = ZappTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(12.dp).background(
            when (status) {
                StepStatus.PENDING -> c.border
                StepStatus.IN_PROGRESS -> c.accent
                StepStatus.COMPLETED -> c.accent
                StepStatus.FAILED -> c.danger
            }, RectangleShape
        ))
        BasicText(label, style = ZappTheme.typography.rowTitle.copy(
            color = when (status) {
                StepStatus.PENDING -> c.textMuted
                StepStatus.IN_PROGRESS -> c.text
                StepStatus.COMPLETED -> c.text
                StepStatus.FAILED -> c.danger
            },
            fontWeight = if (status == StepStatus.IN_PROGRESS) FontWeight.Black else FontWeight.Medium,
        ))
        if (status == StepStatus.IN_PROGRESS)
            CircularProgressIndicator(color = c.accent, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        if (status == StepStatus.COMPLETED)
            BasicText("done", style = ZappTheme.typography.chip.copy(color = c.success))
    }
}
```

**CTA changes by state:**
- In progress: "CANCEL" (`ZappButtonVariant.Danger`)
- Completed: "DONE" (`ZappButtonVariant.Primary`)
- Failed/Cancelled: "CLOSE" (`ZappButtonVariant.Ghost`)

**Completed state** shows: amount, merchant, duration, fee, "View technical details" row → opens existing `UpiOfframpProgressView`.

**Audit:**
- [ ] Step indicators: 12x12dp square, `RectangleShape`, token colors
- [ ] Labels: `BasicText`, `.rowTitle`
- [ ] Amount card: `c.surface`, 1dp `c.border`, `RectangleShape`
- [ ] B1 dock: `ZappBackButton` + `ZappButton`
- [ ] CTA in thumb zone
- [ ] "View technical details" opens existing 9-step view
- [ ] No jargon (USDC, bridge, NEAR, Diamond hidden)
- [ ] Insets correct
- [ ] Error: `c.danger` text, retry in `c.textMuted`

---

### Task 2.4 — MerchantPaymentBubble (chat bubble)

**Status:** [ ] Not started

**New file:** `ui-lib/.../screen/chat/view/bubbles/MerchantPaymentBubble.kt`

Follows `TransactionBubble.kt` / `PaymentRequestBubble.kt` pattern:

```kotlin
@Composable
internal fun MerchantPaymentBubble(message: ChatMessage, isFromMe: Boolean, modifier: Modifier = Modifier) {
    val c = ZappTheme.colors
    val data = remember(message.content) { parseMerchantPayment(message.content) }
    Column(
        modifier = modifier
            .background(if (isFromMe) c.accentSoft else c.surface, RectangleShape)
            .border(1.dp, c.border, RectangleShape)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Storefront, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
            BasicText("Paid", style = ZappTheme.typography.chip.copy(color = c.accentText, fontWeight = FontWeight.Black))
        }
        Spacer(Modifier.height(6.dp))
        BasicText("R${data.amountInr}", style = ZappTheme.typography.rowTitle.copy(color = c.text, fontWeight = FontWeight.Black))
        BasicText("to ${data.merchantName}", style = ZappTheme.typography.rowSubtitle.copy(color = c.textMuted))
    }
}
```

**Audit:**
- [ ] `RectangleShape`, token colors
- [ ] `BasicText` throughout
- [ ] `accentSoft` outgoing, `surface` incoming
- [ ] Icon: `c.accent`, 20dp

---

### Task 2.5 — RecentMerchantRepository

**Status:** [ ] Not started

**New file:** `ui-lib/.../common/repository/RecentMerchantRepository.kt`

DataStore persistence:
```kotlin
data class RecentMerchant(
    val upiId: String,
    val displayName: String?,
    val lastAmountInr: String?,
    val lastUsedTimestamp: Long,
)
```

**Audit:**
- [ ] Data layer only, no UI
- [ ] Injected via Koin

---

### Task 2.6 — Navigation registration

**Status:** [ ] Not started

**File:** `ui-lib/.../WalletNavGraph.kt`

Add:
```kotlin
composable<MerchantPayArgs> { MerchantPayScreen() }
composable<MerchantAmountArgs> { MerchantAmountScreen(it.toRoute()) }
composable<MerchantProgressArgs> { MerchantProgressScreen(it.toRoute()) }
```

**Audit:**
- [ ] All 3 routes registered
- [ ] Args: `@Serializable` data classes

---

### Task 2.7 — Wire entry points

**Status:** [ ] Not started

| Entry | Trigger | Target |
|---|---|---|
| Pay tab FAB | `ZappFab` onClick | `navigationRouter.forward(MerchantPayArgs)` |
| Chat Z → "Pay Merchant" | `PaymentSheet` row | `navigationRouter.forward(MerchantPayArgs)` |

**Audit:**
- [ ] Pay Merchant from Pay tab: 1 tap (FAB)
- [ ] Pay Merchant from chat: 2 taps (Z → row)
- [ ] Both navigate to same `MerchantPayArgs`

---

### Phase 2 Completion Audit

- [ ] **MerchantPayScreen**: camera + UPI field + recents, B2 dock
- [ ] **MerchantAmountScreen**: INR-primary, Swiss tip, B2 dock "PAY RX"
- [ ] **MerchantProgressScreen**: 3 steps, status mapping, B1 dock
- [ ] **MerchantPaymentBubble**: sharp corners, tokens, BasicText
- [ ] **All screens**: `RectangleShape`, `ZappTheme` only
- [ ] **All docks**: back left, CTA right, thumb zone
- [ ] **All inputs**: `.imePadding()`
- [ ] **All insets**: `statusBars.union(displayCutout)` top, `navigationBars` bottom
- [ ] **48dp minimum**: every tappable
- [ ] **Semantics**: `role = Role.Button`, `contentDescription`
- [ ] **No jargon**: USDC/bridge/NEAR/Diamond hidden from user
- [ ] **Technical details**: via "View details" → existing `UpiOfframpProgressView`

---

## Phase 3 — Onboarding Simplification

### Task 3.1 — Remove MSG_INTRO step

**Status:** [ ] Not started

**File:** `ui-lib/.../screen/onboarding/ZappOnboardingFlow.kt`

Remove `Step.MSG_INTRO` from enum. Initial state: `mutableStateOf(Step.MSG_USERNAME)`. Remove `MessagingPhaseIntro` reference. `UsernameEntryScreen` onBack → `onBackToWelcome`.

**Audit:** [ ] Step count 10 → 9

---

### Task 3.2 — Remove WALLET_INTRO step

**Status:** [ ] Not started

Remove `Step.WALLET_INTRO`. After username → straight to `WALLET_CHOICE` (Tor also removed in 3.3).

**Audit:** [ ] Step count 9 → 8

---

### Task 3.3 — Move TOR_OPTION to You → Privacy

**Status:** [ ] Not started

Remove `Step.TOR_OPTION` from onboarding. Add row in SettingsTabContent:
```kotlin
SettingsGroup(title = "Privacy") {
    ZappRow(title = "Tor", subtitle = if (torEnabled) "Enabled" else "Disabled",
        icon = Icons.Default.Security, onClick = { navigationRouter.forward(TorSettingsArgs) })
}
```

**Audit:** [ ] Step count 8 → 7. Tor in You → Privacy.

---

### Task 3.4 — Defer seed phrase (create path)

**Status:** [ ] Not started

On create: skip `Step.WALLET_SEED`, auto-advance to `SECURE_CHOICE` once wallet + identity ready.

Add persistent backup banner to Pay tab home (`WalletHomeView.kt`):
```kotlin
if (!hasSeedBeenBackedUp) {
    Box(
        modifier = Modifier
            .fillMaxWidth().padding(horizontal = 18.dp)
            .background(c.accentSoft, RectangleShape)
            .border(1.dp, c.accent, RectangleShape)
            .clickable(onClick = onBackupSeed)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { role = Role.Button; contentDescription = "Back up your recovery phrase" },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BasicText("!", style = ZappTheme.typography.rowTitle.copy(color = c.accent))
            Column {
                BasicText("Back up your recovery phrase",
                    style = ZappTheme.typography.rowTitle.copy(color = c.text, fontWeight = FontWeight.Black))
                BasicText("Protect your wallet and messages",
                    style = ZappTheme.typography.rowSubtitle.copy(color = c.textMuted))
            }
        }
    }
}
```

Banner: `c.accentSoft` bg, `c.accent` border, `RectangleShape`, >= 48dp, `semantics`. Dismisses after user views + confirms seed.

**Audit:**
- [ ] Create path: 5 steps (Welcome → Username → Choice → Secure → Done)
- [ ] Banner on Pay tab until backed up
- [ ] Banner: `RectangleShape`, token colors, >= 48dp, `semantics`

---

### Task 3.5 — Simplify restore flow

**Status:** [ ] Not started

**File:** `ui-lib/.../screen/onboarding/ZappRestoreFlow.kt`

Remove Tor step. Flow: USERNAME → SEED_ENTRY → BIRTHDAY → SECURE_CHOICE → BIO/PIN → DONE (6 steps).

**Audit:** [ ] Restore flow 10 → 6 steps.

---

### Phase 3 Completion Audit

- [ ] **New user**: Welcome → Username → Wallet Choice → Secure → Done (5 steps)
- [ ] **Restore**: Welcome → Username → Seed → Birthday → Secure → Done (6 steps)
- [ ] **Tor**: You → Privacy only
- [ ] **Seed**: deferred on create, shown on restore
- [ ] **Backup banner**: Pay tab, `c.accentSoft`, `RectangleShape`, >= 48dp, `semantics`
- [ ] **OnbProgress**: step totals adjusted
- [ ] **All onboarding**: 28dp gutter, `OnbBottomDock` back bottom-left
- [ ] **Insets**: `statusBars.union(displayCutout)`, `navigationBars`, `.imePadding()`

---

## Phase 4 — Unified Contacts (data layer refactor)

### Task 4.1 — ZappContact entity

**Status:** [ ] Not started

```kotlin
@Entity(tableName = "zapp_contacts")
data class ZappContact(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val publicKey: String? = null,
    val walletAddress: String? = null,
    val merchantUpi: String? = null,
    val lastInteractionTimestamp: Long = 0L,
)
```

### Task 4.2 — Migrate ChatContactsRepository

Wrap to read/write from unified model.

### Task 4.3 — Migrate AddressBookRepository

Merge wallet address book into unified contacts.

### Task 4.4 — Update all contact pickers

- `NewConversationView` → `publicKey != null`
- `UnifiedSendScreen` → `walletAddress != null`
- `MerchantPayScreen` → `merchantUpi != null`, sorted by timestamp

### Task 4.5 — Connect Pay → Chat

Send ZEC from Pay tab to contact with `publicKey` → transaction bubble in chat.

### Phase 4 Completion Audit

- [ ] Single `ZappContact` entity
- [ ] All pickers use unified repository
- [ ] Pay → Chat: transaction in conversation
- [ ] No orphaned contacts
- [ ] All contact rows: >= 48dp, `clickable`, `semantics`

---

## Master Audit Protocol

### After each task (1.1, 1.2, etc.)

1. Verify task-specific audit checklist above
2. Grep changed files for anti-patterns:
   ```bash
   grep -rn "RoundedCornerShape\|CircleShape\|MaterialTheme\|Color(0x" <files>
   grep -rn "^import.*Button$\|OutlinedButton\|TextButton" <files>
   ```
3. All new tappable elements >= 48dp
4. All new tappable elements have `semantics { role = Role.Button }`
5. Back button is bottom-left on every new/changed screen
6. All CTAs in thumb zone (bottom dock or BottomEnd FABs)
7. Build: `./gradlew :app:assembleZcashtestnetFossDebug`

### After each phase (1, 2, 3, 4)

1. Install: `./gradlew :app:installZcashtestnetFossDebug`
2. Walk every changed flow manually
3. Thumb reachability: all primary actions in bottom 40% of 6" screen
4. Dark mode: all tokens resolve (no hardcoded colors)
5. Full grep check on all changed files
6. Lint: `./gradlew :ui-lib:lintZcashtestnetFossDebug`

---

## Priority & Sequencing

```
Phase 1 (Quick wins):
  1.1  Default tab → Chats
  1.2  Split attachment → clip + Z
  1.3  Rename tabs (Pay/Chats/You), remove Contacts enum
  1.4  Remove Contacts tab content, merge into You
  1.5  Rename screen header to "You"
  1.6  Restructure Pay tab FABs, demote Swap

Phase 2 (Merchant Pay):
  2.5  RecentMerchantRepository (data first)
  2.1  MerchantPayScreen
  2.2  MerchantAmountScreen
  2.3  MerchantProgressScreen
  2.6  Navigation registration
  2.7  Wire entry points
  2.4  MerchantPaymentBubble

Phase 3 (Onboarding):
  3.1  Remove MSG_INTRO
  3.2  Remove WALLET_INTRO
  3.3  Move Tor to settings
  3.4  Defer seed phrase + backup banner
  3.5  Simplify restore flow

Phase 4 (Unified Contacts):
  4.1  ZappContact entity
  4.2  Migrate ChatContactsRepository
  4.3  Migrate AddressBookRepository
  4.4  Update pickers
  4.5  Pay → Chat connection
```
