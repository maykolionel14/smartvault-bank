package co.smartvault.identity.domain;

import java.util.Locale;
import java.util.Objects;

public record UserAccount(String id, String email, String passwordHash, boolean suspended, int sessionVersion, boolean activated) {
    public UserAccount(String id, String email, String passwordHash, boolean suspended, int sessionVersion) {
        this(id,email,passwordHash,suspended,sessionVersion,false);
    }
    public UserAccount {
        Objects.requireNonNull(id);
        Objects.requireNonNull(email);
        Objects.requireNonNull(passwordHash);
        if (id.isBlank() || passwordHash.isBlank()) throw new IllegalArgumentException("Usuario inválido");
        email = normalize(email);
        if (sessionVersion < 0) throw new IllegalArgumentException("Versión negativa");
    }
    public static String normalize(String email) {
        if (email == null) throw new IllegalArgumentException("Correo obligatorio");
        String clean = email.strip().toLowerCase(Locale.ROOT);
        if (!clean.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || clean.length() > 254) throw new IllegalArgumentException("Correo inválido");
        return clean;
    }
    public boolean canAct(int tokenVersion) {
        return activated && !suspended && tokenVersion == sessionVersion;
    }
    public boolean canResume(int tokenVersion) {
        return suspended && tokenVersion == sessionVersion;
    }
    public UserAccount freeze() {
        if (suspended) throw new IllegalStateException("Cuenta ya suspendida");
        return new UserAccount(id,email,passwordHash,true,sessionVersion,activated);
    }
    public UserAccount unfreeze() {
        if (!suspended) throw new IllegalStateException("Cuenta no suspendida");
        return new UserAccount(id,email,passwordHash,false,sessionVersion+1,activated);
    }
    public UserAccount revoke() {
        return new UserAccount(id,email,passwordHash,suspended,sessionVersion+1,activated);
    }
    public UserAccount withPassword(String hash) {
        return new UserAccount(id,email,hash,suspended,sessionVersion+1,activated);
    }
}
