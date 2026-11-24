# CVE register

Every payload that reproduces, exercises or is derived from a disclosed
vulnerability gets a row here. A payload with **no** CVE says so explicitly:
silence is not an answer, because a reader cannot tell a research primitive from
an unattributed exploit.

- Register: the table below, one row per CVE.
- Payload mapping: the CVE column in [`PAYLOADS.md`](PAYLOADS.md).
- Policy and naming: [`RESEARCH.md`](RESEARCH.md).
- Evidence: [`evidence/`](evidence/).

## Status of this register

- attributed CVEs: **0**
- explicit "none claimed" entries: **1**
  (`dev.lanyard.payload.pagecache-relay`)
- embargoed entries: **0**

When the register outgrows this page, each CVE moves to
`docs/cves/CVE-YYYY-NNNNN.md` and this file keeps the index row.

## Register

| CVE | component | class | CVSS | fix / bulletin | payload | status |
| --- | --- | --- | --- | --- | --- | --- |
| _none claimed_ | see below | page-cache write (XFRM ESP-in-UDP + CBC) | - | not attributed | [`dev.lanyard.payload.pagecache-relay`](payloads/dev.lanyard.payload.pagecache-relay.md) | research artifact |

## Entries

### dev.lanyard.payload.pagecache-relay - no CVE claimed

This payload stages a 576-byte patch into a root-executed library's page cache
through an IPsec/UDP encapsulation socket. It is a research artifact, and this
repository does **not** attribute a CVE to it.

What is known, from the repository and the upstream tree:

- upstream: Open Crown, commit `fcaadff4c1a75da05ba0cb1736f69d0037d239ef`, MIT;
- the upstream manifest, payload README and vendored relay name no CVE;
- the payload's targets are `libbase.so` (`LogMessage::~LogMessage`) and
  `libbluetooth_jni.so` (`gatt_act_read`): the effect is on the device, not a
  claim about a defect identifier;
- the behaviour is a page-cache overwrite of an executable mapping, escalated by
  the fact that the mapped library is executed by `init` or the Bluetooth stack.

Consequences:

- the id stays `dev.lanyard.payload.pagecache-relay`, not
  `dev.lanyard.payload.cve-…`;
- no README, manifest, log entry or commit may describe it as "the CVE" for this
  build, and no CVE number may be attached to it without the conditions below;
- if a CVE is later identified, the register gains a row, the manifest gains a
  `cve` field, the payload id is renamed into the CVE namespace, the payload
  README gains the CVE section, and the change is recorded in the research log.

#### What attributing a CVE would require

1. A CVE record (cve.org, NVD or a vendor advisory) naming the affected
   component and version range.
2. The affected range covering the verified device row
   ([`DEV-0001`](DEVICES.md#dev-0001---samsung-galaxy-a52s-5g-sm-a528b)).
3. A patch, bulletin or commit that shows the fix, and therefore what was
   vulnerable.
4. An evidence entry that demonstrates the behaviour on that recorded device.
5. The register row, manifest `cve` field, payload id and payload README updated
   together.

Until all five hold, the honest label is "research primitive, unattributed".
Technique similarity is not attribution: two payloads can use the same page-cache
primitive against different defects, and a vendor advisory for one of them says
nothing about the other.

#### Device exposure

[`DEV-0001`](DEVICES.md#dev-0001---samsung-galaxy-a52s-5g-sm-a528b) runs Android
14 with the 2025-09-01 security patch. Whether any particular CVE applies to it
is a property of that CVE's affected range, not of this register; the register
records only what this repository has actually attributed and measured.

## What a full CVE record contains

A row is a pointer; the substance lives in the row's section. "Fully documented"
means all of these groups, not just an id:

| Group | Fields |
| --- | --- |
| Advisory | CVE id, title, cve.org/NVD/vendor links, CWE, CVSS score and vector, publication date, last modified |
| Affected | component (library, service, kernel subsystem), versions/builds/kernels in range, whether the bench device falls inside it |
| Fix | patch commit, Android security bulletin month, vendor build that carries it, how the fix was confirmed |
| Preconditions | what the payload needs on the device to reach the vulnerable state (permissions, domain, mapped file, live service), stated at record level rather than as a recipe |
| Effect | what the reproduction demonstrates (code-execution domain, read/write, denial of service), and what it does not |
| Reliability | observed success rate and the conditions it varied with |
| Side effects | crashes, resets, watchdog, log noise, anything left behind |
| Detection | what would show this being exercised: audit, SELinux denials, logcat, marker files |
| Remediation | the patch level that removes it, and how to verify that on the device |
| Disclosure | coordination state (public, coordinated, embargoed) and what may be published here |
| Reproduction | payload id, device record, entry, trigger, persistence, cleanup, evidence entry |

A payload that cannot fill the **Advisory** and **Affected** groups does not get a
CVE id in its name. That is the line between an attributed reproduction and a
research primitive.

## CVE row template

```md
### CVE-YYYY-NNNNN - <short title>

| field | value |
| --- | --- |
| advisories | cve.org / NVD / vendor bulletin |
| component | library, service or kernel subsystem |
| class | use-after-free, out-of-bounds write, page-cache overwrite, ... |
| affected | version, build or kernel range |
| CVSS | score and vector |
| fix | patch commit or bulletin month |
| exploit status | KEV, public PoC, embargoed |
| payload id | dev.lanyard.payload.cve-<year>-<cve>-<slug> |
| reproduction | what the payload does, trigger, persistence, cleanup |
| device row | which DEV-#### falls inside the affected range |
| status | idea / gated / running / verified / failed / shelved / retired |
| evidence | docs/evidence/... |
| disclosure | coordination state and embargo window |
```

The payload README's CVE section repeats the Advisory, Affected, Fix and
Disclosure groups; it does not need to repeat the reproduction block, which
belongs to the payload README proper.

## Rules

- **No claim without a record.** A CVE number appears in this repository only if
  this file carries its row, with advisories and an affected range.
- **Naming follows attribution.** A CVE reproduction is named
  `dev.lanyard.payload.cve-<year>-<cve>-<slug>`; a research primitive keeps a
  plain `dev.lanyard.payload.<slug>` id.
- **Manifest carries the mapping.** A CVE reproduction adds `"cve": "CVE-…"` to
  its manifest next to the `upstream` block.
- **Embargo respected.** An embargoed CVE gets a row with the disclosure state
  recorded and no exploitation detail; the artifact stays out of the tree until
  coordination allows it.
- **Evidence before `verified`.** A row reaches `verified` only with an evidence
  entry on a recorded device.
- **Rows are retired, never deleted.** A withdrawn or superseded attribution
  keeps its row with a `retired` status and the reason.
- **No third-party targeting.** A CVE row describes a defect and a lab
  reproduction; it never describes how to reach a device the operator does not
  own.
