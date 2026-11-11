# LS KiteUI Starter

A Kotlin multiplatform starter template using [KiteUI](https://github.com/lightningkite/kiteui) for the frontend and [Lightning Server](https://github.com/lightningkite/lightning-server) for the backend. Supports Android, iOS, and web (JS) on the frontend, with a JVM backend that can deploy to AWS Lambda.

<!-- template-only:start -->
## Start a New Project

```bash
curl -fsSL https://raw.githubusercontent.com/lightningkite/ls-kiteui-starter/master/init.sh | sh -s -- my-app com.mycompany.myapp
```

This creates `./my-app`, a copy of this template with the package renamed to `com.mycompany.myapp`, the project renamed to `my-app`, and a fresh git repository with one initial commit. Nothing links it back to this repo. The package is optional and defaults to `com.example.myapp`.

Options go before the project name:

| Option | Effect |
|--------|--------|
| `--minimal` | Only the build setup: no sample models, endpoints or screens, an empty `Server` object and a one-page `App.kt` |
| `-b <branch>` | Start from a branch or tag of this repo instead of `master` |

```bash
curl -fsSL https://raw.githubusercontent.com/lightningkite/ls-kiteui-starter/master/init.sh | sh -s -- --minimal my-app com.mycompany.myapp
```

Requires `sh`, `curl`, `tar`, `git` and `perl`.

Without `--minimal`, you get everything below: email/password/TOTP auth, users with roles, organizations and memberships, push notifications, file uploads, app release checks, and tests.
<!-- template-only:end -->

## Module Breakdown

| Module | Purpose |
|--------|---------|
| `shared/` | Multiplatform data models shared between client and server (`User`, `Organization`, etc.) |
| `server/` | JVM backend: Lightning Server endpoints, authentication, email, push notifications |
| `apps/` | KiteUI multiplatform frontend: Android, iOS, and web targets |
| `apps-android/`, `apps-ios/` | Native app shells for the `apps` module |
| `integration-tests/` | Tests that run the real app against an in-memory server |
| `local/` | Local dev files (database files, temp artifacts); gitignored |
| `testing/` | Scripts for AI-assisted browser testing |

## Getting Started

```bash
# Start the server. The first run writes a default settings.json (gitignored) and exits;
# edit it with your database, email, and notification config, then run again.
./gradlew :server:serve

# Start the web frontend with hot reload
./gradlew :apps:jsBrowserDevelopmentRun

# Regenerate the client SDK after changing server endpoints
./gradlew :server:generateSdk
```

## Common Commands

```bash
./gradlew :server:test             # Server tests
./gradlew :integration-tests:test  # App-against-server tests
./gradlew build                    # Build everything
./gradlew :apps:installDebug       # Install the Android app on a device/emulator
./gradlew :apps:podInstall         # Build the iOS framework (macOS only)
./gradlew :apps:deployWebdefault   # Deploy the web frontend to AWS
```

## First-Time Setup

See [SETUP.md](SETUP.md) for the full onboarding checklist, including Firebase configuration and CORS settings.
