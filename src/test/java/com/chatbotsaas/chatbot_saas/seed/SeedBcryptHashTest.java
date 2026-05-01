package com.chatbotsaas.chatbot_saas.seed;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida que el hash BCrypt quemado en
 * {@code src/main/resources/db/seed/seed_initial_admin.sql} efectivamente
 * corresponde a la contraseña documentada "Zolvion2026!". Si alguien regenera
 * el hash para otra contraseña, este test le avisa.
 */
class SeedBcryptHashTest {

    private static final String SEEDED_HASH =
            "$2b$10$tu.a1Q/kY.h0n2OAGPr4O.Rk.2AGqlJ9jjGSVzgDYzxp2W3MUulTC";
    private static final String EXPECTED_PASSWORD = "Zolvion2026!";

    @Test
    void hashMatchesDocumentedPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches(EXPECTED_PASSWORD, SEEDED_HASH),
                "El hash del seed no matchea Zolvion2026! — regenerar y actualizar seed_initial_admin.sql");
    }

    @Test
    void hashRejectsOtherPasswords() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertFalse(encoder.matches("wrong", SEEDED_HASH));
        assertFalse(encoder.matches("zolvion2026!", SEEDED_HASH)); // case-sensitive
        assertFalse(encoder.matches("Zolvion2026", SEEDED_HASH));  // sin !
    }
}
