package com.example.country.adapters.lambda;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a route mapping from HTTP method + path to handler action.
 */
public class RouteMapping {
    private final String action;
    private final Map<String, String> pathParams;
    
    public RouteMapping(String action, Map<String, String> pathParams) {
        this.action = Objects.requireNonNull(action, "Action must not be null");
        this.pathParams = Objects.requireNonNull(pathParams, "Path params must not be null");
    }
    
    public String getAction() {
        return action;
    }
    
    public Map<String, String> getPathParams() {
        return pathParams;
    }
}
