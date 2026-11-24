# Tether rooting model

Lanyard separates three questions that are often conflated:

1. **Transport** — is the phone reachable over ADB (USB or TCP)?
2. **UID** — does a command run as uid 0?
3. **SELinux domain** — what can uid 0 actually do?

## Transport

```sh
lanyard --allow-all tether connect 192.168.1.50 5555
lanyard --allow-all devices
```

Only use a host you control. `0.0.0.0`/`::` are rejected.

## Root probe

```sh
lanyard --allow-serial R5CR916B48N --allow-root root-status R5CR916B48N
```

Expected healthy output from a real root solution:

```text
uid=0
selinux_context=u:r:su:s0
enforcing=Enforcing
```

A page-cache/init exploit may produce:

```text
uid=0
selinux_context=u:r:init:s0
```

That is not the same capability. It cannot necessarily open a Bluetooth HCI
socket, change SELinux context, or start a normal root daemon. Lanyard reports
the distinction instead of treating “uid 0” as victory.

## Root providers

`su` is the default provider. It invokes the device's existing `su` binary.
`adb-root` is available for userdebug builds. An external provider can be added
in `src/lanyard/root.py`; it remains subject to `--allow-root` and serial policy.

## What Lanyard deliberately does not do

- ship an exploit
- use a hidden persistence mechanism
- grant root through the MCP server itself
- pretend a temporary init shell is a general root plane

For research devices with a custom exploit chain, the correct integration is an
external root provider that yields a real domain, not a workaround inside the
MCP protocol.
