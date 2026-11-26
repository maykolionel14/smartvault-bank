package co.smartvault.identity.application;

import co.smartvault.identity.domain.UserAccount;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationAaaTest {
    @Test
    void registersDemoAccountWithHashedPassword() {
        // Arrange
        var users = mock(UserStore.class);
        var passwords = mock(PasswordPort.class);
        var tokens = mock(TokenPort.class);
        when(users.byEmail("alice@example.test")).thenReturn(Optional.empty());
        when(passwords.hash("demo-password")).thenReturn("hashed-value");
        when(users.save(any(UserAccount.class))).thenAnswer(call -> call.getArgument(0));
        var useCases = new IdentityUseCases(users, passwords, tokens, () -> "id");
        // Act
        var user = useCases.register(" Alice@Example.Test ", "demo-password");
        // Assert
        assertTrue(user.activated());
        assertEquals("alice@example.test", user.email());
        assertEquals("hashed-value", user.passwordHash());
        verify(passwords).hash("demo-password");
        verify(users).save(user);
        verifyNoInteractions(tokens);
    }

    @Test
    void pendingLegacyAccountCannotAccessProtectedOperations() {
        // Arrange
        var users = mock(UserStore.class);
        var pending = new UserAccount("id", "alice@example.test", "hash", false, 0, false);
        when(users.byId("id")).thenReturn(Optional.of(pending));
        var useCases = new IdentityUseCases(users, mock(PasswordPort.class), mock(TokenPort.class), () -> "id");
        // Act
        boolean allowed = useCases.verify("id", 0);
        // Assert
        assertFalse(allowed);
        verify(users).byId("id");
    }
}
