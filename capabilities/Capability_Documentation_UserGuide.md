# Capability: Documentation & User Guide

## Overview
Deliver rich, accurate, and example-driven documentation for both API consumers and developers. This covers the user guide, API examples (including from countries_iso3166b.csv), developer onboarding, and architectural rationale, ensuring new team members or integrators can succeed without deep handover.

## Tasks Breakdown

### Sprint 1: User API Guide
- For each endpoint, provide:
  - Path, method summary, description, parameters, and expected responses
  - Example requests and responses using real data from countries_iso3166b.csv
  - Clear explanation of versioning/history and logical delete behavior
  - Authentication and error handling usage with request/response samples
- Organize into quickstart, reference, and troubleshooting sections

### Sprint 2: Developer Guide & Architecture Docs
- Document the rationale for the hexagonal architecture in the project
- Provide onboarding, setup, and run/test instructions (LocalStack, Testcontainers, etc.)
- Show how to add new features, adapters, or migrate infrastructure
- Include sections on architectural testing (ArchUnit), CI practices, and contributing guidelines

### Sprint 3: OpenAPI Specification and Integration Samples
- Ensure openapi.yml is referenced, maintained, and integrated with code
- Add samples showing how to call the API from common languages/tools (curl, httpie, Postman)
- Set up static docs (Swagger UI or alternative) for API exploration

---

## Acceptance Criteria
- All API endpoints are covered by actionable documentation and examples
- Guides are validated against running code and sample data
- Developer onboarding is smooth and up-to-date with local/CI environment
- Architecture section aligns with actual codebase enforcement

---

## Completion
This capability is complete when documentation enables independent use and development, and minimizes the need for synchronous handovers or troubleshooting.
