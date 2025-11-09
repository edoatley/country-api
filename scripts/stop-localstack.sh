#!/bin/bash

# Script to stop LocalStack
# Usage: ./scripts/stop-localstack.sh

set -e

echo "🔧 Stopping LocalStack..."

docker compose down -v

echo "✅ LocalStack stopped"

