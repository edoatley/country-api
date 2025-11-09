#!/bin/bash

# Convenience script to set up LocalStack, create DynamoDB table, and seed data
# This script uses the modular scripts for each step
# Usage: ./scripts/setup-local-dynamodb.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "🔧 Setting up Local DynamoDB table and seeding data..."
echo ""

# Step 1: Start LocalStack (if not already running)
if ! curl -s http://localhost:4566/_localstack/health > /dev/null 2>&1; then
    echo "1. Starting LocalStack..."
    "$SCRIPT_DIR/start-localstack.sh"
    echo ""
else
    echo "✅ LocalStack is already running"
    echo ""
fi

# Step 2: Create DynamoDB table
echo "2. Creating DynamoDB table..."
"$SCRIPT_DIR/create-dynamodb-table.sh"
echo ""

# Step 3: Start application to seed data
echo "3. Starting application to seed data..."
cd "$PROJECT_ROOT"
"$SCRIPT_DIR/start-app.sh" /tmp/seed-data.log
echo ""

# Step 4: Verify data was seeded
echo "4. Verifying data..."
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test

COUNT=$(aws dynamodb scan \
    --endpoint-url $AWS_ENDPOINT_URL \
    --table-name Countries \
    --select COUNT \
    --query 'Count' \
    --output text 2>/dev/null || echo "0")

if [ "$COUNT" -gt 0 ]; then
    echo "✅ Found $COUNT items in the table"
else
    echo "⚠️  No items found in table. Seeding may have failed."
    echo "   Check /tmp/seed-data.log for details"
fi
echo ""

# Step 5: Stop the application (it was started just for seeding)
echo "5. Stopping application..."
"$SCRIPT_DIR/stop-app.sh" /tmp/seed-data.log
echo ""

echo "✅ Local DynamoDB setup complete!"
echo ""
echo "You can now start the application:"
echo "  ./scripts/start-app.sh"
echo ""
echo "Or test the API:"
echo "  curl -H 'X-API-KEY: default-test-key' http://localhost:8080/api/v1/countries?limit=5"

