"""Command line entry point for Lanyard."""

from __future__ import annotations

import argparse
import sys

from .adb import Adb, AdbError
from .audit import AuditLog
from .config import Config, PolicyError
from .mcp import LanyardMcpServer
from .root import SuProvider


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="lanyard",
        description="A tethered Android root plane, exposed as an MCP shell.",
    )
    parser.add_argument("--adb", default="adb", help="ADB executable (default: adb)")
    parser.add_argument(
        "--allow-serial",
        action="append",
        default=[],
        metavar="SERIAL",
        help="Allow this serial (repeatable).",
    )
    parser.add_argument(
        "--allow-all",
        action="store_true",
        help="Allow every attached serial. Use only on an authorized lab workstation.",
    )
    parser.add_argument(
        "--allow-root",
        action="store_true",
        help="Allow shell commands with as_root=true.",
    )
    parser.add_argument("--timeout", type=int, default=30, help="Default command timeout.")
    parser.add_argument("--output-limit", type=int, default=262144, help="Max bytes per stream.")
    parser.add_argument("--audit", default=None, help="Audit JSONL path.")

    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("devices", help="List attached devices.")
    sub.add_parser("mcp", help="Run the MCP stdio server.")

    root = sub.add_parser("root-status", help="Probe su/uid/SELinux without changing state.")
    root.add_argument("serial")

    shell = sub.add_parser("shell", help="Run one bounded shell command.")
    shell.add_argument("serial")
    shell.add_argument("--as-root", action="store_true", dest="as_root")
    shell.add_argument("--timeout", type=int, default=None)
    shell.add_argument("argv", nargs=argparse.REMAINDER)

    tether = sub.add_parser("tether", help="Connect or disconnect ADB over TCP.")
    tether.add_argument("action", choices=["connect", "disconnect"])
    tether.add_argument("host")
    tether.add_argument("port", type=int)

    return parser


def _config(args: argparse.Namespace) -> Config:
    return Config.from_env(
        adb_path=args.adb,
        allowed_serials=args.allow_serial,
        allow_all=args.allow_all,
        allow_root=args.allow_root,
        timeout_s=args.timeout,
        output_limit=args.output_limit,
        audit_path=args.audit,
    )


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    config = _config(args)
    audit = AuditLog(config.audit_path)
    adb = Adb(config.adb_path, config.timeout_s, config.output_limit)

    try:
        if args.command == "devices":
            devices = adb.devices()
            if not devices:
                print("no devices attached")
                return 0
            for device in devices:
                details = [device.serial, device.state]
                if device.model:
                    details.append(f"model={device.model}")
                if device.device:
                    details.append(f"device={device.device}")
                print("  ".join(details))
            return 0

        if args.command == "root-status":
            config.ensure_serial(args.serial)
            config.ensure_root()
            probe = SuProvider().probe(adb, args.serial)
            audit.record(
                event="root_probe",
                serial=args.serial,
                as_root=False,
                ok=probe.available,
                detail=probe.detail,
            )
            print(probe.render())
            return 0 if probe.available else 2

        if args.command == "shell":
            config.ensure_serial(args.serial)
            if args.as_root:
                config.ensure_root()
            argv = list(args.argv)
            if argv and argv[0] == "--":
                argv = argv[1:]
            command = " ".join(argv).strip()
            if not command:
                parser.error("shell requires a command after --")
            result = adb.shell(
                args.serial,
                command,
                as_root=args.as_root,
                timeout_s=args.timeout or config.timeout_s,
            )
            audit.record(
                event="shell",
                serial=args.serial,
                as_root=args.as_root,
                ok=result.ok,
                duration_ms=result.duration_ms,
                command=command,
                detail=None if result.ok else result.stderr[:500],
            )
            print(result.render())
            return 0 if result.ok else 1

        if args.command == "tether":
            if args.action == "connect":
                print(adb.tether_connect(args.host, args.port))
            else:
                print(adb.tether_disconnect(args.host, args.port))
            audit.record(
                event=f"tether_{args.action}",
                serial=f"{args.host}:{args.port}",
                as_root=False,
                ok=True,
            )
            return 0

        if args.command == "mcp":
            return LanyardMcpServer(config, adb=adb, audit=audit).run()

        parser.error(f"unknown command: {args.command}")
        return 2
    except (PolicyError, AdbError) as exc:
        print(f"lanyard: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
