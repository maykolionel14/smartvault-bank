package co.smartvault.ledger.application;

import java.time.Instant;
import java.util.*;

public interface LedgerPort {
    record Movement(String id, long amount_cents, String kind) {
    }
    record Entry(String transfer_id, long signed_cents, Instant created_at) {
    }
    record Integrity(boolean valid, int transfers, List<String> anomalous_transfer_ids) {
    }
    Movement credit(String wallet,long cents,String key);
    Movement transfer(String from,String to,long cents,String key,String kind);
    void openVault(String vaultId);
    long balance(String wallet);
    List<Entry> statement(String wallet);
    Integrity integrity();
}
