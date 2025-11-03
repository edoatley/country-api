package com.example.country.bootstrap.config;

import com.example.country.adapters.api.CountryApi;
import com.example.country.adapters.persistence.DynamoDbCountryRepository;
import com.example.country.application.CountryServiceImpl;
import com.example.country.application.ports.CountryRepositoryPort;
import com.example.country.application.ports.CountryServicePort;
import com.example.country.domain.Country;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CountryServiceConfigurationTest {

    private CountryServiceConfiguration configuration;
    
    @BeforeEach
    void setUp() {
        configuration = new CountryServiceConfiguration();
        ReflectionTestUtils.setField(configuration, "awsEndpointUrl", "http://localhost:4566");
        ReflectionTestUtils.setField(configuration, "awsRegion", "us-east-1");
    }

    @Test
    void shouldCreateDynamoDbClientWithEndpointOverride() {
        DynamoDbClient client = configuration.dynamoDbClient();
        
        assertNotNull(client);
    }

    @Test
    void shouldCreateDynamoDbClientWithoutEndpointOverride() {
        ReflectionTestUtils.setField(configuration, "awsEndpointUrl", "");
        
        DynamoDbClient client = configuration.dynamoDbClient();
        
        assertNotNull(client);
    }

    @Test
    void shouldCreateDynamoDbClientWithNullEndpoint() {
        ReflectionTestUtils.setField(configuration, "awsEndpointUrl", null);
        
        DynamoDbClient client = configuration.dynamoDbClient();
        
        assertNotNull(client);
    }

    @Test
    void shouldCreateCountryRepository() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        
        CountryRepositoryPort repository = configuration.countryRepository(client);
        
        assertNotNull(repository);
        assertInstanceOf(DynamoDbCountryRepository.class, repository);
    }

    @Test
    void shouldCreateCountryService() {
        CountryRepositoryPort repository = mock(CountryRepositoryPort.class);
        
        CountryServicePort service = configuration.countryService(repository);
        
        assertNotNull(service);
        assertInstanceOf(CountryServiceImpl.class, service);
    }

    @Test
    void shouldCreateCountryApi() {
        CountryServicePort service = mock(CountryServicePort.class);
        
        CountryApi api = configuration.countryApi(service);
        
        assertNotNull(api);
    }

    @Test
    void shouldCreateObjectMapperWithMixIn() throws Exception {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        builder.modules(new JavaTimeModule());
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        ObjectMapper mapper = configuration.objectMapper(builder);
        
        assertNotNull(mapper);
        
        // Verify that Country objects can be serialized
        Country country = Country.of("United Kingdom", "GB", "GBR", "826", 
                Instant.now(), null, false);
        
        String json = mapper.writeValueAsString(country);
        assertNotNull(json);
        assertTrue(json.contains("GB"));
        assertTrue(json.contains("GBR"));
        assertTrue(json.contains("826"));
    }

    @Test
    void shouldConfigureObjectMapperWithJavaTimeModule() throws Exception {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        builder.modules(new JavaTimeModule());
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        ObjectMapper mapper = configuration.objectMapper(builder);
        
        assertNotNull(mapper.getRegisteredModuleIds());
        // Verify Instant serialization works
        Country country = Country.of("United Kingdom", "GB", "GBR", "826", 
                Instant.parse("2024-01-01T00:00:00Z"), null, false);
        
        String json = mapper.writeValueAsString(country);
        assertTrue(json.contains("2024-01-01"));
    }
}
