package com.lumina.clinic.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationTest {
    private final String secret = "unique-test-secret-with-more-than-thirty-two-characters";
    private final String password = "unique-test-staff-password";

    @Test
    void publicDeploymentRejectsEveryDocumentedLocalCredential() {
        assertThatThrownBy(() -> new ProductionConfiguration("local-demo-token-secret-change-before-deploy-123456789", password, "db-secret"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("BOOKING_TOKEN_SECRET");
        assertThatThrownBy(() -> new ProductionConfiguration(secret, "local-reception-only", "db-secret"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("STAFF_PASSWORD");
        assertThatThrownBy(() -> new ProductionConfiguration(secret, password, "lumina-local-only"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("DB_PASSWORD");
    }

    @Test
    void shortSecretsFailClosed() {
        assertThatThrownBy(() -> new ProductionConfiguration("short", password, "db-secret"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ProductionConfiguration(secret, "short", "db-secret"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsExplicitDeploymentConfiguration() {
        assertThatCode(() -> new ProductionConfiguration(secret, password, "deployment-db-password"))
                .doesNotThrowAnyException();
    }
}
