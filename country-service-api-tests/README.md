# Country Service API Tests

This module contains tests that run against **deployed** applications (local or staging). It is completely isolated from other modules and has no dependencies on the application code.

## Purpose

These tests verify that the deployed API works correctly end-to-end. They can run against:
- **Local application** (with LocalStack backend)
- **Staging environment** (deployed API Gateway)

## Framework

- **RestAssured**: Framework-agnostic API testing library
- **JUnit 5**: Test framework
- **Awaitility**: For handling eventual consistency

## Running Tests

### Against Local Application

1. **Start LocalStack:**
   ```bash
   docker compose up -d
   # OR with Podman:
   podman compose up -d
   ```

2. **Set up DynamoDB table and seed data:**
   ```bash
   ./scripts/setup-local-dynamodb.sh
   ```

3. **Start the application:**
   ```bash
   export AWS_ENDPOINT_URL=http://localhost:4566
   export AWS_ACCESS_KEY_ID=test
   export AWS_SECRET_ACCESS_KEY=test
   export AWS_REGION=us-east-1
   export API_KEY=default-test-key
   ./gradlew :country-service-bootstrap:bootRun
   ```

4. **Run the tests:**
   ```bash
   ./gradlew :country-service-api-tests:testLocal
   # OR with explicit configuration:
   ./gradlew :country-service-api-tests:test \
     -Dapi.test.base.url=http://localhost:8080 \
     -Dapi.test.api.key=default-test-key
   ```

### Against Staging Environment

```bash
export API_TEST_BASE_URL=https://your-api-gateway-url.execute-api.region.amazonaws.com/staging
export API_TEST_API_KEY=your-staging-api-key

./gradlew :country-service-api-tests:testStaging
```

Or with system properties:
```bash
./gradlew :country-service-api-tests:test \
  -Dapi.test.base.url=https://your-api-gateway-url.execute-api.region.amazonaws.com/staging \
  -Dapi.test.api.key=your-staging-api-key
```

## Configuration

Tests can be configured via:
- **System properties**: `-Dapi.test.base.url=...` and `-Dapi.test.api.key=...`
- **Environment variables**: `API_TEST_BASE_URL` and `API_TEST_API_KEY`
- **Default values**: `http://localhost:8080` and `default-test-key` (for local testing)

## Test Coverage

The test suite covers:
- ✅ List all countries (paginated)
- ✅ Get country by alpha-2 code
- ✅ Get country by alpha-3 code
- ✅ Get country by numeric code
- ✅ Create a new country
- ✅ Update a country
- ✅ Delete a country
- ✅ Get country history
- ✅ Authentication (401 without/invalid API key)
- ✅ Error handling (404 for non-existent countries)
- ✅ Performance validation (<200ms response time requirement)

## Performance Tests

Performance tests validate the PRD requirement: **"API responses must be fast (<200ms in normal use, locally or in dev cluster)"**.

### Running Performance Tests

#### Against Local Application

**Option 1: Automated Script (Recommended)**

Use the convenience script that handles all setup automatically:

```bash
./scripts/local-performance-test.sh
```

This script will:
- Start LocalStack (if not running)
- Set up DynamoDB table and seed data
- Start the application
- Run performance tests
- Clean up (stop the application)

To keep the application running after tests (for debugging):

```bash
./scripts/local-performance-test.sh --keep-running
```

**Option 2: Manual Steps**

1. **Start LocalStack and the application** (same as regular tests):
   ```bash
   docker compose up -d
   ./scripts/setup-local-dynamodb.sh
   export AWS_ENDPOINT_URL=http://localhost:4566
   export AWS_ACCESS_KEY_ID=test
   export AWS_SECRET_ACCESS_KEY=test
   export AWS_REGION=us-east-1
   export API_KEY=default-test-key
   ./gradlew :country-service-bootstrap:bootRun
   ```

2. **Run performance tests:**
   ```bash
   ./gradlew :country-service-api-tests:testPerformanceLocal
   ```

#### Against Staging Environment

**Option 1: Automated Script (Recommended)**

Use the convenience script that handles configuration automatically:

```bash
./scripts/test-performance-staging.sh
```

This script will:
- Load configuration from `.env` file (if exists)
- Fetch API Gateway URL from CloudFormation (if available)
- Fetch API key from Lambda function (if available)
- Test connectivity before running tests
- Run performance tests against staging

**Option 2: Manual Configuration**

Set environment variables and run directly:

```bash
export API_TEST_BASE_URL=https://your-api-gateway-url.execute-api.region.amazonaws.com/staging/api/v1
export API_TEST_API_KEY=your-staging-api-key

./gradlew :country-service-api-tests:testPerformanceStaging
```

**Option 3: Using .env File**

Create a `.env` file in the project root:

```bash
API_TEST_BASE_URL=https://your-api-gateway-url.execute-api.region.amazonaws.com/staging/api/v1
API_TEST_API_KEY=your-staging-api-key
```

Then run:
```bash
./scripts/test-performance-staging.sh
```

**Note:** Staging environment tests may include network latency and Lambda cold start time, which is acceptable for the first request. Subsequent requests should meet the <200ms requirement.

### Performance Requirements

Performance thresholds vary by environment:

- **Local Environment:** 200ms per endpoint
  - Direct connection, no network latency
  - Matches PRD requirement: "API responses must be fast (<200ms in normal use, locally or in dev cluster)"
  
- **Staging/Remote Environment:** 1000ms per endpoint
  - Accounts for network latency (20-100ms)
  - API Gateway processing time (10-50ms)
  - Lambda execution time (15-73ms from local tests)
  - Lambda cold starts (first request only, can be 500-2000ms)

- **Test Coverage:** All 8 API endpoints are tested
- **Measurement:** Response time is measured using RestAssured's built-in timing
- **Warm-up Requests:** Tests include warm-up requests to account for cold starts
- **Custom Threshold:** Override via system property: `api.test.performance.max.response.time.ms`

### Performance Test Results

Performance tests log the actual response time for each endpoint with the environment type and threshold used. The threshold is automatically selected based on the environment:

- **Local environment** (http://localhost:8080): 200ms threshold
- **Remote environment** (https://*.execute-api.*.amazonaws.com): 1000ms threshold

The threshold can be overridden via system property: `api.test.performance.max.response.time.ms`

**Note:** Staging environment tests use a 1000ms threshold to account for network latency, API Gateway processing, and Lambda overhead. This is realistic for remote environments while still ensuring good performance.

### Troubleshooting

**Connection Refused Errors:**
If you see `java.net.ConnectException: Connection refused` errors, this means the application is not running. Make sure you have:
1. Started LocalStack: `docker compose up -d` (or `podman compose up -d`)
2. Set up the DynamoDB table: `./scripts/setup-local-dynamodb.sh`
3. Started the application: `./gradlew :country-service-bootstrap:bootRun` (in a separate terminal)
4. Verified the application is running on `http://localhost:8080`

The application must be running **before** executing the performance tests.

## Integration with CI/CD

These tests can be integrated into your CI/CD pipeline to run against staging after deployment. See `.github/workflows/deploy.yml` for an example.

## Notes

- Tests are tagged with `@Tag("api")` to distinguish them from other test types
- Tests use unique country codes generated from timestamps to avoid conflicts
- Tests handle eventual consistency using Awaitility where needed
- This module has **zero dependencies** on other application modules

