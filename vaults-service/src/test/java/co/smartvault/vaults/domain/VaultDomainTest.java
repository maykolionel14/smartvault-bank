package co.smartvault.vaults.domain;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class VaultDomainTest {
    static final String A="aaaa",B="bbbb",C="cccc";
    private Vault newVault(){
        var num=new AtomicInteger();
        return Vault.create("vault-id","Grupo comunitario",A,List.of(A,B,C),2,List.of("Entrega final"),()->"id"+num.incrementAndGet());
    }
    @Test void governsQuorumAndRejects(){
        var g=new Governance(List.of(A,B,C),2);
        assertTrue(g.canRelease(Set.of(A,B),Set.of(A,C)));
        assertFalse(g.canRelease(Set.of(A),Set.of(A,B)));
        assertTrue(g.rejected(Set.of(B,C)));
        assertFalse(g.rejected(Set.of(A)));
        assertThrows(SecurityException.class,()->g.requireMember("unknown"));
        assertThrows(IllegalArgumentException.class,()->new Governance(List.of(A,A),1));
        assertThrows(IllegalArgumentException.class,()->new Governance(List.of(A,B),3));
    }
    @Test void fromCreateThroughReleaseAndAudits(){
        var v=newVault();
        assertEquals(1,v.audit().size());
        assertTrue(v.verifyAudit(A).valid());
        var milestone=v.milestones().getFirst().id();
        v=v.contribution(A,"tx",500);
        assertTrue(v.hasTransfer("tx"));
        assertSame(v,v.contribution(A,"tx",500));
        v=v.propose(A,"proposal",milestone,B,500,"Pago de obra");
        assertFalse(v.status(A,"proposal").releasable());
        v=v.vote(A,"proposal","approve");
        v=v.vote(B,"proposal","approve");
        assertFalse(v.status(A,"proposal").releasable());
        v=v.attest(A,milestone,"Entrega firmada");
        v=v.attest(B,milestone,"Revisión técnica");
        assertTrue(v.status(A,"proposal").releasable());
        v=v.executed(A,"proposal","payout-id");
        assertEquals("executed",v.status(A,"proposal").status());
        assertEquals("payout-id",v.proposal(A,"proposal").payoutId());
        assertTrue(v.verifyAudit(A).valid());
        assertSame(v,v.executed(A,"proposal","payout-id"));
        assertThrows(SecurityException.class,()->newVault().verifyAudit("stranger"));
    }
    @Test void rejectsDuplicateAttestVotesInvalidMilestonesAndConflicts(){
        var v=newVault();
        var milestone=v.milestones().getFirst().id();
        assertThrows(IllegalArgumentException.class,()->v.propose(A,"id","invalid",B,100,"Useful purpose"));
        assertThrows(IllegalArgumentException.class,()->v.attest(A,milestone,"tiny"));
        assertThrows(SecurityException.class,()->v.contribution("outsider","xx",1));
        assertThrows(IllegalArgumentException.class,()->v.contribution(A,"xx",0));
        var v2=v.attest(A,milestone,"Well supported");
        assertThrows(IllegalStateException.class,()->v2.attest(A,milestone,"Same second time"));
        var proposed=v2.propose(A,"p",milestone,B,10,"Verified work");
        var voted=proposed.vote(A,"p","approve");
        assertThrows(IllegalStateException.class,()->voted.vote(A,"p","approve"));
        assertThrows(IllegalStateException.class,()->voted.executed(A,"p","tx"));
        assertThrows(IllegalArgumentException.class,()->proposed.vote(A,"p","maybe"));
    }
    @Test void rejectsWhenQuorumImpossible(){
        var v=newVault();
        var ms=v.milestones().getFirst().id();
        v=v.propose(A,"p",ms,A,100,"Pay supplier");
        v=v.vote(B,"p","reject");
        v=v.vote(C,"p","reject");
        assertEquals("rejected",v.status(A,"p").status());
        var done=v;
        assertThrows(IllegalStateException.class,()->done.vote(A,"p","approve"));
    }
    @Test void malformedAuditIsReported(){
        var v=newVault();
        var fake=new ArrayList<>(v.audit());
        var old=fake.getFirst();
        fake.set(0,new Vault.AuditEvent(old.seq(),old.action(),"tampered",old.previousHash(),old.hash()));
        var tampered=new Vault(v.id(),v.name(),v.creatorId(),v.members(),v.quorum(),v.milestones(),v.proposals(),fake);
        assertFalse(tampered.verifyAudit(A).valid());
        assertEquals(List.of(1),tampered.verifyAudit(A).anomalous_sequences());
        assertEquals(64,AuditHash.sha256("GENESIS",1,"test","payload").length());
    }
    @Test void refusesInvalidVault(){
        assertThrows(IllegalArgumentException.class,()->Vault.create("v","a",A,List.of(A),1,List.of("Hito"),()->"id"));
        assertThrows(IllegalArgumentException.class,()->Vault.create("v","Grupo",A,List.of(A),1,List.of(),()->"id"));
        assertThrows(SecurityException.class,()->Vault.create("v","Grupo",B,List.of(A),1,List.of("Hito"),()->"id"));
    }
}
