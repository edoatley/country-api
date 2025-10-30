# Release & Deployment Guide (GitHub Actions)

This guide defines how we version, build, test, package, and deploy the Country Reference Service using GitHub Actions. It includes required secrets, environments, and rollback strategies.

## Versioning Strategy
- Semantic Versioning: `MAJOR.MINOR.PATCH`
- Release tags: `vX.Y.Z`
- Main branches: `main` (stable), `develop` (integration).
- Release cut: Merge to `main` and tag; artifacts published from tag builds.

## Environments
- `dev`: Ephemeral/testing (LocalStack or test AWS account)
- `staging`: Pre-prod validation
- `prod`: Production

## Required GitHub Secrets
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`
- `REGISTRY_USERNAME`, `REGISTRY_PASSWORD` (if using container registry)
- `X_API_KEY` (for smoke tests)
- Optional: `AWS_ROLE_TO_ASSUME` for OIDC-based auth

## Build Artifacts
- JARs per module; one runnable artifact in `country-service-bootstrap`
- Container image: `ghcr.io/<org>/country-service:<tag>` (or ECR/GCR)

## GitHub Actions Workflows

### 1) CI (on PRs to develop/main)
- Triggers: `pull_request`
- Steps:
  1. Checkout
  2. Setup JDK 21 and Gradle cache
  3. Build & Unit Tests (`gradle build test`)
  4. ArchUnit tests (`gradle test` runs them)
  5. OpenAPI contract check (lint/validation)
  6. Integration tests with LocalStack/Testcontainers
  7. Publish test reports and coverage

### 2) Release (on tag `v*`)
- Triggers: `push: tags: [ "v*" ]`
- Steps:
  1. Checkout
  2. Setup JDK 21
  3. Build & test
  4. Package image and push to registry
  5. Deploy to `staging`
  6. Run smoke tests (health check, sample endpoint calls with `X-API-KEY`)
  7. Manual approval step
  8. Deploy to `prod`

### Sample workflow yaml (excerpt)
```yaml
name: release
on:
  push:
    tags: [ "v*" ]
jobs:
  build-test-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - name: Build & Test
        run: ./gradlew clean build
      - name: Build Image
        run: docker build -t ghcr.io/${{ github.repository }}:${{ github.ref_name }} .
      - name: Login Registry
        run: echo ${{ secrets.REGISTRY_PASSWORD }} | docker login ghcr.io -u ${{ secrets.REGISTRY_USERNAME }} --password-stdin
      - name: Push Image
        run: docker push ghcr.io/${{ github.repository }}:${{ github.ref_name }}
      - name: Deploy Staging
        run: ./scripts/deploy.sh staging ghcr.io/${{ github.repository }}:${{ github.ref_name }}
      - name: Smoke Tests
        run: ./scripts/smoke.sh staging ${{ secrets.X_API_KEY }}
      - name: Manual Approval
        uses: fjogeleit/approve-pullrequest-action@v2
      - name: Deploy Prod
        run: ./scripts/deploy.sh prod ghcr.io/${{ github.repository }}:${{ github.ref_name }}
```

## Deploy Scripts (expected)
- `scripts/deploy.sh <env> <image>`: Creates/updates infra (IaC or kubectl) and rolls out image.
- `scripts/smoke.sh <env> <apiKey>`: Calls `/countries` and a `/countries/code/GB` request; checks 200 and schema basics.

## Rollback Strategy
- Image-based rollback: redeploy previous image tag.
- Infra rollback: use IaC version pin or previous stack template.
- Data: versioned writes; logical deletes are non-destructive, no reversal needed unless a new version is written to “undelete.”

## Environment Variables
- `AWS_ENDPOINT_URL` (for LocalStack or remote)
- `AWS_REGION`
- `X_API_KEY`
- Service specific: timeouts, log level, and feature flags

## Release Checklist
- [ ] All CI checks green
- [ ] OpenAPI spec and examples updated
- [ ] Smoke tests pass in staging
- [ ] Approval recorded
- [ ] Prod deploy completed and monitored

This guide provides a ready path for automated release, with manual control at promotion to production.
