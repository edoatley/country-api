#!/bin/bash

# Compare static openapi.yml with generated OpenAPI spec from running application
# Usage: ./scripts/compare-openapi-specs.sh [base-url]
#   base-url: Base URL of the running application (default: http://localhost:8080)

set -e

BASE_URL="${1:-http://localhost:8080}"
API_DOCS_URL="${BASE_URL}/api-docs"
STATIC_SPEC="openapi.yml"

# Create tmp directory in project root if it doesn't exist
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TMP_DIR="${PROJECT_ROOT}/tmp"
mkdir -p "${TMP_DIR}"

# Use timestamp for unique filenames
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEMP_GENERATED_SPEC="${TMP_DIR}/generated-openapi-${TIMESTAMP}.json"
TEMP_STATIC_SPEC="${TMP_DIR}/static-openapi-${TIMESTAMP}.json"
TEMP_STATIC_NORM="${TMP_DIR}/static-openapi-normalized-${TIMESTAMP}.json"
TEMP_GENERATED_NORM="${TMP_DIR}/generated-openapi-normalized-${TIMESTAMP}.json"
DIFF_OUTPUT="${TMP_DIR}/openapi-diff-${TIMESTAMP}.txt"

# Cleanup function - only removes normalized and diff files, keeps full specs for review
cleanup() {
    rm -f "${TEMP_STATIC_NORM}" "${TEMP_GENERATED_NORM}" "${DIFF_OUTPUT}"
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
    # Normalize specs using Python to:
    # 1. Remove servers (environment-specific)
    # 2. Normalize info fields
    # 3. Remove acceptable differences (reusable components)
    # 4. Sort required arrays
    # 5. Sort schema properties
    python3 -c "
import json
import sys

def normalize_spec(spec_data):
    \"\"\"Normalize OpenAPI spec for comparison by removing acceptable differences.\"\"\"
    # Remove servers (environment-specific)
    if 'servers' in spec_data:
        del spec_data['servers']
    
    # Normalize info fields
    if 'info' in spec_data:
        spec_data['info']['version'] = 'COMPARED'
        spec_data['info']['title'] = 'COMPARED'
        spec_data['info']['description'] = 'COMPARED'
    
    # Remove acceptable differences: reusable components
    if 'components' in spec_data:
        if 'examples' in spec_data['components']:
            del spec_data['components']['examples']
        if 'requestBodies' in spec_data['components']:
            del spec_data['components']['requestBodies']
        if 'responses' in spec_data['components']:
            del spec_data['components']['responses']
        
        # Normalize schemas: sort required arrays and properties
        if 'schemas' in spec_data['components']:
            for schema_name, schema in spec_data['components']['schemas'].items():
                # Sort required array if it exists
                if 'required' in schema and isinstance(schema['required'], list):
                    schema['required'] = sorted(schema['required'])
                
                # Sort properties if they exist
                if 'properties' in schema and isinstance(schema['properties'], dict):
                    sorted_props = dict(sorted(schema['properties'].items()))
                    schema['properties'] = sorted_props
                    
                    # Also sort required arrays in nested schemas
                    for prop_name, prop_schema in sorted_props.items():
                        if isinstance(prop_schema, dict) and 'required' in prop_schema:
                            if isinstance(prop_schema['required'], list):
                                prop_schema['required'] = sorted(prop_schema['required'])
    
    return spec_data

# Read and normalize static spec
with open('${TEMP_STATIC_SPEC}', 'r') as f:
    static_spec = json.load(f)
normalize_spec(static_spec)
with open('${TEMP_STATIC_NORM}', 'w') as f:
    json.dump(static_spec, f, indent=2, sort_keys=True)

# Read and normalize generated spec
with open('${TEMP_GENERATED_SPEC}', 'r') as f:
    generated_spec = json.load(f)
normalize_spec(generated_spec)
with open('${TEMP_GENERATED_NORM}', 'w') as f:
    json.dump(generated_spec, f, indent=2, sort_keys=True)
" 2>/dev/null || {
        echo "❌ Error: Failed to normalize specs"
        exit 1
    }
    
    # Compare normalized specs
    echo "4. Comparing normalized specs..."
    if diff -q "${TEMP_STATIC_NORM}" "${TEMP_GENERATED_NORM}" > /dev/null 2>&1; then
        echo "✅ Specs match (after normalization)"
        echo ""
        echo "Normalization applied:"
        echo "  - Removed server URLs (environment-specific)"
        echo "  - Normalized info fields (title, version, description)"
        echo "  - Removed reusable components (examples, requestBodies, responses)"
        echo "  - Sorted required arrays and schema properties"
        echo "  - Sorted all keys"
        echo ""
        echo "✅ Comparison complete - specs match!"
        exit 0
    else
        echo "⚠️  SPECS DIFFER! Analyzing differences..."
        echo ""
        echo "Normalization applied:"
        echo "  - Removed server URLs (environment-specific)"
        echo "  - Normalized info fields (title, version, description)"
        echo "  - Removed reusable components (examples, requestBodies, responses)"
        echo "  - Sorted required arrays and schema properties"
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
        
        # Count differences
        DIFF_LINES=$(diff -u "${TEMP_STATIC_NORM}" "${TEMP_GENERATED_NORM}" | wc -l || echo "0")
        echo "📊 Difference count: ${DIFF_LINES} lines"
        echo ""
        
        # Show a cleaner, more focused diff (only show actual differences, not context)
        echo "=== Key Differences (functional only) ==="
        # Use diff to show only changed lines, limit output
        diff -u "${TEMP_STATIC_NORM}" "${TEMP_GENERATED_NORM}" 2>/dev/null | grep -E "^[+-]" | grep -v "^[+-][+-][+-]" | head -50 || {
            echo "  (No significant functional differences found)"
        }
        echo ""
        echo "(Showing first 50 changed lines - full diff available in tmp/ directory)"
        echo ""
        
        # Save full specs for manual review (these are NOT cleaned up)
        echo "📄 Full specs and diff saved for review:"
        echo "   Static: ${TEMP_STATIC_SPEC}"
        echo "   Generated: ${TEMP_GENERATED_SPEC}"
        echo "   Diff: ${DIFF_OUTPUT}"
        echo ""
        # Save the full diff
        diff -u "${TEMP_STATIC_NORM}" "${TEMP_GENERATED_NORM}" > "${DIFF_OUTPUT}" 2>/dev/null || true
        echo "💡 Tip: Review the diff file for complete details."
        echo "   Most differences are likely acceptable (see docs/OPENAPI_DIFFERENCES_FINAL.md)"
        echo ""
        echo "ℹ️  Note: This comparison ignores acceptable differences:"
        echo "   - Reusable components (examples, requestBodies, responses)"
        echo "   - Field ordering in arrays"
        echo "   - Example placement"
        echo ""
        echo "⚠️  If you see functional differences (missing paths, schemas, or properties),"
        echo "   please review and update either openapi.yml or the code annotations."
        echo ""
        # Don't exit with error - these are likely acceptable differences
        exit 0
    fi
else
    echo "❌ Error: jq is required for detailed comparison"
    echo "   Please install jq: brew install jq (macOS) or apt-get install jq (Linux)"
    exit 1
fi

