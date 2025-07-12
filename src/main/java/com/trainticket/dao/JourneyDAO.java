package com.trainticket.dao;
import com.trainticket.model.Train;
import com.trainticket.model.Station;
import com.trainticket.model.Journey;
import com.trainticket.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JourneyDAO {
    public boolean addJourney(Journey journey) throws SQLException {
        // First check if journey already exists
        if (isJourneyExists(journey)) {
            return false;
        }
        
        String sql = "INSERT INTO journeys (train_id, departure_station_id, arrival_station_id, " +
                     "departure_time, arrival_time, fare, available_seats) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, journey.getTrainId());
            stmt.setInt(2, journey.getDepartureStationId());
            stmt.setInt(3, journey.getArrivalStationId());
            stmt.setTimestamp(4, journey.getDepartureTime());
            stmt.setTimestamp(5, journey.getArrivalTime());
            stmt.setDouble(6, journey.getFare());
            stmt.setInt(7, journey.getAvailableSeats());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    private boolean isJourneyExists(Journey journey) throws SQLException {
        String sql = "SELECT 1 FROM journeys WHERE train_id = ? AND departure_station_id = ? " +
                     "AND arrival_station_id = ? AND departure_time = ? LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, journey.getTrainId());
            stmt.setInt(2, journey.getDepartureStationId());
            stmt.setInt(3, journey.getArrivalStationId());
            stmt.setTimestamp(4, journey.getDepartureTime());
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    public boolean updateJourney(Journey journey) throws SQLException {
        String sql = "UPDATE journeys SET train_id=?, departure_station_id=?, arrival_station_id=?, " +
                     "departure_time=?, arrival_time=?, fare=?, available_seats=? WHERE journey_id=?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, journey.getTrainId());
            stmt.setInt(2, journey.getDepartureStationId());
            stmt.setInt(3, journey.getArrivalStationId());
            stmt.setTimestamp(4, journey.getDepartureTime());
            stmt.setTimestamp(5, journey.getArrivalTime());
            stmt.setDouble(6, journey.getFare());
            stmt.setInt(7, journey.getAvailableSeats());
            stmt.setInt(8, journey.getJourneyId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    public boolean deleteJourney(int journeyId) throws SQLException {
        String sql = "DELETE FROM journeys WHERE journey_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, journeyId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    public List<Journey> getAllJourneys() {
        List<Journey> journeys = new ArrayList<>();
        String sql = "SELECT j.*, t.train_number, t.train_name, " +
                "ds.station_code as dep_code, ds.station_name as dep_name, " +
                "ar.station_code as arr_code, ar.station_name as arr_name " +
                "FROM journeys j " +
                "JOIN trains t ON j.train_id = t.train_id " +
                "JOIN stations ds ON j.departure_station_id = ds.station_id " +
                "JOIN stations ar ON j.arrival_station_id = ar.station_id";
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Journey journey = new Journey();
                journey.setJourneyId(rs.getInt("journey_id"));
                journey.setTrainId(rs.getInt("train_id"));
                journey.setDepartureStationId(rs.getInt("departure_station_id"));
                journey.setArrivalStationId(rs.getInt("arrival_station_id"));
                journey.setDepartureTime(rs.getTimestamp("departure_time"));
                journey.setArrivalTime(rs.getTimestamp("arrival_time"));
                journey.setFare(rs.getDouble("fare"));
                journey.setAvailableSeats(rs.getInt("available_seats"));
                
                Train train = new Train();
                train.setTrainId(rs.getInt("train_id"));
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
                
                journeys.add(journey);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return journeys;
    }
    
    public List<Journey> searchJourneys(int departureStationId, int arrivalStationId, Date travelDate) {
        List<Journey> journeys = new ArrayList<>();
        String sql = "SELECT j.*, t.train_number, t.train_name, " +
                "ds.station_code as dep_code, ds.station_name as dep_name, " +
                "ar.station_code as arr_code, ar.station_name as arr_name " +
                "FROM journeys j " +
                "JOIN trains t ON j.train_id = t.train_id " +
                "JOIN stations ds ON j.departure_station_id = ds.station_id " +
                "JOIN stations ar ON j.arrival_station_id = ar.station_id " +
                "WHERE j.departure_station_id = ? AND j.arrival_station_id = ? " +
                "AND DATE(j.departure_time) = ? AND j.available_seats > 0";
       
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, departureStationId);
            stmt.setInt(2, arrivalStationId);
            stmt.setDate(3, travelDate);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Journey journey = new Journey();
                    journey.setJourneyId(rs.getInt("journey_id"));
                    journey.setTrainId(rs.getInt("train_id"));
                    journey.setDepartureStationId(rs.getInt("departure_station_id"));
                    journey.setArrivalStationId(rs.getInt("arrival_station_id"));
                    journey.setDepartureTime(rs.getTimestamp("departure_time"));
                    journey.setArrivalTime(rs.getTimestamp("arrival_time"));
                    journey.setFare(rs.getDouble("fare"));
                    journey.setAvailableSeats(rs.getInt("available_seats"));
                    
                    Train train = new Train();
                    train.setTrainId(rs.getInt("train_id"));
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
                    
                    journeys.add(journey);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return journeys;
    }
    
    public Journey getJourneyById(int journeyId) {
        Journey journey = null;
        String sql = "SELECT j.*, t.train_number, t.train_name, " +
                "ds.station_code as dep_code, ds.station_name as dep_name, " +
                "ar.station_code as arr_code, ar.station_name as arr_name " +
                "FROM journeys j " +
                "JOIN trains t ON j.train_id = t.train_id " +
                "JOIN stations ds ON j.departure_station_id = ds.station_id " +
                "JOIN stations ar ON j.arrival_station_id = ar.station_id " +
                "WHERE j.journey_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, journeyId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    journey = new Journey();
                    journey.setJourneyId(rs.getInt("journey_id"));
                    journey.setTrainId(rs.getInt("train_id"));
                    journey.setDepartureStationId(rs.getInt("departure_station_id"));
                    journey.setArrivalStationId(rs.getInt("arrival_station_id"));
                    journey.setDepartureTime(rs.getTimestamp("departure_time"));
                    journey.setArrivalTime(rs.getTimestamp("arrival_time"));
                    journey.setFare(rs.getDouble("fare"));
                    journey.setAvailableSeats(rs.getInt("available_seats"));
                    
                    Train train = new Train();
                    train.setTrainId(rs.getInt("train_id"));
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
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return journey;
    }
    
    public boolean updateAvailableSeats(int journeyId, int seatsBooked) {
        String sql = "UPDATE journeys SET available_seats = available_seats - ? WHERE journey_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, seatsBooked);
            stmt.setInt(2, journeyId); // Only 2 parameters
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}