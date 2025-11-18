# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin multiplatform starter project using KiteUI for the frontend and Lightning Server for the backend. It supports Android, iOS, and web (via JS) on the frontend, with a JVM-based backend server.

**Key Technologies:**
- **KiteUI** (v7.0.0): Lightning Kite's declarative UI framework for multiplatform apps
- **Lightning Server** (v5.0.0): Backend framework with typed endpoints, sessions, and WebSocket support
- **Service Abstractions** (v0.0.1): Database, file storage, notifications, and email abstractions
- Kotlin 2.2.20 with context parameters enabled
- Firebase Cloud Messaging for push notifications
- MongoDB/JSON file database support
- AWS Lambda deployment support

## Project Structure

```
ls-kiteui-starter/
├── apps/           # Multiplatform frontend application (Android, iOS, JS)
├── server/         # JVM backend server with Lightning Server
├── shared/         # Shared data models and types between client/server
├── local/          # Local files for development (use instead of /tmp)
├── settings.json   # Server configuration (database, email, notifications, etc.)
└── terraform/      # Infrastructure as code for web deployments
```

### Module Breakdown

**shared/** - Multiplatform module defining core data models:
- `User`, `UserRole`, `FcmToken` models with database annotations
- Uses KSP processor to generate database paths (`@GenerateDataClassPaths`)
- Compiled for Android, JVM, JS, and iOS targets

**server/** - JVM application with Lightning Server:
- `Server.kt` - Main server definition with endpoints, settings, and middleware
- `Main.kt` - Entry point with CLI commands (`serve`, `sdk`)
- `UserEndpoints.kt` - REST endpoints for User CRUD with role-based permissions
- `AuthenticationEndpoints.kt` (UserAuth) - Authentication with email/password/TOTP/backup codes
- `FcmTokenEndpoints.kt` - Firebase Cloud Messaging token management
- `Emails.kt` - Email template utilities

**apps/** - Multiplatform KiteUI application:
- `App.kt` - Main application entry point with navigation and theming
- `LandingScreen.kt`, `HomeScreen.kt`, `LoginScreen.kt` - Screen implementations
- `sdk/` - Auto-generated API client from server (via `generateSdk` task)
- Platform-specific implementations in `androidMain/`, `iosMain/`, `jsMain/`

## Common Commands

### Server Development

```bash
# Start the development server (localhost:8080)
./gradlew :server:serve

# Run the server (alternative)
./gradlew :server:run

# Generate TypeScript/Kotlin SDK for the client from server endpoints
./gradlew :server:generateSdk

# Build server distribution
./gradlew :server:build

# Run server tests
./gradlew :server:test

# Create Lambda deployment package
./gradlew :server:lambda
```

### Frontend Development

```bash
# Run web development server with hot reload
./gradlew :apps:jsBrowserDevelopmentRun

# Build production web bundle with Vite
./gradlew :apps:viteBuild

# Build development webpack bundle
./gradlew :apps:jsBrowserDevelopmentWebpack

# Run Android app (requires Android device/emulator)
./gradlew :apps:installDebug

# Build iOS framework (requires macOS)
./gradlew :apps:podInstall
```

### Testing

```bash
# Run all tests
./gradlew test

# Run all tests and create aggregated report
./gradlew allTests

# Run server tests specifically
./gradlew :server:test

# Run JS tests in browser
./gradlew :apps:jsBrowserTest
```

### Deployment

```bash
# Deploy web frontend to AWS (default environment)
./gradlew :apps:deployWebdefault

# Build and package everything
./gradlew build
```

## Architecture Patterns

### Server-Side Architecture

**Lightning Server** uses a builder pattern to define the server structure:
- All endpoints are defined as object properties in `Server` (extends `ServerBuilder`)
- Settings are defined with `setting()` and loaded from `settings.json`
- Authentication is handled through `PrincipalType` (see `UserAuth`)
- Endpoints use `ApiHttpHandler` with typed inputs/outputs
- WebSockets support via `MultiplexWebSocketHandler` and `QueryParamWebSocketHandler`

**Authentication Flow:**
1. User proves identity via email/password/TOTP (`UserAuth` object)
2. Proof methods are pluggable (email PIN, password, TOTP, backup codes)
3. Sessions are cached with role information for performance (`RoleCache`)
4. Role-based permissions enforce access control in `ModelPermissions`

**Database Abstraction:**
- Models use `@GenerateDataClassPaths` for type-safe queries
- Supports MongoDB (`MongoDatabase`) and JSON files (`JsonFileDatabase`)
- Connection string configured in `settings.json` under `"database"`
- Database operations use `condition { }` DSL for queries

### Client-Side Architecture

**KiteUI** uses reactive programming with `Signal<T>`:
- `currentSession()` - Reactive access to authentication state
- Navigation handled by `PageNavigator` with serializable pages
- Theming via `Theme` and `appTheme` signal
- FCM token registration integrated with authentication flow

**SDK Generation:**
- Server generates client SDK automatically via `./gradlew :server:generateSdk`
- Generated code appears in `apps/src/commonMain/kotlin/.../sdk/`
- Provides `Api`, `CachedApi`, `LiveApi`, and `UserSession` interfaces
- **Always regenerate SDK after changing server endpoints**

### Shared Code Patterns

- Context parameters enabled (`-Xcontext-parameters`) - use `context(_: ServerRuntime)` syntax
- Experimental features used: `kotlin.time.ExperimentalTime`, `kotlin.uuid.ExperimentalUuidApi`
- All models are `@Serializable` for kotlinx.serialization
- Use `Uuid.random()` for IDs (not UUID from Java)

## Development Workflow

1. **Define models** in `shared/src/commonMain/kotlin/.../models.kt`
2. **Create endpoints** in `server/src/main/kotlin/.../*Endpoints.kt`
3. **Regenerate SDK** with `./gradlew :server:generateSdk`
4. **Implement UI** in `apps/src/commonMain/kotlin/.../`
5. **Test locally** with `./gradlew :server:serve` + `./gradlew :apps:jsBrowserDevelopmentRun`

## Configuration

### settings.json

The server runtime configuration includes:
- `database` - Database connection string (e.g., `"json-files://local/database"` or `"mongodb://..."`)
- `files` - File storage settings (local filesystem or S3)
- `email` - Email service config (`"console"` for development, SMTP URL for production)
- `notifications` - FCM credentials JSON for push notifications
- `cors` - CORS configuration for web clients
- `ktorRunConfig` - Server host/port (default: `0.0.0.0:8080`)

### local.properties

Android signing configuration (optional):
- `signingKeystore`, `signingPassword`, `signingAlias`, `signingAliasPassword`

## Firebase Setup

The project uses Firebase Cloud Messaging:
- JS: `firebase` npm package (v10.7.1) in `apps/src/jsMain/`
- Android: `firebase-messaging-ktx` library
- iOS: Native Firebase SDK via CocoaPods
- Server: FCM admin SDK credentials in `settings.json` under `"notifications"`

## Important Notes

- **Do not commit** `settings.json` with real credentials (it contains FCM private key)
- **Use `./local/` directory** for temporary files, not `/tmp`
- **Regenerate SDK** after any server endpoint changes before implementing client features
- **Role hierarchy**: `NoOne < User < Admin < Developer < Root`
- **Tests** require a test database configuration (see `server/src/test/.../TestSettings.kt`)
