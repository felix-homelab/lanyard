# Bench devices

Every device used for an attempt gets a record here **before** the first attempt.
The record is the fixed point every other document points at: the gate row in
[`PAYLOADS.md`](PAYLOADS.md), the evidence entry, and the log entry all cite a
device id.

| id | device | serial | role | last captured |
| --- | --- | --- | --- | --- |
| DEV-0001 | Samsung Galaxy A52s 5G (SM-A528B) | `R5CR916B48N` | verified lab target | 2026-11-24 |

## How to capture

A record is only worth what its raw capture is worth, so capture first and write
the table from it:

```sh
adb -s <serial> get-serialno
adb -s <serial> shell uname -a
adb -s <serial> shell getenforce
adb -s <serial> shell id
adb -s <serial> shell getprop > docs/evidence/<date>-<device>-props.txt
```

The lab runner shows the same facts in **About -> Device record**, and
**Copy device facts** puts them on the clipboard as `key=value` lines.

## DEV-0001 - Samsung Galaxy A52s 5G (SM-A528B)

- serial: `R5CR916B48N`
- role: verified target for `dev.lanyard.payload.pagecache-relay`
- captured: 2026-11-24, [raw dump](evidence/2026-11-24-a52-device-facts.txt)
- attempts: [RL-0001](RESEARCH_LOG.md#rl-0001---stage-the-open-crown-page-cache-relay), [RL-0002](RESEARCH_LOG.md#rl-0002---rebuild-the-lab-runner-as-a-first-party-app)

### Identity

| field | value |
| --- | --- |
| manufacturer | samsung |
| brand | samsung |
| model | SM-A528B |
| codename | a52sxq |
| product | a52sxqeea |
| board | lahaina |
| hardware | qcom |
| SoC | SM7325 / QTI |
| bootloader | A528BXXSBGYI3 |
| first API level | 30 |

### Build

| field | value |
| --- | --- |
| fingerprint | `samsung/a52sxqeea/a52sxq:14/UP1A.231005.007/A528BXXSBGYI3:user/release-keys` |
| build id | UP1A.231005.007 |
| display id | UP1A.231005.007.A528BXXSBGYI3 |
| incremental | A528BXXSBGYI3 |
| type / tags | user / release-keys |
| Android | 14 / SDK 34 |
| security patch | 2025-09-01 |
| build date | Thu Sep 11 17:07:54 KST 2025 |
| built by | `dpi@21DK7A09` |

### Kernel

| field | value |
| --- | --- |
| release | `5.4.254-qgki-28575149-abA528BXXSBGYI3` |
| version | `#1 SMP PREEMPT Thu Sep 11 17:21:09 KST 2025` |
| family | 5.4 qgki |
| arch | aarch64 |
| abi | `arm64-v8a`; supported: `arm64-v8a, armeabi-v7a, armeabi` |
| page size | 4096 |

### Security state

| field | value |
| --- | --- |
| `ro.secure` | 1 |
| `ro.debuggable` | 0 |
| `ro.adb.secure` | 1 |
| SELinux | Enforcing |
| shell context | `u:r:shell:s0` |
| app domain | `u:r:untrusted_app:s0:c60,c257,c512,c768` (as reported by the runner) |
| `su` provider | absent (`command -v su` empty) |
| root plane | none; no `su` domain, no `adb root` on a `user` build |

### Partition fingerprints

| partition | value |
| --- | --- |
| system | `samsung/a52sxqxx/qssi:14/UP1A.231005.007/A528BXXSBGYI3:user/release-keys` |
| vendor | `samsung/a52sxqxx/a52sxq:11/RP1A.200720.012/A528BXXSBGYI3:user/release-keys` |
| odm | `samsung/a52sxqxx/a52sxq:11/RP1A.200720.012/A528BXXSBGYI3:user/release-keys` |

### Gate row

Matches the `verified` row in
[`PAYLOADS.md`](PAYLOADS.md#gate-row) for
`dev.lanyard.payload.pagecache-relay`: model, codename, build and kernel all
agree. Observed `Verified` in the runner on 2026-11-24.

### Notes

- The vendor and odm partitions report an Android 11 fingerprint while system is
  Android 14 (Samsung vendor skew, same build id). The kernel release and the
  build incremental are what the gate row keys on.
- The serial is stable for this device; re-capture if it is ever reflashed or
  replaced, and open a new device record rather than editing this one.
- No payload has been executed on this device. See [RL-0001](RESEARCH_LOG.md#rl-0001---stage-the-open-crown-page-cache-relay).

## New device template

```md
## DEV-000N - <marketing name> (<model>)

- serial:
- role: verified target / candidate / host-only
- captured: <date>; raw dump: docs/evidence/<date>-<device>-props.txt
- attempts:

### Identity   manufacturer / brand / model / codename / product / board / hardware / SoC / bootloader
### Build      fingerprint / build id / display id / incremental / type / tags / android / security patch / build date
### Kernel     release / version / family / arch / abi / page size
### Security   ro.secure / ro.debuggable / ro.adb.secure / selinux / shell context / su / root plane
### Gate row   which registry row it satisfies, and what the runner displayed
```
