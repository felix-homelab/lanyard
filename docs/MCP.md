# MCP interface

Lanyard uses MCP over stdio with JSON-RPC 2.0. Messages use the standard
`Content-Length` frame. A single-line JSON object is also accepted for
development clients.

## Initialize

```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"0"}}}
```

Response includes `protocolVersion`, `capabilities.tools`, and server info.

## Tools

### `lanyard_devices`

No arguments. Returns attached devices and states.

### `lanyard_device_info`

`{"serial":"..."}`. Reads model/build/SELinux properties.

### `lanyard_root_status`

`{"serial":"..."}`. Requires the server's `--allow-root` policy, then runs
`id; cat /proc/self/attr/current; getenforce` through `su -c`. A uid-0 result in `u:r:init:s0` is reported as init-domain root, not as
a general root plane.

### `lanyard_shell`

```json
{"serial":"...","command":"getprop ro.product.model","as_root":false,"timeout_s":30}
```

`as_root` is rejected unless the server was started with `--allow-root`.

### `lanyard_tether_connect` / `lanyard_tether_disconnect`

```json
{"host":"192.168.1.50","port":5555}
```

Wildcard addresses, malformed hosts and invalid ports are rejected.

## Failure format

Policy and transport errors are normal tool results:

```json
{"content":[{"type":"text","text":"device ... is not allowed"}],"isError":true}
```

Protocol errors use JSON-RPC error objects. No traceback is written to stdout.
