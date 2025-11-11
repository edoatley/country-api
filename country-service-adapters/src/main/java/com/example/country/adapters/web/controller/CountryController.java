package com.example.country.adapters.web.controller;

import com.example.country.adapters.api.CountryApi;
import com.example.country.application.model.CountryInput;
import com.example.country.domain.Country;
import com.example.country.adapters.web.exception.GlobalExceptionHandler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/countries")
public class CountryController {
    private final CountryApi countryApi;

    public CountryController(CountryApi countryApi) {
        this.countryApi = countryApi;
    }

    @GetMapping
    @Operation(summary = "Get All Countries (Paginated)", description = "Retrieves a paginated list of the latest version of all country records.")
    @ApiResponse(responseCode = "200", description = "A paginated list of countries")
    @ApiResponse(responseCode = "401", description = "Unauthorized. The API key is missing or invalid.",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal Server Error",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<Country>> getAllCountries(
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset) {
        return ResponseEntity.ok(countryApi.listCountries(limit, offset));
    }

    @PostMapping
    @Operation(summary = "Create a New Country", description = "Adds a new country record to the system. The combination of alpha2, alpha3, and numeric codes must be unique.")
    @ApiResponse(responseCode = "201", description = "Country created successfully")
    @ApiResponse(responseCode = "400", description = "Bad Request",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized. The API key is missing or invalid.",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflict. A country with the given code(s) already exists.",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal Server Error",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Country> createCountry(@RequestBody CountryInput input) {
        Country created = countryApi.createCountry(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/code/{alpha2Code}")
    @Operation(summary = "Get Country by 2-Letter Code", description = "Retrieves the latest version of a country by its ISO 3166-1 alpha-2 code.")
    @ApiResponse(responseCode = "200", description = "The requested country data")
    @ApiResponse(responseCode = "401", description = "Unauthorized. The API key is missing or invalid.",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not Found",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal Server Error",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Country> getByAlpha2(@PathVariable("alpha2Code") String alpha2Code) {
        Country country = countryApi.getByAlpha2(alpha2Code);
        return ResponseEntity.ok(country);
    }

    @PutMapping("/code/{alpha2Code}")
    @Operation(summary = "Update an Existing Country", description = "Modifies an existing country record using its primary `alpha2Code` identifier. This action creates a new version of the data.")
    @ApiResponse(responseCode = "200", description = "Country updated successfully")
    @ApiResponse(responseCode = "400", description = "Bad Request",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized. The API key is missing or invalid.",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not Found",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal Server Error",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Country> updateByAlpha2(
            @PathVariable("alpha2Code") String alpha2Code,
            @RequestBody CountryInput input) {
        Country updated = countryApi.updateByAlpha2(alpha2Code, input);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/code/{alpha2Code}")
    @Operation(summary = "Logically Delete a Country", description = "Marks a country record as deleted using its primary `alpha2Code` identifier. This is a logical deletion; the record and its history are preserved.")
    @ApiResponse(responseCode = "204", description = "Country deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized. The API key is missing or invalid.",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not Found",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal Server Error",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteByAlpha2(@PathVariable("alpha2Code") String alpha2Code) {
        countryApi.deleteByAlpha2(alpha2Code);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/code/{alpha2Code}/history")
    @Operation(summary = "Get Country Version History", description = "Retrieves the complete, ordered version history for a specific country.")
    @ApiResponse(responseCode = "200", description = "A list of all versions of the country, ordered from newest to oldest")
    @ApiResponse(responseCode = "401", description = "Unauthorized. The API key is missing or invalid.",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not Found",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal Server Error",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<Country>> getHistory(@PathVariable("alpha2Code") String alpha2Code) {
        List<Country> history = countryApi.historyByAlpha2(alpha2Code);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/code3/{alpha3Code}")
    @Operation(summary = "Get Country by 3-Letter Code", description = "Retrieves the latest version of a country by its ISO 3166-1 alpha-3 code.")
    @ApiResponse(responseCode = "200", description = "The requested country data")
    @ApiResponse(responseCode = "401", description = "Unauthorized. The API key is missing or invalid.",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not Found",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal Server Error",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Country> getByAlpha3(@PathVariable("alpha3Code") String alpha3Code) {
        Country country = countryApi.getByAlpha3(alpha3Code);
        return ResponseEntity.ok(country);
    }

    @GetMapping("/number/{numericCode}")
    @Operation(summary = "Get Country by Numeric Code", description = "Retrieves the latest version of a country by its ISO 3166-1 numeric code.")
    @ApiResponse(responseCode = "200", description = "The requested country data")
    @ApiResponse(responseCode = "401", description = "Unauthorized. The API key is missing or invalid.",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not Found",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal Server Error",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Country> getByNumeric(@PathVariable("numericCode") String numericCode) {
        Country country = countryApi.getByNumeric(numericCode);
        return ResponseEntity.ok(country);
    }
}
