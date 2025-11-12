package com.example.country.api;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Base class for API tests that can run against local or staging environments.
 * 
 * Configure the test environment using:
 * - System property: `api.test.base.url` (default: `http://localhost:8080`)
 * - System property: `api.test.api.key` (default: `default-test-key`)
 * - Environment variable: `API_TEST_BASE_URL`
 * - Environment variable: `API_TEST_API_KEY`
 * 
 * OpenAPI validation is enabled by default. To disable it, set system property:
 * - `api.test.openapi.validation.enabled=false`
 */
public abstract class BaseApiTest {
    
    protected static String baseUrl;
    protected static String apiKey;
    protected static Filter openApiValidationFilter;
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
        
        // Setup OpenAPI validation filter if enabled (default: true)
        boolean validationEnabled = !"false".equalsIgnoreCase(
                System.getProperty("api.test.openapi.validation.enabled",
                        System.getenv().getOrDefault("API_TEST_OPENAPI_VALIDATION_ENABLED", "true")));
        
        if (validationEnabled) {
            try {
                // Load OpenAPI spec from classpath
                String openApiSpecPath = loadOpenApiSpec();
                openApiValidationFilter = new OpenApiValidationFilter(openApiSpecPath);
                System.out.println("  OpenAPI validation: ENABLED");
            } catch (Exception e) {
                System.err.println("  WARNING: Failed to load OpenAPI spec for validation: " + e.getMessage());
                System.err.println("  OpenAPI validation: DISABLED");
                openApiValidationFilter = null;
            }
        } else {
            System.out.println("  OpenAPI validation: DISABLED (via configuration)");
            openApiValidationFilter = null;
        }
        
        System.out.println("API Test Configuration:");
        System.out.println("  Base URL: " + baseUrl);
        System.out.println("  API Key: " + (apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : apiKey));
    }
    
    /**
     * Load OpenAPI spec from classpath and return path to temporary file.
     * The validator requires a file path, so we copy the resource to a temp file.
     */
    private static String loadOpenApiSpec() throws Exception {
        InputStream specStream = BaseApiTest.class.getClassLoader()
                .getResourceAsStream("openapi.yml");
        
        if (specStream == null) {
            throw new IllegalStateException("OpenAPI spec not found in test resources (openapi.yml)");
        }
        
        // Copy to temp file (validator needs a file path, not a stream)
        Path tempFile = Files.createTempFile("openapi-", ".yml");
        Files.copy(specStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        tempFile.toFile().deleteOnExit(); // Clean up on JVM exit
        
        return tempFile.toAbsolutePath().toString();
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
            System.out.println("  Base path set to: /api/v1");
        } else {
            System.out.println("  Base URL already includes /api/v1, not setting base path");
        }
        
        // Add OpenAPI validation filter if enabled
        if (openApiValidationFilter != null) {
            builder.addFilter(openApiValidationFilter);
        }
        
        requestSpec = builder.build();
        
        // Enable logging for failed requests to help debug
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
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

