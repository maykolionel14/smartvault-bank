package co.smartvault.vaults.application;

import co.smartvault.vaults.domain.Vault;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VaultAaaTest {
    @Test
    void repeatContributionDoesNotDuplicateAudit() {
        // Arrange
        var store = mock(VaultPorts.VaultStore.class);
        var ledger = mock(VaultPorts.LedgerGateway.class);
        var directory = mock(VaultPorts.Directory.class);
        var sequence = new AtomicInteger();
        var vault = Vault.create("vault", "Grupo", "alice", List.of("alice"), 1,
                List.of("Hito"), () -> "id-" + sequence.incrementAndGet()).contribution("alice", "tx", 500);
        when(store.byId("vault")).thenReturn(Optional.of(vault));
        when(ledger.contribute("alice", "vault", 500, "unique-key")).thenReturn("tx");
        var useCases = new VaultUseCases(store, ledger, directory, () -> "id");
        // Act
        var result = useCases.contribute("vault", "alice", 500, "unique-key");
        // Assert
        assertEquals("tx", result.transfer_id());
        verify(store, never()).save(any());
    }

    @Test
    void nonMemberCannotReadVaultOrAskForBalance() {
        // Arrange
        var store = mock(VaultPorts.VaultStore.class);
        var ledger = mock(VaultPorts.LedgerGateway.class);
        var vault = Vault.create("vault", "Grupo", "alice", List.of("alice"), 1, List.of("Hito"), () -> "id");
        when(store.byId("vault")).thenReturn(Optional.of(vault));
        var useCases = new VaultUseCases(store, ledger, mock(VaultPorts.Directory.class), () -> "id");
        // Act / Assert
        assertThrows(SecurityException.class, () -> useCases.details("vault", "outsider"));
        verifyNoInteractions(ledger);
    }
}
