# Capability: REST API Endpoints

## Overview
Develop and expose REST API endpoints for all core country operations according to the OpenAPI specification. This includes request/response payloads as per schema, error handling, security features, and integration with the application core via ports/adapters.

## Status (Sprint 2 updates)
- Introduced `CountryServiceImpl` in application layer.
- Added plain Java API facade `CountryApi` to keep HTTP transport concerns separate.
- Added minimal `CountryLambdaHandler` to model Lambda dispatch without AWS SDK lock-in.
- Tests in adapters validate facade and handler behavior with in-memory repo.

## Tasks Breakdown

### Sprint 1: REST Controller Scaffold
- Scaffold REST controllers for each endpoint (Planned when framework selected)
- Country API facade (Done)

### Sprint 2: Request/Response Models and Validation
- Application-level `CountryInput` model (Done)
- Validation handled at domain/service boundary (Done)

### Sprint 3: Standardized Error Handling
- Centralized error mapping to be introduced with chosen HTTP framework (Planned)

### Sprint 4: Security Integration
- API key checks per ADR_0003 with API Gateway + Lambda integration (Planned)

### Sprint 5: OpenAPI Docs and Examples
- Framework-integrated OpenAPI exposure and examples (Planned)

### Sprint 6: Integration Testing
- End-to-end tests with LocalStack/APIGW mapping (Planned)

## Acceptance Criteria
- API facade aligned to OpenAPI operations (Done)
- Lambda handler scaffold invokes service via facade (Done)
- Ready to bind to HTTP and API Gateway in the next sprint

## Completion
This capability will be complete when endpoints are callable via REST and API Gateway with auth, error handling, and contract verification. Current sprint delivers the core service and adapter scaffolding without framework lock-in.
