package co.smartvault.ledger.infrastructure;

import co.smartvault.ledger.application.*;
import co.smartvault.platform.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
public class LedgerController {
    private final LedgerUseCases service;
    private final ReferenceUseCases references;
    private final RemoteIdentityGuard actors;
    private final ServiceKeyGuard internalKey;
    public LedgerController(LedgerUseCases service,ReferenceUseCases references,RemoteIdentityGuard actors,ServiceKeyGuard internalKey) {
        this.service=service;
        this.references=references;
        this.actors=actors;
        this.internalKey=internalKey;
    }
    public record Amount(long cents,String idempotency_key) {
    }
    public record VaultContribution(String actor_id,String vault_id,long cents,String idempotency_key) {
    }
    public record Payout(String vault_id,String beneficiary_id,String proposal_id,long cents) {
    }
    public record VaultRef(String vault_id) {
    }
    private String actor(String authorization){
        return actors.actor(authorization);
    }
    @GetMapping("/health") public Map<String,String> health(){
        return Map.of("status","ok","service","ledger");
    }
    @PostMapping("/api/v1/ledger/demo-credit") public Map<String,Object> credit(@RequestHeader(value="Authorization",required=false)String auth,@RequestBody Amount body) {
        String userId=actor(auth);
        var transfer=service.credit(userId,body.cents(),body.idempotency_key());
        return Map.of("transfer_id",transfer.id(),"simulated_cents",transfer.amount_cents(),"kind",transfer.kind());
    }
    @GetMapping("/api/v1/ledger/balance") public Map<String,Object> balance(@RequestHeader(value="Authorization",required=false)String auth) {
        String user=actor(auth);
        return Map.of("wallet_id","user:"+user,"balance_cents",service.userBalance(user));
    }
    @GetMapping("/api/v1/ledger/statement") public Map<String,Object> statement(@RequestHeader(value="Authorization",required=false)String auth) {
        String user=actor(auth);
        return Map.of("wallet_id","user:"+user,"entries",service.statement(user));
    }
    @GetMapping("/api/v1/ledger/verify-integrity") public LedgerPort.Integrity integrity(@RequestHeader(value="Authorization",required=false)String auth) {
        actor(auth);
        return service.integrity();
    }
    @GetMapping("/api/v1/ledger/fx/quote") public ReferenceUseCases.Quote fx(@RequestHeader(value="Authorization",required=false)String auth,
    @RequestParam(defaultValue="USD")String base,@RequestParam(defaultValue="COP")String quote,@RequestParam(defaultValue="1")BigDecimal amount) {
        actor(auth);
        return references.quote(base,quote,amount);
    }
    @GetMapping("/api/v1/ledger/calendar/next-business-day") public ReferenceUseCases.NextBusinessDay calendar(@RequestHeader(value="Authorization",required=false)String auth,
    @RequestParam(defaultValue="CO")String country,@RequestParam(required=false)LocalDate on) {
        actor(auth);
        return references.nextDay(country,on==null?LocalDate.now():on);
    }
    @PostMapping("/internal/ledger/open-vault") public Map<String,String> open(@RequestHeader(value="X-Service-Key",required=false)String key,@RequestBody VaultRef body) {
        internalKey.require(key);
        service.openVault(body.vault_id());
        return Map.of("wallet_id","vault:"+body.vault_id());
    }
    @GetMapping("/internal/ledger/vault-balance/{id}") public Map<String,Long> balanceVault(@RequestHeader(value="X-Service-Key",required=false)String key,@PathVariable String id) {
        internalKey.require(key);
        return Map.of("balance_cents",service.vaultBalance(id));
    }
    @PostMapping("/internal/ledger/contribute") public Map<String,Object> contribution(@RequestHeader(value="X-Service-Key",required=false)String key,@RequestBody VaultContribution body) {
        internalKey.require(key);
        var t=service.contribute(body.actor_id(),body.vault_id(),body.cents(),body.idempotency_key());
        return Map.of("transfer_id",t.id(),"amount_cents",t.amount_cents());
    }
    @PostMapping("/internal/ledger/payout") public Map<String,String> payout(@RequestHeader(value="X-Service-Key",required=false)String key,@RequestBody Payout body) {
        internalKey.require(key);
        var transfer=service.payout(body.vault_id(),body.beneficiary_id(),body.proposal_id(),body.cents());
        return Map.of("transfer_id",transfer.id());
    }
}
