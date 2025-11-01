package com.example.country.adapters.persistence;

import com.example.country.application.ports.CountryRepositoryPort;
import com.example.country.domain.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class DynamoDbCountryRepositoryTest {
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
            .withServices(LocalStackContainer.Service.DYNAMODB);

    private DynamoDbClient dynamoDb;
    private CountryRepositoryPort repository;

    @BeforeEach
    void setUp() {
        dynamoDb = DynamoDbClient.builder()
                .endpointOverride(URI.create(localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .region(Region.of(localStack.getRegion()))
                .build();
        DynamoDbTestHelper.createTable(dynamoDb);
        repository = new DynamoDbCountryRepository(dynamoDb);
    }

    @Test
    void canSaveAndRetrieveCountry() {
        Country country = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now(), null, false);
        Country saved = repository.saveNewVersion(country);
        assertEquals(country.alpha2Code(), saved.alpha2Code());

        var found = repository.findLatestByAlpha2("GB");
        assertTrue(found.isPresent());
        assertEquals("GB", found.get().alpha2Code());
        assertEquals("GBR", found.get().alpha3Code());
    }

    @Test
    void canFindByAlpha3() {
        Country country = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now(), null, false);
        repository.saveNewVersion(country);
        var found = repository.findLatestByAlpha3("GBR");
        assertTrue(found.isPresent());
        assertEquals("GB", found.get().alpha2Code());
    }

    @Test
    void canFindByNumeric() {
        Country country = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now(), null, false);
        repository.saveNewVersion(country);
        var found = repository.findLatestByNumeric("826");
        assertTrue(found.isPresent());
        assertEquals("GB", found.get().alpha2Code());
    }

    @Test
    void versioningCreatesHistory() {
        Country v1 = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now(), null, false);
        repository.saveNewVersion(v1);
        Country v2 = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now().plusSeconds(1), null, false);
        repository.saveNewVersion(v2);

        var history = repository.historyByAlpha2("GB");
        assertEquals(2, history.size());
        assertTrue(history.get(0).createDate().isAfter(history.get(1).createDate()));
    }
}
