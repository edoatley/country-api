package com.example.country.adapters;

import com.example.country.adapters.api.CountryApi;
import com.example.country.application.CountryServiceImpl;
import com.example.country.application.model.CountryInput;
import com.example.country.application.ports.CountryRepositoryPort;
import com.example.country.domain.Country;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CountryApiTest {

    static class InMemoryRepo implements CountryRepositoryPort {
        private final Map<String, List<Country>> byAlpha2 = new HashMap<>();
        @Override public Country saveNewVersion(Country country) {
            byAlpha2.computeIfAbsent(country.alpha2Code(), k -> new ArrayList<>()).add(country);
            return country;
        }
        @Override public Optional<Country> findLatestByAlpha2(String alpha2Code) {
            return byAlpha2.getOrDefault(alpha2Code, List.of()).stream().max(Comparator.comparing(Country::createDate));
        }
        @Override public Optional<Country> findLatestByAlpha3(String alpha3Code) {
            return byAlpha2.values().stream().flatMap(List::stream).filter(c -> c.alpha3Code().equals(alpha3Code))
                    .max(Comparator.comparing(Country::createDate));
        }
        @Override public Optional<Country> findLatestByNumeric(String numericCode) {
            return byAlpha2.values().stream().flatMap(List::stream).filter(c -> c.numericCode().equals(numericCode))
                    .max(Comparator.comparing(Country::createDate));
        }
        @Override public List<Country> listLatest(int limit, int offset) {
            return byAlpha2.values().stream()
                    .map(list -> list.stream().max(Comparator.comparing(Country::createDate)).orElse(null))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Country::alpha2Code))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }
        @Override public List<Country> historyByAlpha2(String alpha2Code) {
            return byAlpha2.getOrDefault(alpha2Code, List.of()).stream()
                    .sorted(Comparator.comparing(Country::createDate).reversed()).toList();
        }
    }

    @Test
    void apiCreateAndGet() {
        InMemoryRepo repo = new InMemoryRepo();
        CountryApi api = new CountryApi(new CountryServiceImpl(repo));
        Country created = api.createCountry(new CountryInput("United Kingdom", "GB", "GBR", "826"));
        assertEquals("GBR", api.getByAlpha3("GBR").alpha3Code());
        assertEquals(1, api.listCountries(10, 0).size());
    }
}
