"""Bounded read-only local load, stepped stress, and post-stress recovery."""
import csv
import math
import statistics
import time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path


def percentile(values, fraction):
    return sorted(values)[max(0, math.ceil(len(values) * fraction) - 1)]


def measure(api, token, endpoint, concurrency, requests, scenario):
    # Arrange: a fixed number of workers and authenticated reads only.
    assert 1 <= concurrency <= 80 and concurrency <= requests <= 4000
    def one(_):
        start = time.perf_counter()
        status, data = api.call(endpoint, token=token)
        ms = (time.perf_counter() - start) * 1000
        valid = status == 200 and isinstance(data, (dict, list))
        return ms, valid
    # Act: send requests and capture observed latency including network and JSON.
    start = time.perf_counter()
    with ThreadPoolExecutor(max_workers=concurrency) as workers:
        results = list(workers.map(one, range(requests)))
    duration = time.perf_counter() - start
    times = [x[0] for x in results]
    errors = sum(not x[1] for x in results)
    # Assert: observations are well-formed; acceptance thresholds are checked below.
    assert len(times) == requests and all(x >= 0 for x in times)
    return {"scenario": scenario, "endpoint": endpoint, "concurrency": concurrency,
            "requests": requests, "duration_s": round(duration, 3),
            "requests_per_s": round(requests / duration, 2), "mean_ms": round(statistics.mean(times), 2),
            "p95_ms": round(percentile(times, .95), 2), "p99_ms": round(percentile(times, .99), 2),
            "max_ms": round(max(times), 2), "errors": errors, "error_pct": round(errors * 100 / requests, 2)}


def run(api, token, evidence, p95_limit_ms=500):
    # Arrange: warm up and configure repeatable workloads.
    endpoints = ["/api/v1/identity/me", "/api/v1/ledger/balance", "/api/v1/vaults", "/api/v1/reports/summary"]
    evidence = Path(evidence)
    evidence.mkdir(parents=True, exist_ok=True)
    rows = []
    try:
        for endpoint in endpoints:
            api.expect(endpoint, token=token)
            rows.append(measure(api, token, endpoint, 10, 100, "load"))
        for concurrency in (20, 40, 80):
            rows.append(measure(api, token, endpoints[-1], concurrency, concurrency * 5, "stress"))
        # Act: return to normal load after stress.
        rows.append(measure(api, token, endpoints[-1], 10, 100, "post-stress"))
    finally:
        if rows:
            with (evidence / "performance.csv").open("w", newline="", encoding="utf-8") as output:
                writer = csv.DictWriter(output, fieldnames=rows[0].keys())
                writer.writeheader()
                writer.writerows(rows)
    # Assert: baseline and post-stress meet the configured target; stress observes saturation.
    for row in rows:
        print(row)
    baseline = [row for row in rows if row["scenario"] != "stress"]
    assert all(row["errors"] == 0 and row["p95_ms"] <= p95_limit_ms for row in baseline), "Carga normal o recuperación fuera del objetivo p95"
    assert all(row["error_pct"] <= 5 for row in rows if row["scenario"] == "stress"), "Estrés superó 5% de errores"
    print("PASS carga, estrés y recuperación posterior al estrés")
    return rows
