package com.example.country.bootstrap.config;

import com.example.country.domain.Country;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson MixIn to properly serialize Country class accessor methods.
 * This allows Jackson to serialize Country without adding annotations to the domain class.
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

