package co.smartvault.reports.infrastructure;

import co.smartvault.reports.application.MovementPort;
import co.smartvault.reports.application.ReportSourceUnavailable;
import co.smartvault.reports.domain.Movement;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.time.Duration;
import java.util.List;

public final class LedgerMovementAdapter implements MovementPort {
    private final RestClient ledger;

    public LedgerMovementAdapter(String baseUrl, String key) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(500));
        factory.setReadTimeout(Duration.ofSeconds(3));
        ledger = RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
                .defaultHeader("X-Service-Key", key).build();
    }

    @Override
    public List<Movement> recentForUser(String actor) {
        try {
            var response = ledger.get().uri("/internal/ledger/report-movements/{userId}", actor)
                    .retrieve().body(new ParameterizedTypeReference<List<Movement>>() {});
            if (response == null) throw new ReportSourceUnavailable(null);
            return response;
        } catch (RestClientException ex) {
            throw new ReportSourceUnavailable(ex);
        }
    }
}
