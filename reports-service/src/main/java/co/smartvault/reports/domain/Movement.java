package co.smartvault.reports.domain;

import java.time.Instant;
import java.util.Objects;

public record Movement(String transfer_id, long signed_cents, String kind, Instant created_at) {
    public Movement {
        Objects.requireNonNull(transfer_id);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(created_at);
        if (transfer_id.isBlank() || signed_cents == 0 || signed_cents == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Movimiento inválido");
        }
    }

    public boolean isIncome() { return signed_cents > 0; }
    public boolean isContribution() { return signed_cents < 0 && "contribution".equals(kind); }
}
