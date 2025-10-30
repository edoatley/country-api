# ADR 0001: Adopt Hexagonal Architecture (Ports & Adapters)

- Status: Accepted
- Date: 2025-10-30
- Owners: Country Reference Service Team
- Tags: architecture, layering, modularity

## Context
We need a clean separation between domain logic and infrastructure (web, database, cloud runtimes) to support long-term maintainability, easy testing, and future adapter changes (e.g., replacing DynamoDB, adding gRPC, or Lambda handlers). The README mandates hexagonal architecture and modular boundaries with enforcement via ArchUnit.

## Decision
Adopt Hexagonal Architecture (Ports & Adapters) with a multi-module structure:
- `country-service-domain`: Immutable domain models and business rules. No external deps.
- `country-service-application`: Use cases and ports (interfaces). Depends only on domain.
- `country-service-adapters`: Infrastructure adapters (REST, DynamoDB repository, Lambda). Depends on application.
- `country-service-bootstrap`: Wire dependencies and provide runnable entry points.

## Rationale
- Enables technology independence for persistence and presentation layers.
- Facilitates high testability: domain and application layers can be tested without infra.
- Reduces coupling and prevents architectural drift with ArchUnit tests.
- Eases future extensibility (new adapters, migrations) with minimal core changes.

## Consequences
- Positive:
  - Clear dependency direction (outer → inner) improves maintainability.
  - Mockable boundaries enable fast unit and integration tests.
  - Easier onboarding due to explicit module responsibilities.
- Negative/Costs:
  - More modules and indirection vs a monolithic module.
  - Requires discipline and enforcement tooling (ArchUnit) to keep boundaries intact.

## Alternatives Considered
- Layered MVC within a single module
  - Simpler setup but risks leakage of infra concerns into domain and harder migrations.
- Clean Architecture (variation)
  - Similar principles; chosen Hexagonal due to emphasis on ports/adapters vocabulary already used in org.

## Implementation Notes
- Enforce with ArchUnit rules per module.
- Prefer constructor injection and explicit port interfaces.
- Avoid framework annotations inside domain and application modules.
- Keep DTOs in adapters; map to/from domain types at boundaries.

## References
- README requirements in this repo
- PRD: Product Requirements Breakdown (PRODUCT_REQUIREMENTS.md)
