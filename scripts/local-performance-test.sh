#!/bin/bash

# Script to run performance tests against local application
# This script automates the entire setup and teardown process:
# 1. Starts LocalStack (if not running)
# 2. Sets up DynamoDB table and seeds data
# 3. Starts the application
# 4. Runs performance tests
# 5. Optionally stops the application
#
# Usage: ./scripts/local-performance-test.sh [--keep-running]
#   --keep-running: Keep the application running after tests complete

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
KEEP_RUNNING=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --keep-running)
            KEEP_RUNNING=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--keep-running]"
            exit 1
            ;;
    esac
done

# Cleanup function
cleanup() {
    if [ "$KEEP_RUNNING" = false ]; then
        echo ""
        echo "🧹 Cleaning up..."
        "$SCRIPT_DIR/stop-app.sh" /tmp/performance-test-app.log || true
    else
        echo ""
        echo "ℹ️  Application is still running (PID saved in /tmp/country-service-app.pid)"
        echo "   To stop it later, run: ./scripts/stop-app.sh"
    fi
}

# Set trap to cleanup on exit
trap cleanup EXIT

echo "🚀 Starting Performance Test Setup"
echo "=================================="
echo ""

# Step 1: Start LocalStack (if not running)
echo "1️⃣  Checking LocalStack..."
if ! curl -s http://localhost:4566/_localstack/health > /dev/null 2>&1; then
    echo "   Starting LocalStack..."
    "$SCRIPT_DIR/start-localstack.sh"
else
    echo "   ✅ LocalStack is already running"
fi
echo ""

# Step 2: Set up DynamoDB table and seed data
echo "2️⃣  Setting up DynamoDB table and seeding data..."
# Check if table exists and has data (need at least a few countries for all tests)
if aws dynamodb describe-table --table-name Countries --endpoint-url http://localhost:4566 --region us-east-1 > /dev/null 2>&1; then
    COUNT=$(aws dynamodb scan \
        --endpoint-url http://localhost:4566 \
        --region us-east-1 \
        --table-name Countries \
        --select COUNT \
        --query 'Count' \
        --output text 2>/dev/null || echo "0")
    
    # Need at least 10 countries for comprehensive testing
    if [ "$COUNT" -gt 10 ]; then
        echo "   ✅ DynamoDB table exists with $COUNT items (sufficient for testing)"
    else
        echo "   Table exists but has insufficient data ($COUNT items), setting up..."
        "$SCRIPT_DIR/setup-local-dynamodb.sh"
        # Verify seeding completed
        NEW_COUNT=$(aws dynamodb scan \
            --endpoint-url http://localhost:4566 \
            --region us-east-1 \
            --table-name Countries \
            --select COUNT \
            --query 'Count' \
            --output text 2>/dev/null || echo "0")
        if [ "$NEW_COUNT" -gt 10 ]; then
            echo "   ✅ Data seeded successfully ($NEW_COUNT items)"
        else
            echo "   ⚠️  Warning: Only $NEW_COUNT items found after seeding"
        fi
    fi
else
    echo "   Table doesn't exist, setting up..."
    "$SCRIPT_DIR/setup-local-dynamodb.sh"
    # Verify seeding completed
    NEW_COUNT=$(aws dynamodb scan \
        --endpoint-url http://localhost:4566 \
        --region us-east-1 \
        --table-name Countries \
        --select COUNT \
        --query 'Count' \
        --output text 2>/dev/null || echo "0")
    if [ "$NEW_COUNT" -gt 10 ]; then
        echo "   ✅ Data seeded successfully ($NEW_COUNT items)"
    else
        echo "   ⚠️  Warning: Only $NEW_COUNT items found after seeding"
    fi
fi
echo ""

# Step 3: Start the application
echo "3️⃣  Starting application..."
# Check if application is already running
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "   ✅ Application is already running on port 8080"
    echo "   ⚠️  Using existing instance (may affect test results)"
else
    echo "   Starting new application instance..."
    "$SCRIPT_DIR/start-app.sh" /tmp/performance-test-app.log
    echo "   ✅ Application started"
fi
echo ""

# Step 4: Verify application is ready
echo "4️⃣  Verifying application is ready..."
if ! curl -s http://localhost:8080/actuator/health | grep -q "\"status\":\"UP\""; then
    echo "   ⏳ Waiting for application to be ready..."
    # Cross-platform timeout: use timeout command if available, otherwise use a loop
    if command -v timeout >/dev/null 2>&1; then
        # Linux: use timeout command
        timeout 30 bash -c 'until curl -s http://localhost:8080/actuator/health | grep -q "\"status\":\"UP\""; do sleep 1; done' || {
            echo "   ❌ Application failed to become ready"
            echo "   === Application logs ==="
            tail -50 /tmp/performance-test-app.log 2>/dev/null || true
            exit 1
        }
    else
        # macOS/BSD: use a loop with elapsed time check
        MAX_WAIT=30
        ELAPSED=0
        while [ $ELAPSED -lt $MAX_WAIT ]; do
            if curl -s http://localhost:8080/actuator/health | grep -q "\"status\":\"UP\""; then
                break
            fi
            sleep 1
            ELAPSED=$((ELAPSED + 1))
        done
        if [ $ELAPSED -ge $MAX_WAIT ]; then
            echo "   ❌ Application failed to become ready"
            echo "   === Application logs ==="
            tail -50 /tmp/performance-test-app.log 2>/dev/null || true
            exit 1
        fi
    fi
fi
echo "   ✅ Application is ready"
echo ""

# Step 5: Run performance tests
echo "5️⃣  Running performance tests..."
echo "   This may take a few moments..."
echo ""
cd "$PROJECT_ROOT"

# Run the performance tests
if ./gradlew :country-service-api-tests:testPerformanceLocal --no-daemon; then
    echo ""
    echo "✅ Performance tests completed successfully!"
    echo ""
    echo "📊 Test Results:"
    echo "   Check the output above for response time measurements"
    echo "   All endpoints should have response times < 200ms"
    TEST_RESULT=0
else
    echo ""
    echo "❌ Performance tests failed!"
    echo ""
    echo "Troubleshooting:"
    echo "   - Check if any endpoints exceeded the 200ms requirement"
    echo "   - Verify the application logs: tail -f /tmp/performance-test-app.log"
    echo "   - Ensure LocalStack is running: docker compose ps"
    TEST_RESULT=1
fi

# Exit with test result
exit $TEST_RESULT

