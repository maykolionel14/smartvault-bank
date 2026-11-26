package co.smartvault.ledger.infrastructure;

import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface WalletJpaRepository extends JpaRepository<WalletRow,String> {
    @Modifying
    @Query(value="insert into ledger_wallets (id,balance_cents,row_version) values (:id,0,0) on conflict (id) do nothing",nativeQuery=true)
    void ensureWallet(String id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletRow w where w.id in :ids order by w.id")
    List<WalletRow> lockMany(@Param("ids") Collection<String> ids);
}
