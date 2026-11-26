package co.smartvault.platform;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class NeonDatabaseInitializerTest {
    private final NeonDatabaseInitializer initializer = new NeonDatabaseInitializer();
    private static final String SOURCE =
            "postgresql://user%40role:p+%3Aa%2Fb@ep-app-pooler.region.aws.neon.tech/example?sslmode=require&channel_binding=require";

    @Test void vercelCredentialsAreDecodedOutsideTheVerifiedJdbcUrl() {
        try (var context = new GenericApplicationContext()) {
            var env = new MockEnvironment().withProperty("DATABASE_URL", SOURCE);
            context.setEnvironment(env);
            initializer.initialize(context);
            assertEquals("user@role", env.getProperty("spring.datasource.username"));
            assertEquals("p+:a/b", env.getProperty("spring.datasource.password"));
            assertEquals("jdbc:postgresql://ep-app-pooler.region.aws.neon.tech:5432/example"
                    + "?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory&channelBinding=require",
                    env.getProperty("spring.datasource.url"));
        }
    }

    @Test void explicitLocalConnectionIsPreserved() {
        try (var context = new GenericApplicationContext()) {
            var env = new MockEnvironment().withProperty("DATABASE_URL", SOURCE).withProperty("DB_URL","jdbc:postgresql://localhost/test");
            context.setEnvironment(env);
            initializer.initialize(context);
            assertFalse(env.getPropertySources().contains("neonDatabase"));
        }
    }

    @Test void unsafeOrMalformedConnectionFailsWithoutLeakingItsSecret() {
        for (String url : List.of("postgresql://user:secret-value@elsewhere.test/db",
                "postgresql://user:secret-value@some.neon.tech/db%zz", "postgresql://some.neon.tech/db")) {
            try (var context = new GenericApplicationContext()) {
                context.setEnvironment(new MockEnvironment().withProperty("DATABASE_URL", url));
                var failure = assertThrows(IllegalStateException.class, () -> initializer.initialize(context));
                assertFalse(failure.toString().contains("secret-value"));
                assertNull(failure.getCause());
            }
        }
    }
}
