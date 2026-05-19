# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**HeroScript** — a HIPAA-compliant prescription-ordering platform for the Gameday Men's Health clinic network. Replaces ~12 separate pharmacy logins per clinic with one portal: unified catalog, order tracking, patient shipment SMS, refill-due queue, and clinic invoicing. Built by Lightning Kite for Jon Benson / Gameday. V1 targets ~10 pilot clinics + ≥6 integrated pharmacies. PRD lives in `project-docs/prd.txt`.

Kotlin multiplatform: KiteUI v8 frontend (Android, iOS, web/JS) + Lightning Server v5 backend (JVM, AWS Lambda deployable). MongoDB or JSON-file database. Firebase Cloud Messaging for push.

**Project docs (read before building):**
- `project-docs/prd.txt` — V1 PRD (spec source for *what*).
- `project-docs/ui.md` — confirmed screen list and UI strategy (mobile-first; per-model list/summary/detail; sublists are composed views with locked filters). **The closest thing to a UI spec we have.**
- `project-docs/planned-models.kt` — annotated source-of-truth data model with intent comments. Mirrors `shared/.../models.kt`.
- `project-docs/questions.md` — proposal-stage decisions; deviations from PRD (catalog as product+variant, sigs as freehand, ship-to-state licensing filter, payment processor TBD).
- `project-docs/transcript.md` — discovery-call notes (HIPAA scope, ID.me cadence, billing flow, prior-prototype decisions).
- `project-docs/lifefile-explanation.md` + `lifefile-sandbox.env` — LifeFile pharmacy API sandbox creds and product list (first concrete pharmacy adapter).
- `TODO.md` — outstanding work items between here and V1 pilot launch.

**Current state (2026-05-19):** Models defined. REST/CRUD endpoints with role-scoped `ModelPermissions` in place for every model. Auth + 3 auth-caches (`RoleCache`, `ClinicMembershipsCache`, `CoClinicUsersCache`) wired in `UserAuth.kt`. `Seed.kt` populates a sample clinic/users/pharmacies/product. ~6 server tests covering CRUD + permission boundaries. **No UI past starter Landing/Login/Home screens. No pharmacy adapter, no SMS, no payments, no address verification, no ID.me, no audit log.** Roadmap is `TODO.md`.

## Project Structure

```
herxoscript/
├── apps/           # Multiplatform frontend application (Android, iOS, JS)
├── server/         # JVM backend server with Lightning Server
├── shared/         # Shared data models and types between client/server
├── project-docs/   # PRD, planned models, UI spec, decisions log
├── local/          # Local files for development (use instead of /tmp)
├── tmp/            # Project-scoped temp files (use instead of /tmp)
├── testing/        # Browser-test harness (ports 8081 backend / 8951 frontend)
├── settings.json   # Server configuration (database, email, notifications, etc.)
└── terraform/      # Infrastructure as code for web deployments
```

### Module Breakdown

**shared/** — Multiplatform module defining the HeroScript data model:
- `models.kt` holds every entity: `User` + `PrescriberLicensing`, `Clinic`, `ClinicMembership` (+ `ClinicRole`), `Patient` + `ClinicalEntry`, `Pharmacy` + `Pharmacy.StateInfo`, `Product` + `Product.Form`, `ProductPharmacyMapping`, `Prescription`, `PrescriptionOrder` (+ `Fulfillment`, `Cancellation`), `PharmacyOrder`, `Shipment`, `ClinicianReview`, `PharmacyAccept*` / `PharmacyReject`, `ClinicInvoice`, `Address` / `VerifiedAddress`, `AppRelease`, `FcmToken`.
- KSP `@GenerateDataClassPaths` generates type-safe query paths.
- Compiled for Android, JVM, JS, and iOS targets.

**server/** — JVM application with Lightning Server:
- `Server.kt` — top-level `ServerBuilder` wiring every endpoint module, settings (`database`, `email`, `cache`, `files`, `cors`, `notifications`, `webUrl`), and infrastructure singletons.
- `Main.kt` — CLI entry (`serve`, `sdk`, `seed`).
- `UserAuth.kt` — `PrincipalType<User>` impl, proof methods (email PIN, password, TOTP, backup codes), `SessionEndpoints`, and the three `AuthCacheKey`s.
- `Seed.kt` — dev-only sample-data populator (`./gradlew :server:serve --args="seed"`, requires `general.debug = true`).
- `data/*Endpoints.kt` — one file per model with `modelInfo` + `ModelRestEndpoints`. Clinic-scoped endpoints (Patient, Prescription, PrescriptionOrder, ClinicMembership, ClinicInvoice) gate on `ClinicMembershipsCache` helpers (`clinicIds`, `clinicAdminIds`, `prescriberClinicIds`).

**apps/** — Multiplatform KiteUI application (currently shell-only):
- `App.kt` — app entry, theming, nav scaffold, FCM bootstrap.
- `views/LandingScreen.kt`, `LoginScreen.kt`, `HomeScreen.kt` — placeholder screens from the starter; real HeroScript screens are still TODO (see `ui.md` for the target screen list).
- `sdk/` — auto-generated client (`Api`, `CachedApi`, `LiveApi`, `UserSession`). Regenerate via `./gradlew :server:generateSdk` after any endpoint change.
- Platform-specific implementations in `androidMain/`, `iosMain/`, `jsMain/`.

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
1. User proves identity via email/password/TOTP (`UserAuth` object).
2. Proof methods are pluggable (email PIN, password, TOTP, backup codes); `requiredProofStrengthFor` returns 20 when >1 non-backup proof is established, else 10. AppStoreTester needs 10.
3. Sessions cache per-user `UserRole` (`RoleCache`), accepted clinic memberships (`ClinicMembershipsCache`), and co-clinic user IDs (`CoClinicUsersCache`) — all 5-min TTL.
4. `ModelPermissions` blocks on the cached values. Helpers on `ClinicMembershipsCache`: `clinicIds()`, `clinicAdminIds()`, `prescriberClinicIds()`. Used pervasively in `data/*Endpoints.kt`.

**Permission model conventions in this repo:**
- *System admin* = `UserRole >= Admin` — full read/write everywhere.
- *Clinic-scoped reads/writes* gated on `auth.clinicIds()`.
- *Clinic-admin-only operations* (deletes, role changes, invoice management) gated on `auth.clinicAdminIds()`.
- *Prescriber-only writes* (e.g. `PrescriptionOrder.clinicianReview` — the submission act) gated on `auth.prescriberClinicIds()` via `updateRestrictions { it.field.requires(...) }`.
- *Fields written only by the system* (e.g. `PrescriptionOrder.fulfilled`, `PrescriptionOrder.shipment`) require `systemAdmin` in updateRestrictions — the pharmacy webhook path is the only legitimate writer.
- *Immutable post-create fields* (denormalized snapshots on `PrescriptionOrder`, `createdAt`, `createdBy`, `prescribedBy`) marked `.cannotBeModified()`.

**Workflow gaps (not in CRUD):** order submission dispatch to pharmacy adapter, pharmacy status ingestion, refill calculation, SMS dispatch, payments, address verification, ID.me, and the audit log are NOT implemented yet — see `TODO.md` § 1.

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

## Manual/Browser Testing

The `testing/` directory contains scripts for AI-assisted browser testing. These use ports 8081 (backend) and 8951 (frontend) to avoid conflicts with other projects.

### Quick Start

```bash
# One command to start everything:
./testing/prepare-browser-test.sh
```

### Scripts

```bash
./testing/start-all.sh        # Start backend + frontend
./testing/start-backend.sh    # Start backend on :8081
./testing/start-frontend.sh   # Start frontend on :8951
./testing/stop-all.sh         # Stop all servers
./testing/api.sh GET /path    # Make API calls
```

### Chrome Integration (Claude Code)

After running `prepare-browser-test.sh`, use Claude's Chrome MCP tools:

1. `mcp__claude-in-chrome__tabs_context_mcp(createIfEmpty=true)`
2. `mcp__claude-in-chrome__navigate(tabId=..., url='http://localhost:8951')`
3. Inject session token if available (see `testing/.admin-token`)
4. `mcp__claude-in-chrome__computer(action='screenshot')` for visual verification

### Configuration

- `testing/settings.testing.json` - Testing-specific settings (port 8081, debug mode)
- `testing/.admin-token` - Admin session token (generated when debug mode enabled)
- See `testing/README.md` for full documentation

## Adding a New Feature (Step-by-Step)

Follow this sequence to add a complete new feature (e.g., a new model + screens):

1. **Add model** to `shared/src/commonMain/kotlin/.../models.kt`
   - Annotate with `@Serializable` and `@GenerateDataClassPaths`.
   - Implement `HasId<ID>` with a `value class ID(...) : TypedId<...>` inner type; default the field to `ID(Uuid.random())`.
   - Add any enums in the same file. Keep value-object embeds (like `VerifiedAddress`) annotated `@GenerateDataClassPaths` so paths generate for nested fields.

2. **Create endpoints** in `server/src/main/kotlin/.../data/ThingEndpoints.kt`
   - Follow `PrescriptionOrderEndpoints` for the clinic-scoped pattern, or `ProductEndpoints` for the global-read pattern.
   - Define `ModelPermissions` with `create`/`read`/`update`/`delete`.
   - Use `updateRestrictions { it.field.requires(...) }` or `.cannotBeModified()` for immutable / role-gated fields.
   - Export `val Thing.Companion.info get() = ThingEndpoints.info` for ergonomic table access.

3. **Wire into Server.kt**
   - Add `val things = path.path("things") module ThingEndpoints` in the `// Endpoints, tasks, and schedules` block.

4. **Add auth cache** (only if a new permission predicate doesn't fit existing caches)
   - Follow `ClinicMembershipsCache` in `UserAuth.kt`.
   - Append to `UserAuth.precache`.
   - 5-minute TTL is the convention.

5. **Add tests** to `server/src/test/kotlin/.../ServerTest.kt`
   - CRUD path: insert through `info.table()`, read through `rest.detail.test(...)` with `UserAuth.testAuth(user)`.
   - Permission boundaries: assert `NotFoundException` for forbidden read/update/delete (Lightning narrows the query, so denied operations surface as not-found).

6. **Regenerate SDK** — `./gradlew :server:generateSdk`

7. **Create UI screen** in `apps/src/commonMain/kotlin/.../views/`
   - Reach for `currentSession()?.api` for typed API calls.
   - Follow `ui.md` § Strategy: build list / row summary / detail-with-inline-edit per model; compose by attaching locked filters.
   - Mobile-first, then desktop additions. Hide (don't disable) out-of-role actions.

8. **Add navigation** in `App.kt`
   - Register the screen in `PageNavigator`. Use the role-aware nav split described in `ui.md` (Clinic context vs Ops context).

9. **Verify**
   ```bash
   ./testing/prepare-browser-test.sh
   ```
   Use Chrome MCP tools to visually confirm the screen; never report a UI feature done without a visual check (per global methodology).

## Mobile Deployment

<!-- by Claude -->

```bash
# Android
./gradlew :apps:publishAndroid          # Build AAB + upload to Play internal track
./gradlew :apps:promoteAndroid          # Promote internal → production

# iOS (requires Fastlane: bundle install)
./gradlew :apps:setupMatch              # One-time: generate cert + profile → S3
./gradlew :apps:publishIos              # Match certs + build + upload to TestFlight
./gradlew :apps:submitIos               # Submit for App Store review
```

See `local.properties.example` for required credentials.

## Important Notes

- **Do not commit** `settings.json` with real credentials (it contains FCM private key, will contain pharmacy creds, processor keys, Twilio tokens — all of which must live in AWS Secrets Manager in production).
- **Use `./tmp/` or `./local/` directory** for temporary files, not `/tmp` (per project-methodology — `/tmp` is shared).
- **Regenerate SDK** with `./gradlew :server:generateSdk` after any server endpoint change before working on the client.
- **System role hierarchy** (`UserRole`): `User < Admin < Developer < Root`. Admin and above is "Ops" / system-admin and can cross clinic boundaries. **Clinic-level role** lives separately on `ClinicMembership.role` (`ClinicAdmin` / `Prescriber` / `MedicalAssistant`) and gates clinic-context actions.
- **HIPAA discipline** (PRD § 11):
  - **No PHI in logs, URLs, error messages, or non-HIPAA telemetry.** No PHI sent to AI/LLM systems.
  - Secrets live in AWS Secrets Manager — `Pharmacy.credentialsSecretRef` is the modeled pointer; never put a real secret in the DB or in code.
  - Every PHI-touching read and every state-changing write is supposed to land in the audit log (mechanism still TBD — `TODO.md` § 1.7).
  - BAAs required with every downstream PHI vendor (each pharmacy, Twilio, SendGrid, ID.me, Smarty/Lob/USPS, processor) before integrating.
- **Tests** spin up an in-memory DB via `Server.test(settings = { database set Database.Settings("ram") }) { ... }`. Use `UserAuth.testAuth(user)` to construct an auth context. See `server/src/test/.../ServerTest.kt`.
- **Source-of-truth deviations from PRD** (decided in `project-docs/questions.md` + `ui.md`):
  - Catalog is **product + variant axes** (`Product.forms` set + `ProductPharmacyMapping` keyed by FormType with optional `strength`/`quantity` per mapping), NOT flat SKU rows.
  - Sigs are **freehand strings** on `Prescription.instructions`, NOT a pre-authored sig menu.
  - State-licensing filter keys off `PrescriptionOrder.destination.address.state` (ship-to), NOT patient residence — PRD bug to flag at kickoff.
