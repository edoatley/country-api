package com.example.country.adapters.lambda;

import com.example.country.adapters.api.CountryApi;

import java.util.Objects;

/**
 * Factory for creating AWS Lambda handlers.
 * Used to wire up dependencies for Lambda deployment.
 */
public class LambdaHandlerFactory {
    
    /**
     * Creates an ApiGatewayLambdaHandler with all dependencies wired.
     * This is the main entry point for AWS Lambda deployment.
     * 
     * @param countryApi The CountryApi instance (typically from DI container)
     * @param expectedApiKey The expected API key for validation
     * @return Configured ApiGatewayLambdaHandler ready for Lambda deployment
     */
    public static ApiGatewayLambdaHandler createHandler(CountryApi countryApi, String expectedApiKey) {
        Objects.requireNonNull(countryApi, "CountryApi must not be null");
        Objects.requireNonNull(expectedApiKey, "Expected API key must not be null");
        
        CountryLambdaHandler lambdaHandler = new CountryLambdaHandler(countryApi);
        ApiKeyValidator validator = new ApiKeyValidator(expectedApiKey);
        RouteMapper routeMapper = new RouteMapper();
        
        return new ApiGatewayLambdaHandler(lambdaHandler, validator, routeMapper);
    }
    
    /**
     * Creates an ApiGatewayLambdaHandler using API key from environment variable.
     * Falls back to a default key if environment variable is not set (for local testing).
     * 
     * @param countryApi The CountryApi instance
     * @return Configured ApiGatewayLambdaHandler
     */
    public static ApiGatewayLambdaHandler createHandlerFromEnvironment(CountryApi countryApi) {
        String apiKey = System.getenv("API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getProperty("api.key", "default-test-key");
        }
        return createHandler(countryApi, apiKey);
    }
}
