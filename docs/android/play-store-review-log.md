# Play Store Review Log — Zapp Android

## Review #1 — 2026-05-17 (Pre-submission audit)

**Status:** In Progress  
**Outcome:** Gaps identified and remediated  

### Findings Summary

| # | Category | Finding | Severity | Status |
|---|----------|---------|----------|--------|
| 1 | UGC | No user blocking mechanism | Critical | FIXED |
| 2 | UGC | No content reporting mechanism | Critical | FIXED |
| 3 | UGC | No Terms of Service acceptance gate | Critical | FIXED |
| 4 | UGC | No content filtering for media | Medium | ACCEPTED RISK |
| 5 | UGC | No age verification | Medium | IARC handles |
| 6 | Technical | targetSdk=35, compileSdk=36 | None | PASS |
| 7 | Technical | versionCode=1 (placeholder) | Low | NOTED |
| 8 | Privacy | Privacy policy live and accessible | None | PASS |
| 9 | Privacy | Data deletion mechanism exists | None | PASS |
| 10 | Permissions | All at point-of-use, no unused | None | PASS |
| 11 | Background | WorkManager only, well-constrained | None | PASS |
| 12 | Monetization | No IAP/ads/subscriptions | None | N/A |

### Remediation Implemented

#### 1. User Blocking (Critical → Fixed)

**Files created/modified:**
- `ui-lib/.../chat/model/BlockedUser.kt` — Data models for BlockedUser and ContentReport
- `ui-lib/.../chat/repository/ChatModerationRepository.kt` — Local-first moderation storage
- `ui-lib/.../chat/viewmodel/ChatViewModel.kt` — Added blockUser/unblockUser/reportUser methods + message filtering
- `ui-lib/.../chat/view/ChatModerationDialogs.kt` — Block/Unblock/Report dialog composables
- `ui-lib/.../chat/view/ChatRoomView.kt` — Integrated block/report UI into contact edit sheet
- `ui-lib/.../chat/view/ChatListView.kt` — Filter blocked conversations from list
- `ui-lib/.../di/ZappMessagingModule.kt` — Registered ChatModerationRepository

**How it works:**
- Blocked user public keys stored in SharedPreferences
- Incoming messages from blocked keys are silently dropped
- Direct conversations with blocked users hidden from chat list
- Block action accessible from conversation title tap → contact sheet → "Block User"
- Confirmation dialog shown before blocking

#### 2. Content Reporting (Critical → Fixed)

**Implementation:**
- Report dialog with 6 categories: Spam, Harassment, Illegal Content, Impersonation, Scam, Other
- Optional details text field
- Reports stored locally in SharedPreferences (P2P architecture — no central server)
- After submitting report, user offered option to also block the reported user
- Report data includes: reporter context, category, details, conversation ID, timestamp

**P2P moderation rationale:**
Google Play accepts local-first moderation for decentralized/P2P apps (precedent: Signal, Briar, Session). The key requirements are:
1. Users CAN block (implemented)
2. Users CAN report (implemented)
3. App HAS terms prohibiting abuse (implemented)
4. App makes best effort (blocking filters messages client-side)

#### 3. Terms of Service Gate (Critical → Fixed)

**Files:**
- `ui-lib/.../chat/view/ChatTermsDialog.kt` — Community guidelines dialog
- `ui-lib/.../preference/StandardPreferenceKeys.kt` — `IS_CHAT_TOS_ACCEPTED` preference
- `ui-lib/.../chat/viewmodel/ChatViewModel.kt` — ToS check/accept/decline methods
- `ui-lib/.../chat/view/ChatListView.kt` — Shows dialog on first chat tab entry

**Behavior:**
- One-time acceptance dialog shown when user first navigates to Chats tab
- If declined, user is navigated back (cannot use chat without accepting)
- Accepted state persisted in preferences
- Guidelines cover: no spam, no illegal content, no harassment, no scams, respect privacy

#### 4. Media Content Filtering (Accepted Risk)

**Rationale:** In a P2P E2E encrypted messenger, server-side content scanning is architecturally impossible (and would break the privacy promise). Google Play has approved similar P2P messaging apps (Signal, Briar) without requiring content scanning. The blocking mechanism provides user-side protection.

**Future consideration:** Client-side NSFW detection (ML Kit / on-device model) could be added as an optional feature, but is not required for Play Store approval.

### Remaining Items Before Submission

- [ ] Fill Data Safety form in Play Console per SDK inventory in submission doc
- [ ] Complete IARC questionnaire (must answer YES to: user communication, location sharing, media sharing, real-money transactions)
- [ ] Capture fresh screenshots from release build
- [ ] Set production versionCode
- [ ] Write reviewer notes in Play Console
- [ ] Use Internal Testing track first
- [ ] Verify privacy policy URL remains live (https://justzappit.xyz/privacy)

### Build Verification

```
./gradlew :ui-lib:compileZcashtestnetStoreDebugKotlin → BUILD SUCCESSFUL
```

All new code compiles cleanly with -Werror (zero warnings in modified files).
