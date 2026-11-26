"""Fault recovery and real PostgreSQL backup/restore on an isolated QA project."""
import json
import time
from pathlib import Path
from uuid import uuid4
from concurrent.futures import ThreadPoolExecutor
from qa_http import wait_until

P = "/api/v1/"


def run(api, compose, fixture, evidence):
    # Arrange: baseline known, isolated DB and pre-existing idempotency key.
    token = fixture["alice"]
    report_before = api.expect(P + "reports/summary", token=token)
    evidence = Path(evidence)
    evidence.mkdir(parents=True, exist_ok=True)
    # Act: stop only the QA Ledger container, then restore availability.
    compose("stop", "ledger")
    try:
        status, _ = api.call(P + "reports/summary", token=token)
        assert status == 503, f"Reporte durante caída: esperado 503, recibido {status}"
    finally:
        started = time.monotonic()
        compose("start", "ledger")
    wait_until(lambda: api.call(P + "reports/summary", token=token)[0] == 200, timeout=120)
    rto = time.monotonic() - started
    # Assert: reports and data are the same, and replay does not mint funds twice.
    report_after = api.expect(P + "reports/summary", token=token)
    assert report_before == report_after
    with ThreadPoolExecutor(max_workers=8) as pool:
        retries = list(pool.map(lambda _: api.expect(P + "ledger/demo-credit", method="POST", body=fixture["credit"], token=token), range(16)))
    assert all(item["transfer_id"] == fixture["transfer_id"] for item in retries)
    assert api.expect(P + "reports/summary", token=token) == report_before
    assert api.expect(P + "ledger/verify-integrity", token=token)["valid"] is True
    # Arrange: take a PostgreSQL custom-format backup after quiescent test writes.
    # Only in memory; avoids storing password hashes or account rows in the evidence ZIP.
    dump = compose("exec", "-T", "postgres", "pg_dump", "-U", "smartvault", "-Fc", "smartvault", capture=True)
    assert dump.startswith(b"PGDMP"), "Respaldo inválido"
    target = "restore_" + uuid4().hex[:12]
    tables = ("identity_users", "ledger_wallets", "ledger_transfers", "ledger_entries", "vaults_aggregates")
    # Obtain actual vault table name from the original entity; kept in sync with source.
    def fingerprint(database, table):
        query = f"select count(*) || ':' || md5(coalesce(string_agg(to_jsonb(t)::text, E'\n' order by to_jsonb(t)::text), '')) from {table} t;"
        return compose("exec", "-T", "postgres", "psql", "-U", "smartvault", "-d", database, "-At", "-v", "ON_ERROR_STOP=1", "-c", query, capture=True).decode().strip()
    compose("exec", "-T", "postgres", "createdb", "-U", "smartvault", target)
    try:
        # Act: restore into a new database, never over the source.
        compose("exec", "-T", "postgres", "pg_restore", "-U", "smartvault", "-d", target, "--exit-on-error", "--no-owner", data=dump)
        # Assert: compare each business table row count and content fingerprint.
        comparisons = {table: fingerprint("smartvault", table) == fingerprint(target, table) for table in tables}
        assert all(comparisons.values()), comparisons
    finally:
        compose("exec", "-T", "postgres", "dropdb", "-U", "smartvault", target)
    result = {"ledger_unavailable_http": 503, "service_recovery_seconds": round(rto, 3),
              "report_unchanged": True, "idempotent_retries": len(retries), "backup_bytes": len(dump),
              "restored_tables_match": comparisons}
    (evidence / "recovery.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    print("PASS recuperación del servicio y restauración de datos", result)
