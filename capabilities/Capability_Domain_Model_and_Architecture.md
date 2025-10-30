# Capability: Domain Model & Architecture

## Overview
Establish the project's core architecture and domain model. This forms the backbone for all subsequent capabilities and ensures clear separation of concerns as per the hexagonal (ports & adapters) pattern.

## Status (Sprint 1 updates)
- Implemented immutable `Country` in `country-service-domain` with validation.
- Added application ports: `CountryRepositoryPort`, `CountryServicePort` and `CountryInput` model.
- Added ArchUnit tests enforcing boundaries between `domain`, `application`, and `adapters`.

## Tasks Breakdown

### Sprint 1: Core Project Structure & Hexagonal Boundaries
- Multi-Module Gradle: domain, application, adapters, bootstrap (Done)
- ArchUnit tests: enforce no inward violations (Done)

### Sprint 1: Domain Model Implementation
- `Country` immutable class with invariants (Done)
- Unit tests for validation and invariants (Done)

### Sprint 1: Application Ports (Interfaces)
- `CountryRepositoryPort` (Done)
- `CountryServicePort` with OpenAPI-aligned methods (Done)

## Acceptance Criteria
- All boundary tests pass (ArchUnit green)
- Domain unit tests green
- Ports available for adapters to implement in next sprints

## References
- ADRs: see `capabilities/ADRs/README.md`
