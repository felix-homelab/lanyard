# Lanyard security model

## Assets

- operator workstation
- authorized Android devices
- local audit log
- MCP client and its credentials

## Trust boundaries

1. **Workstation → ADB** — the workstation user can already control attached
   devices. Lanyard reduces accidental scope, it does not replace OS access
   control.
2. **ADB → device shell** — commands run in the device shell domain. Root is a
   separate `su` provider and must be explicitly enabled.
3. **MCP client → Lanyard** — tools are typed, serial allowlisted, bounded and
   audited. The MCP client is not trusted to be safe by itself.

## Device lab runner (separate artifact)

`device-app/` is a manually built, debug-signed Android app. It is not part of
the host package, and the MCP server never installs, starts, or communicates
with it. It packages the first-party
`dev.lanyard.payload.pagecache-relay` payload (derived from Open Crown, with the
upstream id kept in its manifest) and the native relay for the exact SM-A528B /
`A528BXXSBGYI3` / `5.4.254-qgki` target.

Controls specific to that app:

- exact payload-manifest row; no override; arm controls inert otherwise;
- the ABI, SDK floor and kernel family are checked before any verified row, so an
  unsupported device can never be reported as verified;
- a 1.5 s hold-to-arm gesture, then a per-run consent sheet naming payload,
  version, family, capabilities, persistence, target, trigger and reset risk;
- no automatic execution and no runtime payload input;
- no network service, no remote command interface, no generic shell;
- memory-only; reboot clears it; no boot or partition write;
- the app only stages; it cannot trigger init or perform a GATT read;
- debug certificate; not a distribution artifact.

Residual risk: a deliberate run patches a root-executed library in memory and can
reset the device. The init relay is an unauthenticated raw-syscall RPC that never
returns to init. It is a measured research artifact, not a production agent. Use
it only on an authorized lab device with a recovery path.

## Research artifacts

The payloads in this repository are research artifacts, and they follow the
protocol in [`docs/RESEARCH.md`](docs/RESEARCH.md):

- authorized targets only; a payload is gated to an exact measured device row;
- every attempt is logged in [`docs/RESEARCH_LOG.md`](docs/RESEARCH_LOG.md),
  including failures and shelved ideas;
- every measured claim has an entry in [`docs/evidence/`](docs/evidence/);
- payloads carry a first-party `dev.lanyard.payload.*` id and are registered in
  [`docs/PAYLOADS.md`](docs/PAYLOADS.md); derived work keeps its upstream id,
  project, commit and licence in the manifest `upstream` block;
- no artifact here is intended for a device the operator does not own or
  administer, and none is a distribution artifact.

## Non-goals

- the MCP server does not acquire, install, or deliver exploits
- no stealth or persistence
- no bypassing an authorization prompt
- no multi-tenant device-sharing service
- no arbitrary TCP listener

## Controls

- Serial allowlist; empty means deny, `--allow-all` is explicit.
- Root disabled unless `--allow-root`.
- Commands bounded by timeout and byte limit.
- Root probe reports UID and SELinux context separately.
- Audit records hashes of commands, not full command text.
- TCP tether rejects wildcard addresses and invalid ports.
- The default policy never starts a daemon.

## Residual risk

The MCP client can issue destructive commands against an allowed device. The
operator must scope serials and use a lab device. There is no undo. Lanyard
prints this in the policy path; it cannot prevent an authorized operator from
misusing an authorized device.

The device lab runner adds a second residual risk: an operator who confirms its
consent sheet can patch the phone's page cache for the current boot. The
manifest gate prevents a non-matching target, and the app cannot trigger the
payload itself, but a successful run can leave init unstable until reboot. The
runner is outside the MCP server's policy and audit path by design.
