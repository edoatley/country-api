# Swagger UI Validation Instructions

This guide helps you validate that Swagger UI is working correctly before merging.

## Prerequisites

- LocalStack running (for DynamoDB)
- Application running on port 8080

## Step 1: Start LocalStack (Podman)

If LocalStack is not already running:

```bash
# Using podman compose (if available)
podman compose up -d

# OR using docker-compose (Podman can use docker-compose)
docker-compose up -d

# OR manually with podman
podman run -d \
  --name localstack \
  -p 4566:4566 \
  -e SERVICES=dynamodb \
  -e DEBUG=1 \
  -e DATA_DIR=/tmp/localstack/data \
  -v ./.localstack:/tmp/localstack \
  localstack/localstack:latest
```

Verify LocalStack is running:
```bash
podman ps | grep localstack
# OR
curl http://localhost:4566/_localstack/health
```

## Step 2: Start the Application

```bash
# Set environment variables (if not already set)
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_REGION=us-east-1
export API_KEY=default-test-key
export DATA_SEEDING_ENABLED=false  # Set to true if you want to seed data

# Start the application
./gradlew :country-service-bootstrap:bootRun
```

Wait for the application to start (you'll see "Started CountryServiceApplication" in the logs).

## Step 3: Validate Swagger UI

### Option A: Using the Validation Script

```bash
./scripts/validate-swagger-ui.sh
```

This script will:
- Check if the application is running
- Verify Swagger UI is accessible
- Validate OpenAPI JSON structure
- Check for API key security scheme
- Verify country endpoints are present
- Test an API endpoint

### Option B: Manual Validation

1. **Check Swagger UI is accessible:**
   ```bash
   curl -I http://localhost:8080/swagger-ui.html
   ```
   Should return `200 OK`

2. **Check OpenAPI JSON:**
   ```bash
   curl http://localhost:8080/api-docs | jq '.info.title'
   ```
   Should return: `"Country Reference Service API"`

3. **Open in Browser:**
   - Navigate to: `http://localhost:8080/swagger-ui.html`
   - You should see the Swagger UI interface

4. **Test API Key Authentication:**
   - Click the "Authorize" button (lock icon)
   - Enter API key: `default-test-key`
   - Click "Authorize"
   - Close the dialog

5. **Test an Endpoint:**
   - Expand any endpoint (e.g., `GET /api/v1/countries`)
   - Click "Try it out"
   - Click "Execute"
   - Verify you get a `200 OK` response

## Step 4: Verify Key Features

- ✅ Swagger UI loads at `/swagger-ui.html`
- ✅ OpenAPI JSON is available at `/api-docs`
- ✅ All country endpoints are listed
- ✅ API key authentication is configured
- ✅ "Try it out" feature works
- ✅ Requests with API key succeed
- ✅ Requests without API key return 401

## Troubleshooting

### Swagger UI not accessible

- Check application is running: `curl http://localhost:8080/actuator/health`
- Check port 8080 is not in use: `lsof -i :8080`
- Check application logs for errors

### API key authentication not working

- Verify `api.key` property is set in `application.yml` or environment
- Check API key filter is not excluding Swagger UI paths
- Verify `X-API-KEY` header is being sent

### OpenAPI JSON is invalid

- Check SpringDoc OpenAPI dependency is correct version
- Verify `OpenApiConfiguration` class is loaded
- Check application logs for configuration errors

## Expected Results

When everything is working correctly:

1. **Swagger UI** (`http://localhost:8080/swagger-ui.html`):
   - Shows "Country Reference Service API" as title
   - Lists all country endpoints
   - Has "Authorize" button for API key
   - "Try it out" feature works

2. **OpenAPI JSON** (`http://localhost:8080/api-docs`):
   - Valid JSON structure
   - Contains `info.title`: "Country Reference Service API"
   - Contains `components.securitySchemes.ApiKeyAuth`
   - Contains all country endpoints under `paths`

3. **API Endpoints**:
   - All endpoints respond correctly with API key
   - Endpoints return 401 without API key

