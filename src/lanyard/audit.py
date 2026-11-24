"""Append-only JSONL audit trail for privileged actions."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path


@dataclass
class AuditLog:
    path: Path | None

    def record(
        self,
        *,
        event: str,
        serial: str,
        as_root: bool,
        ok: bool,
        duration_ms: int | None = None,
        command: str | None = None,
        detail: str | None = None,
    ) -> None:
        if self.path is None:
            return
        payload = {
            "ts": datetime.now(timezone.utc).isoformat(),
            "event": event,
            "serial": serial,
            "as_root": as_root,
            "ok": ok,
        }
        if duration_ms is not None:
            payload["duration_ms"] = duration_ms
        if command is not None:
            payload["command_sha256"] = hashlib.sha256(command.encode()).hexdigest()
            payload["command_length"] = len(command)
        if detail:
            payload["detail"] = detail[:500]
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self.path.open("a", encoding="utf-8") as handle:
            handle.write(json.dumps(payload, sort_keys=True) + "\n")
