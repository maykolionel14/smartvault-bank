package co.smartvault.ledger.infrastructure;

import co.smartvault.platform.BusinessException;
import co.smartvault.platform.ServiceKeyGuard;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LedgerReportReaderTest {
    @Test
    void mapsOnlyRequestedWalletAndCorrectSigns() {
        // Arrange
        var transfers = mock(TransferJpaRepository.class);
        var incoming = new TransferRow("credit", "key1", "DEMO_MINT", "user:alice", 5000, "demo-credit");
        var outgoing = new TransferRow("saving", "key2", "user:alice", "vault:v", 1000, "contribution");
        when(transfers.findTop100BySourceOrTargetOrderByCreatedAtDescIdDesc("user:alice", "user:alice"))
                .thenReturn(List.of(outgoing, incoming));
        var reader = new LedgerReportReader(transfers);
        // Act
        var movements = reader.recentForUser("alice");
        // Assert
        assertEquals(-1000, movements.get(0).signed_cents());
        assertEquals(5000, movements.get(1).signed_cents());
        assertEquals("contribution", movements.get(0).kind());
        verify(transfers).findTop100BySourceOrTargetOrderByCreatedAtDescIdDesc("user:alice", "user:alice");
    }

    @Test
    void internalEndpointRequiresServiceKey() {
        // Arrange
        var reader = mock(co.smartvault.ledger.application.ReportMovementReader.class);
        var controller = new LedgerReportController(reader, new ServiceKeyGuard("test-key-012345678901234567890123456789"));
        // Act
        var error = assertThrows(BusinessException.class, () -> controller.recent(null, "alice"));
        // Assert
        assertEquals(403, error.httpStatus());
        verifyNoInteractions(reader);
    }
}
