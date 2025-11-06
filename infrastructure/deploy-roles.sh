#!/bin/bash

# Deploy Lambda execution roles using CloudFormation
# Usage: ./deploy-roles.sh [staging|production|all] [region] [profile]
#   or: AWS_PROFILE=streaming ./deploy-roles.sh

set -e

ENVIRONMENT="${1:-all}"
REGION="${2:-us-east-1}"
AWS_PROFILE="${3:-${AWS_PROFILE}}"
STACK_NAME="country-service-lambda-execution-roles"
TEMPLATE_FILE="lambda-execution-roles.yaml"
DYNAMODB_TABLE_NAME="${DYNAMODB_TABLE_NAME:-Countries}"

echo "Deploying Lambda execution roles..."
echo "Environment: $ENVIRONMENT"
echo "Region: $REGION"
if [ -n "$AWS_PROFILE" ]; then
  echo "AWS Profile: $AWS_PROFILE"
  export AWS_PROFILE="$AWS_PROFILE"
fi
echo "Stack Name: $STACK_NAME"
echo "DynamoDB Table: $DYNAMODB_TABLE_NAME"
echo ""

# Check if template file exists
if [ ! -f "$TEMPLATE_FILE" ]; then
  echo "Error: Template file $TEMPLATE_FILE not found"
  echo "Please run this script from the infrastructure directory"
  exit 1
fi

# Deploy the stack
echo "Deploying CloudFormation stack..."
AWS_CMD="aws"
if [ -n "$AWS_PROFILE" ]; then
  AWS_CMD="aws --profile $AWS_PROFILE"
fi

# Check if stack exists and is in a failed state
STACK_STATUS=$($AWS_CMD cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" --query 'Stacks[0].StackStatus' --output text 2>/dev/null || echo "NOT_FOUND")

if [ "$STACK_STATUS" = "ROLLBACK_COMPLETE" ] || [ "$STACK_STATUS" = "ROLLBACK_FAILED" ]; then
  echo "⚠️  Stack is in $STACK_STATUS state. Deleting it first..."
  $AWS_CMD cloudformation delete-stack --stack-name "$STACK_NAME" --region "$REGION"
  echo "Waiting for stack deletion to complete..."
  $AWS_CMD cloudformation wait stack-delete-complete --stack-name "$STACK_NAME" --region "$REGION" || true
  echo "Stack deleted. Proceeding with deployment..."
fi

$AWS_CMD cloudformation deploy \
  --template-file "$TEMPLATE_FILE" \
  --stack-name "$STACK_NAME" \
  --parameter-overrides \
    DynamoDBTableName="$DYNAMODB_TABLE_NAME" \
    DynamoDBRegion="$REGION" \
  --capabilities CAPABILITY_NAMED_IAM \
  --region "$REGION" || {
    echo ""
    echo "❌ Deployment failed. Checking stack events..."
    $AWS_CMD cloudformation describe-stack-events \
      --stack-name "$STACK_NAME" \
      --region "$REGION" \
      --max-items 5 \
      --query 'StackEvents[?ResourceStatus==`CREATE_FAILED` || ResourceStatus==`UPDATE_FAILED`].[Timestamp,LogicalResourceId,ResourceStatusReason]' \
      --output table 2>/dev/null || true
    exit 1
  }

# Get the role ARNs
echo ""
echo "Retrieving role ARNs..."
STAGING_ROLE_ARN=$($AWS_CMD cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query 'Stacks[0].Outputs[?OutputKey==`LambdaExecutionRoleArnStaging`].OutputValue' \
  --output text)

PRODUCTION_ROLE_ARN=$($AWS_CMD cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query 'Stacks[0].Outputs[?OutputKey==`LambdaExecutionRoleArnProduction`].OutputValue' \
  --output text)

echo ""
echo "✅ Deployment complete!"
echo ""
echo "Lambda Execution Role ARNs:"
echo "  Staging:    $STAGING_ROLE_ARN"
echo "  Production: $PRODUCTION_ROLE_ARN"
echo ""
echo "Add these to your GitHub secrets:"
echo "  LAMBDA_EXECUTION_ROLE_ARN_STAGING=$STAGING_ROLE_ARN"
echo "  LAMBDA_EXECUTION_ROLE_ARN_PRODUCTION=$PRODUCTION_ROLE_ARN"

