# Lanyard

**A tethered root plane for Android, exposed as an MCP shell.**

> A lanyard keeps a device attached to you. Lanyard does the same for an
> authorized Android phone: it tethers the phone over ADB, exposes a typed
> remote shell through the Model Context Protocol, and keeps root behind an
> explicit policy gate.

Lanyard is for **your own devices, lab devices, and devices you are explicitly
authorized to administer**. The host tool is not a stealth tool. A separate,
debug-only lab runner under [`device-app/`](device-app/README.md) packages a
public Open Crown page-cache payload for the verified A52; it is manually
installed and is not part of the MCP server.

## Why it exists

Rooting and operating a phone from a workstation is usually a pile of shell
history: `adb devices`, `adb shell`, `su -c`, copied serials, and commands
scattered across terminals. Lanyard makes that a small, auditable service:

- discover tethered devices;
- probe whether a real `su` root domain exists;
- run a bounded remote shell;
- expose the same operations as MCP tools for an agent or IDE;
- keep every privileged action behind an allowlist, a root switch, and an
  audit log.

The important distinction is **uid 0 is not a root domain**. Lanyard reports
both the UID and the SELinux context, so a temporary init-domain exploit shell
is not mistaken for a usable root plane.

## Name

**Lanyard** — the cord that keeps the device attached, and the thing you hold
when you are working at height. The agent gets a short lease, not a permanent
credential.

## Install

```sh
python3 -m venv .venv
. .venv/bin/activate
pip install -e .
```

No third-party runtime dependencies; the MCP transport is implemented against
the standard MCP stdio framing.

## Quick start

```sh
# See attached devices. --allow-all is explicit for a lab workstation.
lanyard --allow-all devices

# Run a bounded shell command on one device.
lanyard --allow-all shell R5CR916B48N -- getprop ro.product.model

# Probe for a real su domain; does not attempt to obtain root.
lanyard --allow-all --allow-root root-status R5CR916B48N

# Run one command as root when a real su provider is available.
lanyard --allow-all --allow-root shell R5CR916B48N --as-root -- id
```

Serial allowlisting is the default. There is no implicit "all devices" mode in
the MCP server; pass `--allow-all` or explicit `--allow-serial` arguments.

## MCP

```sh
lanyard --allow-all mcp
```

Example client configuration:

```json
{
  "mcpServers": {
    "lanyard": {
      "command": "lanyard",
      "args": ["--allow-serial", "R5CR916B48N", "--allow-root", "mcp"]
    }
  }
}
```

Tools:

| Tool | Purpose |
| --- | --- |
| `lanyard_devices` | List attached ADB devices and their transport state. |
| `lanyard_device_info` | Read model, build, SELinux and root probe data. |
| `lanyard_root_status` | Probe `su`, UID and SELinux context without changing state. |
| `lanyard_shell` | Run one bounded command; `as_root` must be explicitly enabled. |
| `lanyard_tether_connect` | Connect ADB over TCP to an authorized host. |
| `lanyard_tether_disconnect` | Disconnect a TCP ADB transport. |

All tools return text content and set `isError` for policy/transport failures.
There is no arbitrary file read/write, no persistent daemon, and no reboot tool
in this first surface.

See [docs/MCP.md](docs/MCP.md).

## Device payload runner (lab, separate)

[`device-app/`](device-app/README.md) is a debug-only Android app that stages the
Open Crown page-cache payloads on the exact SM-A528B / `A528BXXSBGYI3` /
`5.4.254-qgki` build. It is not installed, started, or called by the Lanyard MCP
server, and it is not a root provider for the host.

It matches the payload manifest row exactly, requires an on-device consent
gesture for every run, and only stages the payload. The init relay is triggered
from the shell, where the relay is reachable; the app cannot connect to it. The
first execution can consume init's logging thread and reset the phone. It is
deployed on the lab A52 as `dev.lanyard.payload`.

The 0.2.0 runner is a Compose app: a verified-target gate, a press-and-hold arm
gesture followed by a consent sheet, a classified run log and an About tab with
the packaged payload hashes. The redesign changes presentation only — payload
bytes, the native relay and the target rules are untouched, and the manifest
match now checks the ABI, SDK floor and kernel family before it accepts a
verified row.

Build, install, observed evidence, provenance and limits:
[device-app/README.md](device-app/README.md).

## Research bench

This repository is also a bench. CVE reproductions, payloads and
post-exploitation primitives are developed here, on hardware the operator owns,
and every attempt stays on the record. Three rules:

1. **Authorized targets only.** A payload is gated to an exact measured device
   row. Nothing is written for a device you do not own or administer.
2. **Everything tried is documented**, including failures, dead ends and shelved
   ideas. An undocumented attempt is a defect.
3. **Payloads are first-party.** Each payload lives here and carries a
   `dev.lanyard.payload.*` id. Derived work keeps its upstream id, project and
   commit in the manifest `upstream` block, but it is not shipped under someone
   else's name.

| Record | Purpose |
| --- | --- |
| [`docs/RESEARCH.md`](docs/RESEARCH.md) | protocol: authorization, attempt lifecycle, naming, evidence rules, disclosure, cleanup, definition of done |
| [`docs/DEVICES.md`](docs/DEVICES.md) | bench inventory: model, codename, build, kernel, SoC, security state and gate row per device |
| [`docs/CVES.md`](docs/CVES.md) | CVE register: attribution per payload, affected range, fix, disclosure state, or an explicit "none claimed" |
| [`docs/PAYLOADS.md`](docs/PAYLOADS.md) | registry of every payload: target rows, capabilities, hashes, limits |
| [`docs/RESEARCH_LOG.md`](docs/RESEARCH_LOG.md) | append-only log of attempts, negative results included |
| [`docs/evidence/`](docs/evidence/) | one entry per measured claim: device facts, raw output, artifact hashes |
| [`docs/payloads/`](docs/payloads/) | one README per payload: hypothesis, procedure, trigger, cleanup |

The bench does not change the host contract: the MCP server still ships no
exploit, and the lab runner still stages rather than consumes its relay.

## Root model

The Lanyard host package does **not** ship a privilege-escalation exploit. Root
is provided by the device:

- **`su` provider** — an existing Magisk/KernelSU/APatch `su` binary.
- **`adb root` provider** — only for debuggable/userdebug builds where the
  platform permits it.
- **external provider** — an operator-supplied, explicitly authorized adapter.

If the phone only has a temporary init-domain exploit relay, Lanyard will report
the UID/context and refuse to call it a root plane. That is deliberate: a shell
that cannot create a Bluetooth socket or change SELinux context is not the same
as rooted Android.

The separate lab runner in `device-app/` is not a root provider. It stages an
Open Crown payload that yields a temporary init-domain relay; the host still
reports that as a limited context, not as a general root plane.

See [docs/ROOTING.md](docs/ROOTING.md).

## Safety contract

- Authorized devices only.
- Serial allowlist by default.
- Root shell requires both `--allow-root` and a real provider.
- Commands have timeouts and output caps.
- Privileged actions are hashed into a JSONL audit log.
- No persistence, no boot integration, no hidden daemon.
- The host package contains no exploit code.
- The separate `device-app/` lab runner is debug-only, exact-target,
  consent-gated, memory-only, and outside the MCP surface. It is never installed
  or invoked by the MCP server.
- Research artifacts follow the bench protocol: a first-party id, a registry
  row, a log entry, and an evidence entry before any result is claimed.

Read [SECURITY.md](SECURITY.md) and [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md).

## Status

Alpha. Device discovery, policy, root probe, bounded shell, audit and MCP stdio
are implemented and unit-tested. TCP tether is implemented through ADB and has
not been hardware-verified in this repository yet. Root providers beyond `su`
and `adb root` are adapters, not bundled exploits.

A debug-only device payload runner (`dev.lanyard.payload`, 0.2.0 Compose
rewrite) is built and installed on the lab A52. Its exact-target gate,
hold-to-arm consent sheet, classified log, both themes and native relay probe
were verified on 2026-11-24; the page-cache write was deliberately not triggered
during deployment. See [device-app/README.md](device-app/README.md).

## License

MIT.
