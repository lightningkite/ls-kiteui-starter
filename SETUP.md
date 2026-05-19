# Template Project Setup

- [ ] Go to the `personalize.main.kts` script, at the bottom enter in your app's info, then run
- [ ] Run the server to generate your settings file
- [ ] Update the Firebase project info (`apps/google-services.json`, `apps/ios/app/GoogleService-Info.plist`, and `apps/src/jsMain/kotlin/com/lightningkite/lskiteuistarter/utils/fcmSetup.js.kt`)
- [ ] Correct the CORS options for your local use to restrict to `[*]` on all the limit stuff, allow credentials
- [ ] When you have published apps, go to `apps/*/commonMain/*/views/checkAppVersion.kt` and add a link to your app in the dialog

***Remove this when you're done setting up***
