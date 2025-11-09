#!/bin/bash

# Script to validate Swagger UI is working correctly
# This script checks that Swagger UI is accessible and displays the API correctly
# Works with both Docker and Podman

set -e

BASE_URL="http://localhost:8080"
SWAGGER_UI_URL="${BASE_URL}/swagger-ui.html"
API_DOCS_URL="${BASE_URL}/api-docs"
API_KEY="default-test-key"

# Detect container runtime (Docker or Podman)
if command -v podman &> /dev/null && podman ps &> /dev/null 2>&1; then
    CONTAINER_CMD="podman"
    # Try podman compose first, fall back to docker-compose if available
    if podman compose version &> /dev/null 2>&1; then
        COMPOSE_CMD="podman compose"
    elif command -v podman-compose &> /dev/null; then
        COMPOSE_CMD="podman-compose"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="podman compose"
    fi
elif command -v docker &> /dev/null && docker ps &> /dev/null 2>&1; then
    CONTAINER_CMD="docker"
    COMPOSE_CMD="docker-compose"
else
    echo "⚠️  Neither Docker nor Podman is available. Continuing with API checks only..."
    CONTAINER_CMD=""
    COMPOSE_CMD=""
fi

echo "🔍 Validating Swagger UI..."
if [ -n "${CONTAINER_CMD}" ]; then
    echo "📦 Using ${CONTAINER_CMD} for container checks"
fi
echo ""

# Check if application is running
echo "1. Checking if application is running..."
if ! curl -s -f "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
    echo "❌ Application is not running. Please start it with:"
    echo "   ./gradlew :country-service-bootstrap:bootRun"
    echo ""
    echo "   Note: Make sure LocalStack is running first:"
    if [ -n "${COMPOSE_CMD}" ]; then
        echo "   ${COMPOSE_CMD} up -d"
    else
        echo "   podman-compose up -d  (or docker-compose up -d)"
    fi
    exit 1
fi
echo "✅ Application is running"
echo ""

# Check Swagger UI
echo "2. Checking Swagger UI accessibility..."
if ! curl -s -f "${SWAGGER_UI_URL}" > /dev/null 2>&1; then
    echo "❌ Swagger UI is not accessible at ${SWAGGER_UI_URL}"
    exit 1
fi
echo "✅ Swagger UI is accessible at ${SWAGGER_UI_URL}"
echo ""

# Check API docs endpoint
echo "3. Checking OpenAPI JSON endpoint..."
API_DOCS_RESPONSE=$(curl -s -f "${API_DOCS_URL}" 2>&1)
if [ $? -ne 0 ]; then
    echo "❌ OpenAPI JSON endpoint is not accessible at ${API_DOCS_URL}"
    exit 1
fi
echo "✅ OpenAPI JSON endpoint is accessible"
echo ""

# Validate OpenAPI JSON structure
echo "4. Validating OpenAPI JSON structure..."
if echo "${API_DOCS_RESPONSE}" | jq -e '.info.title' > /dev/null 2>&1; then
    TITLE=$(echo "${API_DOCS_RESPONSE}" | jq -r '.info.title')
    echo "✅ OpenAPI JSON is valid (Title: ${TITLE})"
else
    echo "❌ OpenAPI JSON is invalid or malformed"
    exit 1
fi
echo ""

# Check for API key security scheme
echo "5. Checking API key security scheme..."
if echo "${API_DOCS_RESPONSE}" | jq -e '.components.securitySchemes.ApiKeyAuth' > /dev/null 2>&1; then
    echo "✅ API key security scheme is configured"
else
    echo "⚠️  API key security scheme not found in OpenAPI JSON"
fi
echo ""

# Check for country endpoints
echo "6. Checking for country endpoints..."
ENDPOINTS=$(echo "${API_DOCS_RESPONSE}" | jq -r '.paths | keys[]' | grep -c "/countries" || echo "0")
if [ "${ENDPOINTS}" -gt 0 ]; then
    echo "✅ Found ${ENDPOINTS} country endpoint(s)"
    echo "${API_DOCS_RESPONSE}" | jq -r '.paths | keys[]' | grep "/countries" | head -5
else
    echo "❌ No country endpoints found in OpenAPI JSON"
    exit 1
fi
echo ""

# Test API endpoint with API key
echo "7. Testing API endpoint with API key..."
API_RESPONSE=$(curl -s -w "\n%{http_code}" -H "X-API-KEY: ${API_KEY}" "${BASE_URL}/api/v1/countries?limit=1" 2>&1)
HTTP_CODE=$(echo "${API_RESPONSE}" | tail -n 1)
if [ "${HTTP_CODE}" = "200" ] || [ "${HTTP_CODE}" = "401" ]; then
    echo "✅ API endpoint is responding (HTTP ${HTTP_CODE})"
else
    echo "⚠️  API endpoint returned unexpected status: ${HTTP_CODE}"
fi
echo ""

echo "✅ Swagger UI validation complete!"
echo ""
echo "📝 Next steps:"
echo "   1. Open ${SWAGGER_UI_URL} in your browser"
echo "   2. Click 'Authorize' and enter API key: ${API_KEY}"
echo "   3. Try the 'Try it out' feature on any endpoint"
echo "   4. Verify that requests work correctly"
echo ""

