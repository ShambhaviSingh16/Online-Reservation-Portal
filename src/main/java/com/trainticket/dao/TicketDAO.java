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
    	public boolean bookTicket(Ticket ticket) {
    	    String sql = "INSERT INTO tickets (journey_id, user_id, seat_number, coach_number, booking_date, status) " +
    	                "VALUES (?, ?, ?, ?, NOW(), 'CONFIRMED')";
    	    
    	    try (Connection conn = DBUtil.getConnection();
    	            // Add RETURN_GENERATED_KEYS here
    	            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
    	        
    	        stmt.setInt(1, ticket.getJourneyId());
    	        stmt.setInt(2, ticket.getUserId());
    	        stmt.setString(3, ticket.getSeatNumber());
    	        stmt.setString(4, ticket.getCoachNumber());
    	        
    	        int affectedRows = stmt.executeUpdate();
    	        
    	        if (affectedRows > 0) {
    	            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
    	                if (generatedKeys.next()) {
    	                    ticket.setTicketId(generatedKeys.getInt(1));
    	                    return true;
    	                }
    	            }
    	        }
    	        return false;
    	    } catch (SQLException e) {
    	        System.err.println("Error booking ticket: " + e.getMessage());
    	        e.printStackTrace();
    	        return false;
    	    }
    	}
    	// For getTicketsByUser()
    	public List<Ticket> getTicketsByUser(int userId) {
    	    List<Ticket> tickets = new ArrayList<>();
    	    String sql = "SELECT t.*, j.departure_time, j.arrival_time, j.fare, " +
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
    	                "WHERE t.user_id = ?";
    	    
    	    try (Connection conn = DBUtil.getConnection();
    	         PreparedStatement stmt = conn.prepareStatement(sql)) {
    	        
    	        stmt.setInt(1, userId);
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
        String sql = "SELECT t.*, j.departure_time, j.arrival_time, j.fare, " +
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
        String sql = "SELECT t.*, j.departure_time, j.arrival_time, j.fare, " +
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
    public boolean updateAvailableSeats(int journeyId, int seatsBooked) {
        String sql = "UPDATE journeys SET available_seats = available_seats - ? " +
                     "WHERE journey_id = ? AND available_seats >= ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, seatsBooked);
            stmt.setInt(2, journeyId);
            stmt.setInt(3, seatsBooked);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean isSeatAvailable(int journeyId, String seatNumber, String coachNumber) {
        String sql = "SELECT 1 FROM tickets WHERE journey_id = ? AND seat_number = ? AND coach_number = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, journeyId);
            stmt.setString(2, seatNumber);
            stmt.setString(3, coachNumber);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return !rs.next(); // Returns true if seat is available
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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
        
        ticket.setJourney(journey);
        
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username")); // Now available from SQL
        user.setFullName(rs.getString("full_name"));
        ticket.setUser(user);
        
        return ticket;
    }
}