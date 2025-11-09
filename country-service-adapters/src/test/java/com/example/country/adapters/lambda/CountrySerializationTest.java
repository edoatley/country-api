package com.example.country.adapters.lambda;

import com.example.country.domain.Country;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CountrySerializationTest {

    @Test
    void shouldSerializeCountryWithAllFields() throws Exception {
        // Create ObjectMapper with the same configuration as ApiGatewayLambdaHandler
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.addMixIn(Country.class, CountryJacksonMixIn.class);
        
        // Create a Country object
        Country country = Country.of("United Kingdom", "GB", "GBR", "826", 
                Instant.now(), null, false);
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(country);
        
        // Verify all fields are present
        assertNotNull(json);
        assertTrue(json.contains("\"name\""), "JSON should contain 'name' field");
        assertTrue(json.contains("\"alpha2Code\""), "JSON should contain 'alpha2Code' field");
        assertTrue(json.contains("\"alpha3Code\""), "JSON should contain 'alpha3Code' field");
        assertTrue(json.contains("\"numericCode\""), "JSON should contain 'numericCode' field");
        assertTrue(json.contains("\"createDate\""), "JSON should contain 'createDate' field");
        assertTrue(json.contains("\"isDeleted\""), "JSON should contain 'isDeleted' field");
        
        // Verify the actual values
        assertTrue(json.contains("\"United Kingdom\""), "JSON should contain country name");
        assertTrue(json.contains("\"GB\""), "JSON should contain alpha2Code");
        assertTrue(json.contains("\"GBR\""), "JSON should contain alpha3Code");
        assertTrue(json.contains("\"826\""), "JSON should contain numericCode");
        assertTrue(json.contains("\"isDeleted\":false"), "JSON should contain isDeleted field");
        
        System.out.println("Serialized JSON: " + json);
    }
    
    @Test
    void shouldSerializeListOfCountries() throws Exception {
        // Create ObjectMapper with the same configuration as ApiGatewayLambdaHandler
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.addMixIn(Country.class, CountryJacksonMixIn.class);
        
        // Create multiple Country objects
        Country country1 = Country.of("United Kingdom", "GB", "GBR", "826", 
                Instant.now(), null, false);
        Country country2 = Country.of("United States", "US", "USA", "840", 
                Instant.now(), null, false);
        
        // Serialize list to JSON
        String json = objectMapper.writeValueAsString(java.util.List.of(country1, country2));
        
        // Verify all fields are present for each country
        assertNotNull(json);
        assertTrue(json.contains("\"name\""), "JSON should contain 'name' field");
        assertTrue(json.contains("\"alpha2Code\""), "JSON should contain 'alpha2Code' field");
        assertTrue(json.contains("\"alpha3Code\""), "JSON should contain 'alpha3Code' field");
        assertTrue(json.contains("\"numericCode\""), "JSON should contain 'numericCode' field");
        assertTrue(json.contains("\"createDate\""), "JSON should contain 'createDate' field");
        assertTrue(json.contains("\"isDeleted\""), "JSON should contain 'isDeleted' field");
        
        System.out.println("Serialized JSON: " + json);
    }
}

