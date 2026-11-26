package co.smartvault.reports.infrastructure;

import co.smartvault.reports.domain.FinancialReport;

/** CSV is a presentation concern, not a financial calculation. */
public final class ReportCsvWriter {
    private ReportCsvWriter() {}

    public static String write(FinancialReport report) {
        StringBuilder csv = new StringBuilder("transfer_id,created_at,kind,signed_cents,currency\r\n");
        for (var movement : report.movements()) {
            csv.append(cell(movement.transfer_id())).append(',')
                    .append(cell(movement.created_at().toString())).append(',')
                    .append(cell(movement.kind())).append(',')
                    .append(movement.signed_cents()).append(',')
                    .append(report.currency()).append("\r\n");
        }
        return csv.toString();
    }

    private static String cell(String value) {
        // Prevent spreadsheet formula evaluation if an upstream string ever changes.
        if (!value.isEmpty() && "=+@-\t\r\n".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
