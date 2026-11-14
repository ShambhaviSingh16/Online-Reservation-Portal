package com.trainticket.dao;

import com.trainticket.model.Station;
import com.trainticket.model.Train;
import com.trainticket.model.Journey;
import com.trainticket.model.User;
import com.trainticket.model.Ticket;
import com.trainticket.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO {
    public enum BookingResult { CONFIRMED, WAITLISTED, CANCELLED, UNAVAILABLE, INVALID, FAILED }
    public enum CancellationResult { CANCELLED, ALREADY_CANCELLED, NOT_FOUND, DEPARTED, FAILED }
    public enum GroupResult { BOOKED, UNAVAILABLE, INVALID, FAILED }
    public record GroupBooking(GroupResult result, List<Ticket> tickets) { }

    /** One transaction and journey lock cover the entire group, including retries. */
    public GroupBooking reserveGroup(int user, int journey, String requestId,
            List<String> passengerNames, boolean joinWaitingList) {
        java.util.UUID key;
        List<String> names;
        try {
            key=java.util.UUID.fromString(requestId);
            if (user<=0 || journey<=0 || passengerNames==null || passengerNames.isEmpty() || passengerNames.size()>6)
                return new GroupBooking(GroupResult.INVALID,List.of());
            names=passengerNames.stream().map(String::strip).toList();
            if (names.stream().anyMatch(n -> n.isEmpty() || n.length()>100 || n.codePoints().anyMatch(Character::isISOControl)))
                return new GroupBooking(GroupResult.INVALID,List.of());
        } catch (IllegalArgumentException | NullPointerException e) {
            return new GroupBooking(GroupResult.INVALID,List.of());
        }
        try (Connection conn=DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Serialize a request key even if a conflicting retry names another journey.
                try (PreparedStatement stmt=conn.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                    stmt.setLong(1, key.getMostSignificantBits() ^ key.getLeastSignificantBits() ^ user);
                    stmt.execute();
                }
                boolean departed,active;
                java.math.BigDecimal fare;
                try (PreparedStatement stmt=conn.prepareStatement(
                        "SELECT j.advanced_reservations,j.departure_time<=CURRENT_TIMESTAMP AS departed,j.fare,(tr.is_active AND ds.is_active AND ar.is_active) AS active "
                        + "FROM journeys j JOIN trains tr ON tr.train_id=j.train_id JOIN stations ds ON ds.station_id=j.departure_station_id "
                        + "JOIN stations ar ON ar.station_id=j.arrival_station_id WHERE j.journey_id=? FOR UPDATE OF j")) {
                    stmt.setInt(1,journey);
                    try (ResultSet rs=stmt.executeQuery()) {
                        if (!rs.next()) { conn.rollback(); return new GroupBooking(GroupResult.UNAVAILABLE,List.of()); }
                        if(rs.getBoolean("advanced_reservations")){conn.rollback();return new GroupBooking(GroupResult.UNAVAILABLE,List.of());}
                        departed=rs.getBoolean("departed");active=rs.getBoolean("active");fare=rs.getBigDecimal("fare");
                    }
                }
                List<Ticket> booked=new ArrayList<>();
                try (PreparedStatement stmt=conn.prepareStatement(
                        "SELECT ticket_id,journey_id,seat_number,coach_number,status,passenger_name FROM tickets "
                        + "WHERE user_id=? AND booking_group_id=? ORDER BY ticket_id")) {
                    stmt.setInt(1,user);stmt.setObject(2,key);
                    try (ResultSet rs=stmt.executeQuery()) {
                        while(rs.next()) {
                            int index=booked.size();
                            if(rs.getInt("journey_id")!=journey || index>=names.size() || !names.get(index).equals(rs.getString("passenger_name"))) {
                                conn.rollback();return new GroupBooking(GroupResult.INVALID,List.of());
                            }
                            Ticket t=new Ticket();t.setTicketId(rs.getInt("ticket_id"));t.setJourneyId(journey);t.setUserId(user);
                            t.setStatus(rs.getString("status"));t.setSeatNumber(rs.getString("seat_number"));t.setCoachNumber(rs.getString("coach_number"));
                            t.setPassengerName(rs.getString("passenger_name"));t.setBookingGroupId(key.toString());booked.add(t);
                        }
                    }
                }
                if(!booked.isEmpty()) {
                    conn.rollback();return new GroupBooking(booked.size()==names.size()?GroupResult.BOOKED:GroupResult.INVALID,
                            booked.size()==names.size()?booked:List.of());
                }
                // A key belonging to the older single-ticket endpoint cannot be reused as a group.
                try (PreparedStatement stmt=conn.prepareStatement("SELECT 1 FROM tickets WHERE user_id=? AND booking_request_id=?")) {
                    stmt.setInt(1,user);stmt.setObject(2,key);
                    try(ResultSet rs=stmt.executeQuery()) { if(rs.next()){conn.rollback();return new GroupBooking(GroupResult.INVALID,List.of());} }
                }
                if(departed || !active){conn.rollback();return new GroupBooking(GroupResult.UNAVAILABLE,List.of());}
                ensureInventory(conn,journey);promoteWaiting(conn,journey);
                for(int i=0;i<names.size();i++) {
                    String[] seat=availableSeat(conn,journey);
                    boolean reserved=seat!=null && decrementCapacity(conn,journey);
                    if(!reserved && !joinWaitingList){conn.rollback();return new GroupBooking(GroupResult.UNAVAILABLE,List.of());}
                    Ticket t=new Ticket();t.setJourneyId(journey);t.setUserId(user);t.setPassengerName(names.get(i));
                    t.setBookingGroupId(key.toString());t.setStatus(reserved?"CONFIRMED":"WAITLISTED");
                    t.setSeatNumber(reserved?seat[0]:null);t.setCoachNumber(reserved?seat[1]:null);
                    java.util.UUID ticketKey=i==0?key:java.util.UUID.nameUUIDFromBytes((key+":"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    try(PreparedStatement stmt=conn.prepareStatement(
                            "INSERT INTO tickets(journey_id,user_id,seat_number,coach_number,booking_date,status,booking_request_id,booked_fare,booking_group_id,passenger_name) "
                            + "VALUES(?,?,?,?,NOW(),?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                        stmt.setInt(1,journey);stmt.setInt(2,user);stmt.setString(3,t.getSeatNumber());stmt.setString(4,t.getCoachNumber());
                        stmt.setString(5,t.getStatus());stmt.setObject(6,ticketKey);stmt.setBigDecimal(7,fare);
                        stmt.setObject(8,key);stmt.setString(9,t.getPassengerName());stmt.executeUpdate();
                        try(ResultSet rs=stmt.getGeneratedKeys()){if(!rs.next())throw new SQLException("Ticket ID missing");t.setTicketId(rs.getInt(1));}
                    }
                    event(conn,t.getTicketId(),user,t.getStatus());booked.add(t);
                }
                conn.commit();return new GroupBooking(GroupResult.BOOKED,booked);
            } catch(SQLException e){conn.rollback();throw e;}
        } catch(SQLException e){
            java.util.logging.Logger.getLogger(TicketDAO.class.getName()).log(java.util.logging.Level.SEVERE,"Group booking transaction failed",e);
            return new GroupBooking(GroupResult.FAILED,List.of());
        }
    }

    public List<Ticket> getBookingGroup(int user, String groupId) {
        try { java.util.UUID.fromString(groupId); }
        catch(IllegalArgumentException | NullPointerException e) { return List.of(); }
        return getTicketsByUser(user,groupId).stream()
                .sorted(java.util.Comparator.comparingInt(Ticket::getTicketId)).toList();
    }

    public BookingResult reserveTicket(Ticket ticket) {
        return reserveTicket(ticket, java.util.UUID.randomUUID().toString(), false);
    }
    public BookingResult reserveTicket(Ticket ticket, String requestId) {
        return reserveTicket(ticket, requestId, false);
    }
    public BookingResult reserveTicket(Ticket ticket, String requestId, boolean joinWaitingList) {
        if (ticket == null || ticket.getJourneyId() <= 0 || ticket.getUserId() <= 0) { return BookingResult.INVALID; }
        java.util.UUID key;
        try { key = java.util.UUID.fromString(requestId); }
        catch (IllegalArgumentException | NullPointerException e) { return BookingResult.INVALID; }
        ticket.setTicketId(0); ticket.setSeatNumber(null); ticket.setCoachNumber(null);
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean departed;
                boolean active;
                java.math.BigDecimal fare;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT j.advanced_reservations,j.departure_time<=CURRENT_TIMESTAMP AS departed,j.fare,(tr.is_active AND ds.is_active AND ar.is_active) AS active "
                        + "FROM journeys j JOIN trains tr ON tr.train_id=j.train_id JOIN stations ds ON ds.station_id=j.departure_station_id "
                        + "JOIN stations ar ON ar.station_id=j.arrival_station_id WHERE j.journey_id=? FOR UPDATE OF j")) {
                    stmt.setInt(1,ticket.getJourneyId());
                    try (ResultSet rs=stmt.executeQuery()) {
                        if (!rs.next()) { conn.rollback(); return BookingResult.UNAVAILABLE; }
                        if(rs.getBoolean("advanced_reservations")){conn.rollback();return BookingResult.UNAVAILABLE;}
                        departed=rs.getBoolean("departed"); active=rs.getBoolean("active"); fare=rs.getBigDecimal("fare");
                    }
                }
                try (PreparedStatement stmt=conn.prepareStatement(
                        "SELECT ticket_id,journey_id,seat_number,coach_number,status FROM tickets WHERE user_id=? AND booking_request_id=?")) {
                    stmt.setInt(1,ticket.getUserId()); stmt.setObject(2,key);
                    try (ResultSet rs=stmt.executeQuery()) {
                        if (rs.next()) {
                            if (rs.getInt("journey_id")!=ticket.getJourneyId()) { conn.rollback(); return BookingResult.INVALID; }
                            ticket.setTicketId(rs.getInt("ticket_id")); ticket.setSeatNumber(rs.getString("seat_number"));
                            ticket.setCoachNumber(rs.getString("coach_number")); ticket.setStatus(rs.getString("status"));
                            conn.rollback(); return BookingResult.valueOf(ticket.getStatus());
                        }
                    }
                }
                if (departed || !active) { conn.rollback(); return BookingResult.UNAVAILABLE; }
                ensureInventory(conn,ticket.getJourneyId());
                promoteWaiting(conn,ticket.getJourneyId());
                String[] seat=availableSeat(conn,ticket.getJourneyId());
                boolean reserved=seat!=null && decrementCapacity(conn,ticket.getJourneyId());
                if (!reserved && !joinWaitingList) { conn.rollback(); return BookingResult.UNAVAILABLE; }
                String status=reserved?"CONFIRMED":"WAITLISTED";
                int id;
                try (PreparedStatement stmt=conn.prepareStatement(
                        "INSERT INTO tickets(journey_id,user_id,seat_number,coach_number,booking_date,status,booking_request_id,booked_fare) "
                        + "VALUES(?,?,?,?,NOW(),?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1,ticket.getJourneyId()); stmt.setInt(2,ticket.getUserId());
                    stmt.setString(3,reserved?seat[0]:null); stmt.setString(4,reserved?seat[1]:null);
                    stmt.setString(5,status); stmt.setObject(6,key); stmt.setBigDecimal(7,fare);
                    stmt.executeUpdate();
                    try (ResultSet rs=stmt.getGeneratedKeys()) { if (!rs.next()) { throw new SQLException("Ticket ID missing"); } id=rs.getInt(1); }
                }
                event(conn,id,ticket.getUserId(),status);
                conn.commit();
                ticket.setTicketId(id); ticket.setStatus(status);
                if (reserved) { ticket.setSeatNumber(seat[0]); ticket.setCoachNumber(seat[1]); }
                return BookingResult.valueOf(status);
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TicketDAO.class.getName()).log(java.util.logging.Level.SEVERE,"Booking transaction failed",e);
            return BookingResult.FAILED;
        }
    }

    private void ensureInventory(Connection conn,int journey) throws SQLException {
        try (PreparedStatement stmt=conn.prepareStatement(
                "INSERT INTO journey_seats(journey_id,position,coach_number,seat_number) "
                + "SELECT j.journey_id,n,'S'||((n-1)/80+1),((n-1)%80+1)::text FROM journeys j JOIN trains t ON t.train_id=j.train_id "
                + "CROSS JOIN LATERAL generate_series(1,t.total_seats) n WHERE j.journey_id=? "
                + "AND NOT EXISTS(SELECT 1 FROM journey_seats s WHERE s.journey_id=j.journey_id)")) {
            stmt.setInt(1,journey); stmt.executeUpdate();
        }
    }
    private String[] availableSeat(Connection conn,int journey) throws SQLException {
        try (PreparedStatement stmt=conn.prepareStatement(
                "SELECT s.seat_number,s.coach_number FROM journey_seats s WHERE s.journey_id=? AND NOT EXISTS "
                + "(SELECT 1 FROM tickets t WHERE t.journey_id=s.journey_id AND UPPER(TRIM(t.seat_number))=s.seat_number "
                + "AND UPPER(TRIM(t.coach_number))=s.coach_number AND t.status='CONFIRMED') ORDER BY s.position LIMIT 1")) {
            stmt.setInt(1,journey);
            try (ResultSet rs=stmt.executeQuery()) { return rs.next()?new String[]{rs.getString(1),rs.getString(2)}:null; }
        }
    }
    private boolean decrementCapacity(Connection conn,int journey) throws SQLException {
        try (PreparedStatement stmt=conn.prepareStatement("UPDATE journeys SET available_seats=available_seats-1 WHERE journey_id=? AND available_seats>0")) {
            stmt.setInt(1,journey); return stmt.executeUpdate()==1;
        }
    }
    private void event(Connection conn,int ticket,int actor,String action) throws SQLException {
        try (PreparedStatement stmt=conn.prepareStatement("INSERT INTO booking_events(ticket_id,actor_id,action) VALUES(?,?,?)")) {
            stmt.setInt(1,ticket);
            if(actor>0) { stmt.setInt(2,actor); } else { stmt.setNull(2,Types.INTEGER); }
            stmt.setString(3,action); stmt.executeUpdate();
        }
    }
    private void promoteWaiting(Connection conn,int journey) throws SQLException {
        while (true) {
            int next,owner;
            try (PreparedStatement stmt=conn.prepareStatement("SELECT ticket_id,user_id FROM tickets WHERE journey_id=? AND status='WAITLISTED' ORDER BY ticket_id LIMIT 1 FOR UPDATE")) {
                stmt.setInt(1,journey);
                try (ResultSet rs=stmt.executeQuery()) { if (!rs.next()) { return; } next=rs.getInt(1); owner=rs.getInt(2); }
            }
            String[] seat=availableSeat(conn,journey);
            if (seat==null || !decrementCapacity(conn,journey)) { return; }
            try (PreparedStatement stmt=conn.prepareStatement("UPDATE tickets SET status='CONFIRMED',seat_number=?,coach_number=? WHERE ticket_id=?")) {
                stmt.setString(1,seat[0]); stmt.setString(2,seat[1]); stmt.setInt(3,next); stmt.executeUpdate();
            }
            event(conn,next,0,"PROMOTED");
        }
    }
    public CancellationResult cancelTicket(int id,int user) {
        try (Connection conn=DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int journey;
                try (PreparedStatement stmt=conn.prepareStatement("SELECT journey_id,advanced FROM tickets WHERE ticket_id=? AND user_id=?")) {
                    stmt.setInt(1,id); stmt.setInt(2,user);
                    try (ResultSet rs=stmt.executeQuery()) { if (!rs.next()) { conn.rollback(); return CancellationResult.NOT_FOUND; }
                        if(rs.getBoolean("advanced")){conn.rollback();return new AdvancedReservationDAO().cancel(user,id);}journey=rs.getInt(1); }
                }
                boolean departed;
                try (PreparedStatement stmt=conn.prepareStatement("SELECT departure_time<=CURRENT_TIMESTAMP FROM journeys WHERE journey_id=? FOR UPDATE")) {
                    stmt.setInt(1,journey);
                    try (ResultSet rs=stmt.executeQuery()) { if (!rs.next()) { conn.rollback(); return CancellationResult.NOT_FOUND; } departed=rs.getBoolean(1); }
                }
                String status;
                try (PreparedStatement stmt=conn.prepareStatement("SELECT status FROM tickets WHERE ticket_id=? AND user_id=? FOR UPDATE")) {
                    stmt.setInt(1,id); stmt.setInt(2,user);
                    try (ResultSet rs=stmt.executeQuery()) { if (!rs.next()) { conn.rollback(); return CancellationResult.NOT_FOUND; } status=rs.getString(1); }
                }
                if ("CANCELLED".equals(status)) { conn.rollback(); return CancellationResult.ALREADY_CANCELLED; }
                if (departed) { conn.rollback(); return CancellationResult.DEPARTED; }
                try (PreparedStatement stmt=conn.prepareStatement("UPDATE tickets SET status='CANCELLED',cancelled_at=NOW() WHERE ticket_id=?")) {
                    stmt.setInt(1,id); stmt.executeUpdate();
                }
                if ("CONFIRMED".equals(status)) {
                    try (PreparedStatement stmt=conn.prepareStatement("UPDATE journeys SET available_seats=available_seats+1 WHERE journey_id=?")) { stmt.setInt(1,journey); stmt.executeUpdate(); }
                }
                ensureInventory(conn,journey);
                promoteWaiting(conn,journey);
                event(conn,id,user,"CANCELLED");
                conn.commit(); return CancellationResult.CANCELLED;
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TicketDAO.class.getName()).log(java.util.logging.Level.SEVERE,"Cancellation transaction failed",e);
            return CancellationResult.FAILED;
        }
    }
        // For getTicketsByUser()
        public List<Ticket> getTicketsByUser(int userId) {
            return getTicketsByUser(userId,null);
        }
        private List<Ticket> getTicketsByUser(int userId,String groupId) {
            List<Ticket> tickets = new ArrayList<>();
            String sql = "SELECT t.*, (SELECT COUNT(*) FROM tickets w WHERE w.journey_id=t.journey_id AND w.status='WAITLISTED' AND w.ticket_id<=t.ticket_id) AS waiting_position, j.departure_time, j.arrival_time, COALESCE(t.booked_fare,j.fare) AS fare, " +
                        "tr.train_number, tr.train_name, " +
                        "ds.station_code AS dep_code, ds.station_name AS dep_name, " +
                        "ar.station_code AS arr_code, ar.station_name AS arr_name, " +
                        "u.username, u.full_name " + // Added user columns
                        "FROM tickets t " +
                        "JOIN journeys j ON t.journey_id = j.journey_id " +
                        "JOIN trains tr ON j.train_id = tr.train_id " +
                        "JOIN stations ds ON j.departure_station_id = ds.station_id " +
                        "JOIN stations ar ON j.arrival_station_id = ar.station_id " +
                        "JOIN users u ON t.user_id = u.user_id " + // Added users join
                        "WHERE t.user_id = ? AND (?::uuid IS NULL OR t.booking_group_id=?::uuid)";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setString(2,groupId);stmt.setString(3,groupId);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Ticket ticket = extractTicketFromResultSet(rs);
                    tickets.add(ticket);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return tickets;
        }

    public Ticket getTicketById(int ticketId) {
        Ticket ticket = null;
        String sql = "SELECT t.*, (SELECT COUNT(*) FROM tickets w WHERE w.journey_id=t.journey_id AND w.status='WAITLISTED' AND w.ticket_id<=t.ticket_id) AS waiting_position, j.departure_time, j.arrival_time, COALESCE(t.booked_fare,j.fare) AS fare, " +
                     "tr.train_number, tr.train_name, " +
                     "ds.station_code AS dep_code, ds.station_name AS dep_name, " +
                     "ar.station_code AS arr_code, ar.station_name AS arr_name, " +
                     "u.username, u.full_name, u.email " +
                     "FROM tickets t " +
                     "JOIN journeys j ON t.journey_id = j.journey_id " +
                     "JOIN trains tr ON j.train_id = tr.train_id " +
                     "JOIN stations ds ON j.departure_station_id = ds.station_id " +
                     "JOIN stations ar ON j.arrival_station_id = ar.station_id " +
                     "JOIN users u ON t.user_id = u.user_id " +
                     "WHERE t.ticket_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ticketId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ticket = extractTicketFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ticket;
    }

    // For getAllTickets()
    public List<Ticket> getAllTickets() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.*, (SELECT COUNT(*) FROM tickets w WHERE w.journey_id=t.journey_id AND w.status='WAITLISTED' AND w.ticket_id<=t.ticket_id) AS waiting_position, j.departure_time, j.arrival_time, COALESCE(t.booked_fare,j.fare) AS fare, " +
                "tr.train_number, tr.train_name, " +
                "ds.station_code AS dep_code, ds.station_name AS dep_name, " +
                "ar.station_code AS arr_code, ar.station_name AS arr_name, " + // Fixed alias
                "u.username, u.full_name " +
                "FROM tickets t " +
                "JOIN journeys j ON t.journey_id = j.journey_id " +
                "JOIN trains tr ON j.train_id = tr.train_id " +
                "JOIN stations ds ON j.departure_station_id = ds.station_id " +
                "JOIN stations ar ON j.arrival_station_id = ar.station_id " + // Fixed here
                "JOIN users u ON t.user_id = u.user_id";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Ticket ticket = extractTicketFromResultSet(rs);
                tickets.add(ticket);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tickets;
    }
    private Ticket extractTicketFromResultSet(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setTicketId(rs.getInt("ticket_id"));
        ticket.setJourneyId(rs.getInt("journey_id"));
        ticket.setUserId(rs.getInt("user_id"));
        ticket.setBookingDate(rs.getTimestamp("booking_date"));
        ticket.setSeatNumber(rs.getString("seat_number"));
        ticket.setCoachNumber(rs.getString("coach_number"));
        ticket.setStatus(rs.getString("status"));
        ticket.setAdvanced(rs.getBoolean("advanced"));
        ticket.setTravelClass(rs.getString("travel_class"));
        ticket.setBerthPreference(rs.getString("berth_preference"));
        ticket.setSegmentStart(rs.getInt("segment_start"));ticket.setSegmentEnd(rs.getInt("segment_end"));
        ticket.setPassengerName(rs.getString("passenger_name"));
        ticket.setBookingGroupId(rs.getString("booking_group_id"));
        ticket.setWaitingPosition("WAITLISTED".equals(ticket.getStatus())?rs.getInt("waiting_position"):0);

        Journey journey = new Journey();
        journey.setJourneyId(rs.getInt("journey_id"));
        journey.setDepartureTime(rs.getTimestamp("departure_time"));
        journey.setArrivalTime(rs.getTimestamp("arrival_time"));
        journey.setFare(rs.getDouble("fare"));

        Train train = new Train();
        train.setTrainNumber(rs.getString("train_number"));
        train.setTrainName(rs.getString("train_name"));
        journey.setTrain(train);

        Station depStation = new Station();
        depStation.setStationCode(rs.getString("dep_code"));
        depStation.setStationName(rs.getString("dep_name"));
        journey.setDepartureStation(depStation);

        Station arrStation = new Station();
        arrStation.setStationCode(rs.getString("arr_code"));
        arrStation.setStationName(rs.getString("arr_name"));
        journey.setArrivalStation(arrStation);
        if(ticket.isAdvanced()) {
            depStation.setStationName(rs.getString("segment_departure_name"));depStation.setStationCode(rs.getString("segment_departure_code"));
            arrStation.setStationName(rs.getString("segment_arrival_name"));arrStation.setStationCode(rs.getString("segment_arrival_code"));
            journey.setDepartureTime(rs.getTimestamp("segment_departure_time"));journey.setArrivalTime(rs.getTimestamp("segment_arrival_time"));
        }

        ticket.setJourney(journey);

        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username")); // Now available from SQL
        user.setFullName(rs.getString("full_name"));
        ticket.setUser(user);

        return ticket;
    }
}
