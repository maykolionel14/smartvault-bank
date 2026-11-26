package co.smartvault.vaults.application;

import co.smartvault.vaults.domain.Vault;
import java.util.*;

public final class VaultPorts {
    private VaultPorts() {
    }
    public interface VaultStore {
        Optional<Vault> byId(String id);
        List<Vault> forMember(String actor);
        List<Vault> all();
        void save(Vault vault);
    }
    public interface LedgerGateway {
        void open(String vaultId);
        String contribute(String actor,String vault,long cents,String key);
        String payout(String vault,String beneficiary,String proposal,long cents);
        long balance(String vaultId);
    }
    public interface Directory {
        Set<String> existing(List<String> ids);
    }
}
