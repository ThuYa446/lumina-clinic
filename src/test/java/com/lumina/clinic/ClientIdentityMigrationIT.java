package com.lumina.clinic;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the real PostgreSQL upgrade without starting the application or migrating its schema. */
@SpringBootTest(classes = ClientIdentityMigrationIT.DatabaseConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ClientIdentityMigrationIT {
    @Autowired DataSourceProperties database;

    @Test
    void upgradePreservesLegacyBookingsAndAllowsStoringIdentityDetails() {
        String schema = "lumina_identity_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        var dataSource = new DriverManagerDataSource(database.determineUrl(),
                database.determineUsername(), database.determinePassword());
        var jdbc = new JdbcTemplate(dataSource);
        UUID bookingId = UUID.randomUUID();
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .initSql("SET search_path TO " + schema + ", public")
                    .locations("classpath:db/migration").target("6").load().migrate();

            assertThat(jdbc.update("""
                INSERT INTO %1$s.bookings (id, version, branch_id, treatment_id, therapist_id, room_id,
                    client_name, client_email, client_phone, member, starts_at, ends_at,
                    room_occupied_until, therapist_occupied_until, created_at, status, payment_status,
                    deposit_amount, currency, idempotency_key, request_fingerprint)
                SELECT ?, 0, therapist.branch_id, treatment.id, therapist.id, room.id,
                    'Legacy Client', 'legacy@example.com', '+95 9 000 123 456', true,
                    TIMESTAMPTZ '2026-09-14 09:00:00+06:30',
                    TIMESTAMPTZ '2026-09-14 09:00:00+06:30' + treatment.duration_minutes * interval '1 minute',
                    TIMESTAMPTZ '2026-09-14 09:00:00+06:30' + (treatment.duration_minutes + 15) * interval '1 minute',
                    TIMESTAMPTZ '2026-09-14 09:00:00+06:30' + (treatment.duration_minutes + therapist.turnaround_minutes) * interval '1 minute',
                    TIMESTAMPTZ '2026-09-10 09:00:00+06:30', 'CONFIRMED', 'NOT_REQUIRED', 0, 'MMK', ?, 'legacy-fingerprint'
                FROM %1$s.therapists therapist
                JOIN %1$s.therapist_treatments qualification ON qualification.therapist_id = therapist.id
                JOIN %1$s.treatments treatment ON treatment.id = qualification.treatment_id
                JOIN %1$s.rooms room ON room.branch_id = therapist.branch_id
                ORDER BY therapist.id, treatment.id, room.id LIMIT 1
                """.formatted(schema), bookingId, UUID.randomUUID())).isEqualTo(1);
            Map<String, Object> legacy = jdbc.queryForMap("SELECT * FROM " + schema + ".bookings WHERE id=?", bookingId);
            assertThat(legacy).doesNotContainKeys("client_id_number", "client_date_of_birth");

            var migration = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .initSql("SET search_path TO " + schema + ", public")
                    .locations("classpath:db/migration").load().migrate();
            assertThat(migration.migrationsExecuted).isEqualTo(1);
            Map<String, Object> upgraded = jdbc.queryForMap("SELECT * FROM " + schema + ".bookings WHERE id=?", bookingId);
            assertThat(upgraded).containsAllEntriesOf(legacy)
                    .containsEntry("id", bookingId)
                    .containsEntry("client_name", "Legacy Client")
                    .containsEntry("client_email", "legacy@example.com")
                    .containsEntry("client_phone", "+95 9 000 123 456")
                    .containsEntry("status", "CONFIRMED")
                    .containsEntry("client_id_number", null)
                    .containsEntry("client_date_of_birth", null);

            LocalDate dateOfBirth = LocalDate.of(1990, 2, 3);
            assertThat(jdbc.update("UPDATE " + schema + ".bookings SET client_id_number=?, client_date_of_birth=? WHERE id=?",
                    "0012/ABC(N)001234", Date.valueOf(dateOfBirth), bookingId)).isEqualTo(1);
            Map<String, Object> completed = jdbc.queryForMap("SELECT * FROM " + schema + ".bookings WHERE id=?", bookingId);
            assertThat(completed).containsAllEntriesOf(legacy)
                    .containsEntry("client_id_number", "0012/ABC(N)001234")
                    .containsEntry("client_date_of_birth", Date.valueOf(dateOfBirth));
        } finally {
            // Only this test's randomly named schema is removed; shared/public schemas are untouched.
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DataSourceProperties.class)
    static class DatabaseConfiguration {}
}
