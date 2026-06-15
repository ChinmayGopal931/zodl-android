# Push notifications — self-hosted UnifiedPush (no Google)

**Status:** **M1 (local notifications) is IMPLEMENTED** — branch `feat/m1-chat-notifications`, verified on real hardware 2026-06-14 (phone → emulator posts a correct notification; backgrounded-phone non-delivery confirmed = the M2 gap). The background-wake mechanism itself was proven end-to-end on real hardware (2026-06-15). **M2 (the self-hosted UnifiedPush wake) is the next milestone** — see §7 for the exact M1 seams it must reuse. The production gating item remains blind-peer transport reliability (§9 / M3), not the wake.

This doc is self-contained: a fresh engineer should be able to read it, navigate the code via the references, re-run the verification, and implement. Paths are repo-relative to `zodl-android` unless noted. Sibling repos: `../zappMessaging` (the JS messaging engine + Android SDK bridge), `~/dev/zapp/blind-peer` (the blind-peer server library).

---

## 1. Problem

The app posts **no** Android notifications — no `NotificationManager`, `NotificationChannel`, FCM, or background service (verified: zero matches). It only receives messages while **foregrounded**: when the app is backgrounded the BareKit worklet's blind-peer connection suspends and nothing is pulled until the app is reopened (pull-on-resume). So a user only discovers new messages by opening the app.

**Constraint:** no Google / FCM. Keep it P2P / self-hosted.

---

## 2. Architecture primer (how messaging works today)

- The messaging engine is a **BareKit (Holepunch) JS worklet** bundled from `../zappMessaging/core` into `android/src/main/assets/worklet.bundle`. It runs **in the app process** (no separate service).
- Kotlin talks to it over an **IPC bridge** (NDJSON framing). `ZappMessagingSDK` is the public Kotlin API; `BareWorkletManager` owns the worklet lifecycle; `IPCBridge` does request/response + events.
- Transport is **Hyperswarm/Hypercore DHT** for direct P2P, plus a **blind peer** (a store-and-forward relay on a VPS) for offline delivery. Blind-peer config is compiled into `BuildConfig` (`BLIND_PEER_KEYS`, `BLIND_PEER_BOOTSTRAP`) and passed to the worklet as `--blind-peer-keys` / `--bootstrap-nodes` argv.
- **The engine is lazy.** `ZappMessagingSDK` and `ChatBootstrap` are plain Koin `single{}` (not `createdAtStart`), and `ZcashApplication.onCreate()` does **not** inject `ChatBootstrap`. The worklet only boots when a chat screen first injects `ChatBootstrap`, whose `init{}` calls `sdk.initialize(application)`. → A headless process start does **not** boot messaging unless something explicitly injects/initializes it.
- Incoming messages: worklet → IPC `message.received` event → `ZappMessagingSDK._messageReceived` (a `SharedFlow`) → collected by `ChatBootstrap` (unread) and `ChatConversationsRepositoryImpl` (chat-list preview). Nothing alerts the user.

Blind peer (VPS, Oracle Ubuntu @ `140.245.193.100:49737`, pubkey `5ccrwsg…eps5o`): runs `blind-peer-cli` (`bin.js`) wrapping the `blind-peer` library. It is a Hypercore replicator that is "blind" to content but tracks a per-recipient `referrer` per core and emits a server-side `core-append` event — i.e. it can already detect "new data for recipient X". Deployment: `.git-side-work/blind-peer-vps/blind-peer.service` → `node_modules/.bin/blind-peer --storage … --port 49737 …`.

---

## 3. What we verified (real devices)

Devices: OnePlus CPH2747 (Android 16) + `Medium_Phone_API_36.1` emulator, both `xyz.justzappit.zapp.foss.debug` versionCode 595.

1. **Backgrounded phone receives nothing.** Worklet stays alive (process not killed; Kotlin `BareWorkletManager.suspend()` is never called) but the blind-peer connection drops; **0 inbound IPC over minutes**, then a flood on foreground. (BareKit's *internal* `Bare.on('suspend')` + OS/Doze/NAT, not a Kotlin suspend, is the cause.)
2. **Only real devices suspend.** The **emulator does not** (no Doze/bg restrictions) — it keeps receiving while backgrounded. So the **phone is the only meaningful wake-test target**; the emulator gives false positives.
3. **The wake works, end-to-end, on the phone.** Background broadcast → `ZappMessagingSDK.resume()` → log `Worklet resumed` → reconnect → pull. A message sent from the emulator to the **backgrounded** phone was delivered `fromMe=false` ~6s after the wake, **with the app never foregrounded**. Timeline was airtight: last inbound 33s before the wake, then silence, then inbound only after `resume()`.
4. **SDK hooks already exist** (none currently called) — see §6 table: `resume()`, `suspend()`, `refreshConversations()`, `getMessages()`, `initialize()`, `conversations` flow.
5. **Cold-start caveat.** A wake to a dead process finds the SDK singleton constructed but **not initialized** (ChatBootstrap is lazy) → logs "SDK is not initialized". The real wake must call `sdk.initialize(appContext)` first.
6. **Transport reliability is the real blocker.** The blind-peer/DHT link degrades after ~20–30 min on residential Wi-Fi (`DHT degraded` in the chat header / `Waiting for peer` on the sender). The wake only helps if the blind peer is reachable when it fires. **A fresh restart of both apps restores connectivity** for a few minutes.
7. **Matched builds are mandatory.** Every early "no delivery" was a confound: the emulator was sending from a **7-week-old build** (versionCode 1678 vs current 595) whose messaging stack couldn't interoperate → permanent "Waiting for peer". Re-onboarding both ends on the *same current build* is what finally produced the clean pass.
8. **JS worklet console does not reach logcat** even with `ZAPP_MESSAGING_LOG_LEVEL=debug`. Only Kotlin tags surface: `BareWorkletManager`, `IPCBridge`, `ZappMessagingSDK`. Usable signals: `BareWorkletManager: IPC data received from worklet: N bytes` (inbound), `Worklet started` / `Worklet resumed` (lifecycle).
9. **Blind-peer server already supports targeted wakes** — per-recipient `referrer`, `core-append` event, `cores-by-referrer` index, `adminRouter` RPC, and an in-band `announceToReferrer`/`WakeupHandler`. The **client never sets `referrer`** today (see §6).

Platform: `minSdk 27`, `targetSdk 35`, `compileSdk 36` (`gradle.properties:190-192`).

---

## 4. Design — the "doorbell"

The push is a doorbell, not a mail carrier: it carries **no message content**. The blind peer pings the device; the app wakes, pulls the real message from its own E2E store, posts a **local** notification. (Same shape as Molly/MollySocket.)

```
sender appends msg → replicates to blind-peer VPS
   → server core-append fires → reads core.referrer (= recipient identity)
   → POST encrypted "ping" to recipient's ntfy endpoint   (debounced ~5s/referrer)
ntfy server → UnifiedPush distributor (ntfy app, one device-wide connection)
   → broadcast Intent to Zapp (org.unifiedpush.android.connector.MESSAGE)
   → wake foreground service: initialize() if cold → resume() → pull → post local notification → stop
```

What each party sees: the on-device distributor + your own ntfy server see only an opaque ping to topic `upXXXX` at time T. The VPS already knew "core got data". Content stays E2E. New trust surface = your own ntfy server + the distributor app the user installs.

### Decisions

| Decision | Choice | Why |
|---|---|---|
| Doorbell vs payload push | **Doorbell** | Blind peer is content-blind; message already replicates. No payload crypto on the VPS. |
| Endpoint registration channel | **Existing Noise-authenticated blind-peer RPC** (`rpc.respond`/`adminRouter.method`) | Already keyed to identity; no new auth/surface. |
| Lookup key | **Recipient identity pubkey = `referrer`**; groups = later | Matches the existing `referrer` mechanism. |
| Wake work window | **shortService foreground service** | ~3-min cap covers the ~13–15s cold start + replication; no Play special-use review. Started inside the UnifiedPush 5s foreground grant. |
| Notification hook | **One shared path** for foreground + woken | `ChatConversationsRepositoryImpl.observeConversationEvents()` already has `(conversationId, msg)` + `activeConversationId` + `isBlocked`. |
| Distributor / push server | **ntfy** self-hosted on the VPS, **≥ 2.12.0** (Web-Push UnifiedPush path) | De-Googled, self-hostable. |

---

## 5. Reproduction recipe (rebuild + re-verify the wake)

The spike that proved this was deleted (branch `spike/m0-resume-wake`, never merged). To reproduce:

1. **Two devices on the SAME current build.** Build/install: `./gradlew :app:installZcashmainnetFossDebug` (→ `xyz.justzappit.zapp.foss.debug` on every connected device). Confirm both `versionName/versionCode` match (`adb shell dumpsys package <pkg> | grep version`). A stale sender = "Waiting for peer", no delivery. (Force a downgrade install with `adb install -r -d <apk>` if `INSTALL_FAILED_VERSION_DOWNGRADE`; old data may crash → `pm clear` + re-onboard.)
2. Onboard both, pair them (each is a chat contact of the other), confirm a message flows when both are foreground (chat header shows `P2P connected`).
3. Throwaway debug `BroadcastReceiver` in `ui-lib` that calls `sdk.resume()` (+ optional `refreshConversations()`/`getMessages()` to log the pull). Register in the `ui-lib` manifest, `android:exported="true"`. Trigger: `adb shell am broadcast -n <pkg>/<receiver>`. The proven receiver body is in §7.
4. **Test (phone = receiver, it suspends):** launch phone app → background it (`adb shell input keyevent KEYCODE_HOME`) → send from the emulator → confirm phone has 0 inbound (`adb logcat … | grep 'IPC data received from worklet'`) → fire the broadcast → confirm `Worklet resumed` + the message arrives `fromMe=false` while the phone stays backgrounded (`adb shell dumpsys activity activities | grep topResumedActivity` shows the launcher throughout).
5. Optional deeper visibility: set `ZAPP_MESSAGING_LOG_LEVEL=debug` in `local.properties`, rebuild. (Note: only enables more *Kotlin* signal; JS console still doesn't reach logcat. Revert before release — it ships keypair dumps into `BuildConfig`.)

Build hygiene: after editing `../zappMessaging` JS, run `npm run build:android` there before `./gradlew`. Don't bump the SDK pin (`gradle.properties:183 SDK_INCLUDED_BUILD_PATH`, `.zapp-deps`). Load `.claude/skills/zodl-style/SKILL.md` before writing `.kt`. Permissions/receivers/services go in `ui-lib/src/main/AndroidManifest.xml`.

---

## 6. Reference — verified code locations

### zappMessaging SDK + bridge (`../zappMessaging/android/...`)

| Symbol | Location | Note |
|---|---|---|
| `ZappMessagingSDK.initialize(context)` | `ZappMessagingSDK.kt:136-187` | `suspend`; boots worklet. Call on cold wake. |
| `ZappMessagingSDK.resume()` | `ZappMessagingSDK.kt:697-706` | **The wake hook.** `workletManager.resume()` + refreshes connection status. Currently uncalled. |
| `ZappMessagingSDK.suspend()` | `ZappMessagingSDK.kt:690-692` | Currently uncalled. |
| `ZappMessagingSDK.refreshConversations()` | `ZappMessagingSDK.kt:327-333` | `suspend`; IPC `conversation.list` → updates `conversations`. |
| `ZappMessagingSDK.getMessages(convId, limit=50)` | `ZappMessagingSDK.kt:431-440` | `suspend`; pure disk read (IPC `message.list`). |
| `ZappMessagingSDK.conversations` | `ZappMessagingSDK.kt:51-53` | `StateFlow<List<ZMConversation>>`. |
| `ZappMessagingSDK.messageReceived` | `ZappMessagingSDK.kt:77-79` | `SharedFlow<Pair<convId, ZMMessage>>`, no replay (subscribe before emit). |
| `message.received` handler | `ZappMessagingSDK.kt:710-845` (case `713-722`) | emits `_messageReceived`. |
| `ZappMessagingSDK.shutdown()` | `ZappMessagingSDK.kt:192-198` | Tear down on wake-service exit. |
| `BareWorkletManager` tag / `start()` | `BareWorkletManager.kt:302` / `:39-134` | log tag `BareWorkletManager`. |
| argv wiring (`--blind-peer-keys`/`--bootstrap-nodes`/`--log-level`) | `BareWorkletManager.kt:76-96` | from `BuildConfig`. |
| `BareWorkletManager.suspend()/resume()` | `:180-188` / `:208-216` | call `worklet?.suspend()/resume()`. |
| inbound log | `BareWorkletManager.kt:117` | `Log.d(TAG, "IPC data received from worklet: ${bytes.size} bytes")`. |
| `IPCBridge.sendRequest()/onEvent()` | `IPCBridge.kt:55-86` / `:186-190` | 30s default timeout. |
| `ZMMessage` | `models/ZMMessage.kt:10-47` | `content` (20), `isFromMe` (26), `senderName?` (18), `timestamp` (24), `conversationId` (14), `senderId`. |
| `ZMConversation` | `models/ZMConversation.kt:10-37` | `id` (12), `displayName` (26), `lastMessage?` (28). |
| log-level default / wiring | `core/lib/config.js:77` / `android/build.gradle.kts:51` | `getArg('log-level') || 'info'`. |

**Gap:** no `registerPushEndpoint`/push method exists yet — to be added (§7 M2).

### App integration (`zodl-android`)

| Symbol | Location | Note |
|---|---|---|
| `ChatBootstrap` Koin def (lazy `single`) | `di/ZappMessagingModule.kt:19` | not `createdAtStart` → worklet is lazy. |
| `ChatBootstrap.init{}` (boots worklet) | `…/chat/common/ChatBootstrap.kt:64-76` | `sdk.initialize` + `observeMessagesForUnread` + derive observer. |
| `observeMessagesForUnread()` (+ `isBlocked`) | `ChatBootstrap.kt:150-158` | filters `isFromMe` + blocked. |
| `derive()` (identity from seed) | `ChatBootstrap.kt:130-148` | `sdk.restoreFromSeedPhrase`. |
| `ZcashApplication.onCreate` modules / eager inits | `app/…/ZcashApplication.kt:65-81` / `87-92` | **ChatBootstrap not eagerly injected.** Add a new module to the `modules(...)` list. |
| Koin module (where messaging is wired) | `di/ZappMessagingModule.kt:14-20` | put a `NotificationsModule` / helper binding here or a sibling `di/*Module.kt`. |
| **Notification hook** `observeConversationEvents()` | `…/chat/repository/ChatConversationsRepositoryImpl.kt:101-128` | has `conversationId`, `msg.content`, `msg.timestamp`, `msg.isFromMe`, `isBlocked`, `activeConversationId`. Conversation `displayName` for the title. |
| `activeConversationId` / `setActiveConversation` | `ChatConversationsRepositoryImpl.kt:42,70-72` | on-screen suppression. |
| `ChatModerationRepository.isBlocked(pubKey)` | `…/chat/repository/ChatModerationRepository.kt:13` | respect in the notifier. |
| `ChatSettingsState` (add `NotificationMode`) | `…/chat/settings/ChatSettingsState.kt:7-24` | has `connectionStatus`/`dhtHealth`/`peerCount`. |
| `ChatSettingsVM` / `ChatSettingsView` | `…/chat/settings/ChatSettingsVM.kt:29-80` / `…/chat/view/ChatSettingsView.kt:56-125` | `ZappGroupHeader` sections; add a "Notifications" group. |
| chat strings (+ es) | `res/ui/chat/values/strings.xml:149-182` / `values-es/strings.xml` | mirror new strings. |
| `ui-lib` manifest (perms / `<application>`) | `ui-lib/src/main/AndroidManifest.xml:1-42` | add `POST_NOTIFICATIONS`, the UnifiedPush `<receiver>`, the wake `<service>`. |
| SDK levels / blind-peer defaults | `gradle.properties:190-192` / `:75,79` | 27/35/36; keys + `140.245.193.100:49737`. |

### Blind-peer server (lib `~/dev/zapp/blind-peer`; deployed CLI `.git-side-work/blind-peer-vps/node_modules/blind-peer-cli`)

| Symbol | Location | Note |
|---|---|---|
| `referrer` on core record | `blind-peer/lib/db.js:106-121` | `referrer: info.referrer || null`. |
| `cores-by-referrer` index | `blind-peer-encodings/spec/hyperdb/db.json:44-68` | `find('@blind-peer/cores-by-referrer', {gte/lte:{referrer}})`. |
| **`core-append` emit** | `blind-peer/index.js:626-634` | `this.emit('core-append', core)` on new blocks. |
| **`core-append` listener (webhook injection point)** | `…/blind-peer-cli/bin.js:221-223` | add the ntfy POST here. |
| `CoreTracker.record` (has `referrer`) | `blind-peer/index.js:34-112` | available in the handler. |
| `announceToReferrer()` (in-band wakeup) | `blind-peer/index.js:113-138` | existing P2P wakeup; coexists with webhook. |
| `WakeupHandler.onpeeractive` | `blind-peer/index.js:171-187` | queries `cores-by-referrer`. |
| **RPC registration patterns** | `blind-peer/index.js:278-293` (`adminRouter.method`) / `509-516` (`rpc.respond`) | add `register-push-endpoint` here. |
| CLI constructor (add `webhookUrl` opt) | `…/blind-peer-cli/bin.js:1-120` | `new BlindPeer(storage, {…})`. |
| deployment | `.git-side-work/blind-peer-vps/blind-peer.service:10` | systemd `ExecStart`. |

### Blind-peer client (`../zappMessaging/core`)

| Symbol | Location | Note |
|---|---|---|
| `addCoreBackground(core, key, {announce:true})` — **no referrer** | `core/lib/blind-mirror.js:417` | must add `referrer: recipientKey`. |
| `addRemoteCore` registration — no referrer | `core/lib/blind-mirror.js:439` | same. |
| `addCoreBackground` supports `referrer` | `node_modules/blind-peering/index.js:85-100` | option already plumbed. |
| recipient key sources | `core/lib/p2p-manager.js:422` (`addLocalCore`), `:438-439` (`addRemoteCore`, `peerKeyHex`), `:456` (`peerToConversation`) | resolve recipient pubkey here. |
| identity pubkey (stable referrer key) | `core/lib/identity.js:27-45` | `keyPair.publicKey` / `publicKeyHex`. |

---

## 7. Implementation plan

### M1 — Local notifications — ✅ DONE (branch `feat/m1-chat-notifications`)
Implemented and verified on real hardware. **M2 MUST reuse these exact seams — do not build a parallel notification path.** All paths are in `ui-lib`.

- **`ChatNotifier`** — `…/ui/common/provider/ChatNotifier.kt`. Interface `post(conversationId, conversationName: String?, senderName: String?, content: String)`. Creates the `chat_messages` channel (`IMPORTANCE_HIGH`, `CATEGORY_MESSAGE`), posts **tag-based** `notify(conversationId, NOTIFICATION_ID=1, …)` (collision-free per conversation), small icon `R.drawable.ic_notification_chat` (a real 24dp alpha glyph — NOT `ic_launcher_monochrome`), deep-link PendingIntent → `MainActivity`. Koin: `singleOf(::ChatNotifierImpl) bind ChatNotifier::class` in `di/ProviderModule.kt` (no new module — the original "new di/*Module.kt" plan was unnecessary). **M2's `ChatWakeService` calls `chatNotifier.post(...)` after pulling.**
- **Single notify decision = `ChatConversationsRepositoryImpl.maybeNotify(conversationId, msg, isViewingConversation)`** — called from the `sdk.messageReceived` collector in `observeConversationEvents()`. Gates: `msg.isFromMe`, on-screen conversation (`activeConversationId`), and the `notificationsEnabled` toggle (nullable StateFlow; suppresses until the persisted value loads). Blocked senders are already filtered one line up by `moderationRepository.isBlocked`. **This is the M2 seam:** once the wake calls `sdk.resume()` and replication lands, pulled messages re-emit on `messageReceived` → `maybeNotify` fires automatically. (If a given pull path does NOT re-emit `messageReceived`, the wake service should call `chatNotifier.post(...)` directly — same presentation seam.)
- **Deep-link** — `CHAT_CONVERSATION_ID_EXTRA` (const in `ChatNotifier.kt`) on an explicit intent to the **non-exported** `MainActivity`; `onCreate`/`onNewIntent` forward it via `navigationRouter.forward(ChatRoomArgs(conversationId))`.
- **Permission** — `POST_NOTIFICATIONS` in `ui-lib/src/main/AndroidManifest.xml`; requested by `RequestNotificationPermissionEffect()` (`…/ui/screen/chat/common/RequestNotificationPermission.kt`) hosted in `ChatListScreen` (Android 13+; no-op below). M2 may also request earlier (first launch).
- **Toggle** — `StandardPreferenceKeys.IS_CHAT_NOTIFICATIONS_ENABLED` (default `true`); UI in the `ChatSettings{State,VM,View}` triad. **M2 extends this to `NotificationMode { Push, LocalOnly, Off }`** (replace the boolean; `maybeNotify` reads it for the LocalOnly/Off gating, the wake registrar reads it for Push).
- **Strings** — `chat_notifications_*` + `chat_settings_section_notifications` in `res/ui/chat/values{,-es}/strings.xml` (mirror any new M2 strings to both locales).

### M1.5 — what M2 inherits (quick orientation for a fresh context)
A wake (UnifiedPush MESSAGE broadcast) must, on the receiver: `sdk.initialize(appContext)` if the process is cold (ChatBootstrap is lazy → singleton otherwise uninitialized), then `sdk.resume()` (→ `BareWorkletManager.resume()` reconnect), await replication, and let messages flow through `maybeNotify`/`ChatNotifier` (above). The resume→reconnect→pull path is proven (§3, §5). Do this inside a `shortService` FGS started within the UnifiedPush 5s foreground grant. SDK hooks + line refs: §6.

### M2 — Self-hosted UnifiedPush wake (end-to-end) — NEXT

**VPS** (`~/dev/zapp/blind-peer` + `.git-side-work/blind-peer-vps`):
- Stand up **ntfy ≥ 2.12** behind TLS; grant anonymous write to UP topics: `ntfy access '*' 'up*' write-only`.
- Add a `register-push-endpoint` RPC (pattern: `blind-peer/index.js:278-293`/`509-516`), authenticated as the caller identity → store `{referrer, endpoint, p256dh, auth, ttl}` (RocksDB).
- In the `core-append` handler (`bin.js:221`): read `core` → `CoreTracker.record.referrer` → look up endpoints → Web-Push-encrypt a constant ping → POST to each (debounce ~5s/referrer, fire-and-forget + backoff). Lazy TTL prune.
- Update `blind-peer.service` to run the wrapper (the `referrer`/`core-append`/`adminRouter` primitives already exist upstream — verify `adminRouter` is reachable from the CLI; otherwise a thin custom entry that imports `blind-peer`).

**JS engine** (`../zappMessaging/core`):
- `blind-mirror.js:417,439`: pass `referrer: recipientKey` (resolve via `p2p-manager.js` `peerToConversation`/`peerKeyHex`; recipient identity pubkey from `identity.js`). Direct chats only; groups later.
- New IPC `push.register_endpoint` → call the `register-push-endpoint` RPC over the existing Noise connection.
- Expose `sdk.registerPushEndpoint(endpoint, keys)`. Then `npm run build:android`.

**Android** (`ui-lib`):
- Add `org.unifiedpush.android:connector`. Declare the UP `MessagingReceiver` + a `shortService` foreground service (`FOREGROUND_SERVICE`) in the `ui-lib` manifest.
- `ChatWakeService` (short FGS, started during the UP 5s foreground grant): `sdk.initialize()` if cold → `sdk.resume()` → await replication (collect `messageReceived` or bounded `getMessages` poll) → `ChatNotifier.post(...)` → `sdk.shutdown()` / `stopSelf()`.
- `UnifiedPushRegistrar`: after identity is ready (in `ChatBootstrap`), register with the distributor; on `NEW_ENDPOINT`, call `sdk.registerPushEndpoint`.
- Settings: `NotificationMode { Push, LocalOnly, Off }` in `ChatSettingsState`; distributor picker (skip if one); no-distributor fallback messaging.

### M3 — Reliability + polish (the real blocker)
- **Harden blind-peer transport** so a woken device reliably reaches the blind peer and pulls. The VPS is already a DHT bootstrap node (`BLIND_PEER_BOOTSTRAP`), yet delivery still fails when the recipient backgrounds and the DHT thins — investigate making the blind peer an always-reachable relay independent of general DHT health. **Gates production notifications; helps messaging overall.**
- No-distributor fallback: periodic `WorkManager` pull (≥15-min floor) for users without ntfy.
- Groups (multi-recipient `referrer`), multi-device (set of endpoints per identity, TTL'd).

### Proven wake-handler reference (from the deleted spike)

```kotlin
// In ChatWakeService (real) / a debug BroadcastReceiver (spike), Main thread (IPC has main-thread affinity):
sdk.resume()                          // BareWorkletManager.resume() → reconnect blind peer
CoroutineScope(Dispatchers.Main).launch {
    sdk.refreshConversations()
    delay(6_000)                      // let blind-peer replication land on disk
    sdk.conversations.value.forEach { c ->
        val latest = sdk.getMessages(c.id, limit = 3).maxByOrNull { it.timestamp }
        // latest.isFromMe == false && recent ts  → new message pulled → ChatNotifier.post(c, latest)
    }
}
// Cold process: call sdk.initialize(appContext) FIRST (ChatBootstrap is lazy; singleton otherwise uninitialized).
```

---

## 8. External facts (decision-critical)

**UnifiedPush / ntfy:** a single **distributor app** (ntfy/NextPush) holds one device-wide connection; UP-enabled apps do **not** each run a foreground service. The app exposes a `BroadcastReceiver` for `org.unifiedpush.android.connector.NEW_ENDPOINT` / `.MESSAGE`; on delivery the distributor raises the app to foreground importance for **5s** (enough to start a FGS). Content is Web-Push (RFC 8030/8291, `aes128gcm`) so the push server/distributor see only ciphertext + metadata (topic, time, size). ntfy is self-hostable as both push server and distributor; the encrypted UP path needs **ntfy ≥ 2.12.0**. The end user **must install a distributor** (none ships on stock/de-Googled Android) → ship a fallback (embedded FCM distributor *(reintroduces Google)*, or polling) or require ntfy (Molly's posture). Connector lib: `org.unifiedpush.android:connector`.

**Android platform:** `WorkManager` periodic **minimum 15 min** (clamped), paused by Doze → not near-real-time. Foreground services on 14+ must declare `foregroundServiceType` + the matching permission; **`shortService`** needs only `FOREGROUND_SERVICE`, ~3-min cap, no Play review (use this for the wake); **`dataSync`** is capped 6h/24h on Android 15; **`specialUse`** needs a Play Console justification. Starting a FGS from the background is blocked on 12+ **except** within the UP 5s grant (or high-priority FCM / `BOOT_COMPLETED` / battery-opt exemption). `POST_NOTIFICATIONS` is a runtime permission on 13+. Play policy forbids `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for messengers that could use FCM — irrelevant here (we don't), but note it if ever adding a persistent FGS.

**Comparators (precedent for no-Google):** SimpleX (foreground-service "instant" / 20-min "periodic" / off), Briar (FGS + Tor wake-lock), **Molly** (UnifiedPush via MollySocket gateway — the closest analog to blind-peer + wake), Session (FCM or slow-poll, no UP), Status (Waku node in a FGS).

---

## 9. Open risks / failure modes

1. **Transport reliability (#1)** — see M3. The wake is proven; delivery is only as good as blind-peer reachability at wake time. DHT degrades after ~20–30 min on residential Wi-Fi.
2. **Distributor requirement** — users must install ntfy; ship the fallback + clear UX.
3. **Test confounds we hit** (avoid re-deriving): stale sender build → "Waiting for peer"; emulator doesn't suspend → false positive (test on a real phone); JS console invisible in logcat → use the Kotlin tags.
4. **versionCode hygiene** — debug `versionCode` 595 is *lower* than an older build's 1678, blocking update installs (`INSTALL_FAILED_VERSION_DOWNGRADE`). Unrelated to notifications but worth fixing for release sanity.
