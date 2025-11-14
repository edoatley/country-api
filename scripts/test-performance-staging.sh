#!/bin/bash
# Script to run performance tests against staging environment

set -e

echo "🚀 Performance Tests - Staging Environment"
echo "=========================================="
echo ""

# Use streaming profile for all AWS operations (staging environment)
# This ensures we use real AWS, not LocalStack
export AWS_PROFILE=streaming

# Check AWS credentials and attempt SSO login if needed
echo "🔐 Checking AWS credentials (profile: streaming)..."
if ! aws sts get-caller-identity --profile streaming >/dev/null 2>&1; then
    echo "⚠️  AWS credentials not valid or expired for profile 'streaming'"
    echo "🔑 Attempting SSO login with profile 'streaming'..."
    
    if aws sso login --profile streaming 2>&1; then
        echo "✅ SSO login successful"
        
        # Verify credentials are now valid
        if aws sts get-caller-identity --profile streaming >/dev/null 2>&1; then
            echo "✅ AWS credentials verified"
        else
            echo "❌ Error: AWS credentials still not valid after SSO login"
            echo "Please run 'aws sso login --profile streaming' manually"
            exit 1
        fi
    else
        echo "❌ Error: SSO login failed"
        echo "Please run 'aws sso login --profile streaming' manually"
        exit 1
    fi
else
    echo "✅ AWS credentials are valid (using profile 'streaming')"
fi
echo ""

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
  echo "🔍 Fetching API Gateway URL from CloudFormation..."
  SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
  
  # Use the existing get-api-gateway-url.sh script if available
  if [ -f "$SCRIPT_DIR/get-api-gateway-url.sh" ]; then
    # Use streaming profile (already set above)
    API_GATEWAY_URL=$(AWS_PROFILE=streaming "$SCRIPT_DIR/get-api-gateway-url.sh" staging us-east-1 2>/dev/null || echo "")
    
    if [ -n "$API_GATEWAY_URL" ] && [ "$API_GATEWAY_URL" != "None" ]; then
      # Check if it already includes /api/v1
      if [[ "$API_GATEWAY_URL" == *"/api/v1"* ]]; then
        API_GATEWAY_ROOT_URL="$API_GATEWAY_URL"
      else
        API_GATEWAY_ROOT_URL="$API_GATEWAY_URL/api/v1"
      fi
      echo "✅ Using API Gateway URL from script: $API_GATEWAY_ROOT_URL"
    else
      echo "❌ Error: Could not get API Gateway URL from script"
      echo "Please set API_GATEWAY_URL or API_TEST_BASE_URL in .env file"
      echo "Example: API_TEST_BASE_URL=https://abc123.execute-api.us-east-1.amazonaws.com/staging/api/v1"
      exit 1
    fi
  else
    # Fallback to direct CloudFormation query
    # Use streaming profile (already set above)
    API_GATEWAY_ROOT_URL=$(aws cloudformation describe-stacks \
      --profile streaming \
      --stack-name country-service-staging \
      --region us-east-1 \
      --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayRootUrl`].OutputValue' \
      --output text 2>/dev/null || echo "")

    if [ -z "$API_GATEWAY_ROOT_URL" ] || [ "$API_GATEWAY_ROOT_URL" == "None" ]; then
      API_GATEWAY_URL=$(aws cloudformation describe-stacks \
        --profile streaming \
        --stack-name country-service-staging \
        --region us-east-1 \
        --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayUrl`].OutputValue' \
        --output text 2>/dev/null || echo "")
      
      if [ -z "$API_GATEWAY_URL" ] || [ "$API_GATEWAY_URL" == "None" ]; then
        echo "❌ Error: Could not get API Gateway URL from CloudFormation"
        echo "Please set API_GATEWAY_URL or API_TEST_BASE_URL in .env file"
        echo "Example: API_TEST_BASE_URL=https://abc123.execute-api.us-east-1.amazonaws.com/staging/api/v1"
        exit 1
      fi
      # Add /api/v1 if not present
      if [[ "$API_GATEWAY_URL" != *"/api/v1"* ]]; then
        API_GATEWAY_ROOT_URL="$API_GATEWAY_URL/api/v1"
      else
        API_GATEWAY_ROOT_URL="$API_GATEWAY_URL"
      fi
    fi
    echo "✅ Using API Gateway URL from CloudFormation: $API_GATEWAY_ROOT_URL"
  fi
fi

# Get API key (try from .env first, then Lambda environment)
if [ -n "$API_KEY" ] || [ -n "$API_TEST_API_KEY" ]; then
  API_KEY="${API_TEST_API_KEY:-$API_KEY}"
  echo "✅ Using API key from .env"
else
  echo "🔍 Fetching API key from Lambda function..."
  # Use streaming profile (already set above)
  API_KEY=$(aws lambda get-function-configuration \
    --profile streaming \
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
  echo "❌ API connectivity test failed"
  echo "Full URL tested: $API_GATEWAY_ROOT_URL/countries?limit=1"
  exit 1
fi

# Run the performance tests
echo "⚡ Running performance tests against staging..."
echo "   This will measure response times for all endpoints"
echo "   All endpoints must respond in <200ms"
echo ""
export API_TEST_BASE_URL="$API_GATEWAY_ROOT_URL"
export API_TEST_API_KEY="$API_KEY"

if ./gradlew :country-service-api-tests:testPerformanceStaging --no-daemon; then
    echo ""
    echo "✅ Performance tests completed successfully!"
    echo ""
    echo "📊 Results:"
    echo "   Check the output above for response time measurements"
    echo "   All endpoints should have response times < 1000ms (staging threshold)"
    echo ""
    echo "ℹ️  Note: Staging environment uses a 1000ms threshold to account for:"
    echo "   - Network latency (20-100ms)"
    echo "   - Lambda cold start time (first request only, can be 500-2000ms)"
    echo "   - API Gateway processing time (10-50ms)"
    echo "   - Lambda execution overhead"
    echo ""
    echo "   Warm requests (after cold start) typically complete in 100-300ms"
    TEST_RESULT=0
else
    echo ""
    echo "❌ Performance tests failed!"
    echo ""
    echo "Troubleshooting:"
    echo "   - Check if any endpoints exceeded the 200ms requirement"
    echo "   - Verify the API Gateway URL is correct"
    echo "   - Check Lambda function logs in CloudWatch"
    TEST_RESULT=1
fi

exit $TEST_RESULT

