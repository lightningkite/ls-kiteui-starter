# LS KiteUI Starter

A Kotlin multiplatform starter template using [KiteUI](https://github.com/lightningkite/kiteui) for the frontend and [Lightning Server](https://github.com/lightningkite/lightning-server) for the backend. Supports Android, iOS, and web (JS) on the frontend, with a JVM backend that can deploy to AWS Lambda.

## Module Breakdown

| Module | Purpose |
|--------|---------|
| `shared/` | Multiplatform data models shared between client and server (`User`, `FcmToken`, etc.) |
| `server/` | JVM backend — Lightning Server endpoints, authentication, email, push notifications |
| `apps/` | KiteUI multiplatform frontend — Android, iOS, and web targets |
| `local/` | Local dev files (database files, temp artifacts); gitignored |
| `testing/` | Scripts for AI-assisted browser testing |

## Quick Start

### 1. Personalize the template

Fill in your app name and package in `personalize.main.kts`, then run:

```bash
kotlinc -script personalize.main.kts
```

### 2. Configure settings

```bash
cp settings.suggested.json settings.json
# Edit settings.json with your database, email, and notification config
```

### 3. Start the server

```bash
./gradlew :server:serve          # runs on localhost:8080
```

### 4. Start the web frontend

```bash
./gradlew :apps:jsBrowserDevelopmentRun   # runs on localhost:8080 (dev server)
```

### 5. Regenerate the client SDK after server changes

```bash
./gradlew :server:generateSdk
```

## Common Commands

```bash
# Run server tests
./gradlew :server:test

# Build everything
./gradlew build

# Build Android APK
./gradlew :apps:installDebug

# Build iOS framework (macOS only)
./gradlew :apps:podInstall

# Deploy web frontend to AWS
./gradlew :apps:deployWebdefault
```

## First-Time Setup

See [SETUP.md](SETUP.md) for the full onboarding checklist, including Firebase configuration and CORS settings.
