# Architecture Decision Records (ADRs) Index

This directory contains key architectural decisions for the Country Reference Service.

- [ADR_0001_Hexagonal_Architecture.md](ADR_0001_Hexagonal_Architecture.md): Adopts Ports & Adapters (Hexagonal) for all service layering and enforcement.
- [ADR_0002_DynamoDB_Versioning.md](ADR_0002_DynamoDB_Versioning.md): Chooses DynamoDB single-table, versioned history for country records.
- [ADR_0003_Lambda_APIGateway_Auth.md](ADR_0003_Lambda_APIGateway_Auth.md): Documents the choice to deploy behind AWS API Gateway and use API Gateway-native API Key auth with Lambda, with supporting middleware for local/dev/test enforcement.
- [ADR_TEMPLATE.md](ADR_TEMPLATE.md): Use this template for future records.
- Future ADRs (planned):
    - CI/CD GitHub Actions & Delivery
    - DTO and Domain Mapping Strategy
