# Product Requirements Breakdown (PRD)

## 1. Vision & Objective
Build a scalable, versioned microservice API to serve as the single source of truth for country information, supporting full change/auditing history and a modern, testable local development workflow.

---

## 2. High-Level Features
- Expose a RESTful API for CRUD (Create, Read, Update, Delete) and search operations on country data, strictly adhering to ISO 3166 codes as identifiers.
- All country data changes must be versioned and auditable.
- Logical (not physical) deletion of records.
- Full history retrieval for any country.
- Technology-agnostic, clean Hexagonal architecture to decouple domain logic from infrastructure.
- Full local development with LocalStack, including automated tests and architectural validation.

---

## 3. Functional Requirements

### 3.1. API Endpoints (See openapi.yml for HTTP details)
#### 3.1.1. List all Countries (Paginated)
- **Path:** `GET /countries?limit={int}&offset={int}`
  - Returns: Paginated list of *latest* country records.
  - Query Params:
    - `limit`: (integer, 1–100, default: 20)
    - `offset`: (integer, ≥0, default: 0)
- **Response:** Array of countries (see Schema), sorted by name or code as per documentation.

#### 3.1.2. Create a Country
- **Path:** `POST /countries`
  - Requires: Unique combination (alpha2, alpha3, numeric codes)
  - Body: JSON matching `CountryInput` schema (name, codes)
- **Response:** Created country record with system fields.

#### 3.1.3. Get Country by 2-letter Code
- **Path:** `GET /countries/code/{alpha2Code}`
  - Params: ISO 3166-1 alpha-2 code (e.g., "GB")
- **Response:** Latest version of country or 404 if not found.

#### 3.1.4. Update a Country by 2-letter Code
- **Path:** `PUT /countries/code/{alpha2Code}`
  - Body: JSON as per `CountryInput`
  - Effect: Creates a new version, preserves previous history.
- **Response:** Updated country record.

#### 3.1.5. Logically Delete a Country
- **Path:** `DELETE /countries/code/{alpha2Code}`
  - Marks country as deleted (`isDeleted = true`), doesn’t remove from storage or history.

#### 3.1.6. Get Country by 3-letter Code
- **Path:** `GET /countries/code3/{alpha3Code}`

#### 3.1.7. Get Country by Numeric Code
- **Path:** `GET /countries/number/{numericCode}`

#### 3.1.8. Get Country Version History
- **Path:** `GET /countries/code/{alpha2Code}/history`
  - Returns: Array of ALL versions for given country, newest first.

#### 3.1.9. Error & Security Handling
- API Key authentication via `X-API-KEY`.
- Standardized 400/401/404/409/500 JSON error responses.
- Example error payloads provided in openapi.yml.

---

## 4. Data Model

### 4.1. Entities

#### 4.1.1. Country (System Record)
- Fields:
  - `name` (string)
  - `alpha2Code` (string, pattern `[A-Z]{2}`)
  - `alpha3Code` (string, pattern `[A-Z]{3}`)
  - `numericCode` (string, pattern `[0-9]{3}`)
  - `createDate` (UTC datetime; system-generated)
  - `expiryDate` (UTC datetime or null, system-managed)
  - `isDeleted` (boolean, flag for logical deletion)
- Constraints:
  - Unique index/enforcement on the tuple (alpha2Code, alpha3Code, numericCode) for latest records.
  - Previous versions retain all above fields plus correct history linkage/expiry.

#### 4.1.2. CountryInput (For Create/Update)
- Same as Country but without audit/system fields (dates, isDeleted).

#### 4.1.3. Example Data
- Use `countries_iso3166b.csv` for initial population/testing and for guide documentation examples.

---

## 5. Architectural Requirements

### 5.1. Hexagonal Architecture ("Ports & Adapters")
- **Domain/Core:** POJO/Record for Country, strictly independent and dependency-free.
- **Application:** Use case/port interfaces for country operations. Depends only on Domain.
- **Adapters (Infrastructure):** REST controllers, AWS Lambda handlers, DynamoDB repositories. Only depend on Application.
- **Bootstrap:** Central wiring/DI.

### 5.2. Testing and Enforcement
- Enforce modular boundaries with ArchUnit.
- Use JUnit5 for all logic and adapter testing.
- Adapters must be mockable/testable (Decouple from real AWS for local dev/test).

---

## 6. Persistence

- Single-table DynamoDB design.
- Each insert/update creates a new item/version (write-once, never overwrite).
  - Use a composite key: PartitionKey = `alpha2Code`, SortKey = `createDate` or equivalent.
  - Logical deletes set `isDeleted=true`, but items remain and can be ‘resurrected’ by a new version.

---

## 7. Security

- API requests must supply a valid `X-API-KEY` header.
- No unauthenticated access (unless explicitly allowed for some endpoints, e.g., for user guide/read-only).
- All error responses must be JSON and provide human-readable messages.

---

## 8. User and Developer Documentation
- **User Guide must clearly explain:**
  - Each endpoint’s parameters, response schema, constraints, and example requests/responses.
  - Meaning and use of versioning/history; logical delete.
  - How to use with API Key (headers example).
  - Example calls (populated from countries_iso3166b.csv).
  - Error responses, troubleshooting.
- **Developer Guide must include:**
  - Architectural overview and rationale (dependency boundaries, Hexagonal pattern).
  - How to run/test locally with LocalStack and Testcontainers.
  - How to add new adapters or migrate infrastructure with minimal impact.
  - Example of enforcing architecture with ArchUnit.

---

## 9. Delivery Plan

### 9.1. Milestones (Recommended Order)
1. **Project Structure and Core Domain**
   - Create multi-module Gradle structure as per README.
   - Implement Country record, basic validation, and domain logic.

2. **Application and Ports**
   - Define Use Cases/Services interfaces for CRUD and history.
   - Write application logic with zero knowledge of persistence or transport.

3. **Adapters: Persistence (DynamoDB)**
   - Implement repositories for storing/reading countries (write-once, versioned, logical delete).
   - Add test coverage including history, constraints.

4. **Adapters: RESTful API/ Lambda Handlers**
   - Implement input adapters per openapi.yml.
   - Add security (API Key validation).

5. **Population & Example Data**
   - Populate database with countries from countries_iso3166b.csv.
   - Provide examples in documentation.

6. **Testing and Documentation**
   - Complete ArchUnit tests for architecture.
   - Write and publish User Guide and Developer Guide.
   - Add OpenAPI schema validation tests.

7. **Local Development/CI Setup**
   - Script LocalStack/Testcontainers for reproducible local dev.
   - Ensure build/test can run without AWS connectivity.

---

## 10. Non-Functional Requirements

- All source code must be comprehensively tested (unit + integration).
- All modules strictly enforce dependency direction.
- API responses must be fast (<200ms in normal use, locally or in dev cluster).
- Full auditing/versioning of all changes (“write-once” history).

---

## 11. Open Questions / To-Be-Decided

- Retention policy for old versions (if any).
- Soft deletes: Is ‘undelete’ supported?
- Extended search (by name, code, wildcard) for future versions.
- Field localization/multi-language support (if required for guides).

---

This product requirements breakdown is complete and can serve as a basis for implementation, testing, documentation, and user/developer onboarding.
If you need it in a particular document format (e.g. markdown, Confluence, PDF-ready), or want a breakdown for just the delivery plan or just the API, let me know!

---

## References and Architecture Decision Records
- The rationale and background for all key architectural choices are captured in [capabilities/ADRs/README.md](capabilities/ADRs/README.md) with one record per decision.
- Consult ADRs if you have questions on domain/infrastructure separation, DynamoDB schema, delivery architecture, Lambda/API Gateway, or DTO mapping.
