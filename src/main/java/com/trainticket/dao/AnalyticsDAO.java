package com.trainticket.dao;

import com.trainticket.util.DBUtil;
import java.sql.*;
import java.util.*;

public class AnalyticsDAO {
    public Map<String,Object> summary() throws SQLException {
        try(Connection conn=DBUtil.getConnection(); Statement stmt=conn.createStatement();
            ResultSet rs=stmt.executeQuery("SELECT "
                + "(SELECT COUNT(*) FROM trains WHERE is_active) AS trains,"
                + "(SELECT COUNT(*) FROM journeys WHERE departure_time>CURRENT_TIMESTAMP) AS upcoming_journeys,"
                + "(SELECT COUNT(*) FROM tickets WHERE DATE(booking_date)=CURRENT_DATE) AS bookings_today,"
                + "(SELECT COUNT(*) FROM tickets WHERE status='WAITLISTED') AS waiting,"
                + "(SELECT COUNT(*) FROM tickets WHERE status='CONFIRMED') AS confirmed,"
                + "(SELECT COALESCE(SUM(booked_fare),0) FROM tickets WHERE status='CONFIRMED') AS confirmed_booking_value")) {
            rs.next(); Map<String,Object> values=new LinkedHashMap<>();
            for(int i=1;i<=rs.getMetaData().getColumnCount();i++) { values.put(rs.getMetaData().getColumnLabel(i),rs.getObject(i)); }
            return values;
        }
    }
}
