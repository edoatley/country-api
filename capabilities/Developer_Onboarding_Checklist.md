# Developer Onboarding Checklist

Use this checklist to go from clone to running, testing, and deploying locally.

## Prerequisites
- JDK 21 (Temurin recommended)
- Gradle (wrapper included)
- Docker (for LocalStack and container builds)
- Git

## Sprint Zero: Quickstart
1. **Clone the repo:**
   - `git clone ...`
2. **Build the project:**
   - `./gradlew clean build`
3. **Run Hello World app:**
   - `./gradlew :country-service-bootstrap:run`
   - Output should include messages from all 4 sample modules in chain
4. **CI/CD:**
   - GitHub Actions runs CI on PR and main (see status on your PRs)
5. **Docs:**
   - Review the `README.md` for project structure, build/run, and links to detailed design docs under `capabilities/`.

## Local Development (Sprint 3)
1. **Start LocalStack:**
   - `docker-compose up -d`
2. **Set AWS environment variables:**
   ```bash
   export AWS_ENDPOINT_URL=http://localhost:4566
   export AWS_ACCESS_KEY_ID=test
   export AWS_SECRET_ACCESS_KEY=test
   export AWS_REGION=us-east-1
   ```
3. **Run integration tests:**
   - `./gradlew test` (will automatically spin up Testcontainers LocalStack)
4. **Verify DynamoDB repository works:**
   - Tests in `country-service-adapters` validate full persistence layer

## Next Steps (Future Sprints)
- Seed DynamoDB via CSV (planned script)
- Wire up REST/Lambda adapters with actual HTTP framework
- Complete authentication and error handling per OpenAPI spec

## Troubleshooting
- Gradle or Java errors: verify JDK 21 in use (check `java -version`)
- Build issues: try `./gradlew clean build --refresh-dependencies`
- Problems with CI: see logs under GitHub Actions tab
- Next features and fixes should update both this checklist and `README.md` for consistency

## Key Notes
- Always use `./gradlew` for builds and runs (the wrapper is checked in to git)
- Architecture rules (ArchUnit) run with `./gradlew build` and will fail on boundary violations
- If you ever see missing wrapper errors (e.g., missing JAR or script), re-run `gradle wrapper` and commit the missing files
- The CI/CD pipeline (see GitHub Actions) only works with the wrapper
