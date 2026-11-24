# Page-cache Relay

- id: `dev.lanyard.payload.pagecache-relay`
- version: 0.1.0
- domain: `u:r:init:s0` (init entry), `bluetooth` (Bluetooth entry)
- persistence: boot; memory-only; no partition or boot image is written
- reversible: reboot
- status: `gated`; see [research log](../RESEARCH_LOG.md#rl-0001---stage-the-open-crown-page-cache-relay)

## Hypothesis

On a verified Samsung 5.4-qgki build, an IPsec/UDP + CBC page-cache write can
patch a root-executed library for the current boot, and the resulting entry point
can host a relay in the init (or bluetooth) domain. The relay is consumed from
the shell domain; the app only stages it.

## Target

| model | device | build | kernel | sdk | abi | status |
| --- | --- | --- | --- | --- | --- | --- |
| SM-A528B | a52sxq | A528BXXSBGYI3 | 5.4.254-qgki-28575149-abA528BXXSBGYI3 | 34 | arm64-v8a | verified |
| 5.4 qgki, other models | - | - | `^5\.4\.[0-9]+-qgki-.*$` | >= 31 | arm64-v8a | candidate, never enables a run |

## CVE

None claimed. The upstream research does not name a CVE, and this repository does
not attribute one. The payload is a page-cache write primitive, not a
reproduction of a recorded defect. See
[`../../docs/CVES.md`](../../docs/CVES.md#devlanyardpayloadpagecache-relay---no-cve-claimed)
for the record and for the conditions under which that changes.

## Entries

| entry | target | effect | trigger |
| --- | --- | --- | --- |
| `payload.bin` | `libbase.so` `_ZN7android4base10LogMessageD2Ev` | stages the init-domain relay | next init `LOG()` after `setprop ctl.start zzz` from the shell |
| `payload_bt_read.bin` | `libbluetooth_jni.so` `_Z13gatt_act_readP10tGATT_CLCBt` | stages the Bluetooth-domain payload | a live GATT read; the app performs no Bluetooth I/O |

## Procedure

1. Gate the app to the verified row (the runner does this on load).
2. Press and hold the payload card for 1.5 s.
3. Read the consent sheet and confirm.
4. Watch the log for `Verified: 576/576 bytes` and `PAGE CACHE MODIFIED`.

## Trigger (outside the app)

```sh
adb shell setprop ctl.start zzz
```

The command may hang once the relay is live. The app cannot connect to
`@dfi_init` from `untrusted_app`; the shell domain can. The relay may
watchdog-reset the phone if init's logging thread is consumed.

## Observed

Gate `Verified` on the A52; the non-writing probe returns
`pagecache_relay arm64 ready`; both markers are absent before any run. **No
write has been executed.** See the evidence entry.

## Limits and known blockers

- one verified device row; not a general-purpose root tool;
- the relay is an unauthenticated raw-syscall RPC and never returns to init;
- no automated test executes the write;
- the debug certificate is not a distribution identity.

## Provenance

Derived from Open Crown at commit `fcaadff4c1a75da05ba0cb1736f69d0037d239ef`
(MIT). The manifest `upstream` block keeps the upstream id, project, commit and
licence. Payload bytes and exploit logic are unchanged; the id, packaging, gates
and app are Lanyard's. The change list is in
[`../../device-app/README.md`](../../device-app/README.md#provenance).

## Evidence

- [2026-11-24 - page-cache relay on the A52](../evidence/2026-11-24-pagecache-relay-a52.md)

## Cleanup

Reboot. Confirm `/dev/.dfi` and `/dev/.dfib` are absent and that neither
`@dfi_init` nor `@dfi_bt` is listening from the shell domain.
