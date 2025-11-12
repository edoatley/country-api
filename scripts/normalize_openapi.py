#!/usr/bin/env python3
"""
Normalize OpenAPI specifications for comparison.

This script normalizes OpenAPI specs by:
- Expanding $ref references
- Normalizing content types, descriptions, and field ordering
- Removing acceptable differences (examples, reusable components)
- Sorting arrays and properties for consistent comparison

Usage:
    python3 normalize_openapi.py <input_file> <output_file>
"""

import json
import copy
import sys


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
                    # Expand the resolved object recursively (to handle nested refs)
                    expanded = expand_refs_recursive(resolved, spec_data, visited)
                    if isinstance(expanded, dict):
                        # Merge any additional keys from the original (like description)
                        for key, value in obj.items():
                            if key != '$ref' and key not in expanded:
                                expanded[key] = expand_refs_recursive(value, spec_data, visited)
                    return expanded
                else:
                    # Couldn't resolve - remove $ref and continue
                    other_keys = {k: expand_refs_recursive(v, spec_data, visited) for k, v in obj.items() if k != '$ref'}
                    return other_keys if other_keys else {}
            else:
                # Already visited this ref - this might be a circular reference
                # But we still want to expand it if it's the only key (like items: {$ref: ...})
                # Try to resolve it anyway (it should work since components still exist)
                resolved = resolve_ref(ref_path, spec_data)
                if resolved:
                    # Expand it but don't add to visited (already there)
                    expanded = expand_refs_recursive(resolved, spec_data, visited)
                    # Merge any additional keys
                    if isinstance(expanded, dict):
                        for key, value in obj.items():
                            if key != '$ref' and key not in expanded:
                                expanded[key] = expand_refs_recursive(value, spec_data, visited)
                    return expanded
                # If we can't resolve, remove $ref and return other keys
                other_keys = {k: expand_refs_recursive(v, spec_data, visited) for k, v in obj.items() if k != '$ref'}
                return other_keys if other_keys else {}
        
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
    # Remove trailing periods and trim
    normalized = description.rstrip('.').strip()
    # For very short descriptions that are prefixes of longer ones, keep them as-is
    # This handles cases like "Bad Request" vs "Bad Request. The request was malformed..."
    # We'll keep both as-is since they're semantically different
    return normalized


def remove_examples_recursive(obj):
    """Recursively remove all 'examples' keys from the object."""
    if isinstance(obj, dict):
        if 'examples' in obj:
            del obj['examples']
        if 'example' in obj:
            del obj['example']
        for value in obj.values():
            remove_examples_recursive(value)
    elif isinstance(obj, list):
        for item in obj:
            remove_examples_recursive(item)
    return obj


def normalize_operation(operation):
    """Normalize an operation object to ensure consistent structure."""
    if not isinstance(operation, dict):
        return
    
    if 'description' in operation:
        operation['description'] = normalize_description(operation['description'])
    
    if 'parameters' in operation and isinstance(operation['parameters'], list):
        operation['parameters'] = sorted(
            operation['parameters'],
            key=lambda p: (p.get('in', ''), p.get('name', ''))
        )
        for param in operation['parameters']:
            if isinstance(param, dict):
                if 'description' in param:
                    param['description'] = normalize_description(param['description'])
                if param.get('in') == 'query' and param.get('required') is False:
                    del param['required']
                if 'schema' in param and isinstance(param['schema'], dict):
                    normalize_schema(param['schema'])
    
    if 'requestBody' in operation and isinstance(operation['requestBody'], dict):
        if 'description' in operation['requestBody']:
            operation['requestBody']['description'] = normalize_description(operation['requestBody']['description'])
        if 'content' in operation['requestBody']:
            normalized_content = {}
            for content_type, content_obj in operation['requestBody']['content'].items():
                normalized_type = normalize_content_type(content_type)
                normalized_content[normalized_type] = content_obj
                if isinstance(content_obj, dict) and 'schema' in content_obj:
                    normalize_schema(content_obj['schema'])
            operation['requestBody']['content'] = dict(sorted(normalized_content.items()))
    
    if 'responses' in operation and isinstance(operation['responses'], dict):
        def response_code_key(code):
            try:
                return (0, int(code))
            except ValueError:
                return (1, code)
        
        sorted_responses = {}
        for response_code, response_obj in sorted(operation['responses'].items(), key=lambda x: response_code_key(x[0])):
            if isinstance(response_obj, dict):
                if 'description' in response_obj:
                    response_obj['description'] = normalize_description(response_obj['description'])
                if 'content' in response_obj:
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
    
    # If this schema has a $ref, it should have been expanded already, but handle it just in case
    if '$ref' in schema:
        # This shouldn't happen after expansion, but if it does, we'll skip normalization
        return
    
    if 'required' in schema and isinstance(schema['required'], list):
        schema['required'] = sorted(schema['required'])
    
    if 'properties' in schema and isinstance(schema['properties'], dict):
        sorted_props = dict(sorted(schema['properties'].items()))
        schema['properties'] = sorted_props
        for prop_schema in sorted_props.values():
            if isinstance(prop_schema, dict):
                normalize_schema(prop_schema)
    
    if 'items' in schema and isinstance(schema['items'], dict):
        # Normalize the items schema (which may have been expanded from a $ref)
        normalize_schema(schema['items'])


def normalize_all_schemas_recursive(obj, parent_key=None):
    """Recursively find and normalize all schema objects in the structure."""
    if isinstance(obj, dict):
        # Check if this is a schema object
        # A schema object typically has: type, properties, items, allOf, oneOf, anyOf, or $ref
        is_schema = (
            'type' in obj or 
            'properties' in obj or 
            'items' in obj or
            'allOf' in obj or
            'oneOf' in obj or
            'anyOf' in obj or
            '$ref' in obj or
            # Also check if parent key suggests this is a schema
            parent_key in ['schema', 'items', 'additionalProperties', 'allOf', 'oneOf', 'anyOf']
        )
        
        if is_schema:
            # This is a schema - normalize it (which will also normalize nested items)
            normalize_schema(obj)
        
        # Recursively process all values, passing the key as context
        for key, value in obj.items():
            normalize_all_schemas_recursive(value, parent_key=key)
    elif isinstance(obj, list):
        for item in obj:
            normalize_all_schemas_recursive(item, parent_key=parent_key)


def normalize_spec(spec_data):
    """Normalize OpenAPI spec for comparison by removing acceptable differences."""
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
                if 'example' in schema:
                    del schema['example']
    
    if 'paths' in spec_data:
        for path, path_item in spec_data['paths'].items():
            if isinstance(path_item, dict):
                for method in ['get', 'post', 'put', 'delete', 'patch', 'head', 'options', 'trace']:
                    if method in path_item:
                        operation = path_item[method]
                        normalize_operation(operation)
                        
                        # Normalize tags (SpringDoc generates different tag names like "country-controller" vs "Country")
                        # We normalize by converting to a standard format to ignore naming differences
                        if 'tags' in operation and isinstance(operation['tags'], list):
                            # Normalize tag names: convert "country-controller" -> "Country" style
                            normalized_tags = []
                            for tag in operation['tags']:
                                if isinstance(tag, str):
                                    # Convert kebab-case or lowercase to Title Case for comparison
                                    # This makes "country-controller" and "Country" both become comparable
                                    normalized_tag = tag.replace('-', ' ').replace('_', ' ').title().replace(' ', '')
                                    normalized_tags.append(normalized_tag)
                                else:
                                    normalized_tags.append(tag)
                            operation['tags'] = sorted(normalized_tags)
                        
                        # Remove 404 responses from GET endpoints that return arrays (list endpoints)
                        # List endpoints should never return 404 - they return empty arrays instead
                        if method == 'get' and 'responses' in operation:
                            responses = operation['responses']
                            # Check if this is a list endpoint (returns array)
                            is_list_endpoint = False
                            if '200' in responses:
                                response_200 = responses['200']
                                if isinstance(response_200, dict) and 'content' in response_200:
                                    for content_obj in response_200['content'].values():
                                        if isinstance(content_obj, dict) and 'schema' in content_obj:
                                            schema = content_obj['schema']
                                            if isinstance(schema, dict) and schema.get('type') == 'array':
                                                is_list_endpoint = True
                                                break
                            
                            # Remove 404 from list endpoints if it exists
                            if is_list_endpoint and '404' in responses:
                                response_404 = responses['404']
                                # Only remove if it looks auto-generated (has "Not Found" description)
                                if isinstance(response_404, dict):
                                    description = response_404.get('description', '')
                                    if description == 'Not Found' or not description:
                                        del responses['404']
                
                if 'parameters' in path_item and isinstance(path_item['parameters'], list):
                    path_item['parameters'] = sorted(
                        path_item['parameters'],
                        key=lambda p: (p.get('in', ''), p.get('name', ''))
                    )
                    for param in path_item['parameters']:
                        if isinstance(param, dict) and 'description' in param:
                            param['description'] = normalize_description(param['description'])
        
        # After normalizing operations, do a final pass to normalize all schemas in paths
        # This ensures schemas that were expanded from $ref in items are also normalized
        normalize_all_schemas_recursive(spec_data['paths'])
        
        # Remove examples after all normalization is done
        remove_examples_recursive(spec_data['paths'])
    
    # Normalize top-level tags (if present)
    if 'tags' in spec_data and isinstance(spec_data['tags'], list):
        # Sort tags by name for consistency
        spec_data['tags'] = sorted(
            spec_data['tags'],
            key=lambda t: t.get('name', '') if isinstance(t, dict) else str(t)
        )
    
    return spec_data


def main():
    """Main entry point for the script."""
    if len(sys.argv) != 3:
        print("Usage: python3 normalize_openapi.py <input_file> <output_file>", file=sys.stderr)
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    
    try:
        with open(input_file, 'r') as f:
            spec_data = json.load(f)
        
        normalized = normalize_spec(spec_data)
        
        with open(output_file, 'w') as f:
            json.dump(normalized, f, indent=2, sort_keys=True)
    except FileNotFoundError:
        print(f"Error: Input file not found: {input_file}", file=sys.stderr)
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON in input file: {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()

