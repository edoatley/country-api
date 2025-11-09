# Developer Guide & Architecture Documentation

## Overview

This guide provides comprehensive documentation for developers working on the Country Reference Service. It covers the project architecture, development setup, testing practices, and how to extend the system.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Project Structure](#project-structure)
3. [Getting Started](#getting-started)
4. [Development Workflow](#development-workflow)
5. [Testing](#testing)
6. [Adding New Features](#adding-new-features)
7. [CI/CD Pipeline](#cicd-pipeline)
8. [Deployment](#deployment)
9. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

### Hexagonal Architecture (Ports & Adapters)

The Country Reference Service follows a **Hexagonal Architecture** pattern, also known as **Ports & Adapters**. This architecture ensures clear separation of concerns and makes the system highly testable and maintainable.

#### Core Principles

1. **Domain Independence**: The domain layer has zero external dependencies
2. **Ports Define Contracts**: Application layer defines interfaces (ports) that adapters implement
3. **Dependency Inversion**: Dependencies point inward toward the domain
4. **Testability**: Each layer can be tested independently

#### Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                    Bootstrap Layer                      │
│  (Spring Boot Application, Dependency Injection)        │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                   Adapters Layer                        │
│  (REST Controllers, Lambda Handlers, DynamoDB Repo)     │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                Application Layer                        │
│  (Use Cases, Service Implementation, Ports/Interfaces)  │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                    Domain Layer                         │
│  (Country Entity, Business Rules, Validation)           │
└─────────────────────────────────────────────────────────┘
```

### Module Structure

The project is organized into four modules:

1. **`country-service-domain`**
   - Contains the core `Country` entity
   - Zero external dependencies
   - Pure business logic and validation

2. **`country-service-application`**
   - Defines ports (interfaces) for use cases
   - Implements application services
   - Depends only on domain

3. **`country-service-adapters`**
   - REST controllers (Spring Boot)
   - Lambda handlers (AWS)
   - DynamoDB repository implementation
   - Depends on application layer

4. **`country-service-bootstrap`**
   - Spring Boot application entry point
   - Dependency injection configuration
   - Application wiring

### Architecture Enforcement

**ArchUnit** tests enforce architectural boundaries:

- Domain cannot depend on application/adapters
- Application cannot depend on adapters
- All boundary violations are caught at build time

See `ArchitectureTest` classes in each module for enforcement rules.

---

## Project Structure

```
country-api/
├── country-service-domain/          # Domain layer (zero dependencies)
│   └── src/main/java/com/example/country/domain/
│       └── Country.java
│
├── country-service-application/      # Application layer
│   └── src/main/java/com/example/country/application/
│       ├── ports/                    # Interfaces (ports)
│       │   ├── CountryRepositoryPort.java
│       │   └── CountryServicePort.java
│       ├── model/                    # DTOs
│       │   └── CountryInput.java
│       └── CountryServiceImpl.java   # Service implementation
│
├── country-service-adapters/        # Adapters layer
│   └── src/main/java/com/example/country/adapters/
│       ├── web/                      # REST adapters
│       │   └── controller/
│       │       └── CountryController.java
│       ├── lambda/                   # Lambda adapters
│       │   └── ApiGatewayLambdaHandler.java
│       ├── persistence/              # Persistence adapters
│       │   └── DynamoDbCountryRepository.java
│       └── api/                      # API facade
│           └── CountryApi.java
│
├── country-service-bootstrap/        # Bootstrap layer
│   └── src/main/java/com/example/country/bootstrap/
│       ├── CountryServiceApplication.java
│       └── config/
│           ├── CountryServiceConfiguration.java
│           └── OpenApiConfiguration.java
│
├── capabilities/                    # Capability documentation
├── docs/                            # Developer documentation
├── infrastructure/                  # CloudFormation templates
└── openapi.yml                      # OpenAPI specification
```

---

## Getting Started

### Prerequisites

- **Java 21** (LTS)
- **Gradle 8.10+**
- **Docker** (for LocalStack)
- **AWS CLI** (for deployment, optional)

### Local Development Setup

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd country-api
   ```

2. **Start LocalStack:**
   ```bash
   docker-compose up -d
   # OR with Podman:
   podman compose up -d
   ```

3. **Set up DynamoDB table and seed data** (recommended):
   ```bash
   ./scripts/setup-local-dynamodb.sh
   ```
   This creates the DynamoDB table in LocalStack and seeds it with 249 countries.

4. **Build the project:**
   ```bash
   ./gradlew clean build
   ```

5. **Run the application:**
   ```bash
   ./gradlew :country-service-bootstrap:bootRun
   ```

6. **Verify the application:**
   - API: `http://localhost:8080/api/v1/countries`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - Health: `http://localhost:8080/actuator/health`

### Environment Variables

Configure via environment variables or `application.yml`:

- `API_KEY`: API key for authentication (default: `default-test-key`)
- `AWS_ENDPOINT_URL`: LocalStack endpoint (default: `http://localhost:4566`)
- `AWS_REGION`: AWS region (default: `us-east-1`)
- `DATA_SEEDING_ENABLED`: Enable data seeding on startup (default: `false`)

---

## Development Workflow

### Branching Strategy

- **Main branch**: Production-ready code
- **Feature branches**: `{sprint-number}-{feature-description}`
  - Example: `11-documentation-user-guide`

### Running Tests

**Unit tests (fast, no Docker):**
```bash
./gradlew test
```

**Integration tests (slow, requires Docker):**
```bash
./gradlew integrationTest
```

**All tests:**
```bash
./gradlew clean build
```

### Code Quality

- **Architecture tests**: Run automatically with `./gradlew test`
- **Code coverage**: Minimum 80% for adapters and bootstrap modules
- **Linting**: Follow Java coding conventions

---

## Testing

### Test Structure

- **Unit tests**: Test individual components in isolation
- **Integration tests**: Test components with real dependencies (LocalStack, DynamoDB)
- **Architecture tests**: Enforce architectural boundaries (ArchUnit)

### Test Categories

Tests are tagged using JUnit 5 `@Tag`:

- **Unit tests**: No tag (run by default)
- **Integration tests**: `@Tag("integration")` (excluded by default, run with `./gradlew integrationTest`)

### Test Data Isolation

- Each test uses unique identifiers to prevent conflicts
- `@AfterEach` cleanup ensures test data is removed
- Awaitility-based waiting for eventual consistency

### Example: Writing a Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
class MyIntegrationTest {
    
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(...);
    
    @Test
    void testSomething() {
        // Test implementation
    }
}
```

---

## Adding New Features

### Step-by-Step Guide

1. **Define the Domain Model** (if needed)
   - Add to `country-service-domain`
   - Ensure zero dependencies

2. **Define Application Ports**
   - Add interfaces to `country-service-application/ports`
   - Implement services in `country-service-application`

3. **Implement Adapters**
   - REST: Add controllers in `country-service-adapters/web/controller`
   - Lambda: Add handlers in `country-service-adapters/lambda`
   - Persistence: Add repositories in `country-service-adapters/persistence`

4. **Wire Dependencies**
   - Add beans to `CountryServiceConfiguration`
   - Update `OpenApiConfiguration` if adding REST endpoints

5. **Write Tests**
   - Unit tests for each layer
   - Integration tests for end-to-end scenarios
   - Architecture tests to enforce boundaries

6. **Update Documentation**
   - Update `openapi.yml` for API changes
   - Update `USER_API_GUIDE.md` for user-facing changes
   - Update this guide for architectural changes

### Example: Adding a New Endpoint

1. **Add to OpenAPI spec** (`openapi.yml`)
2. **Add to `CountryServicePort`** (application layer)
3. **Implement in `CountryServiceImpl`** (application layer)
4. **Add to `CountryApi`** (adapters layer)
5. **Add to `CountryController`** (adapters layer)
6. **Wire in `CountryServiceConfiguration`** (bootstrap layer)
7. **Write tests** (unit + integration)
8. **Update documentation**

---

## CI/CD Pipeline

### GitHub Actions Workflows

1. **CI Workflow** (`.github/workflows/ci.yml`)
   - Runs on every PR and push to main
   - Builds and runs unit tests
   - Runs integration tests
   - Generates code coverage reports

2. **Deployment Workflow** (`.github/workflows/deploy.yml`)
   - Triggers on tag push (`v*`)
   - Builds Lambda package
   - Deploys to AWS via CloudFormation
   - Runs smoke tests

### CI Pipeline Steps

1. **Checkout code**
2. **Set up Java 21**
3. **Build with Gradle**
4. **Run unit tests** (excludes integration tests)
5. **Run integration tests** (requires Docker)
6. **Generate coverage report**
7. **Publish test results**

---

## Deployment

### AWS Lambda Deployment

The service is deployed as an AWS Lambda function behind API Gateway.

**Deployment Process:**

1. **Tag a release:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **GitHub Actions automatically:**
   - Builds Lambda package
   - Uploads to S3
   - Deploys via CloudFormation
   - Runs smoke tests

**Manual Deployment:**

See `docs/LAMBDA_DEPLOYMENT.md` for manual deployment instructions.

### Infrastructure as Code

All AWS resources are defined in CloudFormation templates:

- `infrastructure/lambda-api-gateway.yaml`: Lambda + API Gateway
- `infrastructure/lambda-execution-roles.yaml`: Lambda execution roles
- `infrastructure/github-actions-roles.yaml`: GitHub Actions deployment roles
- `infrastructure/dynamodb-table.yaml`: DynamoDB table

---

## Troubleshooting

### Common Issues

#### 1. LocalStack Not Starting

**Problem:** `docker-compose up` fails

**Solution:**
- Check Docker is running: `docker ps`
- Check port 4566 is available
- Review LocalStack logs: `docker-compose logs localstack`

#### 2. Integration Tests Failing

**Problem:** Tests fail with `ResourceNotFoundException`

**Solution:**
- Ensure LocalStack container is running
- Check table exists: `aws dynamodb list-tables --endpoint-url http://localhost:4566`
- Verify test data isolation (unique identifiers)

#### 3. API Key Authentication Failing

**Problem:** `401 Unauthorized` responses

**Solution:**
- Check `X-API-KEY` header is included
- Verify API key matches `api.key` property
- Check filter is not excluding the path

#### 4. Build Failures

**Problem:** Gradle build fails

**Solution:**
- Clean build: `./gradlew clean build`
- Check Java version: `java -version` (should be 21)
- Verify Gradle wrapper: `./gradlew --version`

---

## Additional Resources

- **Architecture Decision Records**: `capabilities/ADRs/`
- **User API Guide**: `docs/USER_API_GUIDE.md`
- **Lambda Deployment Guide**: `docs/LAMBDA_DEPLOYMENT.md`
- **OpenAPI Specification**: `openapi.yml`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html` (when running locally)

---

## Contributing

### Code Style

- Follow Java coding conventions
- Use meaningful variable and method names
- Add Javadoc for public APIs
- Keep methods small and focused

### Commit Messages

Use clear, descriptive commit messages:

```
Add OpenAPI/Swagger UI integration

- Add SpringDoc OpenAPI dependency
- Configure OpenAPI with API key security
- Create OpenApiConfiguration class
- Update application.yml with Swagger UI settings
```

### Pull Requests

1. Create a feature branch
2. Make changes with tests
3. Ensure all tests pass
4. Update documentation
5. Create PR with clear description

---

## Support

For questions or issues:
- Review this guide and related documentation
- Check existing issues in the repository
- Contact the development team

