package co.smartvault.identity.application;

public interface TokenPort {
    String issue(String userId, int version);
}
