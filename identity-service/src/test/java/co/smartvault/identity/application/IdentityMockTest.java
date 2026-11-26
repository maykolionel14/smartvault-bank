package co.smartvault.identity.application;

import co.smartvault.identity.domain.UserAccount;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;

class IdentityMockTest {
    @Test void verifyConsultsRepositoryAndNoTokenCreatedForBlockedAccount(){
        UserStore repo=mock(UserStore.class);
        PasswordPort pass=mock(PasswordPort.class);
        TokenPort tokens=mock(TokenPort.class);
        var blocked=new UserAccount("id","me@example.org","hash",true,1);
        when(repo.byEmail("me@example.org")).thenReturn(Optional.of(blocked));
        when(repo.byId("id")).thenReturn(Optional.of(blocked));
        var sut=new IdentityUseCases(repo,pass,tokens,()->"unused");
        assertFalse(sut.verify("id",1));
        assertThrows(SecurityException.class,()->sut.login("me@example.org","password1234"));
        verify(tokens,never()).issue(anyString(),anyInt());
    }
}
