package co.smartvault.identity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpringUserRepository extends JpaRepository<UserRow,String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UserRow u where u.id = :id")
    UserRow lockAccount(String id);
    Optional<UserRow> findByEmail(String email);
}
