package edu.lab.core;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@OpenAPIDefinition(
    info = @Info(
        title = "lab-edu core API",
        version = "v1",
        description = "Core service API documentation",
        contact = @Contact(name = "lab-edu team"),
        license = @License(name = "Internal")
    ),
    security = {
        @SecurityRequirement(name = "bearerAuth")
    }
)
public class OpenApiConfig {
}
