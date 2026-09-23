#!/usr/bin/env python3
"""核对麦鸿设计业务演示数据集数量与角色分布。"""

from __future__ import annotations

import argparse
import json
import subprocess
from pathlib import Path

PREFIX = "1391000%"


def query(sql: str) -> list[list[str]]:
    output = subprocess.check_output([
        "mysql", "-h127.0.0.1", "-uroot", "-pmysqladmin", "decoboon", "-N", "-e", sql,
    ], text=True, stderr=subprocess.DEVNULL)
    return [line.split("\t") for line in output.splitlines() if line.strip()]


def scalar(sql: str) -> int:
    return int(query(sql)[0][0])


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", default="data/seed/maihong-business-simulation-20260921.json")
    args = parser.parse_args()

    manifest = json.loads(Path(args.manifest).read_text(encoding="utf-8"))
    cases = manifest.get("cases", {})
    stage_counts: dict[str, int] = {}
    for case in cases.values():
        stage = case.get("stage")
        stage_counts[stage] = stage_counts.get(stage, 0) + 1

    checks = {
        "catalog": scalar("SELECT COUNT(*) FROM catalog_item WHERE com_id=1789903520997907818 AND active=1"),
        "packages": scalar("SELECT COUNT(*) FROM pack_template WHERE com_id=1789903520997907818"),
        "customers": scalar(f"SELECT COUNT(*) FROM cust_customer WHERE tel_no LIKE '{PREFIX}'"),
        "properties": scalar(f"SELECT COUNT(*) FROM db_property p JOIN cust_customer c ON c.cust_id=p.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
        "measures": scalar(f"SELECT COUNT(*) FROM db_measure m JOIN cust_customer c ON c.cust_id=m.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
        "requirements": scalar(f"SELECT COUNT(*) FROM db_design_requirement r JOIN cust_customer c ON c.cust_id=r.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
        "designs": scalar(f"SELECT COUNT(*) FROM db_design d JOIN cust_customer c ON c.cust_id=d.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
        "spaces": scalar(f"SELECT COUNT(*) FROM db_design_space s JOIN db_design d ON d.design_id=s.design_id JOIN cust_customer c ON c.cust_id=d.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
        "design_items": scalar(f"SELECT COUNT(*) FROM db_design_item i JOIN db_design d ON d.design_id=i.design_id JOIN cust_customer c ON c.cust_id=d.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
        "quotes": scalar(f"SELECT COUNT(*) FROM quote q JOIN cust_customer c ON c.cust_id=q.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
        "talks": scalar(f"SELECT COUNT(*) FROM db_design_talk t JOIN cust_customer c ON c.cust_id=t.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
        "contracts": scalar(f"SELECT COUNT(*) FROM db_project_contract ct JOIN cust_customer c ON c.cust_id=ct.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
        "projects": scalar(f"SELECT COUNT(*) FROM db_project p JOIN cust_customer c ON c.cust_id=p.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
        "projects_ongoing": scalar(f"SELECT COUNT(*) FROM db_project p JOIN cust_customer c ON c.cust_id=p.customer_id WHERE c.tel_no LIKE '{PREFIX}' AND p.status='Ongoing'"),
        "projects_accepting": scalar(f"SELECT COUNT(*) FROM db_project p JOIN cust_customer c ON c.cust_id=p.customer_id WHERE c.tel_no LIKE '{PREFIX}' AND p.status='Accepting'"),
        "acceptances_pending": scalar(f"SELECT COUNT(*) FROM db_acceptance a JOIN db_project p ON p.project_id=a.project_id JOIN cust_customer c ON c.cust_id=p.customer_id WHERE c.tel_no LIKE '{PREFIX}' AND a.status='Pending'"),
        "receivables": scalar(f"SELECT COUNT(*) FROM db_finance_receivable r JOIN cust_customer c ON c.cust_id=r.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
        "receipts": scalar(f"SELECT COUNT(*) FROM db_finance_receipt r JOIN db_project p ON p.project_id=r.project_id JOIN cust_customer c ON c.cust_id=p.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
        "ledgers": scalar(f"SELECT COUNT(*) FROM db_finance_ledger l JOIN db_project p ON p.project_id=l.project_id JOIN cust_customer c ON c.cust_id=p.customer_id WHERE c.tel_no LIKE '{PREFIX}'"),
    }

    design_distribution = {
        name: int(count) for name, count in query(
            f"SELECT e.name, COUNT(*) FROM db_design d JOIN db_tenant_employee e ON e.emp_id=d.emp_id "
            f"JOIN cust_customer c ON c.cust_id=d.customer_id WHERE c.tel_no LIKE '{PREFIX}' GROUP BY e.name"
        )
    }
    quote_distribution = {
        name: int(count) for name, count in query(
            f"SELECT e.name, COUNT(*) FROM quote q JOIN db_tenant_employee e ON e.emp_id=q.emp_id "
            f"JOIN cust_customer c ON c.cust_id=q.customer_id WHERE c.tel_no LIKE '{PREFIX}' GROUP BY e.name"
        )
    }

    failures = []
    expected = {
        "catalog": 550,
        "packages": 2,
        "customers": 44,
        "properties": 44,
        "designs": 36,
        "requirements": 36,
        "spaces": 108,
        "design_items": 216,
        "quotes": 64,
        "contracts": 12,
        "projects": 8,
        "projects_ongoing": 4,
        "projects_accepting": 4,
        "acceptances_pending": 4,
        "receivables": 24,
        "receipts": 4,
        "ledgers": 4,
    }
    for key, minimum in expected.items():
        if checks[key] < minimum:
            failures.append(f"{key}: expected>={minimum}, actual={checks[key]}")
    for stage, count in stage_counts.items():
        if count < 2:
            failures.append(f"stage {stage}: expected>=2, actual={count}")
    for name in ("谢玲玲", "董夏鹏"):
        if design_distribution.get(name, 0) < 18:
            failures.append(f"{name} designs<18")
        if quote_distribution.get(name, 0) < 32:
            failures.append(f"{name} quotes<32")

    result = {
        "batch": manifest.get("batch"),
        "caseCount": len(cases),
        "stageCounts": stage_counts,
        "checks": checks,
        "designerDistribution": design_distribution,
        "quoteDistribution": quote_distribution,
        "failures": failures,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
