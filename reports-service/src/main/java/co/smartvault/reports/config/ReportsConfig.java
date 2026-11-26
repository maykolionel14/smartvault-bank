package co.smartvault.reports.config;

import co.smartvault.platform.RemoteIdentityGuard;
import co.smartvault.platform.SignedTokens;
import co.smartvault.reports.application.MovementPort;
import co.smartvault.reports.application.ReportUseCases;
import co.smartvault.reports.infrastructure.LedgerMovementAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class ReportsConfig {
    @Bean
    RemoteIdentityGuard identity(@Value("${app.jwt-secret}") String secret,
                                 @Value("${app.identity-url}") String identityUrl,
                                 @Value("${app.service-key}") String key) {
        return new RemoteIdentityGuard(new SignedTokens(secret, Clock.systemUTC()), identityUrl, key);
    }

    @Bean
    MovementPort movements(@Value("${app.ledger-url}") String ledgerUrl,
                           @Value("${app.service-key}") String key) {
        return new LedgerMovementAdapter(ledgerUrl, key);
    }

    @Bean
    ReportUseCases reports(MovementPort movements) {
        return new ReportUseCases(movements);
    }
}
