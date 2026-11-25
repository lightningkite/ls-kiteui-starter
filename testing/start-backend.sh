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
PID_FILE="testing/.backend.pid"
# Use absolute path since gradle's workingDir may differ
SETTINGS_FILE="$(pwd)/testing/settings.testing.json"

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "Backend server already running (PID $(cat "$PID_FILE")) on port $PORT"
    if [[ -f "$TOKEN_FILE" ]]; then
        echo "Admin token available in $TOKEN_FILE"
    fi
    exit 0
fi

if nc -z localhost $PORT > /dev/null 2>&1; then
    echo "ERROR: Port $PORT is in use by a process these scripts didn't start (another project's server?)."
    echo "       Free the port first; stop-all.sh only stops servers these scripts started."
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
echo $SERVER_PID > "$PID_FILE"

echo "Waiting for server to start (PID: $SERVER_PID)..."

# Wait for server to be ready (max 90 seconds - first build can take a while)
for i in {1..90}; do
    if curl -s "http://localhost:$PORT/meta/health" > /dev/null 2>&1; then
        echo "Backend server started successfully on http://localhost:$PORT"
        exit 0
    fi

    # Check if process died
    if ! kill -0 $SERVER_PID 2>/dev/null; then
        rm "$PID_FILE"
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

echo "ERROR: Server failed to start within 90 seconds; it may still be starting (stop it with ./testing/stop-all.sh)"
echo "Check $LOG_FILE for details:"
tail -30 "$LOG_FILE"
exit 1
