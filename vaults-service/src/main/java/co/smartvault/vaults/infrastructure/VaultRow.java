package co.smartvault.vaults.infrastructure;

import jakarta.persistence.*;

@Entity @Table(name="vaults_aggregates")
public class VaultRow {
    @Id @Column(length=36) public String id;
    @Column(nullable=false,columnDefinition="text") public String document;
    @Version public long rowVersion;
    protected VaultRow(){
    }
    public VaultRow(String id,String json){
        this.id=id;
        this.document=json;
    }
}
