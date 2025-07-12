package com.trainticket.dao;

import com.trainticket.model.Station;
import com.trainticket.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StationDAO {
	public boolean addStation(Station station) throws SQLException {
        // First check if station code exists
        if (isStationCodeExists(station.getStationCode())) {
            return false;
        }
        
        String sql = "INSERT INTO stations (station_code, station_name, city, state) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, station.getStationCode());
            stmt.setString(2, station.getStationName());
            stmt.setString(3, station.getCity());
            stmt.setString(4, station.getState());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    private boolean isStationCodeExists(String stationCode) throws SQLException {
        String sql = "SELECT 1 FROM stations WHERE station_code = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, stationCode);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean updateStation(Station station) throws SQLException {
        String sql = "UPDATE stations SET station_code=?, station_name=?, city=?, state=? WHERE station_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, station.getStationCode());
            stmt.setString(2, station.getStationName());
            stmt.setString(3, station.getCity());
            stmt.setString(4, station.getState());
            stmt.setInt(5, station.getStationId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    public boolean deleteStation(int stationId) throws SQLException {
        String sql = "DELETE FROM stations WHERE station_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, stationId);
            return stmt.executeUpdate() > 0;
        }
    }
    public List<Station> getAllStations() {
        List<Station> stations = new ArrayList<>();
        String sql = "SELECT * FROM stations";
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Station station = new Station();
                station.setStationId(rs.getInt("station_id"));
                station.setStationCode(rs.getString("station_code"));
                station.setStationName(rs.getString("station_name"));
                station.setCity(rs.getString("city"));
                station.setState(rs.getString("state"));
                stations.add(station);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stations;
    }
    
    public Station getStationById(int stationId) {
        Station station = null;
        String sql = "SELECT * FROM stations WHERE station_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, stationId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    station = new Station();
                    station.setStationId(rs.getInt("station_id"));
                    station.setStationCode(rs.getString("station_code"));
                    station.setStationName(rs.getString("station_name"));
                    station.setCity(rs.getString("city"));
                    station.setState(rs.getString("state"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return station;
    }
}