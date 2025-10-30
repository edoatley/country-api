# Capability: Testing & Local Development (LocalStack, CI)

## Overview
Set up a first-class local development experience for the project, with automated testing, LocalStack/Testcontainers for AWS emulation, and CI that enforces code, contract, and architecture requirements.

## Tasks Breakdown

### Sprint 1: Local AWS Emulation (LocalStack)
- Add LocalStack Docker support for DynamoDB, Lambda, and other AWS resources
- Document how to spin up/shut down local environment
- Script table/lambda creation as part of local setup
- Configure local endpoint overrides for all infra

### Sprint 2: Automated Testing
- Add JUnit5-based unit and integration test structure to all modules
- Integrate Testcontainers for ephemeral DynamoDB/lambda testing
- Provide fixtures and sample data for test scenarios
- Ensure adapters can run with either production AWS or local stack

### Sprint 3: Continuous Integration Pipeline
- Create GitHub Actions or equivalent pipeline that runs:
  - Lint/format checks
  - All tests (unit, integration)
  - OpenAPI contract validation
- Ensure that PRs fail if architectural integrity or contracts are not met

### Sprint 4: Architectural Enforcement (ArchUnit)
- Add ArchUnit tests to continuously check modular boundaries
- Document common architectural violations and fixes

---

## Acceptance Criteria
- Local dev environment is reproducible and emulates all AWS dependencies
- All tests run green both locally and in CI pipeline
- Any architectural drift is caught before merge

---

## Completion
This capability is delivered when developers can run fully functional, isolated local stacks, tests are comprehensive and documented, and delivery is contract- and architecture-compliant.
