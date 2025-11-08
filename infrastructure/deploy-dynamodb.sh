#!/bin/bash

# Deploy DynamoDB table using CloudFormation
# Usage: ./deploy-dynamodb.sh [region] [profile]
#   or: AWS_PROFILE=streaming ./deploy-dynamodb.sh

set -e

REGION="${1:-us-east-1}"
AWS_PROFILE="${2:-${AWS_PROFILE}}"
STACK_NAME="country-service-dynamodb"
TEMPLATE_FILE="dynamodb-table.yaml"

echo "Deploying DynamoDB table..."
echo "Region: $REGION"
if [ -n "$AWS_PROFILE" ]; then
  echo "AWS Profile: $AWS_PROFILE"
  export AWS_PROFILE="$AWS_PROFILE"
fi
echo "Stack Name: $STACK_NAME"
echo ""

# Check if template file exists
if [ ! -f "$TEMPLATE_FILE" ]; then
  echo "Error: Template file $TEMPLATE_FILE not found"
  echo "Please run this script from the infrastructure directory"
  exit 1
fi

# Setup AWS command
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

# Deploy the stack
echo "Deploying CloudFormation stack..."
$AWS_CMD cloudformation deploy \
  --template-file "$TEMPLATE_FILE" \
  --stack-name "$STACK_NAME" \
  --parameter-overrides \
    TableName=Countries \
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

# Get the table ARN
echo ""
echo "Retrieving table information..."
TABLE_NAME=$($AWS_CMD cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query 'Stacks[0].Outputs[?OutputKey==`TableName`].OutputValue' \
  --output text)

TABLE_ARN=$($AWS_CMD cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query 'Stacks[0].Outputs[?OutputKey==`TableArn`].OutputValue' \
  --output text)

echo ""
echo "✅ Deployment complete!"
echo ""
echo "DynamoDB Table:"
echo "  Name: $TABLE_NAME"
echo "  ARN:  $TABLE_ARN"

