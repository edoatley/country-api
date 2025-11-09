#!/bin/bash

# Script to create DynamoDB table in LocalStack and seed it with data
# This makes the local environment ready for testing

set -e

echo "🔧 Setting up Local DynamoDB table and seeding data..."
echo ""

# Check if LocalStack is running
if ! curl -s http://localhost:4566/_localstack/health > /dev/null 2>&1; then
    echo "❌ LocalStack is not running. Please start it first:"
    echo "   podman compose up -d"
    exit 1
fi
echo "✅ LocalStack is running"
echo ""

# Set AWS environment variables for LocalStack
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test

# Create table using AWS CLI
echo "1. Creating DynamoDB table..."
aws dynamodb create-table \
    --endpoint-url $AWS_ENDPOINT_URL \
    --table-name Countries \
    --attribute-definitions \
        AttributeName=alpha2Code,AttributeType=S \
        AttributeName=createDate,AttributeType=S \
        AttributeName=alpha3Code,AttributeType=S \
        AttributeName=numericCode,AttributeType=S \
    --key-schema \
        AttributeName=alpha2Code,KeyType=HASH \
        AttributeName=createDate,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        "[
            {
                \"IndexName\": \"GSI-Alpha3\",
                \"KeySchema\": [
                    {\"AttributeName\": \"alpha3Code\", \"KeyType\": \"HASH\"},
                    {\"AttributeName\": \"createDate\", \"KeyType\": \"RANGE\"}
                ],
                \"Projection\": {
                    \"ProjectionType\": \"ALL\"
                }
            },
            {
                \"IndexName\": \"GSI-Numeric\",
                \"KeySchema\": [
                    {\"AttributeName\": \"numericCode\", \"KeyType\": \"HASH\"},
                    {\"AttributeName\": \"createDate\", \"KeyType\": \"RANGE\"}
                ],
                \"Projection\": {
                    \"ProjectionType\": \"ALL\"
                }
            }
        ]" \
    2>&1 | grep -v "ResourceInUseException" || echo "✅ Table already exists or created successfully"
echo ""

# Wait for table to be active
echo "2. Waiting for table to be active..."
for i in {1..30}; do
    STATUS=$(aws dynamodb describe-table \
        --endpoint-url $AWS_ENDPOINT_URL \
        --table-name Countries \
        --query 'Table.TableStatus' \
        --output text 2>/dev/null || echo "CREATING")
    
    if [ "$STATUS" = "ACTIVE" ]; then
        echo "✅ Table is active"
        break
    fi
    echo "   Waiting... ($i/30) Status: $STATUS"
    sleep 1
done
echo ""

# Seed data by enabling seeding and running the app briefly
echo "3. Seeding data from CSV..."
export DATA_SEEDING_ENABLED=true
export API_KEY=default-test-key

# Run the app in the background, wait for seeding, then stop it
cd "$(dirname "$0")/.."
timeout 60 ./gradlew :country-service-bootstrap:bootRun > /tmp/seed-data.log 2>&1 &
SEED_PID=$!

# Wait for seeding to complete (check health endpoint)
echo "   Waiting for seeding to complete..."
for i in {1..60}; do
    HEALTH=$(curl -s http://localhost:8080/actuator/health 2>/dev/null || echo "DOWN")
    if echo "$HEALTH" | grep -q '"status":"UP"'; then
        echo "✅ Seeding completed"
        sleep 2
        kill $SEED_PID 2>/dev/null || true
        wait $SEED_PID 2>/dev/null || true
        break
    fi
    if [ $i -eq 60 ]; then
        echo "⚠️  Seeding may still be in progress. Check /tmp/seed-data.log"
        kill $SEED_PID 2>/dev/null || true
        break
    fi
    sleep 1
done
echo ""

# Verify data was seeded
echo "4. Verifying data..."
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

echo "✅ Local DynamoDB setup complete!"
echo ""
echo "You can now test the API:"
echo "  curl -H 'X-API-KEY: default-test-key' http://localhost:8080/api/v1/countries?limit=5"

