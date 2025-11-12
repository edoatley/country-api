# OpenAPI Specification Differences

This document tracks differences between the static `openapi.yml` file and the generated OpenAPI specification from the running application.

## Purpose

Before implementing OpenAPI contract validation, we need to ensure the static specification matches what the application actually generates. Any differences need to be resolved by either:
1. Updating `openapi.yml` to match the generated spec, OR
2. Adding annotations to the code to match `openapi.yml`

## How to Compare

Run the comparison script:
```bash
# Start the application first
./gradlew :country-service-bootstrap:bootRun

# In another terminal, run the comparison
./scripts/compare-openapi-specs.sh
```

## Differences Found

_This section will be populated after running the comparison script._

### Paths
- **Static spec paths:**
  - (to be filled)

- **Generated spec paths:**
  - (to be filled)

### Schemas
- **Static spec schemas:**
  - (to be filled)

- **Generated spec schemas:**
  - (to be filled)

### Detailed Differences
_Unified diff will be shown here after comparison_

## Resolution Plan

_After differences are identified, document the resolution plan here:_

- [ ] Decision: Update `openapi.yml` OR Update code annotations
- [ ] List of specific changes needed
- [ ] Test plan to verify resolution

