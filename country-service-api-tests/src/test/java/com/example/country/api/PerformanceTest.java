package com.example.country.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests to validate the PRD requirement:
 * "API responses must be fast (<200ms in normal use, locally or in dev cluster)"
 * 
 * These tests measure response times for all endpoints and assert they meet the performance requirement.
 * 
 * Thresholds:
 * - Local environment: 200ms (direct connection, no network latency)
 * - Staging/Remote environment: 1000ms (accounts for network latency, API Gateway processing, Lambda cold starts)
 * 
 * The threshold can be overridden via system property: api.test.performance.max.response.time.ms
 */
@Tag("performance")
@DisplayName("Country API Performance Tests")
class PerformanceTest extends BaseApiTest {
    private static final Logger log = LoggerFactory.getLogger(PerformanceTest.class);
    
    // Default thresholds: stricter for local, more lenient for remote
    private static final long DEFAULT_LOCAL_MAX_RESPONSE_TIME_MS = 200;
    private static final long DEFAULT_REMOTE_MAX_RESPONSE_TIME_MS = 1000;
    
    // Determine if we're testing against a remote environment (staging/production)
    private static final boolean IS_REMOTE_ENV = isRemoteEnvironment();
    
    // Get max response time from system property or use appropriate default
    private static final long MAX_RESPONSE_TIME_MS = getMaxResponseTime();
    
    private static boolean isRemoteEnvironment() {
        String baseUrl = System.getProperty("api.test.base.url", 
                System.getenv().getOrDefault("API_TEST_BASE_URL", "http://localhost:8080"));
        // Remote environments typically use HTTPS and AWS domains
        return baseUrl.startsWith("https://") && 
               (baseUrl.contains("execute-api") || baseUrl.contains("amazonaws.com"));
    }
    
    private static long getMaxResponseTime() {
        String maxTimeProp = System.getProperty("api.test.performance.max.response.time.ms");
        if (maxTimeProp != null && !maxTimeProp.isEmpty()) {
            try {
                return Long.parseLong(maxTimeProp);
            } catch (NumberFormatException e) {
                log.warn("Invalid api.test.performance.max.response.time.ms value: {}, using default", maxTimeProp);
            }
        }
        
        // Use appropriate default based on environment
        return IS_REMOTE_ENV ? DEFAULT_REMOTE_MAX_RESPONSE_TIME_MS : DEFAULT_LOCAL_MAX_RESPONSE_TIME_MS;
    }
    
    @Test
    @DisplayName("GET /countries - Performance test")
    void testListCountriesPerformance() {
        // Warm-up request (optional, helps account for cold starts)
        given()
                .spec(requestSpec)
                .queryParam("limit", 1)
                .when()
                .get("/countries");
        
        // Measure performance
        Response response = given()
                .spec(requestSpec)
                .queryParam("limit", 10)
                .queryParam("offset", 0)
                .when()
                .get("/countries")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response();
        
        long responseTime = response.getTime();
        String envType = IS_REMOTE_ENV ? "remote" : "local";
        log.info("GET /countries response time: {}ms ({} environment, threshold: {}ms)", 
                responseTime, envType, MAX_RESPONSE_TIME_MS);
        
        assertTrue(responseTime < MAX_RESPONSE_TIME_MS,
                String.format("Response time %dms exceeds %dms requirement (%s environment)", 
                        responseTime, MAX_RESPONSE_TIME_MS, envType));
    }
    
    @Test
    @DisplayName("GET /countries/code/{alpha2Code} - Performance test")
    void testGetCountryByAlpha2CodePerformance() {
        // Get a valid country code first
        Response listResponse = given()
                .spec(requestSpec)
                .queryParam("limit", 1)
                .when()
                .get("/countries")
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        List<Map<String, Object>> countries = listResponse.jsonPath().getList("");
        if (countries.isEmpty()) {
            log.warn("Skipping performance test: No countries available");
            return;
        }
        
        String alpha2Code = null;
        for (Map<String, Object> country : countries) {
            Boolean isDeleted = (Boolean) country.get("isDeleted");
            if (isDeleted == null || !isDeleted) {
                String code = (String) country.get("alpha2Code");
                if (code != null && !code.isEmpty()) {
                    // Verify the country actually exists
                    Response verifyResponse = given()
                            .spec(requestSpec)
                            .pathParam("alpha2Code", code)
                            .when()
                            .get("/countries/code/{alpha2Code}")
                            .then()
                            .extract()
                            .response();
                    
                    if (verifyResponse.getStatusCode() == 200) {
                        alpha2Code = code;
                        break;
                    }
                }
            }
        }
        
        if (alpha2Code == null) {
            log.warn("Skipping performance test: No valid alpha2Code found");
            return;
        }
        
        // Warm-up request
        given()
                .spec(requestSpec)
                .pathParam("alpha2Code", alpha2Code)
                .when()
                .get("/countries/code/{alpha2Code}");
        
        // Measure performance
        Response response = given()
                .spec(requestSpec)
                .pathParam("alpha2Code", alpha2Code)
                .when()
                .get("/countries/code/{alpha2Code}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response();
        
        long responseTime = response.getTime();
        String envType = IS_REMOTE_ENV ? "remote" : "local";
        log.info("GET /countries/code/{} response time: {}ms ({} environment, threshold: {}ms)", 
                alpha2Code, responseTime, envType, MAX_RESPONSE_TIME_MS);
        
        assertTrue(responseTime < MAX_RESPONSE_TIME_MS,
                String.format("Response time %dms exceeds %dms requirement (%s environment)", 
                        responseTime, MAX_RESPONSE_TIME_MS, envType));
    }
    
    @Test
    @DisplayName("GET /countries/code3/{alpha3Code} - Performance test")
    void testGetCountryByAlpha3CodePerformance() {
        // Get a valid country code first
        Response listResponse = given()
                .spec(requestSpec)
                .queryParam("limit", 1)
                .when()
                .get("/countries")
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        List<Map<String, Object>> countries = listResponse.jsonPath().getList("");
        if (countries.isEmpty()) {
            log.warn("Skipping performance test: No countries available");
            return;
        }
        
        String alpha3Code = null;
        for (Map<String, Object> country : countries) {
            Boolean isDeleted = (Boolean) country.get("isDeleted");
            if (isDeleted == null || !isDeleted) {
                String code = (String) country.get("alpha3Code");
                if (code != null && !code.isEmpty()) {
                    // Verify the country actually exists
                    Response verifyResponse = given()
                            .spec(requestSpec)
                            .pathParam("alpha3Code", code)
                            .when()
                            .get("/countries/code3/{alpha3Code}")
                            .then()
                            .extract()
                            .response();
                    
                    if (verifyResponse.getStatusCode() == 200) {
                        alpha3Code = code;
                        break;
                    }
                }
            }
        }
        
        if (alpha3Code == null) {
            log.warn("Skipping performance test: No valid alpha3Code found");
            return;
        }
        
        // Warm-up request
        given()
                .spec(requestSpec)
                .pathParam("alpha3Code", alpha3Code)
                .when()
                .get("/countries/code3/{alpha3Code}");
        
        // Measure performance
        Response response = given()
                .spec(requestSpec)
                .pathParam("alpha3Code", alpha3Code)
                .when()
                .get("/countries/code3/{alpha3Code}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response();
        
        long responseTime = response.getTime();
        String envType = IS_REMOTE_ENV ? "remote" : "local";
        log.info("GET /countries/code3/{} response time: {}ms ({} environment, threshold: {}ms)", 
                alpha3Code, responseTime, envType, MAX_RESPONSE_TIME_MS);
        
        assertTrue(responseTime < MAX_RESPONSE_TIME_MS,
                String.format("Response time %dms exceeds %dms requirement (%s environment)", 
                        responseTime, MAX_RESPONSE_TIME_MS, envType));
    }
    
    @Test
    @DisplayName("GET /countries/number/{numericCode} - Performance test")
    void testGetCountryByNumericCodePerformance() {
        // Get a valid country code first
        Response listResponse = given()
                .spec(requestSpec)
                .queryParam("limit", 1)
                .when()
                .get("/countries")
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        List<Map<String, Object>> countries = listResponse.jsonPath().getList("");
        if (countries.isEmpty()) {
            log.warn("Skipping performance test: No countries available");
            return;
        }
        
        String numericCode = null;
        for (Map<String, Object> country : countries) {
            Boolean isDeleted = (Boolean) country.get("isDeleted");
            if (isDeleted == null || !isDeleted) {
                String code = (String) country.get("numericCode");
                if (code != null && !code.isEmpty()) {
                    // Verify the country actually exists
                    Response verifyResponse = given()
                            .spec(requestSpec)
                            .pathParam("numericCode", code)
                            .when()
                            .get("/countries/number/{numericCode}")
                            .then()
                            .extract()
                            .response();
                    
                    if (verifyResponse.getStatusCode() == 200) {
                        numericCode = code;
                        break;
                    }
                }
            }
        }
        
        if (numericCode == null) {
            log.warn("Skipping performance test: No valid numericCode found");
            return;
        }
        
        // Warm-up request
        given()
                .spec(requestSpec)
                .pathParam("numericCode", numericCode)
                .when()
                .get("/countries/number/{numericCode}");
        
        // Measure performance
        Response response = given()
                .spec(requestSpec)
                .pathParam("numericCode", numericCode)
                .when()
                .get("/countries/number/{numericCode}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response();
        
        long responseTime = response.getTime();
        String envType = IS_REMOTE_ENV ? "remote" : "local";
        log.info("GET /countries/number/{} response time: {}ms ({} environment, threshold: {}ms)", 
                numericCode, responseTime, envType, MAX_RESPONSE_TIME_MS);
        
        assertTrue(responseTime < MAX_RESPONSE_TIME_MS,
                String.format("Response time %dms exceeds %dms requirement (%s environment)", 
                        responseTime, MAX_RESPONSE_TIME_MS, envType));
    }
    
    @Test
    @DisplayName("POST /countries - Performance test")
    void testCreateCountryPerformance() {
        // Generate unique test country codes
        long timestamp = System.currentTimeMillis();
        char[] alpha2Chars = {(char)('A' + (timestamp % 26)), (char)('A' + ((timestamp / 26) % 26))};
        String alpha2 = new String(alpha2Chars);
        char[] alpha3Chars = {
            (char)('A' + (timestamp % 26)),
            (char)('A' + ((timestamp / 26) % 26)),
            (char)('A' + ((timestamp / 676) % 26))
        };
        String alpha3 = new String(alpha3Chars);
        String numeric = String.format("%03d", timestamp % 1000);
        
        String requestBody = String.format("""
                {
                    "name": "Performance Test Country %d",
                    "alpha2Code": "%s",
                    "alpha3Code": "%s",
                    "numericCode": "%s"
                }
                """, timestamp, alpha2, alpha3, numeric);
        
        // Measure performance
        Response response = given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/countries")
                .then()
                .statusCode(201)
                .contentType("application/json")
                .extract()
                .response();
        
        long responseTime = response.getTime();
        String envType = IS_REMOTE_ENV ? "remote" : "local";
        log.info("POST /countries response time: {}ms ({} environment, threshold: {}ms)", 
                responseTime, envType, MAX_RESPONSE_TIME_MS);
        
        assertTrue(responseTime < MAX_RESPONSE_TIME_MS,
                String.format("Response time %dms exceeds %dms requirement (%s environment)", 
                        responseTime, MAX_RESPONSE_TIME_MS, envType));
    }
    
    @Test
    @DisplayName("PUT /countries/code/{alpha2Code} - Performance test")
    void testUpdateCountryPerformance() {
        // First create a country
        long timestamp = System.currentTimeMillis();
        char[] alpha2Chars = {(char)('A' + ((timestamp + 100) % 26)), (char)('A' + (((timestamp + 100) / 26) % 26))};
        String alpha2 = new String(alpha2Chars);
        char[] alpha3Chars = {
            (char)('A' + ((timestamp + 100) % 26)),
            (char)('A' + (((timestamp + 100) / 26) % 26)),
            (char)('A' + (((timestamp + 100) / 676) % 26))
        };
        String alpha3 = new String(alpha3Chars);
        String numeric = String.format("%03d", (timestamp + 100) % 1000);
        
        String createBody = String.format("""
                {
                    "name": "Original Name",
                    "alpha2Code": "%s",
                    "alpha3Code": "%s",
                    "numericCode": "%s"
                }
                """, alpha2, alpha3, numeric);
        
        given()
                .spec(requestSpec)
                .body(createBody)
                .when()
                .post("/countries")
                .then()
                .statusCode(201);
        
        // Wait for eventual consistency
        await().atMost(2, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    given()
                            .spec(requestSpec)
                            .pathParam("alpha2Code", alpha2)
                            .when()
                            .get("/countries/code/{alpha2Code}")
                            .then()
                            .statusCode(200);
                });
        
        // Measure update performance
        String updateBody = String.format("""
                {
                    "name": "Updated Name",
                    "alpha2Code": "%s",
                    "alpha3Code": "%s",
                    "numericCode": "%s"
                }
                """, alpha2, alpha3, numeric);
        
        Response response = given()
                .spec(requestSpec)
                .pathParam("alpha2Code", alpha2)
                .body(updateBody)
                .when()
                .put("/countries/code/{alpha2Code}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response();
        
        long responseTime = response.getTime();
        String envType = IS_REMOTE_ENV ? "remote" : "local";
        log.info("PUT /countries/code/{} response time: {}ms ({} environment, threshold: {}ms)", 
                alpha2, responseTime, envType, MAX_RESPONSE_TIME_MS);
        
        assertTrue(responseTime < MAX_RESPONSE_TIME_MS,
                String.format("Response time %dms exceeds %dms requirement (%s environment)", 
                        responseTime, MAX_RESPONSE_TIME_MS, envType));
    }
    
    @Test
    @DisplayName("DELETE /countries/code/{alpha2Code} - Performance test")
    void testDeleteCountryPerformance() {
        // First create a country
        long timestamp = System.currentTimeMillis();
        char[] alpha2Chars = {(char)('A' + ((timestamp + 200) % 26)), (char)('A' + (((timestamp + 200) / 26) % 26))};
        String alpha2 = new String(alpha2Chars);
        char[] alpha3Chars = {
            (char)('A' + ((timestamp + 200) % 26)),
            (char)('A' + (((timestamp + 200) / 26) % 26)),
            (char)('A' + (((timestamp + 200) / 676) % 26))
        };
        String alpha3 = new String(alpha3Chars);
        String numeric = String.format("%03d", (timestamp + 200) % 1000);
        
        String createBody = String.format("""
                {
                    "name": "Country to Delete",
                    "alpha2Code": "%s",
                    "alpha3Code": "%s",
                    "numericCode": "%s"
                }
                """, alpha2, alpha3, numeric);
        
        given()
                .spec(requestSpec)
                .body(createBody)
                .when()
                .post("/countries")
                .then()
                .statusCode(201);
        
        // Wait for eventual consistency
        await().atMost(2, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    given()
                            .spec(requestSpec)
                            .pathParam("alpha2Code", alpha2)
                            .when()
                            .get("/countries/code/{alpha2Code}")
                            .then()
                            .statusCode(200);
                });
        
        // Measure delete performance
        Response response = given()
                .spec(requestSpec)
                .pathParam("alpha2Code", alpha2)
                .when()
                .delete("/countries/code/{alpha2Code}")
                .then()
                .statusCode(204)
                .extract()
                .response();
        
        long responseTime = response.getTime();
        String envType = IS_REMOTE_ENV ? "remote" : "local";
        log.info("DELETE /countries/code/{} response time: {}ms ({} environment, threshold: {}ms)", 
                alpha2, responseTime, envType, MAX_RESPONSE_TIME_MS);
        
        assertTrue(responseTime < MAX_RESPONSE_TIME_MS,
                String.format("Response time %dms exceeds %dms requirement (%s environment)", 
                        responseTime, MAX_RESPONSE_TIME_MS, envType));
    }
    
    @Test
    @DisplayName("GET /countries/code/{alpha2Code}/history - Performance test")
    void testGetCountryHistoryPerformance() {
        // First create and update a country to create history
        long timestamp = System.currentTimeMillis();
        char[] alpha2Chars = {(char)('A' + ((timestamp + 300) % 26)), (char)('A' + (((timestamp + 300) / 26) % 26))};
        String alpha2 = new String(alpha2Chars);
        char[] alpha3Chars = {
            (char)('A' + ((timestamp + 300) % 26)),
            (char)('A' + (((timestamp + 300) / 26) % 26)),
            (char)('A' + (((timestamp + 300) / 676) % 26))
        };
        String alpha3 = new String(alpha3Chars);
        String numeric = String.format("%03d", (timestamp + 300) % 1000);
        
        String createBody = String.format("""
                {
                    "name": "History Test Original",
                    "alpha2Code": "%s",
                    "alpha3Code": "%s",
                    "numericCode": "%s"
                }
                """, alpha2, alpha3, numeric);
        
        given()
                .spec(requestSpec)
                .body(createBody)
                .when()
                .post("/countries")
                .then()
                .statusCode(201);
        
        // Wait for eventual consistency
        await().atMost(2, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    given()
                            .spec(requestSpec)
                            .pathParam("alpha2Code", alpha2)
                            .when()
                            .get("/countries/code/{alpha2Code}")
                            .then()
                            .statusCode(200);
                });
        
        // Update to create history
        String updateBody = String.format("""
                {
                    "name": "History Test Updated",
                    "alpha2Code": "%s",
                    "alpha3Code": "%s",
                    "numericCode": "%s"
                }
                """, alpha2, alpha3, numeric);
        
        given()
                .spec(requestSpec)
                .pathParam("alpha2Code", alpha2)
                .body(updateBody)
                .when()
                .put("/countries/code/{alpha2Code}")
                .then()
                .statusCode(200);
        
        // Wait for eventual consistency
        await().atMost(2, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    given()
                            .spec(requestSpec)
                            .pathParam("alpha2Code", alpha2)
                            .when()
                            .get("/countries/code/{alpha2Code}")
                            .then()
                            .statusCode(200);
                });
        
        // Warm-up request
        given()
                .spec(requestSpec)
                .pathParam("alpha2Code", alpha2)
                .when()
                .get("/countries/code/{alpha2Code}/history");
        
        // Measure history retrieval performance
        Response response = given()
                .spec(requestSpec)
                .pathParam("alpha2Code", alpha2)
                .when()
                .get("/countries/code/{alpha2Code}/history")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response();
        
        long responseTime = response.getTime();
        String envType = IS_REMOTE_ENV ? "remote" : "local";
        log.info("GET /countries/code/{}/history response time: {}ms ({} environment, threshold: {}ms)", 
                alpha2, responseTime, envType, MAX_RESPONSE_TIME_MS);
        
        assertTrue(responseTime < MAX_RESPONSE_TIME_MS,
                String.format("Response time %dms exceeds %dms requirement (%s environment)", 
                        responseTime, MAX_RESPONSE_TIME_MS, envType));
    }
}

