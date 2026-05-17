# Blind Peer Wire-Up & Relay Investigation

> **Purpose**: Onboarding doc for whoever picks up this debugging next.
> Reading this top-to-bottom should be enough to reproduce the test setup,
> understand what works / what doesn't, and pick the right next step
> without re-deriving anything.

> **Companion branch in `../zappMessaging`**: same name
> (`debug/blind-peer-relay-investigation`). The wiring code lives in that
> repo; this repo only carries this doc + the gitignored `local.properties`
> entry.

> **Sessions in this doc**: 2026-05-13 (initial wire-up + cross-NAT test),
> 2026-05-14 (re-verified wiring + diagnosed why "phone1 works but
> phone2 doesn't" + concrete local-test plan via home-router port-forward),
> 2026-05-15 (production VPS deployed; phone2 keyspace-luck hypothesis
> disproven), and 2026-05-16 (AP isolation on home Wi-Fi exposed as a
> blocker for phone↔phone LAN bootstrap + new instrumentation isolating
> PEER_NOT_FOUND to fresh-findPeer-returning-0-on-both-phones).

---

## TL;DR (state as of 2026-05-16)

**Session 3 outcome**: Production blind-peer is now live on an Oracle Cloud
Always-Free VPS (Ubuntu 24.04, `140.245.193.100`, systemd-managed, pubkey
`5ccrwsgqfg1hawwcbckmisww4sy3qns5scsntxfztgx7pt4eps5o`). Phone1 replicates
its conversation cores reliably to the VPS. The path from `local.properties`
→ `BuildConfig.BLIND_PEER_KEYS` → worklet `argv` → `BlindMirror` → live VPS
connection is end-to-end working on a fresh install.

**Session 3+ debugging (2026-05-16) added three big diagnostic results,
each of which narrows the search for the actual fix:**

1. **Phone↔phone direct LAN connection is blocked by AP isolation on the
   home Wi-Fi.** ICMP ping between phone1 (192.168.0.247) and phone2
   (192.168.0.163) returns `Destination Host Unreachable` despite both
   being on `/24`. Mobile hotspot fallback didn't help (Android's hotspot
   has its own client isolation by default). This is the root cause of
   `conns=0 inviteTopics=1` SWARM state: both phones can't see each
   other's Hyperswarm broadcasts. **Fix is router-side or network-side,
   not code-side**. See Finding #19.
2. **`PEER_NOT_FOUND` originates in hyperdht/connect.js:382** when
   `dht.findPeer()` returns zero responders for the target key. Confirmed
   by adding `_probeFindPeer()` to BlindMirror that runs `findPeer(blindPeerKey)`
   every 20s and logs responder count. **Fresh findPeer returns 0
   responders on BOTH phones** — including phone1 *while it has a live
   `connected=true rpc=true` session to the VPS*. So phone1's connection
   uses a different code path than fresh findPeer (most likely
   `_socketPool.routes` cache populated during some other DHT activity
   we haven't isolated yet). See Finding #20.
3. **Both phones are in the same conversation `dm_3ddc00c17`** with
   distinct local cores (phone1=`f875ed4f3357`, phone2=`8bd3c243b824`).
   Phone1's core appends reach VPS (8 blocks observed mirrored). Phone2
   never receives them because (a) phone2 can't reach VPS, (b) AP
   isolation blocks direct phone↔phone fallback, (c) even if phone2
   could reach VPS, the trust-gate in Finding #11 means the blind-peer
   won't republish phone1's discovery key (we see "Downgraded announce
   for peer ... because the peer is not trusted" in `/var/log/blind-peer.log`
   on every connect). See Finding #21.

**The user-facing symptom "I texted but it doesn't work" therefore has THREE
overlapping causes**, any one of which would be enough to break delivery:

- Network: AP isolation prevents phone↔phone direct sync (Finding #19)
- Device: phone2 can't reach VPS for unknown reason (Finding #17, #20)
- Code: trust-gate prevents blind-peer relay even when both connected (Finding #11)

1. **Wiring works end-to-end. Confirmed twice now.** `local.properties` →
   `BuildConfig.BLIND_PEER_KEYS` → worklet `argv` → `core/lib/config.js` →
   `BlindMirror` → `BlindPeering` → `BlindPeerClient.dht.connect(...)`.
   Phone1 (66683dca…) sustained `connected=true rpc=true` for 9+ minutes
   straight and replicated 18 Hypercore blocks to the Mac blind-peer (15
   from previous sessions plus 3 new appends). Logged in our new
   `blind-mirror-diag.log` STATE dump and in `/tmp/blind-peer.log`.
2. **The remaining blocker is host reachability, not code.** The Mac is
   behind residential NAT. The DHT can't announce a stable public address
   for it. From a fresh DHT client (even on the Mac itself!) `findPeer(blindPeerKey)`
   returns 0 responders. Phone1 succeeds via lucky DHT-keyspace placement
   or a cached route; phone2 — with a different ephemeral DHT keypair —
   never finds the blind-peer for ~10 minutes of `PEER_NOT_FOUND` retries.
   Same code, same network, same time — different DHT-keyspace luck.
3. **WireGuard on the Mac silently breaks everything.** Re-confirmed
   Finding #8 from session 1: when WG is on, the blind-peer announces the
   WG provider's exit IP (e.g. `34.0.35.179` in Google Cloud) as its
   public address. Lookups return that GCP IP, phones try to send UDP
   there, the GCP edge has no NAT rule and drops the packet. **Always
   disable WG before starting the blind-peer for testing.**
4. **The deterministic fix for local dev** is a 3-minute home-router
   port-forward (UDP/49737 → 192.168.0.110:49737). After that, the
   blind-peer's announced public IP is directly reachable, no DHT-discovery
   roulette. Step-by-step under "Local testing setup" below.
5. **The deterministic fix for production** is a public-IP VPS (or better,
   a fleet of 2-3 of them — the Keet pattern). Step-by-step under
   "Production deployment plan" below.

---

## ▶ Resume Here (Session 5 onboarding, 2026-05-16 evening)

If you're picking up after a context clear or a new session, start here.

**The state of the world right now (2026-05-16):**

- VPS blind-peer is deployed and healthy. SSH: `ssh ubuntu@140.245.193.100`.
  systemd unit `blind-peer.service` is enabled+active.
  Logs: `tail -f /var/log/blind-peer.log`.
- `local.properties` points the app at the VPS pubkey already.
- `debug/blind-peer-relay-investigation` branch in `zodl-android` has the
  full doc; the S3 doc commit also lives on the side branch
  `debug/blind-peer-session-3-vps` on the GitHub remote (`justzappit`).
- `../zappMessaging` companion branch has S2 instrumentation + always-on
  relay patch + S4 new `_probeFindPeer` method in `core/lib/blind-mirror.js`.
  The probe-A (findPeer responder count) is live and producing data; the
  probe-B (`_socketPool.routes` cache inspection) was added in code but
  **was NOT rebuilt/redeployed yet** — needs a `npm run build:android`
  in `../zappMessaging` + a fresh APK build + reinstall before its data
  will appear.

**The user's actual problem (what kicked off S4):**

- Both phones are paired in the same conversation `dm_3ddc00c17`. Phone1
  appends messages (8 blocks observed). Phone2 doesn't receive them.
  Phone1 successfully replicates to the VPS. Phone2 can't reach the VPS
  AND phones can't reach each other directly.

**What's now confirmed working:**

- Fresh-install phone1 reaches the VPS, registers its local core, replicates
  blocks. `STATE registered=1 peers=1 connected=true rpc=true` in
  `files/zappmessaging/blind-mirror-diag.log`.
- VPS log shows phone1's messages landing (`hpzxspnofs1j` discovery key,
  block count growing as user types).

**What's now confirmed broken (in priority order for "fix the user's
problem"):**

1. **AP isolation on home Wi-Fi blocks phone↔phone LAN bootstrap**
   (Finding #19). ICMP `ping` between phones fails on same `/24`. Mobile
   hotspot fallback also fails (Android hotspot has its own client
   isolation). **This means even fixing phone2's VPS connection alone
   wouldn't fully solve the user's case** because phones can't exchange
   `__core_keys` over LAN to begin with — but they ALREADY did so at
   some point (they're in `dm_3ddc00c17` together with each other's
   cores known), so phone2 *would* be able to request phone1's core
   from the VPS if it could just reach the VPS.
2. **Phone2 → VPS still PEER_NOT_FOUND** (Finding #17, #20). New
   instrumentation showed that fresh `findPeer(blindPeerKey)` returns
   0 responders **on BOTH phones**, yet phone1 connects fine. The
   differentiator is somewhere else — most likely `_socketPool.routes`
   cache that's populated on phone1 by an early DHT side-effect we
   haven't traced. **Probe-B was added but needs rebuild+redeploy.**
3. **Trust-gate prevents blind-peer relay** (Finding #11, re-confirmed
   in VPS log on every connect: "Downgraded announce for peer ...
   because the peer is not trusted"). The blind-peer accepts cores but
   doesn't republish discovery keys on DHT, so even when both phones
   connect, the blind-peer can't bridge them at the DHT level.

**Three concrete next-step tracks (pick the one matching priority):**

A. **Fastest user-facing fix: bypass AP isolation.** Have the user disable
   AP isolation on their home router (admin panel, look for "AP Isolation"
   / "Client Isolation" / "Privacy Separator" / "Wireless Isolation").
   Once that's off, the phones may directly sync over LAN even with
   phone2's VPS connection still broken. This is the fastest path to
   "messages flow between phones" but doesn't help anyone on different
   networks.

B. **Finish the probe + diagnose phone2's `_socketPool.routes`.** Run:
   ```bash
   cd /Users/chinmaygopal/dev/zapp/zappMessaging && \
     source ~/.nvm/nvm.sh && nvm use 22 && npm run build:android
   cd /Users/chinmaygopal/dev/zapp/zodl-android && \
     ./gradlew :app:assembleZcashtestnetStoreDebug
   ```
   then install on both phones, wipe `files/bare/worklet.bundle` on each,
   force-stop + relaunch, wait ~30s for first `PROBE dht rtNodes=...
   totalRoutes=... routesByTarget=...` line in each phone's
   `files/zappmessaging/blind-mirror-diag.log`. Compare. If phone1's
   `routesByTarget[<vps>]>0` and phone2's `=0`, the next fix is to
   manually seed phone2's routes cache or fix whatever DHT-side-effect
   populates phone1's cache.

C. **Architectural fix: trust-gate + invite-via-Hypercore.** See
   Findings #11 and #18, "Wider architectural improvement" section.
   Wrap `BlindPeering._getBlindPeer` in `core/lib/blind-mirror.js` to
   pass `keyPair: identity.keyPair` so the blind-peer's trust-gate
   matches identity. Then move `sendInvite` and `__core_keys` exchange
   out of direct swarm sockets and into a Hypercore-based metadata
   channel that the blind-peer mirrors. This unblocks all of "two
   strangers globally" cleanly.

**Don't waste time on (already ruled out by S3 + S4):**

- Phone2's "DHT keyspace luck" — disproven across 4+ ephemerals.
- "VPS isn't reachable" — phone1 connects to it from the same network.
- "Bad cached worklet/build state" — full clean wipe + uninstall + rebuild
  + `--no-build-cache` reproduces the same phone2 failure.
- "Phone2 is in Doze" — `mState=ACTIVE mLightState=ACTIVE` while failing.
- "iOS hotspot symmetric NAT" — phone2 fails on home Wi-Fi too, with
  phone1 succeeding on the same Wi-Fi at the same moment.
- "Mobile hotspot will route around AP isolation" — Android hotspot has
  its own client isolation. Doesn't help. Need router-level fix.
- "PEER_NOT_FOUND must be RPC-layer error like findPeer-for-other-peer"
  — actually it's hyperdht/connect.js:382 firing when the DHT walk for
  the blind-peer itself returns 0 responders. Probe-A confirmed.

**Quick verification commands to confirm things still work:**

```bash
# Confirm VPS is healthy
ssh ubuntu@140.245.193.100 'sudo systemctl is-active blind-peer && tail -3 /var/log/blind-peer.log'

# Confirm app's BLIND_PEER_KEYS is the VPS pubkey
grep BLIND_PEER_KEYS /Users/chinmaygopal/dev/zapp/zodl-android/local.properties

# Confirm phone1 is connected to VPS (should show connected=true)
adb -s 3B15B401SNR00000 exec-out "run-as xyz.justzappit.zapp.testnet.debug \
  cat files/zappmessaging/blind-mirror-diag.log" | grep STATE | tail -1

# Confirm phone2's still failing (should show PEER_NOT_FOUND)
adb -s 914652c5 exec-out "run-as xyz.justzappit.zapp.testnet.debug \
  cat files/zappmessaging/blind-mirror-diag.log" | grep -E "STATE|PEER_NOT" | tail -5

# Compare PROBE findPeer responder counts (currently 0 on both phones)
for s in 3B15B401SNR00000 914652c5; do echo "=== $s ==="; \
  adb -s $s exec-out "run-as xyz.justzappit.zapp.testnet.debug \
    cat files/zappmessaging/blind-mirror-diag.log" | grep PROBE | tail -3; done

# Confirm AP isolation on the local Wi-Fi (both must be on Wi-Fi for this test)
adb -s 3B15B401SNR00000 shell ping -c 2 -W 2 192.168.0.163   # phone1 → phone2
adb -s 914652c5         shell ping -c 2 -W 2 192.168.0.247   # phone2 → phone1
# Expect "Destination Host Unreachable" on both → AP isolation present.
```

---

## The vision (what blind-peer is supposed to do)

```
Both peers online + holepunchable        Direct phone↔phone over DHT
Holepunch fails                          Phone → blind-peer → phone
                                         (blind-peer relays packets)
One peer offline                         Phone1 → blind-peer (Hypercore
                                         append). Blind-peer holds the
                                         encrypted blocks until phone2
                                         comes online and pulls them.
```

The blind-peer is identified by its **z32 public key**, never by IP.
Clients DHT-lookup the pubkey, get back whatever address the bootstrap
nodes recorded for the blind-peer, and connect. This is why a single
blind-peer key works for "every Zapp user globally" — provided the
blind-peer is reachable from arbitrary networks (i.e., on a public IP).

---

## Repo layout

| Repo | Purpose |
|---|---|
| `zodl-android` (this repo) | The Android wallet/messenger app. Consumes `:zappmessaging` as a Gradle subproject from `../zappMessaging/android`. |
| `../zappMessaging` | The P2P messaging SDK. JS core under `core/`, Android wrapper under `android/`. Pinned in `.zapp-deps`. |
| `../bare-kit` | Hyperswarm/Bare runtime, vendored from holepunchto/bare-kit v2.0.0. |
| `../zcash-android-wallet-sdk` | Zcash SDK consumed via `SDK_INCLUDED_BUILD_PATH` (Gradle includeBuild). |

`zodl-android/settings.gradle.kts:383-384` includes `:zappmessaging` from
`../zappMessaging/android`. Edits to either repo affect the same Gradle
build.

`.zapp-deps` pins `zappMessaging=d7f5460c…`; that's stale relative to
current `main` (`6ebf858…`). CI verifies the SHA — bump `.zapp-deps`
before merging anything that depends on newer `zappMessaging` code.

---

## What this branch added

Three code changes in `zappMessaging`, one config in `zodl-android`.

### 1. `zappMessaging/android/build.gradle.kts`

```kotlin
val blindPeerKeys: String = run {
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    localProps.getProperty("BLIND_PEER_KEYS")
        ?: providers.gradleProperty("BLIND_PEER_KEYS").orNull
        ?: providers.environmentVariable("BLIND_PEER_KEYS").orNull
        ?: ""
}

android {
  defaultConfig {
    buildConfigField("String", "BLIND_PEER_KEYS", "\"${blindPeerKeys.replace("\"","\\\"")}\"")
  }
  buildFeatures { buildConfig = true }
}
```

Lookup order: `local.properties` → `-PBLIND_PEER_KEYS` Gradle prop → env
var → empty. Empty value → BuildConfig field is `""` → runtime
short-circuits and blind-mirroring is dormant (matches behaviour pre-wire).

### 2. `zappMessaging/android/src/main/java/.../core/BareWorkletManager.kt`

```kotlin
val argv = mutableListOf("--data-dir=${context.filesDir.absolutePath}")
if (BuildConfig.BLIND_PEER_KEYS.isNotEmpty()) {
    argv += "--blind-peer-keys=${BuildConfig.BLIND_PEER_KEYS}"
}
w.start(bundleFile.absolutePath, argv.toTypedArray())
```

Appends `--blind-peer-keys=<comma-separated z32 keys>` only when the
BuildConfig field is non-empty. The Bare worklet receives this in
`Bare.argv`, which `core/lib/config.js:26-29` parses on module load.

### 3. `zappMessaging/core/lib/p2p-manager.js`

```js
const HypercoreId = require('hypercore-id-encoding')
…
let relayThroughFn
const blindKeys = config.BLIND_PEER_KEYS || []
if (blindKeys.length > 0) {
  try {
    const relayKey = HypercoreId.decode(blindKeys[0])
    relayThroughFn = () => relayKey
    diag('Relay through blind peer (always): ' + blindKeys[0].slice(0, 12) + '…')
  } catch (e) { … }
}
this.swarm = new Hyperswarm({
  keyPair,
  bootstrap: allBootstrapNodes.length > 0 ? allBootstrapNodes : undefined,
  relayThrough: relayThroughFn
})
```

**Important: this is the always-on relay form**, not hyperswarm's default.
Hyperswarm's static-key form (`relayThrough: keyBuffer`) only triggers on
`peerInfo.forceRelaying` (set after `HOLEPUNCH_ABORTED` /
`HOLEPUNCH_DOUBLE_RANDOMIZED_NATS` / `REMOTE_NOT_HOLEPUNCHABLE` errors)
or when `swarm.dht.randomized === true`. With our test phones reporting
`firewalled=true addr=none bootstrapped=true`, **`dht.randomized` stayed
false** (the DHT thinks it's bootstrapped fine), and connect attempts
didn't always produce a `shouldForceRelaying`-eligible error code. So the
static form was a no-op for our scenario; the function form
(`() => relayKey`) forces the relay every time.

**Production should revert this** to the static form once a public-IP
blind-peer is available, so direct holepunching gets used when it works
(lower latency than relay).

### 4. `zodl-android/local.properties` *(gitignored, dev-machine only)*

```
BLIND_PEER_KEYS=t41oratar3uuzq4f3txngkkhikqcxtnum56udsgjuotaaberi37y
```

This is the z32 pubkey of the local Mac blind-peer used during this
session. For a real production build, swap for the production VPS's
pubkey, and consider committing it via `gradle.properties` so it ships
in release builds.

---

## How a key flows from `local.properties` to runtime

```
local.properties (BLIND_PEER_KEYS=...)
        │
        ▼  zappMessaging/android/build.gradle.kts reads at config time
BuildConfig.BLIND_PEER_KEYS = "t41ora…"
        │
        ▼  BareWorkletManager.start() at app launch
Worklet argv: ["--data-dir=...", "--blind-peer-keys=t41ora…"]
        │
        ▼  core/lib/config.js parses Bare.argv on require
config.BLIND_PEER_KEYS = ["t41ora…"]
        │
        ▼  p2p-manager.js + blind-mirror.js consume
new Hyperswarm({ relayThrough: () => decode("t41ora…") })
new BlindMirror(swarm, store, { keys: ["t41ora…"] })
```

Verify each hop in `files/zappmessaging/p2p-diag.log` and
`blind-mirror-diag.log` on the device — both should show
`Relay through blind peer (always): t41ora…` and
`BlindMirror ready with 1 blind peer(s)` respectively.

---

## Build, install, run

Network gradle.properties pins `ZCASH_NETWORK=testnet` so only
`Zcashtestnet*` Gradle tasks are registered.

```bash
# 1. zappMessaging side — only needed if you changed core/* JS
cd /Users/chinmaygopal/dev/zapp/zappMessaging
nvm use 20             # or 22 — bare-pack needs ≥20
npm run link:android   # rebuild native .so addons (only after npm install)
npm run build:android  # bundle JS → android/src/main/assets/worklet.bundle

# 2. zodl-android side
cd /Users/chinmaygopal/dev/zapp/zodl-android
./gradlew :app:assembleZcashtestnetStoreDebug

# 3. Install on each device (the install task can't pick a target with multiple devices)
adb -s 3B15B401SNR00000 install -r app/build/outputs/apk/zcashtestnetStore/debug/app-zcashtestnet-store-debug.apk
adb -s 914652c5         install -r app/build/outputs/apk/zcashtestnetStore/debug/app-zcashtestnet-store-debug.apk

# 4. CRITICAL: wipe the cached worklet bundle on each device.
# BareWorkletManager extracts assets/worklet.bundle to files/bare/worklet.bundle
# on first run and uses lastModified() to decide whether to refresh.
# It SHOULD auto-refresh after install, but be explicit during dev cycles.
for s in 3B15B401SNR00000 914652c5; do
  adb -s $s shell run-as xyz.justzappit.zapp.testnet.debug rm -f files/bare/worklet.bundle
  adb -s $s shell am force-stop xyz.justzappit.zapp.testnet.debug
  adb -s $s shell monkey -p xyz.justzappit.zapp.testnet.debug -c android.intent.category.LAUNCHER 1
done
```

App package: `xyz.justzappit.zapp.testnet.debug` (per current APK badging).
Identity creation/restore is gated behind onboarding — the worklet won't
init until the user completes that flow on a fresh install.

---

## Run the local blind-peer

```bash
nvm use 22                  # blind-peer-cli needs ≥20; node 18 will crash
npm install -g blind-peer-cli   # one-time

cd /Users/chinmaygopal/dev/zapp/zappMessaging
nohup blind-peer --storage ./data --port 49737 --max-storage 10gb --debug \
  >> /tmp/blind-peer.log 2>&1 &
```

It logs to `/tmp/blind-peer.log` in pino JSON. The pubkey it announces is
deterministic from the storage dir (the `data/IDENTITY` file). Nuking
`./data` rotates the key; preserve it to keep the same blind-peer
identity across restarts.

**The blind-peer process must be restarted whenever the host's network
changes** — it caches its DHT-detected public address at startup and
doesn't auto-rebind. We hit this multiple times in this session.

---

## Architecture explainer

### How a phone reaches the blind-peer

```
[Phone]                                      [Mac running blind-peer]
   │                                                  │
   │ 1. swarm.dht looks up pubkey on bootstrap        │
   │    nodes (node1/2/3.hyperdht.org)                │
   │ 2. Bootstrap returns the address recorded the    │
   │    last time blind-peer talked to them           │
   │    (= NAT-mapped public IP:port of the Mac)      │
   │ 3. Phone sends UDP to that address               │
   ▼                                                  ▼
        ┌──── Cellular/CGNAT or Wi-Fi NAT ─────┐
        │                                       │
        └──────────► Internet ──────────────────►
                                                   │
                                            Home router NAT
                                                   │
                                                   ▼
                                          Mac (blind-peer @ :49737)
```

**Failure points**:
- Phone's NAT is symmetric (CGNAT, iOS hotspot) — outgoing UDP gets a
  per-destination port mapping. Hyperdht can't predict it, holepunching
  fails.
- Home router is symmetric — incoming UDP from a NAT-mapped phone can't
  match any existing outbound mapping (Mac never sent to phone's IP first).
  Drop.
- Mac is on a VPN (WireGuard) that NATs outbound through a provider exit
  IP — bootstrap nodes record the provider's IP, phone sends to provider
  IP, provider doesn't forward back to a specific WG client. Drop.

### Why same-LAN works regardless

When phone and Mac are on the same Wi-Fi, the Hyperswarm/UDX layer can
discover each other's local addresses (mDNS-ish + DHT hint) and connect
directly via LAN, bypassing all NATs.

### How relay-through fixes (would fix) cross-NAT

Hyperdht supports relay-via-pubkey: phone1 wants to connect to phone2,
both phones are NATted, but both can reach the blind-peer. The DHT
arranges:

```
phone1 ──► blind-peer ──► phone2
```

…where blind-peer just shuttles encrypted UDP packets between them.
**This only works if both phones can reach the blind-peer** — which is
exactly the same reachability problem above. Relay does not help if the
blind-peer itself is unreachable from one side.

---

## Test scenarios & results

| # | Phone1 network | Phone2 network | Mac | Phone1↔BP | Phone2↔BP | P↔P direct |
|---|---|---|---|---|---|---|
| 1 | Same LAN as Mac | Same LAN as Mac | Home Wi-Fi | ✅ | ✅ | ✅ (LAN) |
| 2 | Same LAN as Mac | iOS hotspot 172.20.10.x | Home Wi-Fi | ✅ | ❌ | ❌ |
| 3 | Cellular | Hotspot 10.215.90.x | WireGuard (utun) | ✅ (briefly) | ❌ | ❌ |
| 4 | (not tested) | (not tested) | VPS | (expected ✅) | (expected ✅) | (expected ✅) |

Scenario 2 is the current state. Phones can ping the Mac's public IP
(low-latency: 4.5 ms — looks like same-ISP local routing), but UDP/49737
NAT-traversal fails for phone2.

---

## Instrumentation added in session 2

These are uncommitted patches to `../zappMessaging` on
`debug/blind-peer-relay-investigation`. They change behavior only by
adding diag logs — no logic changes.

### `core/lib/blind-mirror.js`

1. **Identity vs ephemeral keypair dump** at BlindMirror init. Logs
   both `swarm.keyPair.publicKey` (the user's identity) and
   `swarm.dht.defaultKeyPair.publicKey` (the ephemeral DHT key that
   will appear as `remotePublicKey` at the blind-peer). Gives you a
   way to map "Opened connection to <z32>" lines in
   `/tmp/blind-peer.log` back to a specific phone.
2. **Wrapped `BlindPeering._getBlindPeer`** to log every new
   `BlindPeerClient` creation, plus the lifecycle of each underlying
   ProtomuxRPC stream (open/error/close), with the error code on
   failure (`PEER_NOT_FOUND` was the culprit in our case).
3. **Periodic STATE dump every 10s** showing
   `connected/opened/rpc/cores/refs` for each blind peer. Definitive
   way to see whether a phone has a working RPC session right now.
4. **`_b4aHex` helper** for portable Buffer→hex without `toString`
   (works under Bare).

Output goes to `files/zappmessaging/blind-mirror-diag.log` on the
device.

### `core/lib/p2p-manager.js`

- **Wrapped the always-on relay function** to log how often
  `relayThrough()` is invoked by hyperswarm. Counts every connect
  attempt that's about to be relay-routed. Logs the first 5 invocations
  then every 10th, so you don't drown.
- **Added a clear log** when `BLIND_PEER_KEYS` is empty (`NO blind
  peer keys — relay disabled`). Catches the C1 regression silently
  without needing to diff bundles.

Output goes to `files/zappmessaging/p2p-diag.log` on the device.

### How to verify the patches are live

```bash
# md5 of the bundle should match host:
md5 -q ../zappMessaging/android/src/main/assets/worklet.bundle
adb -s <serial> shell "run-as xyz.justzappit.zapp.testnet.debug md5sum files/bare/worklet.bundle"

# A working install will show this on app launch:
adb -s <serial> exec-out "run-as xyz.justzappit.zapp.testnet.debug \
  cat files/zappmessaging/blind-mirror-diag.log" | grep -E "Identity|Ephemeral|NEW BlindPeerClient"
```

If you don't see the `Identity (...)` and `Ephemeral (...)` lines on
init, the device is running an old bundle — wipe `files/bare/worklet.bundle`
and force-stop/relaunch.

These patches should be **kept in the branch but reverted before the
relay-through change ships to main**, since they're noisy in
production. Or keep just the STATE dump and drop the per-attempt
logs.

---

## Diagnostic data (where to look)

### On the Mac

| File | Content |
|---|---|
| `/tmp/blind-peer.log` | pino JSON: `Opened/Closed connection to <peer z32>`, `Core activity for Discovery key …`, `add-core request received` |
| `route -n get default` | Shows whether default route is via VPN (`utun*`) or home gateway (`192.168.0.1`) |
| `curl https://api.ipify.org` | Real public IP visible to bootstrap nodes |
| `ifconfig en0` / `ifconfig utun*` | LAN IP / VPN tunnel IP |
| `lsof -nP -iUDP:49737` | Confirms blind-peer is bound |

### On each Android device

All under `run-as xyz.justzappit.zapp.testnet.debug`:

| Path | Content |
|---|---|
| `files/zappmessaging/diag.log` | Worklet startup, module load, identity init |
| `files/zappmessaging/p2p-diag.log` | Hyperswarm state — every 10s `SWARM` dump, `Relay through blind peer …`, `joinConversation`, `Direct send …` |
| `files/zappmessaging/blind-mirror-diag.log` | `BlindMirror ready`, `Registered local core`, `Registered remote core` |
| `files/zappmessaging/hypercore-diag.log` | `Corestore ready`, `Local core ready conv=… key=…`, `Appended … newLen=…` |
| `cache/zappmessaging_diag.log` | IPC bridge events, NDJSON traffic |

### Key SWARM dump fields

```
SWARM conns=0 peers=0 addr=none dhtReady=true bootstrapped=true
      firewalled=true port=49737 convs=1 globalConns=0 inviteTopics=0
```

| Field | Healthy | Meaning if not |
|---|---|---|
| `port` | `>0` | UDX socket bound. `0` = native addon load failure (jniLibs ABI mismatch — the `bd9e9e8` fix). |
| `bootstrapped` | `true` | DHT got initial routing info from bootstrap nodes. |
| `firewalled` | `false` ideally | DHT thinks it can accept incoming. `true` = no incoming UDP — depends on DHT's STUN-like detection. |
| `addr` | `<ip>:<port>` | DHT-detected reachable address. `none` = STUN never settled. |
| `dhtReady` | `true` | `dht.ready()` resolved. |
| `conns` | matches expected peers | `swarm.connections.size`. **BlindPeering connections do NOT increment this**; they use a separate path on `swarm.dht`. |
| `convs` | per joined conversation | Each direct chat = +1 after `joinConversation`. |
| `inviteTopics` | per pending invite | Topics joined for invite delivery via `sendInvite()`. |

---

## Identifiers used this session

| Thing | Value |
|---|---|
| Blind-peer pubkey (z32) | `t41oratar3uuzq4f3txngkkhikqcxtnum56udsgjuotaaberi37y` |
| Blind-peer encryption pubkey | `bnkae3eghnuj48ahmk6ukbnpfc4aqsx5jbmtbcfsn8exxy1crjsy` |
| Phone1 ADB serial | `3B15B401SNR00000` (CPH2747, OnePlus 6T variant — flaky USB) |
| Phone1 identity hex | `66683dca614519a7511f626cc9e869e45f8cc7cfc03f96fcce300ac2d7c8552b` |
| Phone1 conversation core key (hex prefix) | `77fd2616834d…` |
| Phone1 conversation discovery key (z32) | `h5fcnwgicm5fjutbuchrua5ocwzkgbt1gys9cst5bimgtyaxwscy` |
| Phone2 ADB serial | `914652c5` (OnePlus 8T KB2005) |
| Phone2 identity hex | `c4aebb8981f92c6baf8c2350c0aeda2143e62fde3f1f0dcda910be2d76759c10` |
| Phone2 conversation core key (hex prefix) | `86abae4160be…` |
| Conversation ID | `dm_d29edc649cfa438231c81192cd18982c` |
| Conversation topic (hex prefix) | `201a964fc62b…` |
| App package | `xyz.justzappit.zapp.testnet.debug` |

---

## Findings (confirmed)

> **New findings from session 2 (2026-05-14)** are tagged `[S2]`. Old
> findings from session 1 are unchanged unless explicitly superseded.

1. **The `0922f9c` audit commit silently disabled blind-mirroring on
   Android.** Item C1 ("Extract hardcoded blind peer key to config.js")
   moved `DEFAULT_BLIND_PEER_KEYS` from `blind-mirror.js` to `config.js`
   with empty default + `--blind-peer-keys=` CLI arg. The Android side
   never wired the CLI arg, so from May 7 to today every build had
   `BlindMirror enabled: false reason: no_keys`. This branch fixes that.
2. **bd9e9e8 → HEAD has no swarm/DHT/NAT regression.** 16 files,
   `+461/-170`, almost all `console.log → diag()`. Bootstrap nodes
   unchanged, suspend handler unchanged, native addons unchanged. The
   "feels broken" perception was the wiring regression in (1).
3. **jniLibs are clean.** Single version per ABI. The
   "32 stale .so files → port=0 firewalled=true" bug from `bd9e9e8` is
   not present on current HEAD.
4. **`Bare.on('suspend')` no longer suspends the swarm.** That fix is
   still in place (`core/index.js`).
5. **Phone1 ↔ blind-peer works on same LAN.** Confirmed by repeated
   `Opened connection to e6pgdq…` (= phone1 z32) in `/tmp/blind-peer.log`,
   plus `Core activity for Discovery key h5fcnwg… (14/14, 1 peers)` —
   meaning phone1 has 14 messages in its conversation Hypercore and
   blind-peer has replicated all 14.
6. **Phone2 (iOS hotspot) cannot reach blind-peer.** Even with WireGuard
   off and the Mac announcing its real residential public IP, zero
   incoming UDP/49737 from phone2's hotspot mapping. ICMP ping works
   though, so the issue is specifically UDP/NAT.
7. **`swarm.connections.size` ≠ blind-peer connections.** `BlindPeering`
   uses `swarm.dht.connect()` directly via `BlindPeerClient`, which
   doesn't fire the swarm's public `connection` event. So a healthy
   blind-peer connection coexists with `conns=0` in the SWARM dump.
   Don't be misled — check `/tmp/blind-peer.log` for the truth.
8. **WireGuard on Mac actively *hurts* reachability.** Default-routing
   through WG makes bootstrap nodes record the WG provider's exit IP
   (e.g. `34.0.35.179` in Google Cloud) as the blind-peer's address.
   Phones look that up and try to send packets to a Google Cloud machine
   that has no idea who their target is. Always restart blind-peer with
   WG off for testing.
9. **Hyperswarm's default `relayThrough` static-key form was a no-op for
   our test.** It only fires on `dht.randomized` or specific holepunch
   error codes; with `firewalled=true addr=none bootstrapped=true`
   neither condition triggered. We had to use the function form
   `() => relayKey` to force relay use. **Revert to static form when a
   real public-IP blind-peer is available**, so direct holepunching wins
   when it can.
10. **`[S2]` BlindPeerClient connects with the DHT *default* keypair, not
    the user's identity keypair.** Hyperswarm's constructor creates the
    DHT *without* passing its own `keyPair` (see hyperswarm/index.js:38),
    so `dht.defaultKeyPair = createKeyPair()` is a freshly-random keypair
    generated at app startup. `BlindPeerClient.connect()` calls
    `dht.connect(blindPeerKey, { keyPair: this.keyPair })` with
    `this.keyPair = null`, falling through to `dht.defaultKeyPair`. So at
    the blind-peer's noise layer, `stream.remotePublicKey` is this
    ephemeral DHT key — *not* the identity. **This answers session-1
    Open Question #3**: every z32 prefix in `/tmp/blind-peer.log` like
    `e6pgdq…`, `obgnshpih…`, `1mijeo5f…` is phone1's *DHT default
    keypair* for that specific app session, regenerated on every app
    process boot.
11. **`[S2]` The blind-peer's `--trusted-peer` flag is unusable as-is
    for our model.** `_isTrustedPeer(stream.remotePublicKey)` (in
    `blind-peer/index.js:539`) checks the connecting client's noise
    pubkey, which (per #10) is the ephemeral DHT key. We can't
    pre-trust *identity* z32s because identity isn't what shows up.
    Two ways to fix: (a) wrap `BlindPeering._getBlindPeer` to pass
    `keyPair: identity.keyPair` to `BlindPeerClient` (so the noise
    handshake uses identity, then identity z32s in `--trusted-peer`
    actually match), or (b) fork blind-peer to gate on something else.
    Until one of these, every connection logs `Downgraded announce
    because the peer is not trusted` and the blind-peer stores the core
    but won't republish its discovery key on the DHT — which means a
    second peer can't *find* the first peer's core via the blind-peer
    even after both are connected. Combined with finding #7 from
    session 1, this is a structural limit on how the relay path can
    deliver, not a configuration thing.
12. **`[S2]` Behind residential NAT, the blind-peer's DHT announce
    fails for fresh DHT clients.** Reproduced from a node script *on
    the Mac itself*: `dht.findPeer(blindPeerKey)` returned 0 responders
    even immediately after `Announced all initial cores` and even after
    blind-peer restart. `dht.connect()` from the Mac succeeded only
    because hyperdht has a same-process / same-socket fast-path
    (`firewall` callback in connect.js can claim incoming UDP from any
    source). Phones don't have that fast-path. Phone1's success
    appears to be either DHT-keyspace luck (its ephemeral keypair
    happened to land close enough to the announce target) or stale
    `_socketPool.routes` cache from a previous successful connect.
    Phone2 — different ephemeral keypair, fresh `_socketPool` — never
    succeeded across 10+ minutes of `PEER_NOT_FOUND` retries.
13. **`[S2]` WireGuard re-bit us.** The Mac's default route was via
    `utun4 → 10.8.0.3` (WG provider, exit IP `34.0.35.179` Google
    Cloud) at the start of session 2. Re-confirmed Finding #8: blind-peer
    announced the GCP IP, every phone lookup tried that IP, GCP
    dropped. After turning WG off and restarting blind-peer, phone1
    (which had been retrying for ~30 min) connected within ~30 seconds.
14. **`[S3]` Production blind-peer deployed on Oracle Cloud Always-Free
    VPS** (`140.245.193.100`, Ubuntu 24.04 Minimal, x86_64,
    VM.Standard.E2.1.Micro = 1 OCPU / 1 GB RAM + 2 GB swap, ap-hyderabad-1
    AD-1). systemd unit at `/etc/systemd/system/blind-peer.service`
    auto-restarts on crash/reboot. Logs to `/var/log/blind-peer.log`.
    Pubkey `5ccrwsgqfg1hawwcbckmisww4sy3qns5scsntxfztgx7pt4eps5o`,
    encryption pubkey `wkgj383rnwcdqgzrgoqc36toeg8b51kxyg67bh8y58jdkkfpty1o`.
    Phone1 (ephemeral `36677bab…`) connects and replicates all 28 blocks
    (9 + 19) in seconds — same conversation cores that wouldn't reliably
    reach the residential-NAT Mac blind-peer.
15. **`[S3]` First attempt on Oracle Linux 9 failed at C++ ABI level.**
    OL9 ships `GLIBCXX_3.4.29` (GCC 11.5); blind-peer-cli's
    `rocksdb-native` prebuilds require `GLIBCXX_3.4.30` (GCC 12+).
    `gcc-toolset-13` does *not* ship a runtime libstdc++ that satisfies
    this (only `-devel` headers). **Lesson: for any future Holepunch
    deployment, pick Ubuntu 22.04+, NOT enterprise distros (RHEL/Rocky/OL).**
    The prebuilds assume modern glibc/GLIBCXX and patching enterprise
    distros around this is a dead end.
16. **`[S3]` Phone2 ephemeral-rotation does NOT fix `PEER_NOT_FOUND`.**
    Force-stopping phone2's app and relaunching rotated its DHT default
    keypair from `7a27362c…` to `d1ecb9af…`. Both ephemerals failed
    identically against the public-IP VPS — three `PEER_NOT_FOUND` retries
    in 90 seconds post-restart. **This disproves Open Question #5a from
    session 2** ("phone2's ephemeral landed in unlucky DHT keyspace"):
    the unluck is persistent across ephemerals on the same device, so
    it's a device/network property, not a keyspace coincidence.
17. **`[S3]` Phone2 fails even on the same Wi-Fi as a working phone1.**
    Did a full clean wipe (`gradlew --stop`, removed `build-cache-1`,
    `transforms-*`, `build-conventions-secant/{build,.gradle,.kotlin}`,
    `.gradle`, `app/build`, `ui-lib/build`), uninstalled the app from
    both phones, rebuilt with `--no-build-cache`, fresh-installed and
    re-onboarded both devices. Then put both phones on the same home
    Wi-Fi (confirmed via `ip addr`: 192.168.0.247 / 192.168.0.163, same
    `/24`). **Phone1 connected to the VPS within 15s** (`STATE
    registered=1 peers=1 connected=true rpc=true`) on its third
    ephemeral keypair this session. **Phone2 still PEER_NOT_FOUND**
    (4+ retries observed) even while phone1 was actively connected to
    the same VPS from the same network at the same moment. This
    eliminates: (a) keyspace luck, (b) VPS reachability, (c) network
    NAT, (d) app/identity/cache state. Phone2's `deviceidle` is
    `mState=ACTIVE mLightState=ACTIVE` (not in Doze). The remaining
    candidates are: a OnePlus 8T (KB2005) specific UDP-socket behaviour,
    OxygenOS battery management killing UDX before RPC completes, or
    an IPv6 routing quirk on phone2 that confuses hyperdht's
    Kademlia walk. **Working hypothesis for next session**: instrument
    `BlindPeering._getBlindPeer` / the BlindPeerClient RPC layer in
    `../zappMessaging/core/lib/blind-mirror.js` to log which RPC method
    returned `PEER_NOT_FOUND` (e.g. `addCore` vs an internal `findPeer`
    walk), and capture phone2's UDX socket close reason when the
    PEER_NOT_FOUND fires. If it's `findPeer`-on-the-blind-peer that's
    erroring, the underlying issue is the trust-gate / unannounced
    discovery key (Finding #11).
18. **`[S3]` Invite handshake confirmed dead over cross-NAT (live demo).**
    After fresh installs, phone1 (`convs=0 inviteTopics=0`) and phone2
    (`convs=1 inviteTopics=1`, message sent into the void) both ended up
    with their own solo chats — phone1 never received phone2's invite
    even though both apps were running and the inviter was actively
    listening on its invite topic. Confirms doc's "Wider architectural
    improvement" empirically: blind-peer can't help bootstrap NEW chats,
    only mirror existing ones whose `__core_keys` exchange already
    happened over a direct socket. Same-LAN puts them on the same
    Hyperswarm broadcast and the invite can land — once paired, they
    can separate and blind-peer keeps them synced. But the bootstrap
    requires LAN/holepunchable contact at least once, which is the
    architectural blocker for "any two Zapp users globally" until
    invite-via-Hypercore (or invite-via-blind-peer-RPC) ships.
19. **`[S4]` AP isolation on the user's home Wi-Fi blocks
    phone↔phone direct LAN sync entirely.** ICMP ping from phone1
    (`192.168.0.247`) → phone2 (`192.168.0.163`) returns `Destination
    Host Unreachable`, and the reverse direction same. Both phones on
    the same `/24`, same router, same SSID. This means: Hyperswarm's
    LAN-discovery / same-LAN holepunching cannot succeed regardless of
    code changes, because the router's layer-2 forwarding refuses
    client-to-client traffic. SWARM dumps on both phones show `conns=0
    peers=0 addr=none` — they literally can't see each other on the
    wire. **Mobile hotspot fallback also fails**: Android's built-in
    hotspot has its own client isolation by default; both phones on
    phone1's hotspot still showed `conns=0`. Fix is router-config-only
    ("AP Isolation" / "Client Isolation" / "Privacy Separator" off in
    the admin panel), OR move both phones to a network that doesn't
    isolate, OR don't depend on direct LAN at all (i.e. land the
    invite-via-Hypercore architectural fix). For test setups: most
    coffee-shop/office Wi-Fi has isolation ON; most consumer home Wi-Fi
    has it OFF by default but some routers (Eero in "Privacy Mode",
    older Asus with "AP Isolated", any "guest network") have it ON.
20. **`[S4]` `PEER_NOT_FOUND` traced to hyperdht's findPeer returning 0
    responders, on BOTH phones.** Added `_probeFindPeer()` to
    `BlindMirror.ready()` (in `core/lib/blind-mirror.js`) that runs
    `swarm.dht.findPeer(blindPeerKey, { hash: false, retries: 3 })`
    every 20s and logs `PROBE findPeer(<keyHex>) responders=<N>
    first=<nodeIdHex> ms=<elapsed>`. Result: **both phone1 and phone2
    log `responders=0 first=n/a`**, despite phone1 simultaneously
    being in `connected=true rpc=true` state with the VPS. So fresh
    findPeer-from-zero-cache returns nothing on either phone — yet
    phone1's BlindPeerClient connect succeeds anyway. The
    differentiator must be the alternate-path in `hyperdht/lib/connect.js`
    starting at line 326: when `peer._socketPool.routes` has a cached
    route for the target, hyperdht uses it with
    `onlyClosestNodes:true, retries:1` instead of a full walk.
    **Hypothesis to test next**: phone1 has entries in
    `_socketPool.routes` for the VPS key while phone2 doesn't, and the
    entries get populated by some side effect of swarm/topic joins
    that happens reliably on phone1 but not on phone2. A probe-B was
    coded to inspect `dht.io.serverSocket._socketPool.routes` per-key
    sizes but is not yet rebuilt/redeployed (see Track B in Resume Here).
21. **`[S4]` Both phones are already paired in conversation `dm_3ddc00c17`
    with distinct local cores; phone1's appends reach VPS but phone2
    can't pull them.** Per-phone hypercore-diag:
    - Phone1 local core `f875ed4f3357…`, currently at `newLen=8` (8 messages)
    - Phone2 local core `8bd3c243b824…`, currently at `newLen=7`
    Both reference `conv=dm_3ddc00c17`. So the pairing happened at
    some earlier point (same-LAN previously, before AP isolation became
    an issue or before this Wi-Fi was the test network), the
    `__core_keys` exchange ran, and they have each other's core keys
    locally. Phone1's blocks land on the VPS (VPS log shows discovery
    key `hpzxspnofs1j…` going 1→2→3→4→5 blocks as user types). Phone2
    can't pull them because phone2 can't reach the VPS. **AND** even
    if phone2 connected, the trust-gate from Finding #11 means the
    blind-peer won't republish phone1's discovery key on DHT
    ("Downgraded announce for peer ... because the peer is not
    trusted" fires on every VPS connection log line). So the trust-gate
    fix is necessary not just for invite delivery but for ongoing-chat
    delivery too whenever both peers are NAT-blocked from each other.

---

## Open questions / things we couldn't measure

1. **What address does hyperdht actually announce for the blind-peer?**
   blind-peer-cli logs `Blind peer listening, local address is …` but
   that's the bound socket, not the DHT-announced address. `[S2]
   partially answered`: we observed empirically (via fresh DHT lookup
   from the Mac) that whatever it announces, fresh public DHT clients
   can't find it. To inspect the announce content directly you still
   need a hyperdht monkey-patch.
2. **What NAT type is the home router?** Cone vs symmetric determines
   whether hole-punching from arbitrary phones can ever succeed without
   port forwarding. STUN-style NAT-type detection (e.g. `pystun3`) would
   answer this. `[S2]` Indirect evidence: if the home router were a
   well-behaved cone NAT, phone2 should also have been findable; the
   fact that phone1 succeeds and phone2 doesn't *under identical
   conditions* is suggestive of either symmetric NAT, port-preservation
   conflicts when 2 LAN clients try to map the same external port, or
   no hairpin support.
3. **`[S2] ANSWERED`** — see Finding #10. The z32s are phone1's DHT
   *default* keypair (ephemeral per app boot), not its identity. Phone1
   identity hex `66683dca…` decodes to z32
   `c3wd51ubewc4qwe9cjscu4djhtxa3t6xay93p9gqgyfcfi6ekwio`, which never
   appears in `/tmp/blind-peer.log` because the connecting client uses
   the DHT-default keypair, not identity.
4. **Why does phone2 ↔ Mac ICMP ping take only 4.5 ms?** That's
   suspiciously fast for cellular → public internet → home ISP. Likely
   same-ISP local routing in this region. Unrelated to the bug, but
   worth noting if performance characteristics differ in production.
5. **`[S2]` Why does phone1 succeed and phone2 fail under identical
   conditions?** Best hypothesis is one of:
   (a) Phone1's ephemeral DHT keypair happens to be close in keyspace
       to the blind-peer's announce target, so it queries the right
       neighborhood; phone2 doesn't.
   (b) Phone1 has a `_socketPool.routes` cache entry from a previous
       successful connect (back when WG was off briefly) that bypasses
       findPeer; phone2 has none.
   (c) The home router can only sustain one concurrent hole-punch
       mapping to the Mac's UDP/49737, and phone1 won that race.
   We didn't pin which one; port-forward (next steps) makes the
   distinction moot since findPeer stops being the gate.

---

## Local testing setup (Mac as blind-peer host) — recommended dev path

This is what to do *right now* to get phone↔phone messaging working
locally with a Mac. The plan validated in session 2.

### Prereqs (one-time)

1. **Confirm you're not behind CGNAT.** If you are, no amount of
   port-forwarding on your home router helps because your "public IP"
   isn't actually globally routable.
   ```bash
   curl -s https://api.ipify.org   # what the world sees
   # Then log into your router admin and check the WAN IP.
   # If they MATCH, you're fine. If they DIFFER (router shows
   # 100.x.x.x or some other private range), you're behind CGNAT —
   # skip to "Production deployment plan" instead.
   ```
2. **Install blind-peer-cli with Node 20+.** Node 18 will crash on
   `bare-addon-resolve`.
   ```bash
   nvm use 22
   npm install -g blind-peer-cli
   ```
3. **Make sure WireGuard / any VPN is OFF on the Mac.** Re-check every
   time you start the blind-peer. See Finding #13. Quick check:
   ```bash
   route -n get default | grep interface   # must say en0 (or eth),
                                            # NOT utun*
   ```

### Step 1 — Add a static DHCP lease for the Mac (one-time)

The blind-peer's DHT announce includes the Mac's address. If your
router reassigns the Mac a different LAN IP later, the port-forward
rule (next step) starts pointing at the wrong machine. Pin the Mac to
`192.168.0.110` permanently.

In your router admin (Asus example, varies by brand):

1. Open **LAN → DHCP Server** (or "DHCP Reservation" / "Address
   Reservation" depending on firmware).
2. Find the Mac in the connected-clients list (look for hostname or
   the en0 MAC).
3. Click **Add** / lock icon, set IP to `192.168.0.110`, save.

### Step 2 — Add the port-forward rule (one-time)

In your router admin, find the section called any of: **WAN → Virtual
Server** / **Port Forwarding** / **NAT Forwarding**. Add:

| Field | Value |
|---|---|
| Service Name (label) | `blind-peer` |
| Protocol | **UDP** (not TCP, not both — UDP only) |
| External Port | `49737` |
| Internal IP | `192.168.0.110` |
| Internal Port | `49737` |
| Source IP | leave blank / "any" |

Click **Apply**. Some firmwares need a reboot; most don't.

### Step 3 — Verify the forward is actually open

From outside your network (e.g. tether your phone to cellular and run
this from a Termux-ish app, or use https://canyouseeme.org with
"Custom Port Probe" UDP):

```bash
# from any machine NOT on your home network:
nc -u -v -w 3 <your_public_ip> 49737    # connection should not refuse
```

You can also just send a single UDP packet and look for it on the Mac:

```bash
# on the Mac, in one terminal:
sudo tcpdump -i en0 udp port 49737

# from outside the network, send a packet:
echo TEST | nc -u -w 1 <your_public_ip> 49737

# if tcpdump shows the packet, port-forward is working.
```

### Step 4 — Start blind-peer, install instrumented build, observe

```bash
# On the Mac:
cd /Users/chinmaygopal/dev/zapp/zappMessaging
nvm use 22
nohup blind-peer --storage ./data --port 49737 --max-storage 10gb \
  --debug >> /tmp/blind-peer.log 2>&1 &
tail -f /tmp/blind-peer.log
# Wait for "Listening at t41ora..." line. Note the z32 pubkey.

# In zodl-android repo:
cd /Users/chinmaygopal/dev/zapp/zodl-android
# (BLIND_PEER_KEYS in local.properties already points at this z32)
./gradlew :app:assembleZcashtestnetStoreDebug

# Install on each device (the install task can't pick a target with multiple devices):
adb -s 3B15B401SNR00000 install -r app/build/outputs/apk/zcashtestnetStore/debug/app-zcashtestnet-store-debug.apk
adb -s 914652c5         install -r app/build/outputs/apk/zcashtestnetStore/debug/app-zcashtestnet-store-debug.apk

# Wipe cached worklet bundle + force-restart the app on each device
PKG=xyz.justzappit.zapp.testnet.debug
for s in 3B15B401SNR00000 914652c5; do
  adb -s $s shell run-as $PKG rm -f files/bare/worklet.bundle
  adb -s $s shell am force-stop $PKG
  adb -s $s shell monkey -p $PKG -c android.intent.category.LAUNCHER 1
done
```

### Step 5 — Verify both phones connected

In `/tmp/blind-peer.log` you should see TWO distinct ephemeral z32
pubkeys opening connections (not just one — that was the bug before
the port-forward). Each phone's identity-vs-ephemeral mapping is
logged in the new instrumentation:

```bash
PKG=xyz.justzappit.zapp.testnet.debug
for s in 3B15B401SNR00000 914652c5; do
  echo "=== $s ==="
  adb -s $s exec-out "run-as $PKG cat files/zappmessaging/blind-mirror-diag.log" \
    | grep -E "Identity|Ephemeral|connected=true" | tail -5
done
```

Expected:
- `connected=true rpc=true` STATE lines for *both* phones
- `Opened connection to <z32>` for both phones' ephemerals in
  `/tmp/blind-peer.log`
- `Core activity for Discovery key …` showing growing block counts
  when you send messages in either direction

If only one phone connects after port-forward, see Open Question #5
and check that `--max-storage` isn't choking, that the router didn't
silently drop the rule on reboot, and that the static DHCP lease held.

---

## Production deployment plan

### Long-term: VPS fleet (the Keet pattern)

Keet (Holepunch's reference messaging app) doesn't rely on a single
blind-peer. They run a *fleet* of public blind-peers behind well-known
pubkeys, ship the pubkey list in the client, and have BlindPeering
pick the closest 2-3. If one is down, others serve. We should do the
same.

Targets:

1. **Two free VPSes.** Oracle Cloud Always-Free gives an Ampere VM
   forever; pair it with a tiny $5/mo Hetzner / Linode / DO instance
   for redundancy. Both need:
   - A public IPv4 (CGNAT-free)
   - UDP/49737 open inbound (Oracle: Security List rule; AWS: SG
     rule; Hetzner: ufw or hcloud firewall)
   - SSH key auth, password auth disabled
   - `blind-peer` running as a non-root user via systemd
2. **Persistent identity.** The blind-peer's pubkey is derived from
   `<storage>/IDENTITY`. **Back this file up.** If you nuke storage you
   rotate the key and every existing client breaks until they get the
   new key in an app update.
3. **Bounded storage.** Always pass `--max-storage 10gb` (or whatever
   the disk allows). The blind-peer accepts add-core requests from any
   peer; without a cap, anyone can fill your disk.
4. **Ship multiple keys.** `BlindMirror` accepts an array. After both
   VPSes are up:
   - Move from `local.properties` → committed `gradle.properties`:
     ```properties
     BLIND_PEER_KEYS=<vps1_z32>,<vps2_z32>
     ```
   - In `BlindMirror`, set `mirrors: 2` (or more if you have more
     keys) so each core gets registered with N blind peers.

### Setup script for a fresh VPS (verified 2026-05-15 on Oracle Cloud)

**Important pre-requisites** (learned the hard way in `[S3]`):
- Use **Ubuntu 22.04+**, not Oracle Linux / RHEL / Rocky. Enterprise distros
  ship `GLIBCXX_3.4.29` (GCC 11) but Holepunch prebuilds need
  `GLIBCXX_3.4.30+` (GCC 12+). `gcc-toolset-13` does NOT provide a runtime
  fix — it only ships `-devel` headers.
- If you're on Oracle Cloud's E2.1.Micro (1 GB RAM), **add 2 GB swap first** —
  `npm install` of blind-peer-cli's deps OOMs the kernel without it (we
  burned an instance on this).
- Open UDP/49737 in **both** layers: the cloud firewall (Oracle Security
  List + any attached NSG) AND the in-instance firewall (`iptables`/`ufw`).
  Ubuntu's Oracle image pre-loads iptables INPUT rules that block non-22
  traffic — `iptables -I INPUT 6 -p udp --dport 49737 -j ACCEPT &&
  netfilter-persistent save`.

**Install (Ubuntu 24.04 Minimal, what we shipped on)**:

```bash
# 0. Add swap if instance has <2 GB RAM
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && \
  sudo mkswap /swapfile && sudo swapon /swapfile && \
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 1. Node 22 + build tools
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt-get install -y nodejs build-essential iptables-persistent

# 2. Open in-instance firewall
sudo iptables -I INPUT 6 -p udp --dport 49737 -j ACCEPT
sudo netfilter-persistent save

# 3. Install blind-peer-cli **locally** (NOT globally — require-addon's
#    path resolution misbehaves with global npm installs)
mkdir -p ~/blind-peer-app && cd ~/blind-peer-app
npm init -y
npm install blind-peer-cli
mkdir -p ~/blind-peer-data

# 4. Foreground first to capture pubkey + verify
./node_modules/.bin/blind-peer --storage ~/blind-peer-data --port 49737 \
  --max-storage 10gb --debug
# Note "Listening at <z32>" — copy that. Ctrl-C.

# Open UDP/49737 inbound on the cloud firewall (Oracle Security List + any
# attached NSG; AWS SG; Hetzner ufw/hcloud firewall).
# 5. systemd unit. NOTE: avoid heredocs over SSH — terminals often mangle
# long lines. Use `printf` with embedded \n or a base64-decode trick.
sudo bash -c 'printf "[Unit]\nDescription=Zapp blind-peer relay\nAfter=network-online.target\nWants=network-online.target\n\n[Service]\nType=simple\nUser=ubuntu\nWorkingDirectory=/home/ubuntu/blind-peer-app\nExecStart=/home/ubuntu/blind-peer-app/node_modules/.bin/blind-peer --storage /home/ubuntu/blind-peer-data --port 49737 --max-storage 10gb --debug\nRestart=on-failure\nRestartSec=10s\nStandardOutput=append:/var/log/blind-peer.log\nStandardError=append:/var/log/blind-peer.log\n\n[Install]\nWantedBy=multi-user.target\n" > /etc/systemd/system/blind-peer.service'

sudo touch /var/log/blind-peer.log
sudo chown ubuntu:ubuntu /var/log/blind-peer.log
sudo systemctl daemon-reload
sudo systemctl enable --now blind-peer
tail -f /var/log/blind-peer.log
```

### Is opening UDP/49737 to the world dangerous?

No, with the standard caveats:

- **It's UDP, not TCP.** Most off-the-shelf exploit kits target TCP.
- **The blind-peer protocol requires a Noise handshake.** Random
  scanners get a TLS-like rejection, not a shell.
- **The blind-peer can't read uploaded data.** Cores are encrypted by
  discovery key; the peer is a dumb storage backend by design. Worst
  case is someone fills storage — that's why `--max-storage` is
  mandatory.

Hardening checklist on the VPS:

- Open ONLY UDP/49737 inbound on the cloud firewall. Block everything
  else.
- Run blind-peer as a non-root user (the systemd unit above).
- Disable SSH password auth (`PasswordAuthentication no`).
- Set `--max-storage` to less than the disk size with margin.

### Why not ship just the home-router port-forward solution?

- Bottlenecked by your home upload bandwidth (~10-50 Mbps on most
  residential connections, vs. 1 Gbps+ on most VPSes).
- Down whenever your power, ISP, or router blips.
- Stuck on a dynamic IP that the ISP can rotate without warning,
  silently breaking every client.
- You're routing every other Zapp user's encrypted traffic through
  your home connection.

Port-forwarding is **fine for development and your own personal
testing**, terrible as the production answer for "every Zapp user
globally."

---

## Other follow-ups

### Code cleanup once production blind-peer is up

1. **Revert `relayThrough` to the static form** in
   `core/lib/p2p-manager.js`:
   ```js
   relayThrough: relayKey   // not () => relayKey
   ```
   This restores hyperswarm's default behaviour (try direct first,
   relay only when holepunch fails) and gives lower-latency direct
   connections when both peers are punchable.
2. **Bump `.zapp-deps`** to the new `zappMessaging` SHA so CI passes.
3. **Move the production blind-peer pubkey out of `local.properties`**
   into a committed `gradle.properties` line (or Gradle build flavour),
   so devs don't each have to add it to their machine and so release
   builds always have it.
4. **`[S2]` Decide what to do about the trust-gate / identity-keypair
   issue (Finding #11).** Recommended: wrap
   `BlindPeering._getBlindPeer` to pass `keyPair: identity.keyPair`
   into `BlindPeerClient`. Then the noise pubkey at the blind-peer
   matches the identity, identity-z32s in `--trusted-peer` actually
   work, and we can stop seeing the `Downgraded announce` warning.

### Wider architectural improvement

Even with a public-IP blind-peer, the **invite handshake** still flows
over direct swarm sockets (see `sendInvite` in
`core/lib/p2p-manager.js`). If the inviter and invitee can't reach each
other directly, the invite never lands and blind-peer can't help — it
only mirrors existing conversations. Consider moving invite delivery
into Hypercore so it also benefits from blind-peer relay (Autobase
pattern).

The same applies to the `__core_keys` exchange in `handleConnection`
(`p2p-manager.js:413`). Two peers learn each other's Hypercore keys
*only* over a live direct socket. Without that, even when both phones
are connected to the blind-peer and the blind-peer holds both their
encrypted cores, neither phone knows what core key to ask for. Lifting
that exchange into a Hypercore-based metadata channel is the real
production fix.

### Tailscale — *test-only escape hatch, NOT production*

If you want to validate cross-network without setting up port-forwarding
or a VPS, install Tailscale on Mac + both phones. All three get
`100.x.x.x` IPs on a virtual mesh. The Mac's blind-peer will then be
reachable to the phones via Tailscale's NAT-traversal magic. **Don't
ship this** — every Zapp user would need a Tailscale account.

---

## Glossary

| Term | What it means here |
|---|---|
| **Hyperswarm** | The peer discovery + connection layer. `swarm.join(topic)` announces and looks up peers on a topic. |
| **Hyperdht** | The DHT under hyperswarm. Provides peer lookup, NAT-traversal hole-punching, and the relay-through mechanism. |
| **Bare** | Holepunch's stripped-down JS runtime that runs on mobile (BareKit). The worklet is JS code packed by `bare-pack`. |
| **Blind peer** | A node that holds encrypted Hypercore blocks for offline message delivery. Cannot read messages — only stores blocks indexed by discovery key. |
| **BlindPeering** (the npm pkg) | Client library that registers Hypercores with one or more blind-peer servers and replicates to them. |
| **Hypercore** | Append-only signed log. Each conversation has a writable local Hypercore per participant. |
| **Discovery key** | `crypto.discoveryKey(corePublicKey)`. The DHT-lookup key for a Hypercore — derived but not invertible to the actual key, so DHT lookups don't leak the core's identity. |
| **Hole-punching** | UDP NAT-traversal coordinated by hyperdht via bootstrap nodes. Works for cone NATs, fails for symmetric. |
| **Cone NAT** | NAT that maps `(internal_ip, internal_port) → (external_ip, external_port)` independent of destination. Allows incoming from any source on the mapped port. |
| **Symmetric NAT** | NAT that maps differently per `(destination_ip, destination_port)`. Hole-punching can't predict mappings; usually fatal. CGNAT and most cellular use this. |
| **CGNAT** | Carrier-Grade NAT. ISPs share one public IP across many subscribers. Almost always symmetric. |
| **Hairpin NAT** | Router rewrites a packet sent from inside to its own public IP back to the correct internal client. Many consumer/phone hotspot routers don't support hairpin. |
| **`relayThrough`** | Hyperswarm option. When set, direct peer connections can be relayed through the named pubkey. Static value vs function controls when (default: only on holepunch failure; function: always). |
| **z32** | Holepunch's base32 alphabet (`ybndrfg8ejkmcpqxot1uwisza345h769`). 52-char encoding of 32-byte pubkeys. Used for blind-peer keys, peer pubkeys in DHT. |

---

## How to verify the wire-up is working from scratch

1. Confirm `local.properties` has the blind-peer key.
2. Build + install per the commands above.
3. Walk through onboarding on the device.
4. Check `files/zappmessaging/p2p-diag.log` — should see
   `Relay through blind peer (always): <key prefix>…` near startup.
5. Check `files/zappmessaging/blind-mirror-diag.log` — should see
   `BlindMirror ready with 1 blind peer(s)` and
   `Registered local core: conv=…`.
6. Check `/tmp/blind-peer.log` on the Mac — should see
   `Opened connection to <phone z32>` and `add-core request received`.
7. Send a message in an existing chat. `hypercore-diag.log` should show
   `Appended conv=… newLen=N`. `/tmp/blind-peer.log` should show
   `Core activity for Discovery key … (N / N, 1 peers)`.

If steps 4-6 work but two phones still can't message each other, the
problem is reachability between phone↔blind-peer (this branch's blocker
— see Findings #6 and Next Steps A/B), not the wiring.
