package co.smartvault.reports.domain;

import java.util.HashSet;
import java.util.List;

/** Totals for the selected recent movements, not the lifetime wallet balance. */
public record FinancialReport(long income_cents, long expense_cents, long contribution_cents,
                              long net_cents, int movement_count, int limit,
                              String currency, List<Movement> movements) {
    public static final int LIMIT = 100;

    public FinancialReport {
        movements = List.copyOf(movements);
    }

    public static FinancialReport from(List<Movement> movements) {
        if (movements.size() > LIMIT) throw new IllegalArgumentException("Límite de movimientos excedido");
        long income = 0, expense = 0, contribution = 0;
        var ids = new HashSet<String>();
        for (Movement movement : movements) {
            if (!ids.add(movement.transfer_id())) throw new IllegalArgumentException("Movimiento duplicado");
            long cents = movement.signed_cents();
            if (movement.isIncome()) income = Math.addExact(income, cents);
            else expense = Math.addExact(expense, Math.negateExact(cents));
            if (movement.isContribution()) contribution = Math.addExact(contribution, Math.negateExact(cents));
        }
        return new FinancialReport(income, expense, contribution, Math.subtractExact(income, expense),
                movements.size(), LIMIT, "COP", movements);
    }

    public MovementReport income() {
        var entries = movements.stream().filter(Movement::isIncome).toList();
        return new MovementReport("income", income_cents, entries.size(), currency, limit, entries);
    }

    public MovementReport expenses() {
        var entries = movements.stream().filter(movement -> !movement.isIncome()).toList();
        return new MovementReport("expenses", expense_cents, entries.size(), currency, limit, entries);
    }

    public MovementReport contributions() {
        var entries = movements.stream().filter(Movement::isContribution).toList();
        return new MovementReport("contributions", contribution_cents, entries.size(), currency, limit, entries);
    }
}
