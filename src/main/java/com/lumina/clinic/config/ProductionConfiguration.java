package com.lumina.clinic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Fail fast rather than expose the public demo with repository defaults. */
@Configuration
@Profile("prod")
public class ProductionConfiguration {
    public ProductionConfiguration(
            @Value("${app.booking.token-secret}") String secret,
            @Value("${app.staff.password}") String staffPassword,
            @Value("${spring.datasource.password}") String databasePassword) {
        if (secret.length() < 32 || secret.startsWith("local-")) {
            throw new IllegalStateException("Set BOOKING_TOKEN_SECRET to a random value of at least 32 characters.");
        }
        if (staffPassword.length() < 16 || staffPassword.startsWith("local-")) {
            throw new IllegalStateException("Set STAFF_PASSWORD to a unique value of at least 16 characters.");
        }
        if (databasePassword.isBlank() || databasePassword.equals("lumina-local-only")) {
            throw new IllegalStateException("Set DB_PASSWORD for the deployment database.");
        }
    }
}
