# lskiteuistarter

A Kotlin multiplatform app using [KiteUI](https://github.com/lightningkite/kiteui) for the frontend and [Lightning Server](https://github.com/lightningkite/lightning-server) for the backend. Supports Android, iOS, and web (JS) on the frontend, with a JVM backend that can deploy to AWS Lambda.

Created from the minimal [ls-kiteui-starter](https://github.com/lightningkite/ls-kiteui-starter) template: the build is fully set up, the server has no endpoints yet, and the app has one page.

## Modules

| Module | Purpose |
|--------|---------|
| `shared/` | Multiplatform code shared between client and server, such as data models |
| `server/` | JVM backend: `Server.kt` declares settings and endpoints, `Main.kt` is the CLI entry point |
| `apps/` | KiteUI frontend for Android, iOS, and web; `App.kt` is the app's root |
| `apps-android/`, `apps-ios/` | Native app shells for the `apps` module |
| `integration-tests/` | `TestRunnerFetcher`, for running the app against an in-memory server in tests |
| `local/` | Local dev files (database files, temp artifacts); gitignored |

## Getting Started

```bash
# Start the server. The first run writes a default settings.json (gitignored) and exits;
# edit it if needed, then run again.
./gradlew :server:serve

# Start the web frontend with hot reload
./gradlew :apps:jsBrowserDevelopmentRun

# Regenerate the client SDK in apps/ after changing server endpoints
./gradlew :server:generateSdk
```

## Common Commands

```bash
./gradlew build                    # Build everything
./gradlew :server:test             # Run server tests
./gradlew :apps:installDebug       # Install the Android app on a device/emulator
./gradlew :apps:podInstall         # Build the iOS framework (macOS only)
./gradlew :apps:viteBuild          # Production web bundle
```

See [SETUP.md](SETUP.md) for the first-time setup checklist.
