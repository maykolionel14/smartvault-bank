package co.smartvault.identity.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

@Component
public class AuthenticationBudget {
    private final JdbcTemplate database;

    public AuthenticationBudget(JdbcTemplate database) { this.database=database; }

    @Transactional
    public boolean allow(String operation,String address) {
        long window=System.currentTimeMillis()/60_000;
        database.update("delete from identity_auth_budget where period < ?",window-2);
        String normalized=address.strip().toLowerCase(Locale.ROOT);
        String bucket=operation+"-"+UUID.nameUUIDFromBytes(normalized.getBytes(StandardCharsets.UTF_8));
        return take(operation,window,60) && take(bucket,window,10);
    }

    private boolean take(String id,long period,int maximum) {
        return database.update("""
                insert into identity_auth_budget (id,period,requests) values (?,?,1)
                on conflict (id) do update set period=excluded.period,
                requests=case when identity_auth_budget.period=excluded.period then identity_auth_budget.requests+1 else 1 end
                where identity_auth_budget.period<excluded.period or identity_auth_budget.requests<?
                """,id,period,maximum)==1;
    }
}
