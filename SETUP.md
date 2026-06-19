# Template Project Setup

- [ ] Go to `personalize.main.kts`, fill in your `appName`, `packageName`, and `rootUsers` at the bottom, then run:
  ```
  kotlinc -script personalize.main.kts
  ```
- [ ] Copy `settings.suggested.json` to `settings.json` (it is gitignored), then run the server once to verify it starts:
  ```
  cp settings.suggested.json settings.json
  ./gradlew :server:serve
  ```
  The first run confirms the default settings work. Edit `settings.json` to point at your real database, email service, etc.
- [ ] **Firebase** — Replace the hardcoded starter-project Firebase credentials in **both** of these files with your own Firebase project values:
  - `apps/src/jsMain/resources/public/firebase-messaging-sw.js` (the `firebase.initializeApp({...})` block)
  - `apps/src/jsMain/kotlin/.../utils/fcmSetup.js.kt` (the `firebaseAppOptions[...]` block and `vapidKey`)

  Also replace/download fresh copies of:
  - `apps/google-services.json` (Android — from Firebase Console)
  - `apps/src/iosMain/GoogleService-Info.plist` (iOS — from Firebase Console)
- [ ] CORS: the default `settings.json` uses `"*"` as the allowed origin, which is fine for local development. **Before deploying to production**, replace `"*"` with your real domain(s) and set `allowCredentials` appropriately. Do not ship wildcard CORS to production.
- [ ] When you have published apps, open `apps/src/commonMain/kotlin/com/lightningkite/lskiteuistarter/views/checkAppVersion.kt` and add the store URL in the commented-out button block near line 84.

***Remove this file when you're done setting up***
