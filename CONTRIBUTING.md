# Contributing to Latch

Privacy-first Tasker-style automation for Android — **local rules, no cloud, no trackers**.

**Start here:** [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)

## TL;DR

```bash
git clone https://github.com/wized2/Latch.git
cd Latch && git checkout beta
./gradlew testStableDebugUnitTest
# PR against **beta**
```

| Do | Don’t |
|----|--------|
| Target **`beta`** | Open routine PRs to `main` |
| Keep offline (no trackers) | Add analytics / ads |
| Unit-test `RuleLogic` helpers | Commit keystores |
| Use recipe JSON in `/recipes` when sharing packs | Break existing rule schema without migration notes |

## Good first issues

https://github.com/wized2/Latch/labels/good%20first%20issue

Comment on an issue when you start work.

## Docs

| Doc | |
|-----|--|
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | Setup & file map |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | How the engine fits |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | Community |
| [SECURITY.md](SECURITY.md) | Vulnerabilities |
| [recipes/README.md](recipes/README.md) | Importable rule packs |

## License

MIT — see [LICENSE](LICENSE).
