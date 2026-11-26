package co.smartvault.reports.domain;

import java.util.List;

/** A filtered section of the same last-100-movement snapshot. */
public record MovementReport(String type, long total_cents, int movement_count, String currency,
                             int limit, List<Movement> movements) {
    public MovementReport {
        movements = List.copyOf(movements);
    }
}
