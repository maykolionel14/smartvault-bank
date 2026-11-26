package co.smartvault.ledger.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="ledger_entries")
public class EntryRow {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,length=36) public String transferId;
    @Column(nullable=false,length=90) public String walletId;
    @Column(nullable=false) public long signedCents;
    @Column(nullable=false) public Instant createdAt;
    protected EntryRow() {
    }
    public EntryRow(String transferId,String walletId,long signedCents) {
        this.transferId=transferId;
        this.walletId=walletId;
        this.signedCents=signedCents;
        createdAt=Instant.now();
    }
}
