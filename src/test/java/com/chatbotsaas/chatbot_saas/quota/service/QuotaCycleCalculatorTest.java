package com.chatbotsaas.chatbot_saas.quota.service;

import com.chatbotsaas.chatbot_saas.quota.service.QuotaCycleCalculator.CycleWindow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuotaCycleCalculatorTest {

    @Test
    @DisplayName("clampCycleDay: valores fuera de rango se normalizan a 1..28")
    void clampCycleDay_bounds() {
        assertEquals(1, QuotaCycleCalculator.clampCycleDay(0));
        assertEquals(1, QuotaCycleCalculator.clampCycleDay(-5));
        assertEquals(1, QuotaCycleCalculator.clampCycleDay(1));
        assertEquals(15, QuotaCycleCalculator.clampCycleDay(15));
        assertEquals(28, QuotaCycleCalculator.clampCycleDay(28));
        assertEquals(28, QuotaCycleCalculator.clampCycleDay(29));
        assertEquals(28, QuotaCycleCalculator.clampCycleDay(31));
        assertEquals(28, QuotaCycleCalculator.clampCycleDay(100));
    }

    @Test
    @DisplayName("Tenant día 15, today = 20/mar → ciclo 15/mar – 14/abr")
    void windowFor_midMonth_afterAnchor() {
        CycleWindow w = QuotaCycleCalculator.windowFor(LocalDate.of(2026, 3, 20), 15);
        assertEquals(LocalDate.of(2026, 3, 15), w.start());
        assertEquals(LocalDate.of(2026, 4, 14), w.end());
    }

    @Test
    @DisplayName("Tenant día 15, today = 10/mar → ciclo 15/feb – 14/mar")
    void windowFor_midMonth_beforeAnchor() {
        CycleWindow w = QuotaCycleCalculator.windowFor(LocalDate.of(2026, 3, 10), 15);
        assertEquals(LocalDate.of(2026, 2, 15), w.start());
        assertEquals(LocalDate.of(2026, 3, 14), w.end());
    }

    @Test
    @DisplayName("Tenant día 15, today == anchor → start = today, end = today + 1 mes - 1 día")
    void windowFor_todayEqualsAnchor() {
        CycleWindow w = QuotaCycleCalculator.windowFor(LocalDate.of(2026, 3, 15), 15);
        assertEquals(LocalDate.of(2026, 3, 15), w.start());
        assertEquals(LocalDate.of(2026, 4, 14), w.end());
    }

    @Test
    @DisplayName("Tenant día 28, today = 28/feb (no bisiesto) → ciclo 28/feb – 27/mar")
    void windowFor_day28_atEndOfFebruary() {
        CycleWindow w = QuotaCycleCalculator.windowFor(LocalDate.of(2026, 2, 28), 28);
        assertEquals(LocalDate.of(2026, 2, 28), w.start());
        assertEquals(LocalDate.of(2026, 3, 27), w.end());
    }

    @Test
    @DisplayName("Tenant día 28, today = 3/mar → ciclo 28/feb – 27/mar (anchor baja al mes previo)")
    void windowFor_day28_afterFebruary() {
        CycleWindow w = QuotaCycleCalculator.windowFor(LocalDate.of(2026, 3, 3), 28);
        assertEquals(LocalDate.of(2026, 2, 28), w.start());
        assertEquals(LocalDate.of(2026, 3, 27), w.end());
    }

    @Test
    @DisplayName("Tenant día 1, today = 1/ene → ciclo 1/ene – 31/ene")
    void windowFor_day1_startOfMonth() {
        CycleWindow w = QuotaCycleCalculator.windowFor(LocalDate.of(2026, 1, 1), 1);
        assertEquals(LocalDate.of(2026, 1, 1), w.start());
        assertEquals(LocalDate.of(2026, 1, 31), w.end());
    }

    @Test
    @DisplayName("Tenant día 1, today = 15/ene → ciclo 1/ene – 31/ene")
    void windowFor_day1_midMonth() {
        CycleWindow w = QuotaCycleCalculator.windowFor(LocalDate.of(2026, 1, 15), 1);
        assertEquals(LocalDate.of(2026, 1, 1), w.start());
        assertEquals(LocalDate.of(2026, 1, 31), w.end());
    }

    @Test
    @DisplayName("billing_cycle_day crudo = 31 se clampa a 28")
    void windowFor_rawDay31_clampedTo28() {
        CycleWindow w = QuotaCycleCalculator.windowFor(LocalDate.of(2026, 3, 5), 31);
        // Equivalente a cycleDay = 28
        assertEquals(LocalDate.of(2026, 2, 28), w.start());
        assertEquals(LocalDate.of(2026, 3, 27), w.end());
    }

    @Test
    @DisplayName("Transición de fin-de-año: today = 5/ene, day = 20 → ciclo 20/dic del año anterior – 19/ene")
    void windowFor_yearTransition() {
        CycleWindow w = QuotaCycleCalculator.windowFor(LocalDate.of(2026, 1, 5), 20);
        assertEquals(LocalDate.of(2025, 12, 20), w.start());
        assertEquals(LocalDate.of(2026, 1, 19), w.end());
    }

    @Test
    @DisplayName("Tenant día 5, today = 6/feb → ciclo 5/feb – 4/mar")
    void windowFor_day5_earlyMonth() {
        CycleWindow w = QuotaCycleCalculator.windowFor(LocalDate.of(2026, 2, 6), 5);
        assertEquals(LocalDate.of(2026, 2, 5), w.start());
        assertEquals(LocalDate.of(2026, 3, 4), w.end());
    }
}
