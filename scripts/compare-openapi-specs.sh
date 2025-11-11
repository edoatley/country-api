#!/bin/bash

# Compare static openapi.yml with generated OpenAPI spec from running application
# Usage: ./scripts/compare-openapi-specs.sh [base-url]
#   base-url: Base URL of the running application (default: http://localhost:8080)

set -e

BASE_URL="${1:-http://localhost:8080}"
API_DOCS_URL="${BASE_URL}/api-docs"
STATIC_SPEC="openapi.yml"
TEMP_GENERATED_SPEC="/tmp/generated-openapi-$$.json"
TEMP_STATIC_SPEC="/tmp/static-openapi-$$.json"
TEMP_STATIC_NORM="/tmp/static-openapi-normalized-$$.json"
TEMP_GENERATED_NORM="/tmp/generated-openapi-normalized-$$.json"
DIFF_OUTPUT="/tmp/openapi-diff-$$.txt"

# Cleanup function
cleanup() {
    rm -f "${TEMP_GENERATED_SPEC}" "${TEMP_STATIC_SPEC}" "${TEMP_STATIC_NORM}" "${TEMP_GENERATED_NORM}" "${DIFF_OUTPUT}"
}
trap cleanup EXIT

echo "🔍 Comparing OpenAPI Specifications"
echo "======================================"
echo ""

# Check if application is running
if ! curl -s -f "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
    echo "❌ Error: Application is not running at ${BASE_URL}"
    echo "   Please start the application first:"
    echo "   ./gradlew :country-service-bootstrap:bootRun"
    exit 1
fi

echo "✅ Application is running at ${BASE_URL}"
echo ""

# Fetch generated OpenAPI spec
echo "1. Fetching generated OpenAPI spec from ${API_DOCS_URL}..."
if ! curl -s -f "${API_DOCS_URL}" > "${TEMP_GENERATED_SPEC}" 2>&1; then
    echo "❌ Error: Failed to fetch OpenAPI spec from ${API_DOCS_URL}"
    exit 1
fi
echo "✅ Generated spec fetched"
echo ""

# Convert static YAML to JSON for comparison
echo "2. Converting static openapi.yml to JSON..."
if command -v python3 &> /dev/null; then
    python3 << 'PYTHON_SCRIPT'
import yaml
import json
import sys

try:
    with open('openapi.yml', 'r') as f:
        yaml_data = yaml.safe_load(f)
        with open('/tmp/static-openapi-$$.json', 'w') as out:
            json.dump(yaml_data, out, indent=2, sort_keys=True)
    sys.exit(0)
except Exception as e:
    print(f"Error: {e}", file=sys.stderr)
    sys.exit(1)
PYTHON_SCRIPT
    # Replace $$ with actual PID
    python3 -c "
import yaml
import json
import os

with open('openapi.yml', 'r') as f:
    yaml_data = yaml.safe_load(f)
    with open('${TEMP_STATIC_SPEC}', 'w') as out:
        json.dump(yaml_data, out, indent=2, sort_keys=True)
" 2>/dev/null || {
        echo "❌ Error: Failed to convert YAML to JSON. Please install python3 with PyYAML"
        exit 1
    }
elif command -v yq &> /dev/null; then
    yq eval -o=json "${STATIC_SPEC}" > "${TEMP_STATIC_SPEC}" 2>&1 || {
        echo "❌ Error: Failed to convert YAML to JSON with yq"
        exit 1
    }
else
    echo "❌ Error: Need python3 (with PyYAML) or yq to convert YAML to JSON"
    exit 1
fi
echo "✅ Static spec converted to JSON"
echo ""

# Normalize both specs (remove server URLs, sort keys, etc.)
echo "3. Normalizing specs for comparison..."
if command -v jq &> /dev/null; then
    # Normalize: sort keys, remove servers (they differ by environment), normalize info fields
    jq -S 'del(.servers) | .info.version = "COMPARED" | .info.title = "COMPARED" | .info.description = "COMPARED"' "${TEMP_STATIC_SPEC}" > "${TEMP_STATIC_NORM}" 2>/dev/null
    jq -S 'del(.servers) | .info.version = "COMPARED" | .info.title = "COMPARED" | .info.description = "COMPARED"' "${TEMP_GENERATED_SPEC}" > "${TEMP_GENERATED_NORM}" 2>/dev/null
    
    # Compare normalized specs
    echo "4. Comparing normalized specs..."
    if diff -q "${TEMP_STATIC_NORM}" "${TEMP_GENERATED_NORM}" > /dev/null 2>&1; then
        echo "✅ Specs match (after normalization)"
        echo ""
        echo "Normalization applied:"
        echo "  - Removed server URLs (environment-specific)"
        echo "  - Normalized info fields (title, version, description)"
        echo "  - Sorted keys"
        echo ""
        echo "✅ Comparison complete - specs match!"
        exit 0
    else
        echo "⚠️  SPECS DIFFER! Showing differences..."
        echo ""
        echo "=== Summary of Differences ==="
        echo ""
        
        # Check paths
        echo "📋 Paths comparison:"
        STATIC_PATHS=$(jq -r '.paths | keys[]' "${TEMP_STATIC_SPEC}" 2>/dev/null | sort)
        GENERATED_PATHS=$(jq -r '.paths | keys[]' "${TEMP_GENERATED_SPEC}" 2>/dev/null | sort)
        echo "  Static spec paths:"
        echo "${STATIC_PATHS}" | sed 's/^/    - /'
        echo ""
        echo "  Generated spec paths:"
        echo "${GENERATED_PATHS}" | sed 's/^/    - /'
        echo ""
        
        # Check schemas
        echo "📋 Schemas comparison:"
        STATIC_SCHEMAS=$(jq -r '.components.schemas | keys[]' "${TEMP_STATIC_SPEC}" 2>/dev/null | sort)
        GENERATED_SCHEMAS=$(jq -r '.components.schemas | keys[]' "${TEMP_GENERATED_SPEC}" 2>/dev/null | sort)
        echo "  Static spec schemas:"
        echo "${STATIC_SCHEMAS}" | sed 's/^/    - /'
        echo ""
        echo "  Generated spec schemas:"
        echo "${GENERATED_SCHEMAS}" | sed 's/^/    - /'
        echo ""
        
        # Show detailed diff
        echo "=== Detailed Differences (unified diff) ==="
        diff -u "${TEMP_STATIC_NORM}" "${TEMP_GENERATED_NORM}" | head -100 || true
        echo ""
        echo "(Showing first 100 lines of diff - full diff saved)"
        echo ""
        
        # Save full specs for manual review
        echo "📄 Full specs saved for review:"
        echo "   Static: ${TEMP_STATIC_SPEC}"
        echo "   Generated: ${TEMP_GENERATED_SPEC}"
        echo ""
        echo "⚠️  ⚠️  ⚠️  ACTION REQUIRED ⚠️  ⚠️  ⚠️"
        echo "   Please review the differences above and decide:"
        echo "   1. Update openapi.yml to match generated spec, OR"
        echo "   2. Update code/annotations to match openapi.yml"
        echo ""
        exit 1
    fi
else
    echo "❌ Error: jq is required for detailed comparison"
    echo "   Please install jq: brew install jq (macOS) or apt-get install jq (Linux)"
    exit 1
fi

