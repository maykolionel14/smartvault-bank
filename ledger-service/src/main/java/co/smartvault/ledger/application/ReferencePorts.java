package co.smartvault.ledger.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public final class ReferencePorts {
    private ReferencePorts() {
    }
    public interface ExchangeRates {
        BigDecimal rate(String base,String quote);
    }
    public interface Holidays {
        Set<LocalDate> nationalDays(String country,int year);
    }
}
