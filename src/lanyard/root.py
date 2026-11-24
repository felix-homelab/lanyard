"""Root providers and a read-only root probe.

Lanyard ships no exploit. Providers describe how to use an already-authorized
root capability on the device.
"""

from __future__ import annotations

from dataclasses import dataclass
import re

from .adb import Adb, ShellResult


@dataclass(frozen=True)
class RootProbe:
    available: bool
    uid: int | None
    selinux_context: str | None
    enforcing: str | None
    detail: str

    def render(self) -> str:
        return "\n".join(
            [
                f"available: {str(self.available).lower()}",
                f"uid: {self.uid if self.uid is not None else 'unknown'}",
                f"selinux_context: {self.selinux_context or 'unknown'}",
                f"enforcing: {self.enforcing or 'unknown'}",
                f"detail: {self.detail}",
            ]
        )


PROBE_COMMAND = "id; cat /proc/self/attr/current; getenforce"


class SuProvider:
    """Use an existing su binary via `su -c`."""

    name = "su"

    def probe(self, adb: Adb, serial: str) -> RootProbe:
        result = adb.shell(serial, PROBE_COMMAND, as_root=True, timeout_s=10)
        return probe_from_shell(result, provider=self.name)

    def shell(self, adb: Adb, serial: str, command: str, timeout_s: int = 30) -> ShellResult:
        return adb.shell(serial, command, as_root=True, timeout_s=timeout_s)


class AdbRootProvider:
    """Use `adb root` on debuggable/userdebug builds only.

    `adb root` is a transport property, not a universal privilege escalation.
    It is not called by `root_status`; it is only exposed as a provider for an
    explicit future action.
    """

    name = "adb-root"

    def available(self, adb: Adb) -> bool:
        result = adb._run(["root"])
        text = (result.stdout + result.stderr).decode("utf-8", "replace").lower()
        return result.returncode == 0 and "cannot" not in text and "not allowed" not in text


def probe_from_shell(result: ShellResult, *, provider: str) -> RootProbe:
    combined = (result.stdout + "\n" + result.stderr).strip()
    uid_match = re.search(r"uid=(\d+)", combined)
    uid = int(uid_match.group(1)) if uid_match else None
    context_match = re.search(r"context=([^\s]+)", combined)
    context = context_match.group(1) if context_match else None
    enforce_match = re.search(r"\b(Enforcing|Permissive|Disabled)\b", combined)
    enforcing = enforce_match.group(1) if enforce_match else None

    if result.returncode != 0:
        detail = combined[:400] or f"{provider} probe failed"
        return RootProbe(False, uid, context, enforcing, detail)
    if uid != 0:
        return RootProbe(False, uid, context, enforcing, "su did not return uid 0")
    if not context:
        return RootProbe(True, uid, context, enforcing, "uid 0, context unreadable")
    if context == "u:r:init:s0":
        return RootProbe(
            True,
            uid,
            context,
            enforcing,
            "uid 0 in init; not a general root domain (no setcon/socket escape)",
        )
    return RootProbe(True, uid, context, enforcing, f"uid 0 via {provider}")


ROOT_PROVIDERS = {
    "su": SuProvider,
    "adb-root": AdbRootProvider,
}
