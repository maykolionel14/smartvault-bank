package co.smartvault.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

class ApiErrorsTest {
    @Test void signsAndRejectsTampering() {
        var clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneOffset.UTC);
        var token = new SignedTokens("01234567890123456789012345678901",clock);
        var id = "12345678-1234-1234-1234-123456789012";
        var signed = token.issue(id, 3);
        assertEquals(id,token.read(signed).subject());
        assertEquals(3,token.read(signed).version());
        assertThrows(BusinessException.class,()->token.read(signed+"a"));
        assertThrows(BusinessException.class,()->token.read("garbage"));
        assertThrows(BusinessException.class,()->SignedTokens.bearer(null));
        assertEquals(signed,SignedTokens.bearer("Bearer "+signed));
    }
    @Test void expiryAndKeys() {
        var id="12345678-1234-1234-1234-123456789012";
        var base=new SignedTokens("01234567890123456789012345678901",Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"),ZoneOffset.UTC));
        var expired=new SignedTokens("01234567890123456789012345678901",Clock.fixed(Instant.parse("2026-11-22T11:00:00Z"),ZoneOffset.UTC));
        assertThrows(BusinessException.class,()->expired.read(base.issue(id,1)));
        assertThrows(IllegalArgumentException.class,()->new SignedTokens("short", Clock.systemUTC()));
        var key=new ServiceKeyGuard("01234567890123456789012345678901");
        assertDoesNotThrow(()->key.require("01234567890123456789012345678901"));
        assertThrows(BusinessException.class,()->key.require("invalid"));
    }
}
