package com.trainticket.dao;

import com.trainticket.util.DBUtil;
import com.trainticket.util.PasswordHasher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;

/** Random 128-bit secrets are displayed once; only SHA-256 digests are retained. */
public class RecoveryDAO {
    private static final SecureRandom RANDOM=new SecureRandom();
    private static String digest(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.strip().toUpperCase(Locale.ROOT).replace("-","").getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public List<String> generate(int user,String password) throws SQLException {
        List<String> codes=new ArrayList<>();
        try(Connection c=DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                String stored;
                try(PreparedStatement s=c.prepareStatement("SELECT password FROM users WHERE user_id=? FOR UPDATE")) {
                    s.setInt(1,user);try(ResultSet r=s.executeQuery()){if(!r.next()){c.rollback();return List.of();}stored=r.getString(1);}
                }
                if(!(PasswordHasher.isEncoded(stored)?PasswordHasher.verify(password,stored):PasswordHasher.matchesLegacy(password,stored))){c.rollback();return List.of();}
                try(PreparedStatement s=c.prepareStatement("DELETE FROM recovery_codes WHERE user_id=?")){s.setInt(1,user);s.executeUpdate();}
                try(PreparedStatement s=c.prepareStatement("INSERT INTO recovery_codes(user_id,code_hash) VALUES(?,?)")) {
                    for(int i=0;i<8;i++) {
                        byte[] bytes=new byte[16];RANDOM.nextBytes(bytes);String raw=HexFormat.of().withUpperCase().formatHex(bytes);
                        String code=String.join("-",raw.substring(0,8),raw.substring(8,16),raw.substring(16,24),raw.substring(24,32));
                        codes.add(code);s.setInt(1,user);s.setString(2,digest(code));s.addBatch();
                    }
                    s.executeBatch();
                }
                c.commit();return codes;
            }catch(SQLException e){c.rollback();throw e;}
        }
    }
    public boolean reset(String username,String code,String newPassword) throws SQLException {
        if(username==null || username.length()>100 || code==null || code.length()>64 || newPassword==null || newPassword.length()<12 || newPassword.length()>128)return false;
        if(!code.strip().replace("-","").matches("[a-fA-F0-9]{32}"))return false;
        try(Connection c=DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                int user;
                try(PreparedStatement s=c.prepareStatement("SELECT user_id FROM users WHERE username=? FOR UPDATE")) {
                    s.setString(1,username);try(ResultSet r=s.executeQuery()){if(!r.next()){c.rollback();return false;}user=r.getInt(1);}
                }
                try(PreparedStatement s=c.prepareStatement("DELETE FROM recovery_codes WHERE user_id=? AND code_hash=?")) {
                    s.setInt(1,user);s.setString(2,digest(code));if(s.executeUpdate()!=1){c.rollback();return false;}
                }
                try(PreparedStatement s=c.prepareStatement("UPDATE users SET password=?,credential_version=credential_version+1 WHERE user_id=?")) {
                    s.setString(1,PasswordHasher.hash(newPassword));s.setInt(2,user);s.executeUpdate();
                }
                // A recovered account must explicitly generate a new set after signing in.
                try(PreparedStatement s=c.prepareStatement("DELETE FROM recovery_codes WHERE user_id=?")){s.setInt(1,user);s.executeUpdate();}
                try(PreparedStatement s=c.prepareStatement("DELETE FROM password_reset_tokens WHERE user_id=?")){s.setInt(1,user);s.executeUpdate();}
                c.commit();return true;
            }catch(SQLException e){c.rollback();throw e;}
        }
    }
    public int version(int user) throws SQLException {
        try(Connection c=DBUtil.getConnection();PreparedStatement s=c.prepareStatement("SELECT credential_version FROM users WHERE user_id=?")) {
            s.setInt(1,user);try(ResultSet r=s.executeQuery()){return r.next()?r.getInt(1):-1;}
        }
    }
}
