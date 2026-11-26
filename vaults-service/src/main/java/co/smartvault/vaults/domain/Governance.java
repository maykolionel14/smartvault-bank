package co.smartvault.vaults.domain;

import java.util.*;

public record Governance(List<String> members, int quorum) {
    public Governance {
        if(members==null || members.isEmpty() || members.size()>20 || new HashSet<>(members).size()!=members.size() || members.stream().anyMatch(x->x==null||x.isBlank()))
        throw new IllegalArgumentException("Miembros inválidos o duplicados");
        if(quorum<1 || quorum>members.size())throw new IllegalArgumentException("Quórum fuera de rango");
        members=List.copyOf(members);
    }
    public boolean canRelease(Set<String> approvals, Set<String> attestations) {
        var valid=new HashSet<>(members);
        return approvals.stream().filter(valid::contains).count()>=quorum && attestations.stream().filter(valid::contains).count()>=quorum;
    }
    public boolean rejected(Set<String> rejections) {
        var valid=new HashSet<>(members);
        return rejections.stream().filter(valid::contains).count()>members.size()-quorum;
    }
    public void requireMember(String actor) {
        if(!members.contains(actor))throw new SecurityException("No pertenece a la bóveda");
    }
}
