#!/usr/bin/env python3
"""校验验收 RUN 的结果、证据和缺陷引用完整性。"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
CASES_FILE = ROOT / "docs/testing/cases/core-chain-v0.1.json"
ORACLE_FILE = ROOT / "docs/testing/traceability/oracle-lock.json"
FINANCE_CASES_FILE = ROOT / "docs/testing/cases/finance-accounting-gate-v0.1.json"
FINANCE_ORACLE_FILE = ROOT / "docs/testing/traceability/finance-accounting-gate-oracle.json"


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("run_dir")
    parser.add_argument("--final", action="store_true", help="要求所有用例已执行，不能存在 NOT_RUN")
    args = parser.parse_args()

    run_dir = Path(args.run_dir).resolve()
    errors: list[str] = []
    warnings: list[str] = []
    if not run_dir.is_dir():
        print(f"ERROR RUN目录不存在: {run_dir}", file=sys.stderr)
        return 1
    run_file = run_dir / "run.json"
    if not run_file.exists():
        print("ERROR 缺少 run.json", file=sys.stderr)
        return 1
    run = load(run_file)
    finance_suite = load(FINANCE_CASES_FILE)
    is_finance = run.get("suiteId") == finance_suite.get("suiteId")
    suite = finance_suite if is_finance else load(CASES_FILE)
    oracle = load(FINANCE_ORACLE_FILE) if is_finance else load(ORACLE_FILE)
    case_map = {case["id"]: case for case in suite["cases"]}

    if run.get("suiteId") != suite.get("suiteId"):
        errors.append(f"suiteId 不一致: {run.get('suiteId')} != {suite.get('suiteId')}")
    if run.get("oracleId") != oracle.get("oracleId"):
        errors.append(f"oracleId 不一致: {run.get('oracleId')} != {oracle.get('oracleId')}")
    expected_oracle_commit = oracle.get("gitCommit") or oracle.get("businessCommit")
    if run.get("oracleGitCommit") != expected_oracle_commit:
        errors.append("oracleGitCommit 与当前 oracle-lock 不一致，用例预期可能已过期")
    case_ids = run.get("caseIds") or []
    if not case_ids:
        errors.append("run.json caseIds 为空")

    counts = {"PASS": 0, "FAIL": 0, "BLOCKED": 0, "NOT_RUN": 0}
    for case_id in case_ids:
        if case_id not in case_map:
            errors.append(f"未知用例: {case_id}")
            continue
        result_file = run_dir / "case-results" / f"{case_id}.json"
        if not result_file.exists():
            errors.append(f"缺少结果文件: {case_id}")
            continue
        result = load(result_file)
        status = result.get("status")
        if status not in counts:
            errors.append(f"{case_id} 状态非法: {status}")
            continue
        counts[status] += 1
        evidence = result.get("evidence") or []
        defects = result.get("defectIds") or []
        if status in {"PASS", "FAIL"} and not evidence:
            errors.append(f"{case_id} 为 {status} 但没有证据")
        for evidence_path in evidence:
            path = run_dir / str(evidence_path)
            if not path.exists():
                errors.append(f"{case_id} 证据不存在: {evidence_path}")
        if status == "FAIL" and not defects:
            errors.append(f"{case_id} FAIL 必须关联至少一个 DEF")
        if status == "BLOCKED" and not result.get("blockedReason"):
            errors.append(f"{case_id} BLOCKED 必须填写 blockedReason")
        for defect_id in defects:
            defect_file = run_dir / "defects" / f"{defect_id}.json"
            if not defect_file.exists():
                errors.append(f"{case_id} 引用但缺少缺陷文件: {defect_id}")
                continue
            defect = load(defect_file)
            if case_id not in (defect.get("caseIds") or []):
                errors.append(f"{defect_id} 未关联 {case_id}")
        if status == "NOT_RUN" and args.final:
            errors.append(f"最终校验仍存在 NOT_RUN: {case_id}")

    report = run_dir / "reports/final.md"
    if args.final and not report.exists():
        errors.append("最终校验缺少 reports/final.md")
    declared = run.get("summary") or {}
    expected = {"total": len(case_ids), "pass": counts["PASS"], "fail": counts["FAIL"], "blocked": counts["BLOCKED"], "notRun": counts["NOT_RUN"]}
    if declared and declared != expected:
        warnings.append(f"run.json summary 尚未同步: declared={declared} actual={expected}")
    if is_finance and args.final:
        all_p0 = {case["id"] for case in suite["cases"] if case["priority"] == "P0"}
        missing_p0 = all_p0 - set(case_ids)
        if missing_p0:
            errors.append(f"财务最终门禁缺少 P0 用例: {', '.join(sorted(missing_p0))}")
        if counts["FAIL"] or counts["BLOCKED"]:
            errors.append(f"财务最终门禁禁止 FAIL/BLOCKED: fail={counts['FAIL']} blocked={counts['BLOCKED']}")

    print(json.dumps({"runId": run.get("runId"), "counts": expected, "final": args.final, "errors": len(errors), "warnings": len(warnings)}, ensure_ascii=False))
    for warning in warnings:
        print(f"WARN {warning}", file=sys.stderr)
    for error in errors:
        print(f"ERROR {error}", file=sys.stderr)
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
