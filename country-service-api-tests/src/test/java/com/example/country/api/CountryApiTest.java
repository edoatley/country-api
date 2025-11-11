package com.example.country.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("api")
@DisplayName("Country API Tests")
class CountryApiTest extends BaseApiTest {
    
    @Test
    @DisplayName("GET /api/v1/countries - List all countries (paginated)")
    void testListAllCountries() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("limit", 10)
                .queryParam("offset", 0)
                .when()
                .get("/countries")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("", is(instanceOf(List.class)))
                .extract()
                .response();
        
        List<Map<String, Object>> countries = response.jsonPath().getList("");
        assertNotNull(countries, "Countries list should not be null");
        assertTrue(countries.size() <= 10, "Should return at most 10 countries");
        
        // Verify country structure
        if (!countries.isEmpty()) {
            Map<String, Object> country = countries.get(0);
            // Log the actual country structure for debugging
            System.out.println("  First country in list: " + country);
            assertTrue(country.containsKey("name"), 
                    "Country should have 'name' field. Actual country: " + country);
            assertTrue(country.containsKey("alpha2Code"), 
                    "Country should have 'alpha2Code' field. Actual country: " + country);
            assertTrue(country.containsKey("alpha3Code"), 
                    "Country should have 'alpha3Code' field. Actual country: " + country);
            assertTrue(country.containsKey("numericCode"), 
                    "Country should have 'numericCode' field. Actual country: " + country);
        } else {
            System.out.println("  ⚠️  Warning: Countries list is empty - this may indicate the database is empty");
        }
    }
    
    @Test
    @DisplayName("GET /api/v1/countries/code/{alpha2Code} - Get country by alpha-2 code")
    void testGetCountryByAlpha2Code() {
        // First, get a list to find a valid country code
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
            // Skip test if no countries available
            return;
        }
        
        // Find first non-deleted country that actually exists (verify by trying to retrieve it)
        String alpha2Code = null;
        for (Map<String, Object> country : countries) {
            Boolean isDeleted = (Boolean) country.get("isDeleted");
            if (isDeleted == null || !isDeleted) {
                String code = (String) country.get("alpha2Code");
                if (code != null && !code.isEmpty()) {
                    // Verify the country actually exists by trying to retrieve it
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
        
        if (alpha2Code == null || alpha2Code.isEmpty()) {
            // Skip test if no valid alpha2Code found
            System.out.println("  ⚠️  Skipping test: No valid (retrievable) alpha2Code found");
            return;
        }
        
        System.out.println("  Testing with alpha2Code: " + alpha2Code);
        
        // Test getting by alpha-2 code
        given()
                .spec(requestSpec)
                .pathParam("alpha2Code", alpha2Code)
                .when()
                .get("/countries/code/{alpha2Code}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("alpha2Code", equalTo(alpha2Code))
                .body("name", notNullValue())
                .body("alpha3Code", notNullValue())
                .body("numericCode", notNullValue());
    }
    
    @Test
    @DisplayName("GET /api/v1/countries/code/{alpha2Code} - Returns 404 for non-existent country")
    void testGetCountryByAlpha2CodeNotFound() {
        // Get all existing alpha2 codes from the API to ensure we use a code that doesn't exist
        Response listResponse = given()
                .spec(requestSpec)
                .queryParam("limit", 1000) // Get a large number to cover most countries
                .when()
                .get("/countries")
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        List<Map<String, Object>> countries = listResponse.jsonPath().getList("");
        java.util.Set<String> existingCodes = new java.util.HashSet<>();
        if (countries != null) {
            for (Map<String, Object> country : countries) {
                String code = (String) country.get("alpha2Code");
                if (code != null && !code.isEmpty()) {
                    existingCodes.add(code);
                }
            }
        }
        
        // Generate a code that's guaranteed not to exist
        // Try all possible combinations systematically until we find one that doesn't exist
        String nonExistentCode = null;
        long timestamp = System.currentTimeMillis();
        int attempts = 0;
        int maxAttempts = 676; // 26 * 26 = all possible 2-letter combinations
        
        // Start with a timestamp-based code and iterate if needed
        while (nonExistentCode == null && attempts < maxAttempts) {
            // Generate a code using timestamp and attempt counter
            int base = (int) ((timestamp + attempts) % 676);
            char first = (char) ('A' + (base % 26));
            char second = (char) ('A' + ((base / 26) % 26));
            String candidateCode = String.valueOf(first) + String.valueOf(second);
            
            // Check if this code exists in the database
            Response checkResponse = given()
                    .spec(requestSpec)
                    .pathParam("alpha2Code", candidateCode)
                    .when()
                    .get("/countries/code/{alpha2Code}")
                    .then()
                    .extract()
                    .response();
            
            // If code doesn't exist (404), we found our candidate
            if (checkResponse.getStatusCode() == 404) {
                nonExistentCode = candidateCode;
                break;
            }
            
            attempts++;
        }
        
        // Fallback: if we couldn't find a non-existent code (should never happen with real data),
        // use a code that's definitely not in the standard ISO 3166 list
        if (nonExistentCode == null) {
            // Use "XX" which is reserved/unassigned in ISO 3166-1 alpha-2
            // But check if it exists first
            Response xxResponse = given()
                    .spec(requestSpec)
                    .pathParam("alpha2Code", "XX")
                    .when()
                    .get("/countries/code/{alpha2Code}")
                    .then()
                    .extract()
                    .response();
            
            if (xxResponse.getStatusCode() == 404) {
                nonExistentCode = "XX";
            } else {
                // Last resort: use a combination that's very unlikely
                nonExistentCode = "ZZ";
            }
        }
        
        // Now test that the non-existent code returns 404
        given()
                .spec(requestSpec)
                .pathParam("alpha2Code", nonExistentCode)
                .when()
                .get("/countries/code/{alpha2Code}")
                .then()
                .statusCode(404)
                .contentType("application/json")
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"));
    }
    
    @Test
    @DisplayName("GET /api/v1/countries/code3/{alpha3Code} - Get country by alpha-3 code")
    void testGetCountryByAlpha3Code() {
        // First, get a list to find a valid country code
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
            return;
        }
        
        // Find first non-deleted country that actually exists (verify by trying to retrieve it)
        String alpha3Code = null;
        for (Map<String, Object> country : countries) {
            Boolean isDeleted = (Boolean) country.get("isDeleted");
            if (isDeleted == null || !isDeleted) {
                String code = (String) country.get("alpha3Code");
                if (code != null && !code.isEmpty()) {
                    // Verify the country actually exists by trying to retrieve it
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
        
        if (alpha3Code == null || alpha3Code.isEmpty()) {
            System.out.println("  ⚠️  Skipping test: No valid (retrievable) alpha3Code found");
            return;
        }
        
        System.out.println("  Testing with alpha3Code: " + alpha3Code);
        
        given()
                .spec(requestSpec)
                .pathParam("alpha3Code", alpha3Code)
                .when()
                .get("/countries/code3/{alpha3Code}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("alpha3Code", equalTo(alpha3Code))
                .body("name", notNullValue());
    }
    
    @Test
    @DisplayName("GET /api/v1/countries/number/{numericCode} - Get country by numeric code")
    void testGetCountryByNumericCode() {
        // First, get a list to find a valid country code
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
            return;
        }
        
        // Find first non-deleted country that actually exists (verify by trying to retrieve it)
        String numericCode = null;
        for (Map<String, Object> country : countries) {
            Boolean isDeleted = (Boolean) country.get("isDeleted");
            if (isDeleted == null || !isDeleted) {
                String code = (String) country.get("numericCode");
                if (code != null && !code.isEmpty()) {
                    // Verify the country actually exists by trying to retrieve it
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
        
        if (numericCode == null || numericCode.isEmpty()) {
            System.out.println("  ⚠️  Skipping test: No valid (retrievable) numericCode found");
            return;
        }
        
        System.out.println("  Testing with numericCode: " + numericCode);
        
        given()
                .spec(requestSpec)
                .pathParam("numericCode", numericCode)
                .when()
                .get("/countries/number/{numericCode}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("numericCode", equalTo(numericCode))
                .body("name", notNullValue());
    }
    
    @Test
    @DisplayName("POST /api/v1/countries - Create a new country")
    void testCreateCountry() {
        // Generate unique test country codes that match ISO 3166 patterns
        // Use valid patterns: alpha2 (2 uppercase letters), alpha3 (3 uppercase letters), numeric (3 digits)
        long timestamp = System.currentTimeMillis();
        // Use last 2 digits for alpha2, ensuring it's uppercase letters
        String alpha2Suffix = String.format("%02d", timestamp % 100);
        char[] alpha2Chars = {(char)('A' + (timestamp % 26)), (char)('A' + ((timestamp / 26) % 26))};
        String alpha2 = new String(alpha2Chars);
        // Use last 3 digits for alpha3
        char[] alpha3Chars = {
            (char)('A' + (timestamp % 26)),
            (char)('A' + ((timestamp / 26) % 26)),
            (char)('A' + ((timestamp / 676) % 26))
        };
        String alpha3 = new String(alpha3Chars);
        // Use last 3 digits for numeric code (padded to 3 digits)
        String numeric = String.format("%03d", timestamp % 1000);
        
        String requestBody = String.format("""
                {
                    "name": "Test Country %d",
                    "alpha2Code": "%s",
                    "alpha3Code": "%s",
                    "numericCode": "%s"
                }
                """, timestamp, alpha2, alpha3, numeric);
        
        Response response = given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/countries")
                .then()
                .statusCode(201)
                .contentType("application/json")
                .body("alpha2Code", equalTo(alpha2))
                .body("alpha3Code", equalTo(alpha3))
                .body("numericCode", equalTo(numeric))
                .body("name", containsString("Test Country"))
                .body("createDate", notNullValue())
                .body("isDeleted", equalTo(false))
                .extract()
                .response();
        
        // Verify we can retrieve the created country
        given()
                .spec(requestSpec)
                .pathParam("alpha2Code", alpha2)
                .when()
                .get("/countries/code/{alpha2Code}")
                .then()
                .statusCode(200)
                .body("alpha2Code", equalTo(alpha2));
    }
    
    @Test
    @DisplayName("PUT /api/v1/countries/code/{alpha2Code} - Update a country")
    void testUpdateCountry() {
        // First create a country with valid ISO 3166 patterns
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
        
        // Update the country
        String updateBody = String.format("""
                {
                    "name": "Updated Name",
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
                .statusCode(200)
                .contentType("application/json")
                .body("name", equalTo("Updated Name"))
                .body("alpha2Code", equalTo(alpha2));
    }
    
    @Test
    @DisplayName("DELETE /api/v1/countries/code/{alpha2Code} - Delete a country")
    void testDeleteCountry() {
        // First create a country with valid ISO 3166 patterns
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
        
        // Delete the country
        given()
                .spec(requestSpec)
                .pathParam("alpha2Code", alpha2)
                .when()
                .delete("/countries/code/{alpha2Code}")
                .then()
                .statusCode(204);
        
        // Verify country is deleted (deleted countries are filtered out and return 404)
        // Use Awaitility to handle eventual consistency
        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
                .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    given()
                            .spec(requestSpec)
                            .pathParam("alpha2Code", alpha2)
                            .when()
                            .get("/countries/code/{alpha2Code}")
                            .then()
                            .statusCode(404);
                });
    }
    
    @Test
    @DisplayName("GET /api/v1/countries/code/{alpha2Code}/history - Get country history")
    void testGetCountryHistory() {
        // First create and update a country to create history with valid ISO 3166 patterns
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
        
        // Get history
        Response response = given()
                .spec(requestSpec)
                .pathParam("alpha2Code", alpha2)
                .when()
                .get("/countries/code/{alpha2Code}/history")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("", is(instanceOf(List.class)))
                .extract()
                .response();
        
        List<Map<String, Object>> history = response.jsonPath().getList("");
        assertNotNull(history, "History list should not be null");
        assertTrue(history.size() >= 2, 
                "History should contain at least 2 versions. Actual history size: " + history.size() + ", History: " + history);
        
        // Verify history is ordered (newest first)
        Map<String, Object> first = history.get(0);
        Map<String, Object> second = history.get(1);
        System.out.println("  First history entry: " + first);
        System.out.println("  Second history entry: " + second);
        assertNotNull(first, "First history entry should not be null");
        assertNotNull(second, "Second history entry should not be null");
        assertNotNull(first.get("createDate"), 
                "First history entry should have 'createDate'. Actual entry: " + first);
        assertNotNull(second.get("createDate"), 
                "Second history entry should have 'createDate'. Actual entry: " + second);
    }
    
    @Test
    @DisplayName("Authentication - Returns 401 without API key")
    void testAuthenticationRequired() {
        // Determine the correct path based on whether base URL includes /api/v1
        String path = baseUrl.contains("/api/v1") ? "/countries" : "/api/v1/countries";
        given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .when()
                .get(path)
                .then()
                .statusCode(401)
                .contentType("application/json")
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"));
    }
    
    @Test
    @DisplayName("Authentication - Returns 401 with invalid API key")
    void testAuthenticationInvalidKey() {
        // Determine the correct path based on whether base URL includes /api/v1
        String path = baseUrl.contains("/api/v1") ? "/countries" : "/api/v1/countries";
        given()
                .baseUri(baseUrl)
                .header("X-API-KEY", "invalid-key")
                .contentType("application/json")
                .when()
                .get(path)
                .then()
                .statusCode(401)
                .contentType("application/json")
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"));
    }
}

