package co.smartvault.vaults.application;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VaultsMockTest {
    @Test void unknownMembersCannotCreateVaultOrCallLedger(){
        var store=mock(VaultPorts.VaultStore.class);
        var ledger=mock(VaultPorts.LedgerGateway.class);
        var directory=mock(VaultPorts.Directory.class);
        when(directory.existing(List.of("a","missing"))).thenReturn(Set.of("a"));
        var use=new VaultUseCases(store,ledger,directory,()->"uuid");
        assertThrows(IllegalArgumentException.class,()->use.create("a","Grupo",List.of("a","missing"),2,List.of("Hito")));
        verifyNoInteractions(ledger,store);
    }
}
