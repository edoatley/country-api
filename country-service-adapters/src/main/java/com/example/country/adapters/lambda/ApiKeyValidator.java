package com.example.country.adapters.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Validates API keys from API Gateway events.
 * Extracts X-API-KEY header (case-insensitive) and validates against expected value.
 */
public class ApiKeyValidator {
    private static final String API_KEY_HEADER = "X-API-KEY";
    private final String expectedApiKey;
    
    public ApiKeyValidator(String expectedApiKey) {
        this.expectedApiKey = Objects.requireNonNull(expectedApiKey, "Expected API key must not be null");
    }
    
    /**
     * Validates API key from API Gateway event.
     * Checks headers (case-insensitive) for X-API-KEY.
     * 
     * @param event API Gateway request event
     * @return true if valid API key is present, false otherwise
     */
    public boolean isValid(APIGatewayProxyRequestEvent event) {
        if (event == null || event.getHeaders() == null) {
            return false;
        }
        
        // API Gateway may normalize headers to lowercase
        Map<String, String> headers = event.getHeaders();
        String apiKey = null;
        
        // Check for X-API-KEY (case-insensitive)
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (API_KEY_HEADER.equalsIgnoreCase(entry.getKey())) {
                apiKey = entry.getValue();
                break;
            }
        }
        
        // Also check multiValueHeaders (API Gateway v2 may use this)
        if (apiKey == null && event.getMultiValueHeaders() != null) {
            for (Map.Entry<String, List<String>> entry : event.getMultiValueHeaders().entrySet()) {
                if (API_KEY_HEADER.equalsIgnoreCase(entry.getKey()) && !entry.getValue().isEmpty()) {
                    apiKey = entry.getValue().get(0);
                    break;
                }
            }
        }
        
        return apiKey != null && Objects.equals(apiKey, expectedApiKey);
    }
}
