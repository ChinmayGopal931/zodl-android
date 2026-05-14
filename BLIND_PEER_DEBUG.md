# Blind Peer Wire-Up & Relay Investigation — 2026-05-13

> **Purpose**: Onboarding doc for whoever picks up this debugging next.
> Reading this top-to-bottom should be enough to reproduce the test setup,
> understand what works / what doesn't, and pick the right next step
> without re-deriving anything.

> **Companion branch in `../zappMessaging`**: same name
> (`debug/blind-peer-relay-investigation`). The wiring code lives in that
> repo; this repo only carries this doc + the gitignored `local.properties`
> entry.

---

## TL;DR

1. **Wiring works end-to-end.** Blind-peer key flows
   `local.properties` → `BuildConfig` → Bare worklet `argv` →
   `core/lib/config.js` → `BlindMirror`. Confirmed with `BlindMirror ready
   with 1 blind peer(s)` on both phones, and Hypercore replication from
   phone1 to blind-peer (`Core activity ... 14/14, 1 peers`).
2. **Same-LAN works.** Phone1 (192.168.0.247) connects to the Mac's
   blind-peer (192.168.0.110) via DHT discovery + LAN.
3. **Cross-NAT does not.** Phone2 on iOS Personal Hotspot
   (172.20.10.0/28, cellular CGNAT) cannot reach the Mac's blind-peer at
   all. Phone2 *can* ping the Mac's home public IP (4.5 ms — same ISP),
   but UDP/49737 from phone2's NAT mapping is dropped at the home router.
4. **The blocker is the Mac being behind a residential NAT**, not the
   code. The single fix that makes "any two zapp users globally" work is a
   **public-IP host** for the blind-peer (port-forward home router OR
   VPS).

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

---

## Open questions / things we couldn't measure

1. **What address does hyperdht actually announce for the blind-peer?**
   blind-peer-cli logs `Blind peer listening, local address is …` but
   that's the bound socket, not the DHT-announced address. To inspect
   the announce, we'd need to either monkey-patch hyperdht or add a
   `dht.remoteAddress()` log line to blind-peer-cli.
2. **What NAT type is the home router?** Cone vs symmetric determines
   whether hole-punching from arbitrary phones can ever succeed without
   port forwarding. STUN-style NAT-type detection (e.g. `pystun3`) would
   answer this.
3. **Are `e6pgdq…`, `obgnshpih…`, `zwnt1sf51qx…` (z32) all phone1's
   identity?** Their hex equivalent should be `66683dca…`. We never
   verified by decoding z32 → hex on the Mac side. If they're different
   pubkeys, then either the phone keypair rotates per session or
   BlindPeering uses a separate key pair we missed.
4. **Why does phone2 ↔ Mac ICMP ping take only 4.5 ms?** That's
   suspiciously fast for cellular → public internet → home ISP. Likely
   same-ISP local routing in this region. Unrelated to the bug, but
   worth noting if performance characteristics differ in production.

---

## Next steps (prioritized)

### A. Make the Mac blind-peer reachable from anywhere — *3 minutes, free*

Open the home router admin UI (`http://192.168.0.1`), find Port
Forwarding (sometimes "Virtual Server" / "NAT Forwarding"), and add:

| Field | Value |
|---|---|
| Protocol | UDP |
| External port | 49737 |
| Internal IP | 192.168.0.110 (your Mac's LAN IP) |
| Internal port | 49737 |

Then restart blind-peer. The DHT will detect the new direct reachability
and announce your public IP unmodified. Phone2 (and any phone anywhere)
should then connect within ~30 sec.

### B. Free public-IP VPS — *10 minutes, free forever*

[Oracle Cloud Always-Free](https://signup.cloud.oracle.com) gives a
forever-free Ampere VM with a public IPv4. Steps:

```bash
# On a fresh Ubuntu 22.04 VPS:
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
sudo npm install -g blind-peer-cli
mkdir -p ~/blind-peer-data

# Foreground first to capture pubkey:
blind-peer --storage ~/blind-peer-data --port 49737 --max-storage 10gb
# → copy the "Listening at <z32>" line, paste into local.properties.
# Then run as a service via the systemd unit at zappMessaging/scripts/blind-peer.service.
```

VPS firewall: open UDP/49737 inbound. On Oracle Cloud that's a Security
List rule. On most other providers it's a checkbox.

This is the production answer. Ship the VPS pubkey via
`gradle.properties` (committed) so release builds pick it up
automatically.

### C. After A or B — code cleanup

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

### D. Tailscale — *test-only escape hatch, NOT production*

If you want to validate cross-network without setting up port-forwarding
or a VPS, install Tailscale on Mac + both phones. All three get
`100.x.x.x` IPs on a virtual mesh. The Mac's blind-peer will then be
reachable to the phones via Tailscale's NAT-traversal magic. **Don't
ship this** — every Zapp user would need a Tailscale account.

### E. Wider architectural improvement

Even with a public-IP blind-peer, the **invite handshake** still flows
over direct swarm sockets (see `sendInvite` in
`core/lib/p2p-manager.js`). If the inviter and invitee can't reach each
other directly, the invite never lands and blind-peer can't help — it
only mirrors existing conversations. Consider moving invite delivery
into Hypercore so it also benefits from blind-peer relay (Autobase
pattern).

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
