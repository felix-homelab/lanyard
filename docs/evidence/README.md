# Evidence

One entry per measured claim. The rule from [RESEARCH.md](../RESEARCH.md): a
result is not `verified` until an entry here records what was observed.

## Layout

```text
docs/evidence/YYYY-MM-DD-<slug>.md
```

Each entry records:

- device facts (model, codename, build fingerprint, kernel, SDK, ABI);
- the artifact and its SHA-256, and the payload id/version;
- the operator gesture or command that caused the change, verbatim;
- raw output rather than a summary, including `errno` and SELinux context when
  they matter;
- marker and socket state before and after;
- what was deliberately **not** done;
- cleanup and the state the device was left in;
- when the attempt reproduces a CVE, the row from [`../CVES.md`](../CVES.md) that
  it demonstrates (or an explicit "none claimed").

Raw captures (`getprop`, `uname`, `getenforce`, `id`) are kept next to the
entries that cite them, as `.txt` files. The structured summary of a device lives
in [`../DEVICES.md`](../DEVICES.md).

## Index

| date | attempt | payload | outcome |
| --- | --- | --- | --- |
| 2026-11-24 | [page-cache relay on the A52](2026-11-24-pagecache-relay-a52.md) | `dev.lanyard.payload.pagecache-relay` 0.1.0 | gate verified, probe ready, **no write executed** |

## Raw artifacts

| file | contents |
| --- | --- |
| [`2026-11-24-a52-device-facts.txt`](2026-11-24-a52-device-facts.txt) | serial, `uname -a`, `getenforce`, shell `id`, `su` probe and the full `getprop` dump of DEV-0001 |
