#!/bin/bash
# Script to run API tests against staging environment

set -e

echo "🔍 Fetching staging API Gateway URL and API key..."

# Load .env file if it exists
if [ -f .env ]; then
  echo "📄 Loading environment variables from .env file..."
  export $(grep -v '^#' .env | xargs)
fi

# Get API Gateway URL (try from .env first, then CloudFormation)
if [ -n "$API_GATEWAY_URL" ] || [ -n "$API_TEST_BASE_URL" ]; then
  API_GATEWAY_ROOT_URL="${API_TEST_BASE_URL:-$API_GATEWAY_URL}"
  echo "✅ Using API Gateway URL from .env: $API_GATEWAY_ROOT_URL"
else
  echo "🔍 Fetching API Gateway URL from CloudFormation (optional)..."
  API_GATEWAY_ROOT_URL=$(aws cloudformation describe-stacks \
    --stack-name country-service-staging \
    --region us-east-1 \
    --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayRootUrl`].OutputValue' \
    --output text 2>/dev/null || echo "")

  if [ -z "$API_GATEWAY_ROOT_URL" ] || [ "$API_GATEWAY_ROOT_URL" == "None" ]; then
    API_GATEWAY_URL=$(aws cloudformation describe-stacks \
      --stack-name country-service-staging \
      --region us-east-1 \
      --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayUrl`].OutputValue' \
      --output text 2>/dev/null || echo "")
    
    if [ -z "$API_GATEWAY_URL" ] || [ "$API_GATEWAY_URL" == "None" ]; then
      echo "⚠️  Could not get API Gateway URL from CloudFormation"
      echo "Please set API_GATEWAY_URL or API_TEST_BASE_URL in .env file"
      echo "Example: API_TEST_BASE_URL=https://abc123.execute-api.us-east-1.amazonaws.com/staging/api/v1"
      exit 1
    fi
    API_GATEWAY_ROOT_URL="$API_GATEWAY_URL"
  fi
  echo "✅ Using API Gateway URL from CloudFormation: $API_GATEWAY_ROOT_URL"
fi

# Get API key (try from .env first, then Lambda environment)
if [ -n "$API_KEY" ] || [ -n "$API_TEST_API_KEY" ]; then
  API_KEY="${API_TEST_API_KEY:-$API_KEY}"
  echo "✅ Using API key from .env"
else
  echo "🔍 Fetching API key from Lambda function..."
  API_KEY=$(aws lambda get-function-configuration \
    --function-name country-service-lambda-staging \
    --region us-east-1 \
    --query 'Environment.Variables.API_KEY' \
    --output text 2>/dev/null || echo "")

  if [ -z "$API_KEY" ] || [ "$API_KEY" == "None" ]; then
    echo "❌ Error: Could not get API key from Lambda function or .env file"
    echo "Please set API_KEY or API_TEST_API_KEY in .env file"
    exit 1
  fi
  echo "✅ Using API key from Lambda function"
fi

echo "✅ API Gateway URL: $API_GATEWAY_ROOT_URL"
echo "✅ API Key: ${API_KEY:0:10}..."
echo ""

# Test connectivity first
echo "🔍 Testing API connectivity..."
CONNECTIVITY_RESPONSE=$(curl -s -f -H "X-API-KEY: $API_KEY" "$API_GATEWAY_ROOT_URL/countries?limit=1" 2>&1 || echo "FAILED")
if [ "$CONNECTIVITY_RESPONSE" != "FAILED" ]; then
  echo "✅ API is reachable"
  echo "Sample response (first 200 chars):"
  echo "$CONNECTIVITY_RESPONSE" | head -c 200
  echo ""
  echo ""
else
  echo "⚠️  API connectivity test failed"
  echo "Full URL tested: $API_GATEWAY_ROOT_URL/countries?limit=1"
  exit 1
fi

# Run the tests
echo "🧪 Running API tests against staging..."
export API_TEST_BASE_URL="$API_GATEWAY_ROOT_URL"
export API_TEST_API_KEY="$API_KEY"

./gradlew :country-service-api-tests:testStaging --no-daemon --info

echo ""
echo "✅ Tests completed. Check the output above for results."

