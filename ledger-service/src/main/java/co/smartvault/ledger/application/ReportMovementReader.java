package co.smartvault.ledger.application;

import java.time.Instant;
import java.util.List;

/** Read-only port for the report service. The wallet is always a user wallet. */
public interface ReportMovementReader {
    record Movement(String transfer_id, long signed_cents, String kind, Instant created_at) {}

    List<Movement> recentForUser(String userId);
}
