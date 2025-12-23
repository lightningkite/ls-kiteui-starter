#!/bin/bash
# Prepare everything for browser testing from scratch
# Usage: ./testing/prepare-browser-test.sh
#
# This script:
# 1. Stops any existing servers
# 2. Starts backend and captures admin token
# 3. Starts frontend (Vite)
# 4. Outputs Chrome integration instructions
#
# After running, use Claude's Chrome integration (mcp__claude-in-chrome__*) for testing

set -e
cd "$(dirname "$0")/.."

TOKEN_FILE="testing/.admin-token"
FRONTEND_PORT=""

echo "============================================"
echo "  PREPARING BROWSER TEST ENVIRONMENT"
echo "============================================"
echo ""

# Step 1: Clean slate - stop existing servers
echo "[1/2] Stopping any existing servers..."
./testing/stop-all.sh 2>/dev/null || true
echo ""

# Step 2: Start backend AND frontend in parallel
echo "[2/2] Starting backend and frontend in parallel..."
./testing/start-backend.sh &
BACKEND_PID=$!
./testing/start-frontend.sh &
FRONTEND_PID=$!

# Wait for both to complete
BACKEND_OK=0
FRONTEND_OK=0

wait $BACKEND_PID && BACKEND_OK=1
wait $FRONTEND_PID && FRONTEND_OK=1

if [[ $BACKEND_OK -eq 0 ]]; then
    echo "ERROR: Backend failed to start"
    exit 1
fi

if [[ $FRONTEND_OK -eq 0 ]]; then
    echo "ERROR: Frontend failed to start"
    exit 1
fi

# Get token from backend
TOKEN=""
if [[ -f "$TOKEN_FILE" ]]; then
    TOKEN=$(cat "$TOKEN_FILE")
fi

# Frontend is on port 8941 (configured in vite.config.mjs)
FRONTEND_PORT=8941
echo ""

# Create a file with the injection code for easy copy-paste
INJECT_FILE="testing/.browser-inject.js"
cat > "$INJECT_FILE" << EOF
// Paste this in browser console at http://localhost:${FRONTEND_PORT}
// Uses SameServer API option so requests go through Vite proxy to backend on 8081
localStorage.setItem('apiOption', '"SameServer"');
localStorage.setItem('sessionToken', '"${TOKEN}"');
location.reload();
EOF

# Save URLs for easy access
echo "http://localhost:${FRONTEND_PORT}" > testing/.frontend-url
echo "http://localhost:8081" > testing/.backend-url

echo "============================================"
echo "  BROWSER TEST ENVIRONMENT READY"
echo "============================================"
echo ""
echo "  Backend:  http://localhost:8081"
echo "  Frontend: http://localhost:${FRONTEND_PORT}"
echo ""
if [[ -n "$TOKEN" ]]; then
echo "============================================"
echo "  CHROME INTEGRATION (Claude Code):"
echo "============================================"
echo ""
echo "  Use Claude's Chrome integration tools (mcp__claude-in-chrome__*):"
echo ""
echo "  1. Get tab context:"
echo "     mcp__claude-in-chrome__tabs_context_mcp"
echo ""
echo "  2. Navigate to app:"
echo "     mcp__claude-in-chrome__navigate(url='http://localhost:${FRONTEND_PORT}')"
echo ""
echo "  3. Inject session (via JavaScript tool):"
echo "     mcp__claude-in-chrome__javascript_tool with:"
echo "     localStorage.setItem('apiOption', '\"SameServer\"');"
echo "     localStorage.setItem('sessionToken', '\"${TOKEN}\"');"
echo "     location.reload();"
echo ""
echo "  NOTE: Use 'SameServer' so requests go through Vite proxy to backend on 8081"
echo ""
echo "  4. Take screenshots:"
echo "     mcp__claude-in-chrome__computer(action='screenshot')"
echo ""
echo "  5. Interact with elements:"
echo "     mcp__claude-in-chrome__find(query='...')"
echo "     mcp__claude-in-chrome__form_input(ref='...', value='...')"
echo "     mcp__claude-in-chrome__computer(action='left_click', coordinate=[x,y])"
echo ""
echo "============================================"
echo "  MANUAL BROWSER LOGIN (if needed):"
echo "============================================"
echo ""
echo "  Paste in browser console at http://localhost:${FRONTEND_PORT}:"
echo ""
cat "$INJECT_FILE" | sed 's/^/  /'
echo ""
else
echo "  NOTE: No admin token found. You'll need to log in manually."
echo "  Add debug token feature to server - see testing/README.md"
echo ""
fi
echo "============================================"
echo ""
