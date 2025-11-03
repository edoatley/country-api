package com.example.country.bootstrap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "data.seeding.enabled=true",
        "api.key=test-api-key",
        "spring.main.lazy-initialization=false"
})
@Testcontainers
class CountryServiceApplicationIntegrationTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .withStartupTimeout(java.time.Duration.ofSeconds(120));

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext applicationContext;

    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    private HttpHeaders createHeadersWithApiKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", "test-api-key");
        return headers;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Testcontainers starts the container automatically with @Container
        registry.add("aws.endpoint.url", () -> {
            URI endpoint = localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB);
            return endpoint != null ? endpoint.toString() : "http://localhost:4566";
        });
        registry.add("aws.region", localStack::getRegion);
    }

    @Test
    void applicationStartsAndSeedsData() throws Exception {
        // Create TestRestTemplate with proper timeout settings
        restTemplate = new TestRestTemplate(new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10)));

        // Wait for application to be ready and data seeding to complete using Actuator health endpoint
        // The health endpoint will return UP only when seeding is complete (if enabled)
        await()
                .atMost(120, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
                                getBaseUrl() + "/actuator/health",
                                String.class
                        );
                        if (healthResponse.getStatusCode().is2xxSuccessful() && healthResponse.getBody() != null) {
                            String body = healthResponse.getBody();
                            // Health endpoint returns UP when seeding is complete
                            return body.contains("\"status\":\"UP\"") || body.contains("\"status\":\"up\"");
                        }
                        return false;
                    } catch (Exception e) {
                        // Health endpoint not ready yet, continue waiting
                        return false;
                    }
                });

        // Verify we can list countries and get data back
        HttpEntity<?> listEntity = new HttpEntity<>(createHeadersWithApiKey());
        ResponseEntity<String> listResponse;
        try {
            listResponse = restTemplate.exchange(
                    getBaseUrl() + "/api/v1/countries?limit=5&offset=0",
                    HttpMethod.GET,
                    listEntity,
                    String.class
            );
        } catch (Exception e) {
            fail("Failed to call list countries endpoint: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return; // Unreachable but satisfies compiler
        }
        
        assertEquals(200, listResponse.getStatusCode().value(), 
                "Expected 200 but got " + listResponse.getStatusCode().value() + ". Response body: " + listResponse.getBody());
        assertNotNull(listResponse.getBody());
        assertTrue(listResponse.getBody().startsWith("[") || listResponse.getBody().contains("alpha2Code"),
                "Response body should be a JSON array or contain 'alpha2Code'. Body: " + listResponse.getBody());

        // Verify we can get a specific country (GB should exist in the CSV)
        HttpEntity<?> getEntity = new HttpEntity<>(createHeadersWithApiKey());
        ResponseEntity<String> getResponse = restTemplate.exchange(
                getBaseUrl() + "/api/v1/countries/code/GB",
                HttpMethod.GET,
                getEntity,
                String.class
        );
        assertEquals(200, getResponse.getStatusCode().value(),
                "Expected 200 but got " + getResponse.getStatusCode().value() + ". Response body: " + getResponse.getBody());
        assertNotNull(getResponse.getBody());
        assertTrue(getResponse.getBody().contains("GB"),
                "Response body should contain 'GB'. Body: " + getResponse.getBody());
        assertTrue(getResponse.getBody().contains("GBR"),
                "Response body should contain 'GBR'. Body: " + getResponse.getBody());
    }

    @Test
    void apiKeyAuthenticationEnforced() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Wait for application to be ready first
        await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(getBaseUrl() + "/api/v1/countries?limit=1&offset=0"))
                                .header("X-API-KEY", "test-api-key")
                                .GET()
                                .timeout(Duration.ofSeconds(2))
                                .build();

                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        return response.statusCode() == 200;
                    } catch (Exception e) {
                        return false;
                    }
                });

        // Make a request without API key
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/v1/countries"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("Unauthorized") || response.body().contains("API key"));
    }
}
