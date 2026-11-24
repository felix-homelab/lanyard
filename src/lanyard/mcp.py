"""MCP stdio server for the Lanyard tether.

The implementation is intentionally dependency-free. It speaks JSON-RPC 2.0
with MCP's Content-Length framing and exposes only the Lanyard tools.
"""

from __future__ import annotations

from dataclasses import dataclass
import json
import sys
from typing import Any, BinaryIO

from . import __version__
from .adb import Adb, AdbError
from .audit import AuditLog
from .config import Config, PolicyError
from .root import SuProvider


PROTOCOL_VERSION = "2024-11-05"


@dataclass
class RpcError(Exception):
    code: int
    message: str


TOOLS = [
    {
        "name": "lanyard_devices",
        "description": "List attached ADB devices and their transport state.",
        "inputSchema": {"type": "object", "properties": {}, "additionalProperties": False},
    },
    {
        "name": "lanyard_device_info",
        "description": "Read model, build, SELinux and root-provider information for one device.",
        "inputSchema": {
            "type": "object",
            "properties": {"serial": {"type": "string"}},
            "required": ["serial"],
            "additionalProperties": False,
        },
    },
    {
        "name": "lanyard_root_status",
        "description": "Probe su/uid/SELinux context without obtaining or changing root.",
        "inputSchema": {
            "type": "object",
            "properties": {"serial": {"type": "string"}},
            "required": ["serial"],
            "additionalProperties": False,
        },
    },
    {
        "name": "lanyard_shell",
        "description": "Run one bounded shell command on one device.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "serial": {"type": "string"},
                "command": {"type": "string"},
                "as_root": {"type": "boolean", "default": False},
                "timeout_s": {"type": "integer", "minimum": 1, "maximum": 300},
            },
            "required": ["serial", "command"],
            "additionalProperties": False,
        },
    },
    {
        "name": "lanyard_tether_connect",
        "description": "Connect ADB over TCP to an authorized host and port.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "host": {"type": "string"},
                "port": {"type": "integer", "minimum": 1, "maximum": 65535},
            },
            "required": ["host", "port"],
            "additionalProperties": False,
        },
    },
    {
        "name": "lanyard_tether_disconnect",
        "description": "Disconnect an ADB TCP transport.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "host": {"type": "string"},
                "port": {"type": "integer", "minimum": 1, "maximum": 65535},
            },
            "required": ["host", "port"],
            "additionalProperties": False,
        },
    },
]


class LanyardMcpServer:
    def __init__(
        self,
        config: Config,
        *,
        adb: Adb | None = None,
        audit: AuditLog | None = None,
        stdin: BinaryIO | None = None,
        stdout: BinaryIO | None = None,
        stderr: BinaryIO | None = None,
    ):
        self.config = config
        self.adb = adb or Adb(config.adb_path, config.timeout_s, config.output_limit)
        self.audit = audit or AuditLog(config.audit_path)
        self.stdin = stdin or sys.stdin.buffer
        self.stdout = stdout or sys.stdout.buffer
        self.stderr = stderr or sys.stderr.buffer
        self._initialized = False

    def run(self) -> int:
        while True:
            message = self._read_message()
            if message is None:
                return 0
            if "method" not in message:
                continue
            method = str(message.get("method"))
            msg_id = message.get("id")
            params = message.get("params") or {}
            try:
                if method.startswith("notifications/"):
                    continue
                result = self._dispatch(method, params)
                if msg_id is not None:
                    self._write({"jsonrpc": "2.0", "id": msg_id, "result": result})
            except RpcError as exc:
                if msg_id is not None:
                    self._write(
                        {
                            "jsonrpc": "2.0",
                            "id": msg_id,
                            "error": {"code": exc.code, "message": exc.message},
                        }
                    )
            except Exception:
                # Never leak a traceback into the protocol stream. The message
                # is safe for clients; details belong in local logs.
                if msg_id is not None:
                    self._write(
                        {
                            "jsonrpc": "2.0",
                            "id": msg_id,
                            "error": {"code": -32603, "message": "internal Lanyard error"},
                        }
                    )

    def _dispatch(self, method: str, params: dict[str, Any]) -> dict[str, Any]:
        if method == "initialize":
            self._initialized = True
            return {
                "protocolVersion": PROTOCOL_VERSION,
                "capabilities": {"tools": {}},
                "serverInfo": {"name": "lanyard", "version": __version__},
            }
        if method == "ping":
            return {}
        if method == "tools/list":
            return {"tools": TOOLS}
        if method == "tools/call":
            return self._call_tool(params)
        if method in {"resources/list", "prompts/list"}:
            return {"resources": []} if method.startswith("resources") else {"prompts": []}
        raise RpcError(-32601, f"method not found: {method}")

    def _call_tool(self, params: dict[str, Any]) -> dict[str, Any]:
        if not self._initialized:
            raise RpcError(-32000, "initialize required")
        name = params.get("name")
        arguments = params.get("arguments") or {}
        if not isinstance(arguments, dict):
            raise RpcError(-32602, "arguments must be an object")
        try:
            if name == "lanyard_devices":
                text = self._devices()
            elif name == "lanyard_device_info":
                text = self._device_info(self._string(arguments, "serial"))
            elif name == "lanyard_root_status":
                text = self._root_status(self._string(arguments, "serial"))
            elif name == "lanyard_shell":
                text = self._shell(arguments)
            elif name == "lanyard_tether_connect":
                text = self._tether(arguments, connect=True)
            elif name == "lanyard_tether_disconnect":
                text = self._tether(arguments, connect=False)
            else:
                raise RpcError(-32602, f"unknown tool: {name}")
            return {"content": [{"type": "text", "text": text}], "isError": False}
        except (PolicyError, AdbError, ValueError) as exc:
            return {
                "content": [{"type": "text", "text": str(exc)}],
                "isError": True,
            }

    def _devices(self) -> str:
        devices = self.adb.devices()
        if not devices:
            return "no devices attached"
        lines = []
        for device in devices:
            bits = [f"{device.serial}  {device.state}"]
            if device.model:
                bits.append(f"model={device.model}")
            if device.device:
                bits.append(f"device={device.device}")
            if device.product:
                bits.append(f"product={device.product}")
            lines.append("  ".join(bits))
        return "\n".join(lines)

    def _device_info(self, serial: str) -> str:
        self.config.ensure_serial(serial)
        props = {
            "model": "ro.product.model",
            "release": "ro.build.version.release",
            "sdk": "ro.build.version.sdk",
            "fingerprint": "ro.build.fingerprint",
            "selinux": "ro.boot.selinux",
        }
        lines = [f"serial: {serial}"]
        for label, prop in props.items():
            try:
                value = self.adb.getprop(serial, prop)
            except AdbError as exc:
                value = f"<{exc}>"
            lines.append(f"{label}: {value}")
        return "\n".join(lines)

    def _root_status(self, serial: str) -> str:
        self.config.ensure_serial(serial)
        self.config.ensure_root()
        probe = SuProvider().probe(self.adb, serial)
        self.audit.record(
            event="root_probe",
            serial=serial,
            as_root=False,
            ok=probe.available,
            detail=probe.detail,
        )
        return probe.render()

    def _shell(self, arguments: dict[str, Any]) -> str:
        serial = self._string(arguments, "serial")
        command = self._string(arguments, "command")
        as_root = bool(arguments.get("as_root", False))
        timeout_s = int(arguments.get("timeout_s") or self.config.timeout_s)
        timeout_s = max(1, min(timeout_s, 300))
        self.config.ensure_serial(serial)
        if as_root:
            self.config.ensure_root()
        result = self.adb.shell(serial, command, as_root=as_root, timeout_s=timeout_s)
        self.audit.record(
            event="shell",
            serial=serial,
            as_root=as_root,
            ok=result.ok,
            duration_ms=result.duration_ms,
            command=command,
            detail=None if result.ok else result.stderr[:500],
        )
        return result.render()

    def _tether(self, arguments: dict[str, Any], *, connect: bool) -> str:
        host = self._string(arguments, "host")
        try:
            port = int(arguments.get("port"))
        except (TypeError, ValueError) as exc:
            raise ValueError("port must be an integer") from exc
        if connect:
            text = self.adb.tether_connect(host, port)
            event = "tether_connect"
        else:
            text = self.adb.tether_disconnect(host, port)
            event = "tether_disconnect"
        self.audit.record(
            event=event,
            serial=f"{host}:{port}",
            as_root=False,
            ok=True,
            detail=text[:200],
        )
        return text

    @staticmethod
    def _string(arguments: dict[str, Any], key: str) -> str:
        value = arguments.get(key)
        if not isinstance(value, str) or not value:
            raise RpcError(-32602, f"missing string argument: {key}")
        return value

    def _read_message(self) -> dict[str, Any] | None:
        line = self.stdin.readline()
        if not line:
            return None
        line = line.strip()
        if not line:
            return None
        if line.startswith(b"{"):
            try:
                value = json.loads(line)
            except json.JSONDecodeError as exc:
                raise RpcError(-32700, "invalid JSON") from exc
            return value if isinstance(value, dict) else None
        headers: dict[str, str] = {}
        while line:
            decoded = line.decode("ascii", "replace")
            if ":" in decoded:
                key, value = decoded.split(":", 1)
                headers[key.strip().lower()] = value.strip()
            line = self.stdin.readline().strip()
            if not line:
                break
        length_header = headers.get("content-length")
        if length_header is None:
            raise RpcError(-32700, "missing Content-Length")
        try:
            length = int(length_header)
        except ValueError as exc:
            raise RpcError(-32700, "invalid Content-Length") from exc
        raw = self.stdin.read(length)
        if len(raw) != length:
            raise RpcError(-32700, "short MCP message")
        try:
            value = json.loads(raw)
        except json.JSONDecodeError as exc:
            raise RpcError(-32700, "invalid JSON") from exc
        return value if isinstance(value, dict) else None

    def _write(self, message: dict[str, Any]) -> None:
        raw = json.dumps(message, separators=(",", ":")).encode("utf-8")
        self.stdout.write(f"Content-Length: {len(raw)}\r\n\r\n".encode("ascii"))
        self.stdout.write(raw)
        self.stdout.flush()
