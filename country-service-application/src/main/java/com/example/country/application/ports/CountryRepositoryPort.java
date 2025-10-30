package com.example.country.application.ports;

import com.example.country.domain.Country;

import java.util.List;
import java.util.Optional;

public interface CountryRepositoryPort {
    Country saveNewVersion(Country country);
    Optional<Country> findLatestByAlpha2(String alpha2Code);
    Optional<Country> findLatestByAlpha3(String alpha3Code);
    Optional<Country> findLatestByNumeric(String numericCode);
    List<Country> listLatest(int limit, int offset);
    List<Country> historyByAlpha2(String alpha2Code);
}
