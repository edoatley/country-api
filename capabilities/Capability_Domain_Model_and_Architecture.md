# Capability: Domain Model & Architecture

## Overview
Establish the project's core architecture and domain model. This forms the backbone for all subsequent capabilities and ensures clear separation of concerns as per the hexagonal (ports & adapters) pattern. The deliverables and tasks here are implementation-ready and mapped to user stories/sprints.

## Tasks Breakdown

### Sprint 1: Core Project Structure & Hexagonal Boundaries

#### 1.1. Multi-Module Gradle Project Setup
- Create the directory and build files for each module:
  - `country-service-domain` (Domain model)
  - `country-service-application` (Ports/use cases)
  - `country-service-adapters` (infrastructure: REST, persistence, lambda)
  - `country-service-bootstrap` (Dependency injection, app entry)
- Configure settings.gradle/build.gradle for module dependencies:
  - Domain: no dependencies
  - Application: depends only on Domain
  - Adapters: depends on Application
  - Bootstrap: depends on all

#### 1.2. Enforce Hexagonal Architecture Boundaries
- Add ArchUnit to the test dependencies of each module.
- Write tests for:
  - Domain must have no dependencies outside Java SE and itself
  - Application must only depend on Domain
  - Adapters/Bootstrap can depend on inner layers but not vice versa

---

### Sprint 2: Domain Model Implementation

#### 2.1. Define Country Domain Record
- Implement an immutable `Country` Java Record (or Kotlin data class if preferred) in the domain module with fields:
  - `String name`
  - `String alpha2Code` (validated: `[A-Z]{2}`)
  - `String alpha3Code` (validated: `[A-Z]{3}`)
  - `String numericCode` (validated: `[0-9]{3}`)
  - `Instant createDate`
  - `Instant expiryDate` (nullable for active)
  - `boolean isDeleted`
- Add input validation as needed (factory or validation method).
- Document the record fields and business-invariant requirements.

#### 2.2. Unit Testing Domain Model
- Add tests for all business invariants (e.g., code regexes, required fields).

---

### Sprint 3: Application Ports (Interfaces)

#### 3.1. Define Ports in Application Module
- Define interfaces for:
  - Country Repository (save, update, fetch latest, fetch by code, fetch history, logical delete)
  - Country Use Cases (Create, Read, Update, Delete, History queries)
- Write Javadoc for each interface and method describing: contract, input/output, side effects.

#### 3.2. Domain/Application Tests
- Add mock-based unit tests for each port.
- Ensure use of domain model only and no dependency on infrastructure or frameworks.

---

### Acceptance Criteria
- All boundary/invariant tests pass (ArchUnit, domain, and ports).
- Code fully documented at API and business logic levels.
- Structure is easily extensible for adding REST, persistence, and lambda adapters.

---

## Completion
This capability is done when a developer can run all architecture tests, instantiate the Country domain model, and see a clearly enforced layering between domain, application, adapters, and bootstrap.
