package com.hidoc.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.ExternalDocumentation;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Hi-Doc API Service",
                version = "v1",
                description = "Secure API layer for subscriber management, AI proxy (OpenAI, Grok, Gemini), health data processing, and analytics.",
                contact = @Contact(name = "Hi-Doc Team"),
                license = @License(name = "Proprietary")
        ),
        servers = {
                @Server(url = "/api", description = "Default context path")
        },
        security = {
                @SecurityRequirement(name = "bearer-jwt")
        },
        tags = {
                @Tag(name = "Subscribers", description = "Subscriber management endpoints"),
                @Tag(name = "AI Proxy", description = "Proxy endpoints for AI providers"),
                @Tag(name = "Health Data", description = "Health data processing and trends"),
                @Tag(name = "Analytics", description = "Usage analytics and reporting"),
                @Tag(name = "Health", description = "Service health and readiness")
        }
)
@SecurityScheme(name = "bearer-jwt", type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
