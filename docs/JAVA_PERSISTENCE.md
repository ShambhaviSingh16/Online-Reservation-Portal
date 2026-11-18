# Java Collections, JPA, Hibernate, and Maven in TrackEase

TrackEase uses Spring Data JPA and Hibernate for the train/station catalog and
explicit JDBC transactions for reservations. Maven builds both parts into one
executable WAR. This is an incremental migration, not a second application.

## Follow a real request

1. An administrator submits a train form to `AdminTrainServlet`, or sends
   `POST /api/admin/trains` to `AdminApi`.
2. Both call the Spring-managed `CatalogService`. The servlet obtains the same
   proxied service from the application context during `init()`.
3. The service validates input and establishes the transaction with
   `@Transactional`. Read methods default to read-only transactions.
4. `TrainRepository` extends `JpaRepository` and `JpaSpecificationExecutor`.
   Spring Data generates its implementation and derived queries such as
   `findByActiveTrueOrderByIdAsc` and `existsByNumber`.
5. Hibernate maps `TrainEntity` to the existing `trains` table. PostgreSQL's serial
   primary key supplies the identity. Updates to managed entities are persisted
   by dirty checking when the service transaction commits.
6. The service maps entities to existing `Train`/`Station` DTOs before returning.
   JSON and JSP clients retain their existing fields and never depend on entity
   proxies or an open persistence context.

The entity classes live under `persistence/entity`; repositories live under
`persistence/repository`. `created_by` remains an integer reference to the existing
JDBC-managed user table. User and journey relationships have not been converted
into JPA associations.

## Search and archive behavior

`GET /api/admin/catalog?type=trains&q=express&filter=active&page=0&size=10`
uses a JPA `Specification`, `PageRequest`, and a stable descending ID sort.
Station searches follow the same path. `%`, `_`, and the escape character are
treated literally rather than allowing callers to inject wildcard searches.
Filtering and pagination happen in PostgreSQL, not after loading the full table.
Booking and journey catalog searches retain their existing JDBC implementations.

Archive operations set `is_active=false`; they do not delete the record.
The service obtains a pessimistic row lock for edits/archives, prevents edits to
archived records, and preserves the original train creator. Database uniqueness
constraints remain the final protection against concurrent duplicate creation.
Archived codes/numbers stay reserved so historical references remain unambiguous.

## Transaction boundaries

Both persistence paths share the same Hikari data source. Spring-managed
`JdbcTemplate` writes can join a JPA transaction; the integration test verifies
that both writes roll back together.

The legacy reservation DAOs instead open connections through `DBUtil` and manage
their own transactions. They **do not automatically join** a JPA service transaction.
Booking, cancellation, holds, RAC promotion, and segment occupancy continue using
their existing tested JDBC locks and constraints. Do not wrap those DAOs in a JPA
transaction expecting a single atomic unit without first changing their connection
management and verifying the complete flow.

Flyway remains the only schema migration mechanism:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

No schema change is needed for this migration. Hibernate validates the existing
Flyway-managed tables and does not create or update them.

## Practical Java Collections examples

| Collection | Actual use | Reason |
| --- | --- | --- |
| `List<Train>` / `List<Station>` | Catalog results and DTO mapping | Ordered results for selectors and management pages |
| `Set<String>` | `CatalogService.FILTERS` and reservation class/preference allowlists | Membership checks and unique accepted values |
| `LinkedHashMap<String,Object>` | Catalog row construction | Predictable column order with nullable legacy city/state values |
| `Map<String,Object>` | Paginated responses | Named items, total, page, and size fields without changing the existing API contract |
| `List<String>` | Named passenger groups and searchable field definitions | Preserve passenger order and compose search predicates |

`stream().map(...).toList()` converts entity lists into detached DTO lists. These
are normal production paths, not unused sample classes added for a resume.

## Maven demonstration

`pom.xml` declares `spring-boot-starter-data-jpa`; the Spring Boot parent manages
compatible Spring Data JPA and Hibernate versions. Maven also resolves the JDBC
driver, runs JUnit through Surefire, and packages/repackages the executable WAR.
The compiler targets Java 17.

```powershell
# Unit tests; database integration tests are opt-in
mvn test

# Local PostgreSQL at 127.0.0.1:5433; requires ignored local.properties
mvn "-Dtrackease.integration=true" verify

# Run only the focused JPA integration tests
mvn "-Dtrackease.integration=true" "-Dtest=JpaCatalogIntegrationTest" test

# Inspect the resolved versions
mvn dependency:tree "-Dincludes=org.hibernate.orm:hibernate-core,org.springframework.data:spring-data-jpa"

# Running seeded local app and optional Playwright harness required
node scripts/smoke-jpa.cjs
```

`JpaCatalogIntegrationTest` migrates a disposable local schema with the actual
Flyway migrations and starts real Hibernate/repository proxies. It checks identity
generation, dirty checking, archival with retained ticket history, literal search,
pagination, null legacy fields, concurrent duplicate creation, and rollback across
JPA and Spring JDBC writes. It never targets Render or truncates the demo schema.

Gradle is not added: Maven remains the single maintained build system.

## Interview explanation

“I migrated train and station management to Spring Data JPA with Hibernate while
retaining explicit JDBC for transactional seat allocation. I used repository
queries, Specifications, pagination, dirty checking, and service transactions.
Flyway owns schema changes, and PostgreSQL integration tests verify both paths.”

## References

- [Spring Boot 3.5 SQL/JPA configuration](https://docs.spring.io/spring-boot/3.5/reference/data/sql.html)
- [Spring Data JPA Specifications](https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html)
- [Spring Data JPA transactions](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html)
