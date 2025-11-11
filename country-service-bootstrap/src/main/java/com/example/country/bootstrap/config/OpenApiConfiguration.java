package com.example.country.bootstrap.config;

import com.example.country.adapters.web.dto.CountryDTO;
import com.example.country.domain.Country;
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

import java.util.List;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI countryServiceOpenAPI() {
        return new OpenAPI()
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
                                .description("API Key for authenticating requests")));
    }
    
    /**
     * Customize OpenAPI to use CountryDTO schema for Country domain class.
     * This allows SpringDoc to generate proper schema documentation using the DTO
     * while the actual API continues to use the Country domain class.
     */
    @Bean
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
                    openApi.getComponents().getSchemas().put("Country", resolvedSchema.schema);
                }
            } catch (Exception e) {
                // If schema replacement fails, log but don't fail the application
                System.err.println("Warning: Could not replace Country schema with CountryDTO: " + e.getMessage());
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

