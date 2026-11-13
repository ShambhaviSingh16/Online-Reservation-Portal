package com.trainticket.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test void passwordsAreSaltedAndVerifiable() {
        String first = PasswordHasher.hash("A long password with spaces!");
        String second = PasswordHasher.hash("A long password with spaces!");
        assertNotEquals(first, second);
        assertTrue(PasswordHasher.verify("A long password with spaces!", first));
        assertFalse(PasswordHasher.verify("Wrong password", first));
        assertFalse(PasswordHasher.matchesLegacy(first, first));
    }

    @Test void malformedHashesFailClosed() {
        for (String value : new String[] {"pbkdf2$bad", "pbkdf2$sha256$999999999$x$x", "pbkdf2$sha256$600000$bad$bad"}) {
            assertFalse(PasswordHasher.verify("password", value));
            assertFalse(PasswordHasher.matchesLegacy("password", value));
        }
        assertFalse(PasswordHasher.verify(null, "hash"));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(""));
    }

    @Test void legacyComparisonRequiresExactPassword() {
        assertTrue(PasswordHasher.matchesLegacy("Legacy123!", "Legacy123!"));
        assertFalse(PasswordHasher.matchesLegacy("legacy123!", "Legacy123!"));
        assertFalse(PasswordHasher.matchesLegacy(null, "Legacy123!"));
    }
}
