package co.smartvault.reports.infrastructure;

import co.smartvault.platform.BusinessException;
import co.smartvault.platform.RemoteIdentityGuard;
import co.smartvault.reports.application.MovementPort;
import co.smartvault.reports.application.ReportSourceUnavailable;
import co.smartvault.reports.application.ReportUseCases;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportsControllerTest {
    @Test
    void unauthorizedRequestDoesNotQueryLedger() {
        // Arrange
        var identity = mock(RemoteIdentityGuard.class);
        var port = mock(MovementPort.class);
        when(identity.actor(null)).thenThrow(BusinessException.unauthorized("Sin sesión"));
        var controller = new ReportsController(new ReportUseCases(port), identity);
        // Act
        var error = assertThrows(BusinessException.class, () -> controller.summary(null));
        // Assert
        assertEquals(401, error.httpStatus());
        verifyNoInteractions(port);
    }

    @Test
    void mapsLedgerFailureTo503InsteadOfEmptyReport() {
        // Arrange
        var identity = mock(RemoteIdentityGuard.class);
        var port = mock(MovementPort.class);
        when(identity.actor("Bearer test")).thenReturn("alice");
        when(port.recentForUser("alice")).thenThrow(new ReportSourceUnavailable(null));
        var controller = new ReportsController(new ReportUseCases(port), identity);
        // Act
        var error = assertThrows(BusinessException.class, () -> controller.summary("Bearer test"));
        // Assert
        assertEquals(503, error.httpStatus());
    }

    @Test
    void returnsReportForVerifiedSubject() {
        // Arrange
        var identity = mock(RemoteIdentityGuard.class);
        var port = mock(MovementPort.class);
        when(identity.actor("Bearer test")).thenReturn("bob");
        when(port.recentForUser("bob")).thenReturn(List.of());
        var controller = new ReportsController(new ReportUseCases(port), identity);
        // Act
        var report = controller.summary("Bearer test");
        // Assert
        assertEquals(0, report.movement_count());
        verify(port).recentForUser("bob");
    }
}
