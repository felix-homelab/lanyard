"""Configuration and policy for Lanyard."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
import os


class PolicyError(RuntimeError):
    """The requested device or action is not allowed by local policy."""


DEFAULT_TIMEOUT_S = 30
MAX_TIMEOUT_S = 300
DEFAULT_OUTPUT_LIMIT = 256 * 1024


@dataclass(frozen=True)
class Config:
    """Immutable runtime policy.

    ``allowed_serials`` empty means "no devices" unless ``allow_all`` is set.
    There is intentionally no implicit permissive mode.
    """

    adb_path: str = "adb"
    allowed_serials: frozenset[str] = field(default_factory=frozenset)
    allow_all: bool = False
    allow_root: bool = False
    timeout_s: int = DEFAULT_TIMEOUT_S
    output_limit: int = DEFAULT_OUTPUT_LIMIT
    audit_path: Path | None = None

    @classmethod
    def from_env(
        cls,
        *,
        adb_path: str | None = None,
        allowed_serials: list[str] | None = None,
        allow_all: bool = False,
        allow_root: bool = False,
        timeout_s: int | None = None,
        output_limit: int | None = None,
        audit_path: str | Path | None = None,
    ) -> "Config":
        env_serials = os.environ.get("LANYARD_ALLOWED_SERIALS", "")
        configured_serials = {
            s.strip() for s in env_serials.split(",") if s.strip()
        }
        configured_serials.update(allowed_serials or [])

        env_allow_all = os.environ.get("LANYARD_ALLOW_ALL", "").lower() in {
            "1",
            "true",
            "yes",
        }
        env_allow_root = os.environ.get("LANYARD_ALLOW_ROOT", "").lower() in {
            "1",
            "true",
            "yes",
        }
        env_timeout = os.environ.get("LANYARD_TIMEOUT_S", "")
        env_output = os.environ.get("LANYARD_OUTPUT_LIMIT", "")
        env_audit = os.environ.get("LANYARD_AUDIT", "")

        resolved_timeout = timeout_s
        if resolved_timeout is None and env_timeout:
            resolved_timeout = int(env_timeout)
        resolved_output = output_limit
        if resolved_output is None and env_output:
            resolved_output = int(env_output)

        resolved_audit = audit_path or env_audit or None
        return cls(
            adb_path=adb_path or os.environ.get("LANYARD_ADB", "adb"),
            allowed_serials=frozenset(configured_serials),
            allow_all=allow_all or env_allow_all,
            allow_root=allow_root or env_allow_root,
            timeout_s=max(1, min(resolved_timeout or DEFAULT_TIMEOUT_S, MAX_TIMEOUT_S)),
            output_limit=max(4096, resolved_output or DEFAULT_OUTPUT_LIMIT),
            audit_path=Path(resolved_audit).expanduser() if resolved_audit else None,
        )

    def ensure_serial(self, serial: str) -> None:
        if not serial or "\x00" in serial or "\n" in serial:
            raise PolicyError("invalid serial")
        if self.allow_all:
            return
        if serial not in self.allowed_serials:
            allowed = ", ".join(sorted(self.allowed_serials)) or "<none>"
            raise PolicyError(
                f"device {serial!r} is not allowed; allowed serials: {allowed}"
            )

    def ensure_root(self) -> None:
        if not self.allow_root:
            raise PolicyError(
                "root execution is disabled; pass --allow-root or set LANYARD_ALLOW_ROOT=1"
            )
