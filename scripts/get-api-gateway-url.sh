#!/bin/bash

# Get API Gateway URL from CloudFormation stack
# Usage: ./get-api-gateway-url.sh <environment> <region>
#   environment: staging or production
#   region: AWS region (e.g., us-east-1)

set -e

ENVIRONMENT="${1}"
REGION="${2}"
STACK_NAME="country-service-${ENVIRONMENT}"

if [ -z "$ENVIRONMENT" ] || [ -z "$REGION" ]; then
  echo "Usage: $0 <environment> <region>"
  echo "  environment: staging or production"
  echo "  region: AWS region (e.g., us-east-1)"
  exit 1
fi

echo "Checking for CloudFormation stack: $STACK_NAME" >&2
if ! aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" >/dev/null 2>&1; then
  echo "❌ Error: CloudFormation stack '$STACK_NAME' not found" >&2
  echo "The deployment step should have created this stack. Check deployment logs above." >&2
  exit 1
fi

# For staging, prefer ApiGatewayRootUrl (includes /api/v1), fallback to ApiGatewayUrl
# For production, use ApiGatewayUrl (or ApiGatewayRootUrl if available)
if [ "$ENVIRONMENT" = "staging" ]; then
  # Try ApiGatewayRootUrl first
  API_GATEWAY_ROOT_URL=$(aws cloudformation describe-stacks \
    --stack-name "$STACK_NAME" \
    --region "$REGION" \
    --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayRootUrl`].OutputValue' \
    --output text 2>/dev/null || echo "")
  
  if [ -n "$API_GATEWAY_ROOT_URL" ] && [ "$API_GATEWAY_ROOT_URL" != "None" ]; then
    echo "Using ApiGatewayRootUrl: $API_GATEWAY_ROOT_URL" >&2
    echo "$API_GATEWAY_ROOT_URL"
    exit 0
  fi
fi

# Fallback to ApiGatewayUrl
API_GATEWAY_URL=$(aws cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayUrl`].OutputValue' \
  --output text 2>/dev/null || echo "")

if [ -z "$API_GATEWAY_URL" ] || [ "$API_GATEWAY_URL" = "None" ]; then
  echo "❌ Error: API Gateway URL not found in stack outputs" >&2
  echo "Stack outputs:" >&2
  aws cloudformation describe-stacks \
    --stack-name "$STACK_NAME" \
    --region "$REGION" \
    --query 'Stacks[0].Outputs' \
    --output table || true
  exit 1
fi

echo "Using ApiGatewayUrl: $API_GATEWAY_URL" >&2
echo "$API_GATEWAY_URL"

