package com.example.country.adapters.persistence;

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
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class DynamoDbTableHelperTest {
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
            .withServices(LocalStackContainer.Service.DYNAMODB);

    private DynamoDbClient dynamoDb;

    @BeforeEach
    void setUp() {
        dynamoDb = DynamoDbClient.builder()
                .endpointOverride(URI.create(localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .region(Region.of(localStack.getRegion()))
                .build();
    }

    @Test
    void shouldCreateTableIfNotExists() {
        // First call should create the table
        assertDoesNotThrow(() -> DynamoDbTableHelper.createTableIfNotExists(dynamoDb));
        
        // Verify table exists
        var response = dynamoDb.describeTable(DescribeTableRequest.builder().tableName("Countries").build());
        assertNotNull(response.table());
        assertEquals("Countries", response.table().tableName());
    }

    @Test
    void shouldNotFailIfTableAlreadyExists() {
        // Create table first time
        DynamoDbTableHelper.createTableIfNotExists(dynamoDb);
        
        // Second call should not fail (idempotent)
        assertDoesNotThrow(() -> DynamoDbTableHelper.createTableIfNotExists(dynamoDb));
        
        // Verify table still exists
        var response = dynamoDb.describeTable(DescribeTableRequest.builder().tableName("Countries").build());
        assertNotNull(response.table());
    }

    @Test
    void shouldCreateTableWithCorrectSchema() {
        DynamoDbTableHelper.createTableIfNotExists(dynamoDb);
        
        var response = dynamoDb.describeTable(DescribeTableRequest.builder().tableName("Countries").build());
        var table = response.table();
        
        // Verify key schema
        assertEquals(2, table.keySchema().size());
        assertTrue(table.keySchema().stream().anyMatch(ks -> ks.attributeName().equals("alpha2Code") && ks.keyType().toString().equals("HASH")));
        assertTrue(table.keySchema().stream().anyMatch(ks -> ks.attributeName().equals("createDate") && ks.keyType().toString().equals("RANGE")));
        
        // Verify GSIs exist
        assertEquals(2, table.globalSecondaryIndexes().size());
        assertTrue(table.globalSecondaryIndexes().stream().anyMatch(gsi -> gsi.indexName().equals("GSI-Alpha3")));
        assertTrue(table.globalSecondaryIndexes().stream().anyMatch(gsi -> gsi.indexName().equals("GSI-Numeric")));
    }
}
