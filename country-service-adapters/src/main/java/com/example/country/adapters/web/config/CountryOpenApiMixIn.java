package com.example.country.adapters.web.config;

import com.example.country.domain.Country;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * SpringDoc OpenAPI MixIn to document Country schema for OpenAPI generation.
 * This allows SpringDoc to generate proper schema documentation for the Country domain class
 * without adding annotations directly to the domain class.
 * 
 * Similar to CountryJacksonMixIn but for OpenAPI documentation.
 */
@Schema(name = "Country", description = "Represents a country record, including system-generated fields.")
public abstract class CountryOpenApiMixIn {
    
    @Schema(description = "The official name of the country", example = "United Kingdom", required = true)
    abstract String name();
    
    @Schema(description = "The ISO 3166-1 alpha-2 code", example = "GB", pattern = "^[A-Z]{2}$", required = true)
    abstract String alpha2Code();
    
    @Schema(description = "The ISO 3166-1 alpha-3 code", example = "GBR", pattern = "^[A-Z]{3}$", required = true)
    abstract String alpha3Code();
    
    @Schema(description = "The ISO 3166-1 numeric code", example = "826", pattern = "^[0-9]{3}$", required = true)
    abstract String numericCode();
    
    @Schema(description = "The UTC timestamp when this version of the record was created", 
            example = "2025-09-20T15:20:05Z", 
            format = "date-time", 
            required = true)
    abstract java.time.Instant createDate();
    
    @Schema(description = "The UTC timestamp when this version of the record became obsolete. Null for the current active version.", 
            example = "null", 
            format = "date-time", 
            nullable = true)
    abstract java.time.Instant expiryDate();
    
    @Schema(description = "Flag indicating if the country is logically deleted", 
            example = "false", 
            defaultValue = "false", 
            required = true)
    abstract boolean isDeleted();
}

