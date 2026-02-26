package com.trainticket.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBUtil {

    private static final String DB_URL =
        "jdbc:postgresql://dpg-d6flcofgi27c73fh8cvg-a.singapore-postgres.render.com:5432/trackease_db_1077";

    private static final String DB_USER =
        "trackease_db_1077_user";

    private static final String DB_PASSWORD =
        "hR80ojTJ1lOUTw1N21C6WQaCMnZXTIAA";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
//            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("✅ PostgreSQL Connected Successfully!");
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }
    }

    public static void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
        try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
        try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }
}