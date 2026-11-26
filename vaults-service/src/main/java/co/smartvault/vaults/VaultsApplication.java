package co.smartvault.vaults;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "co.smartvault.vaults", "co.smartvault.platform"
}
)
public class VaultsApplication {
    public static void main(String[] args) {
        SpringApplication.run(VaultsApplication.class, args);
    }
}
