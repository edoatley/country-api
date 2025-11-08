# AWS OIDC Setup for GitHub Actions

This guide explains how to set up OpenID Connect (OIDC) authentication between GitHub Actions and AWS, eliminating the need for long-lived AWS access keys.

## Overview

Instead of storing AWS access keys as GitHub secrets, we use OIDC to allow GitHub Actions to assume IAM roles in AWS. This provides:
- **Better Security**: No long-lived credentials stored in GitHub
- **Short-lived Tokens**: Credentials are automatically rotated
- **Audit Trail**: Better visibility into which GitHub workflow is making AWS calls
- **Fine-grained Permissions**: Different roles for staging vs production

## Prerequisites

- AWS Account with appropriate permissions
- AWS CLI configured locally (for setup commands)
- GitHub repository with Actions enabled

## Step 1: Create OIDC Identity Provider in AWS

### Via AWS Management Console

1. **Open IAM Console** → Identity providers → Add provider

2. **Provider Type**: OpenID Connect

3. **Provider URL**: `https://token.actions.githubusercontent.com`

4. **Audience**: `sts.amazonaws.com`

5. **Get the thumbprint**: Click "Get thumbprint" (or use the thumbprint: `6938fd4d98bab03faadb97b34396831e3780aea1`)

6. **Add provider**

### Via AWS CLI

```bash
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

**Note**: You only need to create the OIDC provider once per AWS account.

## Step 2: Create IAM Roles for GitHub Actions

Create separate IAM roles for staging and production deployments.

### Get Your AWS Account ID

```bash
aws sts get-caller-identity --query Account --output text
```

### Staging Role

1. **Create IAM Role**:
   - Go to IAM Console → Roles → Create role
   - Select "Web identity"
   - Identity provider: `token.actions.githubusercontent.com`
   - Audience: `sts.amazonaws.com`
   - Click "Next"

2. **Configure Trust Policy**:

Replace `ACCOUNT_ID`, `YOUR_GITHUB_ORG`, and `YOUR_REPO` with your values:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:YOUR_GITHUB_ORG/YOUR_REPO:*"
        }
      }
    }
  ]
}
```

**More restrictive** (only allow specific branches/tags):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": [
            "repo:YOUR_GITHUB_ORG/YOUR_REPO:ref:refs/heads/main",
            "repo:YOUR_GITHUB_ORG/YOUR_REPO:ref:refs/tags/v*"
          ]
        }
      }
    }
  ]
}
```

3. **Attach Permissions Policy**:

The role needs permissions to:
- Create/update Lambda functions
- Pass Lambda execution roles to Lambda (required for `lambda:CreateFunction`)
- Read CloudFormation stack outputs (to discover role ARNs)
- Get AWS account ID

**⚠️ Important**: The `iam:PassRole` permission is **required** and must be granted on the specific Lambda execution role ARNs. Without this permission, you will get an `AccessDeniedException` when trying to create Lambda functions.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "lambda:CreateFunction",
        "lambda:UpdateFunctionCode",
        "lambda:UpdateFunctionConfiguration",
        "lambda:GetFunction",
        "lambda:GetFunctionConfiguration"
      ],
      "Resource": "arn:aws:lambda:*:ACCOUNT_ID:function:country-service-lambda-staging"
    },
    {
      "Effect": "Allow",
      "Action": [
        "lambda:ListFunctions"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:PassRole"
      ],
      "Resource": [
        "arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-staging",
        "arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-production"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:GetRole"
      ],
      "Resource": [
        "arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-staging",
        "arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-production"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "cloudformation:DescribeStacks",
        "cloudformation:DescribeStackOutputs"
      ],
      "Resource": "arn:aws:cloudformation:*:ACCOUNT_ID:stack/country-service-lambda-execution-roles/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "sts:GetCallerIdentity"
      ],
      "Resource": "*"
    }
  ]
}
```

4. **Name the role**: `GitHubActions-Deploy-Staging` (or your preferred name)

5. **Create role**

### Production Role

Create a similar role for production with:
- Role name: `GitHubActions-Deploy-Production` (or your preferred name)
- Same trust policy but can be more restrictive (e.g., only allow manual workflow dispatch)
- Permissions for `country-service-lambda-production`

**Production Trust Policy** (restrict to manual workflow dispatch only):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": "repo:YOUR_GITHUB_ORG/YOUR_REPO:workflow:Deploy"
        }
      }
    }
  ]
}
```

## Step 3: Configure GitHub Secrets

Instead of `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`, configure these secrets:

1. Go to **GitHub Repository** → Settings → Secrets and variables → Actions

2. Add the following secrets:

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `AWS_ROLE_ARN_STAGING` | `arn:aws:iam::ACCOUNT_ID:role/GitHubActions-Deploy-Staging` | IAM role ARN for staging |
| `AWS_ROLE_ARN_PRODUCTION` | `arn:aws:iam::ACCOUNT_ID:role/GitHubActions-Deploy-Production` | IAM role ARN for production |
| `LAMBDA_EXECUTION_ROLE_ARN_STAGING` | `arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-staging` | IAM role ARN for Lambda execution (staging, optional - will be created automatically if not provided) |
| `LAMBDA_EXECUTION_ROLE_ARN_PRODUCTION` | `arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-production` | IAM role ARN for Lambda execution (production, optional - will be created automatically if not provided) |
| `API_KEY` | Your staging API key | API key for staging environment |
| `API_KEY_PROD` | Your production API key | API key for production environment |
| `API_GATEWAY_URL_STAGING` | `https://abc123.execute-api.us-east-1.amazonaws.com` | API Gateway URL for staging (optional, for smoke tests) |
| `API_GATEWAY_URL_PRODUCTION` | `https://xyz789.execute-api.us-east-1.amazonaws.com` | API Gateway URL for production (optional, for smoke tests) |

**Note**: 
- You no longer need `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` secrets.
- For instructions on generating API keys, see [API Key Setup Guide](API_KEY_SETUP.md).
- `LAMBDA_EXECUTION_ROLE_ARN_*` secrets are optional - the workflow will automatically create the roles if they don't exist.
- For manual role setup instructions, see [Lambda Execution Role Setup Guide](LAMBDA_EXECUTION_ROLE_SETUP.md).
- `API_GATEWAY_URL_*` secrets are optional but recommended for automated smoke tests after deployment.

## Step 4: Verify Workflow Configuration

The workflow file (`.github/workflows/deploy.yml`) has already been updated to use OIDC:

```yaml
permissions:
  contents: read
  id-token: write # Required for OIDC authentication with AWS

# In deploy-staging job:
- name: Configure AWS credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: ${{ secrets.AWS_ROLE_ARN_STAGING }}
    role-session-name: GitHubActions-Deploy-Staging
    aws-region: ${{ env.AWS_REGION }}

# In deploy-production job:
- name: Configure AWS credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: ${{ secrets.AWS_ROLE_ARN_PRODUCTION }}
    role-session-name: GitHubActions-Deploy-Production
    aws-region: ${{ env.AWS_REGION }}
```

## Step 5: Test the Setup

1. **Test the workflow**:
   - Go to Actions → Deploy workflow
   - Click "Run workflow"
   - Select "staging"
   - Click "Run workflow"

2. **Check the logs**:
   - The "Configure AWS credentials" step should succeed
   - If it fails, check:
     - OIDC provider is correctly configured
     - IAM role trust policy matches your repository
     - GitHub secret contains the correct role ARN

## Troubleshooting

### Error: "Not authorized to perform sts:AssumeRoleWithWebIdentity"

**Possible causes:**
1. OIDC provider not configured correctly
2. IAM role trust policy doesn't match your repository
3. Role ARN in GitHub secret is incorrect

**Solution:**
- Verify the OIDC provider exists: `aws iam list-open-id-connect-providers`
- Check the role trust policy includes your repository (format: `repo:ORG/REPO:*`)
- Verify the role ARN format: `arn:aws:iam::ACCOUNT_ID:role/ROLE_NAME`

### Error: "The request signature we calculated does not match"

**Possible causes:**
- OIDC provider thumbprint is incorrect
- Provider URL is incorrect

**Solution:**
- Verify OIDC provider URL: `https://token.actions.githubusercontent.com`
- Verify thumbprint: `6938fd4d98bab03faadb97b34396831e3780aea1`

### Error: "The role session name is invalid"

**Possible causes:**
- Role session name contains invalid characters

**Solution:**
- Use alphanumeric characters and hyphens only
- Keep it under 64 characters

### Error: "User is not authorized to perform: iam:PassRole"

**This is a common error when creating Lambda functions.**

**Possible causes:**
- The GitHub Actions deployment role doesn't have `iam:PassRole` permission for the Lambda execution roles

**Solution:**
1. **Go to IAM Console** → Roles → Select your GitHub Actions deployment role (e.g., `GitHubActions-Deploy-Staging`)

2. **Add or update the permissions policy** to include `iam:PassRole`:

   ```json
   {
     "Effect": "Allow",
     "Action": [
       "iam:PassRole"
     ],
     "Resource": [
       "arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-staging",
       "arn:aws:iam::ACCOUNT_ID:role/country-service-lambda-execution-production"
     ]
   }
   ```

3. **Replace `ACCOUNT_ID`** with your AWS account ID (e.g., `727361020121`)

4. **Important**: The `iam:PassRole` permission must be granted on the **specific role ARNs** that you want to pass to Lambda. You cannot use wildcards (`*`) for the resource in `iam:PassRole` permissions.

5. **Verify the role ARNs** match exactly what was created by CloudFormation:
   ```bash
   aws cloudformation describe-stacks \
     --stack-name country-service-lambda-execution-roles \
     --query 'Stacks[0].Outputs' \
     --output table
   ```

**Note**: This permission is required because when creating a Lambda function, you must "pass" the execution role to Lambda. The `iam:PassRole` permission allows the GitHub Actions role to specify which execution role the Lambda function should use.

## Security Best Practices

1. **Restrict by repository**: Use `StringLike` condition to only allow your specific repository
2. **Restrict by branch/tag**: Limit staging to specific branches, production to tags only
3. **Separate roles**: Use different IAM roles for staging and production
4. **Least privilege**: Grant only the minimum permissions needed
5. **Audit regularly**: Review CloudTrail logs for role assumptions

## Example: Complete Trust Policy with Restrictions

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": [
            "repo:YOUR_ORG/YOUR_REPO:ref:refs/heads/main",
            "repo:YOUR_ORG/YOUR_REPO:ref:refs/tags/v*"
          ]
        }
      }
    }
  ]
}
```

## Verification Commands

### Check OIDC Provider

```bash
aws iam list-open-id-connect-providers
```

### Check IAM Role Trust Policy

```bash
aws iam get-role --role-name GitHubActions-Deploy-Staging --query 'Role.AssumeRolePolicyDocument'
aws iam get-role --role-name GitHubActions-Deploy-Production --query 'Role.AssumeRolePolicyDocument'
```

### Get Role ARN

```bash
aws iam get-role --role-name GitHubActions-Deploy-Staging --query 'Role.Arn' --output text
aws iam get-role --role-name GitHubActions-Deploy-Production --query 'Role.Arn' --output text
```

## Migration from Access Keys

If you're currently using AWS access keys:

1. **Set up OIDC** (follow steps above)
2. **Update GitHub secrets** with role ARNs
3. **Update workflow file** (already done)
4. **Test deployment** to staging
5. **Remove old secrets** (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`) once verified
6. **Rotate/deactivate** the old access keys in AWS

## Additional Resources

- [AWS Documentation: Creating OpenID Connect identity providers](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc.html)
- [GitHub Documentation: Security hardening with OpenID Connect](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- [AWS IAM Roles for GitHub Actions](https://aws.amazon.com/blogs/security/use-iam-roles-to-connect-github-actions-to-actions-in-aws/)

