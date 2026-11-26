package co.smartvault.verification;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SystemVerificationTest {
    @Test
    void domainAndApplicationDependenciesPointInward() throws Exception {
        // Arrange
        Path root = Path.of("..").toAbsolutePath().normalize();
        List<String> services = List.of("identity", "ledger", "vaults", "reports");
        List<String> violations = new ArrayList<>();

        // Act
        for (String service : services) {
            Path source = root.resolve(service + "-service/src/main/java");
            try (var paths = Files.walk(source)) {
                for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String layer = path.getParent().getFileName().toString();
                    if (!Set.of("domain", "application").contains(layer)) continue;
                    for (String line : Files.readAllLines(path)) {
                        if (!line.stripLeading().startsWith("import ")) continue;
                        if (line.contains("org.springframework") || line.contains("jakarta.persistence")
                                || line.contains(".infrastructure.") || line.contains(".config.")
                                || (layer.equals("domain") && line.contains(".application."))) {
                            violations.add(path + ": " + line);
                        }
                    }
                }
            }
        }
        // Assert
        assertTrue(violations.isEmpty(), violations.toString());
    }
}
