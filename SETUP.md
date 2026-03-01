# Project Setup Checklist

<!-- by Claude — Actionable setup checklist for new projects -->

***Delete this file when you're done setting up.***

## 1. Initialize the project

```bash
./init-project.sh
```

This renames all packages, directories, and references from `lskiteuistarter` to your project name.

## 2. Configure settings.json

Run the server once to generate a default `settings.json`:

```bash
./gradlew :server:serve
```

Then update the generated `settings.json`:
- Set `database` connection string (use `"json-files://local/database"` for local dev, MongoDB URI for production)
- Set `email` (use `"console"` for dev, SMTP URL for production)
- Set `notifications` with your Firebase Cloud Messaging credentials
- Set `cors.limitToDomains` to `["*"]` for local development
- Ensure `general.debug` is `true` for development

## 3. Start the server

```bash
./gradlew :server:serve
```

Server runs at `http://localhost:8081` by default.

## 3b. Seed sample data (optional)

<!-- by Claude -->

Populate the database with sample users, organizations, and memberships for instant dev feedback:

```bash
./gradlew :server:seed
```

Requires `general.debug = true` in `settings.json`. Prints an admin session token for API testing.

## 4. Regenerate the SDK

After any server endpoint changes:

```bash
./gradlew :server:generateSdk
```

## 5. Start the frontend

```bash
./gradlew :apps:jsBrowserDevelopmentRun
```

Frontend runs at `http://localhost:8941` and proxies API calls to the server.

## 6. Run tests

```bash
./gradlew :server:test
```

## 7. Browser testing (optional)

```bash
./testing/setup.sh
```

See `testing/README.md` for details on browser-based testing with Chrome MCP tools.

## 8. Customize for your project

- [ ] Update `deployments.kt` with your domain, AWS region, and deployment settings
- [ ] Update `apps/src/jsMain/resources/index.html` — change the `.mjs` script reference to match your project name
- [ ] Configure `apps/google-services.json` for your Firebase project
- [ ] Add your models to `shared/.../models.kt`
- [ ] Create endpoints in `server/.../data/`
- [ ] Regenerate SDK and build frontend screens
- [ ] Set up CI (`.github/workflows/build.yml` is included as a template)

## 9. Mobile Deployment Setup (optional)

<!-- by Claude -->

### Android (Play Store)

1. Create a service account in Google Cloud Console with Google Play Developer API access
2. Download the JSON key and save to `local/play-store-key.json`
3. Set `playStoreJsonKeyPath=local/play-store-key.json` in `local.properties`
4. Ensure you have a signed release keystore configured (see signing keys above)
5. **First upload must be done manually** via Google Play Console
6. After that:
   ```bash
   ./gradlew :apps:publishAndroid      # Build + upload to internal track
   ./gradlew :apps:promoteAndroid      # Promote internal → production
   ```

### iOS (App Store via Fastlane)

1. Create an App Store Connect API key (`.p8` file) — save to `local/`
2. Create an S3 bucket for Fastlane Match certificate storage
3. Fill in all `ios.*` keys in `local.properties` (see `local.properties.example`)
4. Install Ruby dependencies: `bundle install`
5. One-time cert setup:
   ```bash
   ./gradlew :apps:setupMatch          # Generate cert + profile → S3
   ```
6. After that:
   ```bash
   ./gradlew :apps:publishIos          # Match certs + build + TestFlight
   ./gradlew :apps:submitIos           # Submit for App Store review
   ```

## 10. Deploy

```bash
# Build the web frontend
./gradlew :apps:viteBuild

# Deploy to AWS (update terraform config first)
./gradlew :apps:deployWebdefault

# Build Lambda package for server
./gradlew :server:lambda
```
