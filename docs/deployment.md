# Deploy the review build: Render Free + Neon Free

**Provider documentation checked 10 September 2026.** This repository contains deployment configuration; an online service has not been provisioned by these files alone. A verified public URL requires access to the account owner's Git repository, Render account and Neon database. Do not put credentials in source control or claim a URL that has not passed the checks below.

**Live deployment:** [lumina-clinic.onrender.com](https://lumina-clinic.onrender.com). The owner created the Render Free service in Ohio, connected to the Neon database. `main` is configured to deploy after CI checks pass. Public health and Myanmar/MMK catalogue checks passed on 10 September 2026. The earlier API payment-information blocker is resolved for this service.

## Architecture and free-tier limits

```mermaid
flowchart LR
    Browser[Client / receptionist browser] -->|HTTPS| Render[Render Free web service]
    Render --> Jar[Java 17 Spring Boot JAR<br/>embedded Angular application + API]
    Jar -->|JDBC over verified TLS| Neon[Neon Free PostgreSQL]
    Git[Source repository] -->|Docker build: Maven verify| Render
```

The Docker build runs Maven, which installs its pinned Node/npm toolchain, installs the Angular lockfile, builds/tests the frontend and backend, and packages one executable JAR. The runtime image contains Java 17 and that JAR. There is no separate frontend server, Node runtime, queue, worker or scheduled task.

Render Free currently supplies 512 MB RAM. Services sleep after 15 idle minutes; the next visitor can wait around a minute for startup. The workspace receives 750 free instance hours per month and shared bandwidth/build quotas. Render itself says free instances are unsuitable for production. Render's free PostgreSQL database expires after 30 days; this blueprint therefore uses an external Neon database. [Render free-service limits](https://render.com/docs/free), [Render plan reference](https://render.com/docs/blueprint-spec).

Neon's current Free plan includes 0.5 GB storage and 100 CU-hours per project per month, with compute scaling to zero when idle. These are demo limits, not an availability promise. Review the current plan screen before creating resources; older articles list different quotas. [Neon pricing](https://neon.com/pricing).

This is a simulated-payment review environment. Use fictional client details. Before actual clinic operation, complete payment and membership integration, staff identity and operations workflows, and the launch items in [assumptions and delivery](assumptions-and-delivery.md).

## 1. Verify the same source locally

Install Docker Desktop, start it, then run from the repository root:

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose logs -f app
```

Open `http://localhost:8080`. Both published ports bind to localhost; PostgreSQL is published on host port 5433 (override POSTGRES_PORT if needed), with port 5432 inside the container. Compose overrides the image's `prod` profile with `local` so the documented demo credentials work. `docker compose down` stops the services and retains the PostgreSQL volume. Avoid removing the volume unless you deliberately want to erase local bookings.

To build without Docker, install JDK 17, start PostgreSQL, configure `DATABASE_URL`, `DB_USERNAME` and `DB_PASSWORD` as process environment variables, then run:

```powershell
.\mvnw.cmd -B verify
java -jar target/lumina-clinic-1.0.0.jar
```

The `.env` file is read by Docker Compose, not automatically by Spring or Maven. On macOS/Linux use `./mvnw -B verify`. A globally installed Node or Maven is unnecessary. Database integration tests are opt-in locally and mandatory in CI: `./mvnw -B -Ppostgres-it verify`, against a disposable PostgreSQL database. See the README for complete local commands and test results.

## 2. Create a Neon Free database

1. Create/sign in to a Neon account and keep the **Free** plan.
2. This deployment uses the owner's `LuminaClinic` project with PostgreSQL **18** in AWS Ohio (`aws-us-east-2`); the Render blueprint selects `ohio` to match. CI verifies PostgreSQL **17** as well. For a new deployment, choose a supported PostgreSQL version and keep the database and Render regions close. Name the database `neondb` or record the name you choose.
3. Open **Connect** and select the **direct/unpooled** endpoint. Record the hostname, database, role and password privately. Five application connections are sufficient for this review service. Direct connections also keep Flyway's connection/session behaviour straightforward.
4. Use a role allowed to create the schema and the supported `btree_gist` extension. The first Flyway migration enables it automatically; no manual application-table creation is needed. [Neon btree_gist support](https://neon.com/docs/extensions/btree_gist).

Convert the displayed PostgreSQL URL to this **JDBC** form, with credentials supplied separately:

```text
DATABASE_URL=jdbc:postgresql://YOUR-DIRECT-HOST.neon.tech:5432/neondb?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory
DB_USERNAME=YOUR_NEON_ROLE
DB_PASSWORD=YOUR_NEON_PASSWORD
```

Do not paste a `postgresql://user:password@...` URL into `DATABASE_URL`; Spring expects the JDBC prefix. Keep the full hostname, database name and both SSL parameters. `verify-full` validates hostname and certificate. The specified factory uses Java's default truststore; the standard Temurin image contains public CA roots. The default PostgreSQL factory instead expects its own root-certificate file. [pgJDBC SSL configuration](https://jdbc.postgresql.org/documentation/ssl/), [Neon connection security](https://neon.com/docs/security/security-overview).

For real operation, use separate migration/runtime roles and a documented credential rotation and recovery process. The demo uses one role to keep provisioning small.

## 3. Publish the source and create the Render service

1. The source repository is [ThuYa446/lumina-clinic](https://github.com/ThuYa446/lumina-clinic), branch `main`. GitHub Actions runs the integrated build, PostgreSQL 17 tests and browser checks. Local environment files, IDE state, build output and downloaded dependencies are excluded.
2. Sign in to Render and open [Deploy this repository](https://render.com/deploy?repo=https://github.com/ThuYa446/lumina-clinic), or select **New → Blueprint** and connect the repository. Render reads the root `render.yaml`, which selects `main` and deploys subsequent changes after CI checks pass. Review that the plan says **Free** and that only one web service is created.
3. Supply the values requested for `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `STAFF_PASSWORD` and `DEMO_MEMBER_CODE`. Use a unique staff password of at least **16 characters**. Choose a demo member code to share with the reviewer, or leave membership disabled by using an empty code if the dashboard permits it.
4. The blueprint creates `BOOKING_TOKEN_SECRET` as a random 256-bit value and sets `STAFF_USERNAME=receptionist`, `DEMO_MEMBER_EMAIL=member@example.com`, `DEMO_PAYMENTS_ENABLED=true`, `SPRING_PROFILES_ACTIVE=prod` and `PORT=8080`. The production profile refuses repository-default credentials and short secrets.
5. Create the blueprint and follow the build logs. Successful startup includes Flyway migration, Hibernate schema validation and the HTTP server listening on the configured port. Render then checks `/actuator/health` before routing traffic. [Render first deployment](https://render.com/docs/your-first-deploy), [HTTP health checks](https://render.com/docs/health-checks).

If Blueprint creation is unavailable in your account, create a **Web Service**, connect the same repository, choose the **Docker** runtime and **Free** instance, then copy the above environment values. Add the health path `/actuator/health`. Do not enter a separate build or start command; the Dockerfile supplies both.

Render prompts for `sync: false` values only at initial Blueprint creation; newly added secret variables on an existing service must be set in its Environment screen. Generated secrets persist; keep the booking token secret stable across redeploys so existing private booking tokens remain valid. [Render environment and secret configuration](https://render.com/docs/blueprint-spec).

## 4. Verify the live URL before submitting it

Use the actual `https://…onrender.com` URL shown by Render, then:

- Open `/actuator/health` and confirm HTTP 200 with `{"status":"UP"}`. Wait through one cold start if necessary.
- Open the home page; verify Angular assets and `/api/catalog` load from the same origin.
- Book a future slot using a fictional nonmember, complete the explicitly labelled demo payment, refresh and confirm the booking remains accessible.
- Try the demo member code with `member@example.com`; confirm the deposit is waived. A different email must not qualify.
- Use two separate browsers for the same scarce slot. Only a transaction with available room and therapist capacity may succeed; the other must receive a clear conflict or a different available resource.
- Cancel a paid booking at least 24 hours in the future and confirm it is marked refund pending. Open the staff diary with the configured reception credentials and verify the record.
- Redeploy the same commit and check the booking again. PostgreSQL is the persistence layer; container filesystem contents are disposable.

Record the actual deployed commit SHA, URL, review date and non-sensitive demo instructions in your submission. Share staff credentials privately, not in a public README. The exercise asks for a live URL; infrastructure files alone do not satisfy that item.

## Troubleshooting and operation

| Symptom | Check |
|---|---|
| Production startup rejects configuration | `STAFF_PASSWORD` is unique and at least 16 characters, `BOOKING_TOKEN_SECRET` is random and at least 32 characters, and the database password is not the local default |
| JDBC connection fails | Correct direct hostname/database/role/password, `jdbc:postgresql:` prefix, and SSL factory parameters; inspect server logs without copying secrets |
| Migration cannot install `btree_gist` | Use the Neon database-owner role for initial migrations; ensure the database actually is PostgreSQL |
| Angular build fails or runs out of build memory | Check Node/toolchain downloads and npm lockfile; the runtime memory cap is separate from the build environment |
| Render remains unhealthy | Verify `PORT=8080`, database connectivity and `/actuator/health`; give first startup time to finish migrations |
| First request is slow | Render and Neon can both wake from idle; retest after startup |
| Database appears continually active | Health checks and application activity can consume compute; review Neon usage and pool settings |
| Existing private booking access stops working | Verify `BOOKING_TOKEN_SECRET` was preserved; secret rotation invalidates existing derived access tokens |

Container Java heap is capped at 60% of detected memory, leaving space for metaspace, threads and native memory. The application pool is capped at five connections. Health checks include database readiness. No job scheduler is required for capacity correctness: requests lazily expire stale holds under the same transaction rules as new reservations.

For production, agree a paid availability/backups plan, test restoring a database, set alerts, restrict runtime database privileges, validate HTTPS and proxy trust, and integrate real payment reconciliation before accepting real clients. Retain backward-compatible migrations so a code rollback can still read the updated schema.
