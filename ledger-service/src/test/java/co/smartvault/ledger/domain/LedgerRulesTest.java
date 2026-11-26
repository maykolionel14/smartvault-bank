package co.smartvault.ledger.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LedgerRulesTest {
    @Test void moneyAndDemoBoundaries(){
        assertEquals(1,new Money(1).cents());
        assertThrows(IllegalArgumentException.class,()->new Money(0));
        assertThrows(IllegalArgumentException.class,()->new Money(-3));
        assertEquals(10_000_000,Money.demo(10_000_000).cents());
        assertThrows(IllegalArgumentException.class,()->Money.demo(10_000_001));
    }
    @Test void doubleEntriesAndFunds(){
        assertTrue(PostingPolicy.balanced(List.of(-50L,50L)));
        assertFalse(PostingPolicy.balanced(List.of(-50L,49L)));
        assertFalse(PostingPolicy.balanced(List.of(-50L)));
        assertDoesNotThrow(()->PostingPolicy.validateTransfer("u","v",new Money(2),2));
        assertThrows(IllegalStateException.class,()->PostingPolicy.validateTransfer("u","v",new Money(3),2));
        assertThrows(IllegalArgumentException.class,()->PostingPolicy.validateTransfer("u","u",new Money(1),5));
        assertThrows(IllegalArgumentException.class,()->PostingPolicy.validIdempotencyKey("short"));
        assertDoesNotThrow(()->PostingPolicy.validIdempotencyKey("abcdefgh"));
    }
}
