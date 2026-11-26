package co.smartvault.ledger.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface TransferJpaRepository extends JpaRepository<TransferRow,String> {
    Optional<TransferRow> findByIdempotencyKey(String key);
    List<TransferRow> findTop100BySourceOrTargetOrderByCreatedAtDescIdDesc(String source, String target);
}
