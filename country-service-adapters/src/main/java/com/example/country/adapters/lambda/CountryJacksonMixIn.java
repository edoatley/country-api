package com.example.country.adapters.lambda;

import com.example.country.domain.Country;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson MixIn to properly serialize Country class accessor methods.
 * This allows Jackson to serialize Country without adding annotations to the domain class.
 * Used by the Lambda handler's ObjectMapper.
 * 
 * Using @JsonProperty instead of @JsonGetter to ensure proper serialization.
 */
public abstract class CountryJacksonMixIn {
    @JsonProperty("name")
    abstract String name();
    
    @JsonProperty("alpha2Code")
    abstract String alpha2Code();
    
    @JsonProperty("alpha3Code")
    abstract String alpha3Code();
    
    @JsonProperty("numericCode")
    abstract String numericCode();
    
    @JsonProperty("createDate")
    abstract java.time.Instant createDate();
    
    @JsonProperty("expiryDate")
    abstract java.time.Instant expiryDate();
    
    @JsonProperty("isDeleted")
    abstract boolean isDeleted();
}

