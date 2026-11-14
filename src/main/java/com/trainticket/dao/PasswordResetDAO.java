package com.trainticket.dao;

import com.trainticket.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;

public class PasswordResetDAO {
    public record ResetRequest(String email,String token){}
    private static String digest(String token) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public ResetRequest request(String username) throws SQLException {
        try(Connection c=DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                int user;String email;
                try(PreparedStatement s=c.prepareStatement("SELECT user_id,email FROM users WHERE username=? FOR UPDATE")) {
                    s.setString(1,username);try(ResultSet r=s.executeQuery()){if(!r.next()){c.rollback();return null;}user=r.getInt(1);email=r.getString(2);}
                }
                try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM password_reset_tokens WHERE user_id=? AND created_at>CURRENT_TIMESTAMP-INTERVAL '2 minutes'")) {
                    s.setInt(1,user);try(ResultSet r=s.executeQuery()){if(r.next()){c.rollback();return null;}}
                }
                try(PreparedStatement s=c.prepareStatement("DELETE FROM password_reset_tokens WHERE user_id=?")){s.setInt(1,user);s.executeUpdate();}
                byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=HexFormat.of().formatHex(bytes);
                try(PreparedStatement s=c.prepareStatement("INSERT INTO password_reset_tokens(token_hash,user_id,expires_at) VALUES(?,?,CURRENT_TIMESTAMP+INTERVAL '30 minutes')")){s.setString(1,digest(token));s.setInt(2,user);s.executeUpdate();}
                c.commit();return new ResetRequest(email,token);
            }catch(SQLException e){c.rollback();throw e;}
        }
    }
    public boolean complete(String token,String password) throws SQLException {
        if(token==null || !token.matches("[0-9a-f]{64}") || password==null || password.length()<12 || password.length()>128)return false;
        try(Connection c=DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                int user;
                try(PreparedStatement s=c.prepareStatement("SELECT user_id FROM password_reset_tokens WHERE token_hash=? AND expires_at>CURRENT_TIMESTAMP")) {
                    s.setString(1,digest(token));try(ResultSet r=s.executeQuery()){if(!r.next()){c.rollback();return false;}user=r.getInt(1);}
                }
                try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM users WHERE user_id=? FOR UPDATE")){s.setInt(1,user);s.executeQuery().close();}
                try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM password_reset_tokens WHERE token_hash=? AND user_id=? AND expires_at>CURRENT_TIMESTAMP FOR UPDATE")) {
                    s.setString(1,digest(token));s.setInt(2,user);try(ResultSet r=s.executeQuery()){if(!r.next()){c.rollback();return false;}}
                }
                try(PreparedStatement s=c.prepareStatement("UPDATE users SET password=?,credential_version=credential_version+1 WHERE user_id=?")){s.setString(1,PasswordHasher.hash(password));s.setInt(2,user);s.executeUpdate();}
                for(String table:List.of("password_reset_tokens","recovery_codes"))try(PreparedStatement s=c.prepareStatement("DELETE FROM "+table+" WHERE user_id=?")){s.setInt(1,user);s.executeUpdate();}
                c.commit();return true;
            }catch(SQLException e){c.rollback();throw e;}
        }
    }
}
