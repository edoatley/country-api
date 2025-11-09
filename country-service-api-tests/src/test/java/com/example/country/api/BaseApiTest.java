package com.example.country.api;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for API tests that can run against local or staging environments.
 * 
 * Configure the test environment using:
 * - System property: `api.test.base.url` (default: `http://localhost:8080`)
 * - System property: `api.test.api.key` (default: `default-test-key`)
 * - Environment variable: `API_TEST_BASE_URL`
 * - Environment variable: `API_TEST_API_KEY`
 */
public abstract class BaseApiTest {
    
    protected static String baseUrl;
    protected static String apiKey;
    protected RequestSpecification requestSpec;
    
    @BeforeAll
    static void configureBaseUrl() {
        // Get base URL from system property, environment variable, or use default
        baseUrl = System.getProperty("api.test.base.url", 
                System.getenv().getOrDefault("API_TEST_BASE_URL", "http://localhost:8080"));
        
        // Get API key from system property, environment variable, or use default
        apiKey = System.getProperty("api.test.api.key",
                System.getenv().getOrDefault("API_TEST_API_KEY", "default-test-key"));
        
        // Configure RestAssured base URI
        RestAssured.baseURI = baseUrl;
        
        System.out.println("API Test Configuration:");
        System.out.println("  Base URL: " + baseUrl);
        System.out.println("  API Key: " + (apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : apiKey));
    }
    
    @BeforeEach
    void setUpRequestSpec() {
        // Create request specification with common headers
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .addHeader("X-API-KEY", apiKey)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON);
        
        // If base URL already includes /api/v1, don't add it again
        // Otherwise, set base path to /api/v1
        if (!baseUrl.contains("/api/v1")) {
            builder.setBasePath("/api/v1");
        }
        
        requestSpec = builder.build();
    }
    
    /**
     * Get the base URL for the API (without trailing slash)
     */
    protected String getBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
    
    /**
     * Get the API key being used for tests
     */
    protected String getApiKey() {
        return apiKey;
    }
}

