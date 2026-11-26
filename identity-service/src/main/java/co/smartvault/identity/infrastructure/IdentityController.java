package co.smartvault.identity.infrastructure;

import co.smartvault.identity.application.IdentityUseCases;
import co.smartvault.identity.domain.UserAccount;
import co.smartvault.platform.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

@RestController
public class IdentityController {
    private final IdentityUseCases service;
    private final SignedTokens tokens;
    private final AuthenticationBudget budget;
    private final ServiceKeyGuard internalKey;
    private final Set<String> allowedEmails;
    private final boolean restrictedRegistration;
    public IdentityController(IdentityUseCases service,AuthenticationBudget budget,SignedTokens tokens,ServiceKeyGuard internalKey,
        @Value("${app.demo-allowed-emails:}") String allowedEmails,
        @Value("${app.restrict-registration:true}") boolean restrictedRegistration) {
        this.service=service;
        this.budget=budget;
        this.tokens=tokens;
        this.internalKey=internalKey;
        this.restrictedRegistration=restrictedRegistration;
        this.allowedEmails=new HashSet<>();
        for(String email:allowedEmails.split(",")) {
            if(!email.isBlank()) this.allowedEmails.add(email.strip().toLowerCase(Locale.ROOT));
        }
    }
    public record Credentials(@Email @NotBlank String email,@NotBlank String password) {
    }
    public record PasswordChange(String previous_password,String new_password) {
    }
    public record Verification(String token) {
    }
    public record PublicUser(String id,String email, boolean suspended, boolean activated) {
    }
    private void checkBudget(String operation,String email) {
        if (!budget.allow(operation,email)) throw new BusinessException(429,"Demasiadas solicitudes. Intenta de nuevo en un minuto.");
    }
    private SignedTokens.Claims claims(String header) {
        return tokens.read(SignedTokens.bearer(header));
    }
    private String actor(String header) {
        var c=claims(header);
        if(!service.verify(c.subject(),c.version())) throw BusinessException.unauthorized("Sesión revocada o suspendida");
        return c.subject();
    }
    private PublicUser info(UserAccount user) {
        return new PublicUser(user.id(),user.email(),user.suspended(),user.activated());
    }
    @GetMapping("/health") public Map<String,String> health() {
        return Map.of("status","ok","service","identity");
    }
    @ResponseStatus(HttpStatus.CREATED) @PostMapping("/api/v1/identity/register")
    public Map<String,String> register(@Valid @RequestBody Credentials body) {
        if(restrictedRegistration && !allowedEmails.contains(body.email().strip().toLowerCase(Locale.ROOT)))
            throw BusinessException.forbidden("Registro reservado a participantes autorizados");
        checkBudget("register",body.email());
        var u=service.register(body.email(),body.password());
        return Map.of("user_id",u.id(),"email",u.email(),"message","Cuenta de demostración creada. Ya puedes iniciar sesión.");
    }
    @PostMapping("/api/v1/identity/login") public IdentityUseCases.LoginResult login(@Valid @RequestBody Credentials body) {
        checkBudget("login",body.email());
        return service.login(body.email(),body.password());
    }
    @GetMapping("/api/v1/identity/me") public PublicUser me(@RequestHeader(value="Authorization",required=false) String token) {
        return info(service.user(actor(token)));
    }
    @PostMapping("/api/v1/identity/change-password") public Map<String,String> change(@RequestHeader(value="Authorization",required=false) String token,@RequestBody PasswordChange body) {
        service.changePassword(actor(token),body.previous_password(),body.new_password());
        return Map.of("message","Contraseña cambiada; iniciar sesión nuevamente");
    }
    @PostMapping("/api/v1/identity/freeze") public Map<String,Boolean> freeze(@RequestHeader(value="Authorization",required=false) String token) {
        service.freeze(actor(token));
        return Map.of("suspended",true);
    }
    @PostMapping("/api/v1/identity/unfreeze") public Map<String,Object> unfreeze(@RequestHeader(value="Authorization",required=false) String token) {
        var c=claims(token);
        if(!service.versionValid(c.subject(),c.version())) throw BusinessException.unauthorized("Sesión revocada");
        service.unfreeze(c.subject());
        return Map.of("suspended",false,"message","Iniciar sesión nuevamente");
    }
    @PostMapping("/api/v1/identity/revoke-sessions") public Map<String,String> revoke(@RequestHeader(value="Authorization",required=false) String token) {
        service.revoke(actor(token));
        return Map.of("message","Sesiones revocadas");
    }
    @PostMapping("/api/v1/identity/evaluate-action") public Map<String,Boolean> evaluate(@RequestHeader(value="Authorization",required=false) String token) {
        actor(token);
        return Map.of("allowed",true);
    }
    @PostMapping("/internal/identity/verify") public Map<String,Boolean> verify(@RequestHeader(value="X-Service-Key",required=false) String key,@RequestBody Verification input) {
        internalKey.require(key);
        try {
            var c=tokens.read(input.token());
            return Map.of("active",service.verify(c.subject(),c.version()));
        }
        catch (RuntimeException ex) {
            return Map.of("active",false);
        }
    }
    @PostMapping("/internal/identity/known-users") public Map<String,String> users(@RequestHeader(value="X-Service-Key",required=false) String key,@RequestBody List<String> ids) {
        internalKey.require(key);
        return service.knownUsers(ids);
    }
}
