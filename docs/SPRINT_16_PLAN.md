# Sprint 16: Logging Refactoring

## Overview
Replace all `System.out.println` and `System.err.println` statements with proper SLF4J logging (or Lambda context logger where appropriate) to improve code quality, enable proper log level control, and integrate with CloudWatch Logs.

## Goals
1. Replace all `System.out.println` statements with SLF4J logging
2. Replace all `System.err.println` statements with SLF4J logging
3. Use Lambda context logger in Lambda handlers for CloudWatch integration
4. Ensure proper log levels (DEBUG, INFO, WARN, ERROR) are used appropriately
5. Add SLF4J dependencies where needed
6. Update documentation with logging best practices

## Current State Analysis

### Production Code Issues
1. **ApiGatewayLambdaHandler.java** (3 instances):
   - Line 44: `System.out.println` for ObjectMapper initialization test
   - Line 46: `System.err.println` for serialization warning
   - Line 49: `System.err.println` for serialization error
   - **Priority:** High - Production code should never use System.out/err

2. **OpenApiConfiguration.java** (1 instance):
   - Line 116: `System.err.println` for schema replacement warning
   - **Priority:** High - Spring configuration should use proper logging

### Test Code Issues
1. **BaseApiTest.java** (6 instances):
   - Lines 59, 61-62, 66, 70-72, 108, 110: Test configuration logging
   - **Priority:** Medium - Test output is useful but should use proper logging

2. **CountryApiTest.java** (8 instances):
   - Lines 44, 54, 105, 109, 265, 269, 328, 332, 586-587: Test debugging output
   - **Priority:** Medium - Helps with test debugging

3. **CountrySerializationTest.java** (2 instances):
   - Lines 46, 75: Test output for serialization verification
   - **Priority:** Low - Test-specific debugging

### Documentation (No Change Needed)
- `docs/INTEGRATION_SAMPLES.md` - Code examples for users (keep as-is)

## Implementation Plan

### Phase 1: Production Code (High Priority)
**Estimated Time:** 2-3 hours

#### Step 1.1: Update ApiGatewayLambdaHandler.java
- Replace `System.out.println` with Lambda context logger
- Replace `System.err.println` with Lambda context logger (ERROR level)
- Use `context.getLogger()` when context is available
- For initialization code (before context), use SLF4J logger

**Example:**
```java
// Before
System.out.println("ObjectMapper initialization test - serialized Country: " + testJson);
System.err.println("WARNING: Country serialization missing 'name' field! JSON: " + testJson);

// After (in handleRequest method)
LambdaLogger logger = context.getLogger();
logger.log("ObjectMapper initialization test - serialized Country: " + testJson);
logger.log("WARNING: Country serialization missing 'name' field! JSON: " + testJson);
```

#### Step 1.2: Update OpenApiConfiguration.java
- Replace `System.err.println` with SLF4J logger
- Use WARN level for warnings

**Example:**
```java
// Before
System.err.println("Warning: Could not replace Country schema with CountryDTO: " + e.getMessage());

// After
private static final Logger log = LoggerFactory.getLogger(OpenApiConfiguration.class);
log.warn("Could not replace Country schema with CountryDTO: {}", e.getMessage(), e);
```

#### Step 1.3: Verify Dependencies
- Check if SLF4J is already available in `country-service-adapters` (likely via Spring Boot)
- Check if SLF4J is already available in `country-service-bootstrap` (likely via Spring Boot)

### Phase 2: Test Code (Medium Priority)
**Estimated Time:** 2-3 hours

#### Step 2.1: Add SLF4J Dependencies to Test Module
- Add SLF4J API to `country-service-api-tests/build.gradle`
- Add SLF4J Simple implementation (or logback-classic if preferred)

```gradle
testImplementation 'org.slf4j:slf4j-api'
testImplementation 'org.slf4j:slf4j-simple'
```

#### Step 2.2: Update BaseApiTest.java
- Replace all `System.out.println` with SLF4J logger (INFO level)
- Replace all `System.err.println` with SLF4J logger (WARN level)
- Add logger instance: `private static final Logger log = LoggerFactory.getLogger(BaseApiTest.class);`

**Example:**
```java
// Before
System.out.println("API Test Configuration:");
System.out.println("  Base URL: " + baseUrl);

// After
private static final Logger log = LoggerFactory.getLogger(BaseApiTest.class);
log.info("API Test Configuration:");
log.info("  Base URL: {}", baseUrl);
```

#### Step 2.3: Update CountryApiTest.java
- Replace all `System.out.println` with SLF4J logger (DEBUG level for verbose output)
- Add logger instance

**Example:**
```java
// Before
System.out.println("  First country in list: " + country);

// After
private static final Logger log = LoggerFactory.getLogger(CountryApiTest.class);
log.debug("  First country in list: {}", country);
```

#### Step 2.4: Update CountrySerializationTest.java
- Replace all `System.out.println` with SLF4J logger (DEBUG level)
- Add logger instance

### Phase 3: Testing & Validation
**Estimated Time:** 2-3 hours

#### Step 3.1: Local Testing
1. Run all unit tests to ensure logging changes don't break anything
2. Run integration tests to verify test logging works correctly
3. Run API tests to verify test output is still useful
4. Check that log levels are appropriate (DEBUG for verbose, INFO for important, WARN for warnings, ERROR for errors)

#### Step 3.2: AWS Deployment & Log Verification (Critical)
1. **Deploy to staging via deploy.yml workflow:**
   - Create and push a tag (e.g., `v1.0.29`) to trigger deployment
   - Monitor the deployment workflow in GitHub Actions
   - Verify deployment completes successfully

2. **Verify Lambda logs in CloudWatch:**
   - Navigate to CloudWatch Logs in AWS Console
   - Find the Lambda function log group: `/aws/lambda/country-service-lambda-staging`
   - Check recent log streams for:
     - **Initialization logs:** Should see DEBUG level logs from constructor (ObjectMapper initialization test)
     - **Request processing logs:** Should see INFO/DEBUG logs from handleRequest method
     - **Error handling logs:** Test error scenarios and verify ERROR level logs appear
     - **Warning logs:** Verify WARN level logs appear when appropriate

3. **Verify log format and content:**
   - Logs should use proper SLF4J format (not System.out/err)
   - Lambda context logger should integrate with CloudWatch (timestamps, request IDs)
   - Log levels should be appropriate (no DEBUG logs in production unless needed)
   - Verify structured logging (parameterized messages with `{}` placeholders)

4. **Test API endpoints and verify logging:**
   - Make API calls to staging environment
   - Check CloudWatch logs to verify:
     - Request processing logs appear
     - Serialization debug logs (if enabled) appear
     - Error logs appear for invalid requests
     - No System.out/err messages in logs

5. **Document log verification:**
   - Document CloudWatch log group location
   - Document how to access and filter logs
   - Document expected log patterns
   - Add troubleshooting guide for log issues

### Phase 4: Documentation
**Estimated Time:** 30 minutes

1. Update `docs/LOGGING_REFACTOR_PLAN.md` to mark items as complete
2. Add logging best practices to `docs/DEVELOPER_GUIDE.md`:
   - When to use different log levels
   - Lambda logging best practices
   - Test logging guidelines
3. Update `PROGRESS_SUMMARY.md` with Sprint 16 achievements

## Success Criteria

- [ ] All `System.out.println` statements replaced in production code
- [ ] All `System.err.println` statements replaced in production code
- [ ] All `System.out.println` statements replaced in test code
- [ ] All `System.err.println` statements replaced in test code
- [ ] Lambda handlers use context logger for CloudWatch integration
- [ ] SLF4J dependencies added where needed
- [ ] All tests pass with new logging
- [ ] **Deployment to staging via deploy.yml workflow successful**
- [ ] **Lambda logs visible and correct in CloudWatch Logs**
- [ ] **Log format verified (proper SLF4J, no System.out/err)**
- [ ] **Log levels verified (DEBUG, INFO, WARN, ERROR used appropriately)**
- [ ] **API requests generate expected logs in CloudWatch**
- [ ] Documentation updated with logging best practices
- [ ] CloudWatch log access and troubleshooting documented

## Log Level Guidelines

- **DEBUG:** Verbose output useful for debugging (test debugging, initialization details)
- **INFO:** Important information (test configuration, successful operations)
- **WARN:** Warnings that don't prevent operation (schema replacement failures, validation disabled)
- **ERROR:** Errors that indicate problems (serialization failures, initialization errors)

## Notes

1. **Lambda Context Logger:** Lambda handlers should use `context.getLogger()` when available, as it integrates with CloudWatch Logs and provides better observability
2. **Initialization Logging:** For code that runs before Lambda context is available, use SLF4J logger
3. **Test Logging:** Test output can use DEBUG level for verbose output, INFO for important test information
4. **Documentation Examples:** Keep `System.out.println` in documentation examples (`docs/INTEGRATION_SAMPLES.md`) as they are user-facing examples

## Estimated Total Effort

- **Phase 1 (Production Code):** 2-3 hours
- **Phase 2 (Test Code):** 2-3 hours
- **Phase 3 (Testing & Validation):** 2-3 hours
  - Local testing: 1 hour
  - AWS deployment & log verification: 1-2 hours
- **Phase 4 (Documentation):** 30 minutes
- **Total:** 7-9 hours

## Risks & Mitigation

- **Risk:** Logging changes might affect test output visibility
  - **Mitigation:** Use appropriate log levels (DEBUG for verbose, INFO for important)
  - **Mitigation:** Configure SLF4J Simple to show DEBUG level in tests if needed

- **Risk:** Lambda context logger might not be available in all scenarios
  - **Mitigation:** Use SLF4J logger as fallback for initialization code
  - **Mitigation:** Check context availability before using context logger

## CloudWatch Log Verification Steps

### 1. Deploy to Staging
```bash
# Create and push a tag to trigger deployment
git tag v1.0.29 -m "Sprint 16: Logging refactoring"
git push origin v1.0.29
```

### 2. Monitor Deployment
- Check GitHub Actions workflow: `.github/workflows/deploy.yml`
- Verify deployment completes successfully
- Note the Lambda function name from CloudFormation outputs

### 3. Access CloudWatch Logs
1. Navigate to AWS Console → CloudWatch → Log groups
2. Find log group: `/aws/lambda/country-service-lambda-staging`
3. Click on the most recent log stream
4. Review logs for:
   - Initialization logs (from constructor)
   - Request processing logs
   - Error logs (if any)

### 4. Verify Log Format
- ✅ Logs should have timestamps
- ✅ Logs should have request IDs (from Lambda context)
- ✅ Logs should use proper log levels (DEBUG, INFO, WARN, ERROR)
- ✅ No System.out/err messages should appear
- ✅ Parameterized logging should work (`{}` placeholders)

### 5. Test API and Verify Logs
```bash
# Make API calls to staging
curl -H "X-API-KEY: <api-key>" \
  https://<api-gateway-url>/staging/api/v1/countries?limit=1

# Check CloudWatch logs for:
# - Request received logs
# - Serialization logs (if DEBUG enabled)
# - Response logs
```

### 6. Expected Log Patterns
- **Initialization:** `DEBUG - ObjectMapper initialization test - serialized Country: {...}`
- **Request Processing:** `INFO - Request received: GET /api/v1/countries`
- **Errors:** `ERROR - Error processing request: <message>`
- **Warnings:** `WARN - Country serialization missing 'name' field!`

## References

- [SLF4J Documentation](http://www.slf4j.org/manual.html)
- [AWS Lambda Logging Best Practices](https://docs.aws.amazon.com/lambda/latest/dg/java-logging.html)
- [CloudWatch Logs Console](https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups)
- Current logging refactor plan: `docs/LOGGING_REFACTOR_PLAN.md`
- Deployment workflow: `.github/workflows/deploy.yml`

