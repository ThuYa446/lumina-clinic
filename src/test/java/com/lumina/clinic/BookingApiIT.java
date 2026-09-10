package com.lumina.clinic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real HTTP, Spring Security, Flyway and PostgreSQL; each run owns a disposable schema. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.membership.code=TEST-MEMBER-300", "app.membership.email=member@example.com",
        "app.staff.username=receptionist", "app.staff.password=integration-staff-password",
        "app.booking.token-secret=integration-secret-at-least-thirty-two-characters",
        "app.payments.demo-enabled=true"})
@Import(BookingApiIT.TimeConfiguration.class)
@org.springframework.test.context.ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingApiIT {
    private static final String SCHEMA = "lumina_it_" + UUID.randomUUID().toString().replace("-", "");
    private static final Instant NOW = Instant.parse("2026-09-10T02:30:00Z");
    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired MutableClock clock;
    ApiClient api;
    JsonNode catalog;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA + ", public");
        properties.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
    }

    @BeforeEach
    void setUp() throws Exception {
        clock.set(NOW);
        jdbc.execute("TRUNCATE TABLE " + SCHEMA + ".payment_transactions, " + SCHEMA + ".bookings CASCADE");
        api = new ApiClient();
        catalog = api.get("/api/catalog", null).json();
    }

    @AfterAll
    void removeOwnedSchema() {
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }

    @Test
    void catalogAvailabilityAndPaymentsUseMyanmarSettings() throws Exception {
        assertThat(catalog.path("currency").asText()).isEqualTo("MMK");
        assertThat(catalog.path("depositAmount").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(catalog.path("timeZone").asText()).isEqualTo("Asia/Yangon");
        assertThat(catalog.path("treatments").get(0).path("price").decimalValue()).isEqualByComparingTo("900.00");
        assertThat(catalog.path("treatments").get(1).path("price").decimalValue()).isEqualByComparingTo("1600.00");
        assertThat(catalog.path("treatments").get(2).path("price").decimalValue()).isEqualByComparingTo("2400.00");
        List<String> names = new ArrayList<>();
        for (JsonNode branch : catalog.path("branches")) {
            names.add(branch.path("name").asText());
            assertThat(branch.path("address").asText()).contains("Yangon, Myanmar");
        }
        assertThat(names).containsExactly("Bahan", "Kamayut", "Sanchaung", "Tamwe", "Thingangyun", "Yankin");
        Map<String, Object> request = requestAt("2026-09-14T09:00:00+06:30");
        Reply availability = api.get("/api/availability?branchId=" + request.get("branchId")
                + "&treatmentId=" + request.get("treatmentId") + "&date=2026-09-14", null);
        assertThat(availability.status()).isEqualTo(200);
        assertThat(availability.json().path("timeZone").asText()).isEqualTo("Asia/Yangon");
        var firstStart = java.time.OffsetDateTime.parse(availability.json().path("slots").get(0).path("startsAt").asText());
        assertThat(firstStart.getOffset()).isEqualTo(ZoneOffset.ofHoursMinutes(6, 30));
        assertThat(firstStart.toInstant()).isEqualTo(Instant.parse("2026-09-14T02:30:00Z"));
        JsonNode booking = create();
        assertThat(booking.path("currency").asText()).isEqualTo("MMK");
        assertThat(booking.path("startsAt").asText()).endsWith("+06:30");
        Reply paid = api.post("/api/bookings/" + booking.path("id").asText() + "/payments", Map.of(),
                booking.path("managementToken").asText(), UUID.randomUUID());
        assertThat(paid.status()).isEqualTo(200);
        assertThat(paid.json().path("booking").path("currency").asText()).isEqualTo("MMK");
        assertThat(jdbc.queryForObject("SELECT currency FROM payment_transactions", String.class)).isEqualTo("MMK");
    }

    @Test
    void competingReceptionistsCannotDoubleBookTherapist() throws Exception {
        Map<String, Object> request = requestAt("2026-09-14T09:00:00+06:30");
        ApiClient other = new ApiClient();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> { ready.countDown(); start.await(); return api.post("/api/bookings", request, null, UUID.randomUUID()); });
            var second = executor.submit(() -> { ready.countDown(); start.await(); return other.post("/api/bookings", request, null, UUID.randomUUID()); });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = new ArrayList<>(List.of(first.get(15, TimeUnit.SECONDS).status(), second.get(15, TimeUnit.SECONDS).status()));
            statuses.sort(Integer::compareTo);
            assertThat(statuses).containsExactly(201, 409);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM bookings", Integer.class)).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void bookingRetryReturnsSameReservationAndRejectsChangedPayload() throws Exception {
        UUID key = UUID.randomUUID();
        Map<String, Object> request = requestAt("2026-09-14T09:00:00+06:30");
        Reply first = api.post("/api/bookings", request, null, key);
        assertThat(first.status()).isEqualTo(201);
        Reply retry = api.post("/api/bookings", request, null, key);
        assertThat(retry.status()).isIn(200, 201);
        assertThat(retry.json().path("id").asText()).isEqualTo(first.json().path("id").asText());
        request.put("startsAt", "2026-09-14T11:00:00+06:30");
        assertThat(api.post("/api/bookings", request, null, key).status()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bookings", Integer.class)).isEqualTo(1);
    }

    @Test
    void paymentRetriesCannotChargeTwiceAndCancellationQueuesManualRefund() throws Exception {
        JsonNode booking = create();
        String path = "/api/bookings/" + booking.path("id").asText();
        String token = booking.path("managementToken").asText();
        UUID key = UUID.randomUUID();
        Reply paid = api.post(path + "/payments", Map.of(), token, key);
        assertThat(paid.status()).isEqualTo(200);
        assertThat(paid.json().path("booking").path("status").asText()).isEqualTo("CONFIRMED");
        Reply retried = api.post(path + "/payments", Map.of(), token, key);
        assertThat(retried.status()).isEqualTo(200);
        assertThat(retried.json().path("paymentReference").asText()).isEqualTo(paid.json().path("paymentReference").asText());
        Reply freshKey = api.post(path + "/payments", Map.of(), token, UUID.randomUUID());
        assertThat(freshKey.status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_transactions", Integer.class)).isEqualTo(1);
        Reply cancelled = api.post(path + "/cancel", Map.of(), token, null);
        assertThat(cancelled.status()).isEqualTo(200);
        assertThat(cancelled.json().path("paymentStatus").asText()).isEqualTo("REFUND_PENDING");
        assertThat(api.get(path, "incorrect-token").status()).isIn(403, 404);
    }

    @Test
    void unpaidHoldExpiresWithoutBackgroundJobsAndSlotBecomesAvailable() throws Exception {
        JsonNode booking = create();
        clock.set(java.time.OffsetDateTime.parse(booking.path("holdExpiresAt").asText()).toInstant());
        Reply state = api.get("/api/bookings/" + booking.path("id").asText(), booking.path("managementToken").asText());
        assertThat(state.status()).isEqualTo(200);
        assertThat(state.json().path("status").asText()).isEqualTo("EXPIRED");
        Reply replacement = api.post("/api/bookings", requestAt("2026-09-14T09:00:00+06:30"), null, UUID.randomUUID());
        assertThat(replacement.status()).isEqualTo(201);
        assertThat(api.post("/api/bookings/" + booking.path("id").asText() + "/payments", Map.of(),
                booking.path("managementToken").asText(), UUID.randomUUID()).status()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_transactions", Integer.class)).isZero();
    }

    @Test
    void depositExemptionRequiresVerifiedMembershipAndRejectsClientControlledFlag() throws Exception {
        Map<String, Object> request = requestAt("2026-09-14T09:00:00+06:30");
        request.put("member", true);
        assertThat(api.post("/api/bookings", request, null, UUID.randomUUID()).status()).isEqualTo(400);
        request.remove("member");
        request.put("membershipCode", "TEST-MEMBER-300");
        assertThat(api.post("/api/bookings", request, null, UUID.randomUUID()).status()).isEqualTo(422);
        request.put("client", Map.of("name", "Demo Member", "email", "member@example.com", "phone", "+95 9 123 456 789"));
        Reply member = api.post("/api/bookings", request, null, UUID.randomUUID());
        assertThat(member.status()).isEqualTo(201);
        assertThat(member.json().path("status").asText()).isEqualTo("CONFIRMED");
        assertThat(member.json().path("paymentStatus").asText()).isEqualTo("NOT_REQUIRED");
        assertThat(member.json().path("depositAmount").decimalValue()).isZero();
    }

    @Test
    void validatesSlotAlignmentQualificationAndClosedDays() throws Exception {
        for (String startsAt : List.of("2026-09-14T09:15:00+06:30", "2026-09-13T09:00:00+06:30", "2026-09-14T18:00:00+06:30")) {
            assertThat(api.post("/api/bookings", requestAt(startsAt), null, UUID.randomUUID()).status()).isEqualTo(400);
        }
        Map<String, Object> request = requestAt("2026-09-14T09:00:00+06:30");
        String branch = request.get("branchId").toString();
        for (JsonNode therapist : catalog.path("therapists")) {
            if (!therapist.path("branchId").asText().equals(branch)) {
                request.put("therapistId", therapist.path("id").asText()); break;
            }
        }
        assertThat(api.post("/api/bookings", request, null, UUID.randomUUID()).status()).isEqualTo(422);
    }

    @Test
    void staffRoutesAndStateChangesRequireAuthenticationAndCsrf() throws Exception {
        assertThat(api.get("/api/staff/bookings?date=2026-09-14", null).status()).isEqualTo(401);
        Reply staff = api.send("GET", "/api/staff/bookings?date=2026-09-14", null, null, null,
                "Basic " + Base64.getEncoder().encodeToString("receptionist:integration-staff-password".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(staff.status()).isEqualTo(200);
        HttpResponse<String> noCsrf = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/bookings"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestAt("2026-09-14T09:00:00+06:30")))).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(noCsrf.statusCode()).isEqualTo(403);
        assertThat(api.get("/actuator/env", null).status()).isIn(401, 403);
    }

    @Test
    void databaseIndependentlyRejectsRoomAndTherapistOverlaps() throws Exception {
        UUID bookingId = UUID.fromString(create().path("id").asText());
        UUID anotherRoom = jdbc.queryForObject("SELECT r.id FROM rooms r JOIN bookings b ON r.branch_id=b.branch_id WHERE b.id=? AND r.id<>b.room_id LIMIT 1", UUID.class, bookingId);
        UUID anotherTherapist = jdbc.queryForObject("SELECT t.id FROM therapists t JOIN bookings b ON t.branch_id=b.branch_id JOIN therapist_treatments q ON q.therapist_id=t.id AND q.treatment_id=b.treatment_id WHERE b.id=? AND t.id<>b.therapist_id LIMIT 1", UUID.class, bookingId);
        assertThatThrownBy(() -> cloneReservation(bookingId, anotherRoom, null))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("bookings_no_therapist_overlap");
        assertThatThrownBy(() -> cloneReservation(bookingId, null, anotherTherapist))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("bookings_no_room_overlap");
        assertThatThrownBy(() -> jdbc.update("UPDATE bookings SET room_occupied_until=ends_at WHERE id=?", bookingId))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bookings", Integer.class)).isEqualTo(1);
    }

    @Test
    void simultaneousPaymentRequestsProduceOneReceipt() throws Exception {
        JsonNode booking = create();
        String path = "/api/bookings/" + booking.path("id").asText() + "/payments";
        String token = booking.path("managementToken").asText();
        ApiClient other = new ApiClient();
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> { start.await(); return api.post(path, Map.of(), token, UUID.randomUUID()); });
            var second = executor.submit(() -> { start.await(); return other.post(path, Map.of(), token, UUID.randomUUID()); });
            start.countDown();
            Reply firstReply = first.get(15, TimeUnit.SECONDS);
            Reply secondReply = second.get(15, TimeUnit.SECONDS);
            assertThat(firstReply.status()).isEqualTo(200);
            assertThat(secondReply.status()).isEqualTo(200);
            assertThat(secondReply.json().path("paymentReference").asText()).isEqualTo(firstReply.json().path("paymentReference").asText());
            assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_transactions", Integer.class)).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }

    private void cloneReservation(UUID original, UUID room, UUID therapist) {
        jdbc.update("""
            INSERT INTO bookings (id,version,branch_id,treatment_id,therapist_id,room_id,client_name,client_email,
              client_phone,member,starts_at,ends_at,room_occupied_until,therapist_occupied_until,hold_expires_at,
              created_at,status,payment_status,deposit_amount,currency,idempotency_key,request_fingerprint)
            SELECT ?,version,branch_id,treatment_id,coalesce(?,therapist_id),coalesce(?,room_id),client_name,
              client_email,client_phone,member,starts_at,ends_at,room_occupied_until,therapist_occupied_until,
              hold_expires_at,created_at,status,payment_status,deposit_amount,currency,?,request_fingerprint
            FROM bookings WHERE id=?
            """, UUID.randomUUID(), therapist, room, UUID.randomUUID(), original);
    }

    private JsonNode create() throws Exception {
        Reply response = api.post("/api/bookings", requestAt("2026-09-14T09:00:00+06:30"), null, UUID.randomUUID());
        assertThat(response.status()).describedAs(response.json().toString()).isEqualTo(201);
        return response.json();
    }

    private Map<String, Object> requestAt(String start) {
        JsonNode therapist = catalog.path("therapists").get(0);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("branchId", therapist.path("branchId").asText());
        request.put("treatmentId", therapist.path("treatmentIds").get(0).asText());
        request.put("therapistId", therapist.path("id").asText());
        request.put("startsAt", start);
        request.put("client", Map.of("name", "Test Client", "email", "client@example.com", "phone", "+95 9 123 456 789"));
        return request;
    }

    record Reply(int status, JsonNode json) {}

    final class ApiClient {
        private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        private final String csrf;
        ApiClient() throws Exception {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            csrf = mapper.readTree(response.body()).path("token").asText();
        }
        Reply get(String path, String token) throws Exception { return send("GET", path, null, token, null, null); }
        Reply post(String path, Object body, String token, UUID key) throws Exception { return send("POST", path, body, token, key, null); }
        Reply send(String method, String path, Object body, String token, UUID key, String authorization) throws Exception {
            var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(java.time.Duration.ofSeconds(20));
            if (token != null) builder.header("X-Booking-Token", token);
            if (key != null) builder.header("Idempotency-Key", key.toString());
            if (authorization != null) builder.header("Authorization", authorization);
            if (body != null) builder.header("Content-Type", "application/json").header("X-XSRF-TOKEN", csrf)
                    .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            else builder.GET();
            var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), mapper.readTree(response.body()));
        }
    }

    static final class MutableClock extends Clock {
        private final AtomicReference<Instant> time = new AtomicReference<>(NOW);
        void set(Instant instant) { time.set(instant); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(time.get(), zone); }
        @Override public Instant instant() { return time.get(); }
    }

    @TestConfiguration
    static class TimeConfiguration {
        @Bean @Primary MutableClock integrationClock() { return new MutableClock(); }
    }
}
