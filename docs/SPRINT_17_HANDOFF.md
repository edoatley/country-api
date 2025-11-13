# Sprint 17 Handoff - New Agent Prompt

## Context for New Agent

You are taking over the Country Reference Service project at the start of Sprint 17. The project is a production-ready microservice API for country data management with full versioning, deployed to AWS Lambda/API Gateway.

## Project Status

**Current State:** Sprint 16 (Logging Refactoring) is complete. All `System.out.println`/`System.err.println` statements have been replaced with proper SLF4J logging, and CloudWatch logs have been verified in staging.

**Sprints Completed:** 0-16 (all merged to main)
- Latest tag: `v1.0.30` (Sprint 16 deployment)
- Current branch: `main` (all work merged)

## Key Documentation Files

1. **`PROGRESS_SUMMARY.md`** - Complete sprint history (0-16) and current state
2. **`PRODUCT_REQUIREMENTS.md`** - Product requirements breakdown
3. **`docs/REQUIREMENTS_GAP_ANALYSIS.md`** - Gap analysis and recommended next steps
4. **`docs/SPRINT_16_PLAN.md`** - Just completed sprint (logging refactoring)
5. **`docs/DEVELOPER_GUIDE.md`** - Architecture and development workflow
6. **`docs/USER_API_GUIDE.md`** - API documentation for users

## Recommended Sprint 17: Performance Validation

Based on `docs/REQUIREMENTS_GAP_ANALYSIS.md`, the recommended next sprint is **Performance Validation**:

### Goal
Validate the PRD requirement: "API responses must be fast (<200ms in normal use, locally or in dev cluster)"

### Tasks
1. Add performance tests to the API test suite (`country-service-api-tests`)
2. Measure response times for all endpoints:
   - GET /countries (list, paginated)
   - GET /countries/code/{alpha2Code}
   - GET /countries/code3/{alpha3Code}
   - GET /countries/number/{numericCode}
   - POST /countries (create)
   - PUT /countries/code/{alpha2Code} (update)
   - DELETE /countries/code/{alpha2Code}
   - GET /countries/code/{alpha2Code}/history
3. Document performance characteristics
4. Add performance assertions (fail if >200ms)
5. Run performance tests against both local and staging environments
6. Document findings and recommendations

### Implementation Approach
- Use RestAssured's built-in timing capabilities or add explicit timing measurements
- Create a new test class: `PerformanceTest.java` in `country-service-api-tests`
- Tag tests with `@Tag("performance")` to run separately
- Add Gradle task `testPerformance` similar to `testStaging`
- Consider using JMeter or Gatling for load testing (optional)

## Alternative Sprint 17 Options

1. **OpenAPI Contract Validation (Phase 3-4)** - Complete remaining validation work from Sprint 15
2. **Documentation Updates** - Add logging best practices, CloudWatch troubleshooting
3. **JUnit 6 Upgrade** - Requires Gradle 9.0+ upgrade first (see `docs/JUNIT_6_UPGRADE_PLAN.md`)

## Project Structure

```
country-api/
├── country-service-domain/          # Zero dependencies, immutable domain model
├── country-service-application/     # Use cases and ports
├── country-service-adapters/        # REST, Lambda, DynamoDB implementations
├── country-service-bootstrap/       # Spring Boot application
├── country-service-api-tests/       # API tests against deployed environments
├── docs/                            # All documentation
├── infrastructure/                  # CloudFormation templates
├── scripts/                         # Helper scripts
└── .github/workflows/               # CI/CD pipelines
```

## Key Technologies

- **Java 21** with Gradle 8.10.2
- **Spring Boot 3.5.7** (for local development)
- **AWS Lambda** (production deployment)
- **DynamoDB** (persistence)
- **JUnit 5.12.2** (testing)
- **RestAssured** (API testing)
- **ArchUnit** (architecture enforcement)

## Development Workflow

1. Create feature branch: `git checkout -b 17-performance-validation`
2. Make changes
3. Run tests: `./gradlew test integrationTest`
4. Commit and push
5. Create PR (CI will run automatically)
6. After merge, tag for deployment: `git tag v1.0.31 -m "Sprint 17: ..."`

## Deployment

- **Staging:** Automatic on tag push (`v*`)
- **Production:** Manual via GitHub Actions workflow_dispatch
- **Workflow:** `.github/workflows/deploy.yml`
- **API Gateway URL:** Staging endpoint available (check CloudFormation outputs or `.env`)

## Testing

- **Unit tests:** `./gradlew test`
- **Integration tests:** `./gradlew integrationTest`
- **API tests (local):** `./gradlew :country-service-api-tests:testLocal`
- **API tests (staging):** `./gradlew :country-service-api-tests:testStaging` (requires env vars)

## Important Notes

- **Architecture:** Hexagonal architecture enforced by ArchUnit tests
- **Domain module:** Must remain dependency-free
- **Logging:** All logging uses SLF4J (production) or Lambda context logger (Lambda handlers)
- **API Key:** Required for all requests (in `.env` file)
- **LocalStack:** Used for local DynamoDB testing

## Getting Started

1. Read `PROGRESS_SUMMARY.md` for full context
2. Review `docs/REQUIREMENTS_GAP_ANALYSIS.md` for Sprint 17 recommendations
3. Check `docs/SPRINT_16_PLAN.md` to understand recent work
4. Create Sprint 17 plan document: `docs/SPRINT_17_PLAN.md`
5. Create branch: `17-performance-validation` (or your chosen sprint name)
6. Start implementation

## Questions to Answer

Before starting Sprint 17, consider:
- Which sprint option do you want to pursue? (Performance validation recommended)
- Do you need any clarification on the architecture or codebase?
- Are there any specific requirements or constraints to consider?

Good luck with Sprint 17! 🚀

