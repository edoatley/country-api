# Staging Test Debugging Summary

## Issue Summary

The API tests pass locally against LocalStack but fail against staging. The API is returning only `{"deleted": false}` instead of the full Country object with all fields.

## Test Results

### ✅ LocalStack Tests (PASS)
- All 11 tests pass when running against LocalStack
- Code works correctly in local environment
- Serialization works correctly with LocalStack

### ❌ Staging Tests (FAIL)
- 4 tests failing:
  1. `GET /api/v1/countries - List all countries (paginated)` - Missing 'name' field
  2. `GET /api/v1/countries/code/{alpha2Code}/history - Get country history` - Missing 'createDate' field
  3. `PUT /api/v1/countries/code/{alpha2Code} - Update a country` - Missing 'name' field
  4. `POST /api/v1/countries - Create a new country` - Missing fields

### ✅ DynamoDB Data (CORRECT)
- **Account**: Streaming profile (727361020121)
- **Table**: Countries
- **Item Count**: 375 items
- **Data Structure**: Correct - all fields present:
  - `name` (String)
  - `alpha2Code` (String)
  - `alpha3Code` (String)
  - `numericCode` (String)
  - `createDate` (String - ISO-8601)
  - `isDeleted` (Boolean)

**Sample DynamoDB Item:**
```json
{
  "name": {"S": "Belize"},
  "alpha2Code": {"S": "BZ"},
  "alpha3Code": {"S": "BLZ"},
  "numericCode": {"S": "084"},
  "createDate": {"S": "2025-11-09T11:19:37.485032Z"},
  "isDeleted": {"BOOL": false}
}
```

### ❌ API Response (BROKEN)
**Actual API Response:**
```json
[
  {
    "deleted": false
  },
  {
    "deleted": false
  }
]
```

**Expected API Response:**
```json
[
  {
    "name": "Belize",
    "alpha2Code": "BZ",
    "alpha3Code": "BLZ",
    "numericCode": "084",
    "createDate": "2025-11-09T11:19:37.485032Z",
    "isDeleted": false
  }
]
```

## Root Cause Analysis

### What We Know:
1. ✅ **Code is correct** - Tests pass locally against LocalStack
2. ✅ **Data is correct** - DynamoDB has all required fields
3. ❌ **Serialization is broken** - Only `{deleted: false}` is returned
4. ❌ **MixIn not applied** - The `CountryJacksonMixIn` is not being applied in Lambda

### The Problem:
The Lambda handler's `ObjectMapper` is not correctly serializing `Country` objects. The MixIn configuration (`CountryJacksonMixIn`) is not being applied, causing Jackson to use default serialization which only picks up the `isDeleted` field (and renames it to `deleted`).

### Why It Works Locally But Not in Lambda:
- **LocalStack**: Uses Spring Boot's `ObjectMapper` which has the MixIn correctly configured
- **Lambda**: Uses a custom `ObjectMapper` in `ApiGatewayLambdaHandler` which may not be applying the MixIn correctly

## Configuration

### AWS Account
- **Profile**: `streaming`
- **Account ID**: `727361020121`
- **Region**: `us-east-1`

### API Gateway
- **URL**: `https://q90fp7p3vb.execute-api.us-east-1.amazonaws.com/staging/api/v1`
- **API Key**: Set in `.env` file

### DynamoDB
- **Table**: `Countries`
- **Status**: `ACTIVE`
- **Item Count**: 375

## Next Steps

1. **Verify MixIn is being applied** - Check Lambda logs to see if the initialization test passes
2. **Compare ObjectMapper configuration** - Ensure Lambda's ObjectMapper matches Spring Boot's configuration
3. **Test serialization directly** - Add a test that serializes a Country object in the Lambda handler
4. **Check classpath** - Ensure `CountryJacksonMixIn` class is in the Lambda JAR

## Commands to Debug

### Check DynamoDB Data:
```bash
AWS_PROFILE=streaming aws dynamodb scan --table-name Countries --region us-east-1 --limit 5
```

### Test API Directly:
```bash
curl -H "X-API-KEY: YOUR_API_KEY" \
  "https://q90fp7p3vb.execute-api.us-east-1.amazonaws.com/staging/api/v1/countries?limit=3"
```

### Run Tests Locally Against Staging:
```bash
export API_TEST_BASE_URL='https://q90fp7p3vb.execute-api.us-east-1.amazonaws.com/staging/api/v1'
export API_TEST_API_KEY='YOUR_API_KEY'
./gradlew :country-service-api-tests:testStaging --no-daemon --info
```

### Run Tests Locally Against LocalStack:
```bash
./gradlew :country-service-api-tests:testLocal --no-daemon
```

