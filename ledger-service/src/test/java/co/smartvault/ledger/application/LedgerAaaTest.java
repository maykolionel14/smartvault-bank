package co.smartvault.ledger.application;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LedgerAaaTest {
    @Test
    void creditUsesActorScopedIdempotencyKey() {
        // Arrange
        var ledger = mock(LedgerPort.class);
        var stored = new LedgerPort.Movement("tx", 500, "demo-credit");
        when(ledger.credit("user:alice", 500, "demo:alice:unique-key")).thenReturn(stored);
        var useCases = new LedgerUseCases(ledger);
        // Act
        var result = useCases.credit("alice", 500, "unique-key");
        // Assert
        assertEquals(stored, result);
        verify(ledger).credit("user:alice", 500, "demo:alice:unique-key");
    }

    @Test
    void invalidContributionDoesNotChangeLedger() {
        // Arrange
        var ledger = mock(LedgerPort.class);
        var useCases = new LedgerUseCases(ledger);
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> useCases.contribute("alice", "vault", 0, "unique-key"));
        verifyNoInteractions(ledger);
    }
}
