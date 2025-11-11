# Sprint 15: OpenAPI Contract Validation

## Overview
Implement OpenAPI contract validation in CI to ensure the API implementation matches the OpenAPI specification. This will catch breaking changes early and improve API contract compliance.

## Goals
1. Validate API implementation against OpenAPI specification in CI
2. Catch breaking changes before they reach production
3. Ensure API responses match the OpenAPI schema
4. Validate request/response formats and status codes
5. Integrate validation into the CI workflow

## Current State Analysis

### OpenAPI Specification
- **Location**: `openapi.yml` (root directory)
- **Version**: OpenAPI 3.0.3
- **Status**: Static file, manually maintained
- **SpringDoc Integration**: SpringDoc generates OpenAPI from annotations at runtime

### Current Testing
- Unit tests for controllers
- Integration tests with Testcontainers
- API tests against deployed endpoints
- No contract validation against OpenAPI spec

### Gap
- No validation that implementation matches `openapi.yml`
- No validation that generated OpenAPI matches static spec
- Risk of spec drift between implementation and documentation

## Evaluation Criteria

### Validation Approaches

**Option 1: OpenAPI Generator + Contract Tests**
- Use OpenAPI Generator to generate client SDK
- Write contract tests using generated client
- Validate responses match schema

**Option 2: Swagger Codegen + Schema Validation**
- Generate test models from OpenAPI spec
- Validate request/response against generated models
- Use JSON Schema validation

**Option 3: SpringDoc OpenAPI + Spec Comparison**
- Generate OpenAPI from running application
- Compare generated spec with static `openapi.yml`
- Validate differences

**Option 4: RestAssured + OpenAPI Validator**
- Use RestAssured with OpenAPI validation
- Validate requests/responses against spec
- Use `rest-assured-openapi-validator` or similar

## Recommended Approach: Hybrid

**Primary: Option 4 (RestAssured + OpenAPI Validator)**
- Leverage existing RestAssured tests
- Add OpenAPI validation to existing API tests
- Minimal changes to existing test structure

**Secondary: Option 3 (Spec Comparison)**
- Generate OpenAPI from application
- Compare with static spec in CI
- Catch annotation/spec mismatches

## Implementation Plan

### Phase 1: Add OpenAPI Validation to Existing Tests
- [ ] Add OpenAPI validator dependency to `country-service-api-tests`
- [ ] Configure RestAssured to validate against `openapi.yml`
- [ ] Update existing API tests to enable validation
- [ ] Test locally to ensure validation works

### Phase 2: Spec Comparison in CI
- [ ] Add step to generate OpenAPI from running application
- [ ] Compare generated spec with static `openapi.yml`
- [ ] Fail CI if specs don't match
- [ ] Document differences if any

### Phase 3: Enhanced Validation
- [ ] Validate all endpoints are documented
- [ ] Validate all documented endpoints exist
- [ ] Validate request/response schemas match
- [ ] Validate status codes match spec

### Phase 4: Documentation
- [ ] Update CI documentation
- [ ] Document validation process
- [ ] Update capability documentation
- [ ] Add troubleshooting guide

## Tools & Libraries

### RestAssured OpenAPI Validator
- **Library**: `com.atlassian.oai:rest-assured-openapi-validator` or similar
- **Alternative**: `com.github.java-json-tools:json-schema-validator` with OpenAPI schema extraction

### OpenAPI Comparison
- **Tool**: `openapi-diff` or custom script
- **Alternative**: Use OpenAPI Generator to parse and compare

## Success Criteria
- [ ] CI validates API implementation against OpenAPI spec
- [ ] Breaking changes are caught before merge
- [ ] Spec comparison catches annotation mismatches
- [ ] All existing tests pass with validation enabled
- [ ] Documentation updated

## Risks & Mitigation
- **Risk**: Validation may be too strict and fail on minor differences
  - **Mitigation**: Start with warnings, then enable strict mode
- **Risk**: Performance impact on CI
  - **Mitigation**: Run validation only on API test module, not all tests
- **Risk**: False positives from spec differences
  - **Mitigation**: Document expected differences, use allowlist if needed

## References
- [OpenAPI Specification](openapi.yml)
- [RestAssured Documentation](https://rest-assured.io/)
- [SpringDoc OpenAPI](https://springdoc.org/)
- Current API tests: `country-service-api-tests/src/test/java/com/example/country/api/`

