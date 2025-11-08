# Infrastructure Deployment

This directory contains CloudFormation templates and deployment scripts for the Country Reference Service.

## Prerequisites

1. **AWS CLI** configured with appropriate credentials
2. **DynamoDB Table** - The table must exist before deploying the Lambda function
3. **Lambda Execution Roles** - Deploy these first using `deploy-roles.sh`
4. **S3 Bucket** - For storing Lambda deployment packages (default: `country-service-lambda-deployments`)

## Deployment Order

1. **Deploy Lambda Execution Roles** (if not already deployed):
   ```bash
   cd infrastructure
   ./deploy-roles.sh [region] [profile]
   ```

2. **Deploy DynamoDB Table** (if not already deployed):
   ```bash
   cd infrastructure
   ./deploy-dynamodb.sh [region] [profile]
   ```

3. **Build and Upload Lambda Package**:
   ```bash
   # Build the Lambda package
   ./gradlew :country-service-adapters:buildLambdaPackage
   
   # Upload to S3 (replace with your bucket name)
   aws s3 cp country-service-adapters/build/libs/country-service-lambda-*.jar \
     s3://country-service-lambda-deployments/lambda-packages/
   ```

4. **Deploy Lambda + API Gateway Stack**:
   ```bash
   cd infrastructure
   export API_KEY=your-api-key
   ./deploy-stack.sh staging us-east-1 "" country-service-lambda-deployments lambda-packages/country-service-lambda-0.1.0-SNAPSHOT.jar
   ```

## Files

- **`lambda-execution-roles.yaml`**: CloudFormation template for Lambda execution IAM roles
- **`dynamodb-table.yaml`**: CloudFormation template for the DynamoDB table
- **`lambda-api-gateway.yaml`**: CloudFormation template for Lambda function and API Gateway
- **`deploy-roles.sh`**: Script to deploy Lambda execution roles
- **`deploy-dynamodb.sh`**: Script to deploy DynamoDB table
- **`deploy-stack.sh`**: Script to deploy Lambda + API Gateway stack
- **`test-api.sh`**: Script to test the API from command line

## Testing the API

After deployment, you can test the API using the `test-api.sh` script:

```bash
cd infrastructure
./test-api.sh https://abc123.execute-api.us-east-1.amazonaws.com/staging your-api-key
```

Or get the API Gateway URL from CloudFormation:

```bash
aws cloudformation describe-stacks \
  --stack-name country-service-staging \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayUrl`].OutputValue' \
  --output text
```

## CloudFormation Stack Outputs

The `country-service-{environment}` stack provides the following outputs:

- **`LambdaFunctionArn`**: ARN of the Lambda function
- **`LambdaFunctionName`**: Name of the Lambda function
- **`ApiGatewayId`**: ID of the API Gateway
- **`ApiGatewayUrl`**: Base URL of the API Gateway (e.g., `https://abc123.execute-api.us-east-1.amazonaws.com/staging`)
- **`ApiGatewayRootUrl`**: Root API URL (e.g., `https://abc123.execute-api.us-east-1.amazonaws.com/staging/api/v1`)

## Troubleshooting

### DynamoDB Table Not Found

If you get a "Requested resource not found" error for DynamoDB:

1. Check if the table exists:
   ```bash
   aws dynamodb describe-table --table-name Countries --region us-east-1
   ```

2. If it doesn't exist, deploy it:
   ```bash
   cd infrastructure
   ./deploy-dynamodb.sh us-east-1
   ```

### API Gateway URL Not Found

The API Gateway URL is output from the CloudFormation stack. If it's not available:

1. Check the stack status:
   ```bash
   aws cloudformation describe-stacks --stack-name country-service-staging
   ```

2. Check stack events for errors:
   ```bash
   aws cloudformation describe-stack-events --stack-name country-service-staging --max-items 10
   ```

### Lambda Function Not Found

If the Lambda function doesn't exist, the CloudFormation stack will create it. If it already exists, the stack will update it.
