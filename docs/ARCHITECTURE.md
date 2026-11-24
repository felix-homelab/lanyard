# Architecture

```text
 MCP client
    │ stdio JSON-RPC + Content-Length
    ▼
 Lanyard MCP server
    ├── policy (serial allowlist, root switch, timeout, output cap)
    ├── audit (JSONL, command hashes)
    ├── ADB transport
    │     ├── USB / existing adb server
    │     └── TCP tether (adb connect)
    └── root provider
          ├── su
          ├── adb-root
          └── external adapter (operator supplied)
```

## Layers

**`config.py`** owns policy. It is immutable after construction. An empty
allowlist denies every device; `--allow-all` is an explicit lab mode.

**`adb.py`** is a typed wrapper around the ADB executable. It uses argument
lists, never host `shell=True`, bounds output, and rejects malformed TCP
targets. `adb shell` is passed one remote command string; root commands are
`su -c <quoted>` on the device.

**`root.py`** defines the root probe and providers. It does not attempt
privilege escalation. A probe is a read-only check of `id`, SELinux context and
enforcement state.

**`mcp.py`** implements MCP over stdio. It advertises tools only; it does not
advertise resources or prompts. Tool failures are returned as `isError` content
so the client can reason about them.

**`audit.py`** appends one JSON object per action. Command text is hashed, not
stored.

## Separate device lab runner

[`device-app/`](../device-app/README.md) is a standalone, debug-only Android
app that stages the `dev.lanyard.payload.pagecache-relay` payloads on the exact
verified A52 build. It is outside this process: the MCP server never installs,
starts, or calls it, and it is not a root provider. Its own controls are an exact
manifest match, a hold-to-arm gesture plus a per-run consent sheet, no automatic
execution and no trigger path; it stages the page-cache patch and leaves the
init/GATT trigger to the operator.

Payload research follows [`RESEARCH.md`](RESEARCH.md): one record per device in
[`DEVICES.md`](DEVICES.md), one attribution row in [`CVES.md`](CVES.md) (or an
explicit "none claimed"), one registry row in [`PAYLOADS.md`](PAYLOADS.md), one
append-only entry in [`RESEARCH_LOG.md`](RESEARCH_LOG.md), one payload README
under [`payloads/`](payloads/), and an evidence entry in [`evidence/`](evidence/)
before anything is called verified. The bench changes artifacts, not the MCP
server's policy model.

## Non-goals in v0.1

- persistent device daemon
- arbitrary file transfer
- remote ADB port forwarding
- graphical UI
- exploit delivery in the MCP server or host package

These can be added as explicit tools later; none should bypass `Config`. The
separate device lab runner is outside this surface and does not change the
server's policy model.
