package co.smartvault.ledger.domain;

public record Money(long cents) {
    public Money {
        if (cents<=0) throw new IllegalArgumentException("El monto debe ser positivo");
    }
    public static Money demo(long cents) {
        if (cents>10_000_000L) throw new IllegalArgumentException("Crédito demo máximo de 10.000.000 centavos");
        return new Money(cents);
    }
}
