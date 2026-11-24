"""Small ADB client.

The client deliberately uses argument lists, never host-side ``shell=True``.
Device output is bounded and decoded without trusting it.
"""

from __future__ import annotations

from dataclasses import dataclass
import subprocess
import time


class AdbError(RuntimeError):
    """ADB could not be executed or returned an unusable response."""


@dataclass(frozen=True)
class Device:
    serial: str
    state: str
    model: str | None = None
    device: str | None = None
    product: str | None = None
    transport_id: str | None = None

    @property
    def usable(self) -> bool:
        return self.state == "device"


@dataclass(frozen=True)
class ShellResult:
    serial: str
    command: str
    as_root: bool
    returncode: int
    stdout: str
    stderr: str
    duration_ms: int
    truncated: bool = False

    @property
    def ok(self) -> bool:
        return self.returncode == 0

    def render(self) -> str:
        parts = [
            f"serial: {self.serial}",
            f"root: {str(self.as_root).lower()}",
            f"returncode: {self.returncode}",
            f"duration_ms: {self.duration_ms}",
        ]
        if self.stdout.strip():
            parts.extend(["stdout:", self.stdout.rstrip()])
        if self.stderr.strip():
            parts.extend(["stderr:", self.stderr.rstrip()])
        if self.truncated:
            parts.append("output: truncated by Lanyard output limit")
        return "\n".join(parts)


def parse_devices(output: str) -> list[Device]:
    devices: list[Device] = []
    for raw in output.splitlines():
        line = raw.strip()
        if not line or line.startswith("List of devices"):
            continue
        if "daemon" in line.lower() or line.startswith("*"):
            continue
        fields = line.split()
        if len(fields) < 2:
            continue
        serial, state = fields[0], fields[1]
        attrs: dict[str, str] = {}
        for token in fields[2:]:
            if ":" in token:
                key, value = token.split(":", 1)
                attrs[key] = value
        devices.append(
            Device(
                serial=serial,
                state=state,
                model=attrs.get("model"),
                device=attrs.get("device"),
                product=attrs.get("product"),
                transport_id=attrs.get("transport_id"),
            )
        )
    return devices


class Adb:
    def __init__(self, adb_path: str = "adb", timeout_s: int = 30, output_limit: int = 262144):
        self.adb_path = adb_path
        self.timeout_s = timeout_s
        self.output_limit = output_limit

    def _run(self, args: list[str], timeout_s: int | None = None) -> subprocess.CompletedProcess[bytes]:
        try:
            return subprocess.run(
                [self.adb_path, *args],
                capture_output=True,
                timeout=timeout_s or self.timeout_s,
                check=False,
            )
        except FileNotFoundError as exc:
            raise AdbError(
                f"ADB executable not found: {self.adb_path!r}; set --adb or LANYARD_ADB"
            ) from exc
        except subprocess.TimeoutExpired as exc:
            raise AdbError(f"ADB command timed out after {timeout_s or self.timeout_s}s") from exc
        except OSError as exc:
            raise AdbError(f"could not start ADB: {exc}") from exc

    def devices(self) -> list[Device]:
        result = self._run(["devices", "-l"])
        if result.returncode != 0:
            raise AdbError(
                "adb devices failed: "
                + result.stderr.decode("utf-8", "replace").strip()
            )
        return parse_devices(result.stdout.decode("utf-8", "replace"))

    def tether_connect(self, host: str, port: int) -> str:
        _validate_host_port(host, port)
        result = self._run(["connect", f"{host}:{port}"])
        text = (result.stdout + result.stderr).decode("utf-8", "replace").strip()
        if result.returncode != 0 or "cannot" in text.lower() or "failed" in text.lower():
            raise AdbError(text or "adb connect failed")
        return text

    def tether_disconnect(self, host: str, port: int) -> str:
        _validate_host_port(host, port)
        result = self._run(["disconnect", f"{host}:{port}"])
        text = (result.stdout + result.stderr).decode("utf-8", "replace").strip()
        if result.returncode != 0:
            raise AdbError(text or "adb disconnect failed")
        return text

    def shell(
        self,
        serial: str,
        command: str,
        *,
        as_root: bool = False,
        timeout_s: int | None = None,
    ) -> ShellResult:
        if not command.strip():
            raise AdbError("empty command")
        remote_command = command
        if as_root:
            from shlex import quote

            # The device shell receives one quoted string. su evaluates that
            # string as root; no host-side shell is involved.
            remote_command = f"su -c {quote(command)}"

        started = time.monotonic()
        try:
            result = self._run(
                ["-s", serial, "shell", remote_command],
                timeout_s=timeout_s or self.timeout_s,
            )
            rc = result.returncode
            raw_stdout, raw_stderr = result.stdout, result.stderr
        except AdbError as exc:
            # A timeout still has a useful bounded result for the agent.
            elapsed = int((time.monotonic() - started) * 1000)
            return ShellResult(
                serial=serial,
                command=command,
                as_root=as_root,
                returncode=124,
                stdout="",
                stderr=str(exc),
                duration_ms=elapsed,
            )
        elapsed = int((time.monotonic() - started) * 1000)
        stdout, truncated_out = _decode_limited(raw_stdout, self.output_limit)
        stderr, truncated_err = _decode_limited(raw_stderr, self.output_limit)
        return ShellResult(
            serial=serial,
            command=command,
            as_root=as_root,
            returncode=rc,
            stdout=stdout,
            stderr=stderr,
            duration_ms=elapsed,
            truncated=truncated_out or truncated_err,
        )

    def getprop(self, serial: str, prop: str) -> str:
        # Property names are restricted to avoid turning this into an arbitrary
        # shell call.
        import re

        if not re.fullmatch(r"[A-Za-z0-9_.-]{1,128}", prop):
            raise AdbError("invalid property name")
        result = self._run(["-s", serial, "shell", "getprop", prop])
        return result.stdout.decode("utf-8", "replace").strip()


def _decode_limited(raw: bytes, limit: int) -> tuple[str, bool]:
    if len(raw) > limit:
        raw = raw[:limit]
        return raw.decode("utf-8", "replace"), True
    return raw.decode("utf-8", "replace"), False


def _validate_host_port(host: str, port: int) -> None:
    import ipaddress
    import re

    if not (1 <= int(port) <= 65535):
        raise AdbError("port must be 1..65535")
    if not re.fullmatch(r"[A-Za-z0-9_.:-]{1,255}", host):
        raise AdbError("invalid host")
    if host in {"0.0.0.0", "::"}:
        raise AdbError("wildcard tether addresses are not allowed")
    try:
        ipaddress.ip_address(host)
    except ValueError:
        # Hostnames are allowed; the regex above bounds them.
        pass
