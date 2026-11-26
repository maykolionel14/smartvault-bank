package co.smartvault.ledger.infrastructure;

import jakarta.persistence.*;

@Entity @Table(name="ledger_wallets")
public class WalletRow {
    @Id @Column(length=90) public String id;
    @Column(nullable=false) public long balanceCents;
    @Version public long rowVersion;
    protected WalletRow() {
    }
    public WalletRow(String id) {
        this.id=id;
        this.balanceCents=0;
    }
}
