package co.smartvault.ledger.domain;

import java.util.List;

public final class PostingPolicy {
    private PostingPolicy() {
    }
    public static boolean balanced(List<Long> entries) {
        return entries != null && entries.size()==2 && entries.get(0)+entries.get(1)==0L;
    }
    public static void validateTransfer(String from,String to,Money amount,long balance) {
        if(from==null || to==null || from.equals(to)) throw new IllegalArgumentException("Origen y destino deben ser diferentes");
        if(balance<amount.cents()) throw new IllegalStateException("Fondos insuficientes");
    }
    public static void validIdempotencyKey(String key) {
        if(key==null || key.length()<8 || key.length()>120) throw new IllegalArgumentException("Clave de idempotencia de 8 a 120 caracteres");
    }
}
