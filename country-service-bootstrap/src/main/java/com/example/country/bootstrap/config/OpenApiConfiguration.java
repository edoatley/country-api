package com.example.country.bootstrap.config;

import com.example.country.adapters.web.dto.CountryDTO;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI countryServiceOpenAPI() {
        return new OpenAPI()
                .openapi("3.0.3")
                .info(new Info()
                        .title("Country Reference Service API")
                        .description("""
                                A microservice designed to provide and manage country data.
                                This service acts as a centralized source of truth for country information,
                                supporting a full change history for all data entities.
                                
                                **Key Design Notes:**
                                - **Primary Identifier**: The ISO `alpha2Code` is the primary identifier for all resources. All mutation operations (PUT, DELETE) must use the `/countries/code/{alpha2Code}` path.
                                - **Versioning**: Every update creates a new version of the country record, and the full history is accessible via the history endpoint.
                                """)
                        .version("1.1.0")
                        .contact(new Contact()
                                .name("Country Reference Service")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server"),
                        new Server()
                                .url("https://api.example.com")
                                .description("Production server")))
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-KEY")
                                .description("API Key for authenticating requests.")));
    }
    
    /**
     * Customize OpenAPI to use CountryDTO schema for Country domain class.
     * This allows SpringDoc to generate proper schema documentation using the DTO
     * while the actual API continues to use the Country domain class.
     * 
     * Uses @Order(1) to ensure this runs after other customizers.
     */
    @Bean
    @Order(1)
    public OpenApiCustomizer countrySchemaCustomizer() {
        return openApi -> {
            // Get the CountryDTO schema from ModelConverters
            io.swagger.v3.core.converter.ModelConverters converters = 
                io.swagger.v3.core.converter.ModelConverters.getInstance();
            
            try {
                io.swagger.v3.core.converter.AnnotatedType dtoType = 
                    new io.swagger.v3.core.converter.AnnotatedType(CountryDTO.class);
                
                io.swagger.v3.core.converter.ResolvedSchema resolvedSchema = 
                    converters.readAllAsResolvedSchema(dtoType);
                
                // Replace the Country schema with CountryDTO schema in components
                if (resolvedSchema != null && resolvedSchema.schema != null) {
                    if (openApi.getComponents() == null) {
                        openApi.setComponents(new io.swagger.v3.oas.models.Components());
                    }
                    if (openApi.getComponents().getSchemas() == null) {
                        openApi.getComponents().setSchemas(new java.util.HashMap<>());
                    }
                    
                    io.swagger.v3.oas.models.media.Schema<?> countrySchema = resolvedSchema.schema;
                    
                    // Remove the incorrectly generated "deleted" property (from isDeleted() getter)
                    if (countrySchema.getProperties() != null) {
                        countrySchema.getProperties().remove("deleted");
                    }
                    
                    // Ensure expiryDate has nullable: true
                    // Note: With springdoc.api-docs.version=openapi_3_0, the @Schema(nullable = true) annotation
                    // should work directly, but we keep this as a safety measure to ensure it's set correctly
                    if (countrySchema.getProperties() != null && countrySchema.getProperties().containsKey("expiryDate")) {
                        io.swagger.v3.oas.models.media.Schema<?> expiryDateSchema = 
                            (io.swagger.v3.oas.models.media.Schema<?>) countrySchema.getProperties().get("expiryDate");
                        if (expiryDateSchema != null) {
                            expiryDateSchema.setNullable(true);
                        }
                    }
                    
                    // Explicitly set required fields to match static spec
                    countrySchema.setRequired(List.of("name", "alpha2Code", "alpha3Code", "numericCode", "createDate", "isDeleted"));
                    
                    openApi.getComponents().getSchemas().put("Country", countrySchema);
                }
            } catch (Exception e) {
                // If schema replacement fails, log but don't fail the application
                System.err.println("Warning: Could not replace Country schema with CountryDTO: " + e.getMessage());
            }
        };
    }

    /**
     * Customizer to normalize query parameters by removing explicit required: false.
     * This makes the generated spec match the static spec more closely, as query
     * parameters are optional by default in OpenAPI.
     */
    @Bean
    @Order(2)
    public OpenApiCustomizer normalizeQueryParametersCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, pathItem) -> {
                    if (pathItem != null) {
                        // Process all operations (GET, POST, PUT, DELETE, etc.)
                        processOperation(pathItem.getGet());
                        processOperation(pathItem.getPost());
                        processOperation(pathItem.getPut());
                        processOperation(pathItem.getDelete());
                        processOperation(pathItem.getPatch());
                    }
                });
            }
        };
    }
    
    private void processOperation(io.swagger.v3.oas.models.Operation operation) {
        if (operation != null && operation.getParameters() != null) {
            operation.getParameters().forEach(param -> {
                if (param != null && "query".equals(param.getIn()) && Boolean.FALSE.equals(param.getRequired())) {
                    // Remove explicit required: false for query parameters
                    // This matches the static spec which doesn't include this field
                    param.setRequired(null);
                }
            });
        }
    }
    
    /**
     * Customizer to remove auto-generated 404 responses from list endpoints.
     * SpringDoc auto-generates 404 responses for GET endpoints, but list endpoints
     * (like GET /api/v1/countries) should never return 404 - they return empty lists instead.
     */
    @Bean
    @Order(3)
    public OpenApiCustomizer remove404FromListEndpointsCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                // Remove 404 from GET /api/v1/countries (list endpoint)
                var countriesPath = openApi.getPaths().get("/api/v1/countries");
                if (countriesPath != null && countriesPath.getGet() != null) {
                    var getOperation = countriesPath.getGet();
                    if (getOperation.getResponses() != null) {
                        // Remove 404 if it exists and wasn't explicitly documented
                        var responses = getOperation.getResponses();
                        if (responses.containsKey("404")) {
                            var response404 = responses.get("404");
                            // Only remove if it's auto-generated (has default description or no explicit documentation)
                            if (response404 != null) {
                                String description = response404.getDescription();
                                // SpringDoc auto-generated 404s typically have "Not Found" as description
                                if (description == null || "Not Found".equals(description)) {
                                    responses.remove("404");
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    @Bean
    public GroupedOpenApi countryGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("country")
                .pathsToMatch("/api/v1/countries/**")
                .build();
    }
}

