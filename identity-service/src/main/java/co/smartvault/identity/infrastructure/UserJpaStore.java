package co.smartvault.identity.infrastructure;

import co.smartvault.identity.application.UserStore;
import co.smartvault.identity.domain.UserAccount;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public class UserJpaStore implements UserStore {
    private final SpringUserRepository repo;
    public UserJpaStore(SpringUserRepository repo) {
        this.repo=repo;
    }
    private static UserAccount from(UserRow row) {
        return new UserAccount(row.id,row.email,row.passwordHash,row.suspended,row.sessionVersion,row.activated);
    }
    public Optional<UserAccount> byEmail(String email) {
        return repo.findByEmail(email).map(UserJpaStore::from);
    }
    public Optional<UserAccount> byId(String id) {
        return repo.findById(id).map(UserJpaStore::from);
    }
    @Transactional
    public UserAccount save(UserAccount user) {
        UserRow entity=repo.findById(user.id()).orElseGet(()->new UserRow(user.id(),user.email(),user.passwordHash(),user.suspended(),user.sessionVersion()));
        entity.email=user.email();
        entity.passwordHash=user.passwordHash();
        entity.suspended=user.suspended();
        entity.activated=user.activated();
        entity.sessionVersion=user.sessionVersion();
        return from(repo.saveAndFlush(entity));
    }
}
