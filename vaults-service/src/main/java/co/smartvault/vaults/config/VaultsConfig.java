package co.smartvault.vaults.config;

import co.smartvault.platform.*;
import co.smartvault.vaults.application.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.time.Clock;
import java.util.UUID;

@Configuration
public class VaultsConfig {
    @Bean SignedTokens tokens(@Value("${app.jwt-secret}")String secret){
        return new SignedTokens(secret,Clock.systemUTC());
    }
    @Bean ServiceKeyGuard keys(@Value("${app.service-key}")String secret){
        return new ServiceKeyGuard(secret);
    }
    @Bean RemoteIdentityGuard actorGuard(SignedTokens tokens,@Value("${app.identity-url}")String url,@Value("${app.service-key}")String key){
        return new RemoteIdentityGuard(tokens,url,key);
    }
    @Bean VaultUseCases vaults(VaultPorts.VaultStore store,VaultPorts.LedgerGateway ledger,VaultPorts.Directory directory){
        return new VaultUseCases(store,ledger,directory,()->UUID.randomUUID().toString());
    }
}
