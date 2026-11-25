#!/bin/bash
# Stop the test servers these scripts started (recorded in testing/.backend.pid and testing/.frontend.pid).
# Whatever else holds ports 8081 or 8941, such as another project's server, is left alone.
# Usage: ./testing/stop-all.sh

cd "$(dirname "$0")/.."

echo "Stopping servers..."

for NAME in backend frontend; do
    PID_FILE="testing/.$NAME.pid"
    if [[ ! -f "$PID_FILE" ]]; then
        echo "  $NAME: not running"
        continue
    fi
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        kill "$PID"
        for i in {1..10}; do
            kill -0 "$PID" 2>/dev/null || break
            sleep 1
        done
        if kill -0 "$PID" 2>/dev/null; then
            kill -9 "$PID"
        fi
        echo "  $NAME stopped (PID $PID)"
    else
        echo "  $NAME: already exited"
    fi
    rm "$PID_FILE"
done

echo "Done"
