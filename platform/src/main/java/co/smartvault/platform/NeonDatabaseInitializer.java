package co.smartvault.platform;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

public final class NeonDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        var environment = context.getEnvironment();
        String url = environment.getProperty("DATABASE_URL", "");
        if (url.isBlank() || !environment.getProperty("DB_URL", "").isBlank()) {
            return;
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException invalid) {
            throw invalidConfiguration();
        }

        String host = uri.getHost();
        String userInfo = uri.getRawUserInfo();
        String path = uri.getRawPath();
        if (!("postgresql".equals(uri.getScheme()) || "postgres".equals(uri.getScheme()))
                || host == null || !host.endsWith(".neon.tech") || uri.getFragment() != null
                || userInfo == null || path == null || !path.matches("/[^/]+")) {
            throw invalidConfiguration();
        }
        int separator = userInfo.indexOf(':');
        if (separator < 1 || separator == userInfo.length() - 1) {
            throw invalidConfiguration();
        }
        int port = uri.getPort() < 0 ? 5432 : uri.getPort();
        if (port < 1 || port > 65535) {
            throw invalidConfiguration();
        }
        String jdbc = "jdbc:postgresql://" + host + ":" + port + path
                + "?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory";
        if (uri.getRawQuery() != null && java.util.Arrays.asList(uri.getRawQuery().split("&")).contains("channel_binding=require")) {
            jdbc += "&channelBinding=require";
        }
        environment.getPropertySources().addFirst(new MapPropertySource("neonDatabase", Map.of(
                "spring.datasource.url", jdbc,
                "spring.datasource.username", decode(userInfo.substring(0, separator)),
                "spring.datasource.password", decode(userInfo.substring(separator + 1)))));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static IllegalStateException invalidConfiguration() {
        return new IllegalStateException("Revisa la conexión PostgreSQL del entorno en Vercel.");
    }
}
