# Latch

[![Build & Release](https://github.com/wized2/Latch/actions/workflows/build.yml/badge.svg)](https://github.com/wized2/Latch/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Privacy-first Android automation** — local *if → then* rules. No account, no cloud, no ads, no trackers.

A FOSS alternative spirit to Tasker: triggers and actions that run **only on your device**.

**Site:** https://wized2.github.io/Latch/ · **Releases:** https://github.com/wized2/Latch/releases

## Features (0.1)

| Triggers | Actions |
|----------|---------|
| Battery below / above % | Notification |
| Charger connected / disconnected | Ringer mode (normal / vibrate / silent) |
| Headphones plug / unplug | Toast |
| Daily time (exact alarm) | |
| Boot completed | |

- Material You (dynamic color on Android 12+)
- Rules stored as on-device JSON; **export** for backup
- Optional exact alarms for daily schedules

## Privacy

- No network permission
- No analytics SDKs
- No accounts
- Recipes never leave the phone unless **you** export them

## Build

```bash
./gradlew assembleRelease
```

JDK 17. Release signing uses `RELEASE_*` env secrets (same pattern as Fern).

## Roadmap

- Wi‑Fi / Bluetooth / app-opened triggers (with clear permission UX)
- Multi-action rules & conditions (AND)
- Widget to toggle a rule
- Plugin API for other FOSS apps
- Optional Shizuku / root actions

## License

MIT · Project Endroid
