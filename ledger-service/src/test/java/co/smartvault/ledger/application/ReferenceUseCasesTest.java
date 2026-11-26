package co.smartvault.ledger.application;

import org.junit.jupiter.api.Test;
import java.math.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReferenceUseCasesTest {
    ReferenceUseCases service=new ReferenceUseCases((from,to)->new BigDecimal("4000.123"),(country,year)->Set.of(LocalDate.of(2026,11,23)));
    @Test void quotes(){
        var quote=service.quote("usd","cop",new BigDecimal("2"));
        assertEquals("8000.25",quote.converted());
        assertEquals("1",service.quote("COP","COP",BigDecimal.ONE).converted().replace(".00",""));
        assertThrows(IllegalArgumentException.class,()->service.quote("US","COP",BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,()->service.quote("USD","COP",BigDecimal.ZERO));
    }
    @Test void nextBusinessDaySkipsHolidayAndWeekend(){
        assertEquals("2026-11-24",service.nextDay("co",LocalDate.of(2026,11,20)).next_business_day());
        assertThrows(IllegalArgumentException.class,()->service.nextDay("CO",LocalDate.of(2010,1,1)));
        assertThrows(IllegalArgumentException.class,()->service.nextDay("XXX",LocalDate.now()));
    }
}
