package co.smartvault.identity.infrastructure;

import jakarta.persistence.*;

@Entity @Table(name="identity_users",uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class UserRow {
    @Id @Column(length=36) public String id;
    @Column(nullable=false,length=254) public String email;
    @Column(nullable=false,length=120) public String passwordHash;
    @Column(nullable=false) public boolean suspended;
    @Column(nullable=false,columnDefinition="boolean default false") public boolean activated;
    @Column(nullable=false) public int sessionVersion;
    @Version public long lockVersion;
    protected UserRow() {
    }
    public UserRow(String id,String email,String hash,boolean suspended,int version) {
        this.id=id;
        this.email=email;
        this.passwordHash=hash;
        this.suspended=suspended;
        this.sessionVersion=version;
    }
}
