#!/bin/bash

# Get Lambda execution role ARN from CloudFormation stack or secret
# Usage: ./get-lambda-role-arn.sh <environment> <region> [role-arn-secret]
#   environment: staging or production
#   region: AWS region (e.g., us-east-1)
#   role-arn-secret: Optional secret name for role ARN (if not provided, will query CloudFormation)

set -e

ENVIRONMENT="${1}"
REGION="${2}"
ROLE_ARN_SECRET="${3}"
EXECUTION_ROLES_STACK="country-service-lambda-execution-roles"

if [ -z "$ENVIRONMENT" ] || [ -z "$REGION" ]; then
  echo "Usage: $0 <environment> <region> [role-arn-secret]"
  echo "  environment: staging or production"
  echo "  region: AWS region (e.g., us-east-1)"
  echo "  role-arn-secret: Optional secret name for role ARN"
  exit 1
fi

# Determine output key based on environment
if [ "$ENVIRONMENT" = "staging" ]; then
  OUTPUT_KEY="LambdaExecutionRoleArnStaging"
else
  OUTPUT_KEY="LambdaExecutionRoleArnProduction"
fi

# Check if role ARN secret is provided and not empty
# In GitHub Actions, the secret value is passed directly, not the secret name
if [ -n "$ROLE_ARN_SECRET" ] && [ "$ROLE_ARN_SECRET" != "" ] && [ "$ROLE_ARN_SECRET" != "null" ]; then
  ROLE_ARN="$ROLE_ARN_SECRET"
  echo "Using role ARN from secret: $ROLE_ARN" >&2
  echo "$ROLE_ARN"
  exit 0
fi

# Get from CloudFormation stack
echo "Checking for CloudFormation stack: $EXECUTION_ROLES_STACK" >&2
if ! aws cloudformation describe-stacks --stack-name "$EXECUTION_ROLES_STACK" --region "$REGION" >/dev/null 2>&1; then
  echo "❌ Error: CloudFormation stack '$EXECUTION_ROLES_STACK' not found" >&2
  echo "Please deploy the execution roles stack first:" >&2
  echo "  cd infrastructure" >&2
  echo "  ./deploy-roles.sh" >&2
  exit 1
fi

ROLE_ARN=$(aws cloudformation describe-stacks \
  --stack-name "$EXECUTION_ROLES_STACK" \
  --region "$REGION" \
  --query "Stacks[0].Outputs[?OutputKey==\`${OUTPUT_KEY}\`].OutputValue" \
  --output text 2>/dev/null || echo "")

if [ -z "$ROLE_ARN" ] || [ "$ROLE_ARN" = "None" ]; then
  echo "❌ Error: Lambda execution role ARN not found in stack outputs" >&2
  echo "Stack outputs:" >&2
  aws cloudformation describe-stacks \
    --stack-name "$EXECUTION_ROLES_STACK" \
    --region "$REGION" \
    --query 'Stacks[0].Outputs' \
    --output table || true
  exit 1
fi

echo "Got role ARN from CloudFormation: $ROLE_ARN" >&2
echo "$ROLE_ARN"

