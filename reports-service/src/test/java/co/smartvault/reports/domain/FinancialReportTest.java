package co.smartvault.reports.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FinancialReportTest {
    private static final Instant NOW = Instant.parse("2026-11-26T00:00:00Z");

    @Test
    void calculatesIncomeExpensesContributionsAndNetInCents() {
        // Arrange
        var entries = List.of(new Movement("a", 5000, "demo-credit", NOW),
                new Movement("b", -1200, "contribution", NOW),
                new Movement("c", 300, "governed-payout", NOW));

        // Act
        FinancialReport report = FinancialReport.from(entries);

        // Assert
        assertEquals(5300, report.income_cents());
        assertEquals(1200, report.expense_cents());
        assertEquals(1200, report.contribution_cents());
        assertEquals(4100, report.net_cents());
        assertEquals(3, report.movement_count());
        assertEquals("COP", report.currency());
        assertEquals(2, report.income().movement_count());
        assertEquals(1, report.expenses().movement_count());
        assertEquals(1200, report.contributions().total_cents());
    }

    @Test
    void emptyReportIsZero() {
        // Arrange
        List<Movement> entries = List.of();
        // Act
        var report = FinancialReport.from(entries);
        // Assert
        assertEquals(0, report.net_cents());
        assertEquals(0, report.movement_count());
        assertEquals(100, report.limit());
        assertTrue(report.movements().isEmpty());
    }

    @Test
    void rejectsDuplicateTransfer() {
        // Arrange
        var movement = new Movement("a", 5000, "demo-credit", NOW);
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> FinancialReport.from(List.of(movement, movement)));
    }

    @Test
    void reportDoesNotChangeWhenOriginalListChanges() {
        // Arrange
        var entries = new ArrayList<>(List.of(new Movement("a", 5000, "demo-credit", NOW)));
        // Act
        var report = FinancialReport.from(entries);
        entries.clear();
        // Assert
        assertEquals(1, report.movements().size());
        assertThrows(UnsupportedOperationException.class, () -> report.movements().clear());
    }

    @Test
    void rejectsOverflowAndInvalidMovement() {
        // Arrange
        var entries = List.of(new Movement("a", Long.MAX_VALUE, "demo-credit", NOW),
                new Movement("b", 1, "demo-credit", NOW));
        // Act / Assert
        assertThrows(ArithmeticException.class, () -> FinancialReport.from(entries));
        assertThrows(IllegalArgumentException.class, () -> new Movement("c", 0, "contribution", NOW));
    }
}
