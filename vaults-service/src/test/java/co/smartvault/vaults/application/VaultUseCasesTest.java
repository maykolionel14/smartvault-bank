package co.smartvault.vaults.application;

import co.smartvault.vaults.domain.Vault;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class VaultUseCasesTest {
    static class Memory implements VaultPorts.VaultStore,VaultPorts.LedgerGateway,VaultPorts.Directory {
        Map<String,Vault> state=new HashMap<>();
        int saves=0,paid=0,opened=0;
        long balance=500L;
        public Optional<Vault> byId(String id){
            return Optional.ofNullable(state.get(id));
        }
        public List<Vault> forMember(String actor){
            return state.values().stream().filter(x->x.members().contains(actor)).toList();
        }
        public List<Vault> all(){
            return List.copyOf(state.values());
        }
        public void save(Vault v){
            state.put(v.id(),v);
            saves++;
        }
        public void open(String id){
            opened++;
        }
        public String contribute(String actor,String vault,long cents,String key){
            return "transfer-"+key;
        }
        public String payout(String vault,String beneficiary,String proposal,long cents){
            paid++;
            return "payout";
        }
        public long balance(String id){
            return balance;
        }
        public Set<String> existing(List<String> ids){
            return new HashSet<>(ids);
        }
    }
    private static VaultUseCases use(Memory db){
        AtomicInteger n=new AtomicInteger();
        return new VaultUseCases(db,db,db,()->"id-"+n.incrementAndGet());
    }
    @Test void lifecycle(){
        var db=new Memory();
        var sut=use(db);
        var vault=sut.create("alice","Fondo veredal",List.of("alice","bob"),2,List.of("Hito validado"));
        assertEquals(1,db.opened);
        assertEquals(1,sut.list("alice").size());
        assertEquals(500,sut.details(vault.id(),"alice").balance_cents());
        assertThrows(SecurityException.class,()->sut.details(vault.id(),"invalid"));
        var ms=vault.milestones().getFirst().id();
        var c=sut.contribute(vault.id(),"alice",100,"long-enough");
        assertEquals(100,c.amount_cents());
        var saveCount=db.saves;
        sut.contribute(vault.id(),"alice",100,"long-enough");
        assertEquals(saveCount,db.saves);
        var proposal=sut.propose(vault.id(),"alice",ms,"bob",90,"Compra comunitaria");
        sut.vote("alice",proposal.proposal_id(),"approve");
        sut.vote("bob",proposal.proposal_id(),"approve");
        assertFalse(sut.status("alice",proposal.proposal_id()).releasable());
        assertFalse(sut.attest("alice",ms,"Acta disponible").ready());
        assertTrue(sut.attest("bob",ms,"Firmado y recibido").ready());
        assertTrue(sut.status("alice",proposal.proposal_id()).releasable());
        assertEquals("executed",sut.execute("bob",proposal.proposal_id()).status());
        assertTrue(sut.execute("bob",proposal.proposal_id()).idempotent());
        assertEquals(1,db.paid);
        assertTrue(sut.verifyAudit("alice",vault.id()).valid());
    }
    @Test void invalidInputsAndNotFound(){
        var db=new Memory();
        var sut=use(db);
        assertThrows(NoSuchElementException.class,()->sut.get("missing","alice"));
        assertThrows(IllegalArgumentException.class,()->sut.create("alice","Grupo",List.of("alice"),2,List.of("Hito")));
        var v=sut.create("alice","Grupo",List.of("alice"),1,List.of("Hito"));
        assertThrows(IllegalArgumentException.class,()->sut.contribute(v.id(),"alice",0,"key123456"));
        assertThrows(NoSuchElementException.class,()->sut.vote("alice","unknown","approve"));
        assertThrows(NoSuchElementException.class,()->sut.attest("alice","unknown","Nota válida"));
    }
}
