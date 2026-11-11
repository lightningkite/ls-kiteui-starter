# Project Setup

- [ ] Run the server once. The first run writes a default `settings.json` (gitignored) and exits; run it again to confirm it starts:
  ```
  ./gradlew :server:serve
  ```
  As you add settings to `Server.kt` (database, email, files, ...), they appear in `settings.json` for you to fill in.
- [ ] **Firebase** — The Android and iOS builds include Firebase. Replace the starter project's config files with your own from the Firebase Console:
  - `apps-android/google-services.json` and `apps/google-services.json` (Android)
  - `apps-ios/app/GoogleService-Info.plist` (iOS)
- [ ] **Signing** (optional) — copy `local.properties.example` to `local.properties` and fill in your Android signing config.
- [ ] Before deploying to production, replace any wildcard (`"*"`) CORS origins in your settings with your real domain(s).

***Remove this file when you're done setting up***
