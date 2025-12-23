# Testing Infrastructure Setup Guide

This guide documents how to set up browser testing infrastructure for Lightning Server + KiteUI projects. Use this when adapting the testing setup to a new project or changing ports.

## Port Configuration Locations

When changing ports, update ALL of these locations:

### Backend Port (default: 8081)

1. **`testing/settings.testing.json`** - Server runtime config
   ```json
   "ktorRunConfig": {
     "host": "0.0.0.0",
     "port": 8081,  // <-- CHANGE THIS
     "realIpHeader": null
   }
   ```

2. **`apps/vite.config.mjs`** - Vite proxy target
   ```javascript
   proxy: {
     '/api': {
       target: 'http://localhost:8081',  // <-- CHANGE THIS
       rewrite: (path) => path.replace(/^\/api/, ''),
       ws: true,
     }
   }
   ```

3. **`testing/start-backend.sh`** - Port variable
   ```bash
   PORT=8081  # <-- CHANGE THIS
   ```

4. **`testing/stop-all.sh`** - Port in stop command
   ```bash
   if lsof -i :8081 > /dev/null 2>&1; then  # <-- CHANGE THIS
   ```

5. **`testing/api.sh`** - API base URL
   ```bash
   BASE_URL="http://localhost:8081"  # <-- CHANGE THIS
   ```

### Frontend Port (default: 8941)

1. **`apps/vite.config.mjs`** - Vite server port
   ```javascript
   server: {
     host: true,
     port: 8941,  // <-- CHANGE THIS
     allowedHosts: ["...", "localhost:8941"],  // <-- UPDATE THIS TOO
   }
   ```

2. **`testing/start-frontend.sh`** - Port variable
   ```bash
   PORT=8941  # <-- CHANGE THIS
   ```

3. **`testing/stop-all.sh`** - Port in stop command
   ```bash
   if lsof -i :8941 > /dev/null 2>&1; then  # <-- CHANGE THIS
   ```

## Critical Setup Requirements

### 1. Settings File Must Be Complete

The `testing/settings.testing.json` must include ALL required Lightning Server settings:
- `cache` - Cache backend (use `"ram"` for testing)
- `cors` - CORS configuration
- `database` - Database connection (use `"json-files://local/database"` for testing)
- `email` - Email service (use `"console"` to print emails to stdout)
- `files` - File storage
- `general` - Project settings including `"debug": true` for admin token
- `ktorRunConfig` - Host/port configuration
- `logging` - Log configuration
- `notifications` - FCM config (can be placeholder)
- `pubSub` - PubSub backend (use `"local"` for testing)
- `secretBasis` - Valid Base64-encoded secret
- `telemetry` - Can be `null`

### 2. Debug Admin Token

The server must output an admin token on startup. Add this to your server's endpoints file:

```kotlin
import com.lightningkite.lightningserver.StartupTask
import com.lightningkite.lightningserver.settings.generalSettings

// In your endpoints object:
val debugAdminToken = path.path("debug-admin-token") bind StartupTask {
    if (generalSettings().debug) {
        println("Admin token: '${YourAuth.session.createSession(Uuid.fromLongs(0L, 10L)).second}'")
    }
}
```

Replace `YourAuth` with your authentication object name.

### 3. SDK Must Match Server

If you get 404 errors like `No route matching POST /auth/user/token/simple`, the SDK is out of sync:

```bash
./gradlew :server:generateSdk
```

Then restart the frontend to pick up changes.

### 4. Use SameServer API Option

**CRITICAL**: In the browser, the app must use `SameServer` API option, NOT `Local`.

- `SameServer` → requests go to `/api/*` → Vite proxies to backend
- `Local` → requests go directly to `localhost:8080` → WRONG SERVER

Set via JavaScript injection:
```javascript
localStorage.setItem('apiOption', '"SameServer"');
```

### 5. Session Token Format

The session token must be stored with quotes inside the string:
```javascript
// CORRECT - note the inner quotes
localStorage.setItem('sessionToken', '"refresh/User/uuid:token"');

// WRONG - missing inner quotes
localStorage.setItem('sessionToken', 'refresh/User/uuid:token');
```

## Files to Create

Copy these from a working project and update ports:

```
testing/
├── .gitignore              # Ignore generated files
├── settings.testing.json   # Server config (get from project's settings.json)
├── start-backend.sh        # Start backend server
├── start-frontend.sh       # Start Vite frontend
├── stop-all.sh             # Stop all servers
├── api.sh                  # Make API calls (optional)
├── prepare-browser-test.sh # All-in-one setup script
├── README.md               # Project-specific notes
└── SETUP-GUIDE.md          # This file
```

## Checklist for New Project Setup

- [ ] Create `testing/` directory
- [ ] Create `testing/.gitignore` with: `.admin-token`, `.backend.log`, `.frontend.log`, `.browser-inject.js`, `.frontend-url`, `.backend-url`, `local/`
- [ ] Copy and adapt `settings.testing.json` from project's `settings.json`
- [ ] Update `settings.testing.json` with test port and `"debug": true`
- [ ] Update `apps/vite.config.mjs` proxy target to test backend port
- [ ] Copy and adapt shell scripts, updating port numbers
- [ ] Add debug admin token startup task to server if not present
- [ ] Verify server main class in `server/build.gradle.kts`
- [ ] Test with `./testing/start-backend.sh` - verify admin token output
- [ ] Test with `./testing/start-frontend.sh` - verify Vite starts
- [ ] Test browser login with SameServer option and token injection

## Common Issues

### "ClassNotFoundException: MainKt"
The main class path in `server/build.gradle.kts` is wrong:
```kotlin
application {
    mainClass.set("com.yourcompany.yourproject.MainKt")  // Fix this path
}
```

### "IncompleteSettingsException"
Missing required settings in `settings.testing.json`. Check the error message for which key is missing.

### "Invalid Base64" for secretBasis
Generate a valid secret or copy from the project's real `settings.json`.

### Server starts but API calls fail with 404
SDK is out of sync. Run `./gradlew :server:generateSdk` and restart frontend.

### Login works but immediately logs out
Using wrong API option. Must use `SameServer`, not `Local`.

### Email codes not visible
Check backend log file or stdout. With `"email": "console"`, codes are printed there.

## Port Allocation Suggestions

To avoid conflicts between projects:

| Project | Backend | Frontend |
|---------|---------|----------|
| instaclub | 8080 | 8941 |
| ls-kiteui-starter | 8081 | 8941 |
| project-c | 8082 | 8942 |
| project-d | 8083 | 8943 |

Note: Frontend ports can overlap if you only run one frontend at a time. Backend ports must be unique if running multiple backends simultaneously.
