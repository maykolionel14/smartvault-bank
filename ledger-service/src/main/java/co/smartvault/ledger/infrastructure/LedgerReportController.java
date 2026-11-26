package co.smartvault.ledger.infrastructure;

import co.smartvault.ledger.application.ReportMovementReader;
import co.smartvault.platform.ServiceKeyGuard;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class LedgerReportController {
    private final ReportMovementReader reader;
    private final ServiceKeyGuard keys;

    public LedgerReportController(ReportMovementReader reader, ServiceKeyGuard keys) {
        this.reader = reader;
        this.keys = keys;
    }

    @GetMapping("/internal/ledger/report-movements/{userId}")
    public List<ReportMovementReader.Movement> recent(
            @RequestHeader(value = "X-Service-Key", required = false) String key,
            @PathVariable String userId) {
        keys.require(key);
        return reader.recentForUser(userId);
    }
}
