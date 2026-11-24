# Payload registry

Every payload implemented in this repository. The registry is the contract: a
payload that is not listed here is not loaded by the lab runner and is not
considered part of the bench.

| id | name | domain | persistence | CVE | status | first evidence |
| --- | --- | --- | --- | --- | --- | --- |
| `dev.lanyard.payload.pagecache-relay` | Page-cache Relay 0.1.0 | init + bluetooth | boot (memory-only) | [none claimed](CVES.md#devlanyardpayloadpagecache-relay---no-cve-claimed) | `gated` | [2026-11-24](evidence/2026-11-24-pagecache-relay-a52.md) |

Status follows the lifecycle in [RESEARCH.md](RESEARCH.md): `idea`, `gated`,
`running`, `verified`, `failed`, `shelved`, `retired`.

## dev.lanyard.payload.pagecache-relay

A first-party Lanyard payload built from the published Open Crown research. It
stages a 576-byte page-cache patch into a root-executed library for the current
boot; it does not trigger itself, and a reboot clears it.

### Entries

| Entry | Target symbol | Domain | Trigger |
| --- | --- | --- | --- |
| `payload.bin` | `/system/lib64/libbase.so` `_ZN7android4base10LogMessageD2Ev` | `u:r:init:s0` | next init `LOG()` after `setprop ctl.start zzz` from the shell |
| `payload_bt_read.bin` | `/apex/com.android.btservices/lib64/libbluetooth_jni.so` `_Z13gatt_act_readP10tGATT_CLCBt` | `bluetooth` | a live `gatt_act_read`; the app performs no Bluetooth I/O |

### Gate row

Device record: [`DEV-0001`](DEVICES.md#dev-0001---samsung-galaxy-a52s-5g-sm-a528b)
(Samsung Galaxy A52s 5G).

| model | device | build | kernel | sdk | abi | status |
| --- | --- | --- | --- | --- | --- | --- |
| SM-A528B | a52sxq | A528BXXSBGYI3 | 5.4.254-qgki-28575149-abA528BXXSBGYI3 | 34 | arm64-v8a | verified |

A device that matches only the specification is reported as **Compatible,
untested** and the arm controls stay inert. The runner checks ABI, SDK floor and
kernel family before it considers a verified row.

### CVE

No CVE is claimed for this payload. It is an unattributed research primitive; see
[`CVES.md`](CVES.md#devlanyardpayloadpagecache-relay---no-cve-claimed) for what
is known, what would be required to attribute one, and why the id is not in the
`cve-*` namespace.

### Capabilities

`root`, `init-domain-relay`. The relay is an unauthenticated raw-syscall RPC
that never returns to init. It is not a general root agent: it cannot necessarily
open a Bluetooth HCI socket, change SELinux context or start a normal root
daemon from the `untrusted_app` domain.

### Hashes

```text
48d294ce335cf99f59b6b3031ed3741a033556ac55a445f122aea695b630ba43  payload.bin
9eac4674ae8a092991fe3c981406de6ba43be2f1a44d4dbbf51b1e29b2f1efc4  payload_bt_read.bin
```

### Provenance

Derived from Open Crown at commit `fcaadff4c1a75da05ba0cb1736f69d0037d239ef`
(MIT). The manifest carries the upstream id in its `upstream` block. The
exploit logic and payload bytes are unchanged; the id, packaging, app and gates
are Lanyard's. See the change list in
[`device-app/README.md`](../device-app/README.md#provenance).

### Payload README

[`docs/payloads/dev.lanyard.payload.pagecache-relay.md`](payloads/dev.lanyard.payload.pagecache-relay.md)
carries the hypothesis, procedure, trigger, cleanup and provenance for this
payload.

### Evidence and log

- Evidence: [2026-11-24 - page-cache relay on the A52](evidence/2026-11-24-pagecache-relay-a52.md)
- Log: [RL-0001](RESEARCH_LOG.md#rl-0001---stage-the-open-crown-page-cache-relay), [RL-0002](RESEARCH_LOG.md#rl-0002---rebuild-the-lab-runner-as-a-first-party-app)

### Known limits

- one verified device row; this is not a general-purpose root tool;
- the app stages the relay and never consumes it;
- the `untrusted_app` domain cannot reach `@dfi_init`; the shell domain can;
- the first init-triggered execution can consume init's logging thread and reset
  the phone; keep a recovery path on the bench;
- no automated test executes the write. The run path is covered by unit tests
  against a fake executor, by design.
