package com.example.country.application.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Input model for creating or updating a country.
 */
@Schema(name = "CountryInput", description = "The data required to create or update a country.")
public record CountryInput(
        @Schema(description = "The official name of the country.", required = true)
        String name,
        
        @Schema(description = "The ISO 3166-1 alpha-2 code.", 
                pattern = "^[A-Z]{2}$", 
                required = true)
        String alpha2Code,
        
        @Schema(description = "The ISO 3166-1 alpha-3 code.", 
                pattern = "^[A-Z]{3}$", 
                required = true)
        String alpha3Code,
        
        @Schema(description = "The ISO 3166-1 numeric code.", 
                pattern = "^[0-9]{3}$", 
                required = true)
        String numericCode
) {}
