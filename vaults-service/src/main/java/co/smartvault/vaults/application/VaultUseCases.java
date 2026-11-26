package co.smartvault.vaults.application;

import co.smartvault.vaults.domain.Vault;
import java.util.*;
import java.util.function.Supplier;

public final class VaultUseCases {
    private final VaultPorts.VaultStore vaultRepository;
    private final VaultPorts.LedgerGateway ledgerGateway;
    private final VaultPorts.Directory userDirectory;
    private final Supplier<String> idGenerator;
    public record VaultView(String id,String name,List<String> members,int quorum,long balance_cents,
    List<Vault.Milestone> milestones,List<Vault.ProposalStatus> proposals) {
    }
    public record ContributionResult(String transfer_id,long amount_cents) {
    }
    public record ExecutionResult(String status,String transfer_id,boolean idempotent) {
    }
    public record AttestationResult(int attestations,int quorum,boolean ready) {
    }
    public VaultUseCases(VaultPorts.VaultStore vaultRepository,VaultPorts.LedgerGateway ledgerGateway,VaultPorts.Directory userDirectory,Supplier<String> idGenerator) {
        this.vaultRepository=vaultRepository;
        this.ledgerGateway=ledgerGateway;
        this.userDirectory=userDirectory;
        this.idGenerator=idGenerator;
    }
    public Vault get(String id,String actor) {
        Vault vault=vaultRepository.byId(id).orElseThrow(()->new NoSuchElementException("Bóveda inexistente"));
        vault.requireMember(actor);
        return vault;
    }
    public Vault create(String actor,String name,List<String> members,int quorum,List<String> titles) {
        if(!userDirectory.existing(members).containsAll(members))throw new IllegalArgumentException("Hay miembros no registrados");
        var vault=Vault.create(idGenerator.get(),name,actor,members,quorum,titles,idGenerator);
        ledgerGateway.open(vault.id());
        vaultRepository.save(vault);
        return vault;
    }
    public List<Vault> list(String actor){
        return vaultRepository.forMember(actor);
    }
    public VaultView details(String id,String actor){
        var vault=get(id,actor);
        return new VaultView(vault.id(),vault.name(),vault.members(),vault.quorum(),ledgerGateway.balance(vault.id()),vault.milestones(),
        vault.proposals().stream().map(proposal->vault.status(actor,proposal.id())).toList());
    }
    public ContributionResult contribute(String vaultId,String actor,long cents,String key) {
        var vault=get(vaultId,actor);
        if(cents<=0 || key==null || key.length()<8 || key.length()>120)throw new IllegalArgumentException("Aporte o idempotencia inválidos");
        String transfer=ledgerGateway.contribute(actor,vault.id(),cents,key);
        if(!vault.hasTransfer(transfer))vaultRepository.save(vault.contribution(actor,transfer,cents));
        return new ContributionResult(transfer,cents);
    }
    public Vault.ProposalStatus propose(String vaultId,String actor,String milestone,String beneficiary,long cents,String purpose) {
        var vault=get(vaultId,actor);
        if(!userDirectory.existing(List.of(beneficiary)).contains(beneficiary))throw new IllegalArgumentException("Beneficiario no registrado");
        String id=idGenerator.get();
        var updatedVault=vault.propose(actor,id,milestone,beneficiary,cents,purpose);
        vaultRepository.save(updatedVault);
        return updatedVault.status(actor,id);
    }
    public AttestationResult attest(String actor,String milestoneId,String note) {
        var vault=findByMilestone(actor,milestoneId);
        var updatedVault=vault.attest(actor,milestoneId,note);
        vaultRepository.save(updatedVault);
        var count=updatedVault.milestones().stream().filter(m->m.id().equals(milestoneId)).findFirst().orElseThrow().attestations().size();
        return new AttestationResult(count,vault.quorum(),count>=vault.quorum());
    }
    public Vault.ProposalStatus vote(String actor,String proposalId,String decision) {
        var vault=findByProposal(actor,proposalId);
        var updatedVault=vault.vote(actor,proposalId,decision);
        vaultRepository.save(updatedVault);
        return updatedVault.status(actor,proposalId);
    }
    public Vault.ProposalStatus status(String actor,String proposalId){
        return findByProposal(actor,proposalId).status(actor,proposalId);
    }
    public ExecutionResult execute(String actor,String proposalId) {
        var vault=findByProposal(actor,proposalId);
        var proposal=vault.proposal(actor,proposalId);
        if(proposal.status().equals("executed"))return new ExecutionResult("executed",proposal.payoutId(),true);
        if(!vault.status(actor,proposalId).releasable())throw new IllegalStateException("No cumplen los votos y las validaciones");
        String transfer=ledgerGateway.payout(vault.id(),proposal.beneficiaryId(),proposal.id(),proposal.amountCents());
        vaultRepository.save(vault.executed(actor,proposal.id(),transfer));
        return new ExecutionResult("executed",transfer,false);
    }
    public Vault.AuditReport verifyAudit(String actor,String vaultId){
        return get(vaultId,actor).verifyAudit(actor);
    }
    private Vault findByMilestone(String actor,String id) {
        return vaultRepository.forMember(actor).stream().filter(vault->vault.milestones().stream().anyMatch(m->m.id().equals(id))).findFirst().orElseThrow(()->new NoSuchElementException("Hito inexistente"));
    }
    private Vault findByProposal(String actor,String id) {
        return vaultRepository.forMember(actor).stream().filter(vault->vault.proposals().stream().anyMatch(proposal->proposal.id().equals(id))).findFirst().orElseThrow(()->new NoSuchElementException("Propuesta inexistente"));
    }
}
