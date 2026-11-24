# 2026-11-24 - page-cache relay on the A52

- attempt: [RL-0001](../RESEARCH_LOG.md#rl-0001---stage-the-open-crown-page-cache-relay) and [RL-0002](../RESEARCH_LOG.md#rl-0002---rebuild-the-lab-runner-as-a-first-party-app)
- device record: [DEV-0001](../DEVICES.md#dev-0001---samsung-galaxy-a52s-5g-sm-a528b),
  raw capture [`2026-11-24-a52-device-facts.txt`](2026-11-24-a52-device-facts.txt)
- device: SM-A528B, codename `a52sxq`, serial `R5CR916B48N`
- build: `samsung/a52sxqeea/a52sxq:14/UP1A.231005.007/A528BXXSBGYI3:user/release-keys`
- kernel: `5.4.254-qgki-28575149-abA528BXXSBGYI3`
- android: 14 / SDK 34, abi `arm64-v8a`
- artifact: `dev.lanyard.payload` 0.2.0 (versionCode 2)
  sha256:`ae2e65d9ab4daaca143b54f23249d0d79951eb2384a111dee3753b02f2a8fa8c`
- payload: `dev.lanyard.payload.pagecache-relay` 0.1.0
- cve: none claimed; the payload is an unattributed research primitive, see
  [`CVES.md`](../CVES.md#devlanyardpayloadpagecache-relay---no-cve-claimed)

## Operator gesture

Install the debug APK over the 0.1.0 build, launch it, read the gate, run the
non-writing `Probe native relay` from the About tab. No payload was armed.

```sh
adb -s R5CR916B48N install --no-streaming -r -g app-debug.apk
adb -s R5CR916B48N shell am start -W -n dev.lanyard.payload/.MainActivity
```

## Raw output

Gate, from the console and the About tab:

```text
Verified
SM-A528B / 5.4.254-qgki-28575149-abA528BXXSBGYI3 is a verified target
```

Non-writing native probe, in the run log:

```text
=== native relay check ===
native relay: pagecache_relay arm64 ready
native relay check: OK
```

Relay state before any run:

```text
init marker /dev/.dfi: absent
init socket @dfi_init: unreadable
BT marker /dev/.dfib: absent
BT socket @dfi_bt: unreadable
```

`unreadable` for `/proc/net/unix` is the expected `untrusted_app` result, not a
runner fault; the sockets are confirmed from the shell domain.

The runner reports its own domain, which is the domain that is denied those
sockets:

```text
App domain  u:r:untrusted_app:s0:c60,c257,c512,c768
```

Payload digests as recomputed by the About tab from the packaged assets:

```text
48d294ce335cf99f59b6b3031ed3741a033556ac55a445f122aea695b630ba43  payload.bin
9eac4674ae8a092991fe3c981406de6ba43be2f1a44d4dbbf51b1e29b2f1efc4  payload_bt_read.bin
```

Screenshots: [console](../../device-app/docs/a52-console.png),
[consent](../../device-app/docs/a52-consent.png),
[log](../../device-app/docs/a52-log.png),
[relay](../../device-app/docs/a52-relay.png),
[device record](../../device-app/docs/a52-about.png) and
[its lower half](../../device-app/docs/a52-about-facts.png).

## Not done

- **No payload was armed or executed.** The page-cache write was deliberately
  left to a bench day with a recovery path.
- No init trigger was issued, so the reset behaviour was not observed.
- No Bluetooth peripheral was used, so the GATT-read path was not exercised.
- No root plane was claimed: the app reports a staged relay, never root.

## Verification of the tooling claim (RL-0002)

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline
```

26 tests, 0 failures; lint clean. Both themes and landscape were exercised on
the device.

## Cleanup

Nothing to clean: no patch was written, the markers were absent throughout, and
no process was left running. The debug APK remains installed for bench use.
