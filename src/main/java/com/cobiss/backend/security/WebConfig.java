package com.cobiss.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/graphql")
                .allowedOrigins(frontendUrl)
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*");

        registry.addMapping("/api/**")
                .allowedOrigins(frontendUrl)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");

        registry.addMapping("/swagger-ui/**")
                .allowedOrigins(frontendUrl)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*");

        registry.addMapping("/api-docs/**")
                .allowedOrigins(frontendUrl)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*");

        registry.addMapping("/voyager")
                .allowedOrigins(frontendUrl)
                .allowedMethods("GET")
                .allowedHeaders("*");
    }
}
