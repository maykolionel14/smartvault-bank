package co.smartvault.identity.application;

public interface PasswordPort {
    String hash(String raw);
    boolean matches(String raw, String hash);
}
