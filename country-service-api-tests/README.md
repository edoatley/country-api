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

## Integration with CI/CD

These tests can be integrated into your CI/CD pipeline to run against staging after deployment. See `.github/workflows/deploy.yml` for an example.

## Notes

- Tests are tagged with `@Tag("api")` to distinguish them from other test types
- Tests use unique country codes generated from timestamps to avoid conflicts
- Tests handle eventual consistency using Awaitility where needed
- This module has **zero dependencies** on other application modules

