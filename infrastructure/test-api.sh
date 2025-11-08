#!/bin/bash

# Test the Country Reference Service API
# Usage: ./test-api.sh [api-gateway-url] [api-key]
#   or: API_KEY=your-key ./test-api.sh https://abc123.execute-api.us-east-1.amazonaws.com/staging
#   or: API_GATEWAY_URL=url API_KEY=key ./test-api.sh (for GitHub Actions)

set -e

API_GATEWAY_URL="${1:-${API_GATEWAY_URL}}"
API_KEY="${2:-${API_KEY}}"

if [ -z "$API_GATEWAY_URL" ]; then
  echo "Usage: ./test-api.sh [api-gateway-url] [api-key]"
  echo "   or: API_KEY=your-key ./test-api.sh https://abc123.execute-api.us-east-1.amazonaws.com/staging"
  echo "   or: API_GATEWAY_URL=url API_KEY=key ./test-api.sh (for CI/CD)"
  exit 1
fi

if [ -z "$API_KEY" ]; then
  # Only prompt interactively if we're in a TTY (not in CI/CD)
  if [ -t 0 ]; then
    read -sp "Enter API key: " API_KEY
    echo ""
  else
    echo "❌ Error: API key is required"
    echo "Set API_KEY environment variable or pass as second argument"
    exit 1
  fi
fi

echo "Testing Country Reference Service API"
echo "API Gateway URL: $API_GATEWAY_URL"
echo ""

# Test 1: GET /api/v1/countries
echo "Test 1: GET /api/v1/countries"
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_GATEWAY_URL/api/v1/countries" \
  -H "X-API-KEY: $API_KEY" \
  -H "Content-Type: application/json" || echo -e "\n000")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

# Validate response is valid JSON array (if jq is available)
if [ "$HTTP_CODE" == "200" ] && command -v jq >/dev/null 2>&1; then
  if ! echo "$BODY" | jq -e '. | type == "array"' > /dev/null 2>&1; then
    echo "❌ Smoke test failed: Response is not a valid JSON array"
    echo "Response: $BODY"
    exit 1
  fi
fi

echo "HTTP Status: $HTTP_CODE"
if [ "$HTTP_CODE" == "200" ]; then
  echo "✅ Success!"
  # Try to use jq if available, otherwise just print the body
  if command -v jq >/dev/null 2>&1; then
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
  else
    echo "$BODY"
  fi
else
  echo "❌ Failed"
  echo "$BODY"
  exit 1
fi
echo ""

# Test 2: GET /api/v1/countries/code/GB (if we have data)
if [ "$HTTP_CODE" == "200" ]; then
  echo "Test 2: GET /api/v1/countries/code/GB"
  RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_GATEWAY_URL/api/v1/countries/code/GB" \
    -H "X-API-KEY: $API_KEY" \
    -H "Content-Type: application/json" || echo -e "\n000")
  
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  BODY=$(echo "$RESPONSE" | sed '$d')
  
  echo "HTTP Status: $HTTP_CODE"
  if [ "$HTTP_CODE" == "200" ]; then
    echo "✅ Success!"
    # Try to use jq if available, otherwise just print the body
    if command -v jq >/dev/null 2>&1; then
      echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    else
      echo "$BODY"
    fi
  else
    echo "⚠️  Note: This test may fail if GB is not in the database"
    echo "$BODY"
  fi
  echo ""
fi

# Test 3: Authentication check
echo "Test 3: Authentication validation (no API key)"
AUTH_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_GATEWAY_URL/api/v1/countries" \
  -H "Content-Type: application/json" || echo -e "\n000")

AUTH_HTTP_CODE=$(echo "$AUTH_RESPONSE" | tail -n1)

if [ "$AUTH_HTTP_CODE" == "401" ]; then
  echo "✅ Authentication is working (401 without API key)"
else
  echo "⚠️  Warning: Expected HTTP 401, got $AUTH_HTTP_CODE"
fi

