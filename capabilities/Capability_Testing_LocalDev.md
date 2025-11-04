# Capability: Testing & Local Development (LocalStack, CI)

## Overview
Set up a first-class local development experience for the project, with automated testing, LocalStack/Testcontainers for AWS emulation, and CI that enforces code, contract, and architecture requirements.

## Status (Sprint 5 updates)
- LocalStack Docker Compose setup completed (Sprint 3)
- Testcontainers integration tests for DynamoDB repository (Sprint 3)
- ArchUnit boundary tests implemented (Sprint 1)
- CI/CD pipeline with GitHub Actions (Sprint 0)
- JUnit5 unit and integration tests across modules (Sprint 1-5)
- Code coverage with JaCoCo (Sprint 5)
- Test result reporting in CI (Sprint 5)
- Dependabot for dependency management (Sprint 5)

## Tasks Breakdown

### Sprint 0: CI/CD Pipeline (Done)
- GitHub Actions workflow for build/test on PRs and main (Done)

### Sprint 5: Enhanced CI/CD Pipeline & Test Quality (Done)
- JaCoCo code coverage reporting (Done)
- Test result publishing in CI (Done)
- Code coverage publishing to Codecov (Done)
- Dependabot configuration for dependency updates (Done)
- Gradle dependency caching for faster builds (Done)
- Code coverage improvements: Expanded test coverage to >80% in adapters and bootstrap modules (Done)
- Test data isolation: Unique identifiers per test with automatic cleanup (Done)
- Awaitility-based async testing replaces Thread.sleep() for reliability (Done)
- Write verification before querying/scanning for data consistency (Done)

### Sprint 1: Architectural Enforcement (Done)
- ArchUnit tests enforcing domain/application/adapters boundaries (Done)

### Sprint 3: Local AWS Emulation (Done)
- LocalStack Docker Compose setup (Done)
- Testcontainers for ephemeral DynamoDB testing (Done)
- Table provisioning helper for tests (Done)
- Local endpoint configuration documented (Done)

### Future Sprints
- OpenAPI contract validation in CI (Planned)
- Lambda emulation/testing (Planned)

---

## Acceptance Criteria
- Local dev environment is reproducible and emulates all AWS dependencies
- All tests run green both locally and in CI pipeline
- Any architectural drift is caught before merge

---

## Completion
This capability is delivered when developers can run fully functional, isolated local stacks, tests are comprehensive and documented, and delivery is contract- and architecture-compliant.
