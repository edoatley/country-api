#!/bin/bash

# Deploy Lambda function using CloudFormation
# Usage: ./deploy-lambda.sh [environment] [region] [profile] [s3-bucket] [s3-key]
#   or: AWS_PROFILE=streaming ./deploy-lambda.sh staging us-east-1 streaming my-bucket lambda-package.jar

set -e

ENVIRONMENT="${1:-staging}"
REGION="${2:-us-east-1}"
AWS_PROFILE="${3:-${AWS_PROFILE}}"
S3_BUCKET="${4}"
S3_KEY="${5}"
STACK_NAME="country-service-lambda-${ENVIRONMENT}"
TEMPLATE_FILE="lambda-function.yaml"
EXECUTION_ROLES_STACK="country-service-lambda-execution-roles"

echo "Deploying Lambda function..."
echo "Environment: $ENVIRONMENT"
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

# Get Lambda execution role ARN from CloudFormation stack
echo "Getting Lambda execution role ARN from CloudFormation stack..."
EXECUTION_ROLE_ARN=""
if $AWS_CMD cloudformation describe-stacks --stack-name "$EXECUTION_ROLES_STACK" --region "$REGION" >/dev/null 2>&1; then
  if [ "$ENVIRONMENT" = "staging" ]; then
    EXECUTION_ROLE_ARN=$($AWS_CMD cloudformation describe-stacks \
      --stack-name "$EXECUTION_ROLES_STACK" \
      --region "$REGION" \
      --query 'Stacks[0].Outputs[?OutputKey==`LambdaExecutionRoleArnStaging`].OutputValue' \
      --output text 2>/dev/null || echo "")
  else
    EXECUTION_ROLE_ARN=$($AWS_CMD cloudformation describe-stacks \
      --stack-name "$EXECUTION_ROLES_STACK" \
      --region "$REGION" \
      --query 'Stacks[0].Outputs[?OutputKey==`LambdaExecutionRoleArnProduction`].OutputValue' \
      --output text 2>/dev/null || echo "")
  fi
  
  if [ -n "$EXECUTION_ROLE_ARN" ]; then
    echo "Found execution role: $EXECUTION_ROLE_ARN"
  else
    echo "❌ Error: Could not find execution role ARN from CloudFormation stack"
    exit 1
  fi
else
  echo "❌ Error: Execution roles stack not found: $EXECUTION_ROLES_STACK"
  echo "Please deploy the execution roles stack first: ./deploy-roles.sh"
  exit 1
fi

# Check if S3 bucket and key are provided
if [ -z "$S3_BUCKET" ] || [ -z "$S3_KEY" ]; then
  echo "❌ Error: S3 bucket and key are required"
  echo "Usage: ./deploy-lambda.sh [environment] [region] [profile] [s3-bucket] [s3-key]"
  exit 1
fi

# Get API key from environment variable or prompt
API_KEY="${API_KEY:-}"
if [ -z "$API_KEY" ]; then
  read -sp "Enter API key for $ENVIRONMENT: " API_KEY
  echo ""
fi

if [ -z "$API_KEY" ]; then
  echo "❌ Error: API key is required"
  exit 1
fi

# Deploy the stack
echo ""
echo "Deploying CloudFormation stack..."
$AWS_CMD cloudformation deploy \
  --template-file "$TEMPLATE_FILE" \
  --stack-name "$STACK_NAME" \
  --parameter-overrides \
    Environment="$ENVIRONMENT" \
    LambdaExecutionRoleArn="$EXECUTION_ROLE_ARN" \
    CodeS3Bucket="$S3_BUCKET" \
    CodeS3Key="$S3_KEY" \
    ApiKey="$API_KEY" \
    DynamoDBTableName=Countries \
    Timeout=30 \
    MemorySize=512 \
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

# Get the function ARN
echo ""
echo "Retrieving Lambda function ARN..."
FUNCTION_ARN=$($AWS_CMD cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query 'Stacks[0].Outputs[?OutputKey==`LambdaFunctionArn`].OutputValue' \
  --output text)

FUNCTION_NAME=$($AWS_CMD cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query 'Stacks[0].Outputs[?OutputKey==`LambdaFunctionName`].OutputValue' \
  --output text)

echo ""
echo "✅ Deployment complete!"
echo ""
echo "Lambda Function:"
echo "  Name: $FUNCTION_NAME"
echo "  ARN:  $FUNCTION_ARN"

