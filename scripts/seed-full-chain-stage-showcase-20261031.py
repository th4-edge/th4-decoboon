#!/usr/bin/env python3
"""在本地测试企业生成覆盖完整业务阶段的样例数据。

复用 `seed-maihong-business-scenarios.py` 的真实 Provider 链路，但不再绑定
历史演示企业账号：角色账号从本地 audit manifest 读取，并为每个阶段只生成一个样例，
便于用户直接按阶段进入页面测试。
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
SEED_MODULE = ROOT / "scripts/seed-maihong-business-scenarios.py"


def load_seed_module():
    spec = importlib.util.spec_from_file_location("maihong_stage_seed", SEED_MODULE)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {SEED_MODULE}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def build_employee_map(manifest: dict[str, Any], company_id: str) -> dict[str, dict[str, Any]]:
    company = manifest["companies"][company_id]
    employees = company["employees"]

    def normalized(employee: dict[str, Any]) -> dict[str, Any]:
        result = dict(employee)
        result["phone"] = result.get("phone") or result.get("tel")
        return result

    def first(role: str) -> dict[str, Any]:
        for employee in employees:
            if employee.get("role") == role:
                return normalized(employee)
        raise RuntimeError(f"manifest 缺少角色 {role}")

    designers = [normalized(employee) for employee in employees if employee.get("role") == "Designer"]
    if not designers:
        raise RuntimeError("manifest 缺少 Designer")
    designer_two = designers[1] if len(designers) > 1 else designers[0]

    return {
        "boss": first("Boss"),
        "storeManager": first("StoreManager"),
        "marketer": first("Marketer"),
        "finance": first("Finance"),
        "designerXie": designers[0],
        "designerDong": designer_two,
    }


def ensure_support_catalog(module, ctx) -> None:
    required = [
        ("Custom", "定制", "定制--全屋柜体投影面积", 680, 430),
        ("General", "综合", "综合--成品保护", 120, 80),
    ]
    for item_type, category, name, manual_price, cost_price in required:
        page = ctx.post("boss", "catalog/item/page", {"type": item_type, "current": 1, "size": 50})
        items = module.json_list(page)
        usable = [item for item in items
                  if float(item.get("manualPrice") or 0) + float(item.get("materialPrice") or 0) > 0]
        if usable:
            continue
        ctx.post("boss", "catalog/item/save", {
            "type": item_type,
            "category": category,
            "name": name,
            "unit": "Area",
            "pricingMode": "ManualOnly",
            "materialPrice": 0,
            "manualPrice": manual_price,
            "costPrice": cost_price,
        })


def stage_summary(state: dict[str, Any], account_map: dict[str, Any]) -> dict[str, Any]:
    rows = []
    for key, case in sorted(state.get("cases", {}).items()):
        steps = case.get("steps") or {}
        quote_history = steps.get("quoteHistory") if isinstance(steps.get("quoteHistory"), dict) else {}
        rows.append({
            "caseKey": key,
            "stage": case.get("stage"),
            "customerName": case.get("customerName"),
            "customerId": steps.get("customerId"),
            "propertyId": steps.get("propertyId"),
            "designId": steps.get("designId"),
            "quoteId": (quote_history.get("accepted") or {}).get("quoteId") if quote_history else steps.get("quoteDraft", {}).get("quoteId"),
            "contractId": steps.get("contractId"),
            "projectId": steps.get("projectId"),
            "acceptanceId": (steps.get("acceptancePending") or {}).get("acceptanceId"),
            "completedAt": case.get("completedAt"),
        })
    return {
        "accounts": account_map,
        "stages": rows,
        "manifest": "stage-seed-state.json",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workers-file", default=str(ROOT / "docs/operations/data-changes/2026-10-30-fresh-business-audit-manifest.json"))
    parser.add_argument("--company-id", default="199500000000000001")
    parser.add_argument("--api", default="http://127.0.0.1:8090/api")
    parser.add_argument("--password", default="Audit2026!")
    parser.add_argument("--state", default=str(ROOT / "docs/acceptance/runs/20261031-full-chain-stage-fixture/stage-seed-state.json"))
    parser.add_argument("--output", default=str(ROOT / "docs/acceptance/runs/20261031-full-chain-stage-fixture/account-and-stage-map.json"))
    parser.add_argument("--case-prefix", default="FC-20261031")
    args = parser.parse_args()

    module = load_seed_module()
    workers = json.loads(Path(args.workers_file).read_text(encoding="utf-8"))
    employee_map = build_employee_map(workers, args.company_id)
    module.EMPLOYEES = employee_map

    state = module.BatchState.load(Path(args.state))
    state.data["batch"] = args.case_prefix
    ctx = module.SeedContext(args.api, args.password, state)
    ctx.login_all()
    ensure_support_catalog(module, ctx)
    ctx.load_catalogs()

    stages = [stage for stage, _count in module.STAGES]
    for index, stage in enumerate(stages, start=1):
        spec = {
            "key": f"{args.case_prefix}-{index:02d}-{stage}",
            "index": index,
            "stage": stage,
            "designerRole": "designerXie" if index % 2 else "designerDong",
            "multiProperty": False,
        }
        if stage in {"new", "following", "pending_measure", "measured"}:
            spec["designerRole"] = "designerXie"
        module.progress_case(ctx, spec)

    account_map = {
        "companyId": args.company_id,
        "companyName": workers["companies"][args.company_id]["name"],
        "password": args.password,
        "employees": workers["companies"][args.company_id]["employees"],
    }
    output = stage_summary(state.data, account_map)
    Path(args.output).write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"stages": len(stages), "output": args.output, "state": args.state}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
