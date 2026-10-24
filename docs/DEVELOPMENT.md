# Development guide (Latch)

## Quick start

```bash
git clone https://github.com/wized2/Latch.git
cd Latch
git checkout beta
./gradlew :app:assembleBetaDebug
./gradlew :app:testStableDebugUnitTest
```

APK: `app/build/outputs/apk/beta/debug/`  
Package id: **`com.endroid.latch.beta`** (installs next to stable).

### Requirements

- JDK 17, Android SDK 35
- Optional: [Shizuku](https://shizuku.rikka.app/) for elevated shell actions

## Branches & flavors

| Git branch | Flavor | Application id | Role |
|------------|--------|----------------|------|
| **`beta`** | `beta` | `com.endroid.latch.beta` | Default for PRs |
| **`main`** | `stable` | `com.endroid.latch` | Stable releases |

```bash
./gradlew assembleBetaDebug
./gradlew assembleStableRelease
./gradlew testStableDebugUnitTest
```

## Where to change what

| Area | Path |
|------|------|
| Rule evaluation (pure) | `engine/RuleLogic.kt` — **prefer unit tests here** |
| Fire / actions | `engine/RuleEngine.kt` |
| Conditions / Wi‑Fi | `engine/Conditions.kt`, `WifiHelper.kt` |
| Background service | `engine/LatchMonitorService.kt` |
| Receivers | `receiver/` |
| UI | `ui/LatchApp.kt`, `ui/Theme.kt` |
| Persistence | `data/RuleStore.kt`, `Models.kt` |
| Recipes (JSON) | `recipes/*.json` |
| Strings | `res/values/strings.xml` |

## Tests

```text
app/src/test/java/com/endroid/latch/RuleLogicTest.kt
```

Keep new logic testable without `Context` when you can (same pattern as `cooldownOk`).

## PR flow

1. Branch from **`beta`**
2. Open PR → **`beta`**
3. CI green
4. Maintainers promote `beta` → `main` for stable releases
