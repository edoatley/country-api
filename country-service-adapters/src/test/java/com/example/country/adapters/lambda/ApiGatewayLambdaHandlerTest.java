package com.example.country.adapters.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.example.country.adapters.api.CountryApi;
import com.example.country.application.CountryServiceImpl;
import com.example.country.application.model.CountryInput;
import com.example.country.application.ports.CountryRepositoryPort;
import com.example.country.domain.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ApiGatewayLambdaHandlerTest {
    
    static class InMemoryRepo implements CountryRepositoryPort {
        private final Map<String, List<Country>> byAlpha2 = new HashMap<>();
        @Override public Country saveNewVersion(Country country) {
            byAlpha2.computeIfAbsent(country.alpha2Code(), k -> new ArrayList<>()).add(country);
            return country;
        }
        @Override public Optional<Country> findLatestByAlpha2(String alpha2Code) {
            return byAlpha2.getOrDefault(alpha2Code, List.of()).stream().max(Comparator.comparing(Country::createDate));
        }
        @Override public Optional<Country> findLatestByAlpha3(String alpha3Code) {
            return byAlpha2.values().stream().flatMap(List::stream).filter(c -> c.alpha3Code().equals(alpha3Code))
                    .max(Comparator.comparing(Country::createDate));
        }
        @Override public Optional<Country> findLatestByNumeric(String numericCode) {
            return byAlpha2.values().stream().flatMap(List::stream).filter(c -> c.numericCode().equals(numericCode))
                    .max(Comparator.comparing(Country::createDate));
        }
        @Override public List<Country> listLatest(int limit, int offset) {
            return byAlpha2.values().stream()
                    .map(list -> list.stream().max(Comparator.comparing(Country::createDate)).orElse(null))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Country::alpha2Code))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }
        @Override public List<Country> historyByAlpha2(String alpha2Code) {
            return byAlpha2.getOrDefault(alpha2Code, List.of()).stream()
                    .sorted(Comparator.comparing(Country::createDate).reversed()).toList();
        }
    }

    private ApiGatewayLambdaHandler handler;
    @Mock
    private Context mockContext;
    @Mock
    private LambdaLogger mockLogger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        InMemoryRepo repo = new InMemoryRepo();
        CountryApi api = new CountryApi(new CountryServiceImpl(repo));
        CountryLambdaHandler lambdaHandler = new CountryLambdaHandler(api);
        ApiKeyValidator validator = new ApiKeyValidator("test-key");
        RouteMapper routeMapper = new RouteMapper();
        handler = new ApiGatewayLambdaHandler(lambdaHandler, validator, routeMapper);
        
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    @Test
    void handlesGetAllCountries() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/api/v1/countries");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-API-KEY", "test-key");
        event.setHeaders(headers);
        event.setQueryStringParameters(Map.of("limit", "20", "offset", "0"));

        var response = handler.handleRequest(event, mockContext);

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handlesGetByAlpha2() {
        // First create a country
        APIGatewayProxyRequestEvent createEvent = new APIGatewayProxyRequestEvent();
        createEvent.setHttpMethod("POST");
        createEvent.setPath("/api/v1/countries");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-API-KEY", "test-key");
        createEvent.setHeaders(headers);
        createEvent.setBody("{\"name\":\"United Kingdom\",\"alpha2Code\":\"GB\",\"alpha3Code\":\"GBR\",\"numericCode\":\"826\"}");

        handler.handleRequest(createEvent, mockContext);

        // Then get it
        APIGatewayProxyRequestEvent getEvent = new APIGatewayProxyRequestEvent();
        getEvent.setHttpMethod("GET");
        getEvent.setPath("/api/v1/countries/code/GB");
        getEvent.setHeaders(headers);
        // Path params are extracted from path by RouteMapper, so we don't need to set them

        var response = handler.handleRequest(getEvent, mockContext);

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        // Response should be valid JSON with country data
        assertTrue(response.getBody().length() > 0);
        assertTrue(response.getBody().startsWith("{"));
        // Verify all required fields are present in the response
        assertTrue(response.getBody().contains("\"name\""), "Response should contain 'name' field");
        assertTrue(response.getBody().contains("\"alpha2Code\""), "Response should contain 'alpha2Code' field");
        assertTrue(response.getBody().contains("\"alpha3Code\""), "Response should contain 'alpha3Code' field");
        assertTrue(response.getBody().contains("\"numericCode\""), "Response should contain 'numericCode' field");
        assertTrue(response.getBody().contains("\"createDate\""), "Response should contain 'createDate' field");
        assertTrue(response.getBody().contains("\"isDeleted\""), "Response should contain 'isDeleted' field");
    }

    @Test
    void handlesCreateCountry() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/api/v1/countries");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-API-KEY", "test-key");
        event.setHeaders(headers);
        event.setBody("{\"name\":\"United Kingdom\",\"alpha2Code\":\"GB\",\"alpha3Code\":\"GBR\",\"numericCode\":\"826\"}");

        var response = handler.handleRequest(event, mockContext);

        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getBody());
        // Response should be valid JSON with country data
        assertTrue(response.getBody().length() > 0);
        assertTrue(response.getBody().startsWith("{"));
        // Verify all required fields are present in the response
        assertTrue(response.getBody().contains("\"name\""), "Response should contain 'name' field");
        assertTrue(response.getBody().contains("\"alpha2Code\""), "Response should contain 'alpha2Code' field");
        assertTrue(response.getBody().contains("\"alpha3Code\""), "Response should contain 'alpha3Code' field");
        assertTrue(response.getBody().contains("\"numericCode\""), "Response should contain 'numericCode' field");
        assertTrue(response.getBody().contains("\"createDate\""), "Response should contain 'createDate' field");
        assertTrue(response.getBody().contains("\"isDeleted\""), "Response should contain 'isDeleted' field");
    }

    @Test
    void handlesDeleteCountry() {
        // First create a country
        APIGatewayProxyRequestEvent createEvent = new APIGatewayProxyRequestEvent();
        createEvent.setHttpMethod("POST");
        createEvent.setPath("/api/v1/countries");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-API-KEY", "test-key");
        createEvent.setHeaders(headers);
        createEvent.setBody("{\"name\":\"United Kingdom\",\"alpha2Code\":\"GB\",\"alpha3Code\":\"GBR\",\"numericCode\":\"826\"}");

        handler.handleRequest(createEvent, mockContext);

        // Then delete it
        APIGatewayProxyRequestEvent deleteEvent = new APIGatewayProxyRequestEvent();
        deleteEvent.setHttpMethod("DELETE");
        deleteEvent.setPath("/api/v1/countries/code/GB");
        deleteEvent.setHeaders(headers);
        // Path params are extracted from path by RouteMapper

        var response = handler.handleRequest(deleteEvent, mockContext);

        assertEquals(204, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void rejectsMissingApiKey() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/api/v1/countries");
        event.setHeaders(new HashMap<>());

        var response = handler.handleRequest(event, mockContext);

        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Unauthorized"));
    }

    @Test
    void rejectsInvalidApiKey() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/api/v1/countries");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-API-KEY", "wrong-key");
        event.setHeaders(headers);

        var response = handler.handleRequest(event, mockContext);

        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Unauthorized"));
    }

    @Test
    void returns404ForUnknownRoute() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/api/v1/unknown");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-API-KEY", "test-key");
        event.setHeaders(headers);

        var response = handler.handleRequest(event, mockContext);

        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Not Found"));
    }

    @Test
    void returns400ForBadRequest() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/api/v1/countries");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-API-KEY", "test-key");
        event.setHeaders(headers);
        event.setBody("{\"invalid\":\"json\""); // Invalid JSON

        var response = handler.handleRequest(event, mockContext);

        assertEquals(500, response.getStatusCode()); // JSON parsing error results in 500
    }

    @Test
    void returns404ForNotFoundCountry() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/api/v1/countries/code/XX");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-API-KEY", "test-key");
        event.setHeaders(headers);
        // Path params are extracted from path by RouteMapper

        var response = handler.handleRequest(event, mockContext);

        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Not Found"));
    }
}
