# Research protocol

Lanyard is a lab bench. It exists to reproduce, measure and document Android
privilege and post-exploitation primitives on hardware the operator owns or is
explicitly authorized to administer.

Three rules govern every change in this repository:

1. **Authorized targets only.** A payload is gated to an exact, verified device
   row. Nothing here is written for a device you do not own or administer.
2. **Everything tried is documented**, including failures, dead ends and shelved
   ideas. A negative result is a result; an undocumented attempt is a safety
   defect, not a shortcut.
3. **Payloads are first-party.** A payload lives in this repository, is loaded
   from this repository, and carries a `dev.lanyard.payload.*` id. Work derived
   from public research keeps that attribution in its manifest and its payload
   README, but it is not shipped under someone else's id.

## What this repository is not

- not a product, and not a distribution channel for an exploit;
- not a stealth tool: no persistence past a boot, no hidden daemon, no quiet
  privilege grant;
- not a target-discovery tool: it does not scan, enumerate or attack devices
  that have not been explicitly authorized;
- not a data-exfiltration tool: no payload here reads or ships user data;
- not a bypass of its own gate: there is no override switch, no "try anyway",
  and no payload that runs on an unverified row.

## Authorization boundary

An attempt may proceed only when all of the following hold:

- the device is owned by the operator, or written authorization exists and is
  recorded in the attempt's log entry;
- the serial number is known and allowlisted by the host tool (or the lab runner
  is gated to that exact manifest row);
- the device can be physically reached for recovery (button, cable, recovery
  media) and the operator is present;
- the attempt declares what it changes, whether it survives a reboot, and what
  the recovery path is.

If any of these fail, the attempt is shelved and logged as shelved. It is not
"tested later on a spare".

## The attempt lifecycle

```text
idea -> gated -> running -> evidence -> verdict -> disposition
```

| Status | Meaning |
| --- | --- |
| `idea` | Recorded, not implemented. No code, no bump. |
| `gated` | Implemented, verified against the manifest gate, never executed. |
| `running` | An attempt is in flight; the device is on the bench. |
| `verified` | A measured result exists, with evidence. |
| `failed` | It ran and did not achieve the claim. Evidence still required. |
| `shelved` | Blocked or abandoned; reason recorded. |
| `retired` | Superseded or no longer usable; kept for the record. |

A payload never moves to `verified` on the strength of a README. It moves when
an evidence entry under `docs/evidence/` records the observation.

## Naming and layout

Every payload is a directory with a first-party id:

```text
device-app/app/src/main/assets/payloads/<id>/
├── manifest.json          id, entries, gate rows, capabilities, provenance
└── <entry>.bin            payload bytes

docs/payloads/<id>.md      hypothesis, target, procedure, trigger, cleanup, limits
```

Documentation stays out of the APK: the assets directory carries only what the
app loads, and the payload README lives beside the rest of the research record.

| Kind | Id pattern | Example |
| --- | --- | --- |
| Lanyard payload | `dev.lanyard.payload.<slug>` | `dev.lanyard.payload.pagecache-relay` |
| CVE reproduction | `dev.lanyard.payload.cve-<year>-<cve>-<slug>` | `dev.lanyard.payload.cve-2024-43093-<slug>` |

The manifest id, the asset directory name and the registry row must agree. An
id that belongs to another project means the artifact was vendored, not
implemented here, and it does not pass review.

Derived work carries a machine-readable `upstream` block:

```json
"upstream": {
  "id": "org.opencrown.payload.pagecache-relay",
  "project": "Open Crown",
  "commit": "fcaadff4c1a75da05ba0cb1736f69d0037d239ef",
  "license": "MIT",
  "note": "First-party Lanyard payload built from the published research."
}
```

## Payload contract

A payload ships only if its manifest declares:

- `id` in the Lanyard namespace, and a `version`;
- one `entry` and `entry_size`, with any extra entries listed in its README;
- `capabilities` that match what it actually grants (no aspirational labels);
- `persistence` (`boot` is the maximum accepted here) and `reversible`;
- `requirements.abis`, `android_min_sdk` and `kernel_pattern`;
- at least one `devices` row with `status: verified` for the exact model,
  device codename, build and kernel that were actually measured. Candidate rows
  may exist, but they never enable a run.
- a trigger that lives outside the app. A payload that arms itself is rejected;
- if it reproduces a CVE, a `"cve": "CVE-YYYY-NNNNN"` field beside the
  `upstream` block, and a matching row in [`CVES.md`](CVES.md).

The lab runner checks the ABI, SDK floor and kernel family before it considers a
verified row, so a verified entry can never mask an unsupported device.

## Device facts

A device record is the fixed point the rest of the record set points at. Before
the first attempt on a device, capture it into [`DEVICES.md`](DEVICES.md):

```sh
adb -s <serial> get-serialno
adb -s <serial> shell uname -a
adb -s <serial> shell getenforce
adb -s <serial> shell id
adb -s <serial> shell getprop > docs/evidence/<date>-<device>-props.txt
```

The record must carry, at minimum:

| Group | Fields |
| --- | --- |
| Identity | manufacturer, brand, model, codename (`ro.product.device`), product, board, hardware, SoC, bootloader, first API level |
| Build | full fingerprint, build id, display id, incremental, type, tags, Android release, SDK, security patch, build date |
| Kernel | release (`uname -r`), full version string, family, arch, ABI and supported ABI list, page size |
| Security | `ro.secure`, `ro.debuggable`, `ro.adb.secure`, SELinux mode, shell context, `su` provider, root plane |
| Partitions | system, vendor and odm fingerprints, with a note where they disagree |
| Gate | which registry row this device satisfies, and what the runner displayed |

Keep the raw capture as a file under `docs/evidence/`; the table is a summary of
it. The lab runner shows the same values in **About -> Device record** and
**Copy device facts** exports them as `key=value` lines.

## Documentation duties

Every attempt touches six records:

| Record | Where | Required |
| --- | --- | --- |
| Device record | [`DEVICES.md`](DEVICES.md) | before the first attempt on that device |
| CVE row | [`CVES.md`](CVES.md) | when a payload reproduces or claims a CVE; a "none claimed" row otherwise |
| Registry row | [`PAYLOADS.md`](PAYLOADS.md) | before the payload can be loaded by the app |
| Log entry | [`RESEARCH_LOG.md`](RESEARCH_LOG.md) | before the attempt starts |
| Evidence entry | [`docs/evidence/`](evidence/) | before any result is claimed |
| Payload README | `docs/payloads/<id>.md` | with the payload |

A change that touches an exploit artifact without updating these records is
incomplete, even if it compiles.

## Evidence rules

Evidence is what makes a claim reviewable:

- the device id from [`DEVICES.md`](DEVICES.md), and any field that changed since
  the record was captured;
- device facts: model, codename, build fingerprint, kernel release, SDK, ABI;
- the exact APK or tool build, including its SHA-256;
- the command or gesture that caused the change, verbatim;
- raw output, not a summary, including `errno` and SELinux context when they
  matter;
- the marker and socket state before and after;
- screenshots when the claim is about the UI, and logcat when it is not;
- what was deliberately **not** done.

Statements that are not backed by an evidence entry are labelled as untested in
the README that makes them.

## Disclosure and handling

- A CVE reproduction is for a device the operator owns. Check the disclosure
  status of the vulnerability before writing anything public about it.
- Do not publish an exploit artifact for an unfixed third-party system, and do
  not file it as a "demo" in this repository while an embargo is active.
- When a finding is new, coordinate with the vendor first; record the
  coordination state in the registry row and the log entry.
- The research log is a record of what was tried, not an instruction manual for
  a third party's device.

## Cleanup

After every attempt:

1. trigger nothing further; note the marker and socket state;
2. reboot, and confirm the patch is gone (`/dev/.dfi`, `/dev/.dfib` absent);
3. remove debug builds that are no longer needed;
4. close the log entry with the verdict and the cleanup observation.

## Definition of done

An attempt is complete when all of these are true:

- [ ] device record exists in [`DEVICES.md`](DEVICES.md) and matches the gate row;
- [ ] CVE row exists in [`CVES.md`](CVES.md), or a "none claimed" row explains the absence;
- [ ] registry row exists and matches the manifest id;
- [ ] log entry has authorization, hypothesis, procedure, verdict;
- [ ] evidence entry has build hash, device facts and raw output;
- [ ] payload README lists target, trigger, persistence, limits, provenance;
- [ ] the app gate still refuses an unverified device;
- [ ] cleanup is recorded;
- [ ] no claim in a README outruns its evidence.

## Adding a payload

1. Confirm the target device has a record in [`DEVICES.md`](DEVICES.md); capture
   one first if it does not.
2. Write the payload under `device-app/app/src/main/assets/payloads/<id>/`.
3. Add `manifest.json` with an exact verified row for the device you measured,
   and a `cve` field if it reproduces one. Add the [`CVES.md`](CVES.md) row in the
   same change.
4. Add the payload README at `docs/payloads/<id>.md` using the template below.
5. Register the payload in [`PAYLOADS.md`](PAYLOADS.md).
6. Open a log entry in [`RESEARCH_LOG.md`](RESEARCH_LOG.md).
7. Try it on the bench and write the evidence entry.
8. Update the registry status and the log entry together.

### Payload README template

```md
# <name>

- id: `dev.lanyard.payload.<slug>`
- version:
- domain: init / untrusted_app / bluetooth / other
- persistence: boot | none
- reversible: reboot

## Hypothesis

## Target

| model | build | kernel | sdk | status |
| --- | --- | --- | --- | --- |

## Procedure

## Trigger (outside the app)

## Observed

## Limits and known blockers

## Provenance

## Evidence

## Cleanup
```

### Research log entry template

```md
## RL-0000 - <title>

- date:
- status:
- payload:
- target:
- authorization:
- hypothesis:
- procedure:
- observation:
- verdict:
- evidence:
- follow-ups:
```

### Evidence entry template

```md
# <date> - <attempt>

- device:
- build:
- artifact: <name> sha256:<hash>
- operator gesture:
- raw output:
- not done:
- cleanup:
```
