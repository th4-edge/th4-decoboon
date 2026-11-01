#!/usr/bin/env python3
"""校验装得清测试资产并生成追溯矩阵。仅使用 Python 标准库。"""
from __future__ import annotations

import argparse
import csv
import json
import re
import sys
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TESTING = ROOT / "docs/testing"
CASES_FILE = TESTING / "cases/core-chain-v0.1.json"
ORACLE_FILE = TESTING / "traceability/oracle-lock.json"
COVERAGE_FILE = TESTING / "traceability/coverage-map.json"
UNMAPPED_FILE = TESTING / "traceability/unmapped.json"
MATRIX_FILE = TESTING / "traceability/matrix.csv"
SUMMARY_FILE = TESTING / "traceability/coverage-summary.json"
ACTION_MATRIX_FILE = TESTING / "traceability/role-action-provider-matrix.json"
CATALOG_FILE = TESTING / "cases/core-chain-v0.1.md"
BACKEND_JAVA = ROOT / "src/main/java"
WORKSPACE = ROOT.parent.parent
WEB_ROUTES = WORKSPACE / "前端代码/th4-decoboon-web/src/roles/routes.ts"
APP_CONFIG = WORKSPACE / "前端代码/th4-decoboon-app/src/config/index.ts"

CASE_ID = re.compile(r"^TC-[A-Z0-9-]+$")
RULE_ID = re.compile(r"^(?:(?:BR|DEC|SCN|OBJ|MET)-\d{3}|USER-DEC-\d{8}-\d{3})$")
ACTION_ID = re.compile(r"^ACT-[A-Z0-9-]+$")
EMPLOYEE_ROLES = {"Boss", "StoreManager", "Designer", "Marketer", "Pm", "Finance", "TeamLeader"}
SERVICE = re.compile(r'@Service\("([^"]+)"\)')
WEB_LEAF = re.compile(r"leaf\('([^']+)'")
APP_PATH = re.compile(r"['\"](\/(?:pages|subpackages)\/[^'\"]+)['\"]")

REQUIRED_ARRAYS = [
    "roles", "surfaces", "scenarioTypes", "businessRuleIds", "objectTypes", "preconditions",
    "steps", "expectedPage", "expectedState", "expectedInvariants", "forbiddenOutcomes",
    "providerRoutes", "pageTargets", "evidenceRequired",
]
REQUIRED_STRINGS = ["id", "title", "priority", "area", "fixture", "executionMode", "status"]


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def provider_catalog() -> set[str]:
    routes: set[str] = set()
    for path in BACKEND_JAVA.rglob("*.java"):
        routes.update(SERVICE.findall(path.read_text(encoding="utf-8", errors="ignore")))
    return routes


def page_catalog() -> tuple[set[str], set[str]]:
    web_leaves: set[str] = set()
    app_paths: set[str] = set()
    if WEB_ROUTES.exists():
        web_leaves.update(WEB_LEAF.findall(WEB_ROUTES.read_text(encoding="utf-8")))
    if APP_CONFIG.exists():
        app_paths.update(APP_PATH.findall(APP_CONFIG.read_text(encoding="utf-8")))
    return web_leaves, app_paths


def validate_cases(doc: dict, oracle: dict, providers: set[str], web_leaves: set[str], app_paths: set[str]) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    cases = doc.get("cases")
    if not isinstance(cases, list) or not cases:
        return ["cases 必须是非空数组"], warnings
    if doc.get("oracleId") != oracle.get("oracleId"):
        errors.append(f"用例库 oracleId={doc.get('oracleId')} 与 oracle-lock={oracle.get('oracleId')} 不一致")
    rules = oracle.get("rules", {})
    ids: set[str] = set()
    for index, case in enumerate(cases, start=1):
        prefix = f"case[{index}]"
        if not isinstance(case, dict):
            errors.append(f"{prefix} 不是对象")
            continue
        for field in REQUIRED_STRINGS:
            if not isinstance(case.get(field), str) or not case.get(field, "").strip():
                errors.append(f"{prefix}.{field} 缺失或不是非空字符串")
        for field in REQUIRED_ARRAYS:
            value = case.get(field)
            if not isinstance(value, list) or not value:
                errors.append(f"{prefix}.{field} 必须是非空数组")
        cid = str(case.get("id", ""))
        prefix = cid or prefix
        if cid and not CASE_ID.match(cid):
            errors.append(f"{prefix} ID 格式不合法")
        if cid in ids:
            errors.append(f"{prefix} ID 重复")
        ids.add(cid)
        if case.get("priority") not in {"P0", "P1", "P2", "P3"}:
            errors.append(f"{prefix} priority 不合法")
        if case.get("status") not in {"READY", "BLOCKED", "STALE", "UNMAPPED"}:
            errors.append(f"{prefix} status 不合法")
        if case.get("executionMode") not in {"manual", "e2e", "contract", "probe", "hybrid", "review"}:
            errors.append(f"{prefix} executionMode 不合法")
        rule_ids = case.get("businessRuleIds") or []
        for rule_id in rule_ids:
            if not RULE_ID.match(str(rule_id)):
                errors.append(f"{prefix} 规则ID格式不合法: {rule_id}")
            elif rule_id not in rules:
                errors.append(f"{prefix} 引用了 oracle-lock 中不存在的规则: {rule_id}")
        if not rule_ids:
            errors.append(f"{prefix} 没有业务规则，不能作为可执行测试")
        providers_for_case = case.get("providerRoutes") or []
        if not providers_for_case:
            errors.append(f"{prefix} 没有 Provider 路由")
        for route in providers_for_case:
            if route not in providers:
                errors.append(f"{prefix} 引用了不存在的 Provider: {route}")
        surfaces = set(case.get("surfaces") or [])
        targets = case.get("pageTargets") or []
        prefix_map = {"Web": "web:", "App": "app:", "Owner": "owner:"}
        for surface, prefix_value in prefix_map.items():
            if surface in surfaces and not any(str(target).startswith(prefix_value) for target in targets):
                errors.append(f"{prefix} 声明了 {surface}，但 pageTargets 缺少 {prefix_value} 入口")
        for target in targets:
            target = str(target)
            if target.startswith('web:'):
                path = target[4:].split('?', 1)[0]
                if not any(path == f'/{leaf}' or path.endswith(f'/{leaf}') for leaf in web_leaves):
                    errors.append(f"{prefix} web 页面目标未在 routes.ts 中定位: {target}")
            elif target.startswith('app:') or target.startswith('owner:'):
                path = target.split(':', 1)[1].split('?', 1)[0]
                if app_paths and path not in app_paths:
                    errors.append(f"{prefix} 小程序页面目标未在 config/index.ts 中定位: {target}")
        if "API" in surfaces and not providers_for_case:
            errors.append(f"{prefix} API 用例缺少 Provider")
        if not case.get("forbiddenOutcomes"):
            errors.append(f"{prefix} 缺少禁止结果")
        if not case.get("evidenceRequired"):
            errors.append(f"{prefix} 缺少证据要求")
    return errors, warnings


def validate_decision_coverage(oracle: dict, doc: dict, unmapped: dict) -> list[str]:
    errors: list[str] = []
    used = {rule for case in doc.get("cases", []) for rule in (case.get("businessRuleIds") or [])}
    registered_gaps = {item.get("id") for item in unmapped.get("items", [])}
    for rule_id, meta in sorted(oracle.get("rules", {}).items()):
        if not rule_id.startswith("USER-DEC-") or meta.get("status") != "approved":
            continue
        if rule_id not in used and f"UNMAPPED-{rule_id}" not in registered_gaps:
            errors.append(f"已批准业务决策未映射用例或未登记缺口: {rule_id}")
    return errors


def validate_action_matrix(matrix: dict, doc: dict, oracle: dict, providers: set[str]) -> list[str]:
    errors: list[str] = []
    cases = {case.get("id"): case for case in doc.get("cases", [])}
    rules = oracle.get("rules", {})
    actions = matrix.get("actions")
    if not isinstance(actions, list) or not actions:
        return ["role-action-provider-matrix.actions 必须是非空数组"]
    ids: set[str] = set()
    for index, action in enumerate(actions, start=1):
        aid = str(action.get("id", ""))
        prefix = aid or f"action[{index}]"
        if not ACTION_ID.match(aid):
            errors.append(f"{prefix} ID格式不合法")
        if aid in ids:
            errors.append(f"{prefix} ID重复")
        ids.add(aid)
        case_ids = action.get("caseIds") or []
        if not case_ids:
            errors.append(f"{prefix} 缺少 caseIds")
        for case_id in case_ids:
            case = cases.get(case_id)
            if case is None:
                errors.append(f"{prefix} 引用了不存在的用例: {case_id}")
                continue
            case_providers = set(case.get("providerRoutes") or [])
            required_providers = {action.get("primaryProvider"), *(action.get("dependencyProviders") or [])}
            for provider in sorted(item for item in required_providers if item):
                if provider not in case_providers:
                    errors.append(f"{prefix} {case_id} 未声明动作Provider: {provider}")
            case_rules = set(case.get("businessRuleIds") or [])
            for decision_id in action.get("decisionIds") or []:
                if decision_id not in case_rules:
                    errors.append(f"{prefix} {case_id} 未引用动作决策: {decision_id}")
            case_targets = set(case.get("pageTargets") or [])
            for target in action.get("pageTargets") or []:
                if target not in case_targets:
                    errors.append(f"{prefix} {case_id} 未声明动作页面: {target}")
        decision_ids = action.get("decisionIds") or []
        if not decision_ids:
            errors.append(f"{prefix} 缺少 decisionIds")
        for decision_id in decision_ids:
            if decision_id not in rules:
                errors.append(f"{prefix} 引用了 oracle-lock 中不存在的决策: {decision_id}")
        primary = action.get("primaryProvider")
        dependencies = action.get("dependencyProviders") or []
        if not primary or primary not in providers:
            errors.append(f"{prefix} primaryProvider不存在: {primary}")
        if not isinstance(dependencies, list) or not dependencies:
            errors.append(f"{prefix} dependencyProviders 必须是非空数组")
        probe_payloads = action.get("dependencyProbePayloads") or {}
        for provider in dependencies:
            if provider not in providers:
                errors.append(f"{prefix} dependencyProvider不存在: {provider}")
            if provider not in probe_payloads or not isinstance(probe_payloads.get(provider), dict):
                errors.append(f"{prefix} 缺少依赖Probe参数: {provider}")
        if not action.get("control"):
            errors.append(f"{prefix} 缺少 control")
        if not action.get("pageTargets"):
            errors.append(f"{prefix} 缺少 pageTargets")
        roles = action.get("roles") or []
        if not roles:
            errors.append(f"{prefix} 缺少角色矩阵")
        allowed_roles = [row for row in roles if row.get("allowed") is True]
        denied_roles = [row for row in roles if row.get("allowed") is False]
        if not allowed_roles:
            errors.append(f"{prefix} 至少需要一个 allowed=true 的角色")
        if not denied_roles:
            errors.append(f"{prefix} 至少需要一个 allowed=false 的角色")
        seen_roles: set[str] = set()
        for row in roles:
            role = row.get("role")
            if role not in EMPLOYEE_ROLES:
                errors.append(f"{prefix} 非法角色: {role}")
            if role in seen_roles:
                errors.append(f"{prefix} 角色重复: {role}")
            seen_roles.add(role)
            scopes = row.get("dataScopes")
            if not isinstance(scopes, list):
                errors.append(f"{prefix}.{role} dataScopes 必须是数组")
            if row.get("allowed") is True and not scopes:
                errors.append(f"{prefix}.{role} 允许执行但未声明数据范围")
            for field in ("expectedDependencyStatus", "expectedPrimaryStatus"):
                if not isinstance(row.get(field), int):
                    errors.append(f"{prefix}.{role}.{field} 必须是HTTP状态码")
            if row.get("allowed") is True:
                if row.get("expectedDependencyStatus") != 200 or row.get("expectedPrimaryStatus") != 200:
                    errors.append(f"{prefix}.{role} allowed=true 时主/依赖Provider预期必须为200")
            elif row.get("allowed") is False and isinstance(row.get("expectedPrimaryStatus"), int) and row.get("expectedPrimaryStatus") < 400:
                errors.append(f"{prefix}.{role} allowed=false 时主Provider预期必须为4xx/5xx")
    return errors


def validate_assets() -> int:
    oracle = load_json(ORACLE_FILE)
    doc = load_json(CASES_FILE)
    providers = provider_catalog()
    web_leaves, app_paths = page_catalog()
    errors, warnings = validate_cases(doc, oracle, providers, web_leaves, app_paths)
    action_matrix = load_json(ACTION_MATRIX_FILE)
    errors.extend(validate_action_matrix(action_matrix, doc, oracle, providers))

    coverage = load_json(COVERAGE_FILE)
    domain_counts = Counter(case.get("area") for case in doc.get("cases", []))
    mapped_domains = {item.get("domain"): item for item in coverage.get("domains", [])}
    for area, count in sorted(domain_counts.items()):
        row = mapped_domains.get(area)
        if row is None:
            errors.append(f"coverage-map 缺少业务区域: {area}")
        elif row.get("cases") != count:
            errors.append(f"coverage-map 区域 {area} 声明 {row.get('cases')} 条，实际 {count} 条")
    for area, row in mapped_domains.items():
        if row.get("v01") == "IN_SCOPE" and row.get("cases", 0) == 0:
            errors.append(f"coverage-map 中 IN_SCOPE 区域 {area} 没有用例")

    unmapped = load_json(UNMAPPED_FILE)
    errors.extend(validate_decision_coverage(oracle, doc, unmapped))
    for item in unmapped.get("items", []):
        if item.get("status") not in {"BLOCKED", "PARTIAL", "NOT_TESTED"}:
            errors.append(f"unmapped {item.get('id')} 状态不合法")
        if not item.get("reason") or not item.get("nextAction"):
            errors.append(f"unmapped {item.get('id')} 缺少原因或下一步")

    decision_count = sum(1 for rule_id in oracle.get("rules", {}) if rule_id.startswith("USER-DEC-"))
    print(f"cases={len(doc.get('cases', []))} actions={len(action_matrix.get('actions', []))} providers={len(providers)} webRoutes={len(web_leaves)} appRoutes={len(app_paths)} rules={len(oracle.get('rules', {}))} decisions={decision_count}")
    print(f"areas={dict(sorted(domain_counts.items()))}")
    for warning in warnings:
        print(f"WARN {warning}", file=sys.stderr)
    if errors:
        for error in errors:
            print(f"ERROR {error}", file=sys.stderr)
        print(f"validate FAIL errors={len(errors)}", file=sys.stderr)
        return 1
    print("validate PASS")
    return 0


def build_trace() -> int:
    oracle = load_json(ORACLE_FILE)
    doc = load_json(CASES_FILE)
    providers = provider_catalog()
    web_leaves, app_paths = page_catalog()
    errors, _ = validate_cases(doc, oracle, providers, web_leaves, app_paths)
    action_matrix = load_json(ACTION_MATRIX_FILE)
    errors.extend(validate_action_matrix(action_matrix, doc, oracle, providers))
    if errors:
        for error in errors:
            print(f"ERROR {error}", file=sys.stderr)
        return 1

    rules = oracle.get("rules", {})
    rows = []
    by_area = defaultdict(lambda: {"cases": 0, "P0": 0, "P1": 0, "P2": 0, "P3": 0, "roles": set(), "surfaces": set(), "rules": set()})
    by_role = Counter()
    by_surface = Counter()
    rule_usage = Counter()
    for case in doc["cases"]:
        area = case["area"]
        mapped = bool(case.get("providerRoutes")) and (
            not any(s in {"Web", "App", "Owner"} for s in case["surfaces"]) or bool(case.get("pageTargets"))
        )
        row = {
            "case_id": case["id"],
            "priority": case["priority"],
            "area": area,
            "roles": "|".join(case["roles"]),
            "surfaces": "|".join(case["surfaces"]),
            "scenario_types": "|".join(case["scenarioTypes"]),
            "business_rules": "|".join(case["businessRuleIds"]),
            "objects": "|".join(case["objectTypes"]),
            "providers": "|".join(case["providerRoutes"]),
            "page_targets": "|".join(case["pageTargets"]),
            "fixture": case["fixture"],
            "evidence": "|".join(case["evidenceRequired"]),
            "case_status": case["status"],
            "mapping_status": "MAPPED" if mapped else "UNMAPPED",
        }
        rows.append(row)
        summary = by_area[area]
        summary["cases"] += 1
        summary[case["priority"]] += 1
        summary["roles"].update(case["roles"])
        summary["surfaces"].update(case["surfaces"])
        summary["rules"].update(case["businessRuleIds"])
        by_role.update(case["roles"])
        by_surface.update(case["surfaces"])
        rule_usage.update(case["businessRuleIds"])

    MATRIX_FILE.parent.mkdir(parents=True, exist_ok=True)
    with MATRIX_FILE.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)

    summary = {
        "generatedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
        "suiteId": doc["suiteId"],
        "oracleId": oracle["oracleId"],
        "oracleGitCommit": oracle["gitCommit"],
        "totalCases": len(doc["cases"]),
        "mappedCases": sum(1 for row in rows if row["mapping_status"] == "MAPPED"),
        "unmappedCases": sum(1 for row in rows if row["mapping_status"] == "UNMAPPED"),
        "priority": dict(sorted(Counter(case["priority"] for case in doc["cases"]).items())),
        "byArea": {
            area: {
                "cases": value["cases"],
                "priority": {p: value[p] for p in ("P0", "P1", "P2", "P3")},
                "roles": sorted(value["roles"]),
                "surfaces": sorted(value["surfaces"]),
                "ruleIds": sorted(value["rules"]),
            }
            for area, value in sorted(by_area.items())
        },
        "byRole": dict(sorted(by_role.items())),
        "bySurface": dict(sorted(by_surface.items())),
        "ruleUsage": dict(sorted(rule_usage.items())),
        "unusedOracleRules": sorted(set(rules) - set(rule_usage)),
        "actionCoverage": {
            "totalActions": len(action_matrix.get("actions", [])),
            "roles": sorted({row["role"] for action in action_matrix.get("actions", []) for row in action.get("roles", [])}),
            "actions": [action.get("id") for action in action_matrix.get("actions", [])],
        },
    }
    SUMMARY_FILE.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"trace PASS matrix={MATRIX_FILE} summary={SUMMARY_FILE}")
    return 0


def render_catalog() -> int:
    doc = load_json(CASES_FILE)
    area_names = {
        "CRM": "客户与房屋",
        "DESIGN_QUOTE": "设计方案与报价",
        "CONTRACT": "合同与项目立项",
        "CHANGE": "合同变更",
        "PROJECT": "项目阶段与施工",
        "MATERIAL": "材料选材与采购",
        "ACCEPTANCE": "阶段验收与整改",
        "FINANCE": "财务与经营结果",
        "WORKBENCH": "工作台与正确引导",
        "CROSS_CUTTING": "权限、分页、编号与跨端",
    }
    lines = [
        "# 核心全链路测试用例库 V0.1",
        "",
        f"> Suite：`{doc['suiteId']}`  ",
        f"> Oracle：`{doc['oracleId']}`  ",
        f"> 用例数：{len(doc['cases'])}",
        "",
        "本文件由用例 JSON 自动生成，执行以 JSON、oracle lock 和追溯矩阵为准。",
        "",
    ]
    for area in area_names:
        cases = [case for case in doc["cases"] if case["area"] == area]
        if not cases:
            continue
        lines.extend([f"## {area_names[area]}（{area}）", ""])
        for case in cases:
            lines.extend([
                f"### {case['id']} · {case['title']}",
                "",
                f"- 优先级：`{case['priority']}`",
                f"- 角色：{', '.join(case['roles'])}",
                f"- 端面：{', '.join(case['surfaces'])}",
                f"- 类型：{', '.join(case['scenarioTypes'])}",
                f"- 业务规则：{', '.join(case['businessRuleIds'])}",
                f"- 对象：{', '.join(case['objectTypes'])}",
                f"- 夹具：`{case['fixture']}`",
                f"- 执行方式：`{case['executionMode']}`；状态：`{case['status']}`",
                "",
                "**前置条件**",
                "",
            ])
            lines.extend(f"- {item}" for item in case["preconditions"])
            lines.extend(["", "**步骤**", ""])
            lines.extend(f"{index}. {item}" for index, item in enumerate(case["steps"], 1))
            lines.extend(["", "**预期页面**", ""])
            lines.extend(f"- {item}" for item in case["expectedPage"])
            lines.extend(["", "**预期状态**", ""])
            lines.extend(f"- {item}" for item in case["expectedState"])
            lines.extend(["", "**不变量**", ""])
            lines.extend(f"- {item}" for item in case["expectedInvariants"])
            lines.extend(["", "**禁止结果**", ""])
            lines.extend(f"- {item}" for item in case["forbiddenOutcomes"])
            lines.extend([
                "",
                f"**Provider**：{', '.join(f'`{item}`' for item in case['providerRoutes'])}",
                "",
                f"**页面目标**：{', '.join(f'`{item}`' for item in case['pageTargets'])}",
                "",
                f"**证据**：{', '.join(case['evidenceRequired'])}",
                "",
            ])
    CATALOG_FILE.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"catalog PASS output={CATALOG_FILE}")
    return 0


def self_test() -> int:
    oracle = load_json(ORACLE_FILE)
    doc = load_json(CASES_FILE)
    providers = provider_catalog()
    web_leaves, app_paths = page_catalog()
    broken = json.loads(json.dumps(doc, ensure_ascii=False))
    broken["cases"][0]["businessRuleIds"] = ["BR-999"]
    broken["cases"][1]["providerRoutes"] = ["not/a/provider"]
    broken["cases"][2]["evidenceRequired"] = []
    errors, _ = validate_cases(broken, oracle, providers, web_leaves, app_paths)
    matrix = load_json(ACTION_MATRIX_FILE)
    broken_matrix = json.loads(json.dumps(matrix, ensure_ascii=False))
    broken_matrix["actions"][0]["dependencyProviders"] = ["not/a/provider"]
    action_errors = validate_action_matrix(broken_matrix, doc, oracle, providers)
    missing_probe_matrix = json.loads(json.dumps(matrix, ensure_ascii=False))
    missing_probe_matrix["actions"][0]["dependencyProbePayloads"] = {}
    missing_probe_errors = validate_action_matrix(missing_probe_matrix, doc, oracle, providers)
    broken_oracle = json.loads(json.dumps(oracle, ensure_ascii=False))
    broken_oracle["rules"]["USER-DEC-20269999-001"] = {"title": "orphan", "status": "approved"}
    decision_errors = validate_decision_coverage(broken_oracle, doc, load_json(UNMAPPED_FILE))
    checks = {
        "unknown_rule_detected": any("BR-999" in e for e in errors),
        "unknown_provider_detected": any("not/a/provider" in e for e in errors),
        "missing_evidence_detected": any("evidenceRequired" in e for e in errors),
        "action_dependency_detected": any("not/a/provider" in e for e in action_errors),
        "missing_action_probe_detected": any("缺少依赖Probe参数" in e for e in missing_probe_errors),
        "unmapped_decision_detected": any("USER-DEC-20269999-001" in e for e in decision_errors),
    }
    print(json.dumps(checks, ensure_ascii=False))
    if all(checks.values()):
        print("self-test PASS")
        return 0
    print("self-test FAIL", file=sys.stderr)
    return 1


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["validate", "trace", "catalog", "self-test"])
    args = parser.parse_args()
    if args.command == "validate":
        return validate_assets()
    if args.command == "trace":
        return build_trace()
    if args.command == "catalog":
        return render_catalog()
    return self_test()


if __name__ == "__main__":
    raise SystemExit(main())
