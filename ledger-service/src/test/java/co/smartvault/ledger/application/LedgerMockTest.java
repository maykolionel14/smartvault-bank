package co.smartvault.ledger.application;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LedgerMockTest {
    @Test void failedValidationDoesNotReachStorage(){
        var port=mock(LedgerPort.class);
        var use=new LedgerUseCases(port);
        assertThrows(IllegalArgumentException.class,()->use.credit("a",-1,"abcdefgh"));
        assertThrows(IllegalArgumentException.class,()->use.contribute("a","v",1,"short"));
        verifyNoInteractions(port);
    }
}
