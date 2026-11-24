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

Out of scope: a compromised workstation, a malicious ADB binary, a hostile
device, physical attackers, and any use against a device the operator is not
authorized to administer.
