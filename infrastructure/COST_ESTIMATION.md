# Infrastructure Cost Estimation

This document provides cost estimates for the Country Reference Service infrastructure running on AWS.

## Infrastructure Components

1. **AWS Lambda** - Function execution
   - Memory: 512 MB
   - Timeout: 30 seconds
   - Runtime: Java 21
   - Billing: Pay per request and execution time

2. **API Gateway** - REST API endpoint
   - Type: REST API
   - Billing: Pay per API call

3. **DynamoDB** - Data storage
   - Billing Mode: PAY_PER_REQUEST (on-demand)
   - Table: Countries
   - Global Secondary Indexes: 2 (GSI-Alpha3, GSI-Numeric)

4. **S3** - Lambda package storage
   - Bucket: country-service-lambda-deployments
   - Storage: ~21 MB per deployment package

## Cost Breakdown (as of 2024)

### AWS Lambda

**Pricing:**
- Requests: $0.20 per 1 million requests
- Compute: $0.0000166667 per GB-second

**Example Calculation (1 million requests/month):**
- Each request: 200ms average, 512 MB memory
- Compute: 1M × 0.2s × 0.5 GB × $0.0000166667 = **$1.67**
- Requests: 1M × $0.20/1M = **$0.20**
- **Total Lambda: $1.87/month**

### API Gateway (REST API)

**Pricing:**
- API Calls: $3.50 per 1 million requests
- Data Transfer Out: $0.09 per GB (first 10 TB/month)

**Example Calculation (1 million requests/month):**
- API Calls: 1M × $3.50/1M = **$3.50**
- Data Transfer: ~0.1 GB × $0.09 = **$0.01** (negligible for low traffic)
- **Total API Gateway: $3.50/month**

### DynamoDB (On-Demand)

**Pricing:**
- Write Request Units: $1.25 per million
- Read Request Units: $0.25 per million
- Storage: $0.25 per GB-month (first 25 GB free)

**Example Calculation (1M writes, 2M reads/month, 1 GB storage):**
- Writes: 1M × $1.25/1M = **$1.25**
- Reads: 2M × $0.25/1M = **$0.50**
- Storage: 1 GB (within free tier) = **$0.00**
- **Total DynamoDB: $1.75/month**

### S3 Storage

**Pricing:**
- Standard Storage: $0.023 per GB-month (first 5 GB free)
- PUT Requests: $0.005 per 1,000 requests

**Example Calculation:**
- Storage: ~21 MB × $0.023/GB = **$0.0005/month** (negligible)
- PUT Requests: ~10 deployments/month × $0.005/1K = **$0.00005/month** (negligible)
- **Total S3: < $0.01/month**

## Total Monthly Cost Estimate

### Low Traffic Scenario (1M requests/month)
- Lambda: $1.87
- API Gateway: $3.50
- DynamoDB: $1.75
- S3: $0.01
- **Total: ~$7.13/month**

### Medium Traffic Scenario (10M requests/month)
- Lambda: $18.70
- API Gateway: $35.00
- DynamoDB: $17.50
- S3: $0.01
- **Total: ~$71.21/month**

### High Traffic Scenario (100M requests/month)
- Lambda: $187.00
- API Gateway: $350.00
- DynamoDB: $175.00
- S3: $0.01
- **Total: ~$712.01/month**

## Idle Cost (No Traffic)

If the infrastructure is left running with **zero traffic**:

- **Lambda**: $0.00 (no requests = no charges)
- **API Gateway**: $0.00 (no API calls = no charges)
- **DynamoDB**: $0.00 (on-demand, no operations = no charges, storage is free for < 25 GB)
- **S3**: ~$0.01/month (storage only)
- **CloudFormation**: $0.00 (free)

**Total Idle Cost: < $0.01/month** (essentially free)

## AWS Free Tier

The following services have free tier allowances:

1. **Lambda**: 1 million free requests per month (permanent free tier)
2. **API Gateway**: No free tier for REST APIs (HTTP APIs have free tier)
3. **DynamoDB**: 25 GB storage free (permanent), 2.5 million read units and 2.5 million write units free for 12 months
4. **S3**: 5 GB storage free (permanent)

## Cost Optimization Tips

1. **Use HTTP API instead of REST API** (if possible):
   - HTTP API: $1.00 per million requests (vs $3.50 for REST API)
   - Could save ~$2.50 per million requests

2. **Optimize Lambda memory**:
   - Monitor actual memory usage
   - Reduce memory if not needed (reduces compute cost)

3. **Enable DynamoDB auto-scaling** (if using provisioned capacity):
   - Currently using on-demand, which is optimal for variable traffic

4. **Monitor and set up billing alerts**:
   - Set up AWS Cost Explorer
   - Configure billing alerts in AWS Budgets

5. **Clean up unused resources**:
   - Delete old Lambda deployment packages from S3
   - Remove unused CloudFormation stacks

## Cost Monitoring

### AWS Cost Explorer
- Navigate to: AWS Console → Billing → Cost Explorer
- View costs by service, time period, etc.

### AWS Budgets
- Set up budget alerts to notify when costs exceed thresholds
- Recommended: Set alert at $10/month for low traffic scenarios

### CloudWatch Billing Alarms
- Create CloudWatch alarms for estimated charges
- Get notified via SNS when costs exceed thresholds

## Cost Calculator Tools

1. **AWS Pricing Calculator**:
   - https://calculator.aws/
   - Official AWS tool for cost estimation

2. **Serverless Cost Calculator**:
   - https://cost-calculator.bref.sh/
   - Community tool for serverless applications

## Example: Calculate Your Actual Costs

To calculate costs for your specific usage:

```bash
# Get Lambda invocation count
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --dimensions Name=FunctionName,Value=country-service-lambda-staging \
  --start-time 2024-11-01T00:00:00Z \
  --end-time 2024-11-30T23:59:59Z \
  --period 86400 \
  --statistics Sum

# Get API Gateway request count
aws cloudwatch get-metric-statistics \
  --namespace AWS/ApiGateway \
  --metric-name Count \
  --dimensions Name=ApiName,Value=country-service-api-staging \
  --start-time 2024-11-01T00:00:00Z \
  --end-time 2024-11-30T23:59:59Z \
  --period 86400 \
  --statistics Sum
```

## Notes

- All prices are in USD and may vary by region
- Prices are current as of 2024 and subject to change
- Actual costs depend on your specific usage patterns
- Consider AWS Reserved Capacity for predictable, high-volume workloads
- DynamoDB on-demand is ideal for unpredictable traffic patterns

