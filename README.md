# Country Reference Service

> **Note**: The Gradle wrapper (`./gradlew`) is included in the repo so all builds and runs (locally and in CI) should use this script—no need to install Gradle manually.

## Sprint Zero: Project Scaffold and Local Development Setup

This project adopts Hexagonal Architecture, breaking the codebase into four independent modules to ensure testability and maintainability.

### Directory Structure
- `country-service-domain` – Pure domain model (no dependencies).
- `country-service-application` – Application use cases and ports (depends on domain).
- `country-service-adapters` – Infrastructure (e.g., REST, persistence) (depends on application).
- `country-service-bootstrap` – Application entry point and wiring (depends on adapters).

### Building
- `./gradlew clean build` – Compile and run checks on all modules (Java 21 required).

### Running Hello World
```bash
./gradlew :country-service-bootstrap:run
```
Expected output:
```
Hello from Country Reference Service (Sprint Zero)!
```

### Local Development: Planning
- LocalStack/Docker Compose planned for emulating AWS services (DynamoDB, Lambda, API Gateway) for true local dev.
- See [capabilities/Developer_Onboarding_Checklist.md](capabilities/Developer_Onboarding_Checklist.md) for steps; this will be updated as infra scripts are added in the next sprint.

### CI/CD
- A GitHub Actions workflow will be set up to run build/test for all PRs (see [Release and Deployment Guide](capabilities/Release_and_Deployment_Guide.md)).

### Documentation
- Planning docs, product requirements, architectural decisions, and onboarding guides can be found in the `capabilities/` folder.

For future updates: This README should be kept aligned with code and onboarding as project structure and setup evolves.
