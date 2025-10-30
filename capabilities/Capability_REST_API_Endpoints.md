# Capability: REST API Endpoints

## Overview
Develop and expose REST API endpoints for all core country operations according to the OpenAPI specification. This includes request/response payloads as per schema, error handling, security features, and integration with the application core via ports/adapters.

## Tasks Breakdown

### Sprint 1: REST Controller Scaffold
- Scaffold REST controllers for each endpoint:
  - List countries (GET /countries)
  - Create country (POST /countries)
  - Get by alpha2Code (GET /countries/code/{alpha2Code})
  - Update by alpha2Code (PUT /countries/code/{alpha2Code})
  - Delete by alpha2Code (DELETE /countries/code/{alpha2Code})
  - Get by alpha3Code (GET /countries/code3/{alpha3Code})
  - Get by numericCode (GET /countries/number/{numericCode})
  - Get version history (GET /countries/code/{alpha2Code}/history)
- Ensure controllers are in their own infra package/module.
- Add dependency injection for use case/services port.

### Sprint 2: Request/Response Models and Validation
- Implement DTOs for all request/response schemas (using openapi.yml as contract)
- Add validation logic for input models (e.g., regex patterns on codes, required fields).

### Sprint 3: Standardized Error Handling
- Implement middleware/filter for mapping exceptions to error responses (400/401/404/409/500).
- Ensure all endpoints return JSON error bodies per schema.

### Sprint 4: Security Integration
- Add API key extraction and validation middleware (X-API-KEY header).
- Return 401 Unauthorized on missing/invalid keys.

### Sprint 5: OpenAPI Docs and Examples
- Configure OpenAPI documentation integration (e.g., Swagger UI or alternative).
- Annotate all endpoints, request/response types with doc strings and example payloads.
- Use examples from countries_iso3166b.csv for sample content.

### Sprint 6: Integration Testing
- Ensure all endpoints are covered by integration tests (happy path and failure modes).
- Use mock adapters/services for all I/O.

---

## Acceptance Criteria
- All endpoints callable per openapi.yml spec.
- Security, error handling, and documentation meet functional and non-functional requirements.
- DTOs & contracts match specification exactly; round-trip via tests.

---

## Completion
This capability is done when the API endpoints are callable, enforce contract, validate input, handle errors, document themselves, and can be handed off for external integration.
