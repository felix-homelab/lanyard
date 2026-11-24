# Root providers

Root providers are adapters over an existing, authorized capability. They are
not exploit implementations.

Implemented:

- `SuProvider` — calls `su -c <command>` through ADB.
- `AdbRootProvider` — checks `adb root` on debuggable/userdebug builds.

An external provider is expected to be operator-supplied and reviewed. It must
return a `RootProbe` and a bounded `ShellResult`; it must not persist a daemon,
change boot state, or bypass the serial/root policy in `Config`.

The `root-status` probe reports UID **and** SELinux context. A uid-0 shell in
`u:r:init:s0` is not presented as equivalent to `u:r:su:s0`.
