package com.example.country.adapters.persistence;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

public class DynamoDbTableHelper {
    public static void createTableIfNotExists(DynamoDbClient client) {
        String tableName = "Countries";
        try {
            client.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
            // Table exists, return early
            return;
        } catch (ResourceNotFoundException ignored) {
            // Table doesn't exist, create it
        }
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(
                        KeySchemaElement.builder().attributeName("alpha2Code").keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName("createDate").keyType(KeyType.RANGE).build()
                )
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName("alpha2Code").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName("createDate").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName("alpha3Code").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName("numericCode").attributeType(ScalarAttributeType.S).build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder()
                                .indexName("GSI-Alpha3")
                                .keySchema(
                                        KeySchemaElement.builder().attributeName("alpha3Code").keyType(KeyType.HASH).build(),
                                        KeySchemaElement.builder().attributeName("createDate").keyType(KeyType.RANGE).build()
                                )
                                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                .build(),
                        GlobalSecondaryIndex.builder()
                                .indexName("GSI-Numeric")
                                .keySchema(
                                        KeySchemaElement.builder().attributeName("numericCode").keyType(KeyType.HASH).build(),
                                        KeySchemaElement.builder().attributeName("createDate").keyType(KeyType.RANGE).build()
                                )
                                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                .build()
                )
                .build();
        client.createTable(request);
        // Simple wait for table to be active
        int retries = 10;
        while (retries-- > 0) {
            try {
                Thread.sleep(100);
                DescribeTableResponse response = client.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
                if ("ACTIVE".equals(response.table().tableStatusAsString())) {
                    break;
                }
            } catch (Exception e) {
                // Continue retrying
            }
        }
    }
}
