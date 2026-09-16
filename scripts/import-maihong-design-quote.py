#!/usr/bin/env python3
"""把《麦鸿装饰报价9.5.xlsx》导入装得清设计转化闭环。

默认通过真实 Provider API 创建：
诉求 -> 两套设计方案 -> 区域尺寸 -> 施工/主材分项 -> 设计报价 -> 谈单 PPT。

示例：
  python3 scripts/import-maihong-design-quote.py \
    --api http://127.0.0.1:8090/api \
    --token "$TOKEN" \
    --customer-id 123

如果不传 customer-id，脚本会从当前账号可见客户中优先选择已经有设计师、面积大于 80㎡ 的客户。
使用 --dry-run 只生成本地计划 JSON，不访问后端。
"""

from __future__ import annotations

import argparse
import json
import math
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable
import requests

try:
    from openpyxl import Workbook, load_workbook
    from openpyxl.styles import Alignment, Font, PatternFill
    from openpyxl.utils import get_column_letter
except ImportError as exc:
    raise SystemExit("缺少 openpyxl，请先执行：python3 -m pip install openpyxl") from exc


@dataclass
class QuoteMaster:
    row: int
    name: str
    category: str
    unit: str
    material_price: float
    manual_price: float
    cost_price: float
    pricing_mode: str

    def payload(self, *, item_type: str, space_id: str | None, quantity: float,
                variant: str = "base", sort: int = 0) -> dict[str, Any]:
        multiplier = 1.0
        if variant == "upgrade" and item_type == "Material":
            multiplier = 1.08
        payload: dict[str, Any] = {
            "spaceId": space_id,
            "type": item_type,
            "category": self.category,
            "name": self.name,
            "unit": UNIT_API.get(self.unit, self.unit),
            "quantity": round(quantity, 3),
            "materialPrice": round(self.material_price * multiplier, 2),
            "manualPrice": round(self.manual_price, 2),
            "costPrice": round(self.cost_price, 2),
            "pricingMode": self.pricing_mode,
            "sort": sort,
        }
        return payload


QUESTION_ROOMS = [
    {"name": "客餐厅", "type": "LivingRoom", "length": 6.2, "width": 4.2, "height": 2.85, "kind": "dry"},
    {"name": "主卧", "type": "Bedroom", "length": 4.1, "width": 3.5, "height": 2.85, "kind": "dry"},
    {"name": "次卧", "type": "Bedroom", "length": 3.6, "width": 3.0, "height": 2.85, "kind": "dry"},
    {"name": "北卧", "type": "Bedroom", "length": 3.3, "width": 2.8, "height": 2.85, "kind": "dry"},
    {"name": "厨房", "type": "Kitchen", "length": 3.2, "width": 1.9, "height": 2.85, "kind": "wet"},
    {"name": "卫生间", "type": "Bathroom", "length": 2.7, "width": 1.7, "height": 2.85, "kind": "wet"},
    {"name": "阳台", "type": "Balcony", "length": 3.8, "width": 1.5, "height": 2.85, "kind": "wet"},
]

ROOM_SCALE_BASE_AREA = sum(float(room["length"]) * float(room["width"]) for room in QUESTION_ROOMS)

UNIT_API = {
    "㎡": "Area", "平米": "Area", "m²": "Area", "个": "Count", "项": "Item", "处": "Place",
    "套": "Set", "户": "House", "m": "Metre", "米": "Metre", "cm": "Centimeter",
    "步": "Step", "樘": "Plate", "块": "Block", "根": "Root", "扇": "Hole",
    "台": "Pcs", "支": "Stick", "格": "Grid", "片": "Sheet",
}


class ApiClient:
    def __init__(self, base: str, token: str):
        self.base = base.rstrip("/")
        self.token = token

    def post(self, uri: str, payload: dict[str, Any] | None = None) -> Any:
        try:
            response = requests.post(
                f"{self.base}/{uri.lstrip('/')}",
                json=payload or {},
                headers={"token": self.token},
                timeout=30,
            )
        except requests.RequestException as exc:
            raise RuntimeError(f"{uri} 网络错误: {exc}") from exc
        try:
            envelope = response.json()
        except ValueError as exc:
            raise RuntimeError(f"{uri} HTTP {response.status_code}: {response.text[:500]}") from exc
        if response.status_code >= 400 or not envelope.get("success"):
            code = (envelope.get("errorCode") or {}).get("code", str(response.status_code))
            message = (envelope.get("errorCode") or {}).get("message", response.text[:200])
            raise RuntimeError(f"{uri} 失败 [{code}] {message}")
        return envelope.get("data")


def enum_item_type(value: Any) -> str:
    if isinstance(value, dict):
        return str(value.get("value") or value.get("key") or "")
    return str(value or "")


def number(value: Any, default: float = 0.0) -> float:
    if value is None or value == "":
        return default
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def clean(value: Any) -> str:
    return str(value or "").replace("\n", " ").strip()


def pricing_mode(row: int, material: float, manual: float, formula: str) -> str:
    if formula and re.fullmatch(r"=[A-Z]+\d+", formula.strip()):
        return "FixedTotal"
    if material > 0 and manual > 0:
        return "MaterialAndManual"
    if manual > 0:
        return "ManualOnly"
    return "MaterialOnly"


def read_master(xlsx: Path, sheet_name: str, item_type: str) -> list[QuoteMaster]:
    workbook = load_workbook(xlsx, data_only=False, read_only=True)
    if sheet_name not in workbook.sheetnames:
        raise RuntimeError(f"Excel 中不存在工作表：{sheet_name}")
    sheet = workbook[sheet_name]
    result: list[QuoteMaster] = []
    for row in range(5, sheet.max_row + 1):
        name = clean(sheet.cell(row, 2).value)
        category = clean(sheet.cell(row, 9).value)
        unit = clean(sheet.cell(row, 3).value).replace("元/㎡", "㎡").replace("平米", "㎡")
        if not name or not category or name in {"工程项目名称", "项目类型"}:
            continue
        material = number(sheet.cell(row, 5).value)
        manual = number(sheet.cell(row, 6).value)
        cost = number(sheet.cell(row, 8).value) or material + manual
        formula = clean(sheet.cell(row, 7).value)
        result.append(QuoteMaster(
            row=row,
            name=name,
            category=category,
            unit=unit or "项",
            material_price=material,
            manual_price=manual,
            cost_price=cost,
            pricing_mode=pricing_mode(row, material, manual, formula),
        ))
    return result


def find_item(items: Iterable[QuoteMaster], category: str, keyword: str, fallback: str | None = None) -> QuoteMaster:
    exact = [item for item in items if item.category == category and keyword in item.name]
    if exact:
        return exact[0]
    if fallback:
        loose = [item for item in items if keyword in item.name or fallback in item.name]
        if loose:
            return loose[0]
    raise RuntimeError(f"报价表中找不到施工/材料项：{category}/{keyword}")


def estimate_item(item: dict[str, Any]) -> float:
    quantity = number(item.get("quantity"))
    material = number(item.get("materialPrice"))
    manual = number(item.get("manualPrice"))
    mode = item.get("pricingMode")
    if mode == "FixedTotal":
        return material + manual
    if mode == "MaterialOnly":
        return quantity * material
    if mode == "ManualOnly":
        return quantity * manual
    return quantity * (material + manual)


def page_records(data: Any) -> list[dict[str, Any]]:
    if isinstance(data, list):
        return data
    if isinstance(data, dict):
        return data.get("content") or data.get("records") or []
    return []


def select_customer(client: ApiClient, customer_id: str | None) -> dict[str, Any]:
    if customer_id:
        customer = client.post("customer/customer/info", {"customerId": customer_id})
        if not customer:
            raise RuntimeError(f"客户不存在：{customer_id}")
        return customer
    customers = page_records(client.post("customer/customer/page", {"current": 1, "size": 200}))
    if not customers:
        raise RuntimeError("当前账号没有可导入的客户")
    ranked = sorted(
        customers,
        key=lambda item: (
            bool(item.get("designer")),
            number(item.get("area")) >= 80,
            number(item.get("area")),
        ),
        reverse=True,
    )
    return ranked[0]


def catalog_index(client: ApiClient) -> dict[tuple[str, str, str], str]:
    result: dict[tuple[str, str, str], str] = {}
    for catalog_type in ["Construction", "Material", "General", "Custom"]:
        tree = client.post("catalog/item/tree", {"type": catalog_type}) or {}
        for category in tree.get("categories", []):
            current = 1
            while True:
                page = client.post("catalog/item/page", {
                    "type": catalog_type, "category": category, "current": current, "size": 100
                }) or {}
                content = page_records(page)
                for item in content:
                    result[(catalog_type, category, item.get("name", ""))] = str(item.get("itemId"))
                total = int(page.get("totalElements") or 0)
                if current * 100 >= total or not content:
                    break
                current += 1
    return result


def scale_rooms(area: float) -> list[dict[str, Any]]:
    ratio = math.sqrt(area / ROOM_SCALE_BASE_AREA) if area > 0 else 1.0
    result = []
    for room in QUESTION_ROOMS:
        copied = dict(room)
        copied["length"] = round(float(room["length"]) * ratio, 2)
        copied["width"] = round(float(room["width"]) * ratio, 2)
        result.append(copied)
    return result


def create_requirements(client: ApiClient, customer: dict[str, Any], customer_id: str) -> list[dict[str, Any]]:
    remark = clean(customer.get("remark"))
    budget = number(customer.get("budget"))
    requirements = [
        {
            "customerId": customer_id, "type": "LivingRoom", "spaceName": "客餐厅",
            "kind": "Demand", "category": "Function", "priority": "High",
            "title": "客餐厅动线开阔，兼顾收纳", "detail": remark or "客户希望公共空间通透，日常活动不拥挤。",
            "source": "真实报价数据导入",
        },
        {
            "customerId": customer_id, "type": "Kitchen", "spaceName": "厨房",
            "kind": "Demand", "category": "Function", "priority": "High",
            "title": "厨房操作动线顺手", "detail": "洗切炒顺序明确，台面和插座预留符合电器使用。",
            "source": "真实报价数据导入",
        },
        {
            "customerId": customer_id, "type": "Bathroom", "spaceName": "卫生间",
            "kind": "Demand", "category": "Quality", "priority": "High",
            "title": "防水和排水必须可靠", "detail": "重视隐蔽工程，避免后期返潮、渗水和维修。",
            "source": "真实报价数据导入",
        },
        {
            "customerId": customer_id, "type": "Bedroom", "spaceName": "主卧",
            "kind": "Demand", "category": "Function", "priority": "Medium",
            "title": "主卧收纳充足且保持安静", "detail": "衣柜、床头和静音需求优先。",
            "source": "真实报价数据导入",
        },
        {
            "customerId": customer_id, "kind": "Reject", "category": "Material", "priority": "High",
            "title": "不接受低价材料导致后期返工", "detail": "预算可以分阶段投入，但隐蔽工程和常用材料不能只比价格。",
            "source": "真实报价数据导入",
        },
        {
            "customerId": customer_id, "kind": "Demand", "category": "Budget", "priority": "Medium",
            "title": "投入清晰可解释", "detail": f"客户预算约{budget:g}万元，希望每一笔钱对应具体区域和项目。" if budget else "希望每一笔钱对应具体区域和项目。",
            "source": "真实报价数据导入",
        },
    ]
    existing = {
        item.get("title")
        for item in page_records(client.post("design/requirement/page", {
            "customerId": customer_id, "current": 1, "size": 200
        }))
    }
    for item in requirements:
        if item["title"] not in existing:
            client.post("design/requirement/save", item)
    return requirements


def item_specs_for_room(room: dict[str, Any], construction: list[QuoteMaster], material: list[QuoteMaster],
                        general: list[QuoteMaster], variant: str, start_sort: int) -> list[dict[str, Any]]:
    length = float(room["length"])
    width = float(room["width"])
    height = float(room["height"])
    area = length * width
    wall = (length + width) * 2 * height
    wet = room["kind"] == "wet"
    bedroom = room["type"] == "Bedroom"
    result: list[dict[str, Any]] = []
    sort = start_sort

    def add(item: QuoteMaster, item_type: str, quantity: float) -> None:
        nonlocal sort
        result.append({"master": item, "itemType": item_type, "quantity": max(quantity, 0.1), "sort": sort})
        sort += 10

    add(find_item(construction, "拆除", "墙顶面白灰"), "Construction", wall * 0.75)
    add(find_item(construction, "水电", "水电改造（水管全换，电路增改）"), "Construction", area)
    if wet:
        add(find_item(construction, "瓦工", "东方雨虹滚涂防水"), "Construction", area * 1.25)
        add(find_item(construction, "瓦工", "地面水泥沙浆找平（3cm内）"), "Construction", area)
        add(find_item(construction, "瓦工", "地砖铺贴"), "Construction", area)
        add(find_item(construction, "瓦工", "墙砖铺贴"), "Construction", wall * 0.72)
        add(find_item(material, "瓷砖", "马可波罗400*800（展厅款）"), "Material", wall * 0.76)
        add(find_item(material, "瓷砖", "马可波罗800*800（展厅款）" if variant == "base" else "东鹏800*800（展厅款）"), "Material", area * 1.08)
        if room["type"] == "Bathroom":
            add(find_item(material, "卫浴", "OXO浴室柜"), "Material", 1)
            add(find_item(material, "卫浴", "花洒（展厅款）"), "Material", 1)
            add(find_item(material, "卫浴", "马桶（展厅款）"), "Material", 1)
            add(find_item(material, "集成吊顶", "麦鸿定制集成吊顶（300*600）"), "Material", area)
        elif room["type"] == "Kitchen":
            add(find_item(material, "集成吊顶", "麦鸿定制集成吊顶（300*600）"), "Material", area)
    else:
        add(find_item(construction, "瓦工", "地面水泥沙浆找平（3cm内）"), "Construction", area)
        add(find_item(construction, "木工", "轻钢龙骨石膏板吊平顶"), "Construction", area * 0.7)
        if bedroom:
            add(find_item(material, "地板", "大自然实木多层地板（1.2）" if variant == "base" else "圣象实木多层地板（1.2）"), "Material", area * 1.06)
            add(find_item(material, "木门", "实木复合烤漆门/无漆门"), "Material", 1)
        else:
            add(find_item(construction, "瓦工", "地砖铺贴"), "Construction", area)
            add(find_item(material, "瓷砖", "马可波罗800*800（展厅款）" if variant == "base" else "东鹏800*800（展厅款）"), "Material", area * 1.08)
    add(find_item(construction, "油漆", "倍得丽墙面腻子基础处理"), "Construction", wall)
    add(find_item(construction, "油漆", "乳胶漆（三棵树典萃多效）"), "Construction", wall)

    if room["type"] == "LivingRoom":
        building_area = sum(float(r["length"]) * float(r["width"]) for r in QUESTION_ROOMS)
        for keyword in ["现场成品保护及地固", "材料运输及搬运费", "垃圾清运费", "完工保洁", "方案、图纸设计费", "工程管理费"]:
            add(find_item(general, "综合项目报价清单", keyword, keyword), "General", building_area)
        add(find_item(material, "开关面板", "金斯卡 开关面板"), "Material", 1)
    return result


def create_scheme(client: ApiClient, customer_id: str, customer: dict[str, Any], title: str,
                  style: str, rooms: list[dict[str, Any]], construction: list[QuoteMaster],
                  material: list[QuoteMaster], general: list[QuoteMaster], variant: str,
                  catalogs: dict[tuple[str, str, str], str]) -> dict[str, Any]:
    layout = f"{len(rooms)}个功能区，整体面积{sum(r['length'] * r['width'] for r in rooms):.1f}㎡"
    description = "根据量房尺寸和客户诉求生成的真实报价样例；方案按区域配置施工与主材分项。"
    existing = next(
        (item for item in page_records(client.post("design/scheme/page", {
            "customerId": customer_id, "current": 1, "size": 200
        })) if item.get("name") == title),
        None,
    )
    if existing:
        scheme = client.post("design/scheme/update", {
            "designId": existing.get("designId") or existing.get("id"),
            "name": title,
            "style": style,
            "layout": layout,
            "description": description,
            "coverUrl": customer.get("coverUrl"),
        })
    else:
        scheme = client.post("design/scheme/save", {
            "customerId": customer_id,
            "name": title,
            "style": style,
            "layout": layout,
            "description": description,
            "coverUrl": customer.get("coverUrl"),
        })
    design_id = str(scheme.get("designId") or scheme.get("id"))
    detail = client.post("design/scheme/info", {"designId": design_id}) or {}
    existing_spaces = {space.get("name"): space for space in detail.get("spaces", [])}
    spaces: list[dict[str, Any]] = []
    for index, room in enumerate(rooms):
        old_space = existing_spaces.get(room["name"])
        space = client.post("design/space/save", {
            "designId": design_id,
            "spaceId": old_space.get("spaceId") if old_space else None,
            "type": room["type"],
            "name": room["name"],
            "length": room["length"],
            "width": room["width"],
            "height": room["height"],
            "sort": index,
            "remark": "真实量房尺寸样例",
        })
        spaces.append(space)

    existing_items = {
        (str(item.get("space", {}).get("spaceId")) if item.get("space") else None,
         item.get("name"), enum_item_type(item.get("type")))
        : item for item in detail.get("items", [])
    }
    item_count = 0
    for room, space in zip(rooms, spaces):
        space_id = str(space.get("spaceId") or space.get("id"))
        specs = item_specs_for_room(room, construction, material, general, variant, item_count * 10)
        for spec in specs:
            payload = spec["master"].payload(
                item_type=spec["itemType"],
                space_id=None if spec["itemType"] == "General" else space_id,
                quantity=spec["quantity"],
                variant=variant,
                sort=spec["sort"],
            )
            catalog_id = catalogs.get((spec["itemType"], spec["master"].category, spec["master"].name))
            if not catalog_id:
                raise RuntimeError(f"统一材料库缺少项目：{spec['itemType']}/{spec['master'].category}/{spec['master'].name}")
            payload["catalogItemId"] = catalog_id
            payload["designId"] = design_id
            key = (None if spec["itemType"] == "General" else space_id, payload["name"], spec["itemType"])
            old_item = existing_items.get(key)
            payload["itemId"] = old_item.get("itemId") if old_item else None
            client.post("design/item/save", payload)
            item_count += 1
    built = client.post("design/scheme/build", {"designId": design_id, "status": "Ready"})
    return {"scheme": built, "spaces": spaces, "itemCount": item_count}


def import_data(args: argparse.Namespace) -> dict[str, Any]:
    xlsx = Path(args.xlsx).expanduser()
    if not xlsx.exists():
        raise RuntimeError(f"找不到报价表：{xlsx}")
    construction = read_master(xlsx, "施工报价清单", "Construction")
    material = read_master(xlsx, "主材报价清单", "Material")
    general = read_master(xlsx, "综合项目报价清单", "General")

    if args.dry_run:
        customer = {
            "custId": args.customer_id or "CURRENT_CUSTOMER_ID",
            "name": args.customer_name,
            "area": args.area,
            "budget": args.budget,
            "remark": args.requirement,
        }
        customer_id = str(customer["custId"])
        rooms = scale_rooms(args.area)
        requirements = [
            {"customerId": customer_id, "kind": "Demand", "category": "Function", "priority": "High", "spaceName": "客餐厅", "title": "空间动线开阔，兼顾收纳"},
            {"customerId": customer_id, "kind": "Demand", "category": "Quality", "priority": "High", "spaceName": "卫生间", "title": "防水和排水必须可靠"},
            {"customerId": customer_id, "kind": "Reject", "category": "Material", "priority": "High", "title": "不接受低价材料导致后期返工"},
        ]
        schemes = []
        for variant, title, style in [
            ("base", f"{args.customer_name}现代简约方案", "现代简约"),
            ("upgrade", f"{args.customer_name}品质升级方案", "现代轻奢"),
        ]:
            item_count = 0
            preview_items = []
            for room in rooms:
                specs = item_specs_for_room(room, construction, material, general, variant, item_count * 10)
                item_count += len(specs)
                preview_items.extend({
                    "space": room["name"], "type": spec["itemType"], "category": spec["master"].category,
                    "name": spec["master"].name, "unit": spec["master"].unit,
                    "quantity": round(spec["quantity"], 3), "materialPrice": spec["master"].material_price,
                    "manualPrice": spec["master"].manual_price, "costPrice": spec["master"].cost_price,
                    "pricingMode": spec["master"].pricing_mode,
                } for spec in specs)
            subtotal = sum(estimate_item(item) for item in preview_items)
            discounted = subtotal * args.discount
            tax = discounted * 0.05
            schemes.append({
                "name": title,
                "style": style,
                "status": "Ready",
                "rooms": rooms,
                "itemCount": item_count,
                "estimatedSubtotal": round(subtotal, 2),
                "estimatedDiscount": args.discount,
                "estimatedTax": round(tax, 2),
                "estimatedAmount": round(discounted + tax, 2),
                "items": preview_items,
            })
        return {
            "source": str(xlsx),
            "dryRun": True,
            "customer": customer,
            "requirements": requirements,
            "schemes": schemes,
        }

    client = ApiClient(args.api, args.token)
    customer = select_customer(client, args.customer_id)
    customer_id = str(customer.get("custId") or customer.get("id"))
    customer_area = number(customer.get("area"), args.area)
    rooms = scale_rooms(customer_area)
    requirements = create_requirements(client, customer, customer_id)
    catalogs = catalog_index(client)
    schemes = [
        create_scheme(client, customer_id, customer, f"{customer.get('name', '客户')}现代简约方案", "现代简约",
                      rooms, construction, material, general, "base", catalogs),
        create_scheme(client, customer_id, customer, f"{customer.get('name', '客户')}品质升级方案", "现代轻奢",
                      rooms, construction, material, general, "upgrade", catalogs),
    ]
    quote = client.post("quote/save", {
        "customerId": customer_id,
        "designId": schemes[0]["scheme"].get("designId"),
        "discount": args.discount,
    })
    talk = client.post("design/talk/build", {
        "designId": schemes[0]["scheme"].get("designId"),
        "quoteId": quote.get("quoteId") or quote.get("id"),
    })
    return {
        "source": str(xlsx),
        "dryRun": False,
        "customer": customer,
        "requirements": requirements,
        "schemes": schemes,
        "quote": quote,
        "talk": talk,
    }


def write_preview_workbook(plan: dict[str, Any], output: Path) -> None:
    workbook = Workbook()
    overview = workbook.active
    overview.title = "方案总览"
    overview.append(["客户", "方案", "风格", "区域数", "分项数", "分项合计", "折扣", "税金", "报价合计"])
    for scheme in plan["schemes"]:
        overview.append([
            plan["customer"].get("name", "客户"),
            scheme["name"],
            scheme["style"],
            len(scheme["rooms"]),
            scheme["itemCount"],
            scheme.get("estimatedSubtotal", 0),
            scheme.get("estimatedDiscount", 1),
            scheme.get("estimatedTax", 0),
            scheme.get("estimatedAmount", 0),
        ])

    for scheme in plan["schemes"]:
        sheet = workbook.create_sheet(scheme["name"][:28])
        sheet.append(["区域", "类型", "分类", "项目", "单位", "数量", "材料单价", "人工单价", "计价方式", "预计金额", "成本单价"])
        for item in scheme["items"]:
            sheet.append([
                item["space"],
                item["type"],
                item["category"],
                item["name"],
                item["unit"],
                item["quantity"],
                item["materialPrice"],
                item["manualPrice"],
                item["pricingMode"],
                round(estimate_item(item), 2),
                item["costPrice"],
            ])

    manager = workbook.create_sheet("项目经理施工汇总")
    manager.append(["施工分类", "施工分项", "单位", "总工程量", "预计金额"])
    grouped: dict[tuple[str, str, str], dict[str, float]] = {}
    for item in plan["schemes"][0]["items"]:
        if item["type"] != "Construction":
            continue
        key = (item["category"], item["name"], item["unit"])
        bucket = grouped.setdefault(key, {"quantity": 0.0, "amount": 0.0})
        bucket["quantity"] += number(item["quantity"])
        bucket["amount"] += estimate_item(item)
    for (category, name, unit), value in sorted(grouped.items()):
        manager.append([category, name, unit, round(value["quantity"], 3), round(value["amount"], 2)])

    header_fill = PatternFill("solid", fgColor="16A394")
    header_font = Font(color="FFFFFF", bold=True)
    for sheet in workbook.worksheets:
        sheet.freeze_panes = "A2"
        sheet.auto_filter.ref = sheet.dimensions
        for cell in sheet[1]:
            cell.fill = header_fill
            cell.font = header_font
            cell.alignment = Alignment(horizontal="center")
        for row in sheet.iter_rows(min_row=2):
            for cell in row:
                if isinstance(cell.value, float):
                    cell.number_format = '#,##0.00'
        for index, column in enumerate(sheet.columns, 1):
            width = min(max(len(str(cell.value or "")) for cell in column) + 3, 42)
            sheet.column_dimensions[get_column_letter(index)].width = width
    output.parent.mkdir(parents=True, exist_ok=True)
    workbook.save(output)


def main() -> int:
    parser = argparse.ArgumentParser(description="导入麦鸿真实报价到设计转化闭环")
    parser.add_argument("--xlsx", default="/Users/zhangwei/Desktop/麦鸿装饰报价9.5.xlsx")
    parser.add_argument("--api", default="http://127.0.0.1:8090/api")
    parser.add_argument("--token", default="")
    parser.add_argument("--customer-id", default="")
    parser.add_argument("--customer-name", default="周先生")
    parser.add_argument("--area", type=float, default=105)
    parser.add_argument("--budget", type=float, default=14.5)
    parser.add_argument("--requirement", default="105平三房，已量房，关注水电、防水和收纳。")
    parser.add_argument("--discount", type=float, default=0.98)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--output", default="data/design/maihong-real-quote-plan.json")
    parser.add_argument("--excel-output", default="data/design/maihong-real-quote-preview.xlsx")
    args = parser.parse_args()
    if not args.dry_run and not args.token:
        parser.error("真实导入必须提供 --token；如需先生成计划请使用 --dry-run")

    result = import_data(args)
    output = Path(args.output)
    if not output.is_absolute():
        output = Path.cwd() / output
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    excel_output = Path(args.excel_output)
    if not excel_output.is_absolute():
        excel_output = Path.cwd() / excel_output
    write_preview_workbook(result, excel_output)
    print(json.dumps({
        "success": True,
        "dryRun": result["dryRun"],
        "output": str(output),
        "excelOutput": str(excel_output),
        "customer": result["customer"].get("name"),
        "schemeCount": len(result["schemes"]),
        "itemCount": sum(item["itemCount"] for item in result["schemes"]),
        "quoteId": (result.get("quote") or {}).get("quoteId"),
        "talkId": (result.get("talk") or {}).get("talkId"),
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"导入失败：{exc}", file=sys.stderr)
        raise SystemExit(1)
