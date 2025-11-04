package com.example.country.adapters.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.example.country.adapters.api.CountryApi;
import com.example.country.adapters.persistence.DynamoDbCountryRepository;
import com.example.country.application.CountryServiceImpl;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.Objects;

/**
 * AWS Lambda entry point for the Country Reference Service.
 * 
 * This class is the handler class configured in AWS Lambda.
 * It creates the full dependency graph and delegates to ApiGatewayLambdaHandler.
 * 
 * Handler: com.example.country.adapters.lambda.LambdaEntryPoint
 */
public class LambdaEntryPoint implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final ApiGatewayLambdaHandler handler;
    
    /**
     * Default constructor for AWS Lambda.
     * Creates dependencies from environment variables.
     */
    public LambdaEntryPoint() {
        this.handler = createHandler();
    }
    
    /**
     * Constructor for testing (allows dependency injection).
     */
    public LambdaEntryPoint(ApiGatewayLambdaHandler handler) {
        this.handler = Objects.requireNonNull(handler);
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        return handler.handleRequest(event, context);
    }
    
    /**
     * Creates the full dependency graph for Lambda execution.
     * Reads configuration from environment variables:
     * - AWS_REGION: AWS region (default: us-east-1)
     * - AWS_ENDPOINT_URL: Optional endpoint override (for LocalStack)
     * - API_KEY: API key for authentication
     */
    private ApiGatewayLambdaHandler createHandler() {
        // Configure DynamoDB Client
        String awsRegion = System.getenv("AWS_REGION");
        String awsEndpointUrl = System.getenv("AWS_ENDPOINT_URL");
        
        var dynamoDbBuilder = DynamoDbClient.builder();
        
        if (awsEndpointUrl != null && !awsEndpointUrl.isEmpty()) {
            dynamoDbBuilder.endpointOverride(URI.create(awsEndpointUrl));
        }
        if (awsRegion != null && !awsRegion.isEmpty()) {
            dynamoDbBuilder.region(software.amazon.awssdk.regions.Region.of(awsRegion));
        } else {
            dynamoDbBuilder.region(Region.US_EAST_1); // Default region
        }
        
        DynamoDbClient dynamoDbClient = dynamoDbBuilder.build();
        
        // Build dependency graph
        DynamoDbCountryRepository repository = new DynamoDbCountryRepository(dynamoDbClient);
        CountryServiceImpl countryService = new CountryServiceImpl(repository);
        CountryApi countryApi = new CountryApi(countryService);
        
        // Lambda-specific components
        CountryLambdaHandler lambdaHandler = new CountryLambdaHandler(countryApi);
        String apiKey = System.getenv("API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API_KEY environment variable must be set");
        }
        ApiKeyValidator apiKeyValidator = new ApiKeyValidator(apiKey);
        RouteMapper routeMapper = new RouteMapper();
        
        return new ApiGatewayLambdaHandler(lambdaHandler, apiKeyValidator, routeMapper);
    }
}

