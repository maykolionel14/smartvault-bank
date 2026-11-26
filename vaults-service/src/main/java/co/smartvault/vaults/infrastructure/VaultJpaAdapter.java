package co.smartvault.vaults.infrastructure;

import co.smartvault.vaults.application.VaultPorts;
import co.smartvault.vaults.domain.Vault;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

@Repository
public class VaultJpaAdapter implements VaultPorts.VaultStore {
    private final VaultJpaRepository repo;
    private final ObjectMapper mapper;
    public VaultJpaAdapter(VaultJpaRepository repo,ObjectMapper mapper){
        this.repo=repo;
        this.mapper=mapper;
    }
    private Vault parse(VaultRow row){
        return mapper.readValue(row.document,Vault.class);
    }
    public Optional<Vault> byId(String id){
        return repo.findById(id).map(this::parse);
    }
    public List<Vault> all(){
        return repo.findAll().stream().map(this::parse).toList();
    }
    public List<Vault> forMember(String actor){
        return all().stream().filter(v->v.members().contains(actor)).toList();
    }
    @Transactional
    public void save(Vault vault) {
        String json=mapper.writeValueAsString(vault);
        var stored=repo.lockById(vault.id());
        if(stored.isPresent() && !parse(stored.get()).audit().getLast().hash().equals(vault.audit().getLast().previousHash()))
            throw new IllegalStateException("Otra persona actualizó la bóveda. Actualiza e intenta de nuevo.");
        var row=stored.orElseGet(()->new VaultRow(vault.id(),json));
        row.document=json;
        repo.saveAndFlush(row);
    }
}
