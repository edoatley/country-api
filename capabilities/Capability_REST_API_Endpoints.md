# Capability: REST API Endpoints

## Overview
Develop and expose REST API endpoints for all core country operations according to the OpenAPI specification. This includes request/response payloads as per schema, error handling, security features, and integration with the application core via ports/adapters.

## Status (Sprint 4 updates)
- ✅ Spring Boot REST controllers mapping all OpenAPI endpoints to `CountryApi`
- ✅ Full HTTP binding: GET, POST, PUT, DELETE operations implemented
- ✅ Spring Boot application wiring all components (repository, service, API)
- ✅ All endpoints exposed at `/api/v1/countries` with proper HTTP methods

## Tasks Breakdown

### Sprint 2: REST Controller Scaffold (Done)
- Spring Boot REST controllers for all endpoints (Done)
- Country API facade (Done)

### Sprint 2: Request/Response Models and Validation
- Application-level `CountryInput` model (Done)
- Validation handled at domain/service boundary (Done)

### Sprint 4: Standardized Error Handling (Done)
- Spring `@RestControllerAdvice` global exception handler (Done)
- Error responses match OpenAPI schema format (Done)

### Sprint 4: Security Integration (Done)
- API key authentication filter (`ApiKeyAuthenticationFilter`) (Done)
- All endpoints protected by `X-API-KEY` header validation (Done)
- Returns 401 Unauthorized on missing/invalid keys (Done)

### Sprint 5: OpenAPI Docs and Examples
- Framework-integrated OpenAPI exposure and examples (Planned)

### Sprint 6: Integration Testing
- End-to-end tests with LocalStack/APIGW mapping (Planned)

## Acceptance Criteria
- ✅ API facade aligned to OpenAPI operations (Done)
- ✅ REST endpoints callable via HTTP with proper authentication (Done)
- ✅ Error handling matches OpenAPI spec (Done)
- ✅ Application runs as Spring Boot service on port 8080 (Done)

## Completion
This capability will be complete when endpoints are callable via REST and API Gateway with auth, error handling, and contract verification. Current sprint delivers the core service and adapter scaffolding without framework lock-in.
