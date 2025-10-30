# ADR 0003: Lambda + API Gateway Integration & Authentication

- Status: Proposed
- Date: 2025-10-30
- Owners: Country Reference Service Team
- Tags: aws, lambda, apigateway, authentication

## Context
We are deploying the Country Reference Service as an AWS Lambda, fronted by AWS API Gateway. The service is consumed by clients over HTTP(S). We must secure all API access with an API key (X-API-KEY), in line with requirements, and support both local and production environments. API Gateway can natively handle API keys, but custom extraction may be required inside Lambda for some flows (due to custom header or auth schemes).

## Decision
Integrate the service with AWS API Gateway, leveraging its native API Key support when possible, but providing middleware in the Lambda runtime to extract/validate `X-API-KEY` when in local/dev or in non-AWS environments.
- In AWS: use API Gateway usage plans, require `X-API-KEY` header, and let API Gateway enforce key presence/validity before invoking Lambda.
- In local/test/dev: implement custom header extraction and validation middleware so local Lambda, REST, and tests enforce API key logic identically.
- The core application logic should remain agnostic to auth mechanism; only adapters/middleware should deal with extraction/validation.

## Rationale
- Native API Gateway usage plans simplify production key management and eliminate redundant code.
- Having middleware for dev/test ensures contract is enforced and testable without AWS.
- Keeps core business logic clean and modular.

## Consequences
- Handlers must handle X-API-KEY in both API Gateway format (event + headers) and direct REST/Lambda invocation format.
- Some duplication: need mock validator locally and API Gateway config in prod.
- Tests must cover both paths.

## Alternatives Considered
- AWS Cognito/JWT Auth: overkill for simple service, higher complexity for initial rollout.
- Custom auth everywhere: more control, but loses easy charging, throttling, and offload from API Gateway.
- Minimal/no auth: not compliant with requirements or production security best practices.

## Implementation Notes
- API Gateway mapping template may normalize header name to lowercase for Lambda handler.
- Middleware in adapter checks `X-API-KEY` header (case-insensitive).
- Test harness injects header for all calls.
- Swap-in/out validator implementation via DI, based on environment.
- Usage plan manages per-client quotas/usage in prod.
- Document integration and local-vs-prod differences (see onboarding/checklist).

## References
- [AWS API Gateway - API Key Source](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-api-key-source.html)
- Existing PRD and OpenAPI/README
