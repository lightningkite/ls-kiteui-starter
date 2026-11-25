# Testing Scripts

Scripts for manual and browser testing of the application.

**Port Configuration:** Backend: 8081, Frontend: 8941 (via Vite proxy to backend)

**For other projects:** See `SETUP-GUIDE.md` for how to adapt this testing infrastructure to different ports or projects.

## Quick Start

```bash
# From scratch to browser-ready in one command:
./testing/prepare-browser-test.sh
```

This script:
1. Stops the servers these scripts started earlier (never anything else on 8081 or 8941)
2. Seeds sample data (`seed` command, debug servers only), saves the seeded admin's refresh token to `.admin-token`, and starts the backend on port 8081
3. Starts frontend on port 8941 (Vite with proxy to backend)
4. Outputs Chrome integration instructions

Seeding resets the seed rows each time (rows you made by hand survive).  The scripts pass `GRADLE_ARGS` to every
Gradle call, e.g. `GRADLE_ARGS=--offline ./testing/prepare-browser-test.sh` where dependencies can't be downloaded.

## Chrome Integration (Claude Code)

After running `prepare-browser-test.sh`, use Claude's Chrome MCP tools:

```
# 1. Get tab context
mcp__claude-in-chrome__tabs_context_mcp(createIfEmpty=true)

# 2. Navigate to the app
mcp__claude-in-chrome__navigate(tabId=..., url='http://localhost:8941')

# 3. Inject a refresh token (IMPORTANT: use SameServer, not Local!)
#    `sessionToken` is where old app versions kept it; on load the app moves it into the
#    HttpOnly RefreshToken cookie and clears it.  See "Sessions" below.
mcp__claude-in-chrome__javascript_tool(tabId=..., text='''
  localStorage.setItem('apiOption', '"SameServer"');
  localStorage.setItem('sessionToken', '"<token-from-.admin-token>"');
  location.reload();
''')

# 4. Take screenshots and interact
mcp__claude-in-chrome__computer(tabId=..., action='screenshot')
mcp__claude-in-chrome__find(tabId=..., query='login button')
```

**CRITICAL:** Use `"SameServer"` API option, NOT `"Local"`. SameServer routes requests through Vite's proxy to the backend on port 8081. Local goes directly to port 8080 (wrong server).

## Sessions

The refresh token lives only in an HttpOnly `RefreshToken` cookie, never in localStorage.  After login the app sends
it to `POST auth/session/token/set-cookie`, then gets access tokens from `POST auth/session/token/cookie`; logout
(`POST auth/session/terminate`) deletes the cookie.  For this to work in testing:

- `general.publicUrl` is `http://localhost:8941/api`, the URL the browser uses through the Vite proxy, because the
  server scopes the cookie's `Path` to `<publicUrl path>/auth/session/token/cookie`.
- `refreshTokenCookieSecure` is `false`: Chrome accepts `Secure` cookies from http://localhost, Safari and native
  emulators don't.  Never turn it off in production.
- CORS lists the frontend origin with `allowCredentials: true`.  Never combine `allowCredentials` with `"*"`: the
  server reflects any origin, so every site could use the user's cookie.

Refresh tokens are no longer accepted as bearer credentials.  To call the API by hand, exchange one first:

```bash
ACCESS=$(curl -s -X POST http://localhost:8081/auth/session/token/simple -H 'Content-Type: application/json' -d "\"$(cat testing/.admin-token)\"" | tr -d '"')
curl -H "Authorization: $ACCESS" http://localhost:8081/auth/session/self
```

`./testing/api.sh GET /auth/session/self` does both steps.

## Scripts

| Script | Purpose |
|--------|---------|
| `prepare-browser-test.sh` | All-in-one: stops our servers, starts fresh, outputs instructions |
| `start-backend.sh` | Seed sample data, save the seeded admin's refresh token, start backend on :8081 |
| `start-frontend.sh` | Start Vite frontend on :8941 |
| `start-all.sh` | Start backend + frontend |
| `stop-all.sh` | Stop the servers these scripts started (PIDs in `.backend.pid`, `.frontend.pid`) |
| `api.sh` | `./testing/api.sh METHOD PATH [JSON]`: call the backend as the seeded admin |

## Configuration Files

- `settings.testing.json` - Server settings (port 8081, debug mode, console email)
- `apps/vite/vite.config.mjs.template` - Vite config (port 8941 with `strictPort`, proxy to 8081), copied to the gitignored `apps/vite/vite.config.mjs`

## Generated Files (gitignored)

- `.admin-token` - Refresh token for the seeded admin (admin@example.com), printed by `seed`
- `.seed.log` - Seed command output
- `.backend.log` - Backend server output
- `.frontend.log` - Vite server output
- `.browser-inject.js` - JS snippet for manual session injection
- `.backend.pid`, `.frontend.pid` - PIDs of the servers the start scripts launched, for `stop-all.sh`

## Troubleshooting

### SDK out of sync (404 errors on API calls)
```bash
./gradlew :server:generateSdk
# Then restart frontend
```

### Login works but immediately logs out
You're using the wrong API option. Must use `SameServer`, not `Local`.

### Server won't start
```bash
lsof -i :8081  # Check what's using the port
cat testing/.backend.log  # Check error logs
```

### Frontend not loading
```bash
lsof -i :8941  # Check if Vite is running
cat testing/.frontend.log  # Check error logs
# Note: First start can take 60-120 seconds
```

### Token not working
The injected `sessionToken` is moved into the cookie on the next load, then removed; a token the server rejects is
just removed (you stay logged out).  The token format in localStorage must include inner quotes:
```javascript
// CORRECT
localStorage.setItem('sessionToken', '"refresh/User/uuid:token"');

// WRONG - missing inner quotes
localStorage.setItem('sessionToken', 'refresh/User/uuid:token');
```

## Adapting to Other Projects

See `SETUP-GUIDE.md` for:
- Complete list of files that need port changes
- Checklist for new project setup
- Common issues and solutions
- Port allocation suggestions for multiple projects
