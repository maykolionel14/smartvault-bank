package co.smartvault.ledger.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="ledger_transfers", uniqueConstraints=@UniqueConstraint(columnNames="idempotencyKey"),
        indexes={@Index(name="idx_ledger_transfers_source", columnList="source"),
                 @Index(name="idx_ledger_transfers_target", columnList="target")})
public class TransferRow {
    @Id @Column(length=36) public String id;
    @Column(nullable=false,unique=true,length=180) public String idempotencyKey;
    @Column(nullable=false,length=90) public String source;
    @Column(nullable=false,length=90) public String target;
    @Column(nullable=false) public long amountCents;
    @Column(nullable=false,length=35) public String kind;
    @Column(nullable=false) public Instant createdAt;
    protected TransferRow() {
    }
    public TransferRow(String id,String key,String src,String tgt,long cents,String kind) {
        this.id=id;
        idempotencyKey=key;
        source=src;
        target=tgt;
        amountCents=cents;
        this.kind=kind;
        createdAt=Instant.now();
    }
}
