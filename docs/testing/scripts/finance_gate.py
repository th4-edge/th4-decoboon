#!/usr/bin/env python3
"""财务账务专项测试门禁：校验用例、Provider、页面、规则和覆盖。"""
from __future__ import annotations

import csv
import json
import re
import sys
from collections import Counter
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TESTING = ROOT / "docs/testing"
CASES = TESTING / "cases/finance-accounting-gate-v0.1.json"
ORACLE = TESTING / "traceability/finance-accounting-gate-oracle.json"
MATRIX = TESTING / "traceability/finance-accounting-gate-matrix.csv"
SUMMARY = TESTING / "traceability/finance-accounting-gate-summary.json"
CATALOG = TESTING / "cases/finance-accounting-gate-v0.1.md"
WEB_ROUTES = ROOT.parent.parent / "前端代码/th4-decoboon-web/src/roles/routes.ts"
APP_CONFIG = ROOT.parent.parent / "前端代码/th4-decoboon-app/src/config/index.ts"
SERVICE = re.compile(r'@Service\("([^"]+)"\)')
LEAF = re.compile(r"leaf\('([^']+)'")
REQUIRED_STRINGS = ["id", "title", "priority", "area", "fixture", "executionMode", "status"]
REQUIRED_ARRAYS = ["roles", "surfaces", "scenarioTypes", "businessRuleIds", "objectTypes", "preconditions", "steps", "expectedPage", "expectedState", "expectedInvariants", "forbiddenOutcomes", "providerRoutes", "pageTargets", "evidenceRequired"]
VALID_PRIORITY = {"P0", "P1", "P2", "P3"}
VALID_STATUS = {"READY", "BLOCKED", "STALE", "UNMAPPED"}
VALID_SURFACES = {"Web", "App", "Owner", "API"}
VALID_MODES = {"manual", "e2e", "contract", "probe", "hybrid", "review"}
REQUIRED_CASE_FAMILIES = {
    "PAYROLL-ATTENDANCE", "PAYROLL-PENDING", "ATTENDANCE-STATE", "FINANCE-STORE",
    "SALARY-PAYABLE", "SALARY-PAYMENT", "PAID-HISTORY", "AR-RECEIPT",
    "AP-MATERIAL", "COST-PROFIT", "CROSS-TENANT", "VOUCHER-AUDIT"
}
REQUIRED_FORM_CASE_EVIDENCE = {
    "TC-FIN-ATTENDANCE-FORM-RESET-008": {
        "page_path", "before_after_screenshot", "interaction_sequence", "state_readback"
    },
    "TC-FIN-ATTENDANCE-FORM-RACE-023": {
        "page_path", "before_after_screenshot", "interaction_sequence", "network_trace"
    },
    "TC-FIN-FORM-REOPEN-024": {
        "page_path", "before_after_screenshot", "interaction_sequence", "state_readback"
    },
    "TC-FIN-ATTENDANCE-FORM-APP-025": {
        "page_path", "before_after_screenshot", "interaction_sequence", "state_readback"
    },
    "TC-FIN-COMMISSION-FORM-RESET-026": {
        "page_path", "before_after_screenshot", "interaction_sequence", "state_readback"
    },
}
FORM_CASE_IDS = set(REQUIRED_FORM_CASE_EVIDENCE)

def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))

def provider_catalog() -> set[str]:
    routes: set[str] = set()
    for path in (ROOT / "src/main/java").rglob("*.java"):
        routes.update(SERVICE.findall(path.read_text(encoding="utf-8", errors="ignore")))
    return routes

def page_catalog() -> tuple[set[str], str]:
    leaves = set(LEAF.findall(WEB_ROUTES.read_text(encoding="utf-8"))) if WEB_ROUTES.exists() else set()
    app_text = APP_CONFIG.read_text(encoding="utf-8") if APP_CONFIG.exists() else ""
    return leaves, app_text

def validate_doc(doc: dict, oracle: dict, providers: set[str], web_leaves: set[str], app_text: str) -> list[str]:
    errors: list[str] = []
    if doc.get("oracleId") != oracle.get("oracleId"):
        errors.append("suite oracleId 与 finance oracle-lock 不一致")
    cases = doc.get("cases")
    if not isinstance(cases, list) or not cases:
        return ["cases 必须是非空数组"]
    ids: set[str] = set()
    rule_ids = set(oracle.get("rules", {}))
    for index, case in enumerate(cases, 1):
        label = str(case.get("id") or f"case[{index}]")
        for key in REQUIRED_STRINGS:
            if not isinstance(case.get(key), str) or not case.get(key, "").strip():
                errors.append(f"{label}: 缺少非空字段 {key}")
        for key in REQUIRED_ARRAYS:
            if not isinstance(case.get(key), list) or not case.get(key):
                errors.append(f"{label}: {key} 必须是非空数组")
        cid = str(case.get("id", ""))
        if not re.fullmatch(r"TC-[A-Z0-9-]+", cid):
            errors.append(f"{label}: ID 格式不合法")
        if cid in ids:
            errors.append(f"{label}: ID 重复")
        ids.add(cid)
        if case.get("priority") not in VALID_PRIORITY:
            errors.append(f"{label}: priority 不合法")
        if case.get("status") not in VALID_STATUS:
            errors.append(f"{label}: status 不合法")
        if case.get("executionMode") not in VALID_MODES:
            errors.append(f"{label}: executionMode 不合法")
        if case.get("area") != "FINANCE":
            errors.append(f"{label}: area 必须为 FINANCE")
        for surface in case.get("surfaces", []):
            if surface not in VALID_SURFACES:
                errors.append(f"{label}: 非法端面 {surface}")
        for rule in case.get("businessRuleIds", []):
            if rule not in rule_ids:
                errors.append(f"{label}: 未在财务 oracle 注册业务规则 {rule}")
        for route in case.get("providerRoutes", []):
            if route not in providers:
                errors.append(f"{label}: Provider 不存在 {route}")
        for target in case.get("pageTargets", []):
            if target.startswith("web:/finance/"):
                leaf = target.removeprefix("web:/finance/")
                if leaf not in web_leaves:
                    errors.append(f"{label}: Web 页面不存在 {target}")
            elif target.startswith("web:/account/"):
                leaf = target.removeprefix("web:/account/")
                if leaf not in web_leaves:
                    errors.append(f"{label}: Web 账户页面不存在 {target}")
            elif target.startswith("app:/"):
                if target.removeprefix("app:") not in app_text:
                    errors.append(f"{label}: App 页面不存在 {target}")
            else:
                errors.append(f"{label}: 页面目标格式不合法 {target}")
        if any(s in {"Web", "App", "Owner"} for s in case.get("surfaces", [])) and not case.get("pageTargets"):
            errors.append(f"{label}: 声明页面端但缺少 pageTargets")
        surfaces = set(case.get("surfaces", []))
        targets = [str(target) for target in case.get("pageTargets", [])]
        for surface, prefix in (("Web", "web:/"), ("App", "app:/"), ("Owner", "owner:/")):
            if surface in surfaces and not any(target.startswith(prefix) for target in targets):
                errors.append(f"{label}: 声明 {surface} 端但缺少 {prefix} 页面目标")
        evidence_required = set(case.get("evidenceRequired", []))
        required_evidence = REQUIRED_FORM_CASE_EVIDENCE.get(cid)
        if required_evidence:
            missing_evidence = sorted(required_evidence - evidence_required)
            if missing_evidence:
                errors.append(f"{label}: 表单生命周期证据缺失 {', '.join(missing_evidence)}")
            if "parity" in case.get("scenarioTypes", []) and "web_app_parity" not in evidence_required:
                errors.append(f"{label}: 声明 parity 但缺少 web_app_parity 证据")
        if case.get("priority") == "P0" and case.get("status") == "BLOCKED":
            errors.append(f"{label}: P0 不能以 BLOCKED 进入发布门禁")
    names = " ".join(c["id"] for c in cases)
    for form_case_id in sorted(FORM_CASE_IDS - ids):
        errors.append(f"缺少强制表单生命周期用例: {form_case_id}")
    for family in sorted(REQUIRED_CASE_FAMILIES):
        if family not in names:
            errors.append(f"缺少强制场景族: {family}")
    if sum(c.get("priority") == "P0" for c in cases) < 10:
        errors.append("P0 用例少于 10 条，不满足财务门禁最低覆盖")
    return errors

def build_outputs(doc: dict, oracle: dict) -> None:
    rows = []
    for c in doc["cases"]:
        rows.append({
            "case_id": c["id"], "priority": c["priority"], "roles": "|".join(c["roles"]),
            "surfaces": "|".join(c["surfaces"]), "scenario_types": "|".join(c["scenarioTypes"]),
            "rules": "|".join(c["businessRuleIds"]), "objects": "|".join(c["objectTypes"]),
            "providers": "|".join(c["providerRoutes"]), "pages": "|".join(c["pageTargets"]),
            "fixture": c["fixture"], "execution_mode": c["executionMode"], "status": c["status"],
            "evidence": "|".join(c["evidenceRequired"]),
        })
    MATRIX.parent.mkdir(parents=True, exist_ok=True)
    with MATRIX.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=list(rows[0]))
        writer.writeheader(); writer.writerows(rows)
    priorities = Counter(c["priority"] for c in doc["cases"])
    surfaces = Counter(s for c in doc["cases"] for s in c["surfaces"])
    summary = {
        "generatedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
        "suiteId": doc["suiteId"], "oracleId": oracle["oracleId"],
        "businessCommit": oracle["businessCommit"], "totalCases": len(doc["cases"]),
        "priority": dict(sorted(priorities.items())), "surfaces": dict(sorted(surfaces.items())),
        "scenarioTypes": dict(sorted(Counter(t for c in doc["cases"] for t in c["scenarioTypes"]).items())),
        "providers": sorted({p for c in doc["cases"] for p in c["providerRoutes"]}),
        "gateRule": "P0=100%; P1=no unaccepted blocker; accounting reconciliation required"
    }
    SUMMARY.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lines = [
        "# 财务账务业务域专项用例 V0.1", "",
        f"- suiteId：`{doc['suiteId']}`",
        f"- oracleId：`{doc['oracleId']}`",
        f"- 用例数：{len(doc['cases'])}",
        f"- P0：{priorities.get('P0', 0)}",
        f"- 生成命令：`python3 docs/testing/scripts/finance_gate.py validate`", "",
        "| 用例 | 优先级 | 角色 | 端面 | 场景 |", "| --- | --- | --- | --- | --- |",
    ]
    for c in doc["cases"]:
        lines.append(f"| {c['id']} | {c['priority']} | {', '.join(c['roles'])} | {', '.join(c['surfaces'])} | {', '.join(c['scenarioTypes'])} |")
    lines += ["", "## 用例明细", ""]
    for c in doc["cases"]:
        lines += [
            f"### {c['id']} · {c['title']}", "",
            f"- 优先级：`{c['priority']}`",
            f"- 规则：{', '.join(f'`{x}`' for x in c['businessRuleIds'])}",
            f"- 夹具：`{c['fixture']}`",
            f"- Provider：{', '.join(f'`{x}`' for x in c['providerRoutes'])}",
            f"- 页面：{', '.join(f'`{x}`' for x in c['pageTargets']) or '无'}",
            "- 预期页面：" + "；".join(c["expectedPage"]),
            "- 预期状态：" + "；".join(c["expectedState"]),
            "- 不变量：" + "；".join(c["expectedInvariants"]),
            "- 禁止结果：" + "；".join(c["forbiddenOutcomes"]), "",
        ]
    CATALOG.write_text("\n".join(lines) + "\n", encoding="utf-8")

def self_test(doc: dict, oracle: dict, providers: set[str], web_leaves: set[str], app_text: str) -> int:
    base = json.loads(json.dumps(doc))
    checks = []
    broken = json.loads(json.dumps(base)); broken["cases"][1]["id"] = broken["cases"][0]["id"]
    checks.append(("duplicate_id", validate_doc(broken, oracle, providers, web_leaves, app_text)))
    broken = json.loads(json.dumps(base)); broken["cases"][0]["businessRuleIds"] = ["BR-999"]
    checks.append(("unknown_rule", validate_doc(broken, oracle, providers, web_leaves, app_text)))
    broken = json.loads(json.dumps(base)); broken["cases"][0]["providerRoutes"] = ["finance/not/exist"]
    checks.append(("unknown_provider", validate_doc(broken, oracle, providers, web_leaves, app_text)))
    broken = json.loads(json.dumps(base))
    form_case = next(case for case in broken["cases"] if case["id"] == "TC-FIN-FORM-REOPEN-024")
    form_case["evidenceRequired"].remove("interaction_sequence")
    checks.append(("missing_form_interaction_evidence", validate_doc(broken, oracle, providers, web_leaves, app_text)))
    broken = json.loads(json.dumps(base))
    broken["cases"] = [case for case in broken["cases"] if case["id"] != "TC-FIN-ATTENDANCE-FORM-APP-025"]
    checks.append(("missing_form_case", validate_doc(broken, oracle, providers, web_leaves, app_text)))
    broken = json.loads(json.dumps(base))
    form_case = next(case for case in broken["cases"] if case["id"] == "TC-FIN-FORM-REOPEN-024")
    form_case["pageTargets"] = [target for target in form_case["pageTargets"] if not target.startswith("app:/")]
    checks.append(("surface_without_page_target", validate_doc(broken, oracle, providers, web_leaves, app_text)))
    failed = [name for name, errors in checks if not errors]
    if failed:
        print("SELFTEST FAIL: " + ", ".join(failed), file=sys.stderr)
        return 1
    print(f"SELFTEST PASS: {len(checks)} negative cases detected")
    return 0

def main() -> int:
    command = sys.argv[1] if len(sys.argv) > 1 else "validate"
    if command not in {"validate", "self-test"}:
        print("usage: finance_gate.py [validate|self-test]", file=sys.stderr)
        return 2
    doc, oracle = load(CASES), load(ORACLE)
    providers = provider_catalog(); web_leaves, app_text = page_catalog()
    errors = validate_doc(doc, oracle, providers, web_leaves, app_text)
    if errors:
        for error in errors:
            print(f"ERROR {error}", file=sys.stderr)
        print(f"finance-gate validate FAIL errors={len(errors)}", file=sys.stderr)
        return 1
    if command == "self-test":
        return self_test(doc, oracle, providers, web_leaves, app_text)
    build_outputs(doc, oracle)
    p0 = sum(c["priority"] == "P0" for c in doc["cases"])
    print(f"finance-gate validate PASS cases={len(doc['cases'])} p0={p0} providers={len(providers)}")
    print(f"matrix={MATRIX.relative_to(ROOT)} summary={SUMMARY.relative_to(ROOT)}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
