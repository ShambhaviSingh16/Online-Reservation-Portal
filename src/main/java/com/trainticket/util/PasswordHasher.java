package com.trainticket.util;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Versioned, salted password encoding using the JDK's PBKDF2 implementation. */
public final class PasswordHasher {
    private static final int ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private PasswordHasher() { }

    public static boolean isEncoded(String value) {
        return value != null && value.startsWith("pbkdf2$");
    }

    public static String hash(String password) {
        if (password == null || password.isEmpty() || password.length() > 1024) {
            throw new IllegalArgumentException("Invalid password length");
        }
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "pbkdf2$sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean verify(String password, String encoded) {
        if (password == null || password.isEmpty() || password.length() > 1024 || encoded == null) { return false; }
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 5 || !"pbkdf2".equals(parts[0]) || !"sha256".equals(parts[1])) { return false; }
            int iterations = Integer.parseInt(parts[2]);
            if (iterations < ITERATIONS || iterations > 1_200_000) { return false; }
            byte[] salt = Base64.getDecoder().decode(parts[3]);
            byte[] expected = Base64.getDecoder().decode(parts[4]);
            return salt.length == 16 && expected.length == 32
                    && MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException e) { return false; }
    }

    public static boolean matchesLegacy(String password, String stored) {
        return password != null && !password.isEmpty() && password.length() <= 1024 && stored != null && !isEncoded(stored)
                && MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("Password hashing unavailable", e); }
        finally { spec.clearPassword(); }
    }
}
