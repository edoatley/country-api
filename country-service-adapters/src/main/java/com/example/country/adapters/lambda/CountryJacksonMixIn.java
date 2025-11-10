package com.example.country.adapters.lambda;

import com.example.country.domain.Country;
import com.fasterxml.jackson.annotation.JsonGetter;

/**
 * Jackson MixIn to properly serialize Country class accessor methods.
 * This allows Jackson to serialize Country without adding annotations to the domain class.
 * Used by the Lambda handler's ObjectMapper.
 * 
 * This matches the configuration in country-service-bootstrap CountryJacksonMixIn.
 */
public abstract class CountryJacksonMixIn {
    @JsonGetter("name")
    abstract String name();
    
    @JsonGetter("alpha2Code")
    abstract String alpha2Code();
    
    @JsonGetter("alpha3Code")
    abstract String alpha3Code();
    
    @JsonGetter("numericCode")
    abstract String numericCode();
    
    @JsonGetter("createDate")
    abstract java.time.Instant createDate();
    
    @JsonGetter("expiryDate")
    abstract java.time.Instant expiryDate();
    
    @JsonGetter("isDeleted")
    abstract boolean isDeleted();
}

