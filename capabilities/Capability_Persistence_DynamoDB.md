# Capability: Persistence with DynamoDB (Versioned)

## Overview
Implement DynamoDB persistence for countries using a single-table, versioned schema. Ensure all operations (create, update, fetch, delete, history) are version-preserving and the repository adapter enforces contracts/constraints from the domain/application layer. All adapter code should be testable/mocked and decoupled from AWS SDK specifics where possible.

## Tasks Breakdown

### Sprint 1: DynamoDB Table Model & Infra Setup
- Define single-table schema with key structure:
  - Partition key: `alpha2Code`
  - Sort key: version timestamp or `createDate`
- Add additional DynamoDB GSI or indexes if required for alternate key lookups (alpha3, numericCode)
- Define attribute mapping for domain model fields
- Add local table fixture/config for LocalStack and CI testing

### Sprint 2: Repository Implementation
- Implement DynamoDB repository (adapter) to support:
  - Save new country (write-once)
  - Update as new version (never overwrite)
  - Find latest by alpha2Code, alpha3Code, numericCode
  - List paginated countries (active, not isDeleted)
  - Get history (sort descending by createDate)
  - Logical delete (set isDeleted=true as new version)
- Ensure all mappings/transformations preserve domain contracts and invariants.

### Sprint 3: Error & Edge Handling
- Map AWS/DynamoDB exceptions to application errors.
- Ensure transactional or idempotent operations as required (handling race on concurrent updates)
- Unit and integration test repository thoroughly (with LocalStack or Testcontainers).

### Sprint 4: Populate with Sample Data
- Script initial population of table from countries_iso3166b.csv
- Provide test harness or fixtures for guide/demo

---

## Acceptance Criteria
- DynamoDB contains schema, indices, and documented table definition
- Adapters can be mocked/tested with local table
- Persistence operations are versioned and non-destructive
- Contracts and failure/error handling match requirements

---

## Completion
This capability is finished when the repository reliably writes/reads countries with versioning, satisfies contract and is locally testable in CI or developer machine.
