package com.example.country.bootstrap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
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

    @Bean
    public GroupedOpenApi countryGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("country")
                .pathsToMatch("/api/v1/countries/**")
                .build();
    }
}

