package co.smartvault.identity.application;

import co.smartvault.identity.domain.UserAccount;
import java.util.Optional;

public interface UserStore {
    Optional<UserAccount> byEmail(String email);
    Optional<UserAccount> byId(String id);
    UserAccount save(UserAccount user);
}
