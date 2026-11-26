package co.smartvault.ledger.application;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LedgerUseCasesTest {
    static class Fake implements LedgerPort {
        String from,to,key,kind;
        long amount;
        boolean opened;
        public Movement credit(String wallet,long cents,String key){
            from="DEMO_MINT";
            to=wallet;
            amount=cents;
            this.key=key;
            return new Movement("x",cents,"demo-credit");
        }
        public Movement transfer(String from,String to,long cents,String key,String kind){
            this.from=from;
            this.to=to;
            amount=cents;
            this.key=key;
            this.kind=kind;
            return new Movement("y",cents,kind);
        }
        public void openVault(String id){
            opened=true;
        }
        public long balance(String id){
            return 12;
        }
        public List<Entry> statement(String id){
            return List.of();
        }
        public Integrity integrity(){
            return new Integrity(true,0,List.of());
        }
    }
    @Test void commands(){
        var fake=new Fake();
        var use=new LedgerUseCases(fake);
        assertEquals("demo-credit",use.credit("a",50,"abcdefgh").kind());
        assertEquals("user:a",fake.to);
        assertTrue(fake.key.startsWith("demo:a:"));
        assertEquals("contribution",use.contribute("a","v",50,"abcdefgh").kind());
        assertEquals("vault:v",fake.to);
        assertEquals("governed-payout",use.payout("v","a","p",20).kind());
        assertEquals("approved-proposal:p",fake.key);
        use.openVault("v");
        assertTrue(fake.opened);
        assertEquals(12,use.userBalance("a"));
        assertEquals(12,use.vaultBalance("v"));
        assertTrue(use.statement("a").isEmpty());
        assertTrue(use.integrity().valid());
        assertThrows(IllegalArgumentException.class,()->use.credit("a",0,"abcdefgh"));
        assertThrows(IllegalArgumentException.class,()->use.contribute("a","v",20,"bad"));
        assertThrows(IllegalArgumentException.class,()->use.openVault(""));
    }
}
