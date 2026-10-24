# Architecture (Latch)

Single-module Android app: Kotlin, Compose, Material You.

```text
UI (LatchApp) → RuleStore / SettingsStore
                     ↓
              RuleEngine.fire / evaluate
                     ↓
         Receivers + LatchMonitorService + TimeScheduler
```

- **No `INTERNET` permission** by design — rules stay on-device.
- **Triggers** come from system broadcasts (battery, connectivity, boot, …) and optional app-watch / time alarms.
- **Actions** run in `RuleEngine` (notify, ringer, brightness, open app/URL, shell via optional root/Shizuku).
- **Product flavors** only change install id (`applicationIdSuffix`); source `namespace` stays `com.endroid.latch`.

### Cooldowns & latching

Pure helpers live in `RuleLogic` (battery thresholds, SSID match, time windows, cooldown).  
`RuleEngine` owns SharedPreferences for last-fire timestamps and side effects.
