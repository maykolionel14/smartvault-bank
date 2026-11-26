package co.smartvault.reports.infrastructure;

import co.smartvault.platform.BusinessException;
import co.smartvault.platform.RemoteIdentityGuard;
import co.smartvault.reports.application.ReportSourceUnavailable;
import co.smartvault.reports.application.ReportUseCases;
import co.smartvault.reports.domain.FinancialReport;
import co.smartvault.reports.domain.Movement;
import co.smartvault.reports.domain.MovementReport;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class ReportsController {
    private final ReportUseCases reports;
    private final RemoteIdentityGuard identity;

    public ReportsController(ReportUseCases reports, RemoteIdentityGuard identity) {
        this.reports = reports;
        this.identity = identity;
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok", "service", "reports"); }

    @GetMapping("/api/v1/reports/summary")
    public FinancialReport summary(@RequestHeader(value = "Authorization", required = false) String token) {
        return reportFor(token);
    }

    @GetMapping("/api/v1/reports/movements")
    public List<Movement> movements(@RequestHeader(value = "Authorization", required = false) String token) {
        return reportFor(token).movements();
    }

    @GetMapping("/api/v1/reports/income")
    public MovementReport income(@RequestHeader(value = "Authorization", required = false) String token) {
        return reportFor(token).income();
    }

    @GetMapping("/api/v1/reports/expenses")
    public MovementReport expenses(@RequestHeader(value = "Authorization", required = false) String token) {
        return reportFor(token).expenses();
    }

    @GetMapping("/api/v1/reports/contributions")
    public MovementReport contributions(@RequestHeader(value = "Authorization", required = false) String token) {
        return reportFor(token).contributions();
    }

    @GetMapping("/api/v1/reports/export")
    public ResponseEntity<String> export(@RequestHeader(value = "Authorization", required = false) String token) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=smartvault-report.csv")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ReportCsvWriter.write(reportFor(token)));
    }

    private FinancialReport reportFor(String token) {
        String actor = identity.actor(token);
        try {
            return reports.summary(actor);
        } catch (ReportSourceUnavailable ex) {
            throw new BusinessException(503, "Ledger no disponible. Intenta nuevamente.");
        }
    }
}
