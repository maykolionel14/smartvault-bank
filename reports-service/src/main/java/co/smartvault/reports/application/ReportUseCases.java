package co.smartvault.reports.application;

import co.smartvault.reports.domain.FinancialReport;
import java.util.Objects;

public final class ReportUseCases {
    private final MovementPort movements;

    public ReportUseCases(MovementPort movements) {
        this.movements = Objects.requireNonNull(movements);
    }

    public FinancialReport summary(String actor) {
        if (actor == null || actor.isBlank()) throw new IllegalArgumentException("Usuario obligatorio");
        return FinancialReport.from(movements.recentForUser(actor));
    }
}
