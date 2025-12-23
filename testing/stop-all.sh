#!/bin/bash
# Stop all test servers
# Usage: ./testing/stop-all.sh

cd "$(dirname "$0")/.."

echo "Stopping servers..."

# Stop backend on port 8081
if lsof -i :8081 > /dev/null 2>&1; then
    echo "Stopping backend on port 8081..."
    PID=$(lsof -t -i :8081 2>/dev/null || true)
    if [[ -n "$PID" ]]; then
        kill $PID 2>/dev/null || true
        sleep 1
        # Force kill if still running
        if kill -0 $PID 2>/dev/null; then
            kill -9 $PID 2>/dev/null || true
        fi
        echo "  Backend stopped"
    fi
else
    echo "  Backend not running"
fi

# Stop frontend on port 8941
if lsof -i :8941 > /dev/null 2>&1; then
    echo "Stopping frontend on port 8941..."
    PID=$(lsof -t -i :8941 2>/dev/null || true)
    if [[ -n "$PID" ]]; then
        kill $PID 2>/dev/null || true
        echo "  Frontend stopped"
    fi
else
    echo "  Frontend not running"
fi

echo "Done"
