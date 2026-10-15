# Latch 0.3.1

## Critical fix — rules actually run
- Background **monitor service** keeps battery / Wi‑Fi / headset / screen receivers alive (previously only while the UI was open)
- Battery rules fire on threshold cross **and** when a rule is saved / monitor starts (so “battery below 98%” works immediately)
- Brightness sets **manual mode** first; clear error toast if “Modify system settings” is off

## UX
- Battery threshold **slider**
- Brightness / volume / vibrate / wait **sliders**
- Wi‑Fi SSID **dropdown** of known networks (+ manual type)

## Tip
Enable **Modify system settings** in Settings for brightness rules. Leave at least one rule enabled so the monitor stays active.
