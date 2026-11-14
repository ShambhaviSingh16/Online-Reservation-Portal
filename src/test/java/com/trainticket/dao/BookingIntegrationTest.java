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
            stmt.execute(Files.readString(Path.of("database/schema.sql")));
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

    private AdvancedReservationDAO advanced(int seats,int rac) throws Exception {
        java.time.LocalDateTime departure,arrival;
        try(Connection c=connection();Statement s=c.createStatement()) {
            s.execute("INSERT INTO stations(station_code,station_name) VALUES('C','Middle')");
            try(ResultSet r=s.executeQuery("SELECT departure_time,arrival_time FROM journeys")){r.next();departure=r.getTimestamp(1).toLocalDateTime();arrival=r.getTimestamp(2).toLocalDateTime();}
        }
        var middle=departure.plusHours(12);var dao=new AdvancedReservationDAO();
        dao.configure(journey,List.of(new AdvancedReservationDAO.Stop(1,departure,departure,java.math.BigDecimal.ZERO),
            new AdvancedReservationDAO.Stop(3,middle,middle,java.math.BigDecimal.valueOf(40)),new AdvancedReservationDAO.Stop(2,arrival,arrival,java.math.BigDecimal.valueOf(100))),
            List.of(new AdvancedReservationDAO.Coach("S1",rac>0?"SLEEPER":"STANDARD",seats,java.math.BigDecimal.ONE,rac)));
        return dao;
    }
    private AdvancedReservationDAO.Booking advancedBooking(int from,int to,String cls,int count,boolean rac,boolean waiting,UUID hold) {
        return new AdvancedReservationDAO.Booking(journey,from,to,cls,java.util.stream.IntStream.range(0,count).mapToObj(i->"Passenger "+i).toList(),"LOWER",rac,waiting,UUID.randomUUID(),hold);
    }
    @Test void advancedSegmentsReuseSeatsOnlyOnNonoverlappingLegs() throws Exception {
        var dao=advanced(1,0);
        int first=dao.book(user,advancedBooking(0,1,"STANDARD",1,false,false,null)).get(0);
        int second=dao.book(user,advancedBooking(1,2,"STANDARD",1,false,false,null)).get(0);
        Ticket a=new TicketDAO().getTicketById(first),b=new TicketDAO().getTicketById(second);
        assertEquals(a.getSeatNumber(),b.getSeatNumber());assertEquals(40,a.getJourney().getFare());assertEquals(60,b.getJourney().getFare());
        assertEquals("Middle",a.getJourney().getArrivalStation().getStationName());assertEquals("Middle",b.getJourney().getDepartureStation().getStationName());
        assertThrows(AdvancedReservationDAO.Conflict.class,()->dao.book(user,advancedBooking(0,2,"STANDARD",1,false,false,null)));
        assertEquals(2,scalar("SELECT COUNT(*) FROM tickets"));assertEquals(4,scalar("SELECT COUNT(*) FROM seat_occupancy"));
        assertEquals(BookingResult.UNAVAILABLE,new TicketDAO().reserveTicket(ticket("ignored")));
        assertEquals(TicketDAO.GroupResult.UNAVAILABLE,new TicketDAO().reserveGroup(user,journey,UUID.randomUUID().toString(),List.of("Legacy"),true).result());
    }
    @Test void seatHoldsExpireAndCannotBeConsumedByAnotherUser() throws Exception {
        var dao=advanced(1,0);var hold=dao.hold(user,journey,0,2,"STANDARD",List.of("S1:1"));UUID id=(UUID)hold.get("holdId");
        try(Connection c=connection();Statement s=c.createStatement()){s.execute("INSERT INTO users(username,password,email,full_name) VALUES('other','other','other@local','Other')");}
        assertThrows(AdvancedReservationDAO.Conflict.class,()->dao.book(2,advancedBooking(0,2,"STANDARD",1,false,false,id)));
        assertThrows(AdvancedReservationDAO.Conflict.class,()->dao.book(2,advancedBooking(0,2,"STANDARD",1,false,false,null)));
        assertFalse(dao.release(2,id));
        try(Connection c=connection();Statement s=c.createStatement()){s.execute("UPDATE seat_holds SET expires_at=NOW()-INTERVAL '1 second'");}
        int ticket=dao.book(2,advancedBooking(0,2,"STANDARD",1,false,false,null)).get(0);assertTrue(ticket>0);
        assertThrows(AdvancedReservationDAO.Conflict.class,()->dao.book(user,advancedBooking(0,2,"STANDARD",1,false,false,id)));
        assertEquals(1,scalar("SELECT COUNT(*) FROM tickets"));
    }
    @Test void heldGroupIsAtomicAndExactSeatsSurviveRetries() throws Exception {
        var dao=advanced(2,0);UUID id=(UUID)dao.hold(user,journey,0,2,"STANDARD",List.of("S1:1","S1:2")).get("holdId");
        assertThrows(AdvancedReservationDAO.Conflict.class,()->dao.book(user,advancedBooking(0,2,"STANDARD",1,false,false,id)));
        assertEquals(0,scalar("SELECT COUNT(*) FROM tickets"));assertEquals(8,scalar("SELECT COUNT(*) FROM seat_occupancy"));
        var request=advancedBooking(0,2,"STANDARD",2,false,false,id);var ids=dao.book(user,request);assertEquals(ids,dao.book(user,request));
        assertEquals(2,scalar("SELECT COUNT(*) FROM tickets"));assertEquals(2,scalar("SELECT COUNT(*) FROM notification_outbox"));
        assertEquals(List.of("1","2"),ids.stream().map(i->new TicketDAO().getTicketById(i).getSeatNumber()).toList());
    }
    @Test void racSharesOnlyReservedBerthsAndPromotesBeforeWaiting() throws Exception {
        var dao=advanced(8,1);
        List<Integer> full=new ArrayList<>(dao.book(user,advancedBooking(0,2,"SLEEPER",6,false,false,null)));
        full.addAll(dao.book(user,advancedBooking(0,2,"SLEEPER",1,false,false,null)));
        int first=dao.book(user,advancedBooking(0,2,"SLEEPER",1,true,false,null)).get(0);
        int second=dao.book(user,advancedBooking(0,2,"SLEEPER",1,true,false,null)).get(0);
        assertEquals("RAC",new TicketDAO().getTicketById(first).getStatus());assertEquals("7",new TicketDAO().getTicketById(first).getSeatNumber());
        assertEquals("7",new TicketDAO().getTicketById(second).getSeatNumber());
        int waiting=dao.book(user,advancedBooking(0,2,"SLEEPER",1,true,true,null)).get(0);
        assertEquals("WAITLISTED",new TicketDAO().getTicketById(waiting).getStatus());
        assertEquals(TicketDAO.CancellationResult.CANCELLED,new TicketDAO().cancelTicket(full.get(0),user));
        assertEquals("CONFIRMED",new TicketDAO().getTicketById(first).getStatus());assertEquals("RAC",new TicketDAO().getTicketById(waiting).getStatus());
        assertEquals(0,scalar("SELECT COUNT(*) FROM (SELECT journey_id,coach_number,seat_number,leg,slot,COUNT(*) FROM seat_occupancy GROUP BY 1,2,3,4,5 HAVING COUNT(*)>1) x"));
    }
    @Test void advancedGroupFailureRollsBackTicketsOccupancyAndNotifications() throws Exception {
        var dao=advanced(1,0);assertThrows(AdvancedReservationDAO.Conflict.class,()->dao.book(user,advancedBooking(0,2,"STANDARD",2,false,false,null)));
        assertEquals(0,scalar("SELECT COUNT(*) FROM tickets"));assertEquals(0,scalar("SELECT COUNT(*) FROM seat_occupancy"));assertEquals(0,scalar("SELECT COUNT(*) FROM notification_outbox"));
    }
    @Test void classesAndPreferencesUseTheirOwnInventoryAndFares() throws Exception {
        var dao=advanced(1,0);
        // A plan may be replaced before any reservations or holds exist.
        java.time.LocalDateTime departure,arrival;
        try(Connection c=connection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT departure_time,arrival_time FROM journeys")){r.next();departure=r.getTimestamp(1).toLocalDateTime();arrival=r.getTimestamp(2).toLocalDateTime();}
        dao.configure(journey,List.of(new AdvancedReservationDAO.Stop(1,departure,departure,java.math.BigDecimal.ZERO),new AdvancedReservationDAO.Stop(2,arrival,arrival,java.math.BigDecimal.valueOf(100))),
            List.of(new AdvancedReservationDAO.Coach("S1","SLEEPER",8,java.math.BigDecimal.ONE,0),new AdvancedReservationDAO.Coach("A1","AC2",2,java.math.BigDecimal.valueOf(2),0)));
        int ticket=dao.book(user,advancedBooking(0,1,"AC2",1,false,false,null)).get(0);
        assertEquals("A1",new TicketDAO().getTicketById(ticket).getCoachNumber());assertEquals(200,new TicketDAO().getTicketById(ticket).getJourney().getFare());
        assertThrows(AdvancedReservationDAO.Conflict.class,()->dao.configure(journey,List.of(new AdvancedReservationDAO.Stop(1,departure,departure,java.math.BigDecimal.ZERO),new AdvancedReservationDAO.Stop(2,arrival,arrival,java.math.BigDecimal.valueOf(100))),List.of(new AdvancedReservationDAO.Coach("Z1","STANDARD",1,java.math.BigDecimal.ONE,0))));
    }
    @Test void recoveryCodesAreSingleUseAndPasswordResetRevokesOldCredentials() throws Exception {
        var dao=new RecoveryDAO();assertTrue(dao.generate(user,"wrong").isEmpty());var old=dao.generate(user,"test");assertEquals(8,old.size());
        var current=dao.generate(user,"test");assertFalse(dao.reset("test",old.get(0),"NewPassword123!"));
        assertTrue(dao.reset("test",current.get(0),"NewPassword123!"));assertFalse(dao.reset("test",current.get(0),"AnotherPassword123!"));
        assertNull(new UserDAO().authenticate("test","test"));assertNotNull(new UserDAO().authenticate("test","NewPassword123!"));assertEquals(1,dao.version(user));
    }
    @Test void emailResetTokensExpireAndCannotBeReused() throws Exception {
        var dao=new PasswordResetDAO();assertNull(dao.request("nonexistent"));var request=dao.request("test");assertNotNull(request);assertNull(dao.request("test"));
        assertTrue(dao.complete(request.token(),"EmailPassword123!"));assertFalse(dao.complete(request.token(),"ReusePassword123!"));assertNotNull(new UserDAO().authenticate("test","EmailPassword123!"));
        var expired=dao.request("test");try(Connection c=connection();Statement s=c.createStatement()){s.execute("UPDATE password_reset_tokens SET expires_at=NOW()-INTERVAL '1 second'");}
        assertFalse(dao.complete(expired.token(),"ExpiredPassword123!"));
    }

    @Test void archivedIntermediateStationDoesNotPreventIndividualCancellation() throws Exception {
        var dao=advanced(1,0);int ticket=dao.book(user,advancedBooking(0,1,"STANDARD",1,false,false,null)).get(0);
        dao.book(user,advancedBooking(0,1,"STANDARD",1,false,true,null));
        try(Connection c=connection();Statement s=c.createStatement()){s.execute("UPDATE stations SET is_active=FALSE WHERE station_id=3");}
        assertEquals(TicketDAO.CancellationResult.CANCELLED,new TicketDAO().cancelTicket(ticket,user));
    }
    @Test void outboxDeliveryUsesCommittedEventsAndRetriesWithoutChangingTickets() throws Exception {
        Ticket ticket=ticket("ignored");new TicketDAO().reserveTicket(ticket);
        var factory=new org.springframework.beans.factory.support.StaticListableBeanFactory();
        List<org.springframework.mail.SimpleMailMessage> sent=new ArrayList<>();
        org.springframework.mail.javamail.JavaMailSenderImpl fake=new org.springframework.mail.javamail.JavaMailSenderImpl(){
            @Override public void send(org.springframework.mail.SimpleMailMessage... messages){sent.addAll(List.of(messages));}
        };
        factory.addBean("mail",fake);
        var mail=new com.trainticket.service.EmailDeliveryService(factory.getBeanProvider(org.springframework.mail.javamail.JavaMailSender.class),"sender@example.test","http://localhost:8080","local-test","smtp","",new com.fasterxml.jackson.databind.ObjectMapper());
        mail.deliverPending();assertEquals(1,sent.size());assertTrue(sent.get(0).getText().contains("CONFIRMED"));
        assertEquals(1,scalar("SELECT COUNT(*) FROM notification_outbox WHERE state='SENT'"));
        mail.deliverPending();assertEquals(1,sent.size());
        new TicketDAO().cancelTicket(ticket.getTicketId(),user);mail.deliverPending();assertEquals(2,sent.size());assertTrue(sent.get(1).getText().contains("CANCELLED"));
    }
    @Test void concurrentAdvancedBookingsCannotClaimOneSeatTwice() throws Exception {
        var dao=advanced(1,0);ExecutorService pool=Executors.newFixedThreadPool(3);
        try {
            List<Callable<Boolean>> calls=new ArrayList<>();for(int i=0;i<3;i++)calls.add(()->{try{dao.book(user,advancedBooking(0,2,"STANDARD",1,false,false,null));return true;}catch(AdvancedReservationDAO.Conflict e){return false;}});
            int successes=0;for(var f:pool.invokeAll(calls))if(f.get(20,TimeUnit.SECONDS))successes++;
            assertEquals(1,successes);assertEquals(1,scalar("SELECT COUNT(*) FROM tickets"));assertEquals(4,scalar("SELECT COUNT(*) FROM seat_occupancy"));
        }finally{pool.shutdownNow();}
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
