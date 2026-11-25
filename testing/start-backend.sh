#!/bin/bash
# Seed sample data, save the seeded admin's refresh token, and start the backend server if not already running
# Usage: ./testing/start-backend.sh
#        GRADLE_ARGS=--offline ./testing/start-backend.sh   (extra Gradle arguments, e.g. without network access)
#
# Uses testing/settings.testing.json for port 8081 (to avoid conflict with instaclub on 8080)

set -e
cd "$(dirname "$0")/.."

PORT=8081
LOG_FILE="testing/.backend.log"
SEED_LOG_FILE="testing/.seed.log"
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

echo "Using settings: $SETTINGS_FILE"

# Seeding resets the seed rows (hand-made rows survive) and prints a fresh admin refresh token.  It only runs with general.debug.
echo "Seeding sample data..."
if ! ./gradlew $GRADLE_ARGS :server:run --args="--settings $SETTINGS_FILE seed" > "$SEED_LOG_FILE" 2>&1; then
    echo "ERROR: Seeding failed. Check $SEED_LOG_FILE for details:"
    tail -20 "$SEED_LOG_FILE"
    exit 1
fi
sed -n "s/^Admin token: '\(.*\)'$/\1/p" "$SEED_LOG_FILE" > "$TOKEN_FILE"
if [[ ! -s "$TOKEN_FILE" ]]; then
    echo "ERROR: Seeding printed no admin token. Check $SEED_LOG_FILE"
    exit 1
fi
echo "Admin refresh token saved to $TOKEN_FILE"

echo "Starting backend server on port $PORT..."
./gradlew $GRADLE_ARGS :server:run --args="--settings $SETTINGS_FILE dev" > "$LOG_FILE" 2>&1 &
SERVER_PID=$!

echo "Waiting for server to start (PID: $SERVER_PID)..."

# Wait for server to be ready (max 90 seconds - first build can take a while)
for i in {1..90}; do
    if curl -s "http://localhost:$PORT/meta/health" > /dev/null 2>&1; then
        echo "Backend server started successfully on http://localhost:$PORT"
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
