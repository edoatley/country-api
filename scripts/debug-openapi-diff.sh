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

def resolve_ref(ref_path, spec_data):
    """Resolve a $ref path to the actual object."""
    if not ref_path.startswith('#/'):
        return None
    
    parts = ref_path[2:].split('/')
    current = spec_data
    for part in parts:
        if isinstance(current, dict) and part in current:
            current = current[part]
        else:
            return None
    # Return a deep copy to avoid modifying the original
    import copy
    return copy.deepcopy(current)

def expand_refs_recursive(obj, spec_data, visited=None):
    """Recursively expand all $ref references in the object."""
    if visited is None:
        visited = set()
    
    if isinstance(obj, dict):
        if '$ref' in obj:
            ref_path = obj['$ref']
            if ref_path not in visited:
                visited.add(ref_path)
                resolved = resolve_ref(ref_path, spec_data)
                if resolved:
                    # Expand the resolved object recursively
                    expanded = expand_refs_recursive(resolved, spec_data, visited)
                    # Merge any additional keys from the original (like description, but $ref takes precedence)
                    # Only merge if the expanded object doesn't already have the key
                    if isinstance(expanded, dict):
                        for key, value in obj.items():
                            if key != '$ref' and key not in expanded:
                                expanded[key] = expand_refs_recursive(value, spec_data, visited)
                    return expanded
            # If we've visited this ref before or can't resolve, return as-is but remove $ref
            result = {k: expand_refs_recursive(v, spec_data, visited) for k, v in obj.items() if k != '$ref'}
            return result if result else {}
        
        # Recursively process all values
        return {k: expand_refs_recursive(v, spec_data, visited) for k, v in obj.items()}
    elif isinstance(obj, list):
        return [expand_refs_recursive(item, spec_data, visited) for item in obj]
    else:
        return obj

def normalize_content_type(content_type):
    """Normalize content types - convert */* to application/json."""
    if content_type == '*/*':
        return 'application/json'
    return content_type

def normalize_description(description):
    """Normalize descriptions - remove trailing periods and trim whitespace."""
    if not isinstance(description, str):
        return description
    return description.rstrip('.').strip()

def remove_examples_recursive(obj):
    """Recursively remove all 'examples' keys from the object."""
    if isinstance(obj, dict):
        # Remove examples key if present
        if 'examples' in obj:
            del obj['examples']
        # Remove example key if present (singular)
        if 'example' in obj:
            del obj['example']
        # Recursively process all values
        for value in obj.values():
            remove_examples_recursive(value)
    elif isinstance(obj, list):
        # Recursively process all items
        for item in obj:
            remove_examples_recursive(item)
    return obj

def normalize_operation(operation):
    """Normalize an operation object to ensure consistent structure."""
    if not isinstance(operation, dict):
        return
    
    # Normalize description
    if 'description' in operation:
        operation['description'] = normalize_description(operation['description'])
    
    # Sort parameters by name for consistency
    if 'parameters' in operation and isinstance(operation['parameters'], list):
        # Sort parameters by name, then by 'in' field
        operation['parameters'] = sorted(
            operation['parameters'],
            key=lambda p: (p.get('in', ''), p.get('name', ''))
        )
        # Normalize each parameter
        for param in operation['parameters']:
            if isinstance(param, dict):
                # Normalize description
                if 'description' in param:
                    param['description'] = normalize_description(param['description'])
                # Remove explicit required: false (query params are optional by default)
                # This matches the static spec which doesn't include this field
                if param.get('in') == 'query' and param.get('required') is False:
                    del param['required']
                # Sort schema properties if present
                if 'schema' in param and isinstance(param['schema'], dict):
                    normalize_schema(param['schema'])
    
    # Normalize request body
    if 'requestBody' in operation and isinstance(operation['requestBody'], dict):
        if 'description' in operation['requestBody']:
            operation['requestBody']['description'] = normalize_description(operation['requestBody']['description'])
        if 'content' in operation['requestBody']:
            # Normalize content types and sort
            normalized_content = {}
            for content_type, content_obj in operation['requestBody']['content'].items():
                normalized_type = normalize_content_type(content_type)
                normalized_content[normalized_type] = content_obj
                if isinstance(content_obj, dict) and 'schema' in content_obj:
                    normalize_schema(content_obj['schema'])
            operation['requestBody']['content'] = dict(sorted(normalized_content.items()))
    
    # Normalize responses
    if 'responses' in operation and isinstance(operation['responses'], dict):
        # Sort response codes for consistency (convert to int for numeric codes, keep string codes at end)
        def response_code_key(code):
            try:
                return (0, int(code))  # Numeric codes first, sorted numerically
            except ValueError:
                return (1, code)  # String codes (like 'default') after
        
        sorted_responses = {}
        for response_code, response_obj in sorted(operation['responses'].items(), key=lambda x: response_code_key(x[0])):
            if isinstance(response_obj, dict):
                # Normalize description
                if 'description' in response_obj:
                    response_obj['description'] = normalize_description(response_obj['description'])
                if 'content' in response_obj:
                    # Normalize content types and sort
                    normalized_content = {}
                    for content_type, content_obj in response_obj['content'].items():
                        normalized_type = normalize_content_type(content_type)
                        normalized_content[normalized_type] = content_obj
                        if isinstance(content_obj, dict) and 'schema' in content_obj:
                            normalize_schema(content_obj['schema'])
                    response_obj['content'] = dict(sorted(normalized_content.items()))
            sorted_responses[response_code] = response_obj
        operation['responses'] = sorted_responses

def normalize_schema(schema):
    """Normalize a schema object."""
    if not isinstance(schema, dict):
        return
    
    # Sort required fields
    if 'required' in schema and isinstance(schema['required'], list):
        schema['required'] = sorted(schema['required'])
    
    # Sort properties
    if 'properties' in schema and isinstance(schema['properties'], dict):
        sorted_props = dict(sorted(schema['properties'].items()))
        schema['properties'] = sorted_props
        # Recursively normalize nested schemas
        for prop_schema in sorted_props.values():
            if isinstance(prop_schema, dict):
                normalize_schema(prop_schema)
    
    # Normalize items for array types
    if 'items' in schema and isinstance(schema['items'], dict):
        normalize_schema(schema['items'])

def normalize_spec(spec_data):
    # First, expand all $ref references before we delete components
    spec_data = expand_refs_recursive(spec_data, spec_data)
    
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
                normalize_schema(schema)
                # Remove example fields
                if 'example' in schema:
                    del schema['example']
    
    # Normalize paths and operations
    if 'paths' in spec_data:
        for path, path_item in spec_data['paths'].items():
            if isinstance(path_item, dict):
                # Normalize all operations
                for method in ['get', 'post', 'put', 'delete', 'patch', 'head', 'options', 'trace']:
                    if method in path_item:
                        normalize_operation(path_item[method])
                # Normalize path-level parameters
                if 'parameters' in path_item and isinstance(path_item['parameters'], list):
                    path_item['parameters'] = sorted(
                        path_item['parameters'],
                        key=lambda p: (p.get('in', ''), p.get('name', ''))
                    )
                    # Normalize parameter descriptions
                    for param in path_item['parameters']:
                        if isinstance(param, dict) and 'description' in param:
                            param['description'] = normalize_description(param['description'])
        # Remove all examples from paths (response content, etc.)
        remove_examples_recursive(spec_data['paths'])
    
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
DIFF_OUTPUT=$(diff -u /tmp/static-normalized.json /tmp/generated-normalized.json)
if [ -z "$DIFF_OUTPUT" ]; then
    echo "   ✅ No differences found! Specs match perfectly."
else
    echo "$DIFF_OUTPUT" | head -30
    echo ""
    echo "   ⚠️  Differences found. See full diff above or check the normalized files."
fi
echo ""

echo "📄 Full normalized specs saved to:"
echo "   /tmp/static-normalized.json"
echo "   /tmp/generated-normalized.json"

