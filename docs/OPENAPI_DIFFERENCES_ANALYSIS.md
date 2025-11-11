# OpenAPI Differences Analysis

## Summary

After running the comparison script, the following differences were identified between the static `openapi.yml` and the generated OpenAPI spec:

## Key Differences

### 1. Component Structure
**Static Spec Has:**
- `components.examples` - Reusable examples (e.g., `CountryUK`)
- `components.requestBodies` - Reusable request bodies (e.g., `CountryBody`)
- `components.responses` - Reusable responses (e.g., `BadRequest`, `Unauthorized`, `NotFound`, `InternalServerError`)

**Generated Spec Has:**
- Only `components.schemas` and `components.securitySchemes`
- Request bodies and responses are defined inline in paths (not as reusable components)
- No reusable examples

**Impact:** This is a structural difference but doesn't affect API functionality. The generated spec is valid, just organized differently.

### 2. Error Schema Differences

**Static Spec:**
```yaml
Error:
  properties:
    timestamp:
      type: string
      format: date-time  # ✅ Has format
    path:
      type: string
  required:
    - timestamp
    - status
    - error
    - message
    - path  # ✅ path is required
```

**Generated Spec:**
```json
Error:
  properties:
    timestamp:
      type: string
      # ❌ Missing format: date-time
    path:
      type: string
  required:
    - timestamp
    - status
    - error
    - message
    # ❌ path is NOT in required (but should be)
```

**Fix Needed:** Update `ErrorResponse` class to:
- Add `@Schema(format = "date-time")` to timestamp field
- Ensure `path` is in required fields (may need to adjust annotation)

### 3. Country Schema Issues

**Generated Spec Shows:**
- Only shows `deleted` property (incomplete)
- Missing all other properties (name, alpha2Code, etc.)

**Likely Cause:** The `Country` domain class doesn't have SpringDoc annotations, so SpringDoc can't properly infer the schema. The `CountryJacksonMixIn` is used for serialization but doesn't help with OpenAPI generation.

**Fix Needed:** Add `@Schema` annotations to `Country` class or create a separate DTO with annotations.

### 4. Missing 401 Response Documentation

**Generated Spec:**
- 401 responses show as `null` in paths
- No documentation for unauthorized responses

**Likely Cause:** The `ApiKeyAuthenticationFilter` doesn't have SpringDoc annotations to document the 401 response.

**Fix Needed:** Add `@ApiResponse` annotations to document 401 responses, or configure SpringDoc to include security responses globally.

### 5. Missing Reusable Components

**Static Spec Uses:**
- `$ref: '#/components/requestBodies/CountryBody'`
- `$ref: '#/components/responses/Unauthorized'`
- `$ref: '#/components/examples/CountryUK'`

**Generated Spec:**
- Inline definitions instead of references
- No reusable components

**Impact:** This is acceptable - inline definitions work fine, but reusable components are cleaner and more maintainable.

## Recommended Fixes

### Priority 1: Critical Schema Issues
1. **Fix Error Schema:**
   - Add `format: date-time` to timestamp
   - Ensure `path` is required

2. **Fix Country Schema:**
   - Add proper schema annotations to Country class or create DTO

### Priority 2: Response Documentation
3. **Document 401 Responses:**
   - Add annotations to ApiKeyAuthenticationFilter or configure globally

### Priority 3: Optional Improvements
4. **Reusable Components:**
   - Consider if we need reusable components (may not be necessary if inline works)

## Decision Points

1. **Should we update code to match static spec, or update static spec to match generated?**
   - **Recommendation:** Update code to match static spec (more detailed, better documentation)

2. **Are reusable components necessary?**
   - **Recommendation:** Not critical, but nice to have for consistency. Can be added later if needed.

3. **Should we add annotations to domain classes?**
   - **Recommendation:** Create DTOs with annotations rather than polluting domain classes, OR use a separate OpenAPI configuration to define schemas.

