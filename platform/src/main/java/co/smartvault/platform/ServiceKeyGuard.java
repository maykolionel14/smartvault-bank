package co.smartvault.platform;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class ServiceKeyGuard {
    private final String expected;
    public ServiceKeyGuard(String expected) {
        if (expected == null || expected.length() < 32) throw new IllegalArgumentException("SERVICE_KEY requiere 32 caracteres");
        this.expected = expected;
    }
    public void require(String provided) {
        if (provided == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            throw BusinessException.forbidden("Solo comunicación interna");
        }
    }
}
