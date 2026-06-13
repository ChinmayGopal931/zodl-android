# Release signing & Google Play

How the Zapp Android app is signed and published. **No secrets live in this file or anywhere in git** —
the keystore file and its password are kept out of the repo (see "Where the secrets live" below).

## App on Google Play

| | |
|---|---|
| Play listing | **Zapp for Android** |
| Package | `xyz.justzappit.zapp` |
| Play App Signing | **Enabled** (Google manages the distribution signing key; we manage only the *upload* key) |
| Store release artifact | `./gradlew :app:bundleZcashmainnetStoreRelease` → `app/build/outputs/bundle/zcashmainnetStoreRelease/*.aab` |

## Two keys, two roles

With Play App Signing there are **two** certificates. Don't confuse them:

- **App signing key** — held and managed by Google. It signs what users actually download. We can never
  export or change it. (Used to register the app with API providers, Digital Asset Links, etc.)
- **Upload key** — *we* hold it. We sign every `.aab` locally with it; Google verifies it's really us, then
  re-signs the app with the app signing key for distribution. This is the key that can be reset/rotated.

### App signing key (Google-managed, permanent)

```
SHA-1:   FF:FF:7C:A9:E3:BA:ED:64:59:53:3D:A7:82:E6:62:FB:C4:B3:98:0F
SHA-256: B7:80:75:87:64:D7:E4:AB:D5:83:8D:9E:59:3E:9B:EC:D5:E5:64:93:76:F0:14:13:C3:B6:75:BD:04:1E:3D:83
MD5:     10:3E:3D:D4:96:CC:F8:D3:35:6D:03:25:17:AB:53:B1
```

### Upload key — CURRENT (rotated 2026-06-08)

```
Keystore: ~/keys/justzappit/zapp-upload-v2.keystore   (NOT in git — local machine + password manager only)
Alias:    zapp-upload
Type:     PKCS12, RSA 2048, valid until 2053
Subject:  CN=JustZappIt Zapp Upload, OU=Mobile, O=JustZappIt, C=US
SHA-1:    1F:D6:DC:3A:55:C4:41:57:41:E3:48:C4:39:BE:EB:57:2E:5D:76:34
SHA-256:  80:0C:61:A4:61:11:25:FB:F0:DD:C3:D1:AB:FE:22:CF:F7:EA:D7:65:E8:42:04:D6:DB:E5:62:39:08:02:DF:D4
```

Registered via Play Console → **App integrity → App signing → Request upload key reset** on **2026-06-08**
(reason: "lost upload key"). Google approval of an upload-key reset can take up to ~48h. **Approved and live
as of 2026-06-11** (Play Console shows `1F:D6:DC…` as the active upload certificate).

## How an upload-key reset actually works (read this before panicking)

We lost time once (2026-06-11) looking for "the new key Google sent us". **Google never sends a key.**
The flow is the reverse of what you might expect:

1. **You generate the new keystore yourself, locally**, before or while filing the reset request:
   ```bash
   keytool -genkeypair -keystore ~/keys/justzappit/zapp-upload-v3.keystore \
     -alias zapp-upload -keyalg RSA -keysize 2048 -validity 10000
   keytool -exportcert -rfc -keystore ~/keys/justzappit/zapp-upload-v3.keystore \
     -alias zapp-upload -file upload-cert.pem
   ```
2. You upload only the **certificate** (`.pem`) with the reset request. The private key never leaves
   this machine — which is why nobody can "find it" anywhere but here.
3. Google approves silently after up to ~48h. **There is no obvious notification.** To check whether the
   reset is live, open Play Console → **App integrity → App signing** and compare the *Upload key
   certificate* fingerprints against your local keystore:
   ```bash
   keytool -list -v -keystore ~/keys/justzappit/zapp-upload-v2.keystore -alias zapp-upload | grep SHA1
   ```
   When they match, the reset is done and the next `.aab` you upload must be signed with that keystore.
4. **Immediately after generating a new keystore:** record its path + fingerprints in this doc, and back up
   the keystore file *and* password to the team password manager. Losing either means another reset and
   another ~48h of downtime.

## Where the secrets live (and where they must NOT)

The keystore **file** and its **password** are the only secrets. They are stored in exactly two places:

1. The keystore file `~/keys/justzappit/zapp-upload-v2.keystore` on the release machine.
2. The password in `local.properties` (which is **git-ignored**) and in the team password manager.

The build reads four properties from `local.properties` (never commit these):

```
ZCASH_RELEASE_KEYSTORE_PATH=/Users/<you>/keys/justzappit/zapp-upload-v2.keystore
ZCASH_RELEASE_KEYSTORE_PASSWORD=<secret>
ZCASH_RELEASE_KEY_ALIAS=zapp-upload
ZCASH_RELEASE_KEY_ALIAS_PASSWORD=<secret>
```

The committed `gradle.properties` leaves these **blank** (safe to ship). If all four aren't set, the release
build is produced **unsigned**.

> ⚠️ **Backup imperative.** The 2026-06-08 reset was forced because previous upload keys' files/passwords were
> lost. Back up **both** the keystore file and its password to the team password manager. If both are lost
> again, the only recovery is another upload-key reset (~48h of downtime).

## Retired / unusable keys — DO NOT try to reuse

Google rejects any previously-used upload certificate ("same as a previous upload certificate"), so none of
these can be re-registered. Listed here so nobody wastes time on them again:

| SHA-1 | Subject | What it was | Status |
|---|---|---|---|
| `76:1B:F8:A4:…` | CN=JustZappIt Zapp Upload | original upload key | retired; Play rejects as a previous cert |
| `3F:D5:DE:55:…` | CN=JustZappIt Zapp Upload | 2nd upload key (signed Internal 0.1) | **private key LOST** — the reason for the 2026-06-08 reset |
| `BE:F1:37:A7:…` | CN=renee chiu | Renee's personal keystore | never registered for this app; not used |

## Onboarding another uploader (collaborator)

Play App Signing keeps the distribution key Google-managed, but **every upload must be signed by the one
registered upload key** (`1F:D6:DC…` above). There is no "add a second upload key" — a different cert is
rejected as "same as a previous upload certificate," and swapping keys needs the ~48h reset. So a second
developer uploads with the **same** keystore. To enable one:

1. **Grant Play Console access.** In Play Console → **Users and permissions**, invite their Google account
   and grant a permission covering the tracks they need (e.g. *Release to testing tracks* for internal/closed,
   or *Release to production*). This lets them sign in and upload; it does **not** hand over the upload key.
2. **Share the upload keystore — securely.** Give them a copy of `zapp-upload-v2.keystore` and its password
   via the team password manager (or another end-to-end-encrypted channel). **Never** email/Slack/commit the
   file or password in plaintext. They must use this exact keystore, not generate their own.
3. **Configure their machine.** They put the keystore anywhere local (e.g. `~/keys/justzappit/`) and set the
   four props in their own git-ignored `local.properties`, with `ZCASH_RELEASE_KEYSTORE_PATH` pointing at
   *their* copy:

   ```
   ZCASH_RELEASE_KEYSTORE_PATH=/Users/<them>/keys/justzappit/zapp-upload-v2.keystore
   ZCASH_RELEASE_KEYSTORE_PASSWORD=<secret>
   ZCASH_RELEASE_KEY_ALIAS=zapp-upload
   ZCASH_RELEASE_KEY_ALIAS_PASSWORD=<secret>
   ```

4. **Verify before the first upload.** Build per the procedure below, then confirm the bundle is signed by the
   right key — `keytool -printcert -jarfile <app>.aab` — and that the SHA-1 matches `1F:D6:DC…` above. A
   mismatch (or an unsigned bundle, i.e. one or more props blank) will be rejected by Play.

## Release procedure

1. Confirm `local.properties` has the four `ZCASH_RELEASE_*` props set, and `ZCASH_NETWORK` / `P2P_NETWORK`
   are correct for the build (mainnet store release: `P2P_NETWORK` must be mainnet/Base, **not** `sepolia`).
2. `./gradlew :app:bundleZcashmainnetStoreRelease`
3. Upload the `.aab` from `app/build/outputs/bundle/zcashmainnetStoreRelease/` to Play Console →
   **Test and release** → Internal testing / Closed / Production.
4. Play verifies the upload signature against the registered upload key above, then re-signs with the app
   signing key for distribution.
