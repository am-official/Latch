# Latch 0.4.0

## Reliability
- **Single monitor path** — receivers only in `LatchMonitorService` (no double-fire with UI)
- Battery matching via pure **RuleLogic** (unit-tested): cross threshold + force on save/start
- Wi‑Fi events **debounced** (800ms)
- Day-of-week trigger fixed (was never firing)
- Brightness uses manual mode + clear errors

## Shizuku
- Cached `newProcess` reflection, `/system/bin/sh` fallback, process timeouts
- Binder received/dead listeners
- Elevated helpers: airplane, mobile data, force-stop package

## New actions
- Force-stop app · Mobile data · Airplane mode (via Shizuku/su)

## Quality
- JVM unit tests for RuleLogic (battery, SSID, time window, day bits, brightness)
- Settings **Self-test** button
