package co.smartvault.ledger.application;

import co.smartvault.ledger.domain.Money;
import co.smartvault.ledger.domain.PostingPolicy;

public final class LedgerUseCases {
    private final LedgerPort ledgerGateway;
    public LedgerUseCases(LedgerPort ledgerGateway) {
        this.ledgerGateway=ledgerGateway;
    }
    public LedgerPort.Movement credit(String user,long cents,String key) {
        Money.demo(cents);
        PostingPolicy.validIdempotencyKey(key);
        return ledgerGateway.credit("user:"+user,cents,"demo:"+user+":"+key);
    }
    public LedgerPort.Movement contribute(String user,String vault,long cents,String key) {
        new Money(cents);
        PostingPolicy.validIdempotencyKey(key);
        return ledgerGateway.transfer("user:"+user,"vault:"+vault,cents,"user:"+user+":"+key,"contribution");
    }
    public LedgerPort.Movement payout(String vault,String to,String proposal,long cents) {
        new Money(cents);
        return ledgerGateway.transfer("vault:"+vault,"user:"+to,cents,"approved-proposal:"+proposal,"governed-payout");
    }
    public void openVault(String vault) {
        if(vault==null||vault.isBlank())throw new IllegalArgumentException("Bóveda vacía");
        ledgerGateway.openVault(vault);
    }
    public long userBalance(String user) {
        return ledgerGateway.balance("user:"+user);
    }
    public long vaultBalance(String vault) {
        return ledgerGateway.balance("vault:"+vault);
    }
    public java.util.List<LedgerPort.Entry> statement(String user) {
        return ledgerGateway.statement("user:"+user);
    }
    public LedgerPort.Integrity integrity() {
        return ledgerGateway.integrity();
    }
}
