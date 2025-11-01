package com.example.country.application;

import com.example.country.application.model.CountryInput;
import com.example.country.application.ports.CountryRepositoryPort;
import com.example.country.application.ports.CountryServicePort;
import com.example.country.domain.Country;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class CountryServiceImpl implements CountryServicePort {
    private final CountryRepositoryPort repository;

    public CountryServiceImpl(CountryRepositoryPort repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Country create(CountryInput input) {
        Country country = Country.of(
                input.name(),
                input.alpha2Code(),
                input.alpha3Code(),
                input.numericCode(),
                Instant.now(),
                null,
                false
        );
        return repository.saveNewVersion(country);
    }

    @Override
    public Country getByAlpha2(String alpha2Code) {
        return repository.findLatestByAlpha2(alpha2Code)
                .orElseThrow(() -> new NoSuchElementException("Country not found: " + alpha2Code));
    }

    @Override
    public Country getByAlpha3(String alpha3Code) {
        return repository.findLatestByAlpha3(alpha3Code)
                .orElseThrow(() -> new NoSuchElementException("Country not found: " + alpha3Code));
    }

    @Override
    public Country getByNumeric(String numericCode) {
        return repository.findLatestByNumeric(numericCode)
                .orElseThrow(() -> new NoSuchElementException("Country not found: " + numericCode));
    }

    @Override
    public List<Country> listAll(int limit, int offset) {
        return repository.listLatest(limit, offset);
    }

    @Override
    public Country updateByAlpha2(String alpha2Code, CountryInput input) {
        // Retrieve to ensure existence
        Country latest = getByAlpha2(alpha2Code);
        Country updated = Country.of(
                input.name(),
                alpha2Code,
                input.alpha3Code(),
                input.numericCode(),
                Instant.now(),
                null,
                latest.isDeleted() // preserve deletion flag if necessary, but typically false on update
        );
        return repository.saveNewVersion(updated);
    }

    @Override
    public void deleteByAlpha2(String alpha2Code) {
        Country latest = getByAlpha2(alpha2Code);
        Country deleted = Country.of(
                latest.name(),
                latest.alpha2Code(),
                latest.alpha3Code(),
                latest.numericCode(),
                Instant.now(),
                null,
                true
        );
        repository.saveNewVersion(deleted);
    }

    @Override
    public List<Country> historyByAlpha2(String alpha2Code) {
        return repository.historyByAlpha2(alpha2Code);
    }
}
