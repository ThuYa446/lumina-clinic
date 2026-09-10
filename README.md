# Lumina Clinics — booking exercise

**JDK 17 · Spring Boot 3.5.16 · Angular 22.1.6 / CLI 22.1.7 · PostgreSQL.** Angular lives inside the Spring project at `src/main/frontend`. **One Maven build installs Node/npm, builds and tests Angular, tests Java, and embeds both applications in one executable JAR.** No separate frontend server runs after packaging.

The implementation uses encapsulated domain state, constructor injection, policy objects, repositories, and payment/membership adapters. The [technical handover](docs/technical-notes.md) explains the OOP, SOLID and design-pattern choices.

**Source:** [ThuYa446/lumina-clinic](https://github.com/ThuYa446/lumina-clinic), branch `main`. GitHub Actions verifies the integrated build, PostgreSQL rules and browser flow. Render is configured to deploy `main` after its checks pass.

**Deployment status:** verified locally; Render and Neon account setup is pending, so there is no verified public URL yet. [Deploy this repository to Render](https://render.com/deploy?repo=https://github.com/ThuYa446/lumina-clinic). The [deployment guide](docs/deployment.md) explains the required database and secret values. Deployment configuration alone does not complete the exercise's live-URL requirement.

**Demo with production-oriented structure:** payments are simulated and membership uses one configured demo code/email. Use fictional client details. Actual clinic launch still requires the integrations and operations work under Not done.

## Deliverables

| Exercise item | Artifact |
|---|---|
| Proposal, at most 5 pages | [3-page PDF](docs/proposal.pdf), [Markdown](docs/proposal.md) |
| Technical notes, rules and data model | [Technical handover](docs/technical-notes.md) |
| Booking flowchart | [SVG](docs/booking-flow.svg), [Mermaid and explanation](docs/booking-flow.md) |
| Working system | `src/main/java`, `src/main/frontend`, `src/main/resources/db/migration` |
| Tests and actual failure proof | Java unit/integration, Angular and browser tests; [mutation evidence](docs/test-evidence/mutation-failure.txt) |
| Deployment | [Render + Neon guide](docs/deployment.md), [blueprint](render.yaml), [container](Dockerfile); account connection pending |
| Decisions and six-week plan | [Assumptions and delivery](docs/assumptions-and-delivery.md) |

## Run it

### Windows: build with JDK 17, then run against PostgreSQL

Prerequisites: JDK **17**, internet access for the initial build, PostgreSQL **16 or 17**. Docker Desktop is an optional way to run PostgreSQL. A global Maven or Node installation is unnecessary.

Run from the repository root, in this order:

```powershell
# 1. Use your JDK 17 installation in this terminal.
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version

# 2. Build BOTH applications and run Java + Angular unit tests.
.\mvnw.cmd -B verify

# 3. Start PostgreSQL. Start Docker Desktop first for this option.
Copy-Item .env.example .env
docker compose up -d postgres

# 4. Configure this terminal. Spring does not automatically read .env.
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5433/lumina'
$env:DB_USERNAME = 'lumina'
$env:DB_PASSWORD = 'lumina-local-only'
$env:DEMO_MEMBER_CODE = 'LUMINA-DEMO'
$env:STAFF_PASSWORD = 'local-reception-only'

# 5. Start Spring Boot with the embedded Angular application.
java -jar target/lumina-clinic-1.0.0.jar
```

Open **[http://localhost:8080](http://localhost:8080)**. Health: [localhost:8080/actuator/health](http://localhost:8080/actuator/health). Stop Java with Ctrl+C.

To use an existing PostgreSQL server, skip step 3, create a dedicated empty database, and set its JDBC URL/username/password in step 4. The migration role needs permission to create tables and `btree_gist`. Flyway creates and seeds the schema. Do not point the application at another application's database.

Compose publishes PostgreSQL on **5433** to avoid an existing installation on 5432. Override `POSTGRES_PORT` in `.env` if needed and change the standalone JDBC URL to match. The internal container connection remains 5432. The Spring-only default URL is localhost:5432; the environment above overrides it.

On macOS/Linux, set `JAVA_HOME`, use `./mvnw -B verify`, `export DATABASE_URL=...` etc., then run the JAR. If downloading a ZIP removed executable permissions, run `chmod +x mvnw` first.

### Or run the complete container application

With Docker Desktop running:

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose logs -f app
```

The Docker build executes the same Maven frontend/backend build. Open localhost:8080. `docker compose down` stops services and keeps the database volume. Published ports bind to localhost. Local Compose uses the `local` profile; Render uses `prod` and requires unique secrets.

### Try it

1. Select a treatment, one of six fictional clinics, a future Monday–Saturday, and a therapist/time.
2. Enter fictional contact details. Leave membership blank to reserve a 15-minute payment hold.
3. Click **Pay demo deposit & confirm**. Refresh to verify persistence. Save the private booking link; its bearer token is in a URL fragment, removed after opening and retained in this tab's session storage.
4. Cancel a paid booking at least 24 hours away. It becomes **REFUND_PENDING**.
5. Open **Clinic team** (`/staff`). Local credentials: `receptionist` / `local-reception-only`. Select the date and record a simulated refund reference. Sign out to clear in-memory credentials.
6. Test membership with email `member@example.com` and code `LUMINA-DEMO` when `DEMO_MEMBER_CODE=LUMINA-DEMO` is configured. It confirms without a deposit. Incorrect code/email is rejected. An empty configured code disables exemption.

Times are **Asia/Yangon (Myanmar time, UTC+06:30)** and the deposit is **MMK 300 (Myanmar kyat)**, as requested. The six demonstration branches are **Bahan, Kamayut, Sanchaung, Tamwe, Thingangyun and Yankin** in Yangon. Monday–Saturday 09:00–18:00 hours, addresses, treatments, prices, rooms and therapists remain fictional demo data. Policy constants are centralized in `BookingPolicy`; Angular date formatting uses a shared Myanmar offset regardless of the visitor's browser time zone.

Flyway migration V4 updates the existing demo branch records without changing their IDs. New bookings use MMK for their deposits and payments. Existing bookings retain their recorded amounts, currency and appointment instants; no exchange-rate conversion or appointment rescheduling is performed. Demo treatment prices remain 900, 1,600 and 2,400, now quoted in MMK.

### Development and tests

```powershell
# Full build with real PostgreSQL tests; configure DATABASE_URL first.
.\mvnw.cmd -B -Ppostgres-it verify

# Backend-only iteration, preserving already-built frontend assets:
.\mvnw.cmd -B '-Dfrontend.skip=true' '-Dfrontend.tests.skip=true' test

# Angular development (Node 24.15+ within the 24.x line):
cd src/main/frontend
npm ci
npm start
# localhost:4200 proxies /api to the backend on :8080

npm run test:ci

# Browser tests against the running packaged application on :8080:
npx playwright install chromium
npm run test:e2e
```

Set `DATABASE_URL`, `DB_USERNAME` and `DB_PASSWORD` to a disposable PostgreSQL database for `postgres-it`. Each run creates a unique `lumina_it_<UUID>` schema, migrates it, exercises real HTTP requests, then drops only that schema. It does not truncate the application's public schema. CI provides PostgreSQL 17 and requires this profile. Ordinary `verify` needs no database.

Browser tests create fictional bookings. Configure `E2E_BASE_URL`, `E2E_STAFF_USERNAME` and `E2E_STAFF_PASSWORD` if defaults differ. On Windows, `$env:PLAYWRIGHT_CHANNEL='chrome'` uses installed Chrome. Traces are disabled to keep private request headers out of artifacts; screenshots/reports remain in ignored directories.

Maven lifecycle: `generate-resources` installs pinned Node/npm, runs `npm ci` and Angular production build; `process-resources` copies `dist/lumina/browser` into Java resources; `test` runs Java and Angular tests; `package` produces the executable JAR; optional `postgres-it` runs real database checks. Frontend skipping is an explicit development shortcut, disabled in the normal build and deployment.

## Rules

IDs match [Technical notes → Business rules](docs/technical-notes.md#business-rules). Exact enforcement file/line references in this submission:

<!-- RULE_INDEX_START -->
| Rule | Enforced behavior | File and line |
|---|---|---|
| R1 | Existing, qualified resources at the selected branch | [BookingService:76](src/main/java/com/lumina/clinic/booking/BookingService.java#L76); [V1 schema:66](src/main/resources/db/migration/V1__create_booking_schema.sql#L66) |
| R2 | Only 30/60/90-minute treatments; server-owned prices/deposit | [V3 schema:2](src/main/resources/db/migration/V3__restrict_treatment_durations.sql#L2); [Booking:54](src/main/java/com/lumina/clinic/booking/Booking.java#L54) |
| R3 | Future starts at :00/:30; zero seconds; 90-day horizon | [BookingPolicy:29](src/main/java/com/lumina/clinic/booking/BookingPolicy.java#L29); [BookingPolicy:20](src/main/java/com/lumina/clinic/booking/BookingPolicy.java#L20) |
| R4 | Mon?Sat 09:00?18:00 Myanmar, including both turnarounds | [BookingPolicy:27](src/main/java/com/lumina/clinic/booking/BookingPolicy.java#L27); [BookingPolicy:41](src/main/java/com/lumina/clinic/booking/BookingPolicy.java#L41) |
| R5 | Room always requires 15-minute cleanup and cannot overlap | [ResourceAvailability:18](src/main/java/com/lumina/clinic/booking/ResourceAvailability.java#L18); [V1 schema:73](src/main/resources/db/migration/V1__create_booking_schema.sql#L73) |
| R6 | Therapist has independent turnaround; senior zero still needs clean room | [ResourceAvailability:14](src/main/java/com/lumina/clinic/booking/ResourceAvailability.java#L14); [V1 schema:76](src/main/resources/db/migration/V1__create_booking_schema.sql#L76) |
| R7 | Recheck and allocate under branch lock; DB rejects overlap independently | [BookingService:174](src/main/java/com/lumina/clinic/booking/BookingService.java#L174); [BranchRepository:9](src/main/java/com/lumina/clinic/catalog/BranchRepository.java#L9); [V1 schema:73](src/main/resources/db/migration/V1__create_booking_schema.sql#L73) |
| R8 | Nonmember 300 deposit; 15-minute hold; expires exactly at deadline | [Booking:51](src/main/java/com/lumina/clinic/booking/Booking.java#L51); [Booking:61](src/main/java/com/lumina/clinic/booking/Booking.java#L61); [BookingService:184](src/main/java/com/lumina/clinic/booking/BookingService.java#L184) |
| R9 | Server-verified demo membership grants exemption; invalid code/email rejected | [DemoMembershipVerifier:21](src/main/java/com/lumina/clinic/booking/DemoMembershipVerifier.java#L21) |
| R10 | Creation UUID retry key; identical payload replays, changes conflict | [BookingController:27](src/main/java/com/lumina/clinic/booking/BookingController.java#L27); [BookingService:81](src/main/java/com/lumina/clinic/booking/BookingService.java#L81); [BookingService:214](src/main/java/com/lumina/clinic/booking/BookingService.java#L214) |
| R11 | Private active hold payment; one receipt per booking under retries | [BookingService:112](src/main/java/com/lumina/clinic/booking/BookingService.java#L112); [PaymentTransaction:13](src/main/java/com/lumina/clinic/payment/PaymentTransaction.java#L13); [V1 schema:85](src/main/resources/db/migration/V1__create_booking_schema.sql#L85) |
| R12 | Cancellation at least 24 hours before; paid refund queued; capacity released | [BookingPolicy:49](src/main/java/com/lumina/clinic/booking/BookingPolicy.java#L49); [Booking:78](src/main/java/com/lumina/clinic/booking/Booking.java#L78) |
| R13 | Staff records only pending refunds; nonblank reference; identical retry safe | [Booking:91](src/main/java/com/lumina/clinic/booking/Booking.java#L91); [StaffBookingController:29](src/main/java/com/lumina/clinic/booking/StaffBookingController.java#L29) |
| R14 | Private HMAC capability, staff role and CSRF checks | [BookingTokenService:31](src/main/java/com/lumina/clinic/booking/BookingTokenService.java#L31); [SecurityConfiguration:21](src/main/java/com/lumina/clinic/config/SecurityConfiguration.java#L21) |
| R15 | Validated bounded contact fields; reject unknown fields including ID/DOB | [CreateBookingRequest:16](src/main/java/com/lumina/clinic/booking/CreateBookingRequest.java#L16); [application.yml:24](src/main/resources/application.yml#L24) |
<!-- RULE_INDEX_END -->

Room and therapist intervals are independent. The senior facialist can begin immediately in another clean room; the previous room still needs 15 minutes of cleaning. Availability is only a preview. Creation rechecks capacity under a branch lock, with PostgreSQL exclusion constraints providing independent protection. Hold expiration is request-driven; no scheduled job is required.

## Tests that can fail

Integration tests check concurrent reservation attempts, simultaneous payments, retries, membership, cancellation/refund state, authorization, CSRF, invalid times/therapists and expired-hold replacement. Direct SQL bypasses the allocator to verify that PostgreSQL itself rejects resource overlap and missing room cleanup. Unit tests cover exact time boundaries and therapist exceptions. Angular tests cover lost-response retries, stale availability, failed CSRF setup and the staff response contract.

Actual mutation on 10 September 2026: remove room cleanup. A senior therapist's 17:30–18:00 appointment must be unavailable because the room needs cleaning until 18:15. Removing the rule incorrectly accepts it, so the test fails.

<!-- MUTATION_DIFF_START -->
```diff
--- a/src/main/java/com/lumina/clinic/booking/BookingPolicy.java
+++ b/src/main/java/com/lumina/clinic/booking/BookingPolicy.java
@@ -13,6 +13,6 @@
     public static final BigDecimal DEPOSIT = new BigDecimal("300.00");
     public static final String CURRENCY = "MMK";
     public static final int HOLD_MINUTES = 15;
-    public static final int ROOM_TURNAROUND_MINUTES = 15;
+    public static final int ROOM_TURNAROUND_MINUTES = 0;
     public static final int SLOT_MINUTES = 30;
     public static final int MAX_ADVANCE_DAYS = 90;
```
<!-- MUTATION_DIFF_END -->

Actual failing output excerpt ([complete output](docs/test-evidence/mutation-failure.txt), [exact diff](docs/test-evidence/mutation.diff)):

```text
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BookingPolicyTest.businessHoursIncludeRoomCleanupEvenForSeniorTherapists:18
Expecting value to be false but was true
[INFO] BUILD FAILURE
```

The original rule was restored and the same test passed: [restored output](docs/test-evidence/rule-restored.txt). Reproduce with `python scripts/prove-rule-test.py`. The script captures evidence, always restores the original source in a `finally` block, and requires the expected assertion failure followed by a passing run. No disabled rule remains in the submission.

[Final verification results](docs/verification.md) record the commands, outcomes and platform limits. GitHub Actions runs the full build and PostgreSQL integration tests and preserves reports and the JAR.

## Not done

| Remaining work | Why / next step |
|---|---|
| Public deployment URL | No repository/Render/Neon account was connected. Deploy the supplied unchanged Docker build and follow the live verification checklist. |
| Real payment and refunds | No provider supplied. Add provider idempotency, signed webhooks, durable event deduplication and reconciliation for duplicate/late payments. Replacing the adapter alone is insufficient. |
| Real membership and staff identity | Demo code/email and one staff account prove the flows. Connect authoritative membership, individual staff identities/SSO/MFA and a durable audit trail. |
| Complete receptionist workflow | Daily diary and refund recording work. Next add assisted bookings, paper-diary import, roster/equipment management, rescheduling and authorized overrides. |
| Clinical consent, ID and DOB | Intentionally neither collected nor stored until purpose, access, retention and consent ownership are agreed. |
| Notifications and reconciliation | On-screen confirmation/private links work. Agree email/WhatsApp providers and delivery/retry handling; request-driven expiry itself needs no jobs. |
| Policy confirmation | Myanmar time and MMK follow the user's request. Founder must confirm hours/closures, turnaround exceptions, equipment suitability, booking horizon, actual prices and real branch data. |
| Launch operations and abuse controls | Add rate limits/bot protection, token expiry/revocation/recovery, streaming body limits, retention/deletion, monitoring, least-privilege database roles and tested backups/restoration. Load-test. Free hosting has cold starts and quotas. |

The [proposal](docs/proposal.pdf) distinguishes this first cut from the six-week rollout. No submission email or message was sent on your behalf.

## AI

OpenAI Codex assisted with extracting/interpreting the PDF, identifying ambiguous rules, OOP/SOLID design, Java/Angular/SQL, tests, debugging, mutation proof, styling, documents and deployment research. Parallel assistant work contributed backend, frontend and documentation files. The final application was integrated and tested locally; account-dependent hosting work is marked outstanding. The submitter should review and understand the code for the live session.

## Package the source

```powershell
powershell -ExecutionPolicy Bypass -File scripts/package-source.ps1
```

This creates `lumina-clinic-source.zip` using Git's ignore rules, excluding dependencies, build output, local databases/toolchains, credentials and browser artifacts. The executable is `target/lumina-clinic-1.0.0.jar`. Submit the source ZIP or repository and include the real hosted URL only after deployment succeeds.
