package co.smartvault.vaults.infrastructure;

import co.smartvault.vaults.application.*;
import co.smartvault.vaults.domain.Vault;
import co.smartvault.platform.RemoteIdentityGuard;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class VaultsController {
    private final VaultUseCases use;
    private final RemoteIdentityGuard auth;
    public VaultsController(VaultUseCases use,RemoteIdentityGuard auth){
        this.use=use;
        this.auth=auth;
    }
    public record CreateVault(String name,List<String> member_ids,int quorum,List<String> milestones) {
    }
    public record CreateResult(String vault_id,int quorum,List<Vault.Milestone> milestones) {
    }
    public record Contribution(long cents,String idempotency_key) {
    }
    public record ProposalInput(String milestone_id,String beneficiary_id,long amount_cents,String purpose) {
    }
    public record Evidence(String note) {
    }
    public record ListItem(String id,String name,int quorum,List<String> members) {
    }
    private String actor(String header){
        return auth.actor(header);
    }
    @GetMapping("/health") public Map<String,String> health(){
        return Map.of("status","ok","service","vaults");
    }
    @ResponseStatus(HttpStatus.CREATED) @PostMapping("/api/v1/vaults")
    public CreateResult create(@RequestHeader(value="Authorization",required=false)String authHeader,@RequestBody CreateVault body) {
        var v=use.create(actor(authHeader),body.name(),body.member_ids(),body.quorum(),body.milestones());
        return new CreateResult(v.id(),v.quorum(),v.milestones());
    }
    @GetMapping("/api/v1/vaults") public List<ListItem> list(@RequestHeader(value="Authorization",required=false)String header) {
        return use.list(actor(header)).stream().map(v->new ListItem(v.id(),v.name(),v.quorum(),v.members())).toList();
    }
    @GetMapping("/api/v1/vaults/{id}") public VaultUseCases.VaultView details(@RequestHeader(value="Authorization",required=false)String header,@PathVariable String id) {
        return use.details(id,actor(header));
    }
    @PostMapping("/api/v1/vaults/{id}/contribute") public VaultUseCases.ContributionResult contribution(@RequestHeader(value="Authorization",required=false)String header,@PathVariable String id,@RequestBody Contribution body) {
        return use.contribute(id,actor(header),body.cents(),body.idempotency_key());
    }
    @ResponseStatus(HttpStatus.CREATED) @PostMapping("/api/v1/vaults/{id}/proposals")
    public Vault.ProposalStatus propose(@RequestHeader(value="Authorization",required=false)String header,@PathVariable String id,@RequestBody ProposalInput body) {
        return use.propose(id,actor(header),body.milestone_id(),body.beneficiary_id(),body.amount_cents(),body.purpose());
    }
    @PostMapping("/api/v1/vaults/milestones/{id}/attest") public VaultUseCases.AttestationResult attest(@RequestHeader(value="Authorization",required=false)String header,@PathVariable String id,@RequestBody Evidence body) {
        return use.attest(actor(header),id,body.note());
    }
    @PostMapping("/api/v1/vaults/proposals/{id}/approve") public Vault.ProposalStatus approve(@RequestHeader(value="Authorization",required=false)String header,@PathVariable String id) {
        return use.vote(actor(header),id,"approve");
    }
    @PostMapping("/api/v1/vaults/proposals/{id}/reject") public Vault.ProposalStatus reject(@RequestHeader(value="Authorization",required=false)String header,@PathVariable String id) {
        return use.vote(actor(header),id,"reject");
    }
    @GetMapping("/api/v1/vaults/proposals/{id}/status") public Vault.ProposalStatus status(@RequestHeader(value="Authorization",required=false)String header,@PathVariable String id) {
        return use.status(actor(header),id);
    }
    @PostMapping("/api/v1/vaults/proposals/{id}/execute") public VaultUseCases.ExecutionResult execute(@RequestHeader(value="Authorization",required=false)String header,@PathVariable String id) {
        return use.execute(actor(header),id);
    }
    @GetMapping("/api/v1/vaults/{id}/verify-audit") public Vault.AuditReport audit(@RequestHeader(value="Authorization",required=false)String header,@PathVariable String id) {
        return use.verifyAudit(actor(header),id);
    }
}
