package co.smartvault.ledger.config;

import co.smartvault.ledger.application.*;
import co.smartvault.ledger.infrastructure.ExternalReferenceAdapter;
import co.smartvault.platform.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.time.Clock;

@Configuration
public class LedgerConfig {
    @Bean SignedTokens tokens(@Value("${app.jwt-secret}")String secret){
        return new SignedTokens(secret,Clock.systemUTC());
    }
    @Bean ServiceKeyGuard keys(@Value("${app.service-key}")String secret){
        return new ServiceKeyGuard(secret);
    }
    @Bean RemoteIdentityGuard actorGuard(SignedTokens tokens,@Value("${app.identity-url}")String url,@Value("${app.service-key}")String key) {
        return new RemoteIdentityGuard(tokens,url,key);
    }
    @Bean LedgerUseCases ledger(LedgerPort port){
        return new LedgerUseCases(port);
    }
    @Bean ReferenceUseCases references(ExternalReferenceAdapter adapter){
        return new ReferenceUseCases(adapter,adapter);
    }
}
