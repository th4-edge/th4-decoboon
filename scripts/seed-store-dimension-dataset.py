#!/usr/bin/env python3
"""多门店数据集造数（DESIGN-BIZ-20261013-008 / EV-271）——**仅 dev**。

阶段：
  skeleton  : 造租户骨架（企业 M：A/B/C 三店 + 员工 + 1 名无门店员工；企业 S：单店）——SQL（模板行复制）
  clean     : 按造数 com_id 清理（仅这两个企业，绝不触碰既有夹具）
  snapshot  : 打印企业×门店×对象条数/金额快照（含 md5）

说明：dev 环境**没有可用于 curl 的建企业接口**（`auth/session/register` 依赖微信身份、员工走邀请链路、
建店受 `Plan.maxStores` 限制），故租户骨架以 SQL 造出并在实施文档中标注为「夹具骨架」；
业务链路（客户→房屋→量房→设计→报价→合同→项目→采购/到货→收付款/台账/结算）一律走真实 HTTP 接口，见阶段 2（下轮）。

用法（务必用带 bcrypt 的解释器）：
  /opt/anaconda3/bin/python3 scripts/seed-store-dimension-dataset.py skeleton
  /opt/anaconda3/bin/python3 scripts/seed-store-dimension-dataset.py snapshot
  /opt/anaconda3/bin/python3 scripts/seed-store-dimension-dataset.py clean
"""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from pathlib import Path

DB = ["mysql", "-h127.0.0.1", "-P3306", "-uroot", "-pmysqladmin", "decoboon", "-N", "-B"]
PASSWORD = "Zdq123456"
MANIFEST_DIR = Path(__file__).resolve().parents[1] / "docs" / "operations" / "data-changes"

# ---- 造数企业（独立 com_id，远离既有夹具 999100000000000001/2） ----
M = {
    "com_id": 999200000000000001, "title": "多门店数据集造数企业M", "name": "ZDQ造数M", "plan": "annual-pro",
    "stores": [("A 总店", True), ("B 店", False), ("C 店", False)],
}
S = {
    "com_id": 999300000000000001, "title": "单门店数据集造数企业S", "name": "ZDQ造数S", "plan": "annual-basic",
    "stores": [("唯一门店", True)],
}
# 每店角色：店长、设计、业务、项目经理、门店财务（规格 §1.1）
STORE_ROLES = [("StoreManager", "店长"), ("Designer", "设计"), ("Marketer", "业务"), ("Pm", "项目经理"), ("Finance", "门店财务")]
STORE_LESS = ("Marketer", "无门店业务")
PHONE_BASE = 13910000000  # 造数账号号段
END_YEAR, END_MONTH, END_DAY = 2027, 10, 5  # 造数企业订阅到期（未来）

TEMPLATE_COMPANY = 999100000000000001
TEMPLATE_STORE = 999100000000000011
TEMPLATE_EMPLOYEE = 999100000000000021
TEMPLATE_SUBSCRIPTION_COM = 1789938943485101774


def sql(stmt: str, params: tuple = ()) -> list[list[str]]:
    args = DB + ["-e", stmt % params if params else stmt]
    out = subprocess.run(args, capture_output=True, text=True)
    if out.returncode != 0:
        raise RuntimeError(f"SQL 失败: {stmt[:160]}\n{out.stderr}")
    return [line.split("\t") for line in out.stdout.splitlines() if line]


def columns(table: str) -> list[str]:
    return [row[0] for row in sql("show columns from " + table)]


def copy_row(table: str, template_where: str, overrides: dict) -> None:
    """按模板行复制一行并覆盖指定列（保证派生时间列与模板一致）。"""
    cols = columns(table)
    names, values = [], []
    for col in cols:
        names.append(col)
        values.append(str(overrides[col]) if col in overrides else col)
    stmt = "insert into %s (%s) select %s from %s where %s limit 1" % (
        table, ", ".join(names), ", ".join(values), table, template_where)
    sql(stmt)


def bcrypt_hash(raw: str) -> str:
    import bcrypt
    return bcrypt.hashpw(raw.encode(), bcrypt.gensalt(rounds=10)).decode()


def plan_version(plan_code: str) -> tuple[int, int]:
    """返回 (plan_id, plan_version_id)。"""
    rows = sql("select p.plan_id, v.version_id from db_tenant_plan_version v "
               "join db_tenant_plan p on p.plan_id=v.plan_id "
               "where p.code='%s' order by v.version_id desc limit 1" % plan_code)
    assert rows, "找不到套餐版本：" + plan_code
    return int(rows[0][0]), int(rows[0][1])


def skeleton(manifest_path: Path) -> dict:
    # 幂等：先按造数 com_id 清理，保证可重复执行（只影响这两个造数企业）
    clean([M["com_id"], S["com_id"]])
    manifest: dict = {"generatedAt": None, "password": PASSWORD, "companies": {}, "note": "dev 专用夹具骨架（SQL）"}
    seq = 0
    for spec, store_less in ((M, True), (S, False)):
        com = spec["com_id"]
        copy_row("db_tenant_company", "com_id=%d" % TEMPLATE_COMPANY, {
            "com_id": com, "title": "'%s'" % spec["title"], "name": "'%s'" % spec["name"],
            "size": "'A'", "enable_store": "1",
        })
        stores = []
        for idx, (store_name, is_default) in enumerate(spec["stores"]):
            seq += 1
            sid = com + 10 + idx
            copy_row("db_tenant_store", "store_id=%d" % TEMPLATE_STORE, {
                "store_id": sid, "company_com_id": com, "name": "'%s'" % store_name,
                "default_store": "1" if is_default else "0",
            })
            stores.append({"storeId": sid, "name": store_name, "default": is_default})
        # 订阅：让产品侧套餐能力与门店数一致（M=3 店 / S=1 店）
        sub_id = com + 900
        plan_id, version_id = plan_version(spec["plan"])
        # `DateTime` 派生列编码 = YYYYMMDD / HHMMSS / YYYYMMDDHHMMSS；模板行是 2026-10-05（已过期），
        # 造数企业必须给**未来**到期时间，否则产品侧一律 SUBSCRIPTION_EXPIRED 只读。
        end_year, end_month, end_day = END_YEAR, END_MONTH, END_DAY
        end_date = "%04d%02d%02d" % (end_year, end_month, end_day)
        end_time = "051543"
        copy_row("db_tenant_subscription", "com_id=%d" % TEMPLATE_SUBSCRIPTION_COM, {
            "sub_id": sub_id, "com_id": com, "plan_id": plan_id,
            "plan_version_id": version_id, "status": "'Trial'",
            "service_end_year": end_year, "service_end_month": end_month, "service_end_day": end_day,
            "service_end_hour": 5, "service_end_minute": 15, "service_end_second": 43,
            "service_end_date": end_date, "service_end_time": int(end_time),
            "service_end_date_time": int(end_date + end_time),
        })
        pwd = bcrypt_hash(PASSWORD)
        employees = []
        def add_employee(name: str, role: str, store_id, tel: str) -> dict:
            nonlocal seq
            seq += 1
            emp_id = com + 20 + seq
            cre_id = com + 800 + seq
            copy_row("db_tenant_employee", "emp_id=%d" % TEMPLATE_EMPLOYEE, {
                "emp_id": emp_id, "com_id": com, "store_id": "NULL" if store_id is None else store_id,
                "role": "'%s'" % role, "status": "'Active'", "name": "'%s'" % name,
                "tel_no": "'%s'" % tel, "invite_id": "NULL", "avatar_url": "NULL",
                "resign_reason": "NULL", "resigned_at": "NULL", "team_id": "NULL",
            })
            copy_row("db_tenant_credentials", "phone='13900000001'", {
                "cre_id": cre_id, "phone": "'%s'" % tel, "type": "'Password'", "status": "'Active'",
                "password_hash": "'%s'" % pwd, "emp_id": emp_id, "failed_attempts": 0,
                "locked_until": "NULL", "must_change_password": 0,
                # 微信 openid 列非空且唯一 → 造数账号用独立占位值（非真实 openid）
                "id_key": "'seed-%d'" % cre_id, "id_secret": "'seed-%d'" % cre_id,
            })
            return {"empId": emp_id, "name": name, "role": role, "tel": tel,
                    "storeId": store_id, "creId": cre_id}

        boss_tel = str(PHONE_BASE + (100 if com == M["com_id"] else 200))
        boss = add_employee("造数M负责人" if com == M["com_id"] else "造数S负责人", "Boss", stores[0]["storeId"], boss_tel)
        employees.append(boss)
        for si, store in enumerate(stores):
            for ri, (role, label) in enumerate(STORE_ROLES):
                tel = str(PHONE_BASE + (100 if com == M["com_id"] else 200) + 10 + si * 10 + ri)
                employees.append(add_employee("%s%s" % (store["name"], label), role, store["storeId"], tel))
        if store_less:
            # 【产品缺陷候选 / 规格 §5「不得绕过」】db_tenant_employee.store_id 为 NOT NULL 且无默认值，
            # 全库不存在 store_id 为空/0 的员工行 → 「无门店归属员工」**无法构造**，
            # 也就意味着 StoreAttributionContext.forCustomerOwner 的「负责人无门店 → 单门店默认 / 多门店 400 阻断」
            # 分支在真实数据下**不可达**（仅单测用 mock 员工覆盖）。此处**不造**，在 manifest 与文档中登记。
            manifest.setdefault("productGaps", []).append({
                "key": "NO_STORE_EMPLOYEE_IMPOSSIBLE",
                "title": "无门店归属员工无法构造（A9 运行时不可覆盖）",
                "detail": "db_tenant_employee.store_id 为 NOT NULL 且无默认值；全库无 store_id 为空/0 的员工行。"
                          "故 DEC-070「负责人无门店 → 单门店企业默认该门店 / 多门店企业 400 阻断」在真实数据下不可达"
                          "（仅 StoreAttributionContextTest 用 mock 员工覆盖）。可能的真实触发路径只有「门店被删除/停用」——"
                          "属 P0D-04 批次（门店启用/停用 + 迁移）范围。",
                "affects": ["A9"],
            })
        manifest["companies"][str(com)] = {"comId": com, "title": spec["title"], "stores": stores,
                                           "subscriptionId": sub_id, "employees": employees}
    MANIFEST_DIR.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    return manifest


TABLES = ["cust_customer", "db_property", "db_measure", "quote", "db_project_contract", "db_project",
          "db_finance_receivable", "db_finance_receipt", "db_finance_payable", "db_finance_payment",
          "db_finance_ledger", "db_finance_settlement"]


def existing(name: str) -> bool:
    return bool(sql("select 1 from information_schema.tables where table_schema='decoboon' and table_name='%s'" % name))


def snapshot(com_ids: list[int]) -> str:
    lines = []
    for com in com_ids:
        lines.append("== com_id=%d ==" % com)
        for table in TABLES:
            if not existing(table):
                lines.append("%s	SKIP(表不存在)" % table)
                continue
            cols = columns(table)
            if "com_id" not in cols:
                lines.append("%s	SKIP(无 com_id 列)" % table)
                continue
            key = "store_id" if "store_id" in cols else "NULL"
            rows = sql("select ifnull(%s,-1), count(*) from %s where com_id=%d group by 1 order by 1" % (key, table, com))
            for store_id, cnt in rows:
                lines.append("%s\tstore=%s\tcount=%s" % (table, store_id, cnt))
    text = "\n".join(lines) + "\n"
    return text + "\nmd5=%s\n" % hashlib.md5(text.encode()).hexdigest()


def clean(com_ids: list[int]) -> None:
    for table in TABLES + ["db_tenant_credentials", "db_tenant_employee", "db_tenant_store",
                           "db_tenant_subscription", "db_tenant_company"]:
        if not existing(table):
            continue
        cols = columns(table)
        if "com_id" in cols:
            sql("delete from %s where com_id in (%s)" % (table, ", ".join(str(c) for c in com_ids)))
        elif table == "db_tenant_store":
            sql("delete from db_tenant_store where company_com_id in (%s)" % ", ".join(str(c) for c in com_ids))
        elif table == "db_tenant_credentials":
            sql("delete from db_tenant_credentials where emp_id in (select emp_id from db_tenant_employee where com_id in (%s))"
                % ", ".join(str(c) for c in com_ids))


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("stage", choices=["skeleton", "snapshot", "clean"])
    ap.add_argument("--com", type=int, action="append")
    args = ap.parse_args()
    com_ids = args.com or [M["com_id"], S["com_id"]]
    manifest_path = MANIFEST_DIR / "2026-10-13-门店维度造数manifest.json"
    if args.stage == "skeleton":
        m = skeleton(manifest_path)
        print(json.dumps({"companies": [c["comId"] for c in m["companies"].values()],
                          "employees": {k: len(v["employees"]) for k, v in m["companies"].items()}},
                         ensure_ascii=False))
        print("manifest ->", manifest_path)
    elif args.stage == "snapshot":
        print(snapshot(com_ids))
    else:
        clean(com_ids)
        print("cleaned", com_ids)
    return 0


if __name__ == "__main__":
    sys.exit(main())
