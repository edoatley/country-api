package com.example.country.adapters.web.controller;

import com.example.country.adapters.api.CountryApi;
import com.example.country.application.model.CountryInput;
import com.example.country.domain.Country;
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
    public ResponseEntity<List<Country>> getAllCountries(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(countryApi.listCountries(limit, offset));
    }

    @PostMapping
    public ResponseEntity<Country> createCountry(@RequestBody CountryInput input) {
        Country created = countryApi.createCountry(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/code/{alpha2Code}")
    public ResponseEntity<Country> getByAlpha2(@PathVariable String alpha2Code) {
        Country country = countryApi.getByAlpha2(alpha2Code);
        return ResponseEntity.ok(country);
    }

    @PutMapping("/code/{alpha2Code}")
    public ResponseEntity<Country> updateByAlpha2(
            @PathVariable String alpha2Code,
            @RequestBody CountryInput input) {
        Country updated = countryApi.updateByAlpha2(alpha2Code, input);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/code/{alpha2Code}")
    public ResponseEntity<Void> deleteByAlpha2(@PathVariable String alpha2Code) {
        countryApi.deleteByAlpha2(alpha2Code);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/code/{alpha2Code}/history")
    public ResponseEntity<List<Country>> getHistory(@PathVariable String alpha2Code) {
        List<Country> history = countryApi.historyByAlpha2(alpha2Code);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/code3/{alpha3Code}")
    public ResponseEntity<Country> getByAlpha3(@PathVariable String alpha3Code) {
        Country country = countryApi.getByAlpha3(alpha3Code);
        return ResponseEntity.ok(country);
    }

    @GetMapping("/number/{numericCode}")
    public ResponseEntity<Country> getByNumeric(@PathVariable String numericCode) {
        Country country = countryApi.getByNumeric(numericCode);
        return ResponseEntity.ok(country);
    }
}
