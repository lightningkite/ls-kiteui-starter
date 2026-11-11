# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Kotlin multiplatform project using KiteUI for the frontend and Lightning Server for the backend, created from the minimal ls-kiteui-starter template. The build is fully configured for Android, iOS, web (JS) and a JVM server, but there are no models, endpoints or screens yet: `Server` is empty and the app has one placeholder page.

**Key Technologies:**
- **KiteUI**: Lightning Kite's declarative UI framework for multiplatform apps
- **Lightning Server**: Backend framework with typed endpoints, sessions, and WebSocket support
- **Service Abstractions**: Database, file storage, notifications, and email abstractions
- Kotlin (see `gradle/libs.versions.toml` for exact versions) with context parameters

For worked examples of auth, REST models, permissions, file uploads and tests, see the full starter at https://github.com/lightningkite/ls-kiteui-starter.

## Project Structure

```
lskiteuistarter/
├── apps/               # KiteUI multiplatform frontend (Android, iOS, JS)
├── apps-android/       # Android app shell
├── apps-ios/           # iOS app shell (Xcode project)
├── server/             # JVM backend
├── shared/             # Code shared between client and server (models)
├── integration-tests/  # TestRunnerFetcher for app-against-server tests
├── local/              # Local files for development (use instead of /tmp)
└── settings.json       # Server configuration, written on first run (gitignored)
```

- `server/.../Server.kt` - The `Server` object: settings and endpoints are declared here
- `server/.../Main.kt` - CLI entry point (`serve`, `predeploy`, `sdk`)
- `shared/.../clock.kt` - `now()` and the app clock, replaceable in tests
- `apps/.../App.kt` - App root (`app(navigator)`), theme, and the placeholder `HomePage`
- `apps/src/{androidMain,iosMain,jsMain}/` - Platform entry points that call `app(navigator)`

## Common Commands

```bash
./gradlew :server:serve                    # Start the server (localhost:8080)
./gradlew :server:generateSdk              # Regenerate the client SDK in apps/.../sdk/
./gradlew :server:test                     # Server tests
./gradlew :apps:jsBrowserDevelopmentRun    # Web frontend with hot reload
./gradlew :apps:viteBuild                  # Production web bundle
./gradlew :apps:installDebug               # Android app on a device/emulator
./gradlew :apps:podInstall                 # iOS framework (macOS only)
./gradlew build                            # Build everything
```

## Architecture Patterns

**Server:** `Server` extends `ServerBuilder`. Settings are declared with `setting(...)` and loaded from `settings.json`; endpoints are properties bound to paths, and feature modules are mounted with `path.path("name") module SomeEndpoints`.

**Models:** Put shared models in `shared/src/commonMain/kotlin/...`, annotated with `@Queryable` (applies `@Serializable` and generates field optics). Generated fields are paths: query with `User.email eq email` and modify with `Modification<User> { it.name assign "x" }`.

**Client:** KiteUI pages are classes implementing `Page`, annotated with `@Routable("/path")`; KSP collects them into `AutoRoutes`, which every platform entry point uses. State is reactive (`Signal<T>`, `Reactive<T>`).

**SDK:** `./gradlew :server:generateSdk` writes a typed client for the server's endpoints into `apps/src/commonMain/kotlin/.../sdk/`. **Regenerate it after any server endpoint change.**

**Conventions:**
- Context parameters are enabled: use `context(_: ServerRuntime)` syntax
- Use `Uuid.random()` for IDs, not Java's UUID
- Use `./local/` for temporary files, not `/tmp`
- Do not commit `settings.json`; it holds credentials
