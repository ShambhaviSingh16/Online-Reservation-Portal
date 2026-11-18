package com.trainticket.persistence;

import com.trainticket.model.*;
import com.trainticket.persistence.repository.*;
import com.trainticket.service.CatalogService;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Real Hibernate and repository proxies against a disposable, Flyway-migrated local schema. */
@EnabledIfSystemProperty(named = "trackease.integration", matches = "true")
class JpaCatalogIntegrationTest {
    private static PGSimpleDataSource source;
    private static String schema;
    private static AnnotationConfigApplicationContext context;
    private static CatalogService catalog;
    private static JdbcTemplate jdbc;

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = TrainRepository.class)
    static class PersistenceConfig {
        @Bean DataSource dataSource() { return source; }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.trainticket.persistence.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean CatalogService catalogService(TrainRepository trains, StationRepository stations) {
            return new CatalogService(trains, stations);
        }
    }

    @BeforeAll static void setup() throws Exception {
        for (String env : List.of("DB_URL", "DB_USER", "DB_PASSWORD")) assertNull(System.getenv(env), "Clear " + env + " for isolated tests");
        Properties settings = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("local.properties"))) { settings.load(in); }
        String url = settings.getProperty("db.url");
        assertEquals("jdbc:postgresql://127.0.0.1:5433/trackease", url);
        schema = "jpa_test_" + UUID.randomUUID().toString().replace("-", "");
        source = new PGSimpleDataSource();
        source.setURL(url + "?currentSchema=" + schema);
        source.setUser(settings.getProperty("db.user")); source.setPassword(settings.getProperty("db.password"));
        // Uses the same migrations as production, including existing serial IDs and constraints.
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
        context = new AnnotationConfigApplicationContext(PersistenceConfig.class);
        catalog = context.getBean(CatalogService.class);
        jdbc = new JdbcTemplate(source);
    }

    @BeforeEach void fixture() {
        jdbc.execute("TRUNCATE users,stations,trains,journeys,tickets RESTART IDENTITY CASCADE");
        jdbc.update("INSERT INTO users(username,password,email,full_name,role) VALUES ('jpaadmin','unused','jpa@example.test','JPA Admin','admin')");
    }

    @AfterAll static void cleanup() throws Exception {
        if (context != null) context.close();
        if (source != null && schema != null) try (Connection c = source.getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private static Train train(String number) { return new Train(0, number, "JPA Express", 80); }
    private static Station station(String code) { return new Station(0, code, "JPA Central", "Kanpur", "UP"); }

    @Test void createsIdentityRecordsAndDirtyCheckingPersistsEditsWithoutChangingCreator() {
        assertTrue(catalog.addTrain(train("JP100"), 1));
        Train t = catalog.getAllTrains().get(0);
        assertTrue(t.getTrainId() > 0);
        t.setTrainName("Updated Express"); t.setTotalSeats(120);
        assertTrue(catalog.updateTrain(t));
        assertEquals("Updated Express", jdbc.queryForObject("SELECT train_name FROM trains WHERE train_id=?", String.class, t.getTrainId()));
        assertEquals(1, jdbc.queryForObject("SELECT created_by FROM trains WHERE train_id=?", Integer.class, t.getTrainId()));
        assertTrue(catalog.addStation(station("jpc")));
        Station s = catalog.getAllStations().get(0);
        assertEquals("JPC", s.getStationCode());
        s.setCity("Lucknow"); assertTrue(catalog.updateStation(s));
        assertEquals("Lucknow", catalog.getStationById(s.getStationId()).getCity());
    }

    @Test void archivesPreserveJdbcJourneyAndTicketHistoryAndCannotBeEditedOrRecreated() {
        catalog.addTrain(train("JP100"), 1); catalog.addStation(station("AA")); catalog.addStation(station("BB"));
        jdbc.update("INSERT INTO journeys(train_id,departure_station_id,arrival_station_id,departure_time,arrival_time,fare,available_seats) VALUES(1,1,2,NOW()+INTERVAL '1 day',NOW()+INTERVAL '2 days',100,80)");
        jdbc.update("INSERT INTO tickets(user_id,journey_id,seat_number,coach_number) VALUES(1,1,'1','S1')");
        Train t = catalog.getAllTrains().get(0); Station s = catalog.getStationById(1);
        assertTrue(catalog.deleteTrain(1)); assertTrue(catalog.deleteStation(1));
        assertFalse(catalog.deleteTrain(1)); assertFalse(catalog.deleteStation(1));
        assertFalse(catalog.updateTrain(t)); assertFalse(catalog.updateStation(s));
        assertFalse(catalog.addTrain(train("JP100"), 1)); assertFalse(catalog.addStation(station("AA")));
        assertTrue(catalog.getAllTrains().isEmpty()); assertNull(catalog.getStationById(1));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM tickets k JOIN journeys j USING(journey_id) JOIN trains t USING(train_id) JOIN stations s ON s.station_id=j.departure_station_id WHERE NOT t.is_active AND NOT s.is_active", Integer.class));
    }

    @Test void specificationsPaginateFilterAndTreatWildcardsLiterallyIncludingNullLegacyColumns() {
        for (int i = 0; i < 12; i++) catalog.addTrain(new Train(0, "JP" + i, "Search Express", 40), 1);
        catalog.addTrain(new Train(0, "LITERAL", "100%_! Express", 40), 1);
        var page = catalog.search("trains", "search express", "active", 1, 10);
        assertEquals(12L, page.get("total")); assertEquals(2, ((List<?>) page.get("items")).size());
        assertEquals(1L, catalog.search("trains", "%_!", "all", 0, 10).get("total"));
        catalog.deleteTrain(1);
        assertEquals(1L, catalog.search("trains", "", "archived", 0, 10).get("total"));
        jdbc.update("INSERT INTO stations(station_code,station_name) VALUES('NULLCITY','Legacy Stop')");
        var rows = (List<?>) catalog.search("stations", "legacy", "active", 0, 10).get("items");
        assertEquals(1, rows.size()); assertNull(((Map<?, ?>) rows.get(0)).get("city"));
        assertThrows(IllegalArgumentException.class, () -> catalog.search("trains", "", "invalid", 0, 10));
    }

    @Test void jpaAndSpringJdbcParticipateInTheSameServiceTransactionAndRollbackTogether() {
        var tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            catalog.addTrain(train("ROLLBACK"), 1);
            jdbc.update("INSERT INTO stations(station_code,station_name) VALUES('ROLLBACK','Rollback Stop')");
            throw new IllegalStateException("Force rollback after both writes");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM trains", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM stations", Integer.class));
        assertThrows(DataIntegrityViolationException.class, () -> catalog.addTrain(train("BADOWNER"), 999));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM trains", Integer.class));
    }

    @Test void concurrentDuplicateCreationProducesOneRecordAndOneSuccess() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> add = () -> { start.await(); try { return catalog.addTrain(train("RACE"), 1); }
            catch (DataIntegrityViolationException e) { return false; } };
        try {
            Future<Boolean> a = pool.submit(add), b = pool.submit(add); start.countDown();
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM trains WHERE train_number='RACE'", Integer.class));
        } finally { pool.shutdownNow(); }
    }
}
