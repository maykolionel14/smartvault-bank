package co.smartvault.platform;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public final class SignedTokens {
    private final byte[] secret;
    private final Clock clock;
    public record Claims(String subject, int version) {
    }
    public SignedTokens(String secret, Clock clock) {
        if (secret == null || secret.length() < 32) throw new IllegalArgumentException("JWT_SECRET necesita 32 caracteres o más");
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = Objects.requireNonNull(clock);
    }
    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar token", ex);
        }
    }
    public String issue(String subject, int version) {
        if (subject == null || !subject.matches("[a-f0-9-]{36}") || version < 0) throw BusinessException.invalid("Identidad inválida");
        String data = subject + "." + version + "." + Instant.now(clock).plusSeconds(1200).getEpochSecond();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes(StandardCharsets.UTF_8)) + "." + sign(data);
    }
    public Claims read(String token) {
        try {
            String[] raw = token.split("\\.", -1);
            if (raw.length != 2) throw new IllegalArgumentException();
            String data = new String(Base64.getUrlDecoder().decode(raw[0]), StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(sign(data).getBytes(StandardCharsets.US_ASCII), raw[1].getBytes(StandardCharsets.US_ASCII))) throw new IllegalArgumentException();
            String[] fields = data.split("\\.", -1);
            if (fields.length != 3 || !fields[0].matches("[a-f0-9-]{36}")) throw new IllegalArgumentException();
            int version = Integer.parseInt(fields[1]);
            long expiry = Long.parseLong(fields[2]);
            if (expiry <= Instant.now(clock).getEpochSecond() || version < 0) throw new IllegalArgumentException();
            return new Claims(fields[0], version);
        }
        catch (Exception ex) {
            throw BusinessException.unauthorized("Sesión inválida o vencida");
        }
    }
    public static String bearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw BusinessException.unauthorized("Se requiere Bearer token");
        return authorization.substring(7);
    }
}
