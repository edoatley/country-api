package com.example.country.adapters.seeding;

import com.example.country.application.ports.CountryRepositoryPort;
import com.example.country.domain.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

public class CountryDataSeeder {
    private static final Logger log = LoggerFactory.getLogger(CountryDataSeeder.class);
    
    private final CountryRepositoryPort repository;
    private final CsvCountryReader reader;
    
    public CountryDataSeeder(CountryRepositoryPort repository, CsvCountryReader reader) {
        this.repository = repository;
        this.reader = reader;
    }
    
    public int seedFromCsv(InputStream csvStream) throws Exception {
        log.info("Starting data seeding from CSV...");
        List<Country> countries = reader.readCountries(csvStream);
        log.info("Read {} countries from CSV", countries.size());
        
        int seeded = 0;
        for (Country country : countries) {
            try {
                repository.saveNewVersion(country);
                seeded++;
            } catch (Exception e) {
                log.warn("Failed to seed country {}: {}", country.alpha2Code(), e.getMessage());
            }
        }
        
        log.info("Successfully seeded {} countries", seeded);
        return seeded;
    }
    
    public int seedFromClasspathResource(String resourcePath) throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        return seedFromCsv(stream);
    }
}
