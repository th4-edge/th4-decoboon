#!/usr/bin/env python3
"""Validate a ZHUANGDEQING Web product-application iteration packet."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ALLOWED_MODES = {
    "tenant_onboarding",
    "full_chain_simulation",
    "feature_iteration",
    "ux_review",
    "batch_validation",
}
ALLOWED_STATUSES = {
    "discovery",
    "iterating",
    "frozen",
    "consulted",
    "approved",
    "blocked",
}
ALLOWED_LANES = {"product-planning", "development", "acceptance"}
ALLOWED_PLATFORMS = {"Web", "App"}
FULL_CHAIN = {
    "lead",
    "customer",
    "property",
    "measure",
    "design",
    "quote",
    "contract",
    "project",
    "site",
    "change",
    "task",
    "acceptance",
    "rectification",
    "finance",
    "settlement",
}
MIN_DESKTOP_WIDTH = 1024


def fail(errors: list[str], message: str) -> None:
    errors.append(message)


def nonempty(value: object) -> bool:
    if isinstance(value, str):
        return bool(value.strip())
    if isinstance(value, (list, dict)):
        return bool(value)
    return value is not None


def viewport_width(value: object) -> int | None:
    if not isinstance(value, str):
        return None
    match = re.search(r"(\d+)\s*[x×*]\s*(\d+)", value)
    if match:
        return int(match.group(1))
    match = re.fullmatch(r"\s*(\d+)\s*", value)
    if match:
        return int(match.group(1))
    return None


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: validate_iteration_packet.py <packet.json>", file=sys.stderr)
        return 2

    path = Path(sys.argv[1])
    try:
        packet = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"FAIL: cannot read packet: {exc}", file=sys.stderr)
        return 1

    errors: list[str] = []
    for key in (
        "schema_version",
        "packet_id",
        "mode",
        "status",
        "decision_owner",
        "scope",
        "tenant_profiles",
        "scenarios",
        "permission_cases",
        "money_cases",
        "ux_cases",
        "acceptance",
        "evidence_refs",
        "business_questions",
        "affected_lanes",
    ):
        if key not in packet:
            fail(errors, f"missing top-level key: {key}")

    if packet.get("schema_version") != "1.0":
        fail(errors, "schema_version must be 1.0")
    if packet.get("mode") not in ALLOWED_MODES:
        fail(errors, f"invalid mode: {packet.get('mode')}")
    if packet.get("status") not in ALLOWED_STATUSES:
        fail(errors, f"invalid status: {packet.get('status')}")

    tenants = packet.get("tenant_profiles")
    if not isinstance(tenants, list) or len(tenants) < 2:
        fail(errors, "tenant_profiles must contain at least 2 profiles")
    else:
        for index, tenant in enumerate(tenants):
            for key in ("id", "type", "scale", "focus"):
                if not isinstance(tenant, dict) or not nonempty(tenant.get(key)):
                    fail(errors, f"tenant_profiles[{index}].{key} is required")

    scenarios = packet.get("scenarios")
    if not isinstance(scenarios, list) or len(scenarios) < 2:
        fail(errors, "scenarios must contain at least 2 scenarios")
    else:
        scenario_keys = ("id", "user", "trigger", "precondition", "action", "system_next", "exception")
        for index, scenario in enumerate(scenarios):
            for key in scenario_keys:
                if not isinstance(scenario, dict) or not nonempty(scenario.get(key)):
                    fail(errors, f"scenarios[{index}].{key} is required")

    permissions = packet.get("permission_cases")
    if not isinstance(permissions, list) or not permissions:
        fail(errors, "permission_cases must not be empty")
    else:
        if not any(case.get("scope") == "cross_tenant" and case.get("expected") == "deny" for case in permissions if isinstance(case, dict)):
            fail(errors, "permission_cases must include a cross_tenant deny case")

    money_cases = packet.get("money_cases")
    if not isinstance(money_cases, list) or not money_cases:
        fail(errors, "money_cases must include at least one accounting case")
    else:
        for index, case in enumerate(money_cases):
            for key in ("id", "source", "formula_or_snapshot", "missing_value_policy", "cross_page_expectation"):
                if not isinstance(case, dict) or not nonempty(case.get(key)):
                    fail(errors, f"money_cases[{index}].{key} is required")

    ux_cases = packet.get("ux_cases")
    if not isinstance(ux_cases, list) or not ux_cases:
        fail(errors, "ux_cases must not be empty")
    else:
        web_cases = 0
        desktop_cases = 0
        for index, case in enumerate(ux_cases):
            for key in ("id", "platform", "viewport", "role", "path", "expected"):
                if not isinstance(case, dict) or not nonempty(case.get(key)):
                    fail(errors, f"ux_cases[{index}].{key} is required")
            if not isinstance(case, dict):
                continue
            platform = case.get("platform")
            if platform not in ALLOWED_PLATFORMS:
                fail(errors, f"ux_cases[{index}].platform must be one of {sorted(ALLOWED_PLATFORMS)}")
            if platform == "Web":
                web_cases += 1
                width = viewport_width(case.get("viewport"))
                if width is not None and width >= MIN_DESKTOP_WIDTH:
                    desktop_cases += 1
        if web_cases == 0:
            fail(errors, "ux_cases must contain at least one Web case")
        if desktop_cases == 0:
            fail(errors, f"ux_cases must contain at least one Web desktop viewport (>= {MIN_DESKTOP_WIDTH}px wide)")

    acceptance = packet.get("acceptance")
    if not isinstance(acceptance, dict):
        fail(errors, "acceptance must be an object")
    else:
        for key in ("positive", "negative", "evidence", "batch_policy"):
            if not nonempty(acceptance.get(key)):
                fail(errors, f"acceptance.{key} is required")

    questions = packet.get("business_questions")
    if not isinstance(questions, list):
        fail(errors, "business_questions must be a list")
    elif len(questions) > 5:
        fail(errors, "business_questions must not exceed 5 blocking questions")
    if packet.get("status") == "frozen" and questions:
        fail(errors, "frozen packet cannot contain unresolved business_questions")

    lanes = packet.get("affected_lanes")
    if not isinstance(lanes, list) or not lanes:
        fail(errors, "affected_lanes must not be empty")
    else:
        invalid = sorted(set(lanes) - ALLOWED_LANES)
        if invalid:
            fail(errors, f"affected_lanes contains disabled/unknown lanes: {invalid}")

    if packet.get("mode") == "full_chain_simulation":
        chain = packet.get("mainline")
        if not isinstance(chain, list):
            fail(errors, "full_chain_simulation requires mainline list")
        else:
            missing = sorted(FULL_CHAIN - set(chain))
            if missing:
                fail(errors, f"full_chain_simulation missing stages: {missing}")

    if errors:
        for error in errors:
            print(f"FAIL: {error}", file=sys.stderr)
        return 1

    print(
        f"PASS: {packet['packet_id']} "
        f"mode={packet['mode']} status={packet['status']} "
        f"tenants={len(tenants)} scenarios={len(scenarios)} "
        f"web_ux={web_cases} desktop_ux={desktop_cases} "
        f"blocking_questions={len(questions)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
