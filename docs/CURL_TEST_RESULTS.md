# cURL Test Results - Staging API

## Summary

All API endpoints consistently return only `{"deleted": false}` instead of the full Country object.

## Test Results

### 1. GET /countries (list)
```bash
curl -H "X-API-KEY: ..." "https://q90fp7p3vb.execute-api.us-east-1.amazonaws.com/staging/api/v1/countries?limit=3"
```

**Response:**
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

**Fields present:** `["deleted"]`  
**Expected fields:** `["name", "alpha2Code", "alpha3Code", "numericCode", "createDate", "isDeleted"]`

### 2. GET /countries/code/{alpha2Code}
```bash
curl -H "X-API-KEY: ..." "https://q90fp7p3vb.execute-api.us-east-1.amazonaws.com/staging/api/v1/countries/code/BZ"
```

**Response:**
```json
{
  "deleted": false
}
```

**Fields present:** `["deleted"]`  
**Expected fields:** `["name", "alpha2Code", "alpha3Code", "numericCode", "createDate", "isDeleted"]`

### 3. POST /countries (create)
```bash
curl -X POST -H "X-API-KEY: ..." -H "Content-Type: application/json" \
  -d '{"name":"Test Country","alpha2Code":"XX","alpha3Code":"XXX","numericCode":"999"}' \
  "https://q90fp7p3vb.execute-api.us-east-1.amazonaws.com/staging/api/v1/countries"
```

**Response:**
```json
{
  "deleted": false
}
```

**Fields present:** `["deleted"]`  
**Expected fields:** `["name", "alpha2Code", "alpha3Code", "numericCode", "createDate", "isDeleted"]`

## Analysis

### What We Know:
1. ✅ **DynamoDB has correct data** - All 6 fields present (name, alpha2Code, alpha3Code, numericCode, createDate, isDeleted)
2. ✅ **Code works locally** - All tests pass against LocalStack
3. ❌ **API returns only `deleted` field** - Missing all other fields
4. ❌ **Property name is `deleted` not `isDeleted`** - Jackson default naming (removes `is` prefix)

### Root Cause:
The `CountryJacksonMixIn` is **NOT being applied** in the Lambda environment. Jackson is using default serialization which:
- Only picks up the `isDeleted()` getter method
- Uses default property naming (removes `is` prefix) → `deleted`
- Ignores all other getter methods (name(), alpha2Code(), etc.)

### Why This Happens:
Without the MixIn, Jackson's default serialization:
- Looks for getter methods
- For boolean getters starting with `is`, it removes the prefix
- Only finds `isDeleted()` method
- Doesn't find other methods because Country class uses method-based accessors (not JavaBean-style getters)

### Test Assertions Are Correct:
The tests expect:
- `name` field ✅ (exists in DynamoDB)
- `alpha2Code` field ✅ (exists in DynamoDB)
- `alpha3Code` field ✅ (exists in DynamoDB)
- `numericCode` field ✅ (exists in DynamoDB)
- `createDate` field ✅ (exists in DynamoDB)
- `isDeleted` field ✅ (exists in DynamoDB)

The API should return all these fields, but it doesn't. **The tests are correct - the API is broken.**

## Next Steps

1. **Verify MixIn is in Lambda JAR** ✅ (Confirmed: `CountryJacksonMixIn.class` is in JAR)
2. **Check Lambda initialization logs** - Look for "ObjectMapper initialization test" output
3. **Verify ObjectMapper configuration** - Ensure MixIn is applied during handler construction
4. **Check for classpath issues** - Ensure MixIn class is accessible at runtime
5. **Consider alternative serialization approach** - If MixIn doesn't work, use custom serializer

