package com.example.country.adapters.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyValidatorTest {

    @Test
    void validatesCorrectApiKey() {
        ApiKeyValidator validator = new ApiKeyValidator("test-key");
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("X-API-KEY", "test-key");
        event.setHeaders(headers);
        
        assertTrue(validator.isValid(event));
    }

    @Test
    void rejectsIncorrectApiKey() {
        ApiKeyValidator validator = new ApiKeyValidator("test-key");
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("X-API-KEY", "wrong-key");
        event.setHeaders(headers);
        
        assertFalse(validator.isValid(event));
    }

    @Test
    void rejectsMissingApiKey() {
        ApiKeyValidator validator = new ApiKeyValidator("test-key");
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHeaders(new HashMap<>());
        
        assertFalse(validator.isValid(event));
    }

    @Test
    void rejectsNullEvent() {
        ApiKeyValidator validator = new ApiKeyValidator("test-key");
        
        assertFalse(validator.isValid(null));
    }

    @Test
    void rejectsNullHeaders() {
        ApiKeyValidator validator = new ApiKeyValidator("test-key");
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHeaders(null);
        
        assertFalse(validator.isValid(event));
    }

    @Test
    void validatesCaseInsensitiveHeaderName() {
        ApiKeyValidator validator = new ApiKeyValidator("test-key");
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key", "test-key");  // lowercase
        event.setHeaders(headers);
        
        assertTrue(validator.isValid(event));
    }

    @Test
    void validatesFromMultiValueHeaders() {
        ApiKeyValidator validator = new ApiKeyValidator("test-key");
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        // Set empty headers to ensure we check multiValueHeaders
        event.setHeaders(new HashMap<>());
        Map<String, java.util.List<String>> multiValueHeaders = new HashMap<>();
        multiValueHeaders.put("X-API-KEY", java.util.List.of("test-key"));
        event.setMultiValueHeaders(multiValueHeaders);
        
        assertTrue(validator.isValid(event));
    }

    @Test
    void rejectsNullExpectedApiKey() {
        assertThrows(NullPointerException.class, () -> new ApiKeyValidator(null));
    }
}
