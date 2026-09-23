#!/usr/bin/env python3
"""Complete the local Maihong project 41 supply/payment chain through real Providers."""
from __future__ import annotations
import json
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Any
import requests

API = "http://127.0.0.1:8091/api"
PASSWORD = "Maihong2026"
PROJECT_ID = "228259849996075008"
OUT = Path(__file__).resolve().parents[1] / "api" / "supply-chain-result.json"
BATCH = "AUDIT-SUPPLY-20260923"
PARTNER_NAME = "麦鸿审计测试材料商"
ACCOUNTS = {
    "designer": ("19900000003", "谢玲玲"),
    "finance": ("19900000005", "吴濛濛"),
}


def now_iso() -> str:
    return datetime.now().replace(microsecond=0).isoformat()


def page_items(value: Any) -> list[dict[str, Any]]:
    if isinstance(value, list):
        return value
    if isinstance(value, dict):
        for key in ("content", "list", "records", "items"):
            if isinstance(value.get(key), list):
                return value[key]
    return []


def enum_key(value: Any) -> str:
    if isinstance(value, dict):
        return str(value.get("key") or value.get("value") or value.get("name") or "")
    return str(value or "")


class Api:
    def __init__(self) -> None:
        self.tokens: dict[str, str] = {}

    def login(self) -> None:
        for role, (phone, name) in ACCOUNTS.items():
            body = requests.post(
                f"{API}/auth/session/password-login",
                json={"phone": phone, "password": PASSWORD},
                timeout=20,
            ).json()
            if not body.get("success"):
                raise RuntimeError(f"{role} login failed: {body}")
            self.tokens[role] = body["data"]["token"]
            print(f"[login] {role} {name}")

    def post(self, role: str, uri: str, payload: dict[str, Any] | None = None) -> Any:
        response = requests.post(
            f"{API}/{uri}",
            headers={"Content-Type": "application/json", "token": self.tokens[role]},
            json=payload or {},
            timeout=40,
        )
        body = response.json()
        if not body.get("success"):
            raise RuntimeError(f"{role} {uri} failed: {json.dumps(body, ensure_ascii=False)}")
        return body.get("data")


def find_partner(api: Api) -> str:
    rows = page_items(api.post("finance", "partner/page", {"type": "Supplier", "current": 1, "size": 200}))
    existing = next((row for row in rows if row.get("name") == PARTNER_NAME), None)
    if existing:
        return str(existing["partnerId"])
    saved = api.post("finance", "partner/save", {
        "type": "Supplier",
        "name": PARTNER_NAME,
        "contactName": "沉浸审计测试",
        "telNo": "19900009999",
        "settlementMode": "ReceiptDays",
        "settlementDays": 30,
        "brands": ["马可波罗", "三棵树", "九牧", "奥普", "志邦"],
        "remark": f"{BATCH} 本地供应商主数据",
    })
    return str(saved["partnerId"])


def get_state(api: Api) -> dict[str, Any]:
    info = api.post("designer", "project/project/info", {"projectId": PROJECT_ID})
    plans = page_items(api.post("designer", "project/material-plan/page", {
        "projectId": PROJECT_ID, "current": 1, "size": 100
    }))
    lines = page_items(api.post("designer", "project/contract-line/page", {
        "projectId": PROJECT_ID, "current": 1, "size": 200
    }))
    material_line = next((row for row in lines if enum_key(row.get("type")) == "Material"), None)
    construction_line = next((row for row in lines if enum_key(row.get("type")) == "Construction"), None)
    if not material_line or not construction_line or not plans:
        raise RuntimeError("project 41 missing material line, construction line or material plan")
    plan = plans[0]
    selection = api.post("designer", "material/selection/item/info", {
        "projectId": PROJECT_ID,
        "contractLineId": str(material_line["lineId"]),
    })
    return {
        "project": info,
        "plan": plan,
        "materialLine": material_line,
        "constructionLine": construction_line,
        "selection": selection,
    }


def ensure_supplier_relation(api: Api, ctx: dict[str, Any], partner_id: str) -> str:
    plan = ctx["plan"]
    catalog = plan.get("catalogItem") or {}
    if not catalog.get("itemId"):
        raise RuntimeError("material plan lacks catalog item")
    payload = {
        "catalogItemId": str(catalog["itemId"]),
        "materialName": catalog.get("name"),
        "brand": plan.get("selectedBrand"),
        "spec": plan.get("selectedSpec"),
        "unit": enum_key(plan.get("unit")) or None,
    }
    matched = page_items(api.post("finance", "supply/material-supplier/match", payload))
    if matched:
        return str(matched[0]["materialSupplierId"])
    saved = api.post("finance", "supply/material-supplier/save", {
        **payload,
        "partnerId": partner_id,
        "purchasePrice": round(float(plan.get("unitPrice") or 0) or 1, 2),
        "minimumOrder": 1,
        "leadTimeDays": 7,
        "settlementDays": 30,
        "remark": f"{BATCH} 材料供应关系",
    })
    return str(saved["materialSupplierId"])


def ensure_selection(api: Api, ctx: dict[str, Any]) -> dict[str, Any]:
    current = ctx.get("selection") or {}
    if enum_key(current.get("status")) == "Confirmed":
        return current
    line = ctx["materialLine"]
    content = "品牌：马可波罗；规格：800×800；颜色：奶油灰；工艺：哑光；备注：沉浸审计本地测试选材，已由测试客户身份模拟确认。"
    selection_json = json.dumps({
        "brand": "马可波罗", "spec": "800×800", "color": "奶油灰", "craft": "哑光",
        "remark": "沉浸审计本地测试选材"
    }, ensure_ascii=False)
    saved = api.post("designer", "material/selection/save", {
        "propertyId": str(ctx["project"]["property"]["propertyId"]),
        "contractId": str(ctx["materialLine"]["contract"]["contractId"]),
        "projectId": PROJECT_ID,
        "contractLineId": str(line["lineId"]),
        "title": "沉浸审计主材选材确认",
        "content": content,
        "selectionJson": selection_json,
        "status": "PendingConfirm",
        "remark": f"{BATCH} local customer-confirmation simulation",
    })
    confirmed = api.post("designer", "material/selection/confirm", {
        "selectionId": str(saved["selectionId"]),
        "confirmerName": ctx["project"]["customer"]["name"],
        "confirmedAt": now_iso(),
    })
    confirmed["confirmationSimulated"] = True
    return confirmed


def purchase_of_plan(api: Api) -> dict[str, Any] | None:
    rows = page_items(api.post("finance", "project/purchase/page", {
        "projectId": PROJECT_ID, "current": 1, "size": 50
    }))
    for row in rows:
        plan = row.get("materialPlan") or {}
        if str(plan.get("planId") or "") == str(plan_id_holder["id"]):
            return row
    return None


plan_id_holder: dict[str, str] = {}


def ensure_purchase(api: Api, ctx: dict[str, Any], partner_id: str) -> dict[str, Any]:
    plan = ctx["plan"]
    plan_id_holder["id"] = str(plan["planId"])
    purchase = purchase_of_plan(api)
    if not purchase:
        purchase = api.post("finance", "project/purchase/save", {
            "materialPlanId": str(plan["planId"]),
            "supplierId": partner_id,
            "quantity": float(plan.get("quantity") or 0),
            "unitPrice": float(plan.get("unitPrice") or 0) or 1,
            "deliveryMode": "Direct",
            "expectedAt": (date.today() + timedelta(days=7)).isoformat(),
            "remark": f"{BATCH} 财务确认材料下单",
        })
    status = enum_key(purchase.get("status"))
    if status == "Submitted":
        purchase = api.post("finance", "project/purchase/approve", {
            "purchaseId": str(purchase["purchaseId"]),
            "approved": True,
            "conclusion": f"{BATCH} 财务直接确认",
        })
        status = enum_key(purchase.get("status"))
    if status == "Approved":
        purchase = api.post("finance", "project/purchase/order", {"purchaseId": str(purchase["purchaseId"])})
        status = enum_key(purchase.get("status"))
    if status not in {"Received", "PartiallyReceived"}:
        receipts = page_items(api.post("finance", "project/goods-receipt/page", {
            "purchaseId": str(purchase["purchaseId"]), "current": 1, "size": 50
        }))
        received = sum(float(row.get("quantity") or 0) for row in receipts)
        remaining = max(0.0, float(plan.get("quantity") or purchase.get("quantity") or 0) - received)
        if remaining > 0:
            api.post("finance", "project/goods-receipt/save", {
                "purchaseId": str(purchase["purchaseId"]),
                "quantity": remaining,
                "arrivedAt": now_iso(),
                "images": [f"/demo/{BATCH}/receipt.jpg"],
                "remark": f"{BATCH} 材料全部到场",
            })
            purchase = purchase_of_plan(api) or purchase
    return purchase


def ensure_payment(api: Api) -> dict[str, Any] | None:
    payables = page_items(api.post("finance", "finance/payable/page", {
        "projectId": PROJECT_ID, "current": 1, "size": 100
    }))
    payable = next((row for row in payables if enum_key(row.get("type")) in {"Material", "材料"}), None)
    if not payable:
        return None
    outstanding = float(payable.get("amount") or 0) - float(payable.get("paidAmount") or 0)
    if outstanding <= 0:
        return {"payableId": str(payable["payableId"]), "status": enum_key(payable.get("status")), "alreadyPaid": True}
    payment = api.post("finance", "finance/payment/save", {
        "payableId": str(payable["payableId"]),
        "amount": outstanding,
        "account": "CompanyBank",
        "paidAt": date.today().isoformat(),
        "payeeName": payable.get("payeeName") or PARTNER_NAME,
        "transactionNo": f"{BATCH}-PAY-001",
        "remark": f"{BATCH} 本地测试付款，不对应真实资金",
    })
    return {"payment": payment, "payableId": str(payable["payableId"])}


def main() -> int:
    api = Api()
    api.login()
    partner_id = find_partner(api)
    ctx = get_state(api)
    supplier_relation_id = ensure_supplier_relation(api, ctx, partner_id)
    selection = ensure_selection(api, ctx)
    refreshed = get_state(api)
    purchase = ensure_purchase(api, refreshed, partner_id)
    payment = ensure_payment(api)
    result = {
        "batch": BATCH,
        "projectId": PROJECT_ID,
        "partnerId": partner_id,
        "supplierRelationId": supplier_relation_id,
        "selectionId": str(selection.get("selectionId")),
        "selectionStatus": enum_key(selection.get("status")),
        "selectionConfirmationSimulated": bool(selection.get("confirmationSimulated")),
        "purchaseId": str(purchase.get("purchaseId")),
        "purchaseStatus": enum_key(purchase.get("status")),
        "payment": payment,
        "completedAt": now_iso(),
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
