package co.smartvault.reports.application;

import co.smartvault.reports.domain.Movement;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportUseCasesTest {
    @Test
    void obtainsOnlyTheAuthenticatedActorsMovements() {
        // Arrange
        MovementPort port = mock(MovementPort.class);
        when(port.recentForUser("alice")).thenReturn(List.of(new Movement("tx", 500, "demo-credit", Instant.EPOCH)));
        var useCases = new ReportUseCases(port);
        // Act
        var report = useCases.summary("alice");
        // Assert
        assertEquals(500, report.net_cents());
        verify(port).recentForUser("alice");
        verifyNoMoreInteractions(port);
    }

    @Test
    void recoversWhenLedgerBecomesAvailableAgain() {
        // Arrange
        MovementPort port = mock(MovementPort.class);
        when(port.recentForUser("alice"))
                .thenThrow(new ReportSourceUnavailable(new IllegalStateException("offline")))
                .thenReturn(List.of(new Movement("tx", 800, "demo-credit", Instant.EPOCH)));
        var useCases = new ReportUseCases(port);
        // Act
        var failure = assertThrows(ReportSourceUnavailable.class, () -> useCases.summary("alice"));
        var recovered = useCases.summary("alice");
        // Assert
        assertNotNull(failure.getCause());
        assertEquals(800, recovered.net_cents());
        verify(port, times(2)).recentForUser("alice");
    }

    @Test
    void blankActorDoesNotReachLedger() {
        // Arrange
        MovementPort port = mock(MovementPort.class);
        var useCases = new ReportUseCases(port);
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> useCases.summary(" "));
        verifyNoInteractions(port);
    }
}
