# Logging and Environment Variable Refactoring Plan

## Overview
Plan to replace `System.out`, `System.err`, and `System.getenv` with SLF4J logging and Spring Environment abstraction in a future sprint.

## Analysis

### 1. System.out.println Usage

#### Test Classes (Should Replace)
**Location:** `country-service-api-tests/src/test/java/com/example/country/api/`
- **BaseApiTest.java** (6 instances)
  - Lines 59, 66, 70-72, 108, 110
  - **Purpose:** Test configuration logging
  - **Replacement:** Use SLF4J logger (`LoggerFactory.getLogger(BaseApiTest.class)`)
  - **Priority:** Medium - Test output is useful but should use proper logging

- **CountryApiTest.java** (8 instances)
  - Lines 44, 54, 105, 109, 265, 269, 328, 332, 586-587
  - **Purpose:** Test debugging output
  - **Replacement:** Use SLF4J logger with DEBUG level
  - **Priority:** Medium - Helps with test debugging

- **CountrySerializationTest.java** (2 instances)
  - Lines 46, 75
  - **Purpose:** Test output for serialization verification
  - **Replacement:** Use SLF4J logger with DEBUG level
  - **Priority:** Low - Test-specific debugging

#### Production Code (Should Replace)
**Location:** `country-service-adapters/src/main/java/com/example/country/adapters/lambda/ApiGatewayLambdaHandler.java`
- Line 44
- **Purpose:** ObjectMapper initialization debugging
- **Replacement:** Use SLF4J logger (or Lambda's `context.getLogger()` for Lambda-specific logging)
- **Priority:** High - Production code should never use System.out
- **Note:** Lambda context logger is available and should be preferred in Lambda handlers

#### Documentation (No Change Needed)
**Location:** `docs/INTEGRATION_SAMPLES.md`
- Lines 488, 507, 536, 565, 585, 605
- **Purpose:** Code examples for users
- **Action:** Keep as-is (these are documentation examples)

---

### 2. System.err.println Usage

#### Production Code (Should Replace)
**Location:** `country-service-adapters/src/main/java/com/example/country/adapters/lambda/ApiGatewayLambdaHandler.java`
- Lines 46, 49
- **Purpose:** Error logging during initialization
- **Replacement:** Use SLF4J logger with ERROR level (or Lambda's `context.getLogger()`)
- **Priority:** High - Production code should never use System.err

**Location:** `country-service-bootstrap/src/main/java/com/example/country/bootstrap/config/OpenApiConfiguration.java`
- Line 115
- **Purpose:** Warning when schema replacement fails
- **Replacement:** Use SLF4J logger with WARN level
- **Priority:** High - Spring configuration should use proper logging

#### Test Classes (Should Replace)
**Location:** `country-service-api-tests/src/test/java/com/example/country/api/BaseApiTest.java`
- Lines 61-62
- **Purpose:** Warning when OpenAPI validation fails to load
- **Replacement:** Use SLF4J logger with WARN level
- **Priority:** Medium - Test warnings should use proper logging

---

### 3. System.getenv Usage

#### Lambda Handlers (Consider Carefully)
**Location:** `country-service-adapters/src/main/java/com/example/country/adapters/lambda/`
- **ApiGatewayLambdaHandler.java** (Line 24)
- **LambdaEntryPoint.java** (Lines 59, 60, 83)
- **LambdaHandlerFactory.java** (Line 40)
- **Purpose:** Reading AWS Lambda environment variables
- **Replacement Options:**
  1. **Keep as-is** - Lambda environment variables are standard AWS pattern, `System.getenv()` is appropriate
  2. **Use AWS Lambda Context** - If available, use context for environment access
  3. **Use Spring Cloud Function** - If migrating to Spring Cloud Function, use Spring Environment
- **Recommendation:** **Keep as-is** - Lambda environment variables are a standard AWS pattern. `System.getenv()` is the idiomatic way to access them in Lambda handlers. However, consider creating a configuration class that wraps environment access for better testability.

#### Test Classes (Should Replace)
**Location:** `country-service-api-tests/src/test/java/com/example/country/api/BaseApiTest.java`
- Lines 40, 44, 52
- **Purpose:** Reading test configuration from environment variables
- **Replacement:** Use Spring's `@Value` annotation or `Environment` bean (if Spring is available in tests)
- **Alternative:** Keep as-is if tests don't use Spring - `System.getenv()` is acceptable in test utilities
- **Priority:** Low - Test utilities can use `System.getenv()` if Spring isn't available

#### Gradle Build Scripts (Keep as-is)
**Location:** `country-service-api-tests/build.gradle`
- Lines 49, 51, 86, 88, 103, 106
- **Purpose:** Reading environment variables in Gradle build configuration
- **Action:** **Keep as-is** - Gradle scripts use `System.getenv()` and `System.getProperty()` which is standard and appropriate
- **Reason:** Build scripts are not application code and don't have access to Spring Environment

---

## Recommendations

### High Priority (Production Code)
1. ✅ Replace `System.out.println` in `ApiGatewayLambdaHandler.java` with Lambda context logger
2. ✅ Replace `System.err.println` in `ApiGatewayLambdaHandler.java` with Lambda context logger
3. ✅ Replace `System.err.println` in `OpenApiConfiguration.java` with SLF4J logger

### Medium Priority (Test Code)
1. Replace `System.out.println` in test classes with SLF4J logger
2. Replace `System.err.println` in test classes with SLF4J logger
3. Consider using Spring Environment in tests if Spring is available

### Low Priority / Keep As-Is
1. **Lambda Environment Variables** - Keep `System.getenv()` in Lambda handlers (standard AWS pattern)
2. **Gradle Build Scripts** - Keep `System.getenv()` in build.gradle (appropriate for build scripts)
3. **Test Utilities** - Can keep `System.getenv()` if Spring isn't available in test module

---

## Implementation Plan

### Phase 1: Production Code
1. Add SLF4J dependency to `country-service-adapters` (if not already present)
2. Replace `System.err.println` in `OpenApiConfiguration.java` with SLF4J logger
3. Replace `System.out/err.println` in `ApiGatewayLambdaHandler.java` with Lambda context logger
4. Consider creating a `LambdaEnvironment` wrapper class for better testability

### Phase 2: Test Code
1. Add SLF4J dependency to `country-service-api-tests` (if not already present)
2. Replace `System.out.println` in `BaseApiTest.java` with SLF4J logger
3. Replace `System.out.println` in `CountryApiTest.java` with SLF4J logger
4. Replace `System.out.println` in `CountrySerializationTest.java` with SLF4J logger
5. Replace `System.err.println` in test classes with SLF4J logger

### Phase 3: Configuration (Optional)
1. Consider adding Spring Environment support to test module if beneficial
2. Create configuration classes for Lambda environment variables (for testability)

---

## Dependencies to Add

### country-service-adapters/build.gradle
```gradle
// SLF4J API (likely already present via Spring Boot)
implementation 'org.slf4j:slf4j-api'
```

### country-service-api-tests/build.gradle
```gradle
// SLF4J API for test logging
testImplementation 'org.slf4j:slf4j-api'
testImplementation 'org.slf4j:slf4j-simple' // or use logback-classic if preferred
```

---

## Example Replacements

### Before (System.out):
```java
System.out.println("API Test Configuration:");
System.out.println("  Base URL: " + baseUrl);
```

### After (SLF4J):
```java
private static final Logger log = LoggerFactory.getLogger(BaseApiTest.class);

log.info("API Test Configuration:");
log.info("  Base URL: {}", baseUrl);
```

### Before (System.err):
```java
System.err.println("Warning: Could not replace Country schema with CountryDTO: " + e.getMessage());
```

### After (SLF4J):
```java
private static final Logger log = LoggerFactory.getLogger(OpenApiConfiguration.class);

log.warn("Could not replace Country schema with CountryDTO: {}", e.getMessage(), e);
```

### Before (Lambda System.out):
```java
System.out.println("ObjectMapper initialization test - serialized Country: " + testJson);
```

### After (Lambda Context Logger):
```java
// In handleRequest method where context is available
context.getLogger().log("ObjectMapper initialization test - serialized Country: " + testJson);
```

---

## Notes

1. **Lambda Context Logger**: Lambda handlers should use `context.getLogger()` when available, as it integrates with CloudWatch Logs
2. **Test Logging**: Test output can use SLF4J with appropriate log levels (DEBUG for verbose output)
3. **Environment Variables**: Lambda environment variables are a standard AWS pattern - consider keeping `System.getenv()` but wrapping in a configuration class for testability
4. **Gradle Scripts**: Keep `System.getenv()` in build scripts - this is standard and appropriate

---

## Estimated Effort

- **Phase 1 (Production Code):** 2-3 hours
- **Phase 2 (Test Code):** 2-3 hours
- **Phase 3 (Configuration - Optional):** 3-4 hours
- **Total:** 7-10 hours

---

## Sprint Recommendation

**Sprint 16: Logging and Configuration Refactoring**
- Replace System.out/err with SLF4J in production code
- Replace System.out/err with SLF4J in test code
- Consider Lambda environment variable wrapper for testability
- Update documentation with logging best practices

