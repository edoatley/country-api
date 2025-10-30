# ADR 0002: DynamoDB Versioned Single-Table Design

- Status: Accepted
- Date: 2025-10-30
- Owners: Country Reference Service Team
- Tags: persistence, dynamodb, versioning, single-table

## Context
The service must maintain a full, auditable change history for country data, with logical deletes and retrieval by multiple keys (alpha2, alpha3, numeric). We need scalable reads of the latest version and efficient access to history. DynamoDB is the chosen database per requirements.

## Decision
Use a DynamoDB single-table design with write-once versioned items.
- Partition Key (PK): `alpha2Code`
- Sort Key (SK): `createDate` (ISO-8601 timestamp), enabling time-ordered history
- Attributes: `name`, `alpha3Code`, `numericCode`, `isDeleted`, `expiryDate`
- GSIs:
  - `GSI-Alpha3`: PK=`alpha3Code`, SK=`createDate` (latest query by alpha3)
  - `GSI-Numeric`: PK=`numericCode`, SK=`createDate` (latest query by numeric)
- Latest version: item with `expiryDate == null` for a given `alpha2Code`
- Updates: create a new item with a fresh `createDate`; set previous active version's `expiryDate`
- Logical delete: create a new version with `isDeleted=true` (retains history)

## Rationale
- Versioning and history queries align naturally with time-ordered SK.
- Single-table keeps access patterns optimized and avoids cross-table transactions.
- GSIs allow alternate key lookups without scans.
- Write-once pattern reduces contention and preserves auditability.

## Consequences
- Positive:
  - Efficient latest and history queries per partition.
  - Non-destructive updates ensure full audit trail.
  - Scales with DynamoDB throughput and partitioning model.
- Risks/Costs:
  - Careful management of previous-version expiry updates (two-write flow).
  - Eventual consistency nuances; must design for idempotency and retries.
  - GSI costs and write amplification.

## Alternatives Considered
- Relational DB (e.g., Postgres) with SCD2-style history tables
  - Strong consistency and joins; higher operational overhead and different scaling model.
- S3 + Athena (append-only logs)
  - Great for analytics; poor for low-latency API reads/writes.
- Multi-table DynamoDB (separate latest/history tables)
  - Simpler queries but data duplication and consistency complexity.

## Implementation Notes
- Repository writes are idempotent (client token or deterministic version key) to handle retries.
- Queries for latest must filter by `expiryDate == null` and `isDeleted == false` for active views.
- Paginated list uses a GSI or a scan with projection limited to active latest; prefer indexed patterns.
- Local dev: use LocalStack and Testcontainers; IaC or scripts to bootstrap tables and GSIs.
- Migrations: additive attribute/index updates; versioning prevents destructive changes.

## References
- PRD and OpenAPI contracts in this repo
- AWS DynamoDB single-table and versioning best practices
