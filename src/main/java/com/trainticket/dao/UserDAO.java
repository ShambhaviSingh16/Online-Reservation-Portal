package com.trainticket.dao;

import com.trainticket.model.User;
import com.trainticket.util.DBUtil;
import com.trainticket.util.PasswordHasher;
import java.sql.*;

public class UserDAO {
    public User authenticate(String username, String password) {
        User user = null;
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password");
                    boolean encoded = PasswordHasher.isEncoded(stored);
                    if (!(encoded ? PasswordHasher.verify(password, stored) : PasswordHasher.matchesLegacy(password, stored))) {
                        return null;
                    }
                    if (!encoded) {
                        try (PreparedStatement upgrade = conn.prepareStatement(
                                "UPDATE users SET password = ? WHERE user_id = ? AND password = ?")) {
                            upgrade.setString(1, PasswordHasher.hash(password));
                            upgrade.setInt(2, rs.getInt("user_id"));
                            upgrade.setString(3, stored);
                            if (upgrade.executeUpdate() != 1) { return null; }
                        }
                    }
                    user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setFullName(rs.getString("full_name"));
                    user.setRole(rs.getString("role"));
                    user.setCredentialVersion(rs.getInt("credential_version"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    public boolean registerUser(User user) {
        if (user == null || user.getPassword() == null || user.getPassword().length() < 12
                || user.getPassword().length() > 128) { return false; }
        String sql = "INSERT INTO users (username, password, email, full_name, role) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, PasswordHasher.hash(user.getPassword()));
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, "user"); // Default role is user

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public User getUserById(int userId) {
        User user = null;
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setFullName(rs.getString("full_name"));
                    user.setRole(rs.getString("role"));
                    user.setCredentialVersion(rs.getInt("credential_version"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }
}
