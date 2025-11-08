# Infrastructure as Code

This directory contains CloudFormation templates and deployment scripts for the Country Reference Service infrastructure.

## Lambda Execution Roles

### Template: `lambda-execution-roles.yaml`

CloudFormation template that creates IAM roles for Lambda function execution. These roles provide permissions for:
- DynamoDB table access (query, get, put, update, scan)
- CloudWatch Logs (create log groups, streams, and put log events)

### Deployment Script: `deploy-roles.sh`

Script to deploy the Lambda execution roles using CloudFormation.

**Usage:**
```bash
cd infrastructure
./deploy-roles.sh
```

**Prerequisites:**
- AWS CLI configured with appropriate permissions
- Permissions to create IAM roles and policies

**What it does:**
1. Deploys the CloudFormation stack `country-service-lambda-execution-roles`
2. Creates two IAM roles:
   - `country-service-lambda-execution-staging`
   - `country-service-lambda-execution-production`
3. Outputs the role ARNs for use in GitHub secrets (optional)

**Parameters:**
- `DynamoDBTableName`: Name of the DynamoDB table (default: `Countries`)
- `DynamoDBRegion`: AWS region where DynamoDB table is located (default: `us-east-1`)

**Outputs:**
- `LambdaExecutionRoleArnStaging`: ARN of the staging execution role
- `LambdaExecutionRoleArnProduction`: ARN of the production execution role

### Manual Deployment

If you prefer to deploy manually:

```bash
aws cloudformation deploy \
  --template-file lambda-execution-roles.yaml \
  --stack-name country-service-lambda-execution-roles \
  --parameter-overrides \
    DynamoDBTableName=Countries \
    DynamoDBRegion=us-east-1 \
  --capabilities CAPABILITY_NAMED_IAM \
  --region us-east-1
```

### Getting Role ARNs

After deployment, get the role ARNs:

```bash
# From CloudFormation outputs
aws cloudformation describe-stacks \
  --stack-name country-service-lambda-execution-roles \
  --query 'Stacks[0].Outputs' \
  --output table

# Or from IAM directly
aws iam get-role --role-name country-service-lambda-execution-staging --query 'Role.Arn' --output text
aws iam get-role --role-name country-service-lambda-execution-production --query 'Role.Arn' --output text
```

### Updating Roles

To update the roles (e.g., change permissions):

1. Modify `lambda-execution-roles.yaml`
2. Redeploy:
   ```bash
   ./deploy-roles.sh
   ```

### Deleting the Stack

To remove all roles:

```bash
aws cloudformation delete-stack \
  --stack-name country-service-lambda-execution-roles \
  --region us-east-1
```

**Note**: This will fail if any Lambda functions are still using these roles. You must first update or delete those Lambda functions.

## Integration with Deployment Workflow

The GitHub Actions deployment workflow will automatically:
1. Check for `LAMBDA_EXECUTION_ROLE_ARN_*` secrets
2. If not found, get role ARNs from CloudFormation stack outputs
3. If CloudFormation stack doesn't exist, try to get from IAM directly
4. Use the role ARN when creating Lambda functions

**No secrets required** if the CloudFormation stack is deployed - the workflow will discover the roles automatically.

## GitHub Actions Deployment Roles

### Template: `github-actions-roles.yaml`

CloudFormation template that creates IAM roles for GitHub Actions to deploy Lambda functions. These roles provide permissions for:
- Lambda function creation and updates
- Passing Lambda execution roles to Lambda (`iam:PassRole`)
- Reading CloudFormation stack outputs
- OIDC authentication with GitHub Actions

### Deployment Script: `deploy-github-actions-roles.sh`

Script to deploy the GitHub Actions deployment roles using CloudFormation.

**Usage:**
```bash
cd infrastructure
./deploy-github-actions-roles.sh [region] [profile]
```

Or with environment variable:
```bash
AWS_PROFILE=streaming ./deploy-github-actions-roles.sh
```

**Prerequisites:**
- AWS CLI configured with appropriate permissions
- Permissions to create IAM roles and OIDC providers
- Lambda execution roles stack must be deployed first (or provide role ARNs as parameters)

**What it does:**
1. Detects GitHub org/repo from git remote (or prompts for input)
2. Checks for existing OIDC provider
3. Gets Lambda execution role ARNs from CloudFormation stack (if available)
4. Deploys the CloudFormation stack `country-service-github-actions-roles`
5. Creates two IAM roles:
   - `GitHubActions-Deploy-Staging` (allows deployments from main branch and v* tags)
   - `GitHubActions-Deploy-Production` (allows deployments from workflow_dispatch only)
6. Outputs the role ARNs for use in GitHub secrets

**Parameters:**
- `GitHubOrg`: GitHub organization or username (auto-detected from git remote)
- `GitHubRepo`: GitHub repository name (auto-detected from git remote)
- `OIDCProviderArn`: ARN of existing OIDC provider (optional - will create if not provided)
- `LambdaExecutionRoleArnStaging`: ARN of Lambda execution role for staging (optional - auto-detected from stack)
- `LambdaExecutionRoleArnProduction`: ARN of Lambda execution role for production (optional - auto-detected from stack)

**Outputs:**
- `GitHubActionsRoleArnStaging`: ARN of the staging deployment role
- `GitHubActionsRoleArnProduction`: ARN of the production deployment role
- `OIDCProviderArn`: ARN of the OIDC provider (created or existing)

### Manual Deployment

If you prefer to deploy manually:

```bash
aws cloudformation deploy \
  --template-file github-actions-roles.yaml \
  --stack-name country-service-github-actions-roles \
  --parameter-overrides \
    GitHubOrg=YOUR_GITHUB_ORG \
    GitHubRepo=YOUR_REPO \
    OIDCProviderArn=arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com \
    LambdaExecutionRoleArnStaging=arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-staging \
    LambdaExecutionRoleArnProduction=arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-production \
  --capabilities CAPABILITY_NAMED_IAM \
  --region us-east-1
```

### Updating Roles

To update the roles (e.g., change permissions or trust policy):

1. **Modify the CloudFormation template** (`infrastructure/github-actions-roles.yaml`)

2. **Redeploy the stack**:
   ```bash
   ./deploy-github-actions-roles.sh
   ```

### Deleting the Stack

To remove all roles:

```bash
aws cloudformation delete-stack \
  --stack-name country-service-github-actions-roles \
  --region us-east-1
```

**Note**: This will fail if any GitHub Actions workflows are still using these roles. You must first update or remove those workflows.

## See Also

- [Lambda Execution Role Setup Guide](../docs/LAMBDA_EXECUTION_ROLE_SETUP.md)
- [AWS OIDC Setup Guide](../docs/AWS_OIDC_SETUP.md)

