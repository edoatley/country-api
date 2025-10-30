# Capability: API Key Authentication & Error Handling

_Architectural and implementation details for authentication (including Lambda/API Gateway particulars) are further captured and tracked in [ADRs/README.md](ADRs/README.md), specifically [ADR_0003_Lambda_APIGateway_Auth.md](ADRs/ADR_0003_Lambda_APIGateway_Auth.md)._

## Overview
Secure the Country Reference Service API with API key (header-based) authentication and provide robust, standardized error handling as defined in openapi.yml. Note: When deploying as a Lambda behind AWS API Gateway, some header normalization and mapping may be required to ensure `X-API-KEY` is correctly extracted (see [ADR_0003_Lambda_APIGateway_Auth.md](ADRs/ADR_0003_Lambda_APIGateway_Auth.md)).

## Tasks Breakdown

### Sprint 1: API Key Authentication Middleware
- Implement HTTP middleware/filter/interceptor to extract `X-API-KEY` from all incoming requests
- Validate key against an in-memory, env, or pluggable provider
- On missing/invalid key, return 401 Unauthorized, JSON error per openapi.yml
- Write unit and integration tests for valid/invalid/missing key scenarios

### Sprint 2: Centralized Error Handling & Response Mapping
- Design global (controller or app-level) exception handler to catch: BadRequest, Unauthorized, NotFound, Conflict, and generic errors
- Map domain, infra, and validation exceptions to correct status and body
- Ensure all responses conform to: `{ timestamp, status, error, message, path }` format
- Use examples in openapi.yml for payload structure
- Write tests for all error response codes as per contract

### Sprint 3: Documentation & Edge Cases
- Document expected error payloads and scenarios for the user guide
- Verify edge cases: missing route, bad method, payload parse failure, etc.
- Ensure error responses are not leaking internal details

---

## Acceptance Criteria
- All endpoints strictly enforce API key checks
- Errors always result in a correct, contract-compliant JSON response
- End-to-end and edge case error conditions are fully covered by tests and documentation

---

## Completion
This capability is considered delivered when API is fully secured by key authentication, all error scenarios are predictable and match the OpenAPI schema, and both code and docs support maintenance and onboarding.
