#!/bin/bash
# Start the backend server if not already running
# Usage: ./testing/start-backend.sh
#
# Uses testing/settings.testing.json for port 8081 (to avoid conflict with instaclub on 8080)

set -e
cd "$(dirname "$0")/.."

PORT=8081
LOG_FILE="testing/.backend.log"
TOKEN_FILE="testing/.admin-token"
# Use absolute path since gradle's workingDir may differ
SETTINGS_FILE="$(pwd)/testing/settings.testing.json"

# Check if server is already running
if curl -s "http://localhost:$PORT/meta/health" > /dev/null 2>&1; then
    echo "Backend server already running on port $PORT"
    if [[ -f "$TOKEN_FILE" ]]; then
        echo "Admin token available in $TOKEN_FILE"
    fi
    exit 0
fi

# Check if port is in use by something else
if lsof -i :$PORT > /dev/null 2>&1; then
    echo "ERROR: Port $PORT is in use by another process:"
    lsof -i :$PORT | head -3
    exit 1
fi

# Verify settings file exists
if [[ ! -f "$SETTINGS_FILE" ]]; then
    echo "ERROR: Settings file not found: $SETTINGS_FILE"
    exit 1
fi

echo "Starting backend server on port $PORT..."
echo "Using settings: $SETTINGS_FILE"

# Start server in background with testing settings
./gradlew :server:run --args="--settings $SETTINGS_FILE serve" > "$LOG_FILE" 2>&1 &
SERVER_PID=$!

echo "Waiting for server to start (PID: $SERVER_PID)..."

# Wait for server to be ready (max 90 seconds - first build can take a while)
for i in {1..90}; do
    if curl -s "http://localhost:$PORT/meta/health" > /dev/null 2>&1; then
        echo "Backend server started successfully on http://localhost:$PORT"

        # Extract admin token from log
        sleep 1  # Give it a moment to flush logs
        if grep -q "Admin token:" "$LOG_FILE"; then
            TOKEN=$(grep "Admin token:" "$LOG_FILE" | sed "s/.*Admin token: '\\([^']*\\)'.*/\\1/")
            echo "$TOKEN" > "$TOKEN_FILE"
            echo "Admin token saved to $TOKEN_FILE"
            echo "Token: $TOKEN"
        else
            echo "WARNING: No admin token found in logs (debug mode may need to be enabled)"
            echo "See: testing/README.md for instructions"
        fi
        exit 0
    fi

    # Check if process died
    if ! kill -0 $SERVER_PID 2>/dev/null; then
        echo "ERROR: Server process died. Check $LOG_FILE for details:"
        tail -20 "$LOG_FILE"
        exit 1
    fi

    # Show progress every 15 seconds
    if (( i % 15 == 0 )); then
        echo "Still waiting... ($i seconds)"
    fi

    sleep 1
done

echo "ERROR: Server failed to start within 90 seconds"
echo "Check $LOG_FILE for details:"
tail -30 "$LOG_FILE"
exit 1
