#!/bin/bash
# Start the Vite frontend dev server if not already running
# Usage: ./testing/start-frontend.sh
#
# Uses port 8941 (configured in apps/vite/vite.config.mjs.template)
# Vite proxies /api requests to backend on port 8081

set -e
cd "$(dirname "$0")/.."

LOG_FILE="testing/.frontend.log"
PID_FILE="testing/.frontend.pid"
PORT=8941

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "Frontend already running (PID $(cat "$PID_FILE")) on http://localhost:$PORT"
    exit 0
fi

if nc -z localhost $PORT > /dev/null 2>&1; then
    echo "ERROR: Port $PORT is in use by a process these scripts didn't start (another project's frontend?)."
    echo "       Free the port first; stop-all.sh only stops servers these scripts started."
    exit 1
fi

echo "Starting Vite frontend dev server..."

# Start Vite in background
./gradlew $GRADLE_ARGS :apps:jsViteDev > "$LOG_FILE" 2>&1 &
VITE_PID=$!
echo $VITE_PID > "$PID_FILE"

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
        rm "$PID_FILE"
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

echo "ERROR: Frontend failed to start within 120 seconds; it may still be starting (stop it with ./testing/stop-all.sh)"
echo "Check $LOG_FILE for details:"
tail -30 "$LOG_FILE"
exit 1
