#!/usr/bin/env python3
"""从冻结测试用例创建可恢复的验收 RUN。"""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TESTING = ROOT / "docs/testing"
CASES_FILE = TESTING / "cases/core-chain-v0.1.json"
ORACLE_FILE = TESTING / "traceability/oracle-lock.json"
VALIDATOR = TESTING / "scripts/test_assets.py"
SKILL = ROOT / "docs/testing"
CASE_TEMPLATE = SKILL / "references/templates/case-result-template.json"


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def git_head(path: Path) -> str | None:
    try:
        result = subprocess.run(["git", "-C", str(path), "rev-parse", "HEAD"], capture_output=True, text=True, timeout=10)
        return result.stdout.strip() if result.returncode == 0 else None
    except (OSError, subprocess.SubprocessError):
        return None


def case_hash(case: dict) -> str:
    payload = json.dumps(case, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
    return hashlib.sha256(payload).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--plan", default="TP-20261101-V0.1")
    parser.add_argument("--case-id", action="append", default=[])
    parser.add_argument("--area", action="append", default=[])
    parser.add_argument("--priority", action="append", default=[])
    parser.add_argument("--environment", default="local-fixture")
    parser.add_argument("--tenant-id", default="199500000000000001")
    parser.add_argument("--data-prefix", default=None)
    parser.add_argument("--output-root", default=str(ROOT / "docs/acceptance/runs"))
    args = parser.parse_args()

    validation = subprocess.run([sys.executable, str(VALIDATOR), "validate"], cwd=ROOT, capture_output=True, text=True)
    if validation.returncode != 0:
        print(validation.stdout, end="")
        print(validation.stderr, end="", file=sys.stderr)
        return 1

    suite = load(CASES_FILE)
    oracle = load(ORACLE_FILE)
    cases = suite["cases"]
    selected = cases
    if args.case_id:
        wanted = set(args.case_id)
        selected = [case for case in selected if case["id"] in wanted]
        missing = wanted - {case["id"] for case in selected}
        if missing:
            raise SystemExit(f"未知 case id: {', '.join(sorted(missing))}")
    if args.area:
        selected = [case for case in selected if case["area"] in set(args.area)]
    if args.priority:
        selected = [case for case in selected if case["priority"] in set(args.priority)]
    if not selected:
        raise SystemExit("未选择任何用例")
    not_ready = [case["id"] for case in selected if case.get("status") != "READY"]
    if not_ready:
        raise SystemExit(f"只能执行 READY 用例，以下用例不可执行: {', '.join(not_ready)}")

    run_dir = Path(args.output_root) / args.run_id
    if run_dir.exists():
        raise SystemExit(f"RUN 已存在，拒绝覆盖: {run_dir}")
    for directory in ("case-results", "evidence", "defects", "reports", "raw"):
        (run_dir / directory).mkdir(parents=True, exist_ok=True)

    now = datetime.now().astimezone().isoformat(timespec="seconds")
    data_prefix = args.data_prefix or f"AUDIT-{datetime.now().strftime('%Y%m%d')}-{args.run_id.replace('-', '')[:16]}-"
    run = {
        "runId": args.run_id,
        "createdAt": now,
        "status": "PLANNED",
        "planId": args.plan,
        "suiteId": suite["suiteId"],
        "oracleId": oracle["oracleId"],
        "oracleGitCommit": oracle["gitCommit"],
        "backendCommit": git_head(ROOT),
        "webCommit": git_head(ROOT.parent.parent / "前端代码/th4-decoboon-web"),
        "appCommit": git_head(ROOT.parent.parent / "前端代码/th4-decoboon-app"),
        "artifactHashes": {},
        "environment": args.environment,
        "tenantId": args.tenant_id,
        "dataPrefix": data_prefix,
        "caseIds": [case["id"] for case in selected],
        "summary": {"total": len(selected), "pass": 0, "fail": 0, "blocked": 0, "notRun": len(selected)},
    }
    (run_dir / "run.json").write_text(json.dumps(run, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    template = load(CASE_TEMPLATE)
    for case in selected:
        result = dict(template)
        result.update({
            "caseId": case["id"],
            "status": "NOT_RUN",
            "dataPrefix": data_prefix,
            "caseDefinitionHash": case_hash(case),
            "priority": case["priority"],
            "area": case["area"],
            "businessRuleIds": case["businessRuleIds"],
            "pageTargets": case["pageTargets"],
            "providerRoutes": case["providerRoutes"],
        })
        (run_dir / "case-results" / f"{case['id']}.json").write_text(
            json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    preflight = [
        "# 预检",
        "",
        f"- RUN：`{args.run_id}`",
        f"- Plan：`{args.plan}`",
        f"- Suite：`{suite['suiteId']}`",
        f"- Oracle：`{oracle['oracleId']}` / `{oracle['gitCommit']}`",
        f"- 用例数：{len(selected)}",
        f"- P0/P1：{sum(c['priority'] == 'P0' for c in selected)}/{sum(c['priority'] == 'P1' for c in selected)}",
        f"- 数据前缀：`{data_prefix}`",
        "- 资产校验：PASS",
        "- 状态：`PLANNED`，尚未执行任何业务用例",
        "",
    ]
    (run_dir / "reports/preflight.md").write_text("\n".join(preflight), encoding="utf-8")
    print(json.dumps({"runId": args.run_id, "runDir": str(run_dir), "caseCount": len(selected)}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
