package com.trainticket.dao;

import com.trainticket.model.Train;
import com.trainticket.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrainDAO {
	public boolean addTrain(Train train, int adminId) throws SQLException {
        // First check if train number exists
        if (isTrainNumberExists(train.getTrainNumber())) {
            return false;
        }
        
        String sql = "INSERT INTO trains (train_number, train_name, total_seats, created_by) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, train.getTrainNumber());
            stmt.setString(2, train.getTrainName());
            stmt.setInt(3, train.getTotalSeats());
            stmt.setInt(4, adminId);
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    private boolean isTrainNumberExists(String trainNumber) throws SQLException {
        String sql = "SELECT 1 FROM trains WHERE train_number = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, trainNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    public boolean updateTrain(Train train) throws SQLException {
        String sql = "UPDATE trains SET train_number=?, train_name=?, total_seats=? WHERE train_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, train.getTrainNumber());
            stmt.setString(2, train.getTrainName());
            stmt.setInt(3, train.getTotalSeats());
            stmt.setInt(4, train.getTrainId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    public boolean deleteTrain(int trainId) throws SQLException {
        String sql = "DELETE FROM trains WHERE train_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, trainId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    public List<Train> getAllTrains() {
        List<Train> trains = new ArrayList<>();
        String sql = "SELECT * FROM trains";
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Train train = new Train();
                train.setTrainId(rs.getInt("train_id"));
                train.setTrainNumber(rs.getString("train_number"));
                train.setTrainName(rs.getString("train_name"));
                train.setTotalSeats(rs.getInt("total_seats"));
                trains.add(train);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return trains;
    }
    
    public Train getTrainById(int trainId) {
        Train train = null;
        String sql = "SELECT * FROM trains WHERE train_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, trainId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    train = new Train();
                    train.setTrainId(rs.getInt("train_id"));
                    train.setTrainNumber(rs.getString("train_number"));
                    train.setTrainName(rs.getString("train_name"));
                    train.setTotalSeats(rs.getInt("total_seats"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return train;
    }
}