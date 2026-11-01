#!/usr/bin/env python3
"""为全链路阶段夹具补齐材料采购、到场、收付款和结算数据。"""
from __future__ import annotations

import argparse
import json
from datetime import date, timedelta
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parents[1]
API = "http://127.0.0.1:8090/api"
PASSWORD = "Audit2026!"
CHAIN_STATE = ROOT / "docs/operations/data-changes/2026-10-30-fresh-business-audit-chain-state.json"
OUTPUT = ROOT / "docs/acceptance/runs/20261031-full-chain-stage-fixture/finance-chain-state.json"


def login(phone: str) -> str:
    response = requests.post(f"{API}/auth/session/password-login",
                             json={"phone": phone, "password": PASSWORD}, timeout=20)
    body = response.json()
    if not response.ok or not body.get("success"):
        raise RuntimeError(f"login failed {phone}: {body}")
    return body["data"]["token"]


def post(token: str, route: str, payload: dict | None = None) -> dict:
    response = requests.post(f"{API}/{route}", headers={"token": token, "Content-Type": "application/json"},
                             json=payload or {}, timeout=30)
    body = response.json()
    if not body.get("success"):
        raise RuntimeError(f"{route} failed: {json.dumps(body, ensure_ascii=False)}")
    return body.get("data") or {}


def rows(page: dict | list) -> list[dict]:
    if isinstance(page, list):
        return page
    return page.get("content") or page.get("list") or []


def enum_key(value) -> str:
    if isinstance(value, dict):
        return str(value.get("key") or value.get("value") or value.get("name") or "")
    return str(value or "")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--chain-state", default=str(CHAIN_STATE))
    parser.add_argument("--output", default=str(OUTPUT))
    args = parser.parse_args()

    chain = json.loads(Path(args.chain_state).read_text(encoding="utf-8"))
    record = next(iter(chain.values()))
    project_id = str(record["projectId"])
    property_id = str(record["propertyId"])
    contract_id = str(record["contractId"])

    boss = login("13910002001")
    finance = login("13910002003")

    partners = rows(post(boss, "partner/page", {"current": 1, "size": 50}))
    supplier = next((item for item in partners if item.get("name") == "全链路验收材料供应商"), None)
    if supplier is None:
        supplier = post(boss, "partner/save", {
            "type": "Supplier",
            "name": "全链路验收材料供应商",
            "contactName": "材料联系人",
            "telNo": "13910002991",
            "settlementMode": "ReceiptDays",
            "settlementDays": 30,
            "remark": "本地全链路验收合成供应商",
        })
    supplier_id = int(supplier["partnerId"])

    lines = rows(post(boss, "project/contract-line/page", {"projectId": project_id, "current": 1, "size": 100}))
    material_line = next(item for item in lines if enum_key(item.get("type")) == "Material")
    selection = post(boss, "material/selection/save", {
        "propertyId": property_id,
        "contractId": contract_id,
        "projectId": project_id,
        "contractLineId": str(material_line["lineId"]),
        "title": "全链路验收主材选材",
        "content": "业主确认品牌、花色和规格，交付材料下单。",
        "brand": "验收品牌",
        "spec": "多层实木 910×125",
        "color": "原木色",
        "deliver": True,
    })

    plans = rows(post(boss, "project/material-plan/page", {"projectId": project_id, "current": 1, "size": 50}))
    plan = next(item for item in plans if enum_key(item.get("status")) != "Closed")
    plan_id = str(plan["planId"])
    quantity = float(plan.get("netQuantity") or plan.get("quantity") or 1)

    sheets = rows(post(boss, "material/sheet/page", {"projectId": project_id, "current": 1, "size": 50}))
    sheet = next((item for item in sheets if item.get("sheetType") == "Tile"), None)
    if sheet is None:
        sheet = post(boss, "material/sheet/save", {
            "projectId": project_id,
            "sheetType": "Tile",
            "manufacturerName": "全链路验收材料供应商",
            "supplierId": supplier_id,
            "groupChatAt": date.today().isoformat(),
            "installAt": (date.today() + timedelta(days=10)).isoformat(),
            "remark": "全链路验收作业单",
            "items": [{
                "materialPlan": {"planId": plan_id},
                "locationName": "全屋地面",
                "spaceName": "客餐厅",
                "materialName": plan.get("name") or "主材--实木复合地板",
                "brand": "验收品牌",
                "model": "YS-2026",
                "spec": "多层实木 910×125",
                "color": "原木色",
                "size": "910×125mm",
                "quantity": quantity,
                "unit": "Area",
            }],
        })
    sheet_id = str(sheet["sheetId"])
    if enum_key(sheet.get("status")) in {"Draft", "Measuring", "Measured"}:
        sheet = post(boss, "material/sheet/measure", {
            "sheetId": sheet_id,
            "measureAt": date.today().isoformat(),
            "deliverAt": (date.today() + timedelta(days=3)).isoformat(),
            "installAt": (date.today() + timedelta(days=10)).isoformat(),
            "signatures": "业主、设计师、项目经理",
        })
    if enum_key(sheet.get("status")) == "Draft":
        sheet = post(boss, "material/sheet/submit", {"sheetId": sheet_id})
    elif enum_key(sheet.get("status")) == "Measured":
        sheet = post(boss, "material/sheet/submit", {"sheetId": sheet_id})

    item_rows = sheet.get("items") or []
    if not item_rows:
        sheet = post(boss, "material/sheet/info", {"sheetId": sheet_id})
        item_rows = sheet.get("items") or []
    if not item_rows:
        sheets = rows(post(boss, "material/sheet/page", {"projectId": project_id, "current": 1, "size": 50}))
        sheet = next(item for item in sheets if str(item.get("sheetId")) == sheet_id)
        item_rows = sheet.get("items") or []
    item = item_rows[0]
    order_items = [{
        "itemId": str(item["itemId"]),
        "quantity": quantity,
        "purchasePrice": 120,
    }]
    if enum_key(sheet.get("status")) == "Submitted":
        sheet = post(boss, "material/sheet/order", {
            "sheetId": sheet_id,
            "supplierId": supplier_id,
            "supplierName": "全链路验收材料供应商",
            "deliveryMode": "Direct",
            "expectedAt": (date.today() + timedelta(days=3)).isoformat(),
            "remark": "全链路验收采购下单",
            "idempotencyKey": f"full-chain-order-{sheet_id}",
            "items": order_items,
        })

    purchases = rows(post(boss, "project/purchase/page", {"projectId": project_id, "current": 1, "size": 50}))
    purchase = next(item for item in purchases if item.get("supplierName") == "全链路验收材料供应商")
    purchase_id = str(purchase["purchaseId"])
    if float(purchase.get("receivedQuantity") or 0) < quantity:
        post(boss, "project/goods-receipt/save", {
            "purchaseId": purchase_id,
            "quantity": quantity,
            "arrivedAt": date.today().isoformat() + "T16:00:00",
            "images": ["/demo/full-chain/goods-receipt.jpg"],
            "remark": "全链路验收材料已到场",
        })

    payables = rows(post(finance, "finance/payable/page", {"projectId": project_id, "current": 1, "size": 50}))
    material_payable = next((item for item in payables if item.get("sourceNo") == purchase.get("no")), None)
    payment_id = ""
    if material_payable:
        payable_id = str(material_payable["payableId"])
        amount = float(material_payable.get("amount") or 0) - float(material_payable.get("paidAmount") or 0)
        if amount > 0:
            paid = post(finance, "finance/payment/save", {
                "payableId": payable_id,
                "amount": amount,
                "account": "CompanyBank",
                "paidAt": date.today().isoformat(),
                "payeeName": "全链路验收材料供应商",
                "remark": "全链路验收材料款",
                "idempotencyKey": f"full-chain-payment-{purchase_id}",
            })
            payment_id = str(paid.get("paymentId") or paid.get("id") or "")

    receivables = rows(post(finance, "finance/receivable/page", {"projectId": project_id, "current": 1, "size": 50}))
    receivable = next((item for item in receivables
                       if float(item.get("amount") or 0) - float(item.get("receivedAmount") or 0) > 0), None)
    receipt_id = ""
    if receivable:
        balance = float(receivable.get("amount") or 0) - float(receivable.get("receivedAmount") or 0)
        receipt_amount = round(min(balance, max(balance * 0.4, 100)), 2)
        receipt = post(finance, "finance/receipt/save", {
            "projectId": project_id,
            "receivableId": str(receivable["receivableId"]),
            "amount": receipt_amount,
            "account": "CompanyBank",
            "receivedAt": date.today().isoformat(),
            "payerName": "王芳（造数1100）",
            "remark": "全链路验收阶段收款",
            "idempotencyKey": f"full-chain-receipt-{project_id}-{receivable['receivableId']}",
        })
        receipt_id = str(receipt.get("receiptId") or receipt.get("id") or "")

    settlement = post(finance, "finance/settlement/save", {
        "projectId": project_id,
        "deductionAmount": 0,
        "rewardAmount": 0,
        "retentionRate": 3,
        "retentionReleaseAt": (date.today() + timedelta(days=180)).isoformat(),
        "remark": "全链路验收结算草稿",
    })

    output = {
        "projectId": project_id,
        "propertyId": property_id,
        "contractId": contract_id,
        "supplierId": str(supplier_id),
        "selectionId": str(selection.get("selectionId") or selection.get("id") or ""),
        "planId": plan_id,
        "sheetId": sheet_id,
        "purchaseId": purchase_id,
        "receiptId": receipt_id,
        "paymentId": payment_id,
        "settlementId": str(settlement.get("settlementId") or settlement.get("id") or ""),
        "generatedAt": date.today().isoformat(),
    }
    Path(args.output).write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(output, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
