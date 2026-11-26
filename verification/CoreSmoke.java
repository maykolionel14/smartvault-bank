import co.smartvault.identity.application.*;
import co.smartvault.identity.domain.UserAccount;
import co.smartvault.ledger.application.*;
import co.smartvault.ledger.domain.*;
import co.smartvault.platform.*;
import co.smartvault.reports.application.*;
import co.smartvault.reports.domain.*;
import co.smartvault.reports.infrastructure.ReportCsvWriter;
import co.smartvault.vaults.domain.Vault;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Executes real pure-Java business classes without Maven or Spring. Not an HTTP integration test. */
public class CoreSmoke {
    private static int passed;

    public static void main(String[] args) {
        check("Identity: registration and session revocation", CoreSmoke::identity);
        check("Ledger: actor-scoped idempotency", CoreSmoke::ledger);
        check("Ledger: insufficient funds", CoreSmoke::funds);
        check("Vaults: quorum, contribution replay and audit", CoreSmoke::vault);
        check("Reports: totals, signs and filtering", CoreSmoke::report);
        check("Reports: empty and invalid entries", CoreSmoke::emptyReport);
        check("Reports: dependency recovery", CoreSmoke::reportRecovery);
        check("Reports: CSV", CoreSmoke::csv);
        check("Platform: signed tokens and service key", CoreSmoke::security);
        System.out.println("PASS " + passed + " independent pure-Java scenarios");
    }

    private static void check(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS " + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Unexpected exception", error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void identity() {
        // Arrange: in-memory port and a test-only password stub, not production crypto.
        Map<String, UserAccount> state = new HashMap<>();
        UserStore users = new UserStore() {
            public Optional<UserAccount> byId(String id) { return Optional.ofNullable(state.get(id)); }
            public Optional<UserAccount> byEmail(String email) { return state.values().stream().filter(u -> u.email().equals(email)).findFirst(); }
            public UserAccount save(UserAccount user) { state.put(user.id(), user); return user; }
        };
        PasswordPort passwords = new PasswordPort() {
            public String hash(String raw) { return "test-hash:" + raw; }
            public boolean matches(String raw, String hash) { return hash.equals(hash(raw)); }
        };
        var service = new IdentityUseCases(users, passwords, (id, version) -> id + ":" + version, () -> "id");
        // Act
        var user = service.register(" Alice@Example.Test ", "demo-password");
        var login = service.login("alice@example.test", "demo-password");
        boolean before = service.verify(user.id(), 0);
        service.revoke(user.id());
        // Assert
        require(user.activated() && login.activated() && before, "new demo account must be active");
        require(user.email().equals("alice@example.test"), "normalization");
        require(!service.verify(user.id(), 0), "old version revoked");
        rejects(IllegalStateException.class, () -> service.register("alice@example.test", "demo-password"));
    }

    private static void ledger() {
        // Arrange
        var keys = new ArrayList<String>();
        LedgerPort port = new LedgerPort() {
            public Movement credit(String wallet, long cents, String key) { keys.add(key); return new Movement("tx", cents, "demo-credit"); }
            public Movement transfer(String from, String to, long cents, String key, String kind) { keys.add(key); return new Movement("tx2", cents, kind); }
            public void openVault(String id) {}
            public long balance(String wallet) { return 0; }
            public List<Entry> statement(String wallet) { return List.of(); }
            public Integrity integrity() { return new Integrity(true, 0, List.of()); }
        };
        var service = new LedgerUseCases(port);
        // Act
        var credit = service.credit("alice", 1000, "unique-key");
        var contribution = service.contribute("alice", "vault", 200, "unique-key");
        // Assert
        require(credit.amount_cents() == 1000 && contribution.amount_cents() == 200, "cent amounts");
        require(keys.equals(List.of("demo:alice:unique-key", "user:alice:unique-key")), "scoped key");
        rejects(IllegalArgumentException.class, () -> service.credit("alice", 0, "unique-key"));
        require(keys.size() == 2, "invalid command not persisted");
    }

    private static void funds() {
        // Arrange
        var amount = new Money(1000);
        // Act / Assert
        rejects(IllegalStateException.class, () -> PostingPolicy.validateTransfer("a", "b", amount, 999));
        require(PostingPolicy.balanced(List.of(-1000L, 1000L)), "balanced postings");
        require(!PostingPolicy.balanced(List.of(-1000L, 999L)), "unbalanced postings");
    }

    private static void vault() {
        // Arrange
        var ids = new AtomicInteger();
        var vault = Vault.create("v", "QA colectiva", "alice", List.of("alice", "bob"), 2, List.of("Hito"), () -> "id-" + ids.incrementAndGet());
        String milestone = vault.milestones().getFirst().id();
        // Act
        var contributed = vault.contribution("alice", "tx", 500);
        var voted = contributed.propose("alice", "p", milestone, "bob", 100, "Pago acordado")
                .vote("alice", "p", "approve").vote("bob", "p", "approve");
        var ready = voted.attest("alice", milestone, "Acta completa").attest("bob", milestone, "Acta confirmada");
        var executed = ready.executed("alice", "p", "payout");
        // Assert
        require(contributed == contributed.contribution("alice", "tx", 500), "idempotent audit");
        require(!voted.status("alice", "p").releasable() && ready.status("alice", "p").releasable(), "quorum and milestones");
        require(executed.status("alice", "p").status().equals("executed"), "executed");
        require(executed.verifyAudit("alice").valid(), "audit valid");
        rejects(SecurityException.class, () -> executed.verifyAudit("outsider"));
    }

    private static List<co.smartvault.reports.domain.Movement> entries() {
        return List.of(new co.smartvault.reports.domain.Movement("credit", 5000, "demo-credit", Instant.EPOCH),
                new co.smartvault.reports.domain.Movement("saving", -1200, "contribution", Instant.EPOCH),
                new co.smartvault.reports.domain.Movement("payout", 300, "governed-payout", Instant.EPOCH));
    }

    private static void report() {
        // Arrange
        var service = new ReportUseCases(actor -> { require(actor.equals("alice"), "actor isolation"); return entries(); });
        // Act
        var summary = service.summary("alice");
        // Assert
        require(summary.income_cents() == 5300 && summary.expense_cents() == 1200 && summary.net_cents() == 4100, "report totals");
        require(summary.income().movement_count() == 2 && summary.expenses().movement_count() == 1 && summary.contributions().total_cents() == 1200, "filtered sections");
    }

    private static void emptyReport() {
        // Arrange
        var empty = FinancialReport.from(List.of());
        var repeated = entries().getFirst();
        // Act / Assert
        require(empty.net_cents() == 0 && empty.movement_count() == 0, "empty report");
        rejects(IllegalArgumentException.class, () -> FinancialReport.from(List.of(repeated, repeated)));
        rejects(IllegalArgumentException.class, () -> new ReportUseCases(actor -> entries()).summary(""));
    }

    private static void reportRecovery() {
        // Arrange
        var attempts = new AtomicInteger();
        var service = new ReportUseCases(actor -> {
            if (attempts.incrementAndGet() == 1) throw new ReportSourceUnavailable(null);
            return entries();
        });
        // Act
        rejects(ReportSourceUnavailable.class, () -> service.summary("alice"));
        var recovered = service.summary("alice");
        // Assert
        require(recovered.net_cents() == 4100 && attempts.get() == 2, "dependency recovered");
    }

    private static void csv() {
        // Arrange
        var summary = FinancialReport.from(entries());
        // Act
        String csv = ReportCsvWriter.write(summary);
        // Assert
        require(csv.lines().count() == 4 && csv.contains(",-1200,COP"), "CSV lines and sign");
    }

    private static void security() {
        // Arrange
        String key = "test-only-secret-012345678901234567890";
        var tokens = new SignedTokens(key, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        String user = "12345678-1234-1234-1234-123456789012";
        // Act
        String token = tokens.issue(user, 0);
        // Assert
        require(tokens.read(token).subject().equals(user), "valid signature");
        rejects(BusinessException.class, () -> tokens.read(token + "x"));
        rejects(BusinessException.class, () -> new ServiceKeyGuard(key).require(null));
    }
}
