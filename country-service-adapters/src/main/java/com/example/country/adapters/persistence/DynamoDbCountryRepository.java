package com.example.country.adapters.persistence;

import com.example.country.application.ports.CountryRepositoryPort;
import com.example.country.domain.Country;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class DynamoDbCountryRepository implements CountryRepositoryPort {
    private static final String TABLE_NAME = "Countries";
    private static final String PK = "alpha2Code";
    private static final String SK = "createDate";
    private static final String GSI_ALPHA3 = "GSI-Alpha3";
    private static final String GSI_NUMERIC = "GSI-Numeric";

    private final DynamoDbClient dynamoDb;

    public DynamoDbCountryRepository(DynamoDbClient dynamoDb) {
        this.dynamoDb = dynamoDb;
    }

    @Override
    public Country saveNewVersion(Country country) {
        Map<String, AttributeValue> item = toItem(country);
        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build());
        return country;
    }

    @Override
    public Optional<Country> findLatestByAlpha2(String alpha2Code) {
        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression(PK + " = :pk")
                .expressionAttributeValues(Map.of(":pk", AttributeValue.builder().s(alpha2Code).build()))
                .scanIndexForward(false)
                .limit(1)
                .build();

        QueryResponse response = dynamoDb.query(request);
        return response.items().stream()
                .filter(item -> {
                    AttributeValue expiry = item.get("expiryDate");
                    return expiry == null || expiry.nul();
                })
                .filter(item -> {
                    AttributeValue deleted = item.get("isDeleted");
                    return deleted == null || !deleted.bool();
                })
                .findFirst()
                .map(this::toCountry);
    }

    @Override
    public Optional<Country> findLatestByAlpha3(String alpha3Code) {
        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .indexName(GSI_ALPHA3)
                .keyConditionExpression("alpha3Code = :code")
                .expressionAttributeValues(Map.of(":code", AttributeValue.builder().s(alpha3Code).build()))
                .scanIndexForward(false)
                .limit(1)
                .build();

        QueryResponse response = dynamoDb.query(request);
        return response.items().stream()
                .filter(item -> {
                    AttributeValue expiry = item.get("expiryDate");
                    return expiry == null || expiry.nul();
                })
                .filter(item -> {
                    AttributeValue deleted = item.get("isDeleted");
                    return deleted == null || !deleted.bool();
                })
                .findFirst()
                .map(this::toCountry);
    }

    @Override
    public Optional<Country> findLatestByNumeric(String numericCode) {
        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .indexName(GSI_NUMERIC)
                .keyConditionExpression("numericCode = :code")
                .expressionAttributeValues(Map.of(":code", AttributeValue.builder().s(numericCode).build()))
                .scanIndexForward(false)
                .limit(1)
                .build();

        QueryResponse response = dynamoDb.query(request);
        return response.items().stream()
                .filter(item -> {
                    AttributeValue expiry = item.get("expiryDate");
                    return expiry == null || expiry.nul();
                })
                .filter(item -> {
                    AttributeValue deleted = item.get("isDeleted");
                    return deleted == null || !deleted.bool();
                })
                .findFirst()
                .map(this::toCountry);
    }

    @Override
    public List<Country> listLatest(int limit, int offset) {
        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .limit(limit + offset)
                .build();

        ScanResponse response = dynamoDb.scan(request);
        return response.items().stream()
                .map(item -> new AbstractMap.SimpleEntry<>(item.get(PK).s(), item))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (v1, v2) -> toCountry(v1).createDate().isAfter(toCountry(v2).createDate()) ? v1 : v2))
                .values().stream()
                .filter(item -> {
                    AttributeValue expiry = item.get("expiryDate");
                    return expiry == null || expiry.nul();
                })
                .filter(item -> {
                    AttributeValue deleted = item.get("isDeleted");
                    return deleted == null || !deleted.bool();
                })
                .sorted((a, b) -> toCountry(a).alpha2Code().compareTo(toCountry(b).alpha2Code()))
                .skip(offset)
                .limit(limit)
                .map(this::toCountry)
                .collect(Collectors.toList());
    }

    @Override
    public List<Country> historyByAlpha2(String alpha2Code) {
        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression(PK + " = :pk")
                .expressionAttributeValues(Map.of(":pk", AttributeValue.builder().s(alpha2Code).build()))
                .scanIndexForward(false)
                .build();

        QueryResponse response = dynamoDb.query(request);
        return response.items().stream()
                .map(this::toCountry)
                .sorted(Comparator.comparing(Country::createDate).reversed())
                .collect(Collectors.toList());
    }

    private Map<String, AttributeValue> toItem(Country country) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(PK, AttributeValue.builder().s(country.alpha2Code()).build());
        item.put(SK, AttributeValue.builder().s(country.createDate().toString()).build());
        item.put("name", AttributeValue.builder().s(country.name()).build());
        item.put("alpha3Code", AttributeValue.builder().s(country.alpha3Code()).build());
        item.put("numericCode", AttributeValue.builder().s(country.numericCode()).build());
        item.put("isDeleted", AttributeValue.builder().bool(country.isDeleted()).build());
        if (country.expiryDate() != null) {
            item.put("expiryDate", AttributeValue.builder().s(country.expiryDate().toString()).build());
        }
        return item;
    }

    private Country toCountry(Map<String, AttributeValue> item) {
        String name = item.get("name").s();
        String alpha2 = item.get(PK).s();
        String alpha3 = item.get("alpha3Code").s();
        String numeric = item.get("numericCode").s();
        Instant createDate = Instant.parse(item.get(SK).s());
        AttributeValue expiryVal = item.get("expiryDate");
        Instant expiryDate = expiryVal != null && !expiryVal.nul() ? Instant.parse(expiryVal.s()) : null;
        boolean isDeleted = Boolean.parseBoolean(item.getOrDefault("isDeleted", AttributeValue.builder().bool(false).build()).bool().toString());
        return Country.of(name, alpha2, alpha3, numeric, createDate, expiryDate, isDeleted);
    }
}
