package co.smartvault.identity.infrastructure;

import jakarta.persistence.*;

@Entity
@Table(name="identity_auth_budget")
public class AuthBudgetRow {
    @Id @Column(length=90) String id;
    @Column(nullable=false) long period;
    @Column(nullable=false) int requests;
}
