package co.smartvault.platform;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.Map;

public class RemoteIdentityGuard {
    private final SignedTokens tokens;
    private final RestClient client;
    private final String serviceKey;
    public RemoteIdentityGuard(SignedTokens tokens, String verifyUrl, String serviceKey) {
        this.tokens = tokens;
        this.serviceKey = serviceKey;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(350));
        factory.setReadTimeout(Duration.ofMillis(700));
        this.client = RestClient.builder().requestFactory(factory).baseUrl(verifyUrl).build();
    }
    public String actor(String authorization) {
        String token = SignedTokens.bearer(authorization);
        var claims = tokens.read(token);
        try {
            @SuppressWarnings("unchecked") Map<String,Object> response = client.post().uri("/internal/identity/verify")
            .header("X-Service-Key", serviceKey).body(Map.of("token", token)).retrieve().body(Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("active"))) throw BusinessException.unauthorized("Sesión revocada");
            return claims.subject();
        }
        catch (BusinessException ex) {
            throw ex;
        }
        catch (RestClientException ex) {
            throw new BusinessException(503, "Servicio de identidad no disponible");
        }
    }
}
