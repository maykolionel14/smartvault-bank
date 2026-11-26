package co.smartvault.ledger.infrastructure;

import co.smartvault.ledger.application.ReferencePorts;
import co.smartvault.platform.BusinessException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Component
public class ExternalReferenceAdapter implements ReferencePorts.ExchangeRates, ReferencePorts.Holidays {
    private final RestClient frankfurter;
    private final RestClient nager;
    private final Cache<String,BigDecimal> rates = Caffeine.newBuilder()
            .maximumSize(256).expireAfterWrite(Duration.ofMinutes(15)).build();
    private final Cache<String,Set<LocalDate>> calendars = Caffeine.newBuilder()
            .maximumSize(128).expireAfterWrite(Duration.ofHours(24)).build();

    public ExternalReferenceAdapter() {
        var factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        frankfurter=RestClient.builder().requestFactory(factory).baseUrl("https://api.frankfurter.dev").build();
        nager=RestClient.builder().requestFactory(factory).baseUrl("https://nagerholidays.com").build();
    }

    public record Rate(String base,String quote,BigDecimal rate) {}
    public record Holiday(LocalDate date,String countryCode,Boolean nationalHoliday) {}

    public BigDecimal rate(String base,String quote) {
        return rates.get(base+"/"+quote, ignored -> {
            try {
                var response=frankfurter.get().uri("/v2/rate/{base}/{quote}",base,quote).retrieve().body(Rate.class);
                if(response==null || !base.equals(response.base()) || !quote.equals(response.quote())
                        || response.rate()==null || response.rate().signum()<=0) throw new IllegalStateException();
                return response.rate();
            } catch(RuntimeException ex) {
                throw new BusinessException(503,"El proveedor de divisas no devolvió una cotización válida");
            }
        });
    }

    public Set<LocalDate> nationalDays(String country,int year) {
        return calendars.get(country+"/"+year, ignored -> {
            try {
                var response=nager.get().uri("/api/v4/Holidays/{country}/{year}",country,year)
                        .retrieve().body(Holiday[].class);
                if(response==null) throw new IllegalStateException();
                var dates=new HashSet<LocalDate>();
                for(var holiday:response) {
                    if(!country.equals(holiday.countryCode()) || holiday.date()==null
                            || holiday.date().getYear()!=year || holiday.nationalHoliday()==null)
                        throw new IllegalStateException();
                    if(holiday.nationalHoliday()) dates.add(holiday.date());
                }
                return Set.copyOf(dates);
            } catch(RuntimeException ex) {
                throw new BusinessException(503,"El proveedor de festivos no devolvió un calendario válido");
            }
        });
    }
}
