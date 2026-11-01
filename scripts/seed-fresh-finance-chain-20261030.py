#!/usr/bin/env python3
"""AUDIT-20261030 财务链：通过正式 Provider 创建应收、收款、应付、付款、台账。"""
from __future__ import annotations

import json
import os
import subprocess
from datetime import date, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / "docs/operations/data-changes/2026-10-30-fresh-business-audit-finance-state.json"
API = "http://127.0.0.1:8090/api"
PASSWORD = "Audit2026!"
PROJECT_ID = "241926651905048576"
CONFIG = ROOT / "src/main/resources/application-dev.yml"


def db_password() -> str:
    for line in CONFIG.read_text(encoding="utf-8").splitlines():
        if line.strip().startswith("password:"):
            return line.split(":", 1)[1].strip()
    raise RuntimeError("missing datasource password")


def db(sql: str) -> list[list[str]]:
    p = subprocess.run(["mysql", "-h127.0.0.1", "-P3306", "-uroot", "-D", "decoboon", "-N", "-B", "-e", sql],
                       capture_output=True, text=True, env={**os.environ, "MYSQL_PWD": db_password()})
    if p.returncode != 0:
        raise RuntimeError(p.stderr)
    return [line.split("\t") for line in p.stdout.splitlines() if line]


def login(phone: str) -> str:
    out = subprocess.run(["curl", "-sS", "-X", "POST", API + "/auth/session/password-login",
                          "-H", "Content-Type: application/json",
                          "-d", json.dumps({"phone": phone, "password": PASSWORD})],
                         capture_output=True, text=True, timeout=10)
    body = json.loads(out.stdout or "{}")
    if not body.get("success"):
        raise RuntimeError(("login", phone, body))
    return body["data"]["token"]


def post(token: str, path: str, payload: dict) -> dict:
    out = subprocess.run(["curl", "-sS", "-X", "POST", API + "/" + path,
                          "-H", "Content-Type: application/json", "-H", "token: " + token,
                          "-d", json.dumps(payload, ensure_ascii=False)],
                         capture_output=True, text=True, timeout=20)
    raw = out.stdout
    try:
        body = json.loads(raw)
    except json.JSONDecodeError:
        if raw.startswith('{"data":'):
            return {}
        raise RuntimeError(("invalid-json", path, raw[:300]))
    if not body.get("success"):
        raise RuntimeError((path, body))
    return body.get("data") or {}


def main() -> int:
    finance = login("13910002003")
    today = date.today()
    # 手工其他应收
    post(finance, "finance/receivable/save", {
        "projectId": PROJECT_ID, "name": "AUDIT-20261030 补充应收", "stageName": "补充款",
        "type": "Other", "amount": 500.00, "dueDate": (today + timedelta(days=30)).isoformat(),
        "remark": "审计补充应收",
    })
    # 首期自动应收收款
    first = db(f"SELECT receivable_id,amount FROM db_finance_receivable WHERE com_id=199500000000000001 AND project_id={PROJECT_ID} AND name='合同签约款' LIMIT 1")[0]
    receivable_id, amount = first[0], first[1]
    post(finance, "finance/receipt/save", {
        "projectId": PROJECT_ID, "receivableId": receivable_id, "amount": float(amount),
        "account": "CompanyBank", "receivedAt": today.isoformat(), "payerName": "审计业主",
        "remark": "AUDIT-20261030 首期收款", "idempotencyKey": "AUDIT-20261030-RECEIPT-001",
    })
    # 手工费用支出应付
    post(finance, "finance/payable/save", {
        "projectId": PROJECT_ID, "name": "AUDIT-20261030 项目审计费用", "type": "Expense",
        "payeeName": "审计供应商", "amount": 2000.00, "dueDate": (today + timedelta(days=20)).isoformat(),
        "remark": "审计费用支出",
    })
    payable_id, payable_amount = db(f"SELECT payable_id,amount FROM db_finance_payable WHERE com_id=199500000000000001 AND project_id={PROJECT_ID} AND name='AUDIT-20261030 项目审计费用' LIMIT 1")[0]
    post(finance, "finance/payment/save", {
        "payableId": payable_id, "amount": 500.00, "account": "CompanyBank", "paidAt": today.isoformat(),
        "payeeName": "审计供应商", "remark": "AUDIT-20261030 首笔付款",
        "idempotencyKey": "AUDIT-20261030-PAYMENT-001",
    })
    post(finance, "finance/ledger/save", {
        "projectId": PROJECT_ID, "direction": "Expense", "purpose": "ProjectIncidentalCost",
        "category": "项目零星其他支出", "amount": 800.00, "account": "CompanyBank",
        "occurredAt": today.isoformat(), "counterparty": "审计商户",
        "summary": "AUDIT-20261030 项目零星支出",
    })
    post(finance, "finance/ledger/save", {
        "direction": "Expense", "purpose": "CompanyExpense", "category": "企业其他费用",
        "amount": 300.00, "account": "CompanyBank", "occurredAt": today.isoformat(),
        "counterparty": "审计商户", "summary": "AUDIT-20261030 企业费用",
    })
    state = {
        "projectId": PROJECT_ID,
        "receivableId": receivable_id,
        "manualPayableId": payable_id,
        "receiptAmount": float(amount),
        "manualPayableAmount": float(payable_amount),
        "paymentAmount": 500.00,
        "manualProjectExpense": 800.00,
        "manualCompanyExpense": 300.00,
        "executedAt": date.today().isoformat(),
    }
    STATE.write_text(json.dumps(state, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(state, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
