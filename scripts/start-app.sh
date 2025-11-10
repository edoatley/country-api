#!/bin/bash

# Script to start the Spring Boot application in the background
# Usage: ./scripts/start-app.sh [log-file]
#   log-file: Optional path to log file (default: app.log)

set -e

LOG_FILE="${1:-app.log}"
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "🔧 Starting application in background..."
echo "   Log file: $LOG_FILE"

# Set environment variables
# Try to use localstack profile if it exists, otherwise use endpoint URL
if aws configure list-profiles 2>/dev/null | grep -q "^localstack$"; then
    export AWS_PROFILE=localstack
    unset AWS_ENDPOINT_URL
else
    # Fallback to endpoint URL if profile doesn't exist (e.g., in CI/CD)
    export AWS_ENDPOINT_URL=http://localhost:4566
    unset AWS_PROFILE
fi
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_REGION=us-east-1
export API_KEY=default-test-key
export DATA_SEEDING_ENABLED=true

# Start the application in the background
# Use nohup and disown to ensure the process persists across steps in CI
cd "$SCRIPT_DIR"
nohup ./gradlew :country-service-bootstrap:bootRun > "$LOG_FILE" 2>&1 &
APP_PID=$!
disown $APP_PID

# Save PID to file for later cleanup
echo "$APP_PID" > /tmp/country-service-app.pid
echo "$APP_PID" > "$SCRIPT_DIR/.app.pid"

# Wait for application to be ready
echo "⏳ Waiting for application to start..."
timeout 120 bash -c 'until curl -s http://localhost:8080/actuator/health | grep -q "\"status\":\"UP\""; do sleep 2; done' || {
    echo "❌ Application failed to start within 120 seconds"
    echo "=== Application logs ==="
    tail -100 "$LOG_FILE" || true
    kill $APP_PID 2>/dev/null || true
    exit 1
}

echo "✅ Application is ready (PID: $APP_PID)"

