#!/bin/bash
# Start the Vite frontend dev server if not already running
# Usage: ./testing/start-frontend.sh
#
# Uses port 8941 (configured in apps/vite.config.mjs)
# Vite proxies /api requests to backend on port 8081

set -e
cd "$(dirname "$0")/.."

LOG_FILE="testing/.frontend.log"
PORT=8941

# Check if Vite is already running
if curl -s "http://localhost:$PORT/" 2>/dev/null | grep -q -E "(vite|KiteUI|html)"; then
    echo "Frontend already running on http://localhost:$PORT"
    exit 0
fi

# Check if port is in use by something else
if lsof -i :$PORT > /dev/null 2>&1; then
    echo "WARNING: Port $PORT is in use. Checking if it's our frontend..."
    if curl -s "http://localhost:$PORT/" > /dev/null 2>&1; then
        echo "Frontend running on http://localhost:$PORT"
        exit 0
    fi
    echo "ERROR: Port $PORT is in use by another process:"
    lsof -i :$PORT | head -3
    exit 1
fi

echo "Starting Vite frontend dev server..."

# Start Vite in background
./gradlew :apps:jsViteDev > "$LOG_FILE" 2>&1 &
VITE_PID=$!

echo "Waiting for Vite to start (PID: $VITE_PID)..."

# Wait for Vite to be ready (max 120 seconds - first build takes a while)
for i in {1..120}; do
    if curl -s "http://localhost:$PORT/" 2>/dev/null | grep -q -E "(vite|html)"; then
        echo "Frontend started successfully on http://localhost:$PORT"
        echo ""
        echo "IMPORTANT: In the browser, select 'SameServer' API option for testing"
        echo "           (this uses the Vite proxy to reach the backend on port 8081)"
        exit 0
    fi

    # Check if process died
    if ! kill -0 $VITE_PID 2>/dev/null; then
        echo "ERROR: Vite process died. Check $LOG_FILE for details:"
        tail -30 "$LOG_FILE"
        exit 1
    fi

    # Show progress every 15 seconds
    if (( i % 15 == 0 )); then
        echo "Still waiting... ($i seconds)"
    fi

    sleep 1
done

echo "ERROR: Frontend failed to start within 120 seconds"
echo "Check $LOG_FILE for details:"
tail -30 "$LOG_FILE"
exit 1
