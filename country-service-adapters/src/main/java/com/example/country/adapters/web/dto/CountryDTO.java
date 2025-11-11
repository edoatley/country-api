package com.example.country.adapters.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * DTO for Country with OpenAPI schema annotations.
 * This is used by SpringDoc to generate the OpenAPI schema documentation
 * for the Country domain class without modifying the domain class itself.
 * 
 * The actual API returns Country domain objects, but SpringDoc will use
 * this DTO's schema definition for documentation.
 */
@Schema(name = "Country", description = "Represents a country record, including system-generated fields.")
public class CountryDTO {
    
    @Schema(description = "The official name of the country", 
            example = "United Kingdom", 
            required = true)
    private String name;
    
    @Schema(description = "The ISO 3166-1 alpha-2 code", 
            example = "GB", 
            pattern = "^[A-Z]{2}$", 
            required = true)
    private String alpha2Code;
    
    @Schema(description = "The ISO 3166-1 alpha-3 code", 
            example = "GBR", 
            pattern = "^[A-Z]{3}$", 
            required = true)
    private String alpha3Code;
    
    @Schema(description = "The ISO 3166-1 numeric code", 
            example = "826", 
            pattern = "^[0-9]{3}$", 
            required = true)
    private String numericCode;
    
    @Schema(description = "The UTC timestamp when this version of the record was created", 
            example = "2025-09-20T15:20:05Z", 
            format = "date-time", 
            required = true)
    private Instant createDate;
    
    @Schema(description = "The UTC timestamp when this version of the record became obsolete. Null for the current active version.", 
            example = "null", 
            format = "date-time", 
            nullable = true)
    private Instant expiryDate;
    
    @Schema(description = "Flag indicating if the country is logically deleted", 
            example = "false", 
            defaultValue = "false", 
            required = true)
    private boolean isDeleted;
    
    // Getters and setters for SpringDoc to recognize the schema
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getAlpha2Code() { return alpha2Code; }
    public void setAlpha2Code(String alpha2Code) { this.alpha2Code = alpha2Code; }
    
    public String getAlpha3Code() { return alpha3Code; }
    public void setAlpha3Code(String alpha3Code) { this.alpha3Code = alpha3Code; }
    
    public String getNumericCode() { return numericCode; }
    public void setNumericCode(String numericCode) { this.numericCode = numericCode; }
    
    public Instant getCreateDate() { return createDate; }
    public void setCreateDate(Instant createDate) { this.createDate = createDate; }
    
    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }
    
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}

