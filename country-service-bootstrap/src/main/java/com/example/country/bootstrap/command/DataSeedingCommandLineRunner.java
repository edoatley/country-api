package com.example.country.bootstrap.command;

import com.example.country.adapters.persistence.DynamoDbTableHelper;
import com.example.country.adapters.seeding.CountryDataSeeder;
import com.example.country.adapters.seeding.CsvCountryReader;
import com.example.country.application.ports.CountryRepositoryPort;
import com.example.country.bootstrap.health.DataSeedingHealthIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Component
public class DataSeedingCommandLineRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeedingCommandLineRunner.class);
    
    private final DynamoDbClient dynamoDbClient;
    private final CountryRepositoryPort repository;
    private final boolean enableSeeding;
    private final DataSeedingHealthIndicator healthIndicator;
    
    public DataSeedingCommandLineRunner(
            DynamoDbClient dynamoDbClient,
            CountryRepositoryPort repository,
            DataSeedingHealthIndicator healthIndicator,
            @Value("${data.seeding.enabled:false}") boolean enableSeeding) {
        this.dynamoDbClient = dynamoDbClient;
        this.repository = repository;
        this.healthIndicator = healthIndicator;
        this.enableSeeding = enableSeeding;
    }
    
    @Override
    public void run(String... args) {
        if (!enableSeeding) {
            log.debug("Data seeding is disabled. Set 'data.seeding.enabled=true' to enable.");
            return;
        }
        
        // Ensure table exists
        try {
            DynamoDbTableHelper.createTableIfNotExists(dynamoDbClient);
            log.info("DynamoDB table created/verified");
        } catch (Exception e) {
            log.warn("Table creation check failed (may already exist): {}", e.getMessage());
            healthIndicator.markSeedingFailed("Table creation failed: " + e.getMessage());
            return;
        }
        
        // Seed data
        try {
            CountryDataSeeder seeder = new CountryDataSeeder(repository, new CsvCountryReader());
            int seeded = seeder.seedFromClasspathResource("countries_iso3166b.csv");
            log.info("Data seeding completed: {} countries seeded", seeded);
            healthIndicator.markSeedingComplete();
        } catch (Exception e) {
            log.error("Data seeding failed", e);
            healthIndicator.markSeedingFailed("Data seeding failed: " + e.getMessage());
        }
    }
}
