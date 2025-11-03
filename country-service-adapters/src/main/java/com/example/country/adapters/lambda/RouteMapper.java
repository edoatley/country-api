package com.example.country.adapters.lambda;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Maps API Gateway HTTP method + path to handler actions.
 * Supports the OpenAPI specification routes.
 */
public class RouteMapper {
    private static final Pattern ALPHA2_PATTERN = Pattern.compile("^/api/v1/countries/code/([A-Z]{2})$");
    private static final Pattern ALPHA3_PATTERN = Pattern.compile("^/api/v1/countries/code3/([A-Z]{3})$");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^/api/v1/countries/number/([0-9]{3})$");
    private static final Pattern HISTORY_PATTERN = Pattern.compile("^/api/v1/countries/code/([A-Z]{2})/history$");
    
    /**
     * Maps HTTP method and path to a RouteMapping.
     * 
     * @param httpMethod HTTP method (GET, POST, PUT, DELETE)
     * @param path API Gateway path (e.g., /api/v1/countries)
     * @return RouteMapping with action and path parameter keys, or null if no match
     */
    public RouteMapping map(String httpMethod, String path) {
        Objects.requireNonNull(httpMethod, "HTTP method must not be null");
        Objects.requireNonNull(path, "Path must not be null");
        
        // Normalize path
        String normalizedPath = path;
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        
        return switch (httpMethod.toUpperCase()) {
            case "GET" -> mapGet(normalizedPath);
            case "POST" -> mapPost(normalizedPath);
            case "PUT" -> mapPut(normalizedPath);
            case "DELETE" -> mapDelete(normalizedPath);
            default -> null;
        };
    }
    
    private RouteMapping mapGet(String path) {
        if ("/api/v1/countries".equals(path)) {
            return new RouteMapping("GET_ALL", new HashMap<>());
        }
        
        // Match /countries/code/{alpha2Code}
        var alpha2Matcher = ALPHA2_PATTERN.matcher(path);
        if (alpha2Matcher.matches()) {
            Map<String, String> pathParams = new HashMap<>();
            pathParams.put("alpha2Code", alpha2Matcher.group(1));
            return new RouteMapping("GET_ALPHA2", pathParams);
        }
        
        // Match /countries/code/{alpha2Code}/history
        var historyMatcher = HISTORY_PATTERN.matcher(path);
        if (historyMatcher.matches()) {
            Map<String, String> pathParams = new HashMap<>();
            pathParams.put("alpha2Code", historyMatcher.group(1));
            return new RouteMapping("HISTORY_ALPHA2", pathParams);
        }
        
        // Match /countries/code3/{alpha3Code}
        var alpha3Matcher = ALPHA3_PATTERN.matcher(path);
        if (alpha3Matcher.matches()) {
            Map<String, String> pathParams = new HashMap<>();
            pathParams.put("alpha3Code", alpha3Matcher.group(1));
            return new RouteMapping("GET_ALPHA3", pathParams);
        }
        
        // Match /countries/number/{numericCode}
        var numericMatcher = NUMERIC_PATTERN.matcher(path);
        if (numericMatcher.matches()) {
            Map<String, String> pathParams = new HashMap<>();
            pathParams.put("numericCode", numericMatcher.group(1));
            return new RouteMapping("GET_NUMERIC", pathParams);
        }
        
        return null;
    }
    
    private RouteMapping mapPost(String path) {
        if ("/api/v1/countries".equals(path)) {
            return new RouteMapping("CREATE", new HashMap<>());
        }
        return null;
    }
    
    private RouteMapping mapPut(String path) {
        // Match /countries/code/{alpha2Code}
        var alpha2Matcher = ALPHA2_PATTERN.matcher(path);
        if (alpha2Matcher.matches()) {
            Map<String, String> pathParams = new HashMap<>();
            pathParams.put("alpha2Code", alpha2Matcher.group(1));
            return new RouteMapping("UPDATE_ALPHA2", pathParams);
        }
        return null;
    }
    
    private RouteMapping mapDelete(String path) {
        // Match /countries/code/{alpha2Code}
        var alpha2Matcher = ALPHA2_PATTERN.matcher(path);
        if (alpha2Matcher.matches()) {
            Map<String, String> pathParams = new HashMap<>();
            pathParams.put("alpha2Code", alpha2Matcher.group(1));
            return new RouteMapping("DELETE_ALPHA2", pathParams);
        }
        return null;
    }
}
