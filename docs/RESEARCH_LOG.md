# Research log

Append-only record of every attempt on the bench, including the ones that failed
and the ones that never left the bench. New entries go at the bottom; an existing
entry is amended only by appending to its follow-ups.

Conventions: see [RESEARCH.md](RESEARCH.md). Every entry needs an authorization
line, even when the answer is "operator's own lab device".

| # | date | attempt | status |
| --- | --- | --- | --- |
| RL-0001 | 2026-11-24 | Stage the Open Crown page-cache relay on the A52 | `gated` |
| RL-0002 | 2026-11-24 | Rebuild the lab runner as a first-party app | `verified` (tooling) |
| RL-0003 | 2026-11-24 | Move the runner to Compose 1.12 / AGP 9 | `shelved` |

---

## RL-0001 - Stage the Open Crown page-cache relay

- date: 2026-11-24
- status: `gated` - implemented and gated to the exact device row, never executed
- payload: `dev.lanyard.payload.pagecache-relay` 0.1.0 (`payload.bin`, init domain)
- cve: none claimed; see [`CVES.md`](CVES.md#devlanyardpayloadpagecache-relay---no-cve-claimed)
- target: SM-A528B / a52sxq / build A528BXXSBGYI3 / kernel
  5.4.254-qgki-28575149-abA528BXXSBGYI3 / SDK 34 / arm64-v8a
- authorization: operator's own lab A52 (serial `R5CR916B48N`), physical access,
  operated on the bench
- hypothesis: on this exact build, the vendored IPsec/UDP + CBC chain can stage a
  576-byte patch into `libbase.so`'s page cache for the current boot, and the
  native relay can report a verified write.
- procedure: install the debug APK, open it, confirm the target gate reads
  `Verified`, press and hold the init card, read and accept the consent sheet.
- observation: gate `Verified` on the exact row; the non-writing native probe
  returns `pagecache_relay arm64 ready`; `/dev/.dfi` and `@dfi_init` are absent
  and `/proc/net/unix` is unreadable from `untrusted_app`, as measured before.
- verdict: **not verified**. The write was deliberately not executed during
  deployment. Nothing in this repository claims a successful page-cache write.
- evidence: [2026-11-24 - page-cache relay on the A52](evidence/2026-11-24-pagecache-relay-a52.md)
- follow-ups: on a bench day with a recovery path, arm the init relay, expect
  `Verified: 576/576 bytes` and `PAGE CACHE MODIFIED`, then trigger from the
  shell. Record the reset behaviour if the logging thread is consumed.

## RL-0002 - Rebuild the lab runner as a first-party app

- date: 2026-11-24
- status: `verified` (tooling change, no exploitation)
- payload: `dev.lanyard.payload.pagecache-relay` 0.1.0, unchanged bytes
- cve: none claimed; the packaging change does not create or remove an attribution
- target: same A52, same build
- authorization: operator's own lab device
- hypothesis: the runner can be rebuilt as a Compose app that keeps every gate,
  survives rotation without dropping a run, and is harder to misread than the
  0.1.0 single scroll.
- procedure: rewrite the UI and state layer; make payload parsing and the run
  state machine testable off-device; install over 0.1.0; walk the three
  destinations; run the non-writing native probe.
- observation: 26 JVM tests pass; lint is clean; the gate still reads
  `Verified`; the probe still returns `pagecache_relay arm64 ready`; the
  consent sheet is reachable only through a 1.5 s hold.
- verdict: verified. No payload was executed and no exploitation capability was
  added. The manifest match now checks ABI, SDK floor and kernel family before a
  verified row, which only tightens the gate.
- evidence: [2026-11-24 - page-cache relay on the A52](evidence/2026-11-24-pagecache-relay-a52.md)
- follow-ups: none.

## RL-0003 - Move the runner to Compose 1.12 / AGP 9

- date: 2026-11-24
- status: `shelved`
- payload: none
- target: none; toolchain only
- authorization: not applicable
- hypothesis: the runner could be built on the newest cached Compose (1.12.1) and
  Material 3 (1.4.0).
- procedure: resolve and compile against those versions with the checked-in
  wrapper.
- observation: `checkDebugAarMetadata` refuses them: Compose 1.12 requires
  Android Gradle plugin 9.1.0 and `compileSdk 37`. This toolchain ships AGP
  8.13.2 and SDK 36 only.
- verdict: shelved. The runner is pinned to Compose 1.9.1 / Material 3 1.3.2 /
  Kotlin 2.2.20, which AGP 8.13.2 accepts at `compileSdk 36`.
- evidence: none; the failure is reproducible with
  `./gradlew :app:checkDebugAarMetadata`.
- follow-ups: revisit when the lab toolchain carries AGP 9.x and SDK 37.
