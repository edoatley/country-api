#!/bin/bash

# Script to create DynamoDB table in LocalStack (without seeding data)
# Usage: ./scripts/create-dynamodb-table.sh

set -e

echo "🔧 Creating DynamoDB table..."

# Check if LocalStack is running
if ! curl -s http://localhost:4566/_localstack/health > /dev/null 2>&1; then
    echo "❌ LocalStack is not running. Please start it first:"
    echo "   ./scripts/start-localstack.sh"
    exit 1
fi

# Set AWS environment variables for LocalStack
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test

# Create table using AWS CLI
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

# Wait for table to be active
echo "⏳ Waiting for table to be active..."
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
    if [ $i -eq 30 ]; then
        echo "❌ Table failed to become active within 30 seconds"
        exit 1
    fi
    sleep 1
done

echo "✅ DynamoDB table created and ready"

