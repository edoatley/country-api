#!/bin/bash

# Script to start LocalStack and wait for it to be ready
# Usage: ./scripts/start-localstack.sh

set -e

echo "🔧 Starting LocalStack..."

# Start LocalStack
docker compose up -d

# Wait for LocalStack to be ready
echo "⏳ Waiting for LocalStack to be ready..."
# Cross-platform timeout: use timeout command if available, otherwise use a loop
if command -v timeout >/dev/null 2>&1; then
    # Linux: use timeout command
    timeout 60 bash -c 'until curl -s http://localhost:4566/_localstack/health | grep -q "\"dynamodb\": \"available\""; do sleep 2; done' || {
        echo "❌ LocalStack failed to start within 60 seconds"
        docker compose logs localstack
        exit 1
    }
else
    # macOS/BSD: use a loop with elapsed time check
    MAX_WAIT=60
    ELAPSED=0
    while [ $ELAPSED -lt $MAX_WAIT ]; do
        if curl -s http://localhost:4566/_localstack/health | grep -q "\"dynamodb\": \"available\""; then
            break
        fi
        sleep 2
        ELAPSED=$((ELAPSED + 2))
    done
    if [ $ELAPSED -ge $MAX_WAIT ]; then
        echo "❌ LocalStack failed to start within 60 seconds"
        docker compose logs localstack
        exit 1
    fi
fi

echo "✅ LocalStack is ready"

