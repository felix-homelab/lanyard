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

## Non-goals in v0.1

- persistent device daemon
- arbitrary file transfer
- remote ADB port forwarding
- graphical UI
- exploit delivery

These can be added as explicit tools later; none should bypass `Config`.
