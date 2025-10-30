package com.example.country.application;

import com.example.country.application.model.CountryInput;
import com.example.country.application.ports.CountryRepositoryPort;
import com.example.country.domain.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CountryServiceImplTest {

    private static class InMemoryRepo implements CountryRepositoryPort {
        private final Map<String, List<Country>> byAlpha2 = new HashMap<>();
        @Override public Country saveNewVersion(Country country) {
            byAlpha2.computeIfAbsent(country.alpha2Code(), k -> new ArrayList<>()).add(country);
            return country;
        }
        @Override public Optional<Country> findLatestByAlpha2(String alpha2Code) {
            List<Country> list = byAlpha2.getOrDefault(alpha2Code, List.of());
            return list.stream().max(Comparator.comparing(Country::createDate));
        }
        @Override public Optional<Country> findLatestByAlpha3(String alpha3Code) {
            return byAlpha2.values().stream().flatMap(List::stream)
                    .filter(c -> c.alpha3Code().equals(alpha3Code))
                    .max(Comparator.comparing(Country::createDate));
        }
        @Override public Optional<Country> findLatestByNumeric(String numericCode) {
            return byAlpha2.values().stream().flatMap(List::stream)
                    .filter(c -> c.numericCode().equals(numericCode))
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
                    .sorted(Comparator.comparing(Country::createDate).reversed())
                    .toList();
        }
    }

    private CountryServiceImpl service;
    private InMemoryRepo repo;

    @BeforeEach
    void setup() {
        repo = new InMemoryRepo();
        service = new CountryServiceImpl(repo);
    }

    @Test
    void createAndFetch() {
        CountryInput input = new CountryInput("United Kingdom", "GB", "GBR", "826");
        Country created = service.create(input);
        assertEquals("GB", created.alpha2Code());
        assertEquals("GBR", service.getByAlpha3("GBR").alpha3Code());
        assertEquals("826", service.getByNumeric("826").numericCode());
        assertNotNull(created.createDate());
        assertNull(created.expiryDate());
        assertFalse(created.isDeleted());
    }

    @Test
    void updateCreatesNewVersion() {
        CountryInput input = new CountryInput("United Kingdom", "GB", "GBR", "826");
        Country created = service.create(input);
        Instant firstVersionDate = created.createDate();
        Country updated = service.updateByAlpha2("GB", new CountryInput("United Kingdom", "GB", "GBR", "826"));
        assertTrue(updated.createDate().isAfter(firstVersionDate));
        assertEquals(2, repo.historyByAlpha2("GB").size());
    }

    @Test
    void deleteMarksAsDeleted() {
        service.create(new CountryInput("United Kingdom", "GB", "GBR", "826"));
        service.deleteByAlpha2("GB");
        Country latest = service.getByAlpha2("GB");
        assertTrue(latest.isDeleted());
    }
}
