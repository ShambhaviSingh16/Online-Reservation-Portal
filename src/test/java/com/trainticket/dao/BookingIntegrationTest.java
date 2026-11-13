package com.trainticket.dao;

import com.trainticket.dao.TicketDAO.BookingResult;
import com.trainticket.model.Ticket;
import java.nio.file.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Uses a disposable schema on the workspace PostgreSQL, never Render or demo records. */
@EnabledIfSystemProperty(named = "trackease.integration", matches = "true")
class BookingIntegrationTest {
    private static Properties settings;
    private static String schema;
    private static Path config;
    private static String oldConfig;
    private int journey;
    private int user;

    private static Connection connection() throws SQLException {
        Connection conn = DriverManager.getConnection(settings.getProperty("db.url"),
                settings.getProperty("db.user"), settings.getProperty("db.password"));
        conn.setSchema(schema);
        return conn;
    }

    @BeforeAll static void setup() throws Exception {
        for (String env : List.of("DB_URL", "DB_USER", "DB_PASSWORD")) {
            assertNull(System.getenv(env), "Clear " + env + " to isolate integration tests");
        }
        settings = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("local.properties"))) { settings.load(in); }
        assertEquals("jdbc:postgresql://127.0.0.1:5433/trackease", settings.getProperty("db.url"));
        schema = "booking_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection conn = connection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA " + schema);
            conn.setSchema(schema);
            for (String sql : Files.readString(Path.of("database/schema.sql")).split(";")) {
                // SQL comments may contain semicolons, so schema.sql keeps statements simple.
                if (!sql.isBlank()) { stmt.execute(sql); }
            }
        }
        Properties testSettings = new Properties();
        testSettings.putAll(settings);
        testSettings.setProperty("db.url", settings.getProperty("db.url") + "?currentSchema=" + schema);
        config = Files.createTempFile(Path.of("target"), "booking-test-", ".properties");
        try (OutputStream out = Files.newOutputStream(config)) { testSettings.store(out, "Disposable test schema"); }
        oldConfig = System.getProperty("trackease.config");
        System.setProperty("trackease.config", config.toAbsolutePath().toString());
    }

    @BeforeEach void fixture() throws Exception {
        try (Connection conn = connection(); Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE tickets, journeys, trains, stations, users RESTART IDENTITY CASCADE");
            stmt.execute("INSERT INTO users(username,password,email,full_name) VALUES ('test','test','test@local','Test')");
            stmt.execute("INSERT INTO stations(station_code,station_name) VALUES ('A','A'),('B','B')");
            stmt.execute("INSERT INTO trains(train_number,train_name,total_seats) VALUES ('T','Test',10)");
            stmt.execute("INSERT INTO journeys(train_id,departure_station_id,arrival_station_id,departure_time,arrival_time,fare,available_seats) "
                    + "VALUES (1,1,2,NOW()+INTERVAL '1 day',NOW()+INTERVAL '2 days',100,10)");
        }
        user = 1;
        journey = 1;
    }

    private Ticket ticket(String seat) {
        Ticket ticket = new Ticket();
        ticket.setJourneyId(journey); ticket.setUserId(user);
        ticket.setSeatNumber(seat); ticket.setCoachNumber("s1");
        return ticket;
    }

    @Test void groupAssignsDistinctSeatsAndPreservesPassengerNamesOnRetry() throws Exception {
        TicketDAO dao=new TicketDAO();String key=UUID.randomUUID().toString();
        var first=dao.reserveGroup(user,journey,key,List.of("Asha Rao","Ravi Shah","Meera Singh"),false);
        assertEquals(TicketDAO.GroupResult.BOOKED,first.result());
        assertEquals(3,first.tickets().stream().map(t -> t.getSeatNumber()+t.getCoachNumber()).distinct().count());
        assertEquals(7,scalar("SELECT available_seats FROM journeys WHERE journey_id=1"));
        var retry=dao.reserveGroup(user,journey,key,List.of("Asha Rao","Ravi Shah","Meera Singh"),false);
        assertEquals(first.tickets().stream().map(Ticket::getTicketId).toList(),retry.tickets().stream().map(Ticket::getTicketId).toList());
        assertEquals(List.of("Asha Rao","Ravi Shah","Meera Singh"),dao.getBookingGroup(user,key).stream().map(Ticket::getPassengerName).toList());
        assertTrue(dao.getBookingGroup(user+1,key).isEmpty());
        assertEquals(TicketDAO.GroupResult.INVALID,dao.reserveGroup(user,journey,key,List.of("Someone else"),true).result());
        assertEquals(3,scalar("SELECT COUNT(*) FROM tickets"));
    }

    @Test void insufficientGroupCapacityRollsBackEveryTicketAndAuditEvent() throws Exception {
        try(Connection c=connection();Statement s=c.createStatement()){s.execute("UPDATE journeys SET available_seats=2");}
        var result=new TicketDAO().reserveGroup(user,journey,UUID.randomUUID().toString(),List.of("A","B","C"),false);
        assertEquals(TicketDAO.GroupResult.UNAVAILABLE,result.result());assertTrue(result.tickets().isEmpty());
        assertEquals(0,scalar("SELECT COUNT(*) FROM tickets"));assertEquals(0,scalar("SELECT COUNT(*) FROM booking_events"));
        assertEquals(2,scalar("SELECT available_seats FROM journeys WHERE journey_id=1"));
    }

    @Test void groupWaitingListPromotesAndCancelsPassengersIndependently() throws Exception {
        try(Connection c=connection();Statement s=c.createStatement()){s.execute("UPDATE journeys SET available_seats=1");}
        TicketDAO dao=new TicketDAO();String key=UUID.randomUUID().toString();
        var group=dao.reserveGroup(user,journey,key,List.of("First","Second","Third"),true);
        assertEquals(TicketDAO.GroupResult.BOOKED,group.result());
        assertEquals(List.of("CONFIRMED","WAITLISTED","WAITLISTED"),group.tickets().stream().map(Ticket::getStatus).toList());
        assertEquals(List.of(0,1,2),dao.getBookingGroup(user,key).stream().map(Ticket::getWaitingPosition).toList());
        assertEquals(TicketDAO.CancellationResult.CANCELLED,dao.cancelTicket(group.tickets().get(0).getTicketId(),user));
        assertEquals(List.of("CANCELLED","CONFIRMED","WAITLISTED"),dao.getBookingGroup(user,key).stream().map(Ticket::getStatus).toList());
        assertEquals(TicketDAO.GroupResult.BOOKED,dao.reserveGroup(user,journey,key,List.of("First","Second","Third"),true).result());
        assertEquals(3,scalar("SELECT COUNT(*) FROM tickets"));
    }

    @Test void invalidGroupAndFailedInsertLeaveCapacityUnchanged() throws Exception {
        TicketDAO dao=new TicketDAO();
        for(List<String> names:List.of(List.<String>of(),List.of(" "),List.of("x".repeat(101)),List.of("A","B","C","D","E","F","G"))) {
            assertEquals(TicketDAO.GroupResult.INVALID,dao.reserveGroup(user,journey,UUID.randomUUID().toString(),names,false).result());
        }
        assertEquals(TicketDAO.GroupResult.INVALID,dao.reserveGroup(user,journey,"bad",List.of("A"),false).result());
        assertEquals(TicketDAO.GroupResult.FAILED,dao.reserveGroup(999,journey,UUID.randomUUID().toString(),List.of("A","B"),false).result());
        assertEquals(0,scalar("SELECT COUNT(*) FROM tickets"));assertEquals(10,scalar("SELECT available_seats FROM journeys"));
    }

    @Test void concurrentGroupsCannotOverbookOrCreatePartialGroups() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(3);
        try {
            List<Callable<TicketDAO.GroupBooking>> tasks=new ArrayList<>();
            for(int i=0;i<3;i++) tasks.add(()->new TicketDAO().reserveGroup(user,journey,UUID.randomUUID().toString(),List.of("A","B","C","D"),false));
            List<TicketDAO.GroupBooking> results=new ArrayList<>();for(var f:pool.invokeAll(tasks)) results.add(f.get(20,TimeUnit.SECONDS));
            assertEquals(2,results.stream().filter(r -> r.result()==TicketDAO.GroupResult.BOOKED).count());
            assertEquals(8,scalar("SELECT COUNT(*) FROM tickets"));assertEquals(2,scalar("SELECT available_seats FROM journeys"));
        } finally {pool.shutdownNow();}
    }

    @Test void concurrentGroupRetriesCreateOneGroup() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(3);String key=UUID.randomUUID().toString();
        try {
            List<Callable<TicketDAO.GroupBooking>> tasks=new ArrayList<>();
            for(int i=0;i<3;i++) tasks.add(()->new TicketDAO().reserveGroup(user,journey,key,List.of("A","B","C"),false));
            Set<List<Integer>> groups=new HashSet<>();
            for(var future:pool.invokeAll(tasks)) {
                var result=future.get(20,TimeUnit.SECONDS);assertEquals(TicketDAO.GroupResult.BOOKED,result.result());
                groups.add(result.tickets().stream().map(Ticket::getTicketId).toList());
            }
            assertEquals(1,groups.size());assertEquals(3,scalar("SELECT COUNT(*) FROM tickets"));
        } finally {pool.shutdownNow();}
    }

    private int scalar(String sql) throws Exception {
        try (Connection conn = connection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            rs.next(); return rs.getInt(1);
        }
    }

    private List<BookingResult> race(boolean sameSeat) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        String sharedRequest = UUID.randomUUID().toString();
        try {
            List<Future<BookingResult>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                final String seat = sameSeat ? " a1 " : "A" + i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("Race did not start"); }
                    return new TicketDAO().reserveTicket(ticket(seat), sameSeat ? sharedRequest : UUID.randomUUID().toString());
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            List<BookingResult> results = new ArrayList<>();
            for (Future<BookingResult> future : futures) { results.add(future.get(20, TimeUnit.SECONDS)); }
            return results;
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void concurrentRetriesReturnOneTicket() throws Exception {
        List<BookingResult> results = race(true);
        assertEquals(8, Collections.frequency(results, BookingResult.CONFIRMED));
        assertEquals(1, scalar("SELECT COUNT(*) FROM tickets"));
        assertEquals(9, scalar("SELECT available_seats FROM journeys"));
    }

    @Test void lastSeatCannotBeOversold() throws Exception {
        try (Connection conn = connection(); Statement stmt = conn.createStatement()) { stmt.executeUpdate("UPDATE journeys SET available_seats=1"); }
        List<BookingResult> results = race(false);
        assertEquals(1, Collections.frequency(results, BookingResult.CONFIRMED));
        assertEquals(7, Collections.frequency(results, BookingResult.UNAVAILABLE));
        assertEquals(0, scalar("SELECT available_seats FROM journeys"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM tickets"));
    }

    @Test void insertFailureRollsBackSeatDecrement() throws Exception {
        Ticket ticket = ticket("A1"); ticket.setUserId(999);
        assertEquals(BookingResult.FAILED, new TicketDAO().reserveTicket(ticket));
        assertEquals(0, ticket.getTicketId());
        assertEquals(10, scalar("SELECT available_seats FROM journeys"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM tickets"));
    }

    @Test void invalidAndDepartedJourneysCannotBeBooked() throws Exception {
        Ticket invalid = ticket("ignored"); invalid.setJourneyId(0);
        assertEquals(BookingResult.INVALID, new TicketDAO().reserveTicket(invalid));
        try (Connection conn = connection(); Statement stmt = conn.createStatement()) { stmt.executeUpdate("UPDATE journeys SET departure_time=NOW()-INTERVAL '1 hour'"); }
        assertEquals(BookingResult.UNAVAILABLE, new TicketDAO().reserveTicket(ticket("A1")));
        assertEquals(10, scalar("SELECT available_seats FROM journeys"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM tickets"));
    }

    @Test void databaseRejectsDuplicateNormalizedActiveSeat() throws Exception {
        assertEquals(BookingResult.CONFIRMED, new TicketDAO().reserveTicket(ticket("A1")));
        try (Connection conn = connection(); Statement stmt = conn.createStatement()) {
            SQLException error = assertThrows(SQLException.class, () -> stmt.executeUpdate(
                    "INSERT INTO tickets(journey_id,user_id,seat_number,coach_number) VALUES(1,1,' 1 ',' s1 ' )"));
            assertEquals("23505", error.getSQLState());
        }
    }

    @Test void simultaneousBookingsGetDistinctInventorySeats() throws Exception {
        assertEquals(8, Collections.frequency(race(false), BookingResult.CONFIRMED));
        assertEquals(8, scalar("SELECT COUNT(DISTINCT (coach_number,seat_number)) FROM tickets"));
        assertEquals(8, scalar("SELECT COUNT(*) FROM tickets t JOIN journey_seats s USING(journey_id,coach_number,seat_number)"));
        assertEquals(2, scalar("SELECT available_seats FROM journeys"));
    }

    @Test void retryStillReturnsSameTicketWhenJourneyIsFull() throws Exception {
        try (Connection conn = connection(); Statement stmt = conn.createStatement()) { stmt.executeUpdate("UPDATE journeys SET available_seats=1"); }
        String request = UUID.randomUUID().toString();
        Ticket first = ticket("999"); Ticket retry = ticket("nonexistent");
        assertEquals(BookingResult.CONFIRMED, new TicketDAO().reserveTicket(first, request));
        assertEquals(BookingResult.CONFIRMED, new TicketDAO().reserveTicket(retry, request));
        assertEquals(first.getTicketId(), retry.getTicketId());
        assertEquals("1", retry.getSeatNumber());
        assertEquals("S1", retry.getCoachNumber());
        assertEquals(1, scalar("SELECT COUNT(*) FROM tickets"));
        assertEquals(0, scalar("SELECT available_seats FROM journeys"));
    }

    @Test void existingManualTicketIsPreservedAndItsSeatSkipped() throws Exception {
        try (Connection conn = connection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO tickets(journey_id,user_id,seat_number,coach_number) VALUES(1,1,' 1 ','s1')");
            stmt.executeUpdate("UPDATE journeys SET available_seats=9");
        }
        Ticket allocated = ticket("ignored");
        assertEquals(BookingResult.CONFIRMED, new TicketDAO().reserveTicket(allocated));
        assertEquals("2", allocated.getSeatNumber());
        assertEquals(1, scalar("SELECT COUNT(*) FROM tickets WHERE seat_number=' 1 ' AND coach_number='s1'"));
    }

    @Test void exhaustedInventoryRollsBackEvenWithStaleCapacity() throws Exception {
        try (Connection conn = connection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE trains SET total_seats=1");
            stmt.executeUpdate("INSERT INTO tickets(journey_id,user_id,seat_number,coach_number) VALUES(1,1,'1','S1')");
        }
        assertEquals(BookingResult.UNAVAILABLE, new TicketDAO().reserveTicket(ticket("ignored")));
        assertEquals(10, scalar("SELECT available_seats FROM journeys"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM tickets"));
    }

    @Test void coachBoundaryUsesConfiguredTrainCapacity() throws Exception {
        try (Connection conn = connection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE trains SET total_seats=81");
            stmt.executeUpdate("INSERT INTO tickets(journey_id,user_id,seat_number,coach_number) SELECT 1,1,n::text,'S1' FROM generate_series(1,80) n");
            stmt.executeUpdate("UPDATE journeys SET available_seats=1");
        }
        Ticket allocated = ticket("ignored");
        assertEquals(BookingResult.CONFIRMED, new TicketDAO().reserveTicket(allocated));
        assertEquals("1", allocated.getSeatNumber());
        assertEquals("S2", allocated.getCoachNumber());
    }

    @Test void legacyPasswordUpgradesOnlyAfterCorrectLogin() throws Exception {
        assertNull(new UserDAO().authenticate("test", "wrong"));
        try (Connection conn = connection(); Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT password FROM users WHERE user_id=1")) {
            rs.next(); assertEquals("test", rs.getString(1));
        }
        assertNotNull(new UserDAO().authenticate("test", "test"));
        try (Connection conn = connection(); Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT password FROM users WHERE user_id=1")) {
            rs.next(); assertTrue(com.trainticket.util.PasswordHasher.verify("test", rs.getString(1)));
        }
        assertNotNull(new UserDAO().authenticate("test", "test"));
    }

    @Test void registrationStoresHashAndDoesNotExposeItInUser() throws Exception {
        com.trainticket.model.User account = new com.trainticket.model.User();
        account.setUsername("newuser"); account.setPassword("NewPassword123!");
        account.setEmail("new@local.test"); account.setFullName("New User");
        assertTrue(new UserDAO().registerUser(account));
        com.trainticket.model.User authenticated = new UserDAO().authenticate("newuser", "NewPassword123!");
        assertNotNull(authenticated);
        assertNull(authenticated.getPassword());
        assertNull(new UserDAO().getUserById(authenticated.getUserId()).getPassword());
        try (Connection conn = connection(); Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT password FROM users WHERE username='newuser'")) {
            rs.next(); assertTrue(com.trainticket.util.PasswordHasher.verify("NewPassword123!", rs.getString(1)));
        }
        assertNull(new UserDAO().authenticate("newuser", "WrongPassword!"));
    }

    private void capacity(int count) throws Exception {
        try(Connection conn=connection();Statement stmt=conn.createStatement()) { stmt.executeUpdate("UPDATE journeys SET available_seats="+count); }
    }
    @Test void cancellationPromotesOldestWaitingTicketAtomically() throws Exception {
        capacity(1);
        Ticket first=ticket("ignored"),second=ticket("ignored"),third=ticket("ignored");
        TicketDAO dao=new TicketDAO();
        assertEquals(BookingResult.CONFIRMED,dao.reserveTicket(first));
        assertEquals(BookingResult.WAITLISTED,dao.reserveTicket(second,UUID.randomUUID().toString(),true));
        assertEquals(BookingResult.WAITLISTED,dao.reserveTicket(third,UUID.randomUUID().toString(),true));
        assertEquals(2,dao.getTicketById(third.getTicketId()).getWaitingPosition());
        assertEquals(TicketDAO.CancellationResult.CANCELLED,dao.cancelTicket(first.getTicketId(),user));
        assertEquals("CONFIRMED",dao.getTicketById(second.getTicketId()).getStatus());
        assertEquals(first.getSeatNumber(),dao.getTicketById(second.getTicketId()).getSeatNumber());
        assertEquals("WAITLISTED",dao.getTicketById(third.getTicketId()).getStatus());
        assertEquals(1,dao.getTicketById(third.getTicketId()).getWaitingPosition());
        assertEquals(0,scalar("SELECT available_seats FROM journeys"));
        assertEquals(1,scalar("SELECT COUNT(*) FROM booking_events WHERE action='PROMOTED'"));
        assertEquals(TicketDAO.CancellationResult.ALREADY_CANCELLED,dao.cancelTicket(first.getTicketId(),user));
        assertEquals(1,scalar("SELECT COUNT(*) FROM booking_events WHERE action='PROMOTED'"));
    }
    @Test void cancellingWaitingTicketDoesNotReleaseCapacity() throws Exception {
        capacity(0); Ticket queued=ticket("ignored"); TicketDAO dao=new TicketDAO();
        assertEquals(BookingResult.WAITLISTED,dao.reserveTicket(queued,UUID.randomUUID().toString(),true));
        assertEquals(TicketDAO.CancellationResult.NOT_FOUND,dao.cancelTicket(queued.getTicketId(),999));
        assertEquals("WAITLISTED",dao.getTicketById(queued.getTicketId()).getStatus());
        assertEquals(TicketDAO.CancellationResult.CANCELLED,dao.cancelTicket(queued.getTicketId(),user));
        assertEquals(0,scalar("SELECT available_seats FROM journeys"));
    }
    @Test void concurrentCancellationsReleaseCapacityOnlyOnce() throws Exception {
        capacity(1); Ticket first=ticket("ignored"); TicketDAO dao=new TicketDAO(); dao.reserveTicket(first);
        ExecutorService executor=Executors.newFixedThreadPool(6);
        try {
            List<Future<TicketDAO.CancellationResult>> futures=new ArrayList<>();
            for(int i=0;i<6;i++) { futures.add(executor.submit(()->new TicketDAO().cancelTicket(first.getTicketId(),user))); }
            List<TicketDAO.CancellationResult> results=new ArrayList<>();
            for(Future<TicketDAO.CancellationResult> future:futures) { results.add(future.get(20,TimeUnit.SECONDS)); }
            assertEquals(1,Collections.frequency(results,TicketDAO.CancellationResult.CANCELLED));
            assertEquals(5,Collections.frequency(results,TicketDAO.CancellationResult.ALREADY_CANCELLED));
            assertEquals(1,scalar("SELECT available_seats FROM journeys"));
            assertEquals(1,scalar("SELECT COUNT(*) FROM booking_events WHERE action='CANCELLED'"));
        } finally { executor.shutdownNow(); }
    }
    @Test void cancelledBookingRequestNeverBooksAgain() throws Exception {
        Ticket first=ticket("ignored"); TicketDAO dao=new TicketDAO(); String key=UUID.randomUUID().toString();
        assertEquals(BookingResult.CONFIRMED,dao.reserveTicket(first,key));
        assertEquals(TicketDAO.CancellationResult.CANCELLED,dao.cancelTicket(first.getTicketId(),user));
        Ticket retry=ticket("ignored");
        assertEquals(BookingResult.CANCELLED,dao.reserveTicket(retry,key));
        assertEquals(first.getTicketId(),retry.getTicketId());
        assertEquals(1,scalar("SELECT COUNT(*) FROM tickets"));
        assertEquals(10,scalar("SELECT available_seats FROM journeys"));
    }
    @Test void faresAreSnapshottedAndArchivedTrainsCannotReceiveBookings() throws Exception {
        Ticket first=ticket("ignored"); TicketDAO dao=new TicketDAO(); dao.reserveTicket(first);
        try(Connection conn=connection();Statement stmt=conn.createStatement()) { stmt.executeUpdate("UPDATE journeys SET fare=999"); }
        assertEquals(100.0,dao.getTicketById(first.getTicketId()).getJourney().getFare());
        assertTrue(new TrainDAO().deleteTrain(1));
        assertEquals(BookingResult.UNAVAILABLE,dao.reserveTicket(ticket("ignored")));
        assertNotNull(dao.getTicketById(first.getTicketId()));
    }
    @Test void departedJourneysRejectWaitingAndCancellation() throws Exception {
        Ticket first=ticket("ignored"); TicketDAO dao=new TicketDAO(); dao.reserveTicket(first);
        try(Connection conn=connection();Statement stmt=conn.createStatement()) { stmt.executeUpdate("UPDATE journeys SET departure_time=NOW()-INTERVAL '1 hour'"); }
        assertEquals(TicketDAO.CancellationResult.DEPARTED,dao.cancelTicket(first.getTicketId(),user));
        assertEquals(BookingResult.UNAVAILABLE,dao.reserveTicket(ticket("ignored"),UUID.randomUUID().toString(),true));
        assertEquals(1,scalar("SELECT COUNT(*) FROM tickets"));
    }

    @AfterAll static void cleanup() throws Exception {
        if (oldConfig == null) { System.clearProperty("trackease.config"); }
        else { System.setProperty("trackease.config", oldConfig); }
        if (config != null) { Files.deleteIfExists(config); }
        if (schema != null) {
            try (Connection conn = connection(); Statement stmt = conn.createStatement()) { stmt.execute("DROP SCHEMA " + schema + " CASCADE"); }
        }
    }
}
