package co.smartvault.vaults.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class AuditHash {
    private AuditHash() {
    }
    public static String sha256(String previous, int sequence, String action, String payload) {
        try {
            String canonical=previous+"\n"+sequence+"\n"+action+"\n"+payload;
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        }
        catch(NoSuchAlgorithmException ex){
            throw new IllegalStateException("SHA-256 no disponible",ex);
        }
    }
}
