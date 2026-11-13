package com.trainticket.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBUtil {
    private static volatile javax.sql.DataSource dataSource;
    public static void configure(javax.sql.DataSource source) { dataSource = source; }
    private static String setting(Properties properties, String key, String env) {
        String value = System.getenv(env);
        return value == null || value.isBlank() ? properties.getProperty(key) : value;
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource != null) { return dataSource.getConnection(); }
        Properties properties = new Properties();
        Path config = Path.of(System.getProperty("trackease.config", "local.properties"));
        try {
            if (Files.exists(config)) {
                try (InputStream input = Files.newInputStream(config)) {
                    properties.load(input);
                }
            }
            String url = setting(properties, "db.url", "DB_URL");
            String user = setting(properties, "db.user", "DB_USER");
            String password = setting(properties, "db.password", "DB_PASSWORD");
            if (url == null || user == null || password == null) {
                throw new SQLException("Configure DB_URL, DB_USER and DB_PASSWORD, or local.properties. See README.md.");
            }
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException | IOException e) {
            throw new SQLException("Cannot load database configuration or PostgreSQL driver", e);
        }
    }

    public static void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
        try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
        try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }
}
