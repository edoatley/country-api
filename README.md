# Country Reference Service

A microservice API providing country reference data with full change history and versioning. Built using Hexagonal Architecture to ensure clean separation of concerns and technology independence.

## Overview

The Country Reference Service is a RESTful API that serves as the single source of truth for ISO 3166 country information. It provides:

- **Full CRUD Operations**: Create, read, update, and logical delete country records
- **Multiple Lookup Methods**: Query by ISO 3166-1 alpha-2, alpha-3, or numeric codes
- **Version History**: Complete audit trail of all changes with full version history retrieval
- **Logical Deletion**: Soft deletes preserve data and history for compliance
- **API Authentication**: Protected endpoints using API key authentication

## Architecture

This project follows **Hexagonal Architecture (Ports & Adapters)** with clear module boundaries:

- **`country-service-domain`** – Pure domain model with zero external dependencies
- **`country-service-application`** – Business logic and use cases (depends only on domain)
- **`country-service-adapters`** – Infrastructure implementations (REST, DynamoDB, Lambda)
- **`country-service-bootstrap`** – Application wiring and Spring Boot configuration

See [Architecture Decision Records](capabilities/ADRs/README.md) for detailed architectural rationale.

## Quick Start

### Prerequisites

- JDK 21 (Temurin recommended)
- Docker (for LocalStack)
- Git

### Build and Run

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd country-api
   ```

2. **Build the project:**
   ```bash
   ./gradlew clean build
   ```

3. **Start LocalStack** (for local DynamoDB):
   ```bash
   docker-compose up -d
   ```

4. **Set environment variables:**
   ```bash
   export AWS_ENDPOINT_URL=http://localhost:4566
   export AWS_ACCESS_KEY_ID=test
   export AWS_SECRET_ACCESS_KEY=test
   export AWS_REGION=us-east-1
   export API_KEY=default-test-key
   ```

5. **Set up DynamoDB table and seed data** (optional, but recommended for testing):
   ```bash
   ./scripts/setup-local-dynamodb.sh
   ```
   This will create the DynamoDB table in LocalStack and seed it with 249 countries from `countries_iso3166b.csv`.
   
   Alternatively, you can enable seeding on application startup:
   ```bash
   export DATA_SEEDING_ENABLED=true
   ./gradlew :country-service-bootstrap:bootRun
   ```

6. **Run the application:**
   ```bash
   ./gradlew :country-service-bootstrap:bootRun
   ```

7. **Verify it's running:**
   ```bash
   curl -H "X-API-KEY: default-test-key" http://localhost:8080/api/v1/countries
   ```

## API Documentation

The API follows the [OpenAPI 3.0 specification](openapi.yml). Key endpoints:

- `GET /api/v1/countries` – List all countries (paginated)
- `GET /api/v1/countries/code/{alpha2Code}` – Get country by 2-letter code
- `GET /api/v1/countries/code3/{alpha3Code}` – Get country by 3-letter code
- `GET /api/v1/countries/number/{numericCode}` – Get country by numeric code
- `GET /api/v1/countries/code/{alpha2Code}/history` – Get version history
- `POST /api/v1/countries` – Create new country
- `PUT /api/v1/countries/code/{alpha2Code}` – Update country (creates new version)
- `DELETE /api/v1/countries/code/{alpha2Code}` – Logical delete

All endpoints require the `X-API-KEY` header for authentication.

**Interactive API Documentation:**
- **Swagger UI**: Visit `http://localhost:8080/swagger-ui.html` when running locally
- **OpenAPI JSON**: Available at `http://localhost:8080/api-docs`

**Documentation:**
- [User API Guide](docs/USER_API_GUIDE.md) – Comprehensive API documentation with examples
- [Integration Samples](docs/INTEGRATION_SAMPLES.md) – Code samples for various languages and tools
- [Developer Guide](docs/DEVELOPER_GUIDE.md) – Architecture and development documentation
- [OpenAPI Specification](openapi.yml) – Complete API contract

## Local Development

### Running Tests

- **Unit tests:** `./gradlew test`
- **Integration tests:** `./gradlew test` (uses Testcontainers with LocalStack)
- **Architecture tests:** `./gradlew test` (ArchUnit boundary enforcement)
- **Code coverage:** `./gradlew test jacocoRootReport` (generates HTML and XML coverage reports)

### LocalStack Setup

The project uses LocalStack for local AWS service emulation:

```bash
docker-compose up -d    # Start LocalStack
docker-compose down     # Stop LocalStack
```

Integration tests automatically spin up LocalStack containers using Testcontainers.

See [Developer Onboarding Checklist](capabilities/Developer_Onboarding_Checklist.md) for complete setup instructions.

## Project Structure

```
country-api/
├── country-service-domain/          # Domain model (Country record)
├── country-service-application/      # Business logic and ports
├── country-service-adapters/        # REST, DynamoDB, Lambda adapters
├── country-service-bootstrap/        # Spring Boot application
├── capabilities/                     # Documentation and capability breakdowns
│   ├── ADRs/                        # Architecture Decision Records
│   └── ...
├── docker-compose.yml               # LocalStack configuration
└── openapi.yml                      # API specification
```

## Documentation

Comprehensive documentation is organized in the `capabilities/` directory:

- **[Product Requirements Breakdown](PRODUCT_REQUIREMENTS.md)** – Complete PRD with all requirements
- **[Architecture Decision Records](capabilities/ADRs/README.md)** – Architectural choices and rationale
- **[Developer Onboarding Checklist](capabilities/Developer_Onboarding_Checklist.md)** – Step-by-step setup guide
- **[Release & Deployment Guide](capabilities/Release_and_Deployment_Guide.md)** – CI/CD and deployment process
- **[Progress Summary](PROGRESS_SUMMARY.md)** – Sprint-by-sprint implementation status
- **[Glossary & Conventions](capabilities/Glossary_and_Conventions.md)** – Terminology and naming standards

### Capability Breakdowns

- [Domain Model & Architecture](capabilities/Capability_Domain_Model_and_Architecture.md)
- [REST API Endpoints](capabilities/Capability_REST_API_Endpoints.md)
- [DynamoDB Persistence](capabilities/Capability_Persistence_DynamoDB.md)
- [Authentication & Error Handling](capabilities/Capability_Authentication_ErrorHandling.md)
- [Testing & Local Development](capabilities/Capability_Testing_LocalDev.md)
- [Documentation & User Guide](capabilities/Capability_Documentation_UserGuide.md)

## CI/CD

The project uses GitHub Actions for continuous integration and deployment:

- **Automated builds** on every PR and push to main
- **Test execution** including unit, integration, and architecture tests
- **Architectural validation** via ArchUnit to prevent boundary violations
- **Lambda deployment** on release tags (automatic to staging, manual to production)

See `.github/workflows/ci.yml`, `.github/workflows/deploy.yml`, and [Release & Deployment Guide](capabilities/Release_and_Deployment_Guide.md) for details.

## Deployment

The service can be deployed as an AWS Lambda function behind API Gateway.

### Build Lambda Package

```bash
./gradlew :country-service-adapters:buildLambdaPackage
```

This creates a deployment package at:
```
country-service-adapters/build/libs/country-service-lambda-<version>.jar
```

### Deploy via GitHub Actions

1. **Automatic deployment to staging** (on release tag):
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **Manual deployment** (staging or production):
   - Go to Actions → Deploy workflow
   - Click "Run workflow"
   - Select environment (staging or production)
   - Production requires manual approval

See [Lambda Deployment Guide](docs/LAMBDA_DEPLOYMENT.md) for detailed deployment instructions, including:
- Lambda configuration (handler, runtime, environment variables)
- IAM permissions required
- API Gateway setup
- Troubleshooting guide

## Technology Stack

- **Java 21** – Latest LTS version
- **Gradle** – Build tool with multi-module support
- **Spring Boot 3.2** – REST framework
- **AWS SDK v2** – DynamoDB client
- **DynamoDB** – Versioned single-table storage
- **LocalStack** – Local AWS service emulation
- **Testcontainers** – Integration testing
- **ArchUnit** – Architecture enforcement
- **JUnit 5** – Testing framework

## Contributing

1. Create a feature branch following the naming convention: `NN-feature-description`
2. Implement changes with tests
3. Ensure all tests pass: `./gradlew clean build`
4. Update documentation as needed
5. Open a pull request

## License

[Add your license here]

## Support

For questions or issues, please refer to the documentation in `capabilities/` or open an issue.
