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
1. Stops any existing servers
2. Starts backend on port 8081 (captures admin token)
3. Starts frontend on port 8941 (Vite with proxy to backend)
4. Outputs Chrome integration instructions

## Chrome Integration (Claude Code)

After running `prepare-browser-test.sh`, use Claude's Chrome MCP tools:

```
# 1. Get tab context
mcp__claude-in-chrome__tabs_context_mcp(createIfEmpty=true)

# 2. Navigate to the app
mcp__claude-in-chrome__navigate(tabId=..., url='http://localhost:8941')

# 3. Inject session token (IMPORTANT: use SameServer, not Local!)
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

## Scripts

| Script | Purpose |
|--------|---------|
| `prepare-browser-test.sh` | All-in-one: stops servers, starts fresh, outputs instructions |
| `start-backend.sh` | Start backend on :8081, save admin token |
| `start-frontend.sh` | Start Vite frontend on :8941 |
| `stop-all.sh` | Stop all test servers |
| `api.sh` | Make authenticated API calls |

## API Script Usage

```bash
./testing/api.sh GET /meta/health
./testing/api.sh GET /auth/session/self
./testing/api.sh POST /users/query '{"condition":{"Always":true}}'
VERBOSE=1 ./testing/api.sh GET /auth/session/self
```

## Configuration Files

- `settings.testing.json` - Server settings (port 8081, debug mode, console email)
- `apps/vite.config.mjs` - Vite config (port 8941, proxy to 8081)

## Generated Files (gitignored)

- `.admin-token` - Admin refresh token for bypassing login
- `.backend.log` - Backend server output
- `.frontend.log` - Vite server output
- `.browser-inject.js` - JS snippet for manual session injection

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
The token format in localStorage must include inner quotes:
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
