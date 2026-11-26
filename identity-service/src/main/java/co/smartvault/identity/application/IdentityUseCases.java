package co.smartvault.identity.application;

import co.smartvault.identity.domain.UserAccount;
import java.util.*;
import java.util.function.Supplier;

public final class IdentityUseCases {
    private final UserStore userRepository;
    private final PasswordPort passwordEncoder;
    private final TokenPort tokenService;
    private final Supplier<String> idGenerator;
    public record LoginResult(String access_token, String token_type, String user_id, boolean activated) {
    }
    public IdentityUseCases(UserStore userRepository, PasswordPort passwordEncoder, TokenPort tokenService, Supplier<String> idGenerator) {
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.tokenService=tokenService;
        this.idGenerator=idGenerator;
    }
    public UserAccount register(String email,String password) {
        checkPassword(password);
        String normalizedEmail=UserAccount.normalize(email);
        if(userRepository.byEmail(normalizedEmail).isPresent()) throw new IllegalStateException("Correo ya registrado");
        return userRepository.save(new UserAccount(idGenerator.get(),normalizedEmail,passwordEncoder.hash(password),false,0,true));
    }
    public LoginResult login(String email,String password) {
        var user=userRepository.byEmail(UserAccount.normalize(email)).orElseThrow(()->new SecurityException("Credenciales inválidas"));
        if(user.suspended() || !passwordEncoder.matches(password,user.passwordHash())) throw new SecurityException("Credenciales inválidas");
        return new LoginResult(tokenService.issue(user.id(),user.sessionVersion()),"bearer",user.id(),user.activated());
    }
    public UserAccount user(String id) {
        return userRepository.byId(id).orElseThrow(()->new NoSuchElementException("Usuario inexistente"));
    }
    public boolean verify(String id, int version) {
        return userRepository.byId(id).map(account->account.canAct(version)).orElse(false);
    }
    public boolean versionValid(String id, int version) {
        return user(id).sessionVersion()==version;
    }
    public UserAccount freeze(String actor) {
        return userRepository.save(user(actor).freeze());
    }
    public UserAccount unfreeze(String actor) {
        return userRepository.save(user(actor).unfreeze());
    }
    public void revoke(String actor) {
        userRepository.save(user(actor).revoke());
    }
    public void changePassword(String actor,String oldPassword,String newPassword) {
        checkPassword(newPassword);
        var user=user(actor);
        if(!passwordEncoder.matches(oldPassword,user.passwordHash())) throw new SecurityException("Contraseña anterior incorrecta");
        userRepository.save(user.withPassword(passwordEncoder.hash(newPassword)));
    }
    public Map<String,String> knownUsers(List<String> ids) {
        Map<String,String> knownAccounts=new HashMap<>();
        for(String id:ids) userRepository.byId(id).filter(UserAccount::activated).ifPresent(account->knownAccounts.put(account.id(),account.email()));
        return knownAccounts;
    }
    public static void checkPassword(String value) {
        if(value==null || value.length()<10 || value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new IllegalArgumentException("Contraseña: mínimo 10 caracteres y máximo 72 bytes");
    }
}
