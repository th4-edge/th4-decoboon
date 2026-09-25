#!/usr/bin/env python3
"""通过真实 Provider 链路构造麦鸿设计多角色、多阶段演示数据。

设计原则：
1. 每个阶段从客户建档开始逐级推进，不直接写业务终态表。
2. 设计、报价、合同、项目使用真实接口和真实麦鸿报价项目库。
3. 脚本可重复执行；已完成案例与中间对象 ID 记录在 manifest 中。
4. 项目只推进到业主确认前，员工不得代业主确认验收。
"""

from __future__ import annotations

import argparse
import json
import sys
import time
from dataclasses import dataclass, field
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Any

import requests

COM_ID = "1790000000000000001"
STORE_ID = "1790000000000000002"
EMPLOYEES = {
    "boss": {"empId": "1790000000000000101", "phone": "19900000001", "name": "钱小明"},
    "finance": {"empId": "1790000000000000102", "phone": "19900000002", "name": "吴濛濛"},
    "storeManager": {"empId": "1790000000000000103", "phone": "19900000003", "name": "王洪文"},
    "marketer": {"empId": "1790000000000000104", "phone": "19900000004", "name": "黄贤杰"},
    "designerDong": {"empId": "1790000000000000105", "phone": "19900000005", "name": "董夏鹏", "role": "designer"},
    "designerXie": {"empId": "1790000000000000106", "phone": "19900000006", "name": "谢玲玲", "role": "designer"},
    "pmXu": {"empId": "1790000000000000107", "phone": "19900000007", "name": "徐勇"},
    "pmWang": {"empId": "1790000000000000108", "phone": "19900000008", "name": "王昌富"},
}

PM_BY_DESIGNER = {
    "designerXie": "pmXu",
    "designerDong": "pmWang",
}

STAGES = [
    ("new", 2),
    ("following", 2),
    ("pending_measure", 2),
    ("measured", 2),
    ("designing", 4),
    ("scheme_ready", 4),
    ("quote_draft", 4),
    ("quote_rejected", 4),
    ("quote_submitted", 4),
    ("quote_accepted", 4),
    ("contract_signed", 4),
    ("project_started", 4),
    ("acceptance_pending", 4),
]
STAGE_ORDER = {name: index for index, (name, _) in enumerate(STAGES)}

NAMES = [
    "王芳", "李娜", "张敏", "刘洋", "陈晨", "杨帆", "赵磊", "黄静", "周涛", "吴桐", "徐蕾",
    "孙悦", "胡斌", "朱琳", "高翔", "林雪", "何伟", "郭倩", "马超", "罗丹", "梁爽", "宋佳",
    "谢琳", "唐杰", "许倩", "韩磊", "冯雪", "曹阳", "邓超", "彭静", "曾勇", "肖琳", "田亮",
    "潘婷", "袁媛", "蒋涛", "蔡颖", "余凡", "杜鹏", "叶青", "程曦", "苏杭", "吕萌", "沈悦",
]
COMMUNITIES = ["滨湖世纪城", "香樟花园", "望江府", "锦绣园", "翡翠湖畔", "万象公馆", "高新云谷", "蜀山名邸"]
LAYOUTS = ["三室两厅一厨两卫", "三室两厅一厨一卫", "四室两厅两卫", "两室两厅一厨一卫"]


def now_iso() -> str:
    return datetime.now().replace(microsecond=0).isoformat()


def json_list(value: Any) -> list[dict[str, Any]]:
    if isinstance(value, list):
        return value
    if isinstance(value, dict):
        for key in ("content", "list", "records"):
            if isinstance(value.get(key), list):
                return value[key]
        if "data" in value and isinstance(value["data"], list):
            return value["data"]
    return []


@dataclass
class BatchState:
    path: Path
    data: dict[str, Any]

    @classmethod
    def load(cls, path: Path) -> "BatchState":
        path.parent.mkdir(parents=True, exist_ok=True)
        if path.exists():
            return cls(path, json.loads(path.read_text(encoding="utf-8")))
        return cls(path, {"batch": "MH-FULL-ROLE-20260924", "createdAt": now_iso(), "cases": {}})

    def case(self, key: str) -> dict[str, Any]:
        return self.data["cases"].setdefault(key, {"key": key, "steps": {}})

    def save(self) -> None:
        temp = self.path.with_suffix(".tmp")
        temp.write_text(json.dumps(self.data, ensure_ascii=False, indent=2), encoding="utf-8")
        temp.replace(self.path)


@dataclass
class SeedContext:
    api_base: str
    password: str
    state: BatchState
    dry_run: bool = False
    limit: int | None = None
    only_stage: str | None = None
    tokens: dict[str, str] = field(default_factory=dict)
    catalogs: dict[str, list[dict[str, Any]]] = field(default_factory=dict)

    def post(self, role: str, uri: str, payload: dict[str, Any] | None = None) -> Any:
        if self.dry_run:
            return {"dryRun": True, "uri": uri, "payload": payload or {}}
        response = requests.post(
            f"{self.api_base.rstrip('/')}/{uri}",
            headers={"Content-Type": "application/json", "token": self.tokens[role]},
            json=payload or {},
            timeout=30,
        )
        response.raise_for_status()
        body = response.json()
        if not body.get("success"):
            raise RuntimeError(f"{role} {uri} failed: {json.dumps(body, ensure_ascii=False)}")
        return body.get("data")

    def login_all(self) -> None:
        if self.dry_run:
            self.tokens = {role: "dry-run" for role in EMPLOYEES}
            return
        for role, employee in EMPLOYEES.items():
            response = requests.post(
                f"{self.api_base.rstrip('/')}/auth/session/password-login",
                json={"phone": employee["phone"], "password": self.password},
                timeout=20,
            )
            response.raise_for_status()
            body = response.json()
            if not body.get("success"):
                raise RuntimeError(f"login failed for {employee['name']}: {body}")
            self.tokens[role] = body["data"]["token"]

    def load_catalogs(self) -> None:
        if self.dry_run:
            return
        for item_type in ("Construction", "Material", "Custom", "General"):
            page = self.post("boss", "catalog/item/page", {"type": item_type, "current": 1, "size": 200})
            items = json_list(page)
            usable = [item for item in items if float(item.get("materialPrice") or 0) + float(item.get("manualPrice") or 0) > 0]
            self.catalogs[item_type] = usable
            print(f"[catalog] {item_type}: {len(usable)} usable")


def pick(items: list[dict[str, Any]], offset: int) -> dict[str, Any]:
    if not items:
        raise RuntimeError("empty catalog type")
    return items[offset % len(items)]


def ensure_step(ctx: SeedContext, case: dict[str, Any], step: str, fn):
    steps = case.setdefault("steps", {})
    if step in steps:
        return steps[step]
    value = fn()
    steps[step] = value
    ctx.state.save()
    return value


def create_customer(ctx: SeedContext, spec: dict[str, Any], case: dict[str, Any]) -> str:
    idx = spec["index"]
    payload = {
        "name": f"{NAMES[idx - 1]}（演示{idx:02d}）",
        "telNo": f"1391000{idx:04d}"[-11:],
        "community": COMMUNITIES[idx % len(COMMUNITIES)],
        "addr": f"{COMMUNITIES[idx % len(COMMUNITIES)]}{idx % 12 + 1}栋{idx % 18 + 1}0{idx % 6 + 1}",
        "area": 88 + (idx % 6) * 12,
        "budget": 18 + (idx % 8) * 3,
        "gender": "F" if idx % 3 == 0 else "M",
        "source": ["Lead", "Referral", "WalkIn", "Video"][idx % 4],
    }
    result = ctx.post("marketer", "customer/customer/save", payload)
    return str(result["custId"])


def create_property(ctx: SeedContext, spec: dict[str, Any], case: dict[str, Any], suffix: str = "主房") -> str:
    customer_id = case["steps"]["customerId"]
    idx = spec["index"]
    community = COMMUNITIES[(idx + (1 if suffix != "主房" else 0)) % len(COMMUNITIES)]
    payload = {
        "customerId": customer_id,
        "address": f"{community}{idx % 10 + 1}栋{idx % 20 + 1}0{idx % 6 + 1}（{suffix}）",
        "community": community,
        "building": f"{idx % 10 + 1}栋",
        "roomNo": f"{idx % 20 + 1}0{idx % 6 + 1}",
        "area": 90 + (idx % 7) * 10,
        "layout": LAYOUTS[idx % len(LAYOUTS)],
        "remark": f"{case['key']} 业务推演房屋",
    }
    result = ctx.post("marketer", "property/property/save", payload)
    return str(result["propertyId"])


def create_follow(ctx: SeedContext, spec: dict[str, Any], case: dict[str, Any]) -> str:
    result = ctx.post("marketer", "customer/customer-follow/save", {
        "customerId": case["steps"]["customerId"],
        "way": ["Phone", "Wechat", "Visit"][spec["index"] % 3],
        "content": "已沟通装修预算、入住时间和初步风格偏好，客户同意继续推进量房。",
        "nextFollowAt": (date.today() + timedelta(days=2 + spec["index"] % 5)).isoformat(),
    })
    return str(result["followId"])


def schedule_measure(ctx: SeedContext, spec: dict[str, Any], case: dict[str, Any]) -> str:
    measure_at = datetime.now() + timedelta(days=2 + spec["index"] % 8, hours=spec["index"] % 5)
    ctx.post("marketer", "customer/customer/measure", {
        "customerId": case["steps"]["customerId"],
        "propertyId": case["steps"]["propertyId"],
        "targetPropertyId": case["steps"]["propertyId"],
        "measureAt": measure_at.replace(microsecond=0).isoformat(),
    })
    return measure_at.replace(microsecond=0).isoformat()


def handoff(ctx: SeedContext, case: dict[str, Any], property_id: str) -> str:
    designer = EMPLOYEES[case["designerRole"]]
    ctx.post("storeManager", "customer/customer/handoff", {
        "customerId": case["steps"]["customerId"],
        "propertyId": property_id,
        "designerId": designer["empId"],
    })
    return designer["empId"]


def complete_measure(ctx: SeedContext, spec: dict[str, Any], case: dict[str, Any], property_id: str) -> str:
    design_role = case["designerRole"]
    result = ctx.post(design_role, "property/measure/save", {
        "propertyId": property_id,
        "measureTime": (datetime.now() - timedelta(days=18 - spec["index"] % 6)).replace(microsecond=0).isoformat(),
        "status": "Completed",
        "sitePhotos": [f"/demo/{case['key']}/measure/site.jpg"],
        "structureImages": [f"/demo/{case['key']}/measure/structure.jpg"],
        "attachments": [f"/demo/{case['key']}/measure/checklist.pdf"],
        "originalStructure": "原户型承重结构已确认，现场墙体与图纸一致。",
        "dimensions": "客餐厅 6.2×4.3m；主卧 4.0×3.5m；厨房 3.8×2.6m。",
        "customerRequirements": "环保优先，增加收纳，客餐厅通透，总预算控制在客户填报范围内。",
        "designerConclusion": "现场条件满足设计推进，下一步完成平面方案和报价。",
    })
    return str(result["measureId"])


def create_requirement(ctx: SeedContext, spec: dict[str, Any], case: dict[str, Any], property_id: str) -> str:
    result = ctx.post(case["designerRole"], "design/requirement/save", {
        "customerId": case["steps"]["customerId"],
        "propertyId": property_id,
        "targetPropertyId": property_id,
        "type": "LivingRoom",
        "spaceName": "客餐厅",
        "kind": "Demand",
        "category": "Function",
        "priority": "High",
        "title": "客餐厅通透与收纳需求",
        "detail": "希望客餐厅采光通透，增加餐边收纳，并保留儿童活动空间。",
        "response": "设计上采用开放客餐厅与整墙收纳组合。",
        "source": "量房沟通",
    })
    return str(result["requirementId"])


def create_design(ctx: SeedContext, spec: dict[str, Any], case: dict[str, Any], property_id: str) -> str:
    result = ctx.post(case["designerRole"], "design/scheme/save", {
        "customerId": case["steps"]["customerId"],
        "propertyId": property_id,
        "targetPropertyId": property_id,
        "name": f"麦鸿演示方案 {spec['index']:02d}",
        "style": ["现代简约", "奶油风", "原木风", "新中式"][spec["index"] % 4],
        "layout": LAYOUTS[spec["index"] % len(LAYOUTS)],
        "description": "依据客户诉求、量房尺寸和麦鸿报价项目库生成的设计方案。",
        "area": 90 + (spec["index"] % 7) * 10,
    })
    return str(result["designId"])


def create_spaces(ctx: SeedContext, case: dict[str, Any], design_id: str) -> list[dict[str, Any]]:
    payloads = [
        ("LivingRoom", "客餐厅", 6.2, 4.3),
        ("Kitchen", "厨房", 3.8, 2.6),
        ("Bedroom", "主卧", 4.0, 3.5),
    ]
    result = []
    for sort, (space_type, name, length, width) in enumerate(payloads):
        saved = ctx.post(case["designerRole"], "design/space/save", {
            "designId": design_id,
            "type": space_type,
            "name": name,
            "length": length,
            "width": width,
            "height": 2.8,
            "sort": sort,
            "remark": "按量房数据录入",
        })
        result.append({"spaceId": str(saved["spaceId"]), "name": name})
    return result


def create_design_items(ctx: SeedContext, spec: dict[str, Any], case: dict[str, Any], design_id: str,
                        spaces: list[dict[str, Any]]) -> list[dict[str, Any]]:
    selections = [
        ("Construction", 42),
        ("Construction", 8),
        ("Construction", 16),
        ("Material", 30),
        ("Custom", 12),
        ("General", 1),
    ]
    result = []
    for sort, (item_type, quantity) in enumerate(selections):
        source = pick(ctx.catalogs[item_type], spec["index"] + sort)
        space = spaces[sort % len(spaces)]
        saved = ctx.post(case["designerRole"], "design/item/save", {
            "designId": design_id,
            "spaceId": space["spaceId"],
            "catalogItemId": str(source["itemId"]),
            "quantity": quantity,
            "sort": sort,
            "remark": "来自麦鸿报价9.5报价项目库",
        })
        result.append({
            "itemId": str(saved["itemId"]),
            "catalogItemId": str(source["itemId"]),
            "code": source.get("code"),
            "name": source.get("name"),
            "amount": saved.get("amount"),
        })
    return result


def build_design(ctx: SeedContext, case: dict[str, Any], design_id: str, status: str) -> str:
    result = ctx.post(case["designerRole"], "design/scheme/build", {"designId": design_id, "status": status})
    return str(result["designId"])


def create_quote(ctx: SeedContext, case: dict[str, Any], design_id: str, discount: float = 0.95) -> dict[str, Any]:
    return ctx.post(case["designerRole"], "quote/save", {"designId": design_id, "discount": discount})


def update_quote(ctx: SeedContext, case: dict[str, Any], quote_id: str, status: str) -> dict[str, Any]:
    return ctx.post(case["designerRole"] if status != "Accepted" else "boss", "quote/update", {
        "quoteId": quote_id,
        "status": status,
    })


def create_contract(ctx: SeedContext, spec: dict[str, Any], case: dict[str, Any], quote: dict[str, Any]) -> str:
    amount = float(quote.get("amount") or 0)
    start = date.today() + timedelta(days=5 + spec["index"] % 10)
    end = start + timedelta(days=95)
    result = ctx.post("boss", "project/contract/save", {
        "quoteId": str(quote["quoteId"]),
        "signedAt": (date.today() - timedelta(days=2)).isoformat(),
        "signedAmount": amount,
        "startDate": start.isoformat(),
        "planEndDate": end.isoformat(),
        "paymentTerms": "签约30%，水电验收30%，泥木验收30%，竣工10%",
        "remark": f"{case['key']} 演示合同",
    })
    return str(result["contractId"])


def activate_project(ctx: SeedContext, case: dict[str, Any], contract_id: str, spec: dict[str, Any]) -> str:
    result = ctx.post("boss", "project/contract/update", {"contractId": contract_id, "status": "Effective"})
    project = result.get("project") or {}
    project_id = project.get("projectId") or project.get("id") or result.get("projectId")
    if not project_id:
        raise RuntimeError(f"effective contract did not return project: {result}")
    start = date.today() - timedelta(days=8)
    end = start + timedelta(days=95)
    pm_role = PM_BY_DESIGNER[case["designerRole"]]
    ctx.post("boss", "project/project/update", {
        "projectId": str(project_id),
        "name": f"麦鸿演示项目 {spec['index']:02d}",
        "siteAddr": f"演示项目工地 {spec['index']:02d}",
        "designerId": EMPLOYEES[case["designerRole"]]["empId"],
        "managerId": EMPLOYEES[pm_role]["empId"],
        "supportId": EMPLOYEES["finance"]["empId"],
        "startDate": start.isoformat(),
        "planEndDate": end.isoformat(),
    })
    case.setdefault("steps", {})["pmRole"] = pm_role
    case["steps"]["managerId"] = EMPLOYEES[pm_role]["empId"]
    return str(project_id)


def get_stages(ctx: SeedContext, project_id: str) -> list[dict[str, Any]]:
    data = ctx.post("boss", "project/stage/page", {"projectId": project_id, "current": 1, "size": 100})
    return json_list(data)


def get_tasks(ctx: SeedContext, project_id: str, stage_id: str | None = None) -> list[dict[str, Any]]:
    payload = {"projectId": project_id, "current": 1, "size": 200}
    if stage_id:
        payload["stageId"] = stage_id
    data = ctx.post("boss", "project/task/page", payload)
    return json_list(data)


def stage_id_of(stage: dict[str, Any]) -> str:
    return str(stage.get("stageId") or stage.get("id"))


def stage_status_of(stage: dict[str, Any]) -> str:
    status = stage.get("status")
    if isinstance(status, dict):
        return str(status.get("key") or status.get("value") or status.get("name") or "")
    return str(status or "")


def task_status_of(task: dict[str, Any]) -> str:
    status = task.get("status")
    if isinstance(status, dict):
        return str(status.get("key") or status.get("value") or status.get("name") or "")
    return str(status or "")


def start_project(ctx: SeedContext, project_id: str, pm_role: str) -> dict[str, Any]:
    stages = get_stages(ctx, project_id)
    if not stages:
        raise RuntimeError(f"project {project_id} has no stages")
    first = sorted(stages, key=lambda x: int(x.get("sort") or 0))[0]
    first_id = stage_id_of(first)
    if stage_status_of(first) == "Todo":
        ctx.post(pm_role, "project/stage/update", {
            "stageId": first_id,
            "status": "Doing",
            "content": "项目正式开工，完成现场交底。",
        })
    tasks = get_tasks(ctx, project_id, first_id)
    return {"stageId": first_id, "stageName": first.get("name"), "taskCount": len(tasks)}


def progress_first_task(ctx: SeedContext, project_id: str, stage_id: str, pm_role: str) -> dict[str, Any]:
    tasks = get_tasks(ctx, project_id, stage_id)
    if not tasks:
        return {}
    first = tasks[0]
    task_id = str(first["taskId"])
    ctx.post(pm_role, "project/task/update", {"taskId": task_id, "action": "start"})
    ctx.post(pm_role, "project/task/update", {
        "taskId": task_id,
        "action": "done",
        "evidenceUrls": [f"/demo/{project_id}/task/{task_id}.jpg"],
    })
    return {"taskId": task_id, "name": first.get("name")}


def submit_first_acceptance(ctx: SeedContext, project_id: str, pm_role: str) -> dict[str, Any]:
    stages = get_stages(ctx, project_id)
    if not stages:
        return {}
    first = sorted(stages, key=lambda x: int(x.get("sort") or 0))[0]
    stage_id = stage_id_of(first)
    tasks = get_tasks(ctx, project_id, stage_id)
    for task in tasks:
        if task_status_of(task) == "Done":
            continue
        task_id = str(task["taskId"])
        if task_status_of(task) != "Doing":
            ctx.post(pm_role, "project/task/update", {"taskId": task_id, "action": "start"})
        ctx.post(pm_role, "project/task/update", {
            "taskId": task_id,
            "action": "done",
            "evidenceUrls": [f"/demo/{project_id}/task/{task_id}.jpg"],
        })
    acceptance = ctx.post(pm_role, "project/acceptance/save", {
        "stageId": stage_id,
        "result": "pass",
        "content": f"{first.get('name') or '首阶段'}施工完成，提交业主确认。",
        "images": [f"/demo/{project_id}/acceptance/{stage_id}.jpg"],
    })
    return {"stageId": stage_id, "acceptanceId": str(acceptance.get("acceptanceId") or acceptance.get("id"))}


def create_receipt(ctx: SeedContext, spec: dict[str, Any], case: dict[str, Any], project_id: str) -> str:
    page = ctx.post("finance", "finance/receivable/page", {"projectId": project_id, "current": 1, "size": 20})
    receivables = json_list(page)
    if not receivables:
        return ""
    target = receivables[0]
    receivable_id = str(target.get("receivableId") or target.get("id"))
    amount = float(target.get("amount") or 0)
    received = float(target.get("receivedAmount") or 0)
    due = max(0.0, amount - received)
    payment = round(due if due < amount * 0.4 else amount * 0.3, 2)
    result = ctx.post("finance", "finance/receipt/save", {
        "projectId": project_id,
        "receivableId": receivable_id,
        "amount": payment,
        "account": "CompanyBank",
        "receivedAt": (date.today() - timedelta(days=1)).isoformat(),
        "payerName": case["customerName"],
        "transactionNo": f"DEMO-{spec['index']:02d}-001",
        "remark": "演示数据：客户签约款到账",
    })
    return str(result.get("receiptId") or result.get("id") or "")


def progress_case(ctx: SeedContext, spec: dict[str, Any]) -> None:
    case = ctx.state.case(spec["key"])
    case.update({"stage": spec["stage"], "designerRole": spec["designerRole"], "customerName": f"{NAMES[spec['index'] - 1]}（演示{spec['index']:02d}）"})
    stage_index = STAGE_ORDER[spec["stage"]]
    print(f"[case] {spec['key']} -> {spec['stage']} ({spec['designerRole']})")

    ensure_step(ctx, case, "customerId", lambda: create_customer(ctx, spec, case))
    if stage_index >= STAGE_ORDER["following"]:
        ensure_step(ctx, case, "followId", lambda: create_follow(ctx, spec, case))
    if stage_index >= STAGE_ORDER["pending_measure"]:
        ensure_step(ctx, case, "propertyId", lambda: create_property(ctx, spec, case))
        ensure_step(ctx, case, "measureScheduledAt", lambda: schedule_measure(ctx, spec, case))
    if stage_index >= STAGE_ORDER["measured"]:
        ensure_step(ctx, case, "designerId", lambda: handoff(ctx, case, case["steps"]["propertyId"]))
        ensure_step(ctx, case, "measureId", lambda: complete_measure(ctx, spec, case, case["steps"]["propertyId"]))
    if spec.get("multiProperty") and stage_index >= STAGE_ORDER["measured"]:
        def second_property():
            second = create_property(ctx, spec, case, "次房")
            complete_measure(ctx, spec, case, second)
            return second
        ensure_step(ctx, case, "secondPropertyId", second_property)
    if stage_index >= STAGE_ORDER["designing"]:
        ensure_step(ctx, case, "requirementId", lambda: create_requirement(ctx, spec, case, case["steps"]["propertyId"]))
        ensure_step(ctx, case, "designId", lambda: create_design(ctx, spec, case, case["steps"]["propertyId"]))
        ensure_step(ctx, case, "spaces", lambda: create_spaces(ctx, case, case["steps"]["designId"]))
        ensure_step(ctx, case, "designItems", lambda: create_design_items(ctx, spec, case, case["steps"]["designId"], case["steps"]["spaces"]))
    if stage_index >= STAGE_ORDER["scheme_ready"]:
        ensure_step(ctx, case, "designReady", lambda: build_design(ctx, case, case["steps"]["designId"], "Ready"))

    if stage_index >= STAGE_ORDER["quote_draft"]:
        ensure_step(ctx, case, "quoteDraft", lambda: create_quote(ctx, case, case["steps"]["designId"]))
    if stage_index == STAGE_ORDER["quote_rejected"]:
        ensure_step(ctx, case, "quoteSubmittedForReject", lambda: str(update_quote(ctx, case, str(case["steps"]["quoteDraft"]["quoteId"]), "Submitted")["quoteId"]))
        ensure_step(ctx, case, "quoteRejected", lambda: str(update_quote(ctx, case, str(case["steps"]["quoteDraft"]["quoteId"]), "Rejected")["quoteId"]))
    elif STAGE_ORDER["quote_submitted"] <= stage_index < STAGE_ORDER["quote_accepted"]:
        ensure_step(ctx, case, "quoteSubmitted", lambda: create_quote_submitted(ctx, case))
    if stage_index >= STAGE_ORDER["quote_accepted"]:
        ensure_step(ctx, case, "quoteHistory", lambda: create_accepted_quote_with_history(ctx, case))
    accepted_quote = case.get("steps", {}).get("quoteHistory", {}).get("accepted") if isinstance(case.get("steps", {}).get("quoteHistory"), dict) else None

    talk_quote = None
    if stage_index == STAGE_ORDER["quote_draft"]:
        talk_quote = case.get("steps", {}).get("quoteDraft")
    elif stage_index == STAGE_ORDER["quote_submitted"]:
        talk_quote = case.get("steps", {}).get("quoteSubmitted")
    elif stage_index >= STAGE_ORDER["quote_accepted"]:
        talk_quote = accepted_quote
    if stage_index >= STAGE_ORDER["quote_accepted"]:
        history = case.get("steps", {}).get("quoteHistory") or {}
        if history.get("talkId"):
            ensure_step(ctx, case, "talkId", lambda: str(history["talkId"]))
    elif talk_quote and talk_quote.get("quoteId"):
        ensure_step(ctx, case, "talkId", lambda: create_talk(
                ctx, case, case["steps"]["designId"], str(talk_quote["quoteId"])))

    if stage_index >= STAGE_ORDER["contract_signed"]:
        ensure_step(ctx, case, "contractId", lambda: create_contract(ctx, spec, case, accepted_quote))
    if stage_index >= STAGE_ORDER["project_started"]:
        ensure_step(ctx, case, "projectId", lambda: activate_project(ctx, case, case["steps"]["contractId"], spec))
        pm_role = case.get("steps", {}).get("pmRole", PM_BY_DESIGNER[case["designerRole"]])
        ensure_step(ctx, case, "projectStarted", lambda: start_project(ctx, case["steps"]["projectId"], pm_role))
        ensure_step(ctx, case, "firstTaskProgress", lambda: progress_first_task(ctx, case["steps"]["projectId"], case["steps"]["projectStarted"]["stageId"], pm_role))
        ensure_step(ctx, case, "dailyReport", lambda: create_daily_report(ctx, case, case["steps"]["projectId"], case["steps"]["projectStarted"]["stageId"], pm_role))
    if stage_index >= STAGE_ORDER["acceptance_pending"]:
        pm_role = case.get("steps", {}).get("pmRole", PM_BY_DESIGNER[case["designerRole"]])
        ensure_step(ctx, case, "acceptancePending", lambda: submit_first_acceptance(ctx, case["steps"]["projectId"], pm_role))
        ensure_step(ctx, case, "receiptId", lambda: create_receipt(ctx, spec, case, case["steps"]["projectId"]))

    case["completed"] = True
    case["completedAt"] = now_iso()
    ctx.state.save()
    print(f"[done] {spec['key']}")


def create_talk(ctx: SeedContext, case: dict[str, Any], design_id: str, quote_id: str) -> str:
    result = ctx.post(case["designerRole"], "design/talk/build", {
        "designId": design_id,
        "quoteId": quote_id,
    })
    return str(result.get("talkId") or result.get("id"))


def create_daily_report(ctx: SeedContext, case: dict[str, Any], project_id: str, stage_id: str, pm_role: str) -> str:
    result = ctx.post(pm_role, "project/report/save", {
        "projectId": project_id,
        "stageId": stage_id,
        "type": "Progress",
        "title": "今日现场进度",
        "content": f"{case['customerName']}项目完成现场交底和首项施工任务，现场材料与工艺符合计划，无异常。",
        "images": [f"/demo/{project_id}/daily-report.jpg"],
        "visibleToCustomer": True,
    })
    return str(result.get("reportId") or result.get("id") or "")


def create_quote_submitted(ctx: SeedContext, case: dict[str, Any]) -> dict[str, Any]:
    draft = create_quote(ctx, case, case["steps"]["designId"])
    update_quote(ctx, case, str(draft["quoteId"]), "Submitted")
    return draft


def create_accepted_quote_with_history(ctx: SeedContext, case: dict[str, Any]) -> dict[str, Any]:
    first = create_quote(ctx, case, case["steps"]["designId"])
    update_quote(ctx, case, str(first["quoteId"]), "Submitted")
    second = create_quote(ctx, case, case["steps"]["designId"], discount=0.94)
    update_quote(ctx, case, str(second["quoteId"]), "Submitted")
    # 谈单 PPT 必须在客户接受报价前生成，接受后方案锁定，不能反向补做。
    talk_id = create_talk(ctx, case, case["steps"]["designId"], str(second["quoteId"]))
    accepted = update_quote(ctx, case, str(second["quoteId"]), "Accepted")
    return {"first": first, "accepted": accepted, "talkId": talk_id}


def build_specs() -> list[dict[str, Any]]:
    specs = []
    index = 0
    for stage, count in STAGES:
        for offset in range(count):
            index += 1
            designer_role = "designerXie" if offset % 2 == 0 else "designerDong"
            if STAGE_ORDER[stage] < STAGE_ORDER["designing"]:
                designer_role = "designerXie"
            specs.append({
                "key": f"MH-FULL-ROLE-20260924-{index:03d}",
                "index": index,
                "stage": stage,
                "designerRole": designer_role,
                "multiProperty": offset == 0 and STAGE_ORDER[stage] >= STAGE_ORDER["quote_accepted"],
            })
    return specs


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--api", default="http://127.0.0.1:8090/api")
    parser.add_argument("--password", default="Maihong2026")
    parser.add_argument("--manifest", default="data/seed/maihong-business-simulation-20260921.json")
    parser.add_argument("--limit", type=int)
    parser.add_argument("--only-stage")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    state = BatchState.load(Path(args.manifest))
    ctx = SeedContext(args.api, args.password, state, dry_run=args.dry_run, limit=args.limit,
                      only_stage=args.only_stage)
    ctx.login_all()
    ctx.load_catalogs()

    specs = build_specs()
    if args.only_stage:
        specs = [item for item in specs if item["stage"] == args.only_stage]
    if args.limit:
        specs = specs[:args.limit]

    print(f"[plan] cases={len(specs)} stages={len(STAGES)}")
    if args.dry_run:
        for spec in specs:
            print(json.dumps(spec, ensure_ascii=False))
        return 0
    failures = []
    for spec in specs:
        try:
            progress_case(ctx, spec)
        except Exception as exc:
            failures.append({"case": spec["key"], "stage": spec["stage"], "error": str(exc)})
            print(f"[failed] {spec['key']}: {exc}", file=sys.stderr)
            state.data.setdefault("failures", []).append({"at": now_iso(), **failures[-1]})
            state.save()
            break
    if failures:
        return 1

    print(json.dumps({
        "batch": state.data["batch"],
        "cases": len(state.data["cases"]),
        "manifest": str(Path(args.manifest).resolve()),
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
