# Latch 0.3.0

## Permissions settings
Each capability shows **Enabled** or an **Enable** button (notifications, usage access, write settings, exact alarms, Bluetooth, location/SSID, Shizuku, root).

## Elevated execution
- **Shizuku** preferred for shell when authorized
- **Root (su)** optional fallback
- Wi‑Fi / Bluetooth enable actions via elevated `svc` when possible

## Conditions (AND)
Also require: Wi‑Fi on/off, charging, screen, battery threshold, weekday/weekend.

## More triggers
Airplane mode · media mounted · time window · day mask

## More actions
Media volume · vibrate · clipboard · flashlight · Wi‑Fi/BT enable · media next · multi-step chains

## UX
- Run rule now (play) on each card
- Import / export JSON
- On-device run log
- Tags + notes + cooldown
