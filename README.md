# TrackEase - Railway Reservation Platform

A Java 17 / Spring Boot 3.5 application with a working JSP interface, session-based
Spring Security authorization, PostgreSQL, and a documented JSON API.
The original Eclipse / javax Servlet / Tomcat 9 project has been migrated to
Jakarta Servlet APIs and embedded Tomcat. No external Tomcat installation is
required now. The executable WAR is built from source.

## Implemented features

- Responsive green-and-ivory interface with a custom railway illustration, shared components, and locally served UI assets.
- Station swapping, local-date shortcuts, journey sorting, ticket status filters, printable tickets, and admin booking search.
- Registration, salted PBKDF2 password hashes, and login-time migration of legacy plaintext passwords.
- Fresh sessions after login, HttpOnly/SameSite cookies, role checks, CSRF tokens, and escaped JSP output.
- Station/date journey search, including full future journeys that offer a waiting list.
- Automatic seat allocation from per-journey inventory, with a unique active-seat index.
- Idempotent booking requests: repeated submissions return the original ticket.
- Book 1–6 named passengers in one transaction, with one booking reference and separate passenger tickets.
- Group bookings roll back if seats run out, unless waiting-list consent allows a mix of confirmed and waitlisted passengers. Seats together are not guaranteed.
- Explicit opt-in waiting list, FIFO promotion, queue position, and cancellation before departure.
- Atomic cancellation and promotion under the same journey lock as booking.
- Ticket ownership checks, booking history, and immutable booked fare for new reservations.
- Archived trains/stations retain historical tickets. Archived records cannot receive new bookings.
- Admin CRUD APIs, database-backed dashboard totals, 30-day booking trends and occupancy counts.
- Booking audit events, pooled JDBC connections, Flyway migrations, Bean Validation and API errors.
- OpenAPI/Swagger UI, Actuator health, executable Docker image, Compose and GitHub Actions CI.

The JSP interface uses local Bootstrap 5.1.3, Font Awesome 6.0.0 (solid/regular fonts),
and jQuery 3.6.0 for retained admin forms. It does not require a third-party CDN.
The railway illustration is an original SVG in `assets/images/rail-landscape.svg`.
The old empty image placeholders are not used. Bookings are demonstrations; no payment is collected.

For the optional desktop/mobile browser checks (using installed Edge or Chrome):

```powershell
npm.cmd install --prefix .local/browser-test --no-audit --no-fund playwright
node scripts/smoke-ui.cjs
node scripts/smoke-group.cjs
```

The browser checks save screenshots under `.local/ui-*.png` and verify the JSP interface
with external network requests blocked. Run the local server and seed database first.

For a group reservation, open a journey's booking dialog, choose 1–6 passengers,
and enter each passenger's full name. The total updates before confirmation.
One submission produces a shared booking reference and individual tickets; each
ticket can be cancelled separately. Without waiting-list consent the entire
booking fails if there are too few seats. With consent, some passengers may be
confirmed while others remain waitlisted. Adjacent seats are not guaranteed.
The JSON equivalents are `POST /api/bookings/group` (journeyId, requestId,
passengerNames, joinWaitingList) and `GET /api/bookings/group/{bookingId}`.
The original single-ticket API remains supported.

## Run in VS Code on this Windows machine

Requirements: JDK 17+, Maven 3.9+, VS Code Extension Pack for Java.
Portable PostgreSQL binaries are already installed at `.local/pgsql` on this machine.
For a fresh checkout, download the [EDB Windows binaries](https://www.enterprisedb.com/download-postgresql-binaries)
and extract them to `.local`, creating `.local/pgsql/bin/pg_ctl.exe`.

Use **Terminal > Run Task > TrackEase: run locally**, or:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

The script starts the isolated PostgreSQL database on `127.0.0.1:5433`, seeds demo
records, builds the application and starts Spring Boot at **http://localhost:8080**.
It never connects to Render. Logs are in `.local/app.log` and `.local/app-error.log`.
Database files persist in `.local/pgdata`; do not delete that directory to rebuild.
The default demo journey is New Delhi (NDLS) to Kanpur Central (CNB), tomorrow.

| Role | Username | Password |
| --- | --- | --- |
| Admin | admin | Admin123! |
| Passenger | demo | Demo123! |

These are public local demo credentials. Do not seed them into a production database.

Stop with the **TrackEase: stop locally** task or:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/stop-local.ps1
```

Use `-KeepDatabase` to stop only the app. Stop the app before rebuilding its WAR.
Restart the run task after code changes; refresh existing browser tabs and search
again. Use an external browser if an embedded preview blocks framing: Spring Security
protects pages against clickjacking.

For debugging, run `scripts/start-local.ps1 -DebugServer`, then select
**Attach to local Tomcat (8000)** and press F5. The debugger attaches to embedded Tomcat.
`-Port 8081` selects another HTTP port. `-SkipBuild` runs an already-built WAR.

## Build and test

```powershell
mvn package
mvn "-Dtrackease.integration=true" test
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-api.ps1
```

For a real browser check of Swagger's **Try it out** authentication flow, install
the optional local test harness and run it with Microsoft Edge or Chrome:

```powershell
npm install --prefix .local/browser-test --no-audit --no-fund playwright
node scripts/smoke-swagger.cjs
```

This checks page rendering, documented security, rejected tokenless login,
authorized login, the refreshed token, authenticated account access and logout.
It saves a screenshot at `.local/swagger-tested.png`.

Ordinary builds run unit tests without a database. Integration tests require the
workspace PostgreSQL database and generate a disposable schema. They refuse
remote database URLs and never truncate demo tables. They cover concurrent
allocation, the final seat, retries, rollback, inventory exhaustion, coach
boundaries, legacy seats, password migration, cancellation, FIFO promotion,
concurrent cancellation, booked fare snapshots and archived trains.

The HTTP checks require a running app. They create local accounts, tickets and a
one-seat test journey. The API script archives its test train and cancels its
tickets; audit/history records remain for inspection.

Output: `target/TrackEase.war`. Run it directly with:

```powershell
java -jar target/TrackEase.war --debug=false
```

`bin`, `build`, the root-level `TrackEasee.war` and external Tomcat folders are not
used by the build. Maven caches dependencies in `.local/maven-repository`.

## JSON API and documentation

- Swagger UI: **http://localhost:8080/swagger-ui.html**
- OpenAPI JSON: **http://localhost:8080/v3/api-docs**
- Health: **http://localhost:8080/actuator/health**

Authentication uses the same session cookie as the JSP interface. JWT is not
needed for this browser-first application. For JSON writes:

1. GET `/api/auth/csrf`, retaining the session cookie.
2. Send its token in the `X-CSRF-Token` header on POST/PUT/DELETE.
3. POST `/api/auth/login` with username/password. Login creates a new session;
   use the new cookie and the `csrfToken` returned in its response.

In Swagger UI, execute `/api/auth/csrf`, then use **Authorize > csrfToken** to
enter the returned token. Execute login and replace the authorized token with
the new `csrfToken` in the login response. The browser keeps the session cookie
automatically; no manual cookie entry is needed. You can then use **Try it out**
for protected requests. Log in with `admin` for administrator endpoints.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET/POST | `/api/auth/csrf`, `/api/auth/login`, `/api/auth/register` | Authentication setup |
| GET/POST | `/api/auth/me`, `/api/auth/logout` | Current account / logout |
| GET | `/api/stations` | Station choices |
| GET | `/api/journeys/search?departureStation=1&arrivalStation=2&travelDate=YYYY-MM-DD` | Journey search |
| POST | `/api/bookings` | `{journeyId, requestId: UUID, joinWaitingList: boolean}` |
| GET/DELETE | `/api/bookings/{id}` | Own ticket / cancel |
| GET | `/api/bookings/history?page=0&size=20` | Paginated own history |
| GET/POST/PUT/DELETE | `/api/admin/trains`, `/stations`, `/journeys` | Admin CRUD; PUT/DELETE use `/{id}` |
| GET | `/api/admin/bookings?page=0&size=20` | Paginated bookings |
| GET | `/api/admin/bookings/{id}/events` | Audit events |
| GET | `/api/admin/analytics` | Dashboard, trends and occupancy counts |

Invalid fields return 400; missing authentication 401; forbidden actions/CSRF
403; another user's ticket 404; unavailable inventory or conflicting records 409.
Identifiers and prices are validated on the server. JSON errors do not expose SQL
or stack traces. An unavailable underlying DAO may report a generic service error;
inspect server logs for diagnostics.

## Booking and waiting-list rules

- One ticket per submission. Use a fresh request UUID for an intentional new booking.
- Inventory snapshots use the train's configured capacity, with up to 80 numbered
  seats per standard coach (S1, S2, etc.). This is a simplified project layout.
- Existing manual ticket labels remain intact and consume their existing quota.
  Confirmed labels matching the inventory are skipped during allocation.
- Joining the waiting list requires explicit consent and does not decrement capacity.
- The queue uses ticket creation order under the journey lock. Cancellation offers
  free capacity to the oldest eligible waiting tickets first, in the same transaction.
- Cancelled tickets and audit history are retained. Cancelling a waiting ticket
  does not release capacity. Repeated cancellation cannot release capacity twice.
- Booking/cancellation close at departure. Retrying an existing request still
  returns that ticket's current state, including CANCELLED.
- New tickets snapshot their fare. Legacy tickets take their current journey fare
  during the migration because their original fare was not stored.
- Schedule times are local railway times in Asia/Kolkata. There is no route-segment
  seat reuse, real railway integration, payment collection, RAC or berth-sharing.
- Configure train capacity before bookings. Journey train/route/capacity changes
  after bookings start are rejected; historical reservations are never rewritten.

Audit events start with this upgrade. A null actor on a promotion means the system
performed it. Dashboard booking value is the sum of CONFIRMED ticket fares, not
collected payment revenue. Trends count all created tickets; occupancy reports
confirmed counts and configured remaining quota.

## Configuration and migrations

The local script creates ignored `local.properties` with a random database password.
`DB_URL` (JDBC PostgreSQL URL), `DB_USER` and `DB_PASSWORD` override those settings.
Clear these environment variables when using the isolated local script. Never
commit `local.properties`, `.env`, database data or logs.

Flyway migrations are under `src/main/resources/db/migration` and run at startup.
Existing schemas are baselined at version 0; idempotent V1 adds the reservation
schema without dropping tickets. Never edit an applied migration: add a new version.
`database/schema.sql` is also used to provision the portable local database and
isolated integration-test schemas. Production startup never inserts demo accounts.

Existing duplicate confirmed seat labels must be reviewed before applying the
unique seat index. No migration silently deletes conflicting bookings.

New passwords must contain 12-128 characters. PBKDF2-HMAC-SHA256 uses a random salt
and 600,000 iterations. Correct login upgrades a legacy plaintext row; dormant
legacy accounts remain plaintext until migrated or reset. CSRF/session tokens
must be refreshed after login or expiration. JSP values are escaped before rendering.

## Docker, Render and CI

```powershell
Copy-Item .env.example .env
# Replace DB_PASSWORD in .env before running:
docker compose up --build
```

Compose creates a separate PostgreSQL volume and an app database schema through
Flyway. It does not seed demo users; register a normal account through the UI.
Promote your own account to admin using an authorized database connection if
needed. The Windows portable demo database remains separate.

The multi-stage Dockerfile builds from source and runs the executable WAR with
embedded Tomcat. On Render, set `DB_URL`, `DB_USER`, `DB_PASSWORD` and
`SPRING_PROFILES_ACTIVE=prod`. Render's `PORT` is honored. Production cookies
require HTTPS; forwarded headers support the hosting proxy. No live deployment
is performed automatically by editing this repository.

GitHub Actions builds on Java 17, runs the unit and PostgreSQL integration tests,
and builds the Docker image. It does not deploy or require a Render token.
The workflow is prepared for GitHub execution; local checks are separate evidence.

## Architecture and scope

`api` contains validated Spring MVC endpoints; `service` contains API ownership and
reservation orchestration; `dao` retains the tested PostgreSQL/JDBC transactions.
The application uses Hikari pooling, Flyway and Spring Security. Existing Jakarta
servlets continue serving the JSP workflows during this incremental modernization.
This is not a JPA/Hibernate rewrite, and no ORM or JWT claims are made.

Optional follow-ups are email delivery, downloadable PDF tickets, a seat map,
multiple classes/preferences, RAC, payment sandbox integration and a separate
frontend. These are not required to exercise the implemented booking lifecycle.
The framework version is pinned in `pom.xml`; review release support/security
updates before a public production rollout.
