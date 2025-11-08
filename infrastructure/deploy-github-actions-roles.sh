#!/bin/bash

# Deploy GitHub Actions deployment roles using CloudFormation
# Usage: ./deploy-github-actions-roles.sh [region] [profile]
#   or: AWS_PROFILE=streaming ./deploy-github-actions-roles.sh

set -e

REGION="${1:-us-east-1}"
AWS_PROFILE="${2:-${AWS_PROFILE}}"
STACK_NAME="country-service-github-actions-roles"
TEMPLATE_FILE="github-actions-roles.yaml"
EXECUTION_ROLES_STACK="country-service-lambda-execution-roles"

echo "Deploying GitHub Actions deployment roles..."
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

# Get GitHub org and repo from git remote
echo "Detecting GitHub organization and repository..."
GIT_REMOTE=$(git remote get-url origin 2>/dev/null || echo "")
if [[ "$GIT_REMOTE" =~ github.com[:/]([^/]+)/([^/]+)\.git ]]; then
  GITHUB_ORG="${BASH_REMATCH[1]}"
  GITHUB_REPO="${BASH_REMATCH[2]%.git}"
  echo "Detected: GitHub Org: $GITHUB_ORG, Repo: $GITHUB_REPO"
else
  echo "⚠️  Could not detect GitHub org/repo from git remote"
  read -p "Enter GitHub organization or username: " GITHUB_ORG
  read -p "Enter GitHub repository name: " GITHUB_REPO
fi

# Get AWS Account ID
echo ""
echo "Getting AWS Account ID..."
ACCOUNT_ID=$($AWS_CMD sts get-caller-identity --query Account --output text)
echo "AWS Account ID: $ACCOUNT_ID"

# Check if OIDC provider exists
echo ""
echo "Checking for existing OIDC provider..."
OIDC_PROVIDER_ARN=""
OIDC_PROVIDERS=$($AWS_CMD iam list-open-id-connect-providers --query 'OpenIDConnectProviderList[*].Arn' --output text 2>/dev/null || echo "")
if [ -n "$OIDC_PROVIDERS" ]; then
  for PROVIDER in $OIDC_PROVIDERS; do
    if [[ "$PROVIDER" == *"token.actions.githubusercontent.com"* ]]; then
      OIDC_PROVIDER_ARN="$PROVIDER"
      echo "Found existing OIDC provider: $OIDC_PROVIDER_ARN"
      break
    fi
  done
fi

if [ -z "$OIDC_PROVIDER_ARN" ]; then
  echo "⚠️  No existing OIDC provider found."
  echo "Creating OIDC provider..."
  OIDC_PROVIDER_ARN=$($AWS_CMD iam create-open-id-connect-provider \
    --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com \
    --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 \
    --query 'OpenIDConnectProviderArn' \
    --output text 2>/dev/null || echo "")
  
  if [ -n "$OIDC_PROVIDER_ARN" ]; then
    echo "✅ Created OIDC provider: $OIDC_PROVIDER_ARN"
  else
    echo "❌ Failed to create OIDC provider. Please create it manually or provide the ARN."
    echo "To create manually:"
    echo "  aws iam create-open-id-connect-provider \\"
    echo "    --url https://token.actions.githubusercontent.com \\"
    echo "    --client-id-list sts.amazonaws.com \\"
    echo "    --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1"
    exit 1
  fi
fi

# Try to get Lambda execution role ARNs from CloudFormation stack
echo ""
echo "Getting Lambda execution role ARNs from CloudFormation stack..."
LAMBDA_STAGING_ROLE_ARN=""
LAMBDA_PRODUCTION_ROLE_ARN=""

if $AWS_CMD cloudformation describe-stacks --stack-name "$EXECUTION_ROLES_STACK" --region "$REGION" >/dev/null 2>&1; then
  echo "Found execution roles stack: $EXECUTION_ROLES_STACK"
  LAMBDA_STAGING_ROLE_ARN=$($AWS_CMD cloudformation describe-stacks \
    --stack-name "$EXECUTION_ROLES_STACK" \
    --region "$REGION" \
    --query 'Stacks[0].Outputs[?OutputKey==`LambdaExecutionRoleArnStaging`].OutputValue' \
    --output text 2>/dev/null || echo "")
  
  LAMBDA_PRODUCTION_ROLE_ARN=$($AWS_CMD cloudformation describe-stacks \
    --stack-name "$EXECUTION_ROLES_STACK" \
    --region "$REGION" \
    --query 'Stacks[0].Outputs[?OutputKey==`LambdaExecutionRoleArnProduction`].OutputValue' \
    --output text 2>/dev/null || echo "")
  
  if [ -n "$LAMBDA_STAGING_ROLE_ARN" ]; then
    echo "Found staging execution role: $LAMBDA_STAGING_ROLE_ARN"
  fi
  if [ -n "$LAMBDA_PRODUCTION_ROLE_ARN" ]; then
    echo "Found production execution role: $LAMBDA_PRODUCTION_ROLE_ARN"
  fi
else
  echo "⚠️  Execution roles stack not found. Using default role names."
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

# Build parameter overrides
PARAM_OVERRIDES="GitHubOrg=$GITHUB_ORG GitHubRepo=$GITHUB_REPO"

if [ -n "$OIDC_PROVIDER_ARN" ]; then
  PARAM_OVERRIDES="$PARAM_OVERRIDES OIDCProviderArn=$OIDC_PROVIDER_ARN"
fi

if [ -n "$LAMBDA_STAGING_ROLE_ARN" ]; then
  PARAM_OVERRIDES="$PARAM_OVERRIDES LambdaExecutionRoleArnStaging=$LAMBDA_STAGING_ROLE_ARN"
fi

if [ -n "$LAMBDA_PRODUCTION_ROLE_ARN" ]; then
  PARAM_OVERRIDES="$PARAM_OVERRIDES LambdaExecutionRoleArnProduction=$LAMBDA_PRODUCTION_ROLE_ARN"
fi

# Deploy the stack
echo ""
echo "Deploying CloudFormation stack..."
$AWS_CMD cloudformation deploy \
  --template-file "$TEMPLATE_FILE" \
  --stack-name "$STACK_NAME" \
  --parameter-overrides $PARAM_OVERRIDES \
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
  --query 'Stacks[0].Outputs[?OutputKey==`GitHubActionsRoleArnStaging`].OutputValue' \
  --output text)

PRODUCTION_ROLE_ARN=$($AWS_CMD cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query 'Stacks[0].Outputs[?OutputKey==`GitHubActionsRoleArnProduction`].OutputValue' \
  --output text)

echo ""
echo "✅ Deployment complete!"
echo ""
echo "GitHub Actions Deployment Role ARNs:"
echo "  Staging:    $STAGING_ROLE_ARN"
echo "  Production: $PRODUCTION_ROLE_ARN"
echo ""
echo "Add these to your GitHub secrets:"
echo "  AWS_ROLE_ARN_STAGING=$STAGING_ROLE_ARN"
echo "  AWS_ROLE_ARN_PRODUCTION=$PRODUCTION_ROLE_ARN"

