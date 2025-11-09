# Capability: REST API Endpoints

## Overview
Develop and expose REST API endpoints for all core country operations according to the OpenAPI specification. This includes request/response payloads as per schema, error handling, security features, and integration with the application core via ports/adapters.

## Status (Sprint 6 updates)
- ✅ Spring Boot REST controllers mapping all OpenAPI endpoints to `CountryApi`
- ✅ Full HTTP binding: GET, POST, PUT, DELETE operations implemented
- ✅ Spring Boot application wiring all components (repository, service, API)
- ✅ All endpoints exposed at `/api/v1/countries` with proper HTTP methods
- ✅ AWS Lambda handler (`ApiGatewayLambdaHandler`) for API Gateway integration
- ✅ Route mapping from API Gateway events to handler actions
- ✅ API key authentication in Lambda handler
- ✅ Response mapping to API Gateway format

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

### Sprint 11: OpenAPI Docs and Examples (Done)
- ✅ Framework-integrated OpenAPI exposure using SpringDoc OpenAPI
- ✅ Swagger UI configured at `/swagger-ui.html` with API key authentication
- ✅ OpenAPI configuration with server URLs and security schemes
- ✅ Integration samples added for multiple languages and tools

### Sprint 6: Lambda/API Gateway Integration (Done)
- AWS Lambda `RequestHandler` implementation (`ApiGatewayLambdaHandler`) (Done)
- API Gateway event parsing and route mapping (`RouteMapper`) (Done)
- API key authentication middleware (`ApiKeyValidator`) (Done)
- Response mapping to API Gateway format (Done)
- Lambda handler factory for dependency wiring (`LambdaHandlerFactory`) (Done)
- Unit tests for all Lambda components (Done)

## Acceptance Criteria
- ✅ API facade aligned to OpenAPI operations (Done)
- ✅ REST endpoints callable via HTTP with proper authentication (Done)
- ✅ Error handling matches OpenAPI spec (Done)
- ✅ Application runs as Spring Boot service on port 8080 (Done)
- ✅ Lambda handler ready for AWS deployment (Done)
- ✅ API Gateway integration complete with route mapping and authentication (Done)

## Completion
This capability will be complete when endpoints are callable via REST and API Gateway with auth, error handling, and contract verification. Current sprint delivers the core service and adapter scaffolding without framework lock-in.
