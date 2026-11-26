package co.smartvault.ledger.infrastructure;

import co.smartvault.ledger.application.LedgerPort;
import co.smartvault.ledger.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Repository
public class LedgerJpaAdapter implements LedgerPort {
    private final WalletJpaRepository wallets;
    private final TransferJpaRepository transfers;
    private final EntryJpaRepository entries;
    public LedgerJpaAdapter(WalletJpaRepository wallets,TransferJpaRepository transfers,EntryJpaRepository entries) {
        this.wallets=wallets;
        this.transfers=transfers;
        this.entries=entries;
    }
    private void ensure(String id) {
        wallets.ensureWallet(id);
    }
    private static Movement response(TransferRow row) {
        return new Movement(row.id,row.amountCents,row.kind);
    }
    private Movement previous(String key,String source,String target,long cents,String kind) {
        var old=transfers.findByIdempotencyKey(key).orElse(null);
        if(old==null) return null;
        if(!old.source.equals(source)||!old.target.equals(target)||old.amountCents!=cents||!old.kind.equals(kind)) throw new IllegalStateException("Idempotencia reutilizada con datos distintos");
        return response(old);
    }
    private Movement saveMovement(String key,String source,String target,long cents,String kind) {
        var transfer=transfers.saveAndFlush(new TransferRow(UUID.randomUUID().toString(),key,source,target,cents,kind));
        entries.saveAll(List.of(new EntryRow(transfer.id,source,-cents),new EntryRow(transfer.id,target,cents)));
        return response(transfer);
    }
    @Transactional
    public Movement credit(String wallet,long cents,String key) {
        ensure("DEMO_MINT");
        ensure(wallet);
        var rows=wallets.lockMany(List.of("DEMO_MINT",wallet));
        var old=previous(key,"DEMO_MINT",wallet,cents,"demo-credit");
        if(old!=null) return old;
        var found=new HashMap<String,WalletRow>();
        for(var row:rows) found.put(row.id,row);
        found.get("DEMO_MINT").balanceCents-=cents;
        found.get(wallet).balanceCents+=cents;
        return saveMovement(key,"DEMO_MINT",wallet,cents,"demo-credit");
    }
    @Transactional
    public Movement transfer(String source,String target,long cents,String key,String kind) {
        if(target.startsWith("user:")) ensure(target);
        var found=new HashMap<String,WalletRow>();
        for(var wallet:wallets.lockMany(List.of(source,target)))found.put(wallet.id,wallet);
        var old=previous(key,source,target,cents,kind);
        if(old!=null)return old;
        if(!found.containsKey(source)) throw new NoSuchElementException("Billetera origen inexistente");
        if(!found.containsKey(target)) throw new NoSuchElementException("Billetera destino inexistente");
        PostingPolicy.validateTransfer(source,target,new Money(cents),found.get(source).balanceCents);
        found.get(source).balanceCents-=cents;
        found.get(target).balanceCents+=cents;
        return saveMovement(key,source,target,cents,kind);
    }
    @Transactional public void openVault(String vaultId) {
        ensure("vault:"+vaultId);
    }
    @Transactional(readOnly=true) public long balance(String wallet) {
        return wallets.findById(wallet).map(x->x.balanceCents).orElse(0L);
    }
    @Transactional(readOnly=true) public List<Entry> statement(String wallet) {
        return entries.findTop100ByWalletIdOrderByIdDesc(wallet).stream().map(e->new Entry(e.transferId,e.signedCents,e.createdAt)).toList();
    }
    @Transactional(readOnly=true) public Integrity integrity() {
        var bad=new ArrayList<String>();
        for(var transfer:transfers.findAll()) {
            var e=entries.findByTransferId(transfer.id);
            if(!PostingPolicy.balanced(e.stream().map(x->x.signedCents).toList()) || e.stream().noneMatch(x->x.walletId.equals(transfer.source)&&x.signedCents==-transfer.amountCents)
            || e.stream().noneMatch(x->x.walletId.equals(transfer.target)&&x.signedCents==transfer.amountCents))bad.add(transfer.id);
        }
        Map<String,Long> sum=new HashMap<>();
        for(var entry:entries.findAll()) sum.merge(entry.walletId,entry.signedCents,Long::sum);
        for(var wallet:wallets.findAll()) if(wallet.balanceCents!=sum.getOrDefault(wallet.id,0L))bad.add("wallet:"+wallet.id);
        return new Integrity(bad.isEmpty(),(int)transfers.count(),bad);
    }
}
