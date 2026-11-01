#!/usr/bin/env python3
"""End-to-end JSON contract smoke for representative Provider responses."""
from __future__ import annotations

import argparse
import json
import os
import time
import subprocess
from pathlib import Path
from typing import Any

FORBIDDEN_KEYS = {"password", "passwordHash", "salt", "secret", "privateKey"}


def post(base: str, route: str, token: str, payload: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any], float]:
    started = time.perf_counter()
    result = subprocess.run(
        [
            "curl", "-sS", "-X", "POST", f"{base.rstrip('/')}/api/{route}",
            "-H", "Content-Type: application/json", "-H", f"token: {token}",
            "-d", json.dumps(payload, ensure_ascii=False),
            "-w", "\n__META__%{http_code} %{size_download}",
        ],
        capture_output=True,
        timeout=20,
    )
    elapsed_ms = round((time.perf_counter() - started) * 1000, 2)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.decode("utf-8", errors="replace").strip())
    output = result.stdout.decode("utf-8")
    body, marker, meta = output.rpartition("\n__META__")
    if not marker:
        raise RuntimeError("missing curl metadata")
    status_text, bytes_text = meta.split(maxsplit=1)
    parsed = json.loads(body)
    return parsed, {"status": int(status_text), "bytes": int(bytes_text), "elapsedMs": elapsed_ms}, elapsed_ms


def forbidden_paths(value: Any, path: str = "$") -> list[str]:
    found: list[str] = []
    if isinstance(value, dict):
        for key, child in value.items():
            child_path = f"{path}.{key}"
            if key in FORBIDDEN_KEYS:
                found.append(child_path)
            found.extend(forbidden_paths(child, child_path))
    elif isinstance(value, list):
        for index, child in enumerate(value):
            found.extend(forbidden_paths(child, f"{path}[{index}]"))
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default=os.environ.get("TH4_BASE_URL", "http://127.0.0.1:8090"))
    parser.add_argument("--token", default=os.environ.get("TH4_TOKEN"))
    parser.add_argument("--project-id", default=os.environ.get("TH4_PROJECT_ID"))
    parser.add_argument("--customer-id", default=os.environ.get("TH4_CUSTOMER_ID"))
    parser.add_argument("--quote-id", default=os.environ.get("TH4_QUOTE_ID"))
    parser.add_argument("--output", default=os.environ.get("TH4_SMOKE_OUTPUT"))
    args = parser.parse_args()
    if not all([args.token, args.project_id, args.customer_id, args.quote_id]):
        raise SystemExit("token/project-id/customer-id/quote-id are required")

    probes = [
        ("auth/session/info", {}),
        ("customer/customer/overview", {"current": 1, "size": 20}),
        ("customer/customer/page", {"current": 1, "size": 20}),
        ("customer/customer/info", {"customerId": args.customer_id}),
        ("project/project/page", {"current": 1, "size": 20}),
        ("project/project/info", {"projectId": args.project_id}),
        ("quote/info", {"quoteId": args.quote_id}),
        ("project/contract/info", {"projectId": args.project_id}),
        ("material/selection/item/page", {"projectId": args.project_id}),
        ("material/selection/board", {}),
        ("material/plan/workbench", {"projectId": args.project_id}),
        ("project/acceptance/rectify/page", {}),
        ("finance/receivable/page", {"current": 1, "size": 20}),
        ("finance/payable/page", {"current": 1, "size": 20}),
        ("finance/ledger/page", {"current": 1, "size": 20}),
        ("finance/project/report", {"projectId": args.project_id}),
        ("statistic/boss/workbench", {"eventLimit": 6}),
    ]
    results = []
    failed = False
    for route, payload in probes:
        record: dict[str, Any] = {"route": route}
        try:
            response, http, _ = post(args.base, route, args.token, payload)
            secrets = forbidden_paths(response)
            record.update(http)
            record.update({
                "success": response.get("success") is True,
                "errorCode": response.get("errorCode"),
                "dataType": type(response.get("data")).__name__,
                "dataKeys": sorted((response.get("data") or {}).keys())[:30] if isinstance(response.get("data"), dict) else [],
                "forbiddenKeyCount": len(secrets),
                "forbiddenPaths": secrets[:10],
            })
            if not record["success"] or secrets:
                failed = True
        except Exception as error:  # noqa: BLE001 - smoke evidence must capture every failure
            failed = True
            record.update({"success": False, "error": f"{type(error).__name__}: {error}"})
        results.append(record)

    report = {"base": args.base, "routeCount": len(probes), "failed": failed, "results": results}
    text = json.dumps(report, ensure_ascii=False, indent=2)
    if args.output:
        Path(args.output).write_text(text + "\n", encoding="utf-8")
    print(text)
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
