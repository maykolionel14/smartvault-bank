"""Arrange, Act, Assert: four services over real HTTP and PostgreSQL."""
from uuid import uuid4
from qa_http import wait_until


ALICE = "qa-alice@example.test"
BOB = "qa-bob@example.test"
PASSWORD = "Qa-demo-only-2026!"
P = "/api/v1/"


def prepare(api):
    # Arrange: wait for gateway and all downstream services, not just TCP readiness.
    wait_until(lambda: api.call(P + "identity/me")[0] == 401 and
               api.call(P + "reports/summary")[0] == 401 and
               api.call(P + "ledger/balance")[0] == 401 and
               api.call(P + "vaults")[0] == 401)
    users = []
    for email in (ALICE, BOB):
        result = api.expect(P + "identity/register", 201, method="POST",
                            body={"email": email, "password": PASSWORD})
        users.append(result["user_id"])
    alice, bob = (api.login(email, PASSWORD) for email in (ALICE, BOB))
    # Assert registration restriction and report isolation before transactions.
    api.expect(P + "identity/register", 403, method="POST",
               body={"email": "not-allowed@example.test", "password": PASSWORD})
    api.expect(P + "reports/summary", 401)
    api.expect("/internal/ledger/report-movements/" + users[0], 404)
    return users, alice, bob


def run(api):
    # Arrange
    users, alice, bob = prepare(api)
    key = str(uuid4())
    credit = {"cents": 500_000, "idempotency_key": key}
    # Act: credit and repeat, create collective vault, contribute, vote and execute.
    first = api.expect(P + "ledger/demo-credit", method="POST", body=credit, token=alice)
    second = api.expect(P + "ledger/demo-credit", method="POST", body=credit, token=alice)
    created = api.expect(P + "vaults", 201, method="POST", token=alice,
                         body={"name": "QA Colectiva", "member_ids": users, "quorum": 2, "milestones": ["Entrega QA"]})
    vault = created["vault_id"]
    milestone = created["milestones"][0]["id"]
    contribution = {"cents": 100_000, "idempotency_key": str(uuid4())}
    api.expect(P + f"vaults/{vault}/contribute", method="POST", body=contribution, token=alice)
    proposal = api.expect(P + f"vaults/{vault}/proposals", 201, method="POST", token=alice,
                          body={"milestone_id": milestone, "beneficiary_id": users[1], "amount_cents": 30_000, "purpose": "Pago QA autorizado"})
    proposal_id = proposal["proposal_id"]
    api.expect(P + f"vaults/proposals/{proposal_id}/execute", 409, method="POST", token=alice)
    for token in (alice, bob):
        api.expect(P + f"vaults/milestones/{milestone}/attest", method="POST", token=token, body={"note": "Validación QA completa"})
        api.expect(P + f"vaults/proposals/{proposal_id}/approve", method="POST", token=token)
    executed = api.expect(P + f"vaults/proposals/{proposal_id}/execute", method="POST", token=alice)
    repeated = api.expect(P + f"vaults/proposals/{proposal_id}/execute", method="POST", token=alice)
    alice_report = api.expect(P + "reports/summary", token=alice)
    bob_report = api.expect(P + "reports/summary", token=bob)
    # Assert balances, accounting, governance, idempotency, and data isolation.
    assert first["transfer_id"] == second["transfer_id"]
    assert executed["transfer_id"] == repeated["transfer_id"] and repeated["idempotent"] is True
    assert api.expect(P + "ledger/balance", token=alice)["balance_cents"] == 400_000
    assert api.expect(P + "ledger/balance", token=bob)["balance_cents"] == 30_000
    assert api.expect(P + f"vaults/{vault}", token=alice)["balance_cents"] == 70_000
    assert api.expect(P + "ledger/verify-integrity", token=alice)["valid"] is True
    assert api.expect(P + f"vaults/{vault}/verify-audit", token=alice)["valid"] is True
    assert (alice_report["income_cents"], alice_report["expense_cents"], alice_report["contribution_cents"], alice_report["net_cents"]) == (500_000, 100_000, 100_000, 400_000)
    assert alice_report["movement_count"] == 2 and bob_report["movement_count"] == 1
    assert bob_report["net_cents"] == 30_000
    assert api.expect(P + "reports/income", token=alice)["total_cents"] == 500_000
    assert api.expect(P + "reports/expenses", token=alice)["total_cents"] == 100_000
    assert api.expect(P + "reports/contributions", token=alice)["movement_count"] == 1
    assert len(api.expect(P + "reports/movements", token=alice)) == 2
    csv = api.expect(P + "reports/export", token=alice)
    assert csv.startswith("transfer_id,created_at,kind,signed_cents,currency")
    assert len(csv.splitlines()) == 3 and ",-100000,COP" in csv
    assert {m["transfer_id"] for m in alice_report["movements"]}.isdisjoint(m["transfer_id"] for m in bob_report["movements"])
    # Concurrent retries are verified separately during recovery.
    print("PASS integración: registro, autenticación, libro mayor, bóvedas, reportes y aislamiento")
    return {"alice": alice, "bob": bob, "credit": credit, "transfer_id": first["transfer_id"], "vault": vault, "report": alice_report}
