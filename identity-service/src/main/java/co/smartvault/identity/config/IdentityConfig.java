package co.smartvault.identity.config;

import co.smartvault.identity.application.*;
import co.smartvault.platform.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.Clock;
import java.util.UUID;

@Configuration
public class IdentityConfig {
    @Bean SignedTokens tokens(@Value("${app.jwt-secret}") String secret) {
        return new SignedTokens(secret,Clock.systemUTC());
    }
    @Bean ServiceKeyGuard serviceKey(@Value("${app.service-key}") String secret) {
        return new ServiceKeyGuard(secret);
    }
    @Bean IdentityUseCases identity(UserStore users,SignedTokens tokens) {
        var encoder=new BCryptPasswordEncoder(12);
        PasswordPort password=new PasswordPort() {
            public String hash(String raw) {
                return encoder.encode(raw);
            }
            public boolean matches(String raw,String hash) {
                return encoder.matches(raw,hash);
            }
        };
        return new IdentityUseCases(users,password,tokens::issue,()->UUID.randomUUID().toString());
    }
}
