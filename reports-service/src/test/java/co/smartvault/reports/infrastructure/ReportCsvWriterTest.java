package co.smartvault.reports.infrastructure;

import co.smartvault.reports.domain.FinancialReport;
import co.smartvault.reports.domain.Movement;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReportCsvWriterTest {
    @Test
    void serializesAmountsAndEscapesFormulaLikeText() {
        // Arrange
        var report = FinancialReport.from(List.of(new Movement("=unsafe", -100, "contribution", Instant.EPOCH)));
        // Act
        String csv = ReportCsvWriter.write(report);
        // Assert
        assertTrue(csv.startsWith("transfer_id,created_at,kind,signed_cents,currency"));
        assertTrue(csv.contains("'=unsafe"));
        assertTrue(csv.contains(",-100,COP"));
    }
}
