# Lambda Execution Role Setup

This guide explains how to create the IAM execution roles that your Lambda function uses to access AWS services like DynamoDB and CloudWatch Logs.

## Overview

The Lambda execution role is **different** from the GitHub Actions deployment role:
- **GitHub Actions Role**: Used by GitHub Actions to deploy/update the Lambda function
- **Lambda Execution Role**: Used by the Lambda function itself to access AWS services at runtime

## Recommended Approach: CloudFormation

The recommended approach is to deploy the Lambda execution roles using CloudFormation. This provides:
- **Infrastructure as Code**: Roles are defined in version-controlled templates
- **Idempotent Deployments**: Safe to run multiple times
- **Easy Updates**: Modify the template and redeploy
- **Better Security**: No need for `iam:PassRole` permission in GitHub Actions role

## Step 1: Deploy CloudFormation Stack

### Prerequisites

- AWS CLI configured with appropriate permissions
- Permissions to create IAM roles and policies

### Deploy the Stack

1. **Navigate to the infrastructure directory**:
   ```bash
   cd infrastructure
   ```

2. **Deploy the CloudFormation stack**:
   ```bash
   ./deploy-roles.sh
   ```

   Or manually:
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

3. **Get the role ARNs**:
   ```bash
   aws cloudformation describe-stacks \
     --stack-name country-service-lambda-execution-roles \
     --query 'Stacks[0].Outputs' \
     --output table
   ```

### Alternative: Manual Deployment

If you prefer to create the roles manually or need custom configurations:

## Step 1: Create IAM Role for Lambda Execution

### Staging Role

1. **Open IAM Console** → Roles → Create role

2. **Select trusted entity**: AWS service → Lambda

3. **Attach Permissions Policy**:

Create a custom policy with the following permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:Query",
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:Scan"
      ],
      "Resource": [
        "arn:aws:dynamodb:REGION:ACCOUNT_ID:table/Countries",
        "arn:aws:dynamodb:REGION:ACCOUNT_ID:table/Countries/index/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

**Or use AWS managed policies** (simpler, but less restrictive):
- `AmazonDynamoDBFullAccess` (or create a custom policy for your specific table)
- `CloudWatchLogsFullAccess` (or create a custom policy for logs)

4. **Name the role**: `country-service-lambda-execution-staging` (or your preferred name)

5. **Create role**

### Production Role

Create a similar role for production:
- Role name: `country-service-lambda-execution-production`
- Same permissions policy (but you may want separate DynamoDB tables for production)

## Step 2: Configure GitHub Secrets (Optional)

After deploying the CloudFormation stack, you can optionally add the role ARNs as GitHub secrets. The workflow will automatically discover the roles from CloudFormation if secrets are not provided.

**Option 1: Use CloudFormation outputs (Recommended)**
- No secrets needed - the workflow will automatically get the role ARNs from the CloudFormation stack

**Option 2: Use GitHub secrets (For explicit control)**
- Add these secrets to your GitHub repository:

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `LAMBDA_EXECUTION_ROLE_ARN_STAGING` | `arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-staging` | IAM role ARN for Lambda execution (staging) |
| `LAMBDA_EXECUTION_ROLE_ARN_PRODUCTION` | `arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-production` | IAM role ARN for Lambda execution (production) |

## Step 3: Verify

The deployment workflow will:
1. Check for `LAMBDA_EXECUTION_ROLE_ARN_*` secrets first
2. If not found, get role ARNs from CloudFormation stack outputs
3. If CloudFormation stack doesn't exist, try to get from IAM directly
4. Use the role ARN when creating Lambda functions

## Updating Roles

To update the roles (e.g., change permissions):

1. **Modify the CloudFormation template** (`infrastructure/lambda-execution-roles.yaml`)

2. **Redeploy the stack**:
   ```bash
   cd infrastructure
   ./deploy-roles.sh
   ```

   Or manually:
   ```bash
   aws cloudformation deploy \
     --template-file lambda-execution-roles.yaml \
     --stack-name country-service-lambda-execution-roles \
     --capabilities CAPABILITY_NAMED_IAM \
     --region us-east-1
   ```

3. **No changes needed to Lambda functions** - they will automatically use the updated roles

## Troubleshooting

### Error: "The role defined for the function cannot be assumed by Lambda"

**Possible causes:**
1. Role ARN is incorrect
2. Role trust policy doesn't allow Lambda service to assume it

**Solution:**
- Verify the role ARN is correct
- Ensure the role's trust policy allows `lambda.amazonaws.com` to assume it:
  ```json
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "Service": "lambda.amazonaws.com"
        },
        "Action": "sts:AssumeRole"
      }
    ]
  }
  ```

### Error: "User is not authorized to perform: lambda:CreateFunction"

**Possible causes:**
- GitHub Actions deployment role doesn't have `lambda:CreateFunction` permission

**Solution:**
- Add `lambda:CreateFunction` to the GitHub Actions deployment role permissions (see [AWS OIDC Setup Guide](AWS_OIDC_SETUP.md))

## Additional Resources

- [AWS Lambda Execution Role Documentation](https://docs.aws.amazon.com/lambda/latest/dg/lambda-intro-execution-role.html)
- [IAM Roles for Lambda](https://docs.aws.amazon.com/lambda/latest/dg/lambda-intro-execution-role.html)

