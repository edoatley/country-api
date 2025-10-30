# Developer Onboarding Checklist

Use this checklist to go from clone to running, testing, and deploying locally.

## Prerequisites
- JDK 21 (Temurin recommended)
- Gradle (wrapper included)
- Docker (for LocalStack and container builds)
- Git

## Clone & Build
- Clone repo
- `./gradlew clean build`
- Verify unit tests pass

## Local AWS Emulation (LocalStack)
- Start LocalStack (Docker): `docker run -d --name localstack -p 4566:4566 -e SERVICES=dynamodb localstack/localstack`
- Set environment variables (from README):
```bash
export AWS_ENDPOINT_URL='<your-localstack-openshift-route-url>'
export AWS_ACCESS_KEY_ID='test'
export AWS_SECRET_ACCESS_KEY='test'
export AWS_REGION='us-east-1'
```
- Provision DynamoDB table(s) with project script or IaC (if available):
  - Partition key: `alpha2Code`
  - Sort key: `createDate`
  - GSIs for `alpha3Code`, `numericCode` if used

## Run Tests
- Unit tests: `./gradlew test`
- Integration tests (with LocalStack/Testcontainers): `./gradlew integrationTest` (or combined in `build`)
- ArchUnit: included in `test`

## Seed Sample Data
- Use `countries_iso3166b.csv` to populate table locally:
  - Run provided seed script (add `scripts/seed.sh` if not present)
  - Confirm a few records (e.g., `GB`, `US`) exist via CLI or a small repository test

## Run the Service Locally
- If running as a Spring/Quarkus app: `./gradlew :country-service-bootstrap:run`
- Verify logs show successful startup
- Ensure `X-API-KEY` configured (env or config)

## Smoke Test Endpoints
- List countries:
```bash
curl -H "X-API-KEY: $X_API_KEY" "http://localhost:8080/api/v1/countries?limit=5&offset=0"
```
- Get by code:
```bash
curl -H "X-API-KEY: $X_API_KEY" "http://localhost:8080/api/v1/countries/code/GB"
```
- Create/Update sample (use JSON per openapi.yml)

## Common Troubleshooting
- LocalStack endpoint mismatch → ensure `AWS_ENDPOINT_URL` and region are set
- Failing integration tests → ensure Docker is running and port 4566 free
- 401 responses → set `X-API-KEY` correctly and middleware enabled

## Optional: Local Container Image
- Build container: `docker build -t country-service:local .`
- Run container (map env and port):
```bash
docker run -p 8080:8080 \
  -e AWS_ENDPOINT_URL=$AWS_ENDPOINT_URL \
  -e AWS_REGION=$AWS_REGION \
  -e X_API_KEY=$X_API_KEY \
  country-service:local
```

## Next Steps
- Review `capabilities/` docs for architecture, API, persistence, auth/error handling, testing, and release.
- Open a first PR with a small change to validate CI pipeline.
