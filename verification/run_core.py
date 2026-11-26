"""Compile and run pure-Java smoke tests with the installed JDK 21 only."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    sources = []
    for service in ("identity", "ledger", "vaults", "reports"):
        base = ROOT / (service + "-service") / "src/main/java/co/smartvault" / service
        for layer in ("domain", "application"):
            sources.extend(sorted((base / layer).rglob("*.java")))
    platform = ROOT / "platform/src/main/java/co/smartvault/platform"
    sources.extend(platform / name for name in ("BusinessException.java", "ServiceKeyGuard.java", "SignedTokens.java"))
    sources.append(ROOT / "reports-service/src/main/java/co/smartvault/reports/infrastructure/ReportCsvWriter.java")
    sources.append(ROOT / "verification/CoreSmoke.java")
    with tempfile.TemporaryDirectory(prefix="smartvault-core-") as directory:
        subprocess.run(["javac", "--release", "21", "-encoding", "UTF-8", "-d", directory, *map(str, sources)], check=True)
        subprocess.run(["java", "-cp", directory, "CoreSmoke"], check=True)


if __name__ == "__main__":
    main()
