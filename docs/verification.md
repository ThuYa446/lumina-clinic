# Verification record

Executed on **10 September 2026** in the supplied Windows workspace. The final application was built from source with Microsoft OpenJDK **17.0.18**, Maven wrapper **3.9.11**, Maven-managed Node **24.15.0** / npm **11.11.1**, and PostgreSQL **16.2** in a separate local cluster on port 55432. The user's existing PostgreSQL service was not changed. Compose targets PostgreSQL 17; the subsequent CI and Neon checks are recorded below.

The subsequent [GitHub Actions run](https://github.com/ThuYa446/lumina-clinic/actions/runs/34443155497) for commit `ec049399ca23d6a13ae4d1d5558965e8026f4575` passed on Ubuntu with JDK 17 and PostgreSQL 17: all **49 tests** passed, and GitHub stored the test reports and deployable JAR. Its [actual log summary](test-evidence/github-ci-summary.txt) is included here. The owner's new Neon PostgreSQL **18.6** database also passed production-profile startup, all four Flyway migrations, Hibernate schema validation, verified TLS connectivity and Myanmar catalog/health checks. The bundled Flyway version warns that its advertised PostgreSQL support ends at 17; PostgreSQL 18 results are specific to this observed deployment check.

All **3 browser tests also passed against Neon** using the production profile: booking, payment/retry protection, reload, cancellation, staff refund recording, mobile layout and private-link access checks. This run took 59.7 seconds because the local application connected across regions to Ohio; the Render blueprint now selects Ohio alongside the database. The temporary local verification process was stopped afterward to release its connections. See the [Neon verification summary](test-evidence/neon-summary.txt).

## Final results

| Check | Actual result |
|---|---|
| `mvnw.cmd -B -ntp -Ppostgres-it clean verify` | **BUILD SUCCESS**, 2 minutes 2 seconds with cached toolchain downloads |
| Java unit tests | **30 passed**, zero failures/errors/skips |
| PostgreSQL integration tests | **10 passed**, zero failures/errors/skips; real HTTP, Spring Security, Flyway and PostgreSQL constraints |
| Angular / Vitest tests, invoked by Maven | **6 passed** in 3 files |
| `npm run test:e2e`, installed Chrome | **3 passed**, 9.7 seconds; real packaged application on port 8080, browser time zone America/New_York |
| Packaged JAR inspection | Contains Angular `index.html`, 6 JavaScript bundles and the production stylesheet under `BOOT-INF/classes/static`; includes all 4 Flyway migrations |
| Myanmar upgrade | Existing V3 demo database upgraded to V4; all 2 existing booking rows and 2 payment rows matched their pre-upgrade checksums; catalog shows six Yangon branches, Asia/Yangon and MMK |
| HTTP health | `/actuator/health` returned HTTP 200 and `{"status":"UP"}` |
| `docker compose config --quiet` | Passed configuration parsing |
| Founder proposal | PDF parsed successfully, **3 pages** |
| Deliberately disabled cleaning rule | Focused test **failed** with 1 assertion failure; source restored and the same test passed |

The final Maven command was run with `DATABASE_URL=jdbc:postgresql://127.0.0.1:55432/lumina_test`, `DB_USERNAME=lumina`, and a test-only password. Integration tests create and remove an isolated random schema. Browser checks used the separate `lumina_demo` database and fictional clients. The review application ran with a 256 MB Java heap; this is a functional check, not a load/capacity certification.

The build's real summary is preserved in [build-summary.txt](test-evidence/build-summary.txt). Full Java reports are generated under `target/surefire-reports` and `target/failsafe-reports`. The [mutation diff](test-evidence/mutation.diff), [failing output](test-evidence/mutation-failure.txt) and [restored output](test-evidence/rule-restored.txt) are committed evidence, with a reproducible script referenced by the README.

The Myanmar update passed **49 tests in total** (30 Java unit, 10 PostgreSQL integration, 6 Angular unit and 3 browser tests). The [browser test summary](test-evidence/browser-summary.txt) records the completed client and staff flows. The [database upgrade record](test-evidence/myanmar-upgrade.txt) confirms existing booking/payment data was preserved. The checksums were compared before running browser tests, which create an additional fictional booking and demo payment.

## What the tests exercise

- Two concurrent create requests cannot reserve the same therapist/time. Database tests independently bypass the allocator and attempt room and therapist overlaps; PostgreSQL rejects both and enforces room cleanup.
- A booking retry returns the same ID; changed payload with the same key conflicts. Simultaneous payment requests with different retry keys produce one receipt. Retrying payment does not add another receipt.
- A hold expires exactly at its deadline without a scheduler; capacity can be reused and a late payment is rejected. Normal/senior therapist intervals, half-hour starts, business hours, date horizon and the exact 24-hour cancellation boundary are covered.
- Deposit exemptions require the configured demo membership code/email. Extra client-controlled fields are rejected. Missing CSRF and staff authentication fail; a wrong private booking token cannot retrieve the booking. Public deployment rejects the documented local credentials.
- Angular retains idempotency keys after a lost response, discards obsolete availability responses, stops mutations when CSRF setup fails, and reads the actual staff response envelope.
- Myanmar dates roll over at exactly 17:30 UTC. API tests verify Yangon locations, the +06:30 offset, a 09:00 opening corresponding to 02:30 UTC, and MMK in stored payments. Chrome runs in America/New_York while asserting Myanmar times in slot selection, booking details and the staff diary, plus MMK prices and deposits.
- Chrome completes booking, demo payment, page reload, cancellation, staff sign-in, refund recording and sign-out. A fresh session cannot open a private booking without its token. The mobile page fits a 390-pixel viewport.
- Visual review caught an Angular critical-CSS loader blocked by the Content Security Policy. The final build disables that inline loader. Browser assertions now verify applied layout styles and fail on browser console errors during the booking flow.

## Screenshots of the actual packaged application

[Desktop booking page](screenshots/desktop.png) · [Mobile booking page](screenshots/mobile.png) · [Confirmed appointment](screenshots/confirmed.png)

## Boundaries

Docker configuration was parsed, but the Docker daemon was not available for an actual image build/container run. GitHub Actions has now passed remotely with PostgreSQL 17 and Chromium. Neon is connected and the application schema is provisioned. Render authentication succeeded, but service creation with `plan: free` returned HTTP 402 (`Payment information is required`); no Render service or public deployment URL has been verified. No real payment, membership provider, WhatsApp/email delivery or production load test was performed. These are recorded limitations, not passing checks.
