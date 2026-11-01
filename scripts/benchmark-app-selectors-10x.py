#!/usr/bin/env python3
"""App 业务对象选择器 10× 数据、HTTP P95 与 SQL statement 专项。

只允许本地 dev 库。使用固定 BENCH-SELECTOR-10X 数据段，结束时按 ID 区间清理。
"""
from __future__ import annotations

import argparse
import asyncio
import json
import os
import statistics
import subprocess
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Awaitable, Callable

import aiohttp
import requests

ROOT = Path(__file__).resolve().parents[1]
API = os.environ.get("DECOBOON_BENCH_API", "http://127.0.0.1:8090/api")
PASSWORD = "Audit2026!"
COMPANY = 199500000000000001
STORE = 199500000000000011
BOSS = 199500000000000102
DESIGNER = 199500000000000105
PM = 199500000000000107
PREFIX = "BENCH-SELECTOR-10X"
BASE = 8_700_000_000_000_000_000
OUT = ROOT / "docs/acceptance/runs/20261101-app-selector-10x-p95"
STATE = OUT / "evidence/fixture-state.json"
RESULT = OUT / "evidence/benchmark.json"


def load_env() -> dict[str, str]:
    env = dict(os.environ)
    for name in (".env.dev.local",):
        path = ROOT / name
        if not path.exists():
            continue
        for line in path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            if line.startswith("export "):
                line = line[7:]
            key, value = line.split("=", 1)
            env[key.strip()] = value.strip().strip("'\"")
    return env


ENV = load_env()
DB = ["mysql", "-h127.0.0.1", f"-u{ENV['DB_USERNAME']}", f"-p{ENV['DB_PASSWORD']}", ENV.get("DB_NAME", "decoboon")]


def mysql_exec(sql: str) -> None:
    proc = subprocess.run(DB, input=sql, text=True, capture_output=True)
    if proc.returncode:
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip())


def mysql_rows(sql: str) -> list[list[str]]:
    proc = subprocess.run(DB + ["-N", "-B", "-e", sql], text=True, capture_output=True)
    if proc.returncode:
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip())
    return [line.split("\t") for line in proc.stdout.splitlines() if line]


def mysql_scalar(sql: str) -> str:
    rows = mysql_rows(sql)
    return rows[0][0] if rows else ""


def sql_value(value) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def columns(table: str) -> list[str]:
    return [row[0] for row in mysql_rows(
        "SELECT column_name FROM information_schema.columns "
        f"WHERE table_schema='{ENV.get('DB_NAME', 'decoboon')}' AND table_name={sql_value(table)} ORDER BY ordinal_position"
    )]


def first_pk(table: str, pk: str, where: str) -> int:
    value = mysql_scalar(f"SELECT {pk} FROM {table} WHERE {where} ORDER BY {pk} LIMIT 1")
    if not value:
        raise RuntimeError(f"缺少可克隆源数据: {table} {where}")
    return int(value)


def clone_rows(table: str, pk: str, source_pk: int, ids: list[int], replacements: Callable[[int, int], dict[str, str]]) -> None:
    cols = columns(table)
    col_sql = ", ".join(f"`{c}`" for c in cols)
    statements = []
    for index, new_id in enumerate(ids, start=1):
        override = replacements(index, new_id)
        expr = []
        for col in cols:
            expr.append(override.get(col, f"`{col}`"))
        statements.append(
            f"INSERT INTO {table} ({col_sql}) SELECT {', '.join(expr)} FROM {table} WHERE {pk}={source_pk} LIMIT 1;"
        )
    mysql_exec("START TRANSACTION;\n" + "\n".join(statements) + "\nCOMMIT;")
    actual = int(mysql_scalar(f"SELECT COUNT(*) FROM {table} WHERE {pk} BETWEEN {min(ids)} AND {max(ids)}"))
    if actual != len(ids):
        raise RuntimeError(f"{table} 克隆数量不符: {actual}/{len(ids)}")


def current_count(table: str, pk: str) -> int:
    return int(mysql_scalar(f"SELECT COUNT(*) FROM {table} WHERE com_id={COMPANY}"))


def login(phone: str) -> str:
    response = requests.post(f"{API}/auth/session/password-login", json={"phone": phone, "password": PASSWORD}, timeout=20)
    body = response.json()
    if not response.ok or not body.get("success"):
        raise RuntimeError(f"登录失败 {phone}: {json.dumps(body, ensure_ascii=False)}")
    return body["data"]["token"]


def questions() -> int:
    return int(mysql_rows("SHOW GLOBAL STATUS LIKE 'Questions'")[0][1])


def page_rows(data) -> tuple[int, int]:
    if isinstance(data, list):
        return len(data), len(data)
    if not isinstance(data, dict):
        return 0, 0
    records = data.get("content") or data.get("records") or data.get("list") or []
    if not isinstance(records, list):
        records = []
    total = int(data.get("totalElements") or data.get("total") or len(records))
    return len(records), total


def api_post(token: str, route: str, payload: dict | None = None) -> tuple[dict, float]:
    started = time.perf_counter()
    response = requests.post(f"{API}/{route}", headers={"token": token, "content-type": "application/json"}, json=payload or {}, timeout=60)
    elapsed = (time.perf_counter() - started) * 1000
    body = response.json()
    if not response.ok or not body.get("success"):
        raise RuntimeError(f"{route} 失败: {json.dumps(body, ensure_ascii=False)}")
    return body.get("data"), elapsed


def fixture_counts() -> dict[str, int]:
    specs = {
        "customers": ("cust_customer", "cust_id"), "properties": ("db_property", "property_id"),
        "designs": ("db_design", "design_id"), "quotes": ("quote", "quote_id"),
        "projects": ("db_project", "project_id"), "employees": ("db_tenant_employee", "emp_id"),
        "workers": ("db_worker", "worker_id"), "partners": ("db_partner", "partner_id"),
        "catalog": ("catalog_item", "item_id"), "templates": ("db_content_template", "template_id"),
        "expenseCategories": ("db_finance_expense_category", "category_id"),
        "ledgerCategories": ("db_finance_ledger_category", "category_id"),
        "receivables": ("db_finance_receivable", "receivable_id"), "payables": ("db_finance_payable", "payable_id"),
        "packages": ("pack_template", "template_id"),
        "paymentPlans": ("db_company_payment_plan", "template_id"),
        "constructionPlans": ("db_company_construction_plan", "template_id"),
        "quoteLines": ("quote_line", "item_id"),
    }
    return {key: current_count(table, pk) for key, (table, pk) in specs.items()}


def seed() -> dict:
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "evidence").mkdir(parents=True, exist_ok=True)
    baseline = fixture_counts()
    designer_emp = first_pk("db_tenant_employee", "emp_id", f"com_id={COMPANY} AND role='Designer'")
    pm_emp = first_pk("db_tenant_employee", "emp_id", f"com_id={COMPANY} AND role='Pm'")

    customer_src = first_pk("cust_customer", "cust_id", f"com_id={COMPANY}")
    property_src = first_pk("db_property", "property_id", f"com_id={COMPANY}")
    design_src = first_pk("db_design", "design_id", f"com_id={COMPANY}")
    quote_src = first_pk("quote", "quote_id", f"com_id={COMPANY}")
    project_src = first_pk("db_project", "project_id", f"com_id={COMPANY}")
    partner_src = first_pk("db_partner", "partner_id", f"com_id={COMPANY}")
    catalog_src = first_pk("catalog_item", "item_id", f"com_id={COMPANY}")
    receivable_src = first_pk("db_finance_receivable", "receivable_id", f"com_id={COMPANY}")
    payable_src = first_pk("db_finance_payable", "payable_id", f"com_id={COMPANY}")
    pack_src = first_pk("pack_template", "template_id", f"com_id={COMPANY}")
    payment_src = first_pk("db_company_payment_plan", "template_id", f"com_id={COMPANY}")
    construction_src = first_pk("db_company_construction_plan", "template_id", f"com_id={COMPANY}")
    quote_line_src = first_pk("quote_line", "item_id", f"quote_id={quote_src}")

    customer_count = max(10 * baseline["customers"], 200)
    project_count = max(10 * baseline["projects"], 200)
    quote_count = max(10 * baseline["quotes"], 200)
    employee_count = max(10 * baseline["employees"], 200)

    customer_ids = [BASE + 1000 + i for i in range(customer_count)]
    property_ids = [BASE + 3000 + i for i in range(200)]
    design_ids = [BASE + 5000 + i for i in range(max(10 * baseline["designs"], 200))]
    quote_ids = [BASE + 7000 + i for i in range(quote_count)]
    project_ids = [BASE + 9000 + i for i in range(project_count)]
    employee_ids = [BASE + 11000 + i for i in range(employee_count)]
    quote_line_ids = [BASE + 13000 + i for i in range(200)]

    clone_rows("cust_customer", "cust_id", customer_src, customer_ids, lambda i, pk: {
        "cust_id": sql_value(pk), "name": sql_value(f"{PREFIX}-客户-{i:03d}"),
        "tel_no": sql_value(f"1889{i:08d}"), "community": sql_value(f"基准小区{i:03d}"),
        "addr": sql_value(f"基准路{i:03d}号"), "status": "'Following'", "emp_id": str(designer_emp),
    })
    clone_rows("db_property", "property_id", property_src, property_ids, lambda i, pk: {
        "property_id": sql_value(pk), "property_no": sql_value(f"BENCH-H-{i:04d}"),
        "customer_id": str(customer_ids[0]), "community": sql_value(f"基准小区{i:03d}"),
        "building": sql_value(f"{i}栋"), "room_no": sql_value(f"{i:03d}"),
        "address": sql_value(f"基准小区{i}栋{i:03d}"), "status": "'Quoted'", "owner_id": str(designer_emp),
    })
    clone_rows("db_design", "design_id", design_src, design_ids, lambda i, pk: {
        "design_id": sql_value(pk), "no": sql_value(f"BENCH-D-{i:04d}"), "name": sql_value(f"{PREFIX}-方案-{i:03d}"),
        "customer_id": str(customer_ids[0]), "property_id": str(property_ids[(i - 1) % len(property_ids)]),
        "owner_id": str(designer_emp),
    })
    clone_rows("quote", "quote_id", quote_src, quote_ids, lambda i, pk: {
        "quote_id": sql_value(pk), "no": sql_value(f"BENCH-Q-{i:04d}"), "title": sql_value(f"{PREFIX}-报价-{i:03d}"),
        "customer_id": str(customer_ids[0]), "property_id": str(property_ids[(i - 1) % len(property_ids)]),
        "design_id": str(design_ids[(i - 1) % len(design_ids)]), "status": "'Accepted'", "emp_id": str(designer_emp),
    })
    clone_rows("db_project", "project_id", project_src, project_ids, lambda i, pk: {
        "project_id": sql_value(pk), "no": sql_value(f"BENCH-P-{i:04d}"), "name": sql_value(f"{PREFIX}-项目-{i:03d}"),
        "customer_id": str(customer_ids[0]), "property_id": str(property_ids[(i - 1) % len(property_ids)]),
        "quote_id": str(quote_ids[(i - 1) % len(quote_ids)]), "manager_id": str(pm_emp),
        "designer_id": str(designer_emp), "status": "'Ongoing'",
    })
    clone_rows("db_tenant_employee", "emp_id", designer_emp, employee_ids, lambda i, pk: {
        "emp_id": sql_value(pk), "name": sql_value(f"B10X员工{i:03d}"),
        "tel_no": sql_value(f"1779{i:08d}"), "role": "'Designer'", "status": "'Active'",
    })
    clone_rows("db_partner", "partner_id", partner_src, [BASE + 15000 + i for i in range(200)], lambda i, pk: {
        "partner_id": sql_value(pk), "name": sql_value(f"{PREFIX}-供应商-{i:03d}"),
        "contact_name": sql_value(f"联系人{i}"), "tel_no": sql_value(f"1669{i:08d}"),
    })
    catalog_ids = [BASE + 17000 + i for i in range(200)]
    clone_rows("catalog_item", "item_id", catalog_src, catalog_ids, lambda i, pk: {
        "item_id": sql_value(pk), "code": sql_value(f"BENCH-CAT-{i:04d}"),
        "name": sql_value(f"{PREFIX}-报价项目-{i:03d}"), "type": "'Material'",
    })
    clone_rows("quote_line", "item_id", quote_line_src, quote_line_ids, lambda i, pk: {
        "item_id": sql_value(pk), "quote_id": str(quote_ids[0]), "property_id": str(property_ids[0]),
        "catalog_item_id": str(catalog_ids[(i - 1) % len(catalog_ids)]),
        "name": sql_value(f"{PREFIX}-报价行-{i:03d}"), "sort": str(i), "role": "'Base'",
    })
    receivable_ids = [BASE + 19000 + i for i in range(200)]
    clone_rows("db_finance_receivable", "receivable_id", receivable_src, receivable_ids, lambda i, pk: {
        "receivable_id": sql_value(pk), "name": sql_value(f"{PREFIX}-应收-{i:03d}"),
        "project_id": str(project_ids[0]), "customer_id": str(customer_ids[0]),
        "property_id": str(property_ids[0]), "contract_id": "NULL", "stage_id": "NULL",
        "status": "'Pending'", "received_amount": "0",
    })
    payable_ids = [BASE + 21000 + i for i in range(200)]
    clone_rows("db_finance_payable", "payable_id", payable_src, payable_ids, lambda i, pk: {
        "payable_id": sql_value(pk), "name": sql_value(f"{PREFIX}-应付-{i:03d}"),
        "project_id": str(project_ids[0]), "property_id": str(property_ids[0]),
        "source_type": sql_value("BenchmarkPayable"), "source_id": str(i), "source_no": sql_value(f"BENCH-AP-{i:04d}"),
        "material_plan_id": "NULL", "purchase_order_id": "NULL", "payee_emp_id": "NULL",
        "payee_partner_id": str(BASE + 15000), "type": "'Supplier'", "status": "'Pending'", "paid_amount": "0",
    })
    clone_rows("pack_template", "template_id", pack_src, [BASE + 23000 + i for i in range(200)], lambda i, pk: {
        "template_id": sql_value(pk), "code": sql_value(f"BENCH-PACK-{i:04d}"), "name": sql_value(f"{PREFIX}-套餐-{i:03d}"),
        "status": "'Published'", "is_current_published": "1",
    })
    clone_rows("db_company_payment_plan", "template_id", payment_src, [BASE + 25000 + i for i in range(200)], lambda i, pk: {
        "template_id": sql_value(pk), "code": sql_value(f"BENCH-PAYPLAN-{i:04d}"), "name": sql_value(f"{PREFIX}-付款计划-{i:03d}"),
        "status": "'Enabled'", "is_current_enabled": "1", "is_default": "0",
    })
    clone_rows("db_company_construction_plan", "template_id", construction_src, [BASE + 27000 + i for i in range(200)], lambda i, pk: {
        "template_id": sql_value(pk), "code": sql_value(f"BENCH-CONPLAN-{i:04d}"), "name": sql_value(f"{PREFIX}-施工计划-{i:03d}"),
        "status": "'Enabled'", "is_current_enabled": "1", "is_default": "0",
    })

    manual = ["START TRANSACTION;"]
    for i in range(200):
        wid = BASE + 29000 + i
        manual.append(
            f"INSERT INTO db_worker (worker_id,name,trade,phone,status,com_id) VALUES "
            f"({wid},{sql_value(PREFIX + '-工人-' + str(i+1).zfill(3))},{sql_value('木工')},{sql_value(f'1559{i:08d}')},'Active',{COMPANY});"
        )
        tid = BASE + 31000 + i
        manual.append(
            "INSERT INTO db_content_template "
            "(template_id,name,format,duration_sec,clip_min,clip_max,status,version,allowed_asset_types,com_id) VALUES "
            f"({tid},{sql_value(PREFIX + '-模板-' + str(i+1).zfill(3))},'9:16',{30 + i},3,8,'InternalReady',1,'Image,Video',{COMPANY});"
        )
        eid = BASE + 33000 + i
        manual.append(
            "INSERT INTO db_finance_expense_category "
            "(category_id,code,name,description,built_in,active,sort_no,com_id,creator_id) VALUES "
            f"({eid},{sql_value(f'BENCH-EXP-{i+1:04d}')},{sql_value(PREFIX + '-费用分类-' + str(i+1).zfill(3))},"
            f"{sql_value('10× 基准费用分类')},0,1,{i+1},{COMPANY},{BOSS});"
        )
        lid = BASE + 35000 + i
        direction = "Income" if i % 2 == 0 else "Expense"
        manual.append(
            "INSERT INTO db_finance_ledger_category "
            "(category_id,code,name,description,direction,built_in,active,sort_no,com_id,creator_id,parent_id) VALUES "
            f"({lid},{sql_value(f'BENCH-LEDGER-{direction.upper()}-{i+1:04d}')},{sql_value(PREFIX + '-账务分类-' + str(i+1).zfill(3))},"
            f"{sql_value('10× 基准账务分类')},'{direction}',0,1,{i+1},{COMPANY},{BOSS},NULL);"
        )
    manual.append("COMMIT;")
    mysql_exec("\n".join(manual))

    mysql_exec("ANALYZE TABLE cust_customer, db_property, db_design, quote, db_project, db_tenant_employee, db_worker, db_partner, catalog_item, quote_line, db_finance_receivable, db_finance_payable, pack_template, db_content_template, db_finance_expense_category, db_finance_ledger_category, db_company_payment_plan, db_company_construction_plan;")
    after = fixture_counts()
    state = {
        "prefix": PREFIX,
        "base": BASE,
        "baseline": baseline,
        "after": after,
        "context": {
            "customerId": customer_ids[0], "propertyId": property_ids[0], "designId": design_ids[0],
            "quoteId": quote_ids[0], "projectId": project_ids[0],
            "partnerId": BASE + 15000, "catalogItemId": catalog_ids[0],
        },
    }
    STATE.write_text(json.dumps(state, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(state, ensure_ascii=False, indent=2))
    return state


def cleanup() -> None:
    ranges = {
        "quote_line": ("item_id", 13000, 13200), "db_finance_receivable": ("receivable_id", 19000, 19200),
        "db_finance_payable": ("payable_id", 21000, 21200), "db_project": ("project_id", 9000, 12000),
        "quote": ("quote_id", 7000, 10000), "db_design": ("design_id", 5000, 8000),
        "db_property": ("property_id", 3000, 4000), "cust_customer": ("cust_id", 1000, 3000),
        "db_tenant_employee": ("emp_id", 11000, 13000), "db_worker": ("worker_id", 29000, 31000),
        "db_partner": ("partner_id", 15000, 17000), "catalog_item": ("item_id", 17000, 19000),
        "db_content_template": ("template_id", 31000, 33000),
        "db_finance_expense_category": ("category_id", 33000, 35000),
        "db_finance_ledger_category": ("category_id", 35000, 37000),
        "pack_template": ("template_id", 23000, 25000),
        "db_company_payment_plan": ("template_id", 25000, 27000),
        "db_company_construction_plan": ("template_id", 27000, 29000),
    }
    statements = ["START TRANSACTION;"]
    for table, (pk, start, end) in ranges.items():
        statements.append(f"DELETE FROM {table} WHERE {pk} BETWEEN {BASE + start} AND {BASE + end};")
    statements.append("COMMIT;")
    mysql_exec("\n".join(statements))
    print(json.dumps({"cleanup": "PASS", "counts": fixture_counts()}, ensure_ascii=False, indent=2))


class SelectorBench:
    def __init__(self, state: dict):
        self.state = state
        self.context = state["context"]
        self.tokens = {"boss": login("13910002001"), "designer": login("13910002004"), "pm": login("13910002002")}

    def request_case(self, name: str, role: str, calls: list[tuple[str, dict]], row_counter: Callable[[list], int] | None = None):
        token = self.tokens[role]
        before = questions()
        started = time.perf_counter()
        bodies = []
        for route, payload in calls:
            data, _ = api_post(token, route, payload)
            bodies.append(data)
        elapsed = (time.perf_counter() - started) * 1000
        after = questions()
        rows = sum(page_rows(body)[0] for body in bodies)
        total = sum(page_rows(body)[1] for body in bodies)
        if row_counter:
            rows = row_counter(bodies)
            total = rows
        return {
            "latencyMs": round(elapsed, 2), "rowCount": rows, "totalElements": total,
            "sqlStatements": max(0, after - before - 1),
        }

    async def p95(self, name: str, role: str, calls: list[tuple[str, dict]], row_counter=None, n=20, concurrency=5) -> dict:
        token = self.tokens[role]
        durations = []
        errors = 0
        sem = asyncio.Semaphore(concurrency)

        async def one(session: aiohttp.ClientSession):
            nonlocal errors
            async with sem:
                started = time.perf_counter()
                try:
                    for route, payload in calls:
                        async with session.post(f"{API}/{route}", headers={"token": token}, json=payload) as response:
                            body = await response.json()
                            if response.status >= 400 or not body.get("success"):
                                raise RuntimeError(body)
                    durations.append((time.perf_counter() - started) * 1000)
                except Exception:
                    errors += 1

        async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=120)) as session:
            await asyncio.gather(*(one(session) for _ in range(n)))
        ordered = sorted(durations)
        def pick(p: float):
            if not ordered:
                return None
            return ordered[min(len(ordered) - 1, max(0, round((len(ordered) - 1) * p)))]
        return {
            "samples": len(durations), "errors": errors,
            "avgMs": round(statistics.mean(durations), 2) if durations else None,
            "p50Ms": round(pick(0.50), 2) if durations else None,
            "p95Ms": round(pick(0.95), 2) if durations else None,
            "maxMs": round(max(durations), 2) if durations else None,
        }

    def cases(self) -> list[tuple[str, str, list[tuple[str, dict]], Callable[[list], int] | None, int]]:
        c = self.context
        return [
            ("客户选择器", "boss", [("customer/customer/page", {"current": 1, "size": 20})], None, 20),
            ("房屋选择器", "boss", [("property/property/page", {"customerId": c["customerId"]})], None, 200),
            ("设计方案选择器", "boss", [("design/scheme/page", {"customerId": c["customerId"], "current": 1, "size": 20})], None, 20),
            ("套餐模板选择器", "boss", [("pack/template/page", {"current": 1, "size": 20})], None, 20),
            ("基础报价行选择器", "boss", [("quote/line/page", {"quoteId": c["quoteId"], "role": "Base", "current": 1, "size": 20})], None, 20),
            ("客户确认报价选择器", "boss", [("quote/page", {"status": "Accepted", "current": 1, "size": 20})], None, 20),
            ("设计师报价选择器", "designer", [("quote/designer/page", {"status": "Accepted", "current": 1, "size": 20})], None, 20),
            ("员工选择器", "boss", [("user/employee/page", {"current": 1, "size": 20, "status": "Active"})], None, 20),
            ("薪酬员工选择器", "boss", [("payroll/employee/page", {"current": 1, "size": 20, "status": "Active"})], None, 20),
            ("工人选择器", "boss", [("labor/worker/page", {"current": 1, "size": 20, "status": "Active"})], None, 20),
            ("供应商选择器", "boss", [("partner/page", {"current": 1, "size": 20, "status": "Active"})], None, 20),
            ("内容模板选择器", "boss", [("content/template/page", {"current": 1, "size": 20})], None, 20),
            ("内容项目选择器", "boss", [("project/project/picker/page", {"current": 1, "size": 20})], None, 20),
            ("增减项项目选择器", "boss", [("project/project/picker/page", {"current": 1, "size": 20, "statuses": ["Ongoing"]})], None, 20),
            ("汇报项目选择器", "boss", [("project/project/picker/page", {"current": 1, "size": 20})], None, 20),
            ("财务项目选择器", "boss", [("project/project/picker/page", {"current": 1, "size": 20})], None, 20),
            ("应收选择器", "boss", [("finance/receivable/page", {"projectId": c["projectId"], "current": 1, "size": 20})], None, 20),
            ("应付选择器", "boss", [("finance/payable/page", {"projectId": c["projectId"], "current": 1, "size": 20})], None, 20),
            ("账务分类选择器", "boss", [("finance/ledger-category/tree", {})], None, 250),
            ("费用分类选择器", "boss", [("expense/categories/page", {"current": 1, "size": 20})], None, 20),
            ("付款计划选择器", "boss", [("plan/template/picker/page", {"kind": "Payment", "current": 1, "size": 20})], lambda bodies: len((bodies[0] or {}).get("paymentPlans") or []), 20),
            ("施工计划选择器", "boss", [("plan/template/picker/page", {"kind": "Construction", "current": 1, "size": 20})], lambda bodies: len((bodies[0] or {}).get("constructionPlans") or []), 20),
            ("报价库选择器", "boss", [("catalog/item/tree", {"type": "Material"}), ("catalog/item/page", {"type": "Material", "current": 1, "size": 30})], None, 30),
        ]

    async def run(self) -> dict:
        cases = []
        for name, role, calls, row_counter, max_rows in self.cases():
            first = self.request_case(name, role, calls, row_counter)
            latency = await self.p95(name, role, calls, row_counter)
            low_calls = json.loads(json.dumps(calls))
            reduced = False
            for _, payload in low_calls:
                if "size" in payload:
                    payload["size"] = 10
                    reduced = True
            low_size = self.request_case(name + "-size10", role, low_calls, row_counter) if reduced else None
            sql_growth = (first["sqlStatements"] - low_size["sqlStatements"]) if low_size else None
            sql_budget = 30 * max(1, len(calls))
            sql_ok = first["sqlStatements"] <= sql_budget or (sql_growth is not None and sql_growth <= 2 and first["sqlStatements"] <= 60)
            passed = (latency["errors"] == 0 and latency["p95Ms"] is not None and latency["p95Ms"] <= 500
                      and first["rowCount"] <= max_rows and sql_ok)
            cases.append({
                "name": name, "role": role, "maxRows": max_rows,
                "single": first, "size10": low_size, "sqlGrowthFor10Rows": sql_growth,
                "sqlBudget": sql_budget, "sqlResult": "PASS" if sql_ok else "FAIL",
                "p95": latency, "result": "PASS" if passed else "FAIL",
            })
            print(json.dumps(cases[-1], ensure_ascii=False))
        return {
            "generatedAt": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
            "api": API, "fixture": self.state,
            "criteria": {"p95MsMax": 500, "sqlStatementsMax": 30, "rowCountMaxPerCase": "case.maxRows"},
            "cases": cases,
            "summary": {"total": len(cases), "pass": sum(1 for c in cases if c["result"] == "PASS"),
                        "fail": sum(1 for c in cases if c["result"] == "FAIL")},
        }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["seed", "benchmark", "cleanup", "all"], default="all", nargs="?")
    parser.add_argument("--keep", action="store_true")
    args = parser.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "evidence").mkdir(parents=True, exist_ok=True)
    if args.mode in {"seed", "all"}:
        state = seed()
    elif args.mode == "benchmark":
        state = json.loads(STATE.read_text(encoding="utf-8"))
    else:
        cleanup()
        return 0
    if args.mode in {"benchmark", "all"}:
        result = asyncio.run(SelectorBench(state).run())
        RESULT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(result["summary"], ensure_ascii=False, indent=2))
    if args.mode == "all" and not args.keep:
        cleanup()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
