package com.example.country.adapters.api;

import com.example.country.application.model.CountryInput;
import com.example.country.application.ports.CountryServicePort;
import com.example.country.domain.Country;

import java.util.List;
import java.util.Objects;

public class CountryApi {
    private final CountryServicePort service;

    public CountryApi(CountryServicePort service) {
        this.service = Objects.requireNonNull(service);
    }

    public List<Country> listCountries(int limit, int offset) {
        return service.listAll(limit, offset);
    }

    public Country createCountry(CountryInput input) {
        return service.create(input);
    }

    public Country getByAlpha2(String alpha2) {
        return service.getByAlpha2(alpha2);
    }

    public Country updateByAlpha2(String alpha2, CountryInput input) {
        return service.updateByAlpha2(alpha2, input);
    }

    public void deleteByAlpha2(String alpha2) {
        service.deleteByAlpha2(alpha2);
    }

    public Country getByAlpha3(String alpha3) {
        return service.getByAlpha3(alpha3);
    }

    public Country getByNumeric(String numeric) {
        return service.getByNumeric(numeric);
    }

    public List<Country> historyByAlpha2(String alpha2) {
        return service.historyByAlpha2(alpha2);
    }
}
