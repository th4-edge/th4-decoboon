#!/usr/bin/env python3
"""装得清 App 全链路测试数据构造（2026-09-23）。

在既有“麦鸿演示项目 37~44”基础上，通过真实 Provider 顺序推进：
选材 -> 客户确认 -> 材料计划可下单 -> 材料下单 -> 材料到场/应付；
以及 增项/减项/数量变更 -> 审批 -> 客户确认/待客户确认/待审批。

脚本只使用 5 个已设置本地密码的员工账号：
- 设计师：谢玲玲、董夏鹏
- 财务：吴濛濛
不修改 BOSS 微信凭证，也不直接写业务终态表。
"""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass, field
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Any

import requests

BATCH = "APP-BIZ-SIM-20260923"
DEFAULT_API = "http://127.0.0.1:8090/api"
DEFAULT_PASSWORD = "Maihong2026"
SUPPLIER_NAME = "麦鸿演示材料商"
PROJECT_NUMBERS = range(37, 45)
RECEIVE_PROJECTS = {37, 38, 39}
ORDER_PROJECTS = {37, 38, 39, 40, 41, 42}
CONFIRM_REMOVE_PROJECTS = {37, 39, 41, 43}  # 谢玲玲项目；董夏鹏项目保留“审批通过待客户确认”
QUANTITY_CHANGE_PROJECTS = {43, 44}
PENDING_CHANGE_PROJECT = 38

ACCOUNTS = {
    "storeManager": {"phone": "19900000003", "name": "王洪文", "role": "storeManager"},
    "marketer": {"phone": "19900000004", "name": "黄贤杰", "role": "marketer"},
    "designerDong": {"phone": "19900000005", "name": "董夏鹏", "role": "designer"},
    "designerXie": {"phone": "19900000006", "name": "谢玲玲", "role": "designer"},
    "finance": {"phone": "19900000002", "name": "吴濛濛", "role": "finance"},
}


def now_iso() -> str:
    return datetime.now().replace(microsecond=0).isoformat()


def page_items(data: Any) -> list[dict[str, Any]]:
    if isinstance(data, list):
        return data
    if isinstance(data, dict):
        for key in ("content", "list", "records", "items"):
            if isinstance(data.get(key), list):
                return data[key]
    return []


def enum_key(value: Any) -> str:
    if isinstance(value, dict):
        return str(value.get("key") or value.get("value") or value.get("name") or "")
    return str(value or "")


def project_no(name: str) -> int:
    match = re.search(r"麦鸿演示项目\s*(\d+)", name or "")
    return int(match.group(1)) if match else -1


@dataclass
class BatchState:
    path: Path
    data: dict[str, Any]

    @classmethod
    def load(cls, path: Path) -> "BatchState":
        path.parent.mkdir(parents=True, exist_ok=True)
        if path.exists():
            return cls(path, json.loads(path.read_text(encoding="utf-8")))
        return cls(path, {"batch": BATCH, "createdAt": now_iso(), "projects": {}, "failures": []})

    def save(self) -> None:
        tmp = self.path.with_suffix(".tmp")
        tmp.write_text(json.dumps(self.data, ensure_ascii=False, indent=2), encoding="utf-8")
        tmp.replace(self.path)


@dataclass
class Api:
    base: str
    password: str
    state: BatchState
    tokens: dict[str, str] = field(default_factory=dict)

    def login_all(self) -> None:
        for role, account in ACCOUNTS.items():
            response = requests.post(
                f"{self.base.rstrip('/')}/auth/session/password-login",
                json={"phone": account["phone"], "password": self.password},
                timeout=20,
            )
            body = response.json()
            if not body.get("success"):
                raise RuntimeError(f"{role} 登录失败: {json.dumps(body, ensure_ascii=False)}")
            self.tokens[role] = body["data"]["token"]
            print(f"[login] {role} {account['name']} OK")

    def post(self, role: str, uri: str, payload: dict[str, Any] | None = None) -> Any:
        response = requests.post(
            f"{self.base.rstrip('/')}/{uri}",
            headers={"Content-Type": "application/json", "token": self.tokens[role]},
            json=payload or {},
            timeout=40,
        )
        try:
            body = response.json()
        except Exception as exc:  # noqa: BLE001
            raise RuntimeError(f"{role} {uri} 非 JSON 响应 HTTP {response.status_code}: {response.text[:500]}") from exc
        if not body.get("success"):
            raise RuntimeError(
                f"{role} {uri} 失败 HTTP {response.status_code}: {json.dumps(body, ensure_ascii=False)}"
            )
        return body.get("data")

    def get_state(self, project: dict[str, Any]) -> dict[str, Any]:
        designer_role = "designerXie" if project["designer"]["name"] == ACCOUNTS["designerXie"]["name"] else "designerDong"
        project_id = str(project["projectId"])
        info = self.post(designer_role, "project/project/info", {"projectId": project_id})
        plans = page_items(self.post(designer_role, "project/material-plan/page", {"projectId": project_id, "current": 1, "size": 100}))
        lines = page_items(self.post(designer_role, "project/contract-line/page", {"projectId": project_id, "current": 1, "size": 200}))
        material_line = next((row for row in lines if enum_key(row.get("type")) == "Material"), None)
        construction_line = next((row for row in lines if enum_key(row.get("type")) == "Construction"), None)
        if not material_line or not construction_line:
            raise RuntimeError(f"项目 {project_id} 缺少主材/施工合同行")
        if not plans:
            raise RuntimeError(f"项目 {project_id} 缺少材料计划")
        plan = plans[0]
        selection = self.post(designer_role, "material/selection/item/info", {
            "projectId": project_id,
            "contractLineId": str(material_line["lineId"]),
        })
        return {
            "projectId": project_id,
            "projectNo": project_no(info.get("name") or project.get("name")),
            "projectName": info.get("name"),
            "designerRole": designer_role,
            "designerName": info.get("designer", {}).get("name"),
            "customerName": info.get("customer", {}).get("name"),
            "propertyId": str(info["property"]["propertyId"]),
            "contractId": str(material_line["contract"]["contractId"]),
            "materialLineId": str(material_line["lineId"]),
            "materialLine": material_line,
            "constructionLineId": str(construction_line["lineId"]),
            "constructionLine": construction_line,
            "planId": str(plan["planId"]),
            "plan": plan,
            "existingSelection": selection,
        }

    def find_change(self, role: str, project_id: str, reason: str) -> dict[str, Any] | None:
        rows = page_items(self.post(role, "project/contract-change/page", {"projectId": project_id, "current": 1, "size": 200}))
        return next((row for row in rows if row.get("reason") == reason), None)


def selection_payload(ctx: dict[str, Any], idx: int) -> tuple[str, str, str]:
    category = ctx["materialLine"].get("category") or "主材"
    name = ctx["materialLine"].get("name") or "合同主材"
    color = ["奶油灰", "暖白", "浅胡桃", "拉丝香槟金", "雾霾蓝"][idx % 5]
    brand = ["马可波罗", "三棵树", "九牧", "奥普", "志邦"][idx % 5]
    spec = ["800×800", "600×600", "2.0m 定制", "300×600", "ENF 级"] [idx % 5]
    craft = ["哑光", "防滑", "现场复尺后定制", "同色收边", "环保净味"][idx % 5]
    content = f"颜色/花色：{color}；规格/型号：{spec}；品牌：{brand}；工艺：{craft}；备注：{name} 客户已现场确认。"
    selection_json = json.dumps(
        {"color": color, "spec": spec, "brand": brand, "craft": craft, "remark": f"{category} 客户现场确认"},
        ensure_ascii=False,
    )
    return content, selection_json, f"{category}选材确认"


def ensure_selection(api: Api, ctx: dict[str, Any], idx: int) -> str:
    current = ctx.get("existingSelection") or {}
    if enum_key(current.get("status")) == "Confirmed" or current.get("status") == "Confirmed":
        selection_id = str(current.get("selectionId"))
        ctx["selectionId"] = selection_id
        ctx["selectionStatus"] = "Confirmed"
        return selection_id
    content, selection_json, title = selection_payload(ctx, idx)
    saved = api.post(ctx["designerRole"], "material/selection/save", {
        "propertyId": ctx["propertyId"],
        "contractId": ctx["contractId"],
        "projectId": ctx["projectId"],
        "contractLineId": ctx["materialLineId"],
        "title": title,
        "content": content,
        "selectionJson": selection_json,
        "status": "PendingConfirm",
        "remark": f"{BATCH} 签约后主材选材",
    })
    selection_id = str(saved["selectionId"])
    confirmed = api.post(ctx["designerRole"], "material/selection/confirm", {
        "selectionId": selection_id,
        "confirmerName": ctx["customerName"],
        "confirmedAt": now_iso(),
    })
    ctx["selectionId"] = selection_id
    ctx["selectionStatus"] = enum_key(confirmed.get("status")) or "Confirmed"
    return selection_id


def ensure_supplier_relation(api: Api, ctx: dict[str, Any], partner_id: str) -> str | None:
    plan = ctx["plan"]
    catalog = plan.get("catalogItem") or {}
    catalog_id = catalog.get("itemId")
    if not catalog_id:
        return None
    unit = enum_key(plan.get("unit")) or None
    payload = {
        "catalogItemId": str(catalog_id),
        "materialName": catalog.get("name"),
        "brand": plan.get("selectedBrand"),
        "spec": plan.get("selectedSpec"),
        "unit": unit,
    }
    matched = page_items(api.post("finance", "supply/material-supplier/match", payload))
    if matched:
        ctx["materialSupplierId"] = str(matched[0].get("materialSupplierId"))
        return ctx["materialSupplierId"]
    saved = api.post("finance", "supply/material-supplier/save", {
        "partnerId": partner_id,
        "catalogItemId": str(catalog_id),
        "materialName": catalog.get("name"),
        "brand": plan.get("selectedBrand"),
        "spec": plan.get("selectedSpec"),
        "unit": unit,
        "purchasePrice": round(float(plan.get("unitPrice") or 0) * 0.72, 2),
        "minimumOrder": 1,
        "leadTimeDays": 7,
        "settlementDays": 30,
        "remark": f"{BATCH} 材料-供应商供应关系",
    })
    ctx["materialSupplierId"] = str(saved["materialSupplierId"])
    return ctx["materialSupplierId"]


def ensure_add_change(api: Api, ctx: dict[str, Any], amount: float, cost: float) -> dict[str, Any]:
    reason = f"[{BATCH}][ADD] 阳台增加定制储物柜（演示{ctx['projectNo']}）"
    existing = api.find_change(ctx["designerRole"], ctx["projectId"], reason)
    if existing:
        change = existing
    else:
        change = api.post(ctx["designerRole"], "project/contract-change/batch-save", {
            "contractId": ctx["contractId"],
            "reason": reason,
            "type": "Add",
            "evidenceImages": [f"/demo/{BATCH}/project-{ctx['projectNo']}/add.jpg"],
            "items": [{
                "name": "阳台定制储物柜",
                "itemType": "Custom",
                "changeType": "Add",
                "amountDelta": amount,
                "costDelta": cost,
                "remark": "客户现场新增收纳需求，已复尺确认尺寸。",
            }],
        })
    status = enum_key(change.get("status"))
    if status == "Pending":
        change = api.post("finance", "project/contract-change/update", {
            "changeId": str(change["changeId"]),
            "approved": True,
            "conclusion": "增项金额和成本核对通过",
        })
        status = enum_key(change.get("status"))
    if status == "Approved":
        change = api.post(ctx["designerRole"], "project/contract-change/customer-confirm", {
            "changeId": str(change["changeId"]),
            "signatureUrl": f"/demo/{BATCH}/project-{ctx['projectNo']}/add-sign.png",
            "evidenceImages": [f"/demo/{BATCH}/project-{ctx['projectNo']}/add.jpg"],
        })
    ctx["addChange"] = {"changeId": str(change["changeId"]), "status": enum_key(change.get("status"))}
    return change


def ensure_remove_change(api: Api, ctx: dict[str, Any], amount: float, cost: float) -> dict[str, Any]:
    reason = f"[{BATCH}][REMOVE] 取消部分木工造型（演示{ctx['projectNo']}）"
    existing = api.find_change(ctx["designerRole"], ctx["projectId"], reason)
    if existing:
        change = existing
    else:
        change = api.post(ctx["designerRole"], "project/contract-change/save", {
            "contractId": ctx["contractId"],
            "reason": reason,
            "type": "Remove",
            "contractLineId": ctx["constructionLineId"],
            "amount": amount,
            "costDelta": cost,
            "evidenceImages": [f"/demo/{BATCH}/project-{ctx['projectNo']}/remove.jpg"],
        })
    if enum_key(change.get("status")) == "Pending":
        change = api.post("finance", "project/contract-change/update", {
            "changeId": str(change["changeId"]),
            "approved": True,
            "conclusion": "减项审批通过",
        })
    if ctx["projectNo"] in CONFIRM_REMOVE_PROJECTS and enum_key(change.get("status")) == "Approved":
        change = api.post(ctx["designerRole"], "project/contract-change/customer-confirm", {
            "changeId": str(change["changeId"]),
            "signatureUrl": f"/demo/{BATCH}/project-{ctx['projectNo']}/remove-sign.png",
            "evidenceImages": [f"/demo/{BATCH}/project-{ctx['projectNo']}/remove.jpg"],
        })
    ctx["removeChange"] = {"changeId": str(change["changeId"]), "status": enum_key(change.get("status"))}
    return change


def ensure_quantity_change(api: Api, ctx: dict[str, Any]) -> dict[str, Any]:
    reason = f"[{BATCH}][QTY] 主材数量现场复尺调整（演示{ctx['projectNo']}）"
    existing = api.find_change(ctx["designerRole"], ctx["projectId"], reason)
    if existing:
        change = existing
    else:
        quantity = float(ctx["plan"].get("quantity") or 0)
        new_quantity = max(0.01, round(quantity * 0.9, 3))
        change = api.post(ctx["designerRole"], "project/contract-change/save", {
            "contractId": ctx["contractId"],
            "reason": reason,
            "type": "Quantity",
            "contractLineId": ctx["materialLineId"],
            "newQuantity": new_quantity,
            "evidenceImages": [f"/demo/{BATCH}/project-{ctx['projectNo']}/qty.jpg"],
        })
    if enum_key(change.get("status")) == "Pending":
        change = api.post("finance", "project/contract-change/update", {
            "changeId": str(change["changeId"]),
            "approved": True,
            "conclusion": "数量调整审批通过",
        })
    if enum_key(change.get("status")) == "Approved":
        change = api.post(ctx["designerRole"], "project/contract-change/customer-confirm", {
            "changeId": str(change["changeId"]),
            "signatureUrl": f"/demo/{BATCH}/project-{ctx['projectNo']}/qty-sign.png",
            "evidenceImages": [f"/demo/{BATCH}/project-{ctx['projectNo']}/qty.jpg"],
        })
    ctx["quantityChange"] = {"changeId": str(change["changeId"]), "status": enum_key(change.get("status"))}
    return change


def ensure_pending_change(api: Api, ctx: dict[str, Any]) -> dict[str, Any]:
    reason = f"[{BATCH}][PENDING] 新增软装灯带待审批（演示{ctx['projectNo']}）"
    existing = api.find_change(ctx["designerRole"], ctx["projectId"], reason)
    if existing:
        change = existing
    else:
        change = api.post(ctx["designerRole"], "project/contract-change/batch-save", {
            "contractId": ctx["contractId"],
            "reason": reason,
            "type": "Add",
            "evidenceImages": [f"/demo/{BATCH}/project-{ctx['projectNo']}/pending.jpg"],
            "items": [{
                "name": "客厅线性灯带",
                "itemType": "Custom",
                "changeType": "Add",
                "amountDelta": 1280,
                "costDelta": 880,
                "remark": "客户提出新增照明方案，等待财务审批。",
            }],
        })
    ctx["pendingChange"] = {"changeId": str(change["changeId"]), "status": enum_key(change.get("status"))}
    return change


def purchase_of_plan(api: Api, ctx: dict[str, Any]) -> dict[str, Any] | None:
    rows = page_items(api.post("finance", "project/purchase/page", {"projectId": ctx["projectId"], "current": 1, "size": 50}))
    for row in rows:
        plan = row.get("materialPlan") or {}
        if str(plan.get("planId") or "") == ctx["planId"]:
            return row
    return None


def ensure_purchase(api: Api, ctx: dict[str, Any], partner_id: str, receive: bool) -> dict[str, Any]:
    purchase = purchase_of_plan(api, ctx)
    plan = ctx["plan"]
    if not purchase:
        purchase = api.post("finance", "project/purchase/save", {
            "materialPlanId": ctx["planId"],
            "supplierId": partner_id,
            "quantity": float(plan.get("quantity") or 0),
            "unitPrice": float(plan.get("unitPrice") or 0),
            "deliveryMode": "Direct",
            "expectedAt": plan.get("requiredAt") or (date.today() + timedelta(days=7)).isoformat(),
            "remark": f"{BATCH} 财务确认材料下单",
        })
    status = enum_key(purchase.get("status"))
    if status == "Submitted":
        purchase = api.post("finance", "project/purchase/approve", {
            "purchaseId": str(purchase["purchaseId"]),
            "approved": True,
            "conclusion": "财务直接确认",
        })
        status = enum_key(purchase.get("status"))
    if status == "Approved":
        purchase = api.post("finance", "project/purchase/order", {"purchaseId": str(purchase["purchaseId"])})
        status = enum_key(purchase.get("status"))
    if receive and status not in {"Received", "PartiallyReceived"}:
        receipts = page_items(api.post("finance", "project/goods-receipt/page", {
            "purchaseId": str(purchase["purchaseId"]),
            "current": 1,
            "size": 20,
        }))
        received = sum(float(row.get("quantity") or 0) for row in receipts)
        total = float(plan.get("quantity") or purchase.get("quantity") or 0)
        remaining = max(0.0, total - received)
        if remaining > 0:
            api.post("finance", "project/goods-receipt/save", {
                "purchaseId": str(purchase["purchaseId"]),
                "quantity": remaining,
                "arrivedAt": now_iso(),
                "images": [f"/demo/{BATCH}/project-{ctx['projectNo']}/receipt.jpg"],
                "remark": f"{BATCH} 材料到场确认",
            })
            purchase = purchase_of_plan(api, ctx) or purchase
    ctx["purchaseId"] = str(purchase.get("purchaseId"))
    ctx["purchaseStatus"] = enum_key(purchase.get("status"))
    return purchase


def find_partner(api: Api) -> str:
    partners = page_items(api.post("finance", "partner/page", {"type": "Supplier", "current": 1, "size": 200}))
    existing = next((row for row in partners if row.get("name") == SUPPLIER_NAME), None)
    if existing:
        return str(existing["partnerId"])
    saved = api.post("finance", "partner/save", {
        "type": "Supplier",
        "name": SUPPLIER_NAME,
        "contactName": "演示供应商",
        "telNo": "13910009999",
        "settlementMode": "ReceiptDays",
        "settlementDays": 30,
        "brands": ["奥普", "九牧", "三棵树", "马可波罗", "志邦"],
        "remark": f"{BATCH} App 全链路演示供应商",
    })
    return str(saved["partnerId"])


def discover_projects(api: Api) -> list[dict[str, Any]]:
    data = api.post("finance", "project/project/page", {"current": 1, "size": 200})
    rows = page_items(data)
    projects = [row for row in rows if project_no(row.get("name") or "") in PROJECT_NUMBERS]
    projects.sort(key=lambda row: project_no(row.get("name") or ""))
    if len(projects) != len(PROJECT_NUMBERS):
        raise RuntimeError(f"预期 8 个演示项目，实际 {len(projects)}: {[row.get('name') for row in projects]}")
    return projects


def progress_project(api: Api, project: dict[str, Any], partner_id: str, idx: int) -> None:
    case = api.state.data["projects"].setdefault(str(project["projectId"]), {})
    case.update({"projectName": project.get("name"), "updatedAt": now_iso()})
    ctx = api.get_state(project)
    ctx.update(case)
    print(f"[case] {ctx['projectName']} designer={ctx['designerName']}")
    ensure_supplier_relation(api, ctx, partner_id)
    ensure_selection(api, ctx, idx)
    ensure_add_change(api, ctx, 2800 + idx * 120, 1900 + idx * 80)
    ensure_remove_change(api, ctx, -600 - idx * 20, -400 - idx * 10)
    if ctx["projectNo"] in QUANTITY_CHANGE_PROJECTS:
        ensure_quantity_change(api, ctx)
    if ctx["projectNo"] == PENDING_CHANGE_PROJECT:
        ensure_pending_change(api, ctx)
    if ctx["projectNo"] in ORDER_PROJECTS:
        ensure_purchase(api, ctx, partner_id, receive=ctx["projectNo"] in RECEIVE_PROJECTS)
    case.update({
        "projectId": ctx["projectId"],
        "projectNo": ctx["projectNo"],
        "designerRole": ctx["designerRole"],
        "designerName": ctx["designerName"],
        "customerName": ctx["customerName"],
        "propertyId": ctx["propertyId"],
        "contractId": ctx["contractId"],
        "materialLineId": ctx["materialLineId"],
        "constructionLineId": ctx["constructionLineId"],
        "materialPlanId": ctx["planId"],
        "materialSupplierId": ctx.get("materialSupplierId"),
        "selectionId": ctx.get("selectionId"),
        "selectionStatus": ctx.get("selectionStatus"),
        "purchaseId": ctx.get("purchaseId"),
        "purchaseStatus": ctx.get("purchaseStatus"),
        "addChange": ctx.get("addChange"),
        "removeChange": ctx.get("removeChange"),
        "quantityChange": ctx.get("quantityChange"),
        "pendingChange": ctx.get("pendingChange"),
        "updatedAt": now_iso(),
    })
    api.state.save()
    print(f"[done] {ctx['projectName']} selection={ctx.get('selectionStatus')} purchase={ctx.get('purchaseStatus')}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--api", default=DEFAULT_API)
    parser.add_argument("--password", default=DEFAULT_PASSWORD)
    parser.add_argument("--manifest", default=str(Path(__file__).with_name("seed-manifest.json")))
    args = parser.parse_args()

    state = BatchState.load(Path(args.manifest))
    api = Api(args.api, args.password, state)
    api.login_all()
    partner_id = find_partner(api)
    print(f"[partner] {SUPPLIER_NAME} {partner_id}")
    projects = discover_projects(api)
    failures = state.data.setdefault("failures", [])
    for idx, project in enumerate(projects, start=1):
        try:
            progress_project(api, project, partner_id, idx)
        except Exception as exc:  # noqa: BLE001
            failure = {"at": now_iso(), "project": project.get("name"), "error": str(exc)}
            failures.append(failure)
            api.state.save()
            print(f"[failed] {project.get('name')}: {exc}")
            return 1
    print(json.dumps({
        "batch": BATCH,
        "projectCount": len(state.data["projects"]),
        "manifest": str(Path(args.manifest).resolve()),
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
