# Latch 0.2.0

## New triggers
- Wi‑Fi connected / disconnected / specific **SSID**
- Bluetooth connected / disconnected
- **App opened** (usage access + optional foreground watcher)
- Screen on / off
- **Custom broadcast** (plugin API)

## Multi-action rules
Chain notify → wait → ringer → open app → shell in one rule.

## Actions
- Open app / URL
- Set brightness (write-settings)
- Shell command (optional **elevated su**)
- Plugin broadcast out
- Wait between steps

## Custom options
- Per-rule **cooldown** seconds
- Notes field
- Settings: elevated shell toggle, plugin API toggle, permission shortcuts

## Widget
Home-screen widget to toggle a chosen rule on/off.

## Plugin API
```
com.endroid.latch.api.FIRE_TRIGGER  extra: trigger=<action string>
com.endroid.latch.api.RUN_RULE      extra: rule_id=<id>
```
