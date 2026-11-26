package co.smartvault.vaults.infrastructure;

import org.springframework.data.jpa.repository.*;

public interface VaultJpaRepository extends JpaRepository<VaultRow,String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from VaultRow v where v.id = :id")
    java.util.Optional<VaultRow> lockById(String id);
}
