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

## Non-goals

- no exploit acquisition or delivery
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
