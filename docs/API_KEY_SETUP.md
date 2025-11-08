# API Key Setup Guide

This guide explains how to generate, configure, and use API keys for the Country Reference Service.

## Overview

The API key is a **shared secret** used to authenticate requests to the Country Reference Service. It's not something you "get" from AWS - you **generate it yourself** and configure it in:

1. **Lambda environment variables** (so the Lambda function knows what key to expect)
2. **GitHub secrets** (so the deployment workflow can set it in Lambda)
3. **Client applications** (so they can send it in requests)

## Step 1: Generate API Keys

You need to generate **secure, random API keys** for each environment (staging and production).

### Option 1: Using OpenSSL (Recommended)

```bash
# Generate a secure random API key (32 characters, base64-encoded)
openssl rand -base64 32

# Example output:
# 8K9mP3qR5tW7xY2zA1bC4dE6fG8hI0jK=
```

### Option 2: Using Python

```bash
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# Example output:
# 8K9mP3qR5tW7xY2zA1bC4dE6fG8hI0jK-L9mN2oP4qR6sT8uV0wX2yZ
```

### Option 3: Using Online Tools

You can use online tools like:
- [Random.org](https://www.random.org/strings/)
- [1Password Generator](https://1password.com/password-generator/)
- [Bitwarden Password Generator](https://bitwarden.com/password-generator/)

**Recommendation**: Use at least 32 characters for security.

### Generate Separate Keys for Each Environment

Generate **different keys** for staging and production:

```bash
# Staging API key
STAGING_API_KEY=$(openssl rand -base64 32)
echo "Staging API Key: $STAGING_API_KEY"

# Production API key
PRODUCTION_API_KEY=$(openssl rand -base64 32)
echo "Production API Key: $PRODUCTION_API_KEY"
```

**Security Best Practice**: Use different keys for staging and production to limit exposure if one is compromised.

## Step 2: Configure API Keys in Lambda

The API key needs to be set as an environment variable in your Lambda function. This is done automatically by the deployment workflow, but you can also configure it manually.

### Automatic Configuration (via GitHub Actions)

The deployment workflow (`.github/workflows/deploy.yml`) automatically sets the API key as an environment variable when deploying:

```yaml
- name: Update Lambda environment variables
  run: |
    aws lambda update-function-configuration \
      --function-name ${{ env.LAMBDA_FUNCTION_NAME }}-staging \
      --environment "Variables={AWS_REGION=${{ env.AWS_REGION }},API_KEY=${{ secrets.API_KEY }},DYNAMODB_TABLE_NAME=Countries}" \
      --region ${{ env.AWS_REGION }}
```

### Manual Configuration (via AWS Console)

1. **Open Lambda Console** → Select your function (e.g., `country-service-lambda-staging`)
2. **Configuration** → Environment variables → Edit
3. **Add environment variable**:
   - Key: `API_KEY`
   - Value: Your generated API key
4. **Save**

### Manual Configuration (via AWS CLI)

```bash
# For staging
aws lambda update-function-configuration \
  --function-name country-service-lambda-staging \
  --environment "Variables={AWS_REGION=us-east-1,API_KEY=YOUR_STAGING_API_KEY,DYNAMODB_TABLE_NAME=Countries}" \
  --region us-east-1

# For production
aws lambda update-function-configuration \
  --function-name country-service-lambda-production \
  --environment "Variables={AWS_REGION=us-east-1,API_KEY=YOUR_PRODUCTION_API_KEY,DYNAMODB_TABLE_NAME=Countries}" \
  --region us-east-1
```

## Step 3: Configure API Keys in GitHub Secrets

The deployment workflow needs the API keys to configure the Lambda function. Add them as GitHub secrets:

1. **Go to GitHub Repository** → Settings → Secrets and variables → Actions

2. **Add the following secrets**:

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `API_KEY` | Your staging API key | API key for staging environment |
| `API_KEY_PROD` | Your production API key | API key for production environment |

### Adding Secrets via GitHub UI

1. Click **"New repository secret"**
2. Name: `API_KEY`
3. Value: Paste your staging API key
4. Click **"Add secret"**
5. Repeat for `API_KEY_PROD` with your production API key

### Adding Secrets via GitHub CLI

```bash
# Set staging API key
gh secret set API_KEY --body "YOUR_STAGING_API_KEY"

# Set production API key
gh secret set API_KEY_PROD --body "YOUR_PRODUCTION_API_KEY"
```

## Step 4: Verify Configuration

### Verify Lambda Environment Variable

```bash
# Check staging Lambda environment variables
aws lambda get-function-configuration \
  --function-name country-service-lambda-staging \
  --query 'Environment.Variables.API_KEY' \
  --region us-east-1

# Check production Lambda environment variables
aws lambda get-function-configuration \
  --function-name country-service-lambda-production \
  --query 'Environment.Variables.API_KEY' \
  --region us-east-1
```

### Test API Key Authentication

Once deployed, test that the API key works:

```bash
# Replace with your actual API Gateway URL and API key
curl -X GET "https://YOUR_API_GATEWAY_URL/api/v1/countries" \
  -H "X-API-KEY: YOUR_API_KEY" \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected**: HTTP 200 OK with a list of countries

**Without API key**:

```bash
curl -X GET "https://YOUR_API_GATEWAY_URL/api/v1/countries" \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected**: HTTP 401 Unauthorized with error message

## How Clients Use the API Key

Clients must include the API key in the `X-API-KEY` header for all requests:

### Example: cURL

```bash
curl -X GET "https://api.example.com/api/v1/countries" \
  -H "X-API-KEY: YOUR_API_KEY"
```

### Example: JavaScript (Fetch API)

```javascript
fetch('https://api.example.com/api/v1/countries', {
  headers: {
    'X-API-KEY': 'YOUR_API_KEY'
  }
})
  .then(response => response.json())
  .then(data => console.log(data));
```

### Example: Python (Requests)

```python
import requests

headers = {
    'X-API-KEY': 'YOUR_API_KEY'
}

response = requests.get(
    'https://api.example.com/api/v1/countries',
    headers=headers
)

print(response.json())
```

### Example: Java (OkHttp)

```java
Request request = new Request.Builder()
    .url("https://api.example.com/api/v1/countries")
    .addHeader("X-API-KEY", "YOUR_API_KEY")
    .build();

Response response = client.newCall(request).execute();
```

## API Key Rotation

Periodically rotate API keys for security. Here's the process:

### Step 1: Generate New API Key

```bash
NEW_API_KEY=$(openssl rand -base64 32)
echo "New API Key: $NEW_API_KEY"
```

### Step 2: Update Lambda Environment Variable

```bash
# Update staging Lambda
aws lambda update-function-configuration \
  --function-name country-service-lambda-staging \
  --environment "Variables={AWS_REGION=us-east-1,API_KEY=$NEW_API_KEY,DYNAMODB_TABLE_NAME=Countries}" \
  --region us-east-1
```

### Step 3: Update GitHub Secret

1. Go to GitHub → Settings → Secrets and variables → Actions
2. Find `API_KEY` → Update
3. Paste new API key → Update secret

### Step 4: Update All Clients

Update all client applications to use the new API key.

### Step 5: Verify

Test that the new API key works with all clients.

**Important**: Keep the old API key active for a short grace period (e.g., 24 hours) to allow all clients to update, then remove it.

## Security Best Practices

1. **Use Strong Keys**: Generate keys with at least 32 random characters
2. **Separate Keys**: Use different keys for staging and production
3. **Rotate Regularly**: Rotate keys periodically (e.g., every 90 days)
4. **Secure Storage**: Store keys securely:
   - Use GitHub secrets (encrypted at rest)
   - Use AWS Secrets Manager (if you want to manage keys in AWS)
   - Don't commit keys to version control
5. **Monitor Usage**: Monitor API key usage for suspicious activity
6. **Revoke Compromised Keys**: Immediately rotate keys if compromised

## Alternative: AWS Secrets Manager (Advanced)

For more advanced key management, consider using AWS Secrets Manager:

1. **Store API keys in Secrets Manager**:

```bash
aws secretsmanager create-secret \
  --name country-service/api-key-staging \
  --secret-string "YOUR_STAGING_API_KEY" \
  --region us-east-1

aws secretsmanager create-secret \
  --name country-service/api-key-production \
  --secret-string "YOUR_PRODUCTION_API_KEY" \
  --region us-east-1
```

2. **Grant Lambda permission to read secrets**:

Add IAM policy to Lambda execution role:

```json
{
  "Effect": "Allow",
  "Action": [
    "secretsmanager:GetSecretValue"
  ],
  "Resource": [
    "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:country-service/api-key-*"
  ]
}
```

3. **Update Lambda code** to read from Secrets Manager instead of environment variables.

**Note**: This requires code changes and is more complex. For most use cases, environment variables are sufficient.

## Troubleshooting

### Error: "Missing or invalid API key" (401 Unauthorized)

**Possible causes:**
1. API key not sent in `X-API-KEY` header
2. API key mismatch between client and Lambda
3. Header name is case-sensitive (should be `X-API-KEY`)

**Solution:**
- Verify the client is sending `X-API-KEY` header
- Check that the API key in Lambda matches the one in the client
- Verify header name is exactly `X-API-KEY` (case-sensitive)

### Error: "API_KEY environment variable must be set"

**Possible causes:**
1. Lambda environment variable not configured
2. Environment variable name is incorrect (should be `API_KEY`)

**Solution:**
- Check Lambda function configuration → Environment variables
- Verify `API_KEY` is set (not `API-KEY` or `api_key`)

### API Key Not Working After Deployment

**Possible causes:**
1. Deployment workflow didn't update environment variables
2. GitHub secret not set correctly

**Solution:**
- Check deployment workflow logs
- Verify GitHub secrets are set correctly
- Manually update Lambda environment variables if needed

## Summary

1. **Generate** secure random API keys (one for staging, one for production)
2. **Configure** API keys in Lambda environment variables (via deployment workflow or manually)
3. **Store** API keys in GitHub secrets (for deployment workflow)
4. **Share** API keys with client applications (they need it to authenticate)
5. **Rotate** API keys periodically for security

The API key is a shared secret - treat it like a password and keep it secure!

