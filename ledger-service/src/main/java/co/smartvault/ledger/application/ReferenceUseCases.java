package co.smartvault.ledger.application;

import java.math.*;
import java.time.*;
import java.util.*;

public final class ReferenceUseCases {
    private final ReferencePorts.ExchangeRates rates;
    private final ReferencePorts.Holidays holidays;
    public record Quote(String base,String quote,String reference_rate,String amount,String converted,String provider) {
    }
    public record NextBusinessDay(String country,String after,String next_business_day,String provider) {
    }
    public ReferenceUseCases(ReferencePorts.ExchangeRates rates, ReferencePorts.Holidays holidays) {
        this.rates=rates;
        this.holidays=holidays;
    }
    public Quote quote(String base,String quote,BigDecimal amount) {
        String from=iso(base,3),to=iso(quote,3);
        if(amount==null||amount.signum()<=0||amount.compareTo(new BigDecimal("1000000"))>0)throw new IllegalArgumentException("Monto fuera de rango");
        BigDecimal rate=from.equals(to)?BigDecimal.ONE:rates.rate(from,to);
        return new Quote(from,to,rate.toPlainString(),amount.toPlainString(),rate.multiply(amount).setScale(2,RoundingMode.HALF_UP).toPlainString(),"Frankfurter (referencia, no negociable)");
    }
    public NextBusinessDay nextDay(String country,LocalDate after) {
        String code=iso(country,2);
        if(after.getYear()<2020 || after.getYear()>2035)throw new IllegalArgumentException("Año no soportado");
        int year=after.getYear();
        Set<LocalDate> blocked=holidays.nationalDays(code,year);
        LocalDate current=after.plusDays(1);
        for(int i=0;i<370;i++,current=current.plusDays(1)) {
            if(current.getYear()!=year) {
                year=current.getYear();
                blocked=holidays.nationalDays(code,year);
            }
            if(current.getDayOfWeek()!=DayOfWeek.SATURDAY && current.getDayOfWeek()!=DayOfWeek.SUNDAY && !blocked.contains(current))
            return new NextBusinessDay(code,after.toString(),current.toString(),"Nager.Date (festivos nacionales)");
        }
        throw new IllegalStateException("No se encontró día hábil");
    }
    static String iso(String value,int size) {
        if(value==null||!value.matches("[a-zA-Z]{"+size+"}"))throw new IllegalArgumentException("Código ISO inválido");
        return value.toUpperCase(Locale.ROOT);
    }
}
