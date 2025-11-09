package com.example.country.adapters.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.example.country.application.model.CountryInput;
import com.example.country.domain.Country;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * AWS Lambda handler for API Gateway integration.
 * Parses API Gateway events and delegates to CountryLambdaHandler.
 */
public class ApiGatewayLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String DEFAULT_API_KEY = System.getenv("API_KEY");
    
    private final CountryLambdaHandler handler;
    private final ObjectMapper objectMapper;
    private final ApiKeyValidator apiKeyValidator;
    private final RouteMapper routeMapper;
    
    public ApiGatewayLambdaHandler(CountryLambdaHandler handler, ApiKeyValidator apiKeyValidator, RouteMapper routeMapper) {
        this.handler = Objects.requireNonNull(handler);
        // Configure ObjectMapper with JavaTimeModule and Country MixIn for proper serialization
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.addMixIn(Country.class, CountryJacksonMixIn.class);
        this.apiKeyValidator = Objects.requireNonNull(apiKeyValidator);
        this.routeMapper = Objects.requireNonNull(routeMapper);
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            // Extract and validate API key
            if (!apiKeyValidator.isValid(event)) {
                return createErrorResponse(401, "Unauthorized", "Missing or invalid API key");
            }
            
            // Parse route to get action and path parameters
            RouteMapping mapping = routeMapper.map(event.getHttpMethod(), event.getPath());
            if (mapping == null) {
                return createErrorResponse(404, "Not Found", "No route found for " + event.getHttpMethod() + " " + event.getPath());
            }
            
            // Use path parameters from RouteMapping (extracted from path pattern)
            // Merge with event path parameters if present (API Gateway provides them)
            Map<String, String> pathParams = new HashMap<>(mapping.getPathParams());
            if (event.getPathParameters() != null) {
                pathParams.putAll(event.getPathParameters());
            }
            
            // Extract query parameters
            Map<String, String> queryParams = event.getQueryStringParameters() != null 
                    ? event.getQueryStringParameters() 
                    : new HashMap<>();
            
            // Parse request body if present
            CountryInput body = null;
            if (event.getBody() != null && !event.getBody().isEmpty()) {
                body = objectMapper.readValue(event.getBody(), CountryInput.class);
            }
            
            // Delegate to handler
            Object result = handler.handleRequest(mapping.getAction(), pathParams, queryParams, body);
            
            // Build response
            if (result == null) {
                // DELETE returns 204 No Content
                return createSuccessResponse(204, null);
            } else {
                // Serialize result to JSON
                // Log the result type for debugging
                if (context != null) {
                    context.getLogger().log("Serializing result of type: " + (result != null ? result.getClass().getName() : "null"));
                }
                String jsonBody = objectMapper.writeValueAsString(result);
                // Log first 200 chars of JSON for debugging (truncate if longer)
                if (context != null && jsonBody != null) {
                    String preview = jsonBody.length() > 200 ? jsonBody.substring(0, 200) + "..." : jsonBody;
                    context.getLogger().log("Serialized JSON preview: " + preview);
                }
                int statusCode = mapping.getAction().startsWith("CREATE") ? 201 : 200;
                return createSuccessResponse(statusCode, jsonBody);
            }
            
        } catch (IllegalArgumentException e) {
            return createErrorResponse(400, "Bad Request", e.getMessage());
        } catch (NoSuchElementException e) {
            return createErrorResponse(404, "Not Found", e.getMessage());
        } catch (Exception e) {
            context.getLogger().log("Error processing request: " + e.getMessage());
            return createErrorResponse(500, "Internal Server Error", "An unexpected error occurred");
        }
    }
    
    private APIGatewayProxyResponseEvent createSuccessResponse(int statusCode, String body) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);
        if (body != null) {
            response.setBody(body);
        }
        return response;
    }
    
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String error, String message) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);
        
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("status", statusCode);
        errorBody.put("error", error);
        errorBody.put("message", message);
        errorBody.put("timestamp", java.time.Instant.now().toString());
        
        try {
            response.setBody(objectMapper.writeValueAsString(errorBody));
        } catch (Exception e) {
            response.setBody("{\"status\":" + statusCode + ",\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
        }
        return response;
    }
}
