#!/usr/bin/env python3
"""P0D-04 验收用独立租户 T（**仅 dev**）：3 家门店 + 无业务，用于「启用/停用 + 排序」9 用例。

为什么需要独立租户：造数企业 M 的三家门店都有在手业务（GAP-051 会正确阻断停用），
单门店企业 S 又不满足「多门店 + 可停用非默认门店」的前置，故新建 `999400000000000001`：
  - T 一店（默认总店）/ T 二店 / T 三店，套餐 annual-pro（3 店额度），**不造业务数据**；
  - 员工：BOSS（13910000300）+ 二店店长（用于非 BOSS 403 用例）+ 三店财务（用于造一条**历史流水**，
    再把该员工调离三店，从而让三店「无在职员工、无在手业务、但有历史数据」→ 可停用且验证历史保留）。
只在 T 的 com_id 上增删，绝不触碰既有夹具（M/S；EV-266/267 可比性不受影响）。

用法：/opt/anaconda3/bin/python3 scripts/seed-store-status-tenant.py [create|clean]
"""
from __future__ import annotations

import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE_SCRIPT = ROOT / "scripts" / "seed-store-dimension-dataset.py"
spec = importlib.util.spec_from_file_location("seed_dataset", BASE_SCRIPT)
seed = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(seed)

T = 999400000000000001
T_STORES = [("T 一店", True), ("T 二店", False), ("T 三店", False)]
T_PLAN = "annual-pro"
BOSS_TEL = "13910000300"
PASSWORD = seed.PASSWORD


def create() -> dict:
    seed.clean([T])
    seed.copy_row("db_tenant_company", "com_id=%d" % seed.TEMPLATE_COMPANY, {
        "com_id": T, "title": "'门店状态验收企业T'", "name": "'ZDST-T'", "size": "'A'", "enable_store": "1",
    })
    stores = []
    for idx, (name, is_default) in enumerate(T_STORES):
        sid = T + 10 + idx
        seed.copy_row("db_tenant_store", "store_id=%d" % seed.TEMPLATE_STORE, {
            "store_id": sid, "company_com_id": T, "name": "'%s'" % name,
            "default_store": "1" if is_default else "0",
        })
        stores.append({"storeId": sid, "name": name, "default": is_default, "status": "Active", "sort": 0})
    plan_id, version_id = seed.plan_version(T_PLAN)
    end_date, end_time = "20271005", "051543"
    seed.copy_row("db_tenant_subscription", "com_id=%d" % seed.TEMPLATE_SUBSCRIPTION_COM, {
        "sub_id": T + 900, "com_id": T, "plan_id": plan_id, "plan_version_id": version_id,
        "status": "'Trial'",
        "service_end_year": seed.END_YEAR, "service_end_month": seed.END_MONTH, "service_end_day": seed.END_DAY,
        "service_end_hour": 5, "service_end_minute": 15, "service_end_second": 43,
        "service_end_date": end_date, "service_end_time": int(end_time),
        "service_end_date_time": int(end_date + end_time),
    })
    pwd = seed.bcrypt_hash(PASSWORD)
    employees = []

    def employee(name: str, role: str, store_id: int, tel: str, offset: int) -> dict:
        emp_id, cre_id = T + 20 + offset, T + 800 + offset
        seed.copy_row("db_tenant_employee", "emp_id=%d" % seed.TEMPLATE_EMPLOYEE, {
            "emp_id": emp_id, "com_id": T, "store_id": store_id, "role": "'%s'" % role,
            "status": "'Active'", "name": "'%s'" % name, "tel_no": "'%s'" % tel, "invite_id": "NULL",
            "avatar_url": "NULL", "resign_reason": "NULL", "resigned_at": "NULL", "team_id": "NULL",
        })
        seed.copy_row("db_tenant_credentials", "phone='13900000001'", {
            "cre_id": cre_id, "phone": "'%s'" % tel, "type": "'Password'", "status": "'Active'",
            "password_hash": "'%s'" % pwd, "emp_id": emp_id, "failed_attempts": 0,
            "locked_until": "NULL", "must_change_password": 0,
            "id_key": "'seed-%d'" % cre_id, "id_secret": "'seed-%d'" % cre_id,
        })
        return {"empId": emp_id, "name": name, "role": role, "tel": tel, "storeId": store_id, "creId": cre_id}

    employees.append(employee("造数T负责人", "Boss", stores[0]["storeId"], BOSS_TEL, 1))
    employees.append(employee("T 二店店长", "StoreManager", stores[1]["storeId"], "13910000311", 2))
    employees.append(employee("T 三店财务", "Finance", stores[2]["storeId"], "13910000321", 3))
    manifest = {"comId": T, "title": "门店状态验收企业T", "plan": T_PLAN,
                "stores": stores, "employees": employees, "password": PASSWORD}
    Path(ROOT / "docs" / "operations" / "data-changes" / "2026-10-13-门店状态验收租户T-manifest.json") \
        .write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return manifest


def main() -> int:
    action = sys.argv[1] if len(sys.argv) > 1 else "create"
    if action == "clean":
        seed.clean([T])
        print("cleaned T", T)
        return 0
    print(json.dumps(create(), ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
