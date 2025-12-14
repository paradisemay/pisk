package ru.ifmo.se.pisk.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Pisk API",
                version = "v1",
                description = "REST API для каталога музыкальных треков",
                contact = @Contact(name = "Pisk Platform", email = "support@example.com")
        ),
        servers = {@Server(url = "/", description = "Основной сервер")}
)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi apiV1Group() {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}
