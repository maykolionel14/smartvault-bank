package co.smartvault.reports.application;

public final class ReportSourceUnavailable extends RuntimeException {
    public ReportSourceUnavailable(Throwable cause) {
        super("Ledger no disponible para generar el reporte", cause);
    }
}
