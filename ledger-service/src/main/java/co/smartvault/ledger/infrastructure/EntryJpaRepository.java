package co.smartvault.ledger.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EntryJpaRepository extends JpaRepository<EntryRow,Long> {
    List<EntryRow> findTop100ByWalletIdOrderByIdDesc(String walletId);
    List<EntryRow> findByTransferId(String transferId);
}
