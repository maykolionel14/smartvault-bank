package co.smartvault.vaults.infrastructure;

import co.smartvault.vaults.application.VaultPorts;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.util.*;

@Component
public class InternalServicesAdapter implements VaultPorts.LedgerGateway,VaultPorts.Directory {
    private final RestClient ledger,identity;
    private final String serviceKey;
    public InternalServicesAdapter(@Value("${app.ledger-url}")String ledgerUrl,@Value("${app.identity-url}")String identityUrl,@Value("${app.service-key}")String serviceKey) {
        this.serviceKey=serviceKey;
        var factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(500));
        factory.setReadTimeout(Duration.ofSeconds(3));
        ledger=RestClient.builder().requestFactory(factory).baseUrl(ledgerUrl).defaultHeader("X-Service-Key",serviceKey).build();
        identity=RestClient.builder().requestFactory(factory).baseUrl(identityUrl).defaultHeader("X-Service-Key",serviceKey).build();
    }
    @SuppressWarnings("unchecked") private Map<String,Object> post(String path,Object body) {
        try {
            var response=ledger.post().uri(path).body(body).retrieve().body(Map.class);
            return response==null?Map.of():response;
        }
        catch(RestClientException ex){
            if(ex instanceof org.springframework.web.client.HttpClientErrorException.Conflict)
                throw new IllegalStateException("La operación simulada no pudo completarse. Comprueba el saldo y la referencia.");
            throw new IllegalStateException("Servicio Ledger no disponible");
        }
    }
    public void open(String id){
        post("/internal/ledger/open-vault",Map.of("vault_id",id));
    }
    public String contribute(String actor,String vault,long cents,String key) {
        var result=post("/internal/ledger/contribute",Map.of("actor_id",actor,"vault_id",vault,"cents",cents,"idempotency_key",key));
        return result.get("transfer_id").toString();
    }
    public String payout(String vault,String beneficiary,String proposal,long cents) {
        var result=post("/internal/ledger/payout",Map.of("vault_id",vault,"beneficiary_id",beneficiary,"proposal_id",proposal,"cents",cents));
        return result.get("transfer_id").toString();
    }
    @SuppressWarnings("unchecked") public long balance(String vault) {
        try {
            var result=ledger.get().uri("/internal/ledger/vault-balance/{id}",vault).retrieve().body(Map.class);
            return ((Number)Objects.requireNonNull(result).get("balance_cents")).longValue();
        }
        catch(RestClientException ex){
            throw new IllegalStateException("No se pudo consultar saldo");
        }
    }
    @SuppressWarnings("unchecked") public Set<String> existing(List<String> ids) {
        try {
            var result=identity.post().uri("/internal/identity/known-users").body(ids).retrieve().body(Map.class);
            return result==null?Set.of():new HashSet<>(result.keySet());
        }
        catch(RestClientException ex){
            throw new IllegalStateException("Identidad no disponible");
        }
    }
}
