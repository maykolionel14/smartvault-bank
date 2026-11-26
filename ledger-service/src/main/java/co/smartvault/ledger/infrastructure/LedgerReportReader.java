package co.smartvault.ledger.infrastructure;

import co.smartvault.ledger.application.ReportMovementReader;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public class LedgerReportReader implements ReportMovementReader {
    private final TransferJpaRepository transfers;

    public LedgerReportReader(TransferJpaRepository transfers) {
        this.transfers = transfers;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Movement> recentForUser(String userId) {
        String wallet = "user:" + userId;
        return transfers.findTop100BySourceOrTargetOrderByCreatedAtDescIdDesc(wallet, wallet)
                .stream().map(row -> new Movement(row.id,
                        wallet.equals(row.source) ? -row.amountCents : row.amountCents,
                        row.kind, row.createdAt)).toList();
    }
}
