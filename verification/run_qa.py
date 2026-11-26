"""One-command isolated integration, load, stress and recovery test runner."""
import argparse
import os
import secrets
import socket
import subprocess
import tempfile
from pathlib import Path
from qa_http import Api
import integration_test
import performance_test
import recovery_test

ROOT = Path(__file__).resolve().parents[1]


def free_port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--p95-ms", type=int, default=500, help="Objetivo p95 de carga normal")
    parser.add_argument("--skip-performance", action="store_true", help="Ejecutar solo integración y recuperación")
    args = parser.parse_args()
    assert 1 <= args.p95_ms <= 60000
    project = "smartvault-qa-" + secrets.token_hex(4)
    port = free_port()
    config = {"DB_USER": "smartvault", "DB_PASSWORD": secrets.token_hex(32),
              "JWT_SECRET": secrets.token_hex(32), "SERVICE_KEY": secrets.token_hex(32),
              "RESTRICT_REGISTRATION": "true", "DEMO_ALLOWED_EMAILS": integration_test.ALICE + "," + integration_test.BOB,
              "WEB_PORT": str(port)}
    evidence = ROOT / "evidencias" / project
    evidence.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="smartvault-qa-") as tmp:
        env_file = Path(tmp) / "qa.env"
        env_file.write_text("\n".join(f"{k}={v}" for k, v in config.items()) + "\n", encoding="utf-8")
        # Explicit environment wins over any existing .env or parent-shell DB secrets.
        environment = os.environ.copy()
        environment.update(config)
        environment.pop("COMPOSE_FILE", None)
        environment.pop("COMPOSE_PROJECT_NAME", None)
        def compose(*args, capture=False, data=None):
            command = ["docker", "compose", "-p", project, "--env-file", str(env_file), "-f", str(ROOT / "docker-compose.yml"), *args]
            result = subprocess.run(command, cwd=ROOT, env=environment, input=data, stdout=subprocess.PIPE if capture else None, check=True)
            return result.stdout
        print("Proyecto Docker desechable:", project, "Puerto:", port, flush=True)
        api = Api(f"http://127.0.0.1:{port}")
        try:
            compose("up", "-d", "--build", "--remove-orphans")
            fixture = integration_test.run(api)
            # Re-run recovery even when a performance threshold fails, preserving evidence.
            performance_error = None
            if not args.skip_performance:
                try:
                    performance_test.run(api, fixture["alice"], evidence, args.p95_ms)
                except AssertionError as error:
                    performance_error = error
            fixture["alice"] = api.login(integration_test.ALICE, integration_test.PASSWORD)
            recovery_test.run(api, compose, fixture, evidence)
            if performance_error:
                raise performance_error
            print("PASS pruebas QA. Evidencias:", evidence)
        finally:
            # Deletes ONLY the generated QA project's containers and volume.
            compose("down", "-v", "--remove-orphans")


if __name__ == "__main__":
    main()
