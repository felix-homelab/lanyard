import unittest

from lanyard.adb import AdbError, parse_devices, _validate_host_port
from lanyard.config import Config, PolicyError


class ParseDevicesTest(unittest.TestCase):
    def test_parses_standard_output(self) -> None:
        output = """List of devices attached
R5CR916B48N            device product:a52sxqeea device:a52sxq model:SM_A528B transport_id:1
192.168.1.5:5555       device product:test device:test model:Pixel transport_id:2
emulator-5554          offline transport_id:3
"""
        devices = parse_devices(output)
        self.assertEqual([d.serial for d in devices], [
            "R5CR916B48N", "192.168.1.5:5555", "emulator-5554"
        ])
        self.assertEqual(devices[0].model, "SM_A528B")
        self.assertTrue(devices[0].usable)
        self.assertFalse(devices[2].usable)

    def test_ignores_daemon_noise(self) -> None:
        output = """* daemon not running; starting now at tcp:5037
* daemon started successfully
List of devices attached
serial123	unauthorized
"""
        devices = parse_devices(output)
        self.assertEqual(len(devices), 1)
        self.assertEqual(devices[0].state, "unauthorized")


class PolicyTest(unittest.TestCase):
    def test_empty_allowlist_denies(self) -> None:
        config = Config()
        with self.assertRaises(PolicyError):
            config.ensure_serial("anything")

    def test_explicit_allow(self) -> None:
        config = Config(allowed_serials=frozenset({"abc"}), allow_root=True)
        config.ensure_serial("abc")
        config.ensure_root()
        with self.assertRaises(PolicyError):
            config.ensure_serial("def")

    def test_root_requires_switch(self) -> None:
        with self.assertRaises(PolicyError):
            Config().ensure_root()


class TetherValidationTest(unittest.TestCase):
    def test_rejects_wildcards(self) -> None:
        for host in ("0.0.0.0", "::"):
            with self.assertRaises(AdbError):
                _validate_host_port(host, 5555)

    def test_rejects_invalid_port(self) -> None:
        with self.assertRaises(AdbError):
            _validate_host_port("192.168.1.5", 0)
        with self.assertRaises(AdbError):
            _validate_host_port("192.168.1.5", 70000)


if __name__ == "__main__":
    unittest.main()
