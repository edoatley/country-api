#!/bin/bash
# Script to invoke Lambda function directly (bypassing API Gateway)
# This helps debug if the issue is with Lambda or API Gateway

set -e

echo "=== Invoking Lambda Function Directly ==="
echo ""

# Load .env file if it exists
if [ -f .env ]; then
  echo "📄 Loading environment variables from .env file..."
  export $(grep -v '^#' .env | xargs)
fi

# Use streaming profile for AWS (not LocalStack)
# Check if profile exists, otherwise use default
if aws configure list-profiles 2>/dev/null | grep -q "^streaming$"; then
    export AWS_PROFILE=streaming
    unset AWS_ENDPOINT_URL
else
    echo "⚠️  Warning: 'streaming' profile not found, using default AWS configuration"
    unset AWS_PROFILE
    unset AWS_ENDPOINT_URL
fi

# Get API key from .env or use default
API_KEY="${API_KEY:-A4I2cFtYgZHxLDncqFTMXgzKrMW9+omZOS3ZM9Vbg6g=}"

# Lambda function name
LAMBDA_FUNCTION="country-service-lambda-staging"
REGION="us-east-1"

echo "Lambda function: $LAMBDA_FUNCTION"
echo "Region: $REGION"
echo "API Key: ${API_KEY:0:10}..."
echo ""

# Test 1: List countries
echo "=== Test 1: List Countries ==="
echo "Invoking Lambda with GET /api/v1/countries"
cat > /tmp/lambda-event-list.json <<EOF
{
  "httpMethod": "GET",
  "path": "/api/v1/countries",
  "queryStringParameters": {
    "limit": "3",
    "offset": "0"
  },
  "headers": {
    "X-API-KEY": "$API_KEY"
  },
  "body": null
}
EOF

echo "Event payload:"
cat /tmp/lambda-event-list.json | jq '.' 2>/dev/null || cat /tmp/lambda-event-list.json
echo ""

echo "Lambda response:"
aws lambda invoke \
  --function-name "$LAMBDA_FUNCTION" \
  --region "$REGION" \
  --payload file:///tmp/lambda-event-list.json \
  /tmp/lambda-response-list.json 2>&1

echo ""
echo "Response body:"
cat /tmp/lambda-response-list.json | jq -r '.body' 2>/dev/null | jq '.' 2>/dev/null || cat /tmp/lambda-response-list.json
echo ""

# Test 2: Get country by code
echo "=== Test 2: Get Country by Code ==="
echo "Invoking Lambda with GET /api/v1/countries/code/BZ"
cat > /tmp/lambda-event-get.json <<EOF
{
  "httpMethod": "GET",
  "path": "/api/v1/countries/code/BZ",
  "pathParameters": {
    "alpha2Code": "BZ"
  },
  "headers": {
    "X-API-KEY": "$API_KEY"
  },
  "body": null
}
EOF

echo "Event payload:"
cat /tmp/lambda-event-get.json | jq '.' 2>/dev/null || cat /tmp/lambda-event-get.json
echo ""

echo "Lambda response:"
aws lambda invoke \
  --function-name "$LAMBDA_FUNCTION" \
  --region "$REGION" \
  --payload file:///tmp/lambda-event-get.json \
  /tmp/lambda-response-get.json 2>&1

echo ""
echo "Response body:"
cat /tmp/lambda-response-get.json | jq -r '.body' 2>/dev/null | jq '.' 2>/dev/null || cat /tmp/lambda-response-get.json
echo ""

# Test 3: Create country
echo "=== Test 3: Create Country ==="
echo "Invoking Lambda with POST /api/v1/countries"
cat > /tmp/lambda-event-create.json <<EOF
{
  "httpMethod": "POST",
  "path": "/api/v1/countries",
  "headers": {
    "X-API-KEY": "$API_KEY",
    "Content-Type": "application/json"
  },
  "body": "{\"name\":\"Test Country Direct\",\"alpha2Code\":\"TD\",\"alpha3Code\":\"TDD\",\"numericCode\":\"888\"}"
}
EOF

echo "Event payload:"
cat /tmp/lambda-event-create.json | jq '.' 2>/dev/null || cat /tmp/lambda-event-create.json
echo ""

echo "Lambda response:"
aws lambda invoke \
  --function-name "$LAMBDA_FUNCTION" \
  --region "$REGION" \
  --payload file:///tmp/lambda-event-create.json \
  /tmp/lambda-response-create.json 2>&1

echo ""
echo "Response body:"
cat /tmp/lambda-response-create.json | jq -r '.body' 2>/dev/null | jq '.' 2>/dev/null || cat /tmp/lambda-response-create.json
echo ""

# Cleanup
rm -f /tmp/lambda-event-*.json /tmp/lambda-response-*.json

echo ""
echo "=== Summary ==="
echo "If Lambda direct invocation returns correct data, the issue is with API Gateway."
echo "If Lambda direct invocation also returns {deleted: false}, the issue is with Lambda serialization."

