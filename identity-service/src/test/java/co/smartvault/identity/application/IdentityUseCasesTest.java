package co.smartvault.identity.application;

import co.smartvault.identity.domain.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IdentityUseCasesTest {
    Map<String,UserAccount> db;
    IdentityUseCases sut;
    @BeforeEach void setup(){
        db=new HashMap<>();
        UserStore store=new UserStore(){
            public Optional<UserAccount> byEmail(String e){
                return db.values().stream().filter(x->x.email().equals(e)).findFirst();
            }
            public Optional<UserAccount> byId(String id){
                return Optional.ofNullable(db.get(id));
            }
            public UserAccount save(UserAccount u){
                db.put(u.id(),u);
                return u;
            }
        }
        ;
        PasswordPort passwords=new PasswordPort(){
            public String hash(String x){
                return "h:"+x;
            }
            public boolean matches(String raw,String hashed){
                return hashed.equals("h:"+raw);
            }
        }
        ;
        sut=new IdentityUseCases(store,passwords,(id,ver)->id+":"+ver,()->"my-uuid");
    }
    @Test void registersLogsInAndLooksUp(){
        var user=sut.register(" ME@MAIL.COM ","1234567890");
        assertEquals("me@mail.com",user.email());
        var login=sut.login("me@mail.com","1234567890");
        assertEquals("my-uuid:0",login.access_token());
        assertTrue(login.activated());
        assertTrue(sut.verify("my-uuid",0));
        assertFalse(sut.verify("my-uuid",1));
        assertEquals("me@mail.com",sut.knownUsers(List.of("my-uuid","missing")).get("my-uuid"));
        assertTrue(sut.versionValid("my-uuid",0));
    }
    @Test void rejectsDuplicateWeakPasswordAndBadLogin(){
        assertThrows(IllegalArgumentException.class,()->sut.register("hi@x.co","short"));
        sut.register("hi@x.co","1234567890");
        assertThrows(IllegalStateException.class,()->sut.register("HI@X.CO","1234567890"));
        assertThrows(SecurityException.class,()->sut.login("hi@x.co","wrong-password"));
        assertThrows(SecurityException.class,()->sut.login("no@x.co","1234567890"));
        assertThrows(NoSuchElementException.class,()->sut.user("missing"));
    }
    @Test void freezesResumesChangesPasswordAndRevokes(){
        sut.register("hi@x.co","1234567890");
        sut.freeze("my-uuid");
        assertFalse(sut.verify("my-uuid",0));
        assertThrows(SecurityException.class,()->sut.login("hi@x.co","1234567890"));
        assertThrows(IllegalStateException.class,()->sut.freeze("my-uuid"));
        sut.unfreeze("my-uuid");
        assertTrue(sut.verify("my-uuid",1));
        assertThrows(SecurityException.class,()->sut.changePassword("my-uuid","bad","abcdefghij"));
        sut.changePassword("my-uuid","1234567890","abcdefghij");
        assertTrue(sut.verify("my-uuid",2));
        assertFalse(sut.verify("my-uuid",1));
        assertEquals("my-uuid:2",sut.login("hi@x.co","abcdefghij").access_token());
        sut.revoke("my-uuid");
        assertFalse(sut.verify("my-uuid",2));
    }
}
