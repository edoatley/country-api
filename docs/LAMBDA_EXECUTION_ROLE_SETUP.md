# Lambda Execution Role Setup

This guide explains how the IAM execution role that your Lambda function uses to access AWS services like DynamoDB and CloudWatch Logs is created and managed.

## Overview

The Lambda execution role is **different** from the GitHub Actions deployment role:
- **GitHub Actions Role**: Used by GitHub Actions to deploy/update the Lambda function
- **Lambda Execution Role**: Used by the Lambda function itself to access AWS services at runtime

## Automatic Creation (Recommended)

The deployment workflow automatically creates the Lambda execution roles if they don't exist. **You don't need to create them manually** unless you want to customize them.

The workflow will:
1. Check if the role exists (`country-service-lambda-execution-staging` or `country-service-lambda-execution-production`)
2. If it doesn't exist, create it with:
   - Trust policy allowing Lambda service to assume it
   - Permissions for DynamoDB table access
   - Permissions for CloudWatch Logs
3. Use the role ARN when creating the Lambda function

**Note**: If you want to use a custom role, you can still provide `LAMBDA_EXECUTION_ROLE_ARN_STAGING` or `LAMBDA_EXECUTION_ROLE_ARN_PRODUCTION` secrets, and the workflow will use those instead of creating new ones.

## Manual Creation (Optional)

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

## Step 2: Get the Role ARN

After creating the role, get its ARN:

```bash
aws iam get-role --role-name country-service-lambda-execution-staging --query 'Role.Arn' --output text
aws iam get-role --role-name country-service-lambda-execution-production --query 'Role.Arn' --output text
```

Or from the IAM Console:
1. Go to Roles → Select your role
2. Copy the ARN from the role summary

## Step 3: Configure GitHub Secrets

Add these secrets to your GitHub repository:

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `LAMBDA_EXECUTION_ROLE_ARN_STAGING` | `arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-staging` | IAM role ARN for Lambda execution (staging) |
| `LAMBDA_EXECUTION_ROLE_ARN_PRODUCTION` | `arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-production` | IAM role ARN for Lambda execution (production) |

## Step 4: Verify

The deployment workflow will use these role ARNs when creating Lambda functions. The workflow will automatically:
1. Check if the Lambda function exists
2. If it doesn't exist, create it with the specified execution role
3. If it exists, update the function code

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

