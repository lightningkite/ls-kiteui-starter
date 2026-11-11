# Template Project Setup

- [ ] Replace the display name `LS KiteUI Starter` with your app's name (in `App.kt` and `server/.../deployments.kt`).
- [ ] Add your admin accounts: in `server/.../data/UserEndpoints.kt`, call `root("you@yourcompany.com".toEmailAddress())` inside the `initAdminUsers` block. The block runs only on a database's first predeploy (`predeploy` command or a deploy), so add them before deploying.
- [ ] Run the server once. The first run writes a default `settings.json` (gitignored) and exits; run it again to confirm it starts:
  ```
  ./gradlew :server:serve
  ```
  Edit `settings.json` to point at your real database, email service, etc.
- [ ] **Firebase** — Replace the hardcoded starter-project Firebase credentials in **both** of these files with your own Firebase project values:
  - `apps/src/jsMain/resources/public/firebase-messaging-sw.js` (the `firebase.initializeApp({...})` block)
  - `apps/src/jsMain/kotlin/.../utils/fcmSetup.js.kt` (the `firebaseAppOptions[...]` block and `vapidKey`)

  Also replace/download fresh copies of:
  - `apps-android/google-services.json` and `apps/google-services.json` (Android — from Firebase Console)
  - `apps-ios/app/GoogleService-Info.plist` (iOS — from Firebase Console)
- [ ] CORS: the default `settings.json` uses `"*"` as the allowed origin, which is fine for local development. **Before deploying to production**, replace `"*"` with your real domain(s) and set `allowCredentials` appropriately. Do not ship wildcard CORS to production.
- [ ] When you have published apps, open `apps/src/commonMain/kotlin/com/lightningkite/lskiteuistarter/views/checkAppVersion.kt` and add the store URL in the commented-out button block near line 84.

***Remove this file when you're done setting up***
