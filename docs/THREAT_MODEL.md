# Threat model

| Threat | Control |
| --- | --- |
| MCP client targets the wrong phone | serial allowlist |
| Agent invokes root accidentally | `--allow-root` plus provider probe |
| Command runs forever | timeout, capped at 300 s |
| Command floods context | output cap per stream |
| Host-side shell injection | argument lists, no `shell=True` |
| Tether to wildcard address | rejected |
| Secret appears in audit log | commands are hashed |
| Temporary init root mistaken for root | SELinux context reported |
| Lab runner used on a non-matching build | exact manifest row; run buttons disabled |
| Lab runner run without a gesture | per-run consent sheet; no automatic execution |
| Page-cache patch destabilises init | memory-only; reboot clears; explicit warning; the operator triggers it |
| Research payload drifts to an unverified device | manifest ABI/SDK/kernel checks run before any verified row; arm controls inert |
| A unit differs from the documented device | device record captured before the first attempt; gate row must match it; evidence cites the device id |
| A payload runs without a human decision | 1.5 s hold-to-arm, then a consent sheet; no automatic or scheduled execution |
| A claim outruns its evidence | registry status only moves on an evidence entry in `docs/evidence/` |
| A derived payload ships under an upstream name | first-party `dev.lanyard.payload.*` ids; upstream kept in the manifest `upstream` block |
| A payload implies a CVE it does not reproduce | `CVES.md` row with advisories and affected range, or an explicit "none claimed" row |
| An embargoed CVE leaks through an artifact | register row carries the disclosure state; no artifact detail until coordination allows it |
| A payload persists past the bench | `persistence: boot` is the accepted maximum; cleanup step requires a reboot check |

Out of scope: a compromised workstation, a malicious ADB binary, a hostile
device, physical attackers, and any use against a device the operator is not
authorized to administer.
