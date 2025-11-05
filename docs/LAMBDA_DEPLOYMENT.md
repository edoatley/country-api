# Lambda Deployment Guide

This guide explains how to deploy the Country Reference Service as an AWS Lambda function behind API Gateway.

## Overview

The service is deployed as a Lambda function that handles API Gateway proxy events. The Lambda handler (`LambdaEntryPoint`) processes incoming API Gateway requests and routes them to the appropriate business logic.

## Prerequisites

1. **AWS Account** with appropriate permissions
2. **AWS CLI** configured with credentials
3. **DynamoDB Table** created (see `DYNAMODB_SETUP.md` for table creation)
4. **API Gateway** REST API configured with Lambda integration
5. **GitHub Secrets** configured (see below)

## Build Lambda Package

The Lambda deployment package is built using Gradle:

```bash
./gradlew :country-service-adapters:buildLambdaPackage
```

This creates a fat JAR at:
```
country-service-adapters/build/libs/country-service-lambda-<version>.jar
```

The JAR includes all dependencies required for Lambda execution.

## Lambda Configuration

### Handler Class

The Lambda handler class is:
```
com.example.country.adapters.lambda.LambdaEntryPoint
```

Configure this in the Lambda function settings.

### Environment Variables

The Lambda function requires the following environment variables:

| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `AWS_REGION` | AWS region for DynamoDB | Yes | `us-east-1` |
| `API_KEY` | API key for authentication | Yes | `your-api-key-here` |
| `DYNAMODB_TABLE_NAME` | DynamoDB table name | Optional | `Countries` |
| `AWS_ENDPOINT_URL` | Override AWS endpoint (for LocalStack) | No | `http://localhost:4566` |

### Runtime Configuration

- **Runtime**: Java 21 (Corretto or Amazon Linux 2023)
- **Handler**: `com.example.country.adapters.lambda.LambdaEntryPoint::handleRequest`
- **Timeout**: 30 seconds (recommended)
- **Memory**: 512 MB (recommended, adjust based on load)
- **Architecture**: x86_64 or arm64

### IAM Permissions

The Lambda execution role requires the following permissions:

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
        "arn:aws:dynamodb:REGION:ACCOUNT:table/Countries",
        "arn:aws:dynamodb:REGION:ACCOUNT:table/Countries/index/*"
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

## API Gateway Configuration

### REST API Setup

1. Create a REST API in API Gateway
2. Configure routes matching the OpenAPI specification:
   - `GET /api/v1/countries`
   - `POST /api/v1/countries`
   - `GET /api/v1/countries/code/{alpha2Code}`
   - `PUT /api/v1/countries/code/{alpha2Code}`
   - `DELETE /api/v1/countries/code/{alpha2Code}`
   - `GET /api/v1/countries/code/{alpha2Code}/history`
   - `GET /api/v1/countries/code3/{alpha3Code}`
   - `GET /api/v1/countries/number/{numericCode}`

3. Configure Lambda integration for each route:
   - Integration type: Lambda Function
   - Lambda function: `country-service-lambda-<environment>`
   - Use Lambda Proxy Integration: Yes

### API Key Configuration

1. Create an API Key in API Gateway
2. Configure usage plans to associate the API key with your API
3. Configure API key requirement for each endpoint:
   - Method Request → API Key Required: Yes

### CORS Configuration (if needed)

If the API will be accessed from a browser, configure CORS:
- Access-Control-Allow-Origin: `*` (or specific domain)
- Access-Control-Allow-Headers: `X-API-KEY, Content-Type`
- Access-Control-Allow-Methods: `GET, POST, PUT, DELETE, OPTIONS`

## Deployment via GitHub Actions

### Required GitHub Secrets

### Option 1: OIDC Authentication (Recommended)

Configure the following secrets in your GitHub repository:

| Secret | Description | Example |
|--------|-------------|---------|
| `AWS_ROLE_ARN_STAGING` | IAM role ARN for staging deployment | `arn:aws:iam::123456789012:role/GitHubActions-Deploy-Staging` |
| `AWS_ROLE_ARN_PRODUCTION` | IAM role ARN for production deployment | `arn:aws:iam::123456789012:role/GitHubActions-Deploy-Production` |
| `API_KEY` | API key for staging environment | `staging-api-key` |
| `API_KEY_PROD` | API key for production environment | `prod-api-key` |
| `API_GATEWAY_URL_STAGING` | API Gateway URL for staging (optional, for smoke tests) | `https://abc123.execute-api.us-east-1.amazonaws.com` |
| `API_GATEWAY_URL_PRODUCTION` | API Gateway URL for production (optional, for smoke tests) | `https://xyz789.execute-api.us-east-1.amazonaws.com` |

**See:**
- [AWS OIDC Setup Guide](AWS_OIDC_SETUP.md) for detailed instructions on setting up OIDC authentication
- [API Key Setup Guide](API_KEY_SETUP.md) for instructions on generating and configuring API keys

### Option 2: Access Keys (Legacy - Not Recommended)

If not using OIDC, configure these instead:

| Secret | Description | Example |
|--------|-------------|---------|
| `AWS_ACCESS_KEY_ID` | AWS access key ID | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | AWS secret access key | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |
| `API_KEY` | API key for staging environment | `staging-api-key` |
| `API_KEY_PROD` | API key for production environment | `prod-api-key` |

**Note**: OIDC is recommended for better security. See [AWS OIDC Setup Guide](AWS_OIDC_SETUP.md).

### Deployment Process

1. **Tag Release**: Create a git tag for the release:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **Automatic Deployment**: The workflow automatically:
   - Builds the Lambda package
   - Deploys to staging environment
   - Runs smoke tests (if configured)

3. **Manual Production Deployment**: 
   - Go to Actions → Deploy workflow
   - Click "Run workflow"
   - Select "production" environment
   - Requires manual approval (configured in GitHub Environments)

### Manual Deployment

If deploying manually:

```bash
# Build the Lambda package
./gradlew :country-service-adapters:buildLambdaPackage

# Upload to Lambda
aws lambda update-function-code \
  --function-name country-service-lambda-staging \
  --zip-file fileb://country-service-adapters/build/libs/country-service-lambda-0.1.0-SNAPSHOT.jar \
  --region us-east-1

# Update environment variables
aws lambda update-function-configuration \
  --function-name country-service-lambda-staging \
  --environment "Variables={AWS_REGION=us-east-1,API_KEY=your-api-key,DYNAMODB_TABLE_NAME=Countries}" \
  --region us-east-1
```

## Verification

### Smoke Tests

After deployment, verify the Lambda function is working:

```bash
# Test list endpoint
curl -X GET "https://YOUR_API_GATEWAY_URL/api/v1/countries" \
  -H "X-API-KEY: your-api-key"

# Test get by code
curl -X GET "https://YOUR_API_GATEWAY_URL/api/v1/countries/code/GB" \
  -H "X-API-KEY: your-api-key"
```

### CloudWatch Logs

Monitor Lambda execution logs in CloudWatch:
- Log Group: `/aws/lambda/country-service-lambda-<environment>`
- Check for errors, execution time, and memory usage

## Troubleshooting

### Common Issues

1. **Timeout Errors**: Increase Lambda timeout (default: 30 seconds)
2. **Memory Issues**: Increase Lambda memory allocation
3. **Permission Errors**: Verify IAM role has DynamoDB permissions
4. **API Key Errors**: Verify API key is correctly configured in Lambda environment variables
5. **Cold Start**: First invocation may be slow; consider using Lambda provisioned concurrency

### Debugging

1. Check CloudWatch Logs for Lambda execution errors
2. Test Lambda function directly in AWS Console (Test tab)
3. Verify API Gateway integration settings
4. Check DynamoDB table exists and is accessible

## Rollback

To rollback to a previous version:

1. Find the previous Lambda package (artifact from previous deployment)
2. Upload the previous JAR to Lambda:
   ```bash
   aws lambda update-function-code \
     --function-name country-service-lambda-staging \
     --zip-file fileb://previous-version.jar
   ```

Or use the GitHub Actions workflow to deploy a previous tag:
```bash
git tag v0.9.0
git push origin v0.9.0
```

## Next Steps

- [ ] Set up API Gateway custom domain
- [ ] Configure CloudWatch alarms for Lambda errors
- [ ] Set up Lambda provisioned concurrency for production
- [ ] Configure API Gateway throttling and rate limiting
- [ ] Set up automated smoke tests in CI/CD pipeline

