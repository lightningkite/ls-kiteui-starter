#!/bin/bash
# Start everything needed for testing
# Usage: ./testing/start-all.sh

set -e
cd "$(dirname "$0")/.."

echo "=== Starting Test Environment ==="
echo ""

# Start backend
echo "--- Backend ---"
./testing/start-backend.sh
echo ""

# Start frontend
echo "--- Frontend ---"
./testing/start-frontend.sh
echo ""

echo "=== Test Environment Ready ==="
echo ""
echo "Backend:  http://localhost:8081"
echo "Frontend: http://localhost:8951 (or 8952-8955 if port was busy)"
echo ""
echo "Quick API test:"
echo "  ./testing/api.sh GET /meta/health"
echo "  ./testing/api.sh GET /auth/session/self"
echo ""
echo "To stop everything:"
echo "  ./testing/stop-all.sh"
