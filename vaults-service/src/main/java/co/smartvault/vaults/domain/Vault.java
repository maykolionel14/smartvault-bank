package co.smartvault.vaults.domain;

import java.util.*;
import java.util.function.Supplier;

public record Vault(String id,String name,String creatorId,List<String> members,int quorum,List<Milestone> milestones,
List<Proposal> proposals,List<AuditEvent> audit) {
    public record Milestone(String id,String title,Set<String> attestations) {
        public Milestone {
            attestations=Set.copyOf(attestations);
        }
    }
    public record Proposal(String id,String milestoneId,String beneficiaryId,long amountCents,String purpose,String status,
    Map<String,String> votes,String payoutId) {
        public Proposal {
            votes=Map.copyOf(votes);
        }
    }
    public record AuditEvent(int seq,String action,String payload,String previousHash,String hash) {
    }
    public record ProposalStatus(String proposal_id,String vault_id,String purpose,String beneficiary_id,long amount_cents,String status,
    int approval_count,int rejection_count,int attestation_count,int quorum,boolean releasable) {
    }
    public record AuditReport(boolean valid,int event_count,List<Integer> anomalous_sequences,String note) {
    }
    public Vault {
        members=List.copyOf(members);
        milestones=List.copyOf(milestones);
        proposals=List.copyOf(proposals);
        audit=List.copyOf(audit);
        new Governance(members,quorum);
    }
    public static Vault create(String id,String name,String actor,List<String> memberIds,int quorum,List<String> titles,Supplier<String> newId) {
        if(id==null||id.isBlank()||name==null||name.isBlank()||name.length()<3||name.length()>100)throw new IllegalArgumentException("Nombre de bóveda inválido");
        var governance=new Governance(memberIds,quorum);
        governance.requireMember(actor);
        if(titles==null||titles.isEmpty()||titles.size()>20||titles.stream().anyMatch(t->t==null||t.isBlank()||t.length()>150))throw new IllegalArgumentException("Hitos inválidos");
        var ms=titles.stream().map(t->new Milestone(newId.get(),t,Set.of())).toList();
        return new Vault(id,name,actor,memberIds,quorum,ms,List.of(),List.of()).withEvent("vault.created",actor+";quorum="+quorum);
    }
    public Governance governance(){
        return new Governance(members,quorum);
    }
    public void requireMember(String actor){
        governance().requireMember(actor);
    }
    private Vault copy(List<Milestone> ms,List<Proposal> ps,List<AuditEvent> events){
        return new Vault(id,name,creatorId,members,quorum,ms,ps,events);
    }
    private Vault withEvent(String action,String payload) {
        var items=new ArrayList<>(audit);
        String previous=items.isEmpty()?"GENESIS":items.getLast().hash();
        int seq=items.size()+1;
        items.add(new AuditEvent(seq,action,payload,previous,AuditHash.sha256(previous,seq,action,payload)));
        return copy(milestones,proposals,items);
    }
    public Vault contribution(String actor,String transferId,long cents){
        requireMember(actor);
        if(cents<=0)throw new IllegalArgumentException("Aporte inválido");
        if(hasTransfer(transferId))return this;
        return withEvent("vault.contributed","actor="+actor+";transfer_id="+transferId+";cents="+cents);
    }
    public boolean hasTransfer(String transferId){
        return audit.stream().anyMatch(e->e.action().equals("vault.contributed")&&e.payload().contains(";transfer_id="+transferId+";"));
    }
    private Milestone findMilestone(String milestoneId){
        return milestones.stream().filter(m->m.id().equals(milestoneId)).findFirst().orElseThrow(()->new IllegalArgumentException("Hito no pertenece a la bóveda"));
    }
    private Proposal findProposal(String proposalId){
        return proposals.stream().filter(p->p.id().equals(proposalId)).findFirst().orElseThrow(()->new NoSuchElementException("Propuesta inexistente"));
    }
    public Vault attest(String actor,String milestoneId,String evidence) {
        requireMember(actor);
        if(evidence==null||evidence.strip().length()<5||evidence.length()>500)throw new IllegalArgumentException("Evidencia: 5 a 500 caracteres");
        var current=findMilestone(milestoneId);
        if(current.attestations().contains(actor))throw new IllegalStateException("Ya validó este hito");
        var evidenceSet=new HashSet<>(current.attestations());
        evidenceSet.add(actor);
        var replacement=new Milestone(current.id(),current.title(),evidenceSet);
        var next=new ArrayList<Milestone>();
        for(var x:milestones)next.add(x.id().equals(current.id())?replacement:x);
        return copy(next,proposals,audit).withEvent("milestone.attested","actor="+actor+";milestone="+milestoneId);
    }
    public Vault propose(String actor,String proposalId,String milestoneId,String beneficiary,long amount,String purpose) {
        requireMember(actor);
        findMilestone(milestoneId);
        if(proposalId==null||proposalId.isBlank()||beneficiary==null||beneficiary.isBlank()||amount<=0||purpose==null||purpose.strip().length()<5||purpose.length()>200)
        throw new IllegalArgumentException("Datos de propuesta inválidos");
        var next=new ArrayList<>(proposals);
        next.add(new Proposal(proposalId,milestoneId,beneficiary,amount,purpose,"pending",Map.of(),null));
        return copy(milestones,next,audit).withEvent("proposal.created","actor="+actor+";proposal="+proposalId);
    }
    public Vault vote(String actor,String proposalId,String decision){
        requireMember(actor);
        if(!decision.equals("approve")&&!decision.equals("reject"))throw new IllegalArgumentException("Voto inválido");
        var proposal=findProposal(proposalId);
        if(!proposal.status().equals("pending"))throw new IllegalStateException("Propuesta cerrada");
        if(proposal.votes().containsKey(actor))throw new IllegalStateException("Ya votó");
        var newVotes=new HashMap<>(proposal.votes());
        newVotes.put(actor,decision);
        var rejected=new HashSet<String>();
        newVotes.forEach((member,vote)->{
            if(vote.equals("reject"))rejected.add(member);
        }
        );
        String status=governance().rejected(rejected)?"rejected":"pending";
        var updated=new Proposal(proposal.id(),proposal.milestoneId(),proposal.beneficiaryId(),proposal.amountCents(),proposal.purpose(),status,newVotes,proposal.payoutId());
        var next=new ArrayList<Proposal>();
        for(var p:proposals)next.add(p.id().equals(proposalId)?updated:p);
        return copy(milestones,next,audit).withEvent("proposal."+decision,"actor="+actor+";proposal="+proposalId);
    }
    public ProposalStatus status(String actor,String proposalId){
        requireMember(actor);
        var p=findProposal(proposalId);
        var att=findMilestone(p.milestoneId()).attestations();
        Set<String> approvals=new HashSet<>(),rejections=new HashSet<>();
        p.votes().forEach((member,decision)->{
            if(decision.equals("approve"))approvals.add(member);else if(decision.equals("reject"))rejections.add(member);
        }
        );
        return new ProposalStatus(p.id(),id,p.purpose(),p.beneficiaryId(),p.amountCents(),p.status(),approvals.size(),rejections.size(),att.size(),quorum,
        p.status().equals("pending")&&governance().canRelease(approvals,att));
    }
    public Proposal proposal(String actor,String id){
        requireMember(actor);
        return findProposal(id);
    }
    public Vault executed(String actor,String proposalId,String transferId) {
        var p=proposal(actor,proposalId);
        if(p.status().equals("executed"))return this;
        if(!status(actor,proposalId).releasable())throw new IllegalStateException("Faltan aprobaciones o validaciones de hito");
        if(transferId==null||transferId.isBlank())throw new IllegalArgumentException("Transferencia ausente");
        var updated=new Proposal(p.id(),p.milestoneId(),p.beneficiaryId(),p.amountCents(),p.purpose(),"executed",p.votes(),transferId);
        var next=new ArrayList<Proposal>();
        for(var value:proposals)next.add(value.id().equals(proposalId)?updated:value);
        return copy(milestones,next,audit).withEvent("proposal.executed","proposal="+proposalId+";transfer="+transferId);
    }
    public AuditReport verifyAudit(String actor) {
        requireMember(actor);
        String previous="GENESIS";
        List<Integer> bad=new ArrayList<>();
        int seq=1;
        for(var event:audit){
            if(event.seq()!=seq||!event.previousHash().equals(previous)||!event.hash().equals(AuditHash.sha256(previous,seq,event.action(),event.payload())))bad.add(event.seq());
            previous=event.hash();
            seq++;
        }
        return new AuditReport(bad.isEmpty(),audit.size(),bad,"Cadena verificable; no es inmutabilidad externa");
    }
}
