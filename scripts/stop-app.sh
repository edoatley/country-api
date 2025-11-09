#!/bin/bash

# Script to stop the Spring Boot application
# Usage: ./scripts/stop-app.sh [log-file]
#   log-file: Optional path to log file (default: app.log)

set -e

LOG_FILE="${1:-app.log}"
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "🔧 Stopping application..."

# Try to get PID from file
if [ -f /tmp/country-service-app.pid ]; then
    APP_PID=$(cat /tmp/country-service-app.pid)
    rm -f /tmp/country-service-app.pid
elif [ -f "$SCRIPT_DIR/.app.pid" ]; then
    APP_PID=$(cat "$SCRIPT_DIR/.app.pid")
    rm -f "$SCRIPT_DIR/.app.pid"
else
    # Try to find the process
    APP_PID=$(pgrep -f "country-service-bootstrap" || echo "")
fi

if [ -n "$APP_PID" ]; then
    echo "   Stopping process $APP_PID..."
    kill $APP_PID 2>/dev/null || true
    wait $APP_PID 2>/dev/null || true
    echo "✅ Application stopped"
else
    echo "⚠️  No application process found"
fi

# Also try to kill any remaining Java processes
pkill -f "country-service-bootstrap" 2>/dev/null || true

# Show application logs if they exist
if [ -f "$LOG_FILE" ]; then
    echo ""
    echo "=== Application logs ==="
    tail -100 "$LOG_FILE" || true
fi

