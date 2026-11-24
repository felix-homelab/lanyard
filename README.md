# Lanyard

**A tethered root plane for Android, exposed as an MCP shell.**

> A lanyard keeps a device attached to you. Lanyard does the same for an
> authorized Android phone: it tethers the phone over ADB, exposes a typed
> remote shell through the Model Context Protocol, and keeps root behind an
> explicit policy gate.

Lanyard is for **your own devices, lab devices, and devices you are explicitly
authorized to administer**. It is not a stealth tool and does not bundle an
Android exploit.

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

## Root model

Lanyard does **not** ship a privilege-escalation exploit. Root is provided by
the device:

- **`su` provider** — an existing Magisk/KernelSU/APatch `su` binary.
- **`adb root` provider** — only for debuggable/userdebug builds where the
  platform permits it.
- **external provider** — an operator-supplied, explicitly authorized adapter.

If the phone only has a temporary init-domain exploit relay, Lanyard will report
the UID/context and refuse to call it a root plane. That is deliberate: a shell
that cannot create a Bluetooth socket or change SELinux context is not the same
as rooted Android.

See [docs/ROOTING.md](docs/ROOTING.md).

## Safety contract

- Authorized devices only.
- Serial allowlist by default.
- Root shell requires both `--allow-root` and a real provider.
- Commands have timeouts and output caps.
- Privileged actions are hashed into a JSONL audit log.
- No persistence, no boot integration, no hidden daemon.
- No exploit code is included.

Read [SECURITY.md](SECURITY.md) and [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md).

## Status

Alpha. Device discovery, policy, root probe, bounded shell, audit and MCP stdio
are implemented and unit-tested. TCP tether is implemented through ADB and has
not been hardware-verified in this repository yet. Root providers beyond `su`
and `adb root` are adapters, not bundled exploits.

## License

MIT.
