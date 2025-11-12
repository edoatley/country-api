#!/bin/bash

# Debug script to analyze OpenAPI spec differences in detail

BASE_URL="${1:-http://localhost:8080}"
API_DOCS_URL="${BASE_URL}/api-docs"

echo "🔍 Debugging OpenAPI Spec Differences"
echo "======================================"
echo ""

# Fetch generated spec
echo "Fetching generated spec..."
curl -s -f "${API_DOCS_URL}" > /tmp/generated-debug.json || {
    echo "❌ Failed to fetch generated spec"
    exit 1
}

# Convert static to JSON
echo "Converting static spec..."
python3 -c "
import yaml
import json
with open('openapi.yml', 'r') as f:
    yaml_data = yaml.safe_load(f)
    with open('/tmp/static-debug.json', 'w') as out:
        json.dump(yaml_data, out, indent=2, sort_keys=True)
"

# Normalize both
echo "Normalizing specs..."
python3 << 'PYTHON'
import json

def normalize_spec(spec_data):
    if 'servers' in spec_data:
        del spec_data['servers']
    if 'info' in spec_data:
        spec_data['info']['version'] = 'COMPARED'
        spec_data['info']['title'] = 'COMPARED'
        spec_data['info']['description'] = 'COMPARED'
    if 'components' in spec_data:
        if 'examples' in spec_data['components']:
            del spec_data['components']['examples']
        if 'requestBodies' in spec_data['components']:
            del spec_data['components']['requestBodies']
        if 'responses' in spec_data['components']:
            del spec_data['components']['responses']
        if 'schemas' in spec_data['components']:
            for schema_name, schema in spec_data['components']['schemas'].items():
                if 'required' in schema and isinstance(schema['required'], list):
                    schema['required'] = sorted(schema['required'])
                if 'properties' in schema and isinstance(schema['properties'], dict):
                    sorted_props = dict(sorted(schema['properties'].items()))
                    schema['properties'] = sorted_props
                    for prop_name, prop_schema in sorted_props.items():
                        if isinstance(prop_schema, dict) and 'required' in prop_schema:
                            if isinstance(prop_schema['required'], list):
                                prop_schema['required'] = sorted(prop_schema['required'])
                # Remove example fields
                if 'example' in schema:
                    del schema['example']
    return spec_data

with open('/tmp/static-debug.json', 'r') as f:
    static = json.load(f)
normalize_spec(static)
with open('/tmp/static-normalized.json', 'w') as f:
    json.dump(static, f, indent=2, sort_keys=True)

with open('/tmp/generated-debug.json', 'r') as f:
    generated = json.load(f)
normalize_spec(generated)
with open('/tmp/generated-normalized.json', 'w') as f:
    json.dump(generated, f, indent=2, sort_keys=True)

print("✅ Normalization complete")
PYTHON

echo ""
echo "=== Analyzing Differences ==="
echo ""

# Check what's different
echo "1. Paths:"
echo "   Static:"
jq -r '.paths | keys[]' /tmp/static-debug.json | sort | sed 's/^/     - /'
echo "   Generated:"
jq -r '.paths | keys[]' /tmp/generated-debug.json | sort | sed 's/^/     - /'
echo ""

echo "2. Schemas:"
echo "   Static:"
jq -r '.components.schemas | keys[]' /tmp/static-debug.json | sort | sed 's/^/     - /'
echo "   Generated:"
jq -r '.components.schemas | keys[]' /tmp/generated-debug.json | sort | sed 's/^/     - /'
echo ""

echo "3. Country Schema Properties:"
echo "   Static:"
jq -r '.components.schemas.Country.properties | keys[]' /tmp/static-debug.json | sort | sed 's/^/     - /'
echo "   Generated:"
jq -r '.components.schemas.Country.properties | keys[]' /tmp/generated-debug.json | sort | sed 's/^/     - /'
echo ""

echo "4. Country Required Fields:"
echo "   Static:"
jq -r '.components.schemas.Country.required // []' /tmp/static-debug.json | jq -r '.[]' | sort | sed 's/^/     - /'
echo "   Generated:"
jq -r '.components.schemas.Country.required // []' /tmp/generated-debug.json | jq -r '.[]' | sort | sed 's/^/     - /'
echo ""

echo "5. Sample diff (first 30 lines):"
diff -u /tmp/static-normalized.json /tmp/generated-normalized.json | head -30
echo ""

echo "📄 Full normalized specs saved to:"
echo "   /tmp/static-normalized.json"
echo "   /tmp/generated-normalized.json"

