package com.example.todo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration.
 *
 * <p>The standalone frontend (plain HTML/JS) is served from a different origin
 * (e.g. {@code http://localhost:5500} or a {@code file://} URL), so the API
 * must allow cross-origin calls. Allowed origin patterns come from
 * {@code app.cors.allowed-patterns} (env {@code TODO_CORS_PATTERNS}):
 * permissive locally, locked to the real frontend domain in production.
 * Credentials are never used: this is a single-user application.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedPatterns;

    public CorsConfig(
            @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-patterns:*}")
            String allowedPatterns) {
        this.allowedPatterns = allowedPatterns.split(",");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedPatterns)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
