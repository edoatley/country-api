# Capability: Documentation & User Guide

## Overview
Deliver rich, accurate, and example-driven documentation for both API consumers and developers. This covers the user guide, API examples (including from countries_iso3166b.csv), developer onboarding, and architectural rationale, ensuring new team members or integrators can succeed without deep handover.

## Status (Sprint 11 updates)
- ✅ User API Guide created with comprehensive endpoint documentation
- ✅ Developer Guide & Architecture Documentation created
- ✅ OpenAPI/Swagger UI integrated with Spring Boot
- ✅ Integration samples added (curl, httpie, Postman, JavaScript, Python, Java)

## Tasks Breakdown

### Sprint 11: User API Guide (Done)
- ✅ For each endpoint, provided:
  - Path, method summary, description, parameters, and expected responses
  - Example requests and responses using real data from countries_iso3166b.csv
  - Clear explanation of versioning/history and logical delete behavior
  - Authentication and error handling usage with request/response samples
- ✅ Organized into quickstart, reference, and troubleshooting sections
- ✅ Created `docs/USER_API_GUIDE.md` with comprehensive API documentation

### Sprint 11: Developer Guide & Architecture Docs (Done)
- ✅ Documented the rationale for the hexagonal architecture in the project
- ✅ Provided onboarding, setup, and run/test instructions (LocalStack, Testcontainers, etc.)
- ✅ Showed how to add new features, adapters, or migrate infrastructure
- ✅ Included sections on architectural testing (ArchUnit), CI practices, and contributing guidelines
- ✅ Created `docs/DEVELOPER_GUIDE.md` with comprehensive developer documentation

### Sprint 11: OpenAPI Specification and Integration Samples (Done)
- ✅ Integrated OpenAPI/Swagger UI with Spring Boot using SpringDoc OpenAPI
- ✅ Configured Swagger UI at `/swagger-ui.html` with API key authentication
- ✅ Added samples showing how to call the API from common languages/tools (curl, httpie, Postman, JavaScript, Python, Java)
- ✅ Created `docs/INTEGRATION_SAMPLES.md` with comprehensive integration examples

---

## Acceptance Criteria
- All API endpoints are covered by actionable documentation and examples
- Guides are validated against running code and sample data
- Developer onboarding is smooth and up-to-date with local/CI environment
- Architecture section aligns with actual codebase enforcement

---

## Completion
This capability is complete when documentation enables independent use and development, and minimizes the need for synchronous handovers or troubleshooting.
