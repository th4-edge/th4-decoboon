#!/usr/bin/env python3
"""Capture post-action Provider and DB facts for the arrival/payable/cost RUN."""
from __future__ import annotations
import csv
import io
import json
import subprocess
from datetime import datetime
from pathlib import Path
import requests

ROOT = Path(__file__).resolve().parents[5]
RUN = ROOT / "docs/acceptance/runs/RUN-20261101-MAT-ARRIVAL-FIN-001"
API = "http://127.0.0.1:8090/api"
PASSWORD = "Audit2026!"
PROJECT_ID = "242541620656865280"
PURCHASE_ID = "1793518416069167759"
PURCHASE_ITEM_ID = "1793518416071493403"
PAYABLE_ID = "1793518416093711825"
PLAN_ID = "242541620837220352"
SHEET_ID = "1793517336944847641"


def login(phone: str) -> str:
    response = requests.post(f"{API}/auth/session/password-login", json={"phone": phone, "password": PASSWORD}, timeout=20)
    body = response.json()
    if not response.ok or not body.get("success"):
        raise RuntimeError(f"login failed: {phone}")
    return body["data"]["token"]


def provider(token: str, route: str, payload: dict) -> dict:
    response = requests.post(f"{API}/{route}", headers={"token": token, "Content-Type": "application/json"}, json=payload, timeout=30)
    return {"httpStatus": response.status_code, "body": response.json()}


def sql(statement: str) -> list[dict]:
    result = subprocess.run(
        ["mysql", "-h127.0.0.1", "-uroot", "-pmysqladmin", "-Ddecoboon", "--batch", "--raw", "-e", statement],
        check=True, capture_output=True, text=True)
    rows = list(csv.reader(io.StringIO(result.stdout), delimiter="\t"))
    if not rows:
        return []
    return [dict(zip(rows[0], row)) for row in rows[1:]]


def write(relative: str, value) -> None:
    path = RUN / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


pm = login("13910002006")
finance = login("13910002003")
arrival_api = provider(pm, "project/goods-receipt/page", {"purchaseId": PURCHASE_ID, "current": 1, "size": 20})
payable_api = provider(finance, "finance/payable/page", {"projectId": PROJECT_ID, "current": 1, "size": 20})
payment_api = provider(finance, "finance/payment/page", {"projectId": PROJECT_ID, "current": 1, "size": 20})
project_report_api = provider(finance, "finance/project/report", {"projectId": PROJECT_ID})
partner_ledger_api = provider(finance, "finance/partner-ledger/report", {"partnerId": "1793457851296966369"})
db = {
    "project": sql(f"select project_id,manager_id,support_id,status,contract_amount,cost from db_project where project_id={PROJECT_ID}"),
    "purchase": sql(f"select purchase_id,status,quantity,received_quantity,unit_price,amount,material_plan_id from db_purchase_order where purchase_id={PURCHASE_ID}"),
    "purchase_item": sql(f"select purchase_item_id,status,quantity,received_quantity,unit_price,amount,material_plan_id from db_purchase_order_item where purchase_order_id={PURCHASE_ID}"),
    "receipts": sql(f"select receipt_id,quantity,arrived_at,inspector_id,remark from db_goods_receipt where purchase_id={PURCHASE_ID} order by receipt_id"),
    "receipt_images": sql(f"select receipt_id,url from db_goods_receipt_images where receipt_id in (select receipt_id from db_goods_receipt where purchase_id={PURCHASE_ID})"),
    "material_plan": sql(f"select plan_id,status,quantity,cost,arrived_at from db_material_plan where plan_id={PLAN_ID}"),
    "material_sheet": sql(f"select sheet_id,status from db_material_order_sheet where sheet_id={SHEET_ID}"),
    "payables": sql(f"select payable_id,status,amount,paid_amount,receipt_date,due_date,source_no from db_finance_payable where project_id={PROJECT_ID} order by payable_id"),
    "payments": sql(f"select payment_id,payable_id,amount,paid_at,account,payee_name,transaction_no,idempotency_key from db_finance_payment where project_id={PROJECT_ID} order by payment_id"),
    "settlements": sql(f"select settlement_id,payment_id,payable_id,amount from db_finance_payable_settlement where payable_id={PAYABLE_ID}"),
    "ledgers": sql(f"select ledger_id,source,source_id,direction,amount,project_id,store_id,counterparty,summary,voided from db_finance_ledger where project_id={PROJECT_ID} order by ledger_id"),
}
arrival = {
    "capturedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
    "role": "Pm",
    "surface": "App+Provider+DB",
    "pagePath": "subpackages/supply/pages/receipt-edit",
    "purchaseId": PURCHASE_ID,
    "purchaseItemId": PURCHASE_ITEM_ID,
    "provider": arrival_api,
    "db": db,
    "checks": {
        "receiptCount": len(db["receipts"]) == 1,
        "receiptImageCount": len(db["receipt_images"]),
        "purchaseReceived": db["purchase"][0]["status"] == "Received" and float(db["purchase"][0]["received_quantity"]) == 2.0,
        "itemReceived": db["purchase_item"][0]["status"] == "Received" and float(db["purchase_item"][0]["received_quantity"]) == 2.0,
        "sheetArrived": db["material_sheet"][0]["status"] == "Arrived",
        "planPartiallyArrivedAsExpected": db["material_plan"][0]["status"] == "PartiallyArrived" and float(db["material_plan"][0]["quantity"]) == 12.0,
        "payableDueDateBackfilled": db["payables"][0]["receipt_date"] == "2026-11-01" and db["payables"][0]["due_date"] == "2026-12-01",
    },
}
finance_payable = {
    "capturedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
    "role": "Finance",
    "surface": "Web+App+Provider+DB",
    "payable": payable_api,
    "payment": payment_api,
    "partnerLedger": partner_ledger_api,
    "db": db,
    "checks": {
        "payablePaid": db["payables"][0]["status"] == "Paid" and float(db["payables"][0]["paid_amount"]) == 240.0,
        "onePayment": len(db["payments"]) == 1 and float(db["payments"][0]["amount"]) == 240.0,
        "oneSettlement": len(db["settlements"]) == 1 and float(db["settlements"][0]["amount"]) == 240.0,
        "oneExpenseLedger": len(db["ledgers"]) == 1 and db["ledgers"][0]["direction"] == "Expense" and float(db["ledgers"][0]["amount"]) == 240.0,
    },
}
finance_cost = {
    "capturedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
    "role": "Finance",
    "surface": "Web+App+Provider+DB",
    "projectReport": project_report_api,
    "db": db,
    "checks": {
        "actualCost240": project_report_api["body"]["data"]["actualCost"] == 240.0,
        "totalCost240": project_report_api["body"]["data"]["totalCost"] == 240.0,
        "costCompleteFalse": project_report_api["body"]["data"]["costComplete"] is False,
        "costIncompleteCount2": project_report_api["body"]["data"]["costIncompleteCount"] == 2,
        "profitNull": project_report_api["body"]["data"].get("profitAmount") is None,
        "grossMarginNull": project_report_api["body"]["data"].get("grossMargin") is None,
    },
}
write("evidence/TC-MAT-PM-ARRIVAL-005/11-arrival-provider-db-final.json", arrival)
write("evidence/TC-FIN-FINANCE-PAYABLE-003/10-payable-provider-db-final.json", finance_payable)
write("evidence/TC-FIN-FINANCE-COST-004/10-project-cost-provider-db-final.json", finance_cost)
print(json.dumps({
    "arrivalChecks": arrival["checks"],
    "payableChecks": finance_payable["checks"],
    "costChecks": finance_cost["checks"],
}, ensure_ascii=False, indent=2))
