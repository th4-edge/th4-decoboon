#!/usr/bin/env python3
"""按角色动作矩阵探测每个角色的依赖 Provider 状态、权限和候选是否与预期一致。"""
from __future__ import annotations

import argparse
import json
import os
import sys
import http.client
import urllib.parse
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MATRIX = ROOT / "docs/testing/traceability/role-action-provider-matrix.json"
ROLE_PHONES = {
    "Boss": "13910002001",
    "StoreManager": "13910002002",
    "Finance": "13910002003",
    "Designer": "13910002004",
    "Marketer": "13910002005",
    "Pm": "13910002006",
}


def post(api: str, route: str, payload: dict, token: str | None = None) -> tuple[int, dict]:
    parsed = urllib.parse.urlsplit(api.rstrip("/"))
    connection_cls = http.client.HTTPSConnection if parsed.scheme == "https" else http.client.HTTPConnection
    connection = connection_cls(parsed.hostname, parsed.port, timeout=20)
    body = json.dumps(payload).encode("utf-8")
    path = f"{parsed.path.rstrip('/')}/{route}"
    try:
        connection.putrequest("POST", path)
        connection.putheader("content-type", "application/json")
        if token:
            # th4 Provider 按小写 token 头读取；http.client 可保持原始大小写。
            connection.putheader("token", token)
        connection.putheader("content-length", str(len(body)))
        connection.endheaders(body)
        response = connection.getresponse()
        raw = response.read().decode("utf-8")
        return response.status, json.loads(raw) if raw else {}
    finally:
        connection.close()


def items_of(body: dict) -> list:
    data = body.get("data")
    if isinstance(data, list):
        return data
    if isinstance(data, dict):
        return data.get("content") or data.get("list") or []
    return []


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--api", default="http://127.0.0.1:8090/api")
    parser.add_argument("--password", default=os.environ.get("TH4_TEST_PASSWORD", ""))
    parser.add_argument("--output", default="")
    args = parser.parse_args()
    if not args.password:
        print("缺少测试密码，请设置 TH4_TEST_PASSWORD", file=sys.stderr)
        return 2

    matrix = json.loads(MATRIX.read_text(encoding="utf-8"))
    tokens: dict[str, str] = {}
    login_failures = []
    for role, phone in ROLE_PHONES.items():
        status, body = post(args.api, "auth/session/password-login", {"phone": phone, "password": args.password})
        if status == 200 and body.get("success") and body.get("data", {}).get("token"):
            tokens[role] = body["data"]["token"]
        else:
            login_failures.append({"role": role, "httpStatus": status, "error": body.get("errorCode")})

    checks = []
    for action in matrix.get("actions", []):
        for role_row in action.get("roles", []):
            role = role_row.get("role")
            token = tokens.get(role)
            if not token:
                checks.append({
                    "actionId": action.get("id"), "role": role, "provider": None,
                    "pass": False, "reason": "LOGIN_FAILED",
                })
                continue
            for provider in action.get("dependencyProviders", []):
                payload = (action.get("dependencyProbePayloads") or {}).get(provider, {})
                status, body = post(args.api, provider, payload, token)
                item_count = len(items_of(body))
                expected_status = role_row.get("expectedDependencyStatus")
                error_code = body.get("errorCode")
                actual_business_status = 200 if body.get("success") is True else int((error_code or {}).get("code", status))
                passed = actual_business_status == expected_status
                if role_row.get("allowed") is True and expected_status == 200:
                    passed = passed and item_count > 0
                checks.append({
                    "actionId": action.get("id"),
                    "role": role,
                    "allowed": role_row.get("allowed"),
                    "provider": provider,
                    "expectedStatus": expected_status,
                    "httpStatus": status,
                    "businessStatus": actual_business_status,
                    "success": body.get("success"),
                    "itemCount": item_count,
                    "errorCode": error_code,
                    "pass": passed,
                })

    output = {
        "capturedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
        "api": args.api,
        "matrix": str(MATRIX.relative_to(ROOT)),
        "loginFailures": login_failures,
        "checks": checks,
        "summary": {
            "total": len(checks),
            "pass": sum(1 for item in checks if item["pass"]),
            "fail": sum(1 for item in checks if not item["pass"]),
        },
    }
    text = json.dumps(output, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        path = Path(args.output)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
    print(text, end="")
    return 0 if output["summary"]["fail"] == 0 and not login_failures else 1


if __name__ == "__main__":
    raise SystemExit(main())
