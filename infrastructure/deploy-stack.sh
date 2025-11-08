#!/bin/bash

# Deploy complete stack (Lambda + API Gateway) using CloudFormation
# Usage: ./deploy-stack.sh [environment] [region] [profile] [s3-bucket] [s3-key]
#   or: AWS_PROFILE=streaming ./deploy-stack.sh staging us-east-1 streaming my-bucket lambda-package.jar

set -e

ENVIRONMENT="${1:-staging}"
REGION="${2:-us-east-1}"
AWS_PROFILE="${3:-${AWS_PROFILE}}"
S3_BUCKET="${4}"
S3_KEY="${5}"
STACK_NAME="country-service-${ENVIRONMENT}"
TEMPLATE_FILE="lambda-api-gateway.yaml"
EXECUTION_ROLES_STACK="country-service-lambda-execution-roles"
DYNAMODB_STACK="country-service-dynamodb"

echo "Deploying Country Reference Service stack..."
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

# Check if DynamoDB table exists
echo ""
echo "Checking for DynamoDB table..."
TABLE_NAME="Countries"
if ! $AWS_CMD dynamodb describe-table --table-name "$TABLE_NAME" --region "$REGION" >/dev/null 2>&1; then
  echo "⚠️  DynamoDB table not found. Checking for CloudFormation stack..."
  if $AWS_CMD cloudformation describe-stacks --stack-name "$DYNAMODB_STACK" --region "$REGION" >/dev/null 2>&1; then
    echo "Found DynamoDB stack, table should exist"
  else
    echo "⚠️  DynamoDB table and stack not found. Please deploy the table first:"
    echo "  cd infrastructure"
    echo "  ./deploy-dynamodb.sh"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      exit 1
    fi
  fi
else
  echo "✅ DynamoDB table found: $TABLE_NAME"
fi

# Check if S3 bucket and key are provided
if [ -z "$S3_BUCKET" ] || [ -z "$S3_KEY" ]; then
  echo "❌ Error: S3 bucket and key are required"
  echo "Usage: ./deploy-stack.sh [environment] [region] [profile] [s3-bucket] [s3-key]"
  exit 1
fi

# Verify S3 object exists
echo ""
echo "Verifying S3 object exists..."
if ! $AWS_CMD s3 ls "s3://$S3_BUCKET/$S3_KEY" --region "$REGION" >/dev/null 2>&1; then
  echo "❌ Error: S3 object not found: s3://$S3_BUCKET/$S3_KEY"
  echo "Please upload the Lambda package to S3 first"
  exit 1
fi
echo "✅ S3 object found: s3://$S3_BUCKET/$S3_KEY"

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
    DynamoDBTableName="$TABLE_NAME" \
    Timeout=30 \
    MemorySize=512 \
  --capabilities CAPABILITY_NAMED_IAM \
  --region "$REGION" || {
    echo ""
    echo "❌ Deployment failed. Checking stack events..."
    echo ""
    echo "=== Failed Resources ==="
    FAILED_EVENTS=$($AWS_CMD cloudformation describe-stack-events \
      --stack-name "$STACK_NAME" \
      --region "$REGION" \
      --max-items 50 \
      --query 'StackEvents[?ResourceStatus==`CREATE_FAILED` || ResourceStatus==`UPDATE_FAILED` || ResourceStatus==`DELETE_FAILED` || ResourceStatus==`ROLLBACK_IN_PROGRESS`] | reverse(sort_by(@, &Timestamp)) | [0:5]' \
      --output json 2>/dev/null || echo "[]")
    
    if [ "$FAILED_EVENTS" != "[]" ] && [ -n "$FAILED_EVENTS" ]; then
      echo "$FAILED_EVENTS" | python3 -m json.tool 2>/dev/null || echo "$FAILED_EVENTS"
    else
      echo "No failed events found. Checking recent events..."
    fi
    
    echo ""
    echo "=== Recent Stack Events (last 15) ==="
    $AWS_CMD cloudformation describe-stack-events \
      --stack-name "$STACK_NAME" \
      --region "$REGION" \
      --max-items 15 \
      --query 'reverse(sort_by(StackEvents, &Timestamp)) | [].[Timestamp,LogicalResourceId,ResourceStatus,ResourceStatusReason]' \
      --output table 2>/dev/null || true
    
    echo ""
    echo "For more details, run:"
    echo "  aws cloudformation describe-stack-events --stack-name $STACK_NAME --region $REGION"
    exit 1
  }

# Get the outputs
echo ""
echo "Retrieving stack outputs..."
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

API_GATEWAY_URL=$($AWS_CMD cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayUrl`].OutputValue' \
  --output text)

API_GATEWAY_ROOT_URL=$($AWS_CMD cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayRootUrl`].OutputValue' \
  --output text)

echo ""
echo "✅ Deployment complete!"
echo ""
echo "Lambda Function:"
echo "  Name: $FUNCTION_NAME"
echo "  ARN:  $FUNCTION_ARN"
echo ""
echo "API Gateway:"
echo "  URL: $API_GATEWAY_URL"
echo "  Root URL: $API_GATEWAY_ROOT_URL"
echo ""
echo "Test the API:"
echo "  curl -X GET \"$API_GATEWAY_ROOT_URL/countries\" -H \"X-API-KEY: $API_KEY\""

