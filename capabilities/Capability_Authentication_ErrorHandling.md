# Capability: API Key Authentication & Error Handling

_Architectural and implementation details for authentication (including Lambda/API Gateway particulars) are further captured and tracked in [ADRs/README.md](ADRs/README.md), specifically [ADR_0003_Lambda_APIGateway_Auth.md](ADRs/ADR_0003_Lambda_APIGateway_Auth.md)._

## Overview
Secure the Country Reference Service API with API key (header-based) authentication and provide robust, standardized error handling as defined in openapi.yml. Note: When deploying as a Lambda behind AWS API Gateway, some header normalization and mapping may be required to ensure `X-API-KEY` is correctly extracted (see [ADR_0003_Lambda_APIGateway_Auth.md](ADRs/ADR_0003_Lambda_APIGateway_Auth.md)).

## Status (Sprint 4 updates)
- ✅ `ApiKeyAuthenticationFilter` implemented as Spring servlet filter
- ✅ API key validated against environment variable (`api.key` property)
- ✅ Global exception handler (`GlobalExceptionHandler`) maps exceptions to OpenAPI-compliant error responses
- ✅ All endpoints protected by authentication filter

## Tasks Breakdown

### Sprint 4: API Key Authentication Middleware (Done)
- Spring servlet filter extracts `X-API-KEY` from all requests (Done)
- Validates key against environment property (Done)
- Returns 401 Unauthorized with JSON error on missing/invalid key (Done)

### Sprint 4: Centralized Error Handling & Response Mapping (Done)
- Spring `@RestControllerAdvice` global exception handler (Done)
- Maps `NoSuchElementException` → 404 Not Found (Done)
- Maps `IllegalArgumentException` → 400 Bad Request (Done)
- Maps generic exceptions → 500 Internal Server Error (Done)
- All error responses conform to OpenAPI format: `{ timestamp, status, error, message, path }` (Done)

### Sprint 3: Documentation & Edge Cases
- Document expected error payloads and scenarios for the user guide
- Verify edge cases: missing route, bad method, payload parse failure, etc.
- Ensure error responses are not leaking internal details

---

## Acceptance Criteria
- ✅ All endpoints strictly enforce API key checks (Done)
- ✅ Errors always result in correct, contract-compliant JSON responses (Done)
- ✅ Error responses match OpenAPI schema format (Done)

---

## Completion
This capability is considered delivered when API is fully secured by key authentication, all error scenarios are predictable and match the OpenAPI schema, and both code and docs support maintenance and onboarding.
