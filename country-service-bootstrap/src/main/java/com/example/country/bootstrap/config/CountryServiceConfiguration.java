package com.example.country.bootstrap.config;

import com.example.country.adapters.api.CountryApi;
import com.example.country.adapters.persistence.DynamoDbCountryRepository;
import com.example.country.application.CountryServiceImpl;
import com.example.country.application.ports.CountryRepositoryPort;
import com.example.country.application.ports.CountryServicePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
public class CountryServiceConfiguration {

    @Value("${aws.endpoint.url:http://localhost:4566}")
    private String awsEndpointUrl;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
                .region(software.amazon.awssdk.regions.Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")
                ));
        
        if (awsEndpointUrl != null && !awsEndpointUrl.isEmpty()) {
            builder.endpointOverride(URI.create(awsEndpointUrl));
        }
        
        return builder.build();
    }

    @Bean
    public CountryRepositoryPort countryRepository(DynamoDbClient dynamoDbClient) {
        return new DynamoDbCountryRepository(dynamoDbClient);
    }

    @Bean
    public CountryServicePort countryService(CountryRepositoryPort repository) {
        return new CountryServiceImpl(repository);
    }

    @Bean
    public CountryApi countryApi(CountryServicePort service) {
        return new CountryApi(service);
    }
}
