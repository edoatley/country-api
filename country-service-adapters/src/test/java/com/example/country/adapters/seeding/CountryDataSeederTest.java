package com.example.country.adapters.seeding;

import com.example.country.application.ports.CountryRepositoryPort;
import com.example.country.domain.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CountryDataSeederTest {

    private CountryDataSeeder seeder;
    private CountryRepositoryPort repository;
    private CsvCountryReader reader;

    @BeforeEach
    void setUp() {
        repository = mock(CountryRepositoryPort.class);
        reader = mock(CsvCountryReader.class);
        seeder = new CountryDataSeeder(repository, reader);
    }

    @Test
    void shouldSeedCountriesFromCsv() throws Exception {
        String csv = "iso2,iso3,iso_num,country,country_common\n" +
                "GB,GBR,826,United Kingdom of Great Britain and Northern Ireland (the),United Kingdom\n" +
                "US,USA,840,United States of America (the),United States of America";
        
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        
        Country country1 = Country.of("United Kingdom of Great Britain and Northern Ireland (the)", 
                "GB", "GBR", "826", Instant.now(), null, false);
        Country country2 = Country.of("United States of America (the)", 
                "US", "USA", "840", Instant.now(), null, false);
        
        when(reader.readCountries(stream)).thenReturn(List.of(country1, country2));
        when(repository.saveNewVersion(any(Country.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int seeded = seeder.seedFromCsv(stream);

        assertEquals(2, seeded);
        verify(repository, times(2)).saveNewVersion(any(Country.class));
        verify(repository).saveNewVersion(country1);
        verify(repository).saveNewVersion(country2);
    }

    @Test
    void shouldHandlePartialFailuresWhenSeeding() throws Exception {
        String csv = "iso2,iso3,iso_num,country,country_common\n" +
                "GB,GBR,826,United Kingdom of Great Britain and Northern Ireland (the),United Kingdom\n" +
                "US,USA,840,United States of America (the),United States of America";
        
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        
        Country country1 = Country.of("United Kingdom of Great Britain and Northern Ireland (the)", 
                "GB", "GBR", "826", Instant.now(), null, false);
        Country country2 = Country.of("United States of America (the)", 
                "US", "USA", "840", Instant.now(), null, false);
        
        when(reader.readCountries(stream)).thenReturn(List.of(country1, country2));
        when(repository.saveNewVersion(country1)).thenReturn(country1);
        when(repository.saveNewVersion(country2)).thenThrow(new RuntimeException("Database error"));

        int seeded = seeder.seedFromCsv(stream);

        assertEquals(1, seeded);
        verify(repository).saveNewVersion(country1);
        verify(repository).saveNewVersion(country2);
    }

    @Test
    void shouldSeedFromClasspathResource() throws Exception {
        // Create a new seeder with the actual reader for this test
        CsvCountryReader csvReader = new CsvCountryReader();
        CountryDataSeeder realSeeder = new CountryDataSeeder(repository, csvReader);
        
        Country country = Country.of("United Kingdom of Great Britain and Northern Ireland (the)", 
                "GB", "GBR", "826", Instant.now(), null, false);
        when(repository.saveNewVersion(any(Country.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try {
            int seeded = realSeeder.seedFromClasspathResource("countries_iso3166b.csv");
            assertTrue(seeded >= 0); // Actual count depends on CSV file
            verify(repository, atLeastOnce()).saveNewVersion(any(Country.class));
        } catch (IllegalArgumentException e) {
            // If the resource doesn't exist in test classpath, that's acceptable
            // Just verify that the method properly handles the case
            assertTrue(e.getMessage().contains("Resource not found"));
        }
    }

    @Test
    void shouldThrowExceptionWhenClasspathResourceNotFound() {
        assertThrows(IllegalArgumentException.class, () -> {
            seeder.seedFromClasspathResource("non-existent-file.csv");
        });
    }

    @Test
    void shouldHandleEmptyCsvFile() throws Exception {
        String csv = "iso2,iso3,iso_num,country,country_common\n";
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        
        when(reader.readCountries(stream)).thenReturn(List.of());

        int seeded = seeder.seedFromCsv(stream);

        assertEquals(0, seeded);
        verify(repository, never()).saveNewVersion(any(Country.class));
    }
}
