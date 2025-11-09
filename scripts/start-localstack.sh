#!/bin/bash

# Script to start LocalStack and wait for it to be ready
# Usage: ./scripts/start-localstack.sh

set -e

echo "🔧 Starting LocalStack..."

# Start LocalStack
docker-compose up -d

# Wait for LocalStack to be ready
echo "⏳ Waiting for LocalStack to be ready..."
timeout 60 bash -c 'until curl -s http://localhost:4566/_localstack/health | grep -q "\"dynamodb\": \"available\""; do sleep 2; done' || {
    echo "❌ LocalStack failed to start within 60 seconds"
    docker-compose logs localstack
    exit 1
}

echo "✅ LocalStack is ready"

