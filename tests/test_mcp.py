import io
import json
import unittest

from lanyard.adb import Device, ShellResult
from lanyard.config import Config
from lanyard.mcp import LanyardMcpServer


class FakeAdb:
    def __init__(self) -> None:
        self.calls: list[tuple] = []

    def devices(self):
        return [Device("serial1", "device", model="TestPhone")]

    def getprop(self, serial, prop):
        self.calls.append(("getprop", serial, prop))
        return {"ro.product.model": "TestPhone", "ro.build.version.sdk": "34"}.get(prop, "")

    def shell(self, serial, command, *, as_root=False, timeout_s=None):
        self.calls.append(("shell", serial, command, as_root, timeout_s))
        if as_root:
            return ShellResult(
                serial=serial,
                command=command,
                as_root=True,
                returncode=0,
                stdout="uid=0(root) gid=0(root) context=u:r:su:s0\nEnforcing\n",
                stderr="",
                duration_ms=3,
            )
        return ShellResult(
            serial=serial,
            command=command,
            as_root=False,
            returncode=0,
            stdout="ok\n",
            stderr="",
            duration_ms=2,
        )

    def tether_connect(self, host, port):
        self.calls.append(("connect", host, port))
        return f"connected to {host}:{port}"

    def tether_disconnect(self, host, port):
        self.calls.append(("disconnect", host, port))
        return f"disconnected {host}:{port}"


def frame(payload: dict) -> bytes:
    raw = json.dumps(payload).encode()
    return f"Content-Length: {len(raw)}\r\n\r\n".encode() + raw


def parse_frames(raw: bytes) -> list[dict]:
    messages = []
    offset = 0
    while offset < len(raw):
        header_end = raw.index(b"\r\n\r\n", offset)
        headers = raw[offset:header_end].decode()
        length = int(headers.split(":", 1)[1].strip())
        body_start = header_end + 4
        messages.append(json.loads(raw[body_start:body_start + length]))
        offset = body_start + length
    return messages


class McpTest(unittest.TestCase):
    def run_server(self, requests, *, allow_root=True):
        config = Config(
            allowed_serials=frozenset({"serial1"}),
            allow_root=allow_root,
        )
        payload = b"".join(frame(r) for r in requests)
        stdin = io.BytesIO(payload)
        stdout = io.BytesIO()
        server = LanyardMcpServer(
            config,
            adb=FakeAdb(),
            stdin=stdin,
            stdout=stdout,
            stderr=io.BytesIO(),
        )
        server.run()
        return parse_frames(stdout.getvalue())

    def test_initialize_and_list_tools(self):
        responses = self.run_server([
            {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
            {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}},
        ])
        self.assertEqual(responses[0]["result"]["serverInfo"]["name"], "lanyard")
        tools = {t["name"] for t in responses[1]["result"]["tools"]}
        self.assertIn("lanyard_shell", tools)
        self.assertIn("lanyard_root_status", tools)

    def test_shell_policy(self):
        responses = self.run_server([
            {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
            {
                "jsonrpc": "2.0",
                "id": 2,
                "method": "tools/call",
                "params": {
                    "name": "lanyard_shell",
                    "arguments": {
                        "serial": "serial1",
                        "command": "id",
                        "as_root": True,
                    },
                },
            },
        ], allow_root=False)
        result = responses[1]["result"]
        self.assertTrue(result["isError"])
        self.assertIn("root execution is disabled", result["content"][0]["text"])

    def test_disallowed_serial(self):
        responses = self.run_server([
            {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
            {
                "jsonrpc": "2.0",
                "id": 2,
                "method": "tools/call",
                "params": {
                    "name": "lanyard_devices",
                    "arguments": {},
                },
            },
            {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "tools/call",
                "params": {
                    "name": "lanyard_shell",
                    "arguments": {"serial": "other", "command": "id"},
                },
            },
        ])
        self.assertFalse(responses[1]["result"]["isError"])
        self.assertTrue(responses[2]["result"]["isError"])
        self.assertIn("not allowed", responses[2]["result"]["content"][0]["text"])

    def test_root_probe_read_only(self):
        responses = self.run_server([
            {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
            {
                "jsonrpc": "2.0",
                "id": 2,
                "method": "tools/call",
                "params": {
                    "name": "lanyard_root_status",
                    "arguments": {"serial": "serial1"},
                },
            },
        ])
        text = responses[1]["result"]["content"][0]["text"]
        self.assertIn("context: u:r:su:s0", text)
        self.assertFalse(responses[1]["result"]["isError"])

    def test_root_probe_requires_switch(self):
        responses = self.run_server([
            {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
            {
                "jsonrpc": "2.0",
                "id": 2,
                "method": "tools/call",
                "params": {
                    "name": "lanyard_root_status",
                    "arguments": {"serial": "serial1"},
                },
            },
        ], allow_root=False)
        self.assertTrue(responses[1]["result"]["isError"])
        self.assertIn("root execution is disabled", responses[1]["result"]["content"][0]["text"])

    def test_tether(self):
        responses = self.run_server([
            {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
            {
                "jsonrpc": "2.0",
                "id": 2,
                "method": "tools/call",
                "params": {
                    "name": "lanyard_tether_connect",
                    "arguments": {"host": "192.168.1.5", "port": 5555},
                },
            },
        ])
        self.assertIn("connected", responses[1]["result"]["content"][0]["text"])


if __name__ == "__main__":
    unittest.main()
