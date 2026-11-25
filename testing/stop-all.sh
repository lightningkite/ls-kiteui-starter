#!/bin/bash
# Stop the test servers these scripts started (recorded in testing/.backend.pid and testing/.frontend.pid).
# Whatever else holds ports 8081 or 8941, such as another project's server, is left alone.
# Usage: ./testing/stop-all.sh

cd "$(dirname "$0")/.."
PROJECT_DIR="$(pwd -P)"

echo "Stopping servers..."

for NAME in backend frontend; do
    PID_FILE="testing/.$NAME.pid"
    if [[ ! -f "$PID_FILE" ]]; then
        echo "  $NAME: not running"
        continue
    fi
    PID=$(cat "$PID_FILE")
    rm "$PID_FILE"

    # A PID file left over from before a reboot may name a PID the OS has since given to something else.
    # Both start scripts record `./gradlew`, which execs java, run from this directory.
    # lsof rather than ps, since ps is unavailable in some sandboxes.
    INFO=$(lsof -a -p "$PID" -d cwd -Fcn 2>/dev/null)
    COMMAND=$(printf '%s\n' "$INFO" | sed -n 's/^c//p')
    CWD=$(printf '%s\n' "$INFO" | sed -n 's/^n//p')
    if [[ -z "$INFO" ]]; then
        echo "  $NAME: already exited"
        continue
    fi
    if [[ "$COMMAND" != "java" || "$CWD" != "$PROJECT_DIR" ]]; then
        echo "  $NAME: PID $PID is now another process ($COMMAND in $CWD); left it alone"
        continue
    fi

    kill "$PID"
    for i in {1..10}; do
        kill -0 "$PID" 2>/dev/null || break
        sleep 1
    done
    if kill -0 "$PID" 2>/dev/null; then
        kill -9 "$PID"
    fi
    echo "  $NAME stopped (PID $PID)"
done

echo "Done"
