package com.ead.gearup.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${app.swagger.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${app.swagger.prod-url:https://api.gearup.com}")
    private String prodUrl;

    @Bean
    public OpenAPI openAPI() {
        // Development server
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Server URL in Development environment");

        // Production server
        Server prodServer = new Server();
        prodServer.setUrl(prodUrl);
        prodServer.setDescription("Server URL in Production environment");

        // Contact information
        Contact contact = new Contact();
        contact.setEmail("support@gearup.com");
        contact.setName("GearUp Team");
        contact.setUrl("https://www.gearup.com");

        // License
        License mitLicense = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        // API Info
        Info info = new Info()
                .title("GearUp Vehicle Service Management API")
                .version("1.0.0")
                .contact(contact)
                .description("This API provides comprehensive endpoints for the GearUp vehicle service management system. " +
                           "It includes authentication, customer management, employee operations, appointment scheduling, " +
                           "vehicle management, and task tracking capabilities.")
                .termsOfService("https://www.gearup.com/terms")
                .license(mitLicense);

        // Security scheme for JWT
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name("bearerAuth")
                .description("JWT token authentication. Please add 'Bearer ' prefix to your token.");

        // Security requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer))
                .addSecurityItem(securityRequirement)
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme));
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                registry.addRedirectViewController("/swagger", "/swagger-ui.html");
            }
        };
    }
}
