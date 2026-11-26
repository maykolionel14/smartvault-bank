package co.smartvault.identity.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserAccountTest {
    private UserAccount user(){
        return new UserAccount("id","EXAMPLE@MAIL.CO","hash",false,1,true);
    }
    @Test void normalizesAndValidates(){
        assertEquals("example@mail.co",user().email());
        assertThrows(IllegalArgumentException.class,()->UserAccount.normalize("broken"));
        assertThrows(IllegalArgumentException.class,()->new UserAccount("id","a@b.co","hash",false,-1));
    }
    @Test void freezeUnfreezeAndRevoke(){
        var user=user();
        assertTrue(user.canAct(1));
        assertFalse(user.canAct(2));
        var frozen=user.freeze();
        assertFalse(frozen.canAct(1));
        assertTrue(frozen.canResume(1));
        assertThrows(IllegalStateException.class,frozen::freeze);
        var restored=frozen.unfreeze();
        assertEquals(2,restored.sessionVersion());
        assertFalse(restored.suspended());
        assertThrows(IllegalStateException.class,restored::unfreeze);
        assertEquals(3,restored.revoke().sessionVersion());
        assertEquals(3,restored.withPassword("new").sessionVersion());
    }
}
