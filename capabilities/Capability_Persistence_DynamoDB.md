# Capability: Persistence with DynamoDB (Versioned)

## Overview
Implement DynamoDB persistence for countries using a single-table, versioned schema. Ensure all operations (create, update, fetch, delete, history) are version-preserving and the repository adapter enforces contracts/constraints from the domain/application layer. All adapter code should be testable/mocked and decoupled from AWS SDK specifics where possible.

## Status (Sprint 3 updates)
- Implemented `DynamoDbCountryRepository` following ADR_0002 single-table design.
- Added LocalStack Docker Compose setup for local development.
- Added Testcontainers integration tests verifying versioning and all query paths.
- Repository supports write-once versioning, logical deletes, and full history retrieval.

## Tasks Breakdown

### Sprint 3: DynamoDB Table Model & Infra Setup (Done)
- Single-table schema with PK/SK and GSIs for alpha3/numeric lookups (Done)
- Docker Compose for LocalStack (Done)
- Table provisioning helper for tests (Done)

### Sprint 3: Repository Implementation (Done)
- `DynamoDbCountryRepository` implements `CountryRepositoryPort` (Done)
- Write-once versioning, latest queries, history, logical delete (Done)
- Integration tests with Testcontainers (Done)

### Future Sprints
- Error handling and edge cases (transactional/idempotency concerns) - Planned
- Production deployment scripts/IaC - Planned

### Sprint 5: Populate with Sample Data (Done)
- CSV parser (`CsvCountryReader`) to read `countries_iso3166b.csv` (Done)
- Data seeding service (`CountryDataSeeder`) (Done)
- Spring Boot command line runner for automatic seeding on startup (Done)
- Table creation helper (`DynamoDbTableHelper`) for production use (Done)
- Spring Boot Actuator health indicator (`DataSeedingHealthIndicator`) for seeding status (Done)
- End-to-end integration test verifying data seeding and API calls (Done)
- Jackson MixIn for Country serialization (Done)
- Tests for CSV parsing (Done)

---

## Acceptance Criteria
- DynamoDB contains schema, indices, and documented table definition
- Adapters can be mocked/tested with local table
- Persistence operations are versioned and non-destructive
- Contracts and failure/error handling match requirements

---

## Completion
This capability is finished when the repository reliably writes/reads countries with versioning, satisfies contract and is locally testable in CI or developer machine.
