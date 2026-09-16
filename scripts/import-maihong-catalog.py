#!/usr/bin/env python3
"""将《麦鸿装饰报价9.5.xlsx》中的项目清单导入统一家装材料库。

该脚本必须使用企业负责人、采购员或仓管账号的 token 运行：
  python3 scripts/import-maihong-catalog.py \
    --api http://127.0.0.1/api \
    --token "<材料库维护账号 token>"
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

import requests
from openpyxl import load_workbook

SHEETS = {
    "施工报价清单": ("Construction", "施工"),
    "主材报价清单": ("Material", "主材"),
    "综合项目报价清单": ("General", "综合"),
    "全屋定制、橱柜报价清单": ("Custom", "全屋定制"),
}
SOFT_CATEGORIES = {"窗帘", "墙布"}
BRANDS = [
    "马可波罗", "东鹏", "亚细亚", "大角鹿", "利家福", "高尔夫", "利德福",
    "TATA", "欧派", "九牧", "惠达", "OXO", "爱智贞", "创博", "圣象",
    "大自然", "大艺树", "荣程", "管仲", "雷士", "金斯卡", "德力西",
    "固帝", "依诺", "欧普", "三棵树", "立邦", "志邦",
]
UNIT_API = {
    "㎡": "Area", "平米": "Area", "m²": "Area", "个": "Count", "项": "Item", "处": "Place",
    "套": "Set", "户": "House", "m": "Metre", "米": "Metre", "cm": "Centimeter",
    "步": "Step", "樘": "Plate", "块": "Block", "根": "Root", "扇": "Hole",
    "台": "Pcs", "支": "Stick", "格": "Grid", "片": "Sheet",
}


def clean(value: Any) -> str:
    return str(value or "").replace("\n", " ").strip()


def number(value: Any) -> float:
    try:
        return float(value) if value not in (None, "") else 0.0
    except (TypeError, ValueError):
        return 0.0


def infer_brand(name: str) -> str:
    return next((brand for brand in BRANDS if brand.lower() in name.lower()), "")


def infer_spec(name: str) -> str:
    text = clean(name)
    size = re.search(r"(\d{3,4}\s*[*×xX]\s*\d{3,4}(?:\s*[*×xX]\s*\d{3,4})?)", text)
    if size:
        return re.sub(r"\s+", "", size.group(1)).replace("x", "*").replace("X", "*").replace("×", "*")
    return ""


def pricing(material: float, manual: float, formula: Any) -> tuple[str, str]:
    raw = clean(formula)
    if re.fullmatch(r"=[A-Z]+\d+", raw):
        return "FixedTotal", "固定总价，不乘工程量"
    if material > 0 and manual > 0:
        return "MaterialAndManual", "(材料单价 + 人工单价) × 工程量"
    if manual > 0:
        return "ManualOnly", "(人工单价) × 工程量"
    return "MaterialOnly", "(材料单价) × 工程量"


def rows(xlsx: Path) -> list[dict[str, Any]]:
    workbook = load_workbook(xlsx, data_only=False, read_only=True)
    result: list[dict[str, Any]] = []
    seen: set[tuple[str, str, str]] = set()
    for sheet_name, (catalog_type, type_name) in SHEETS.items():
        if sheet_name not in workbook.sheetnames:
            continue
        sheet = workbook[sheet_name]
        for row in range(5, sheet.max_row + 1):
            name = clean(sheet.cell(row, 2).value)
            category = clean(sheet.cell(row, 9).value)
            unit = clean(sheet.cell(row, 3).value).replace("元/㎡", "㎡").replace("平米", "㎡")
            if not name or not category or name in {"工程项目名称", "项目类型"}:
                continue
            key = (catalog_type, category, name)
            if key in seen:
                continue
            seen.add(key)
            material = number(sheet.cell(row, 5).value)
            manual = number(sheet.cell(row, 6).value)
            mode, formula = pricing(material, manual, sheet.cell(row, 7).value)
            item = {
                "type": catalog_type,
                "code": f"MH-{catalog_type[:3].upper()}-{row}",
                "category": category,
                "brand": infer_brand(name) or None,
                "spec": infer_spec(name) or None,
                "name": name,
                "unit": UNIT_API.get(unit, "Item"),
                "pricingMode": mode,
                "materialPrice": material,
                "manualPrice": manual,
                "costPrice": material + manual,
                "craft": clean(sheet.cell(row, 8).value),
                "criterion": clean(sheet.cell(row, 8).value),
                "description": f"{type_name}｜{category}｜来源：麦鸿装饰报价表",
                "sort": row,
                "active": True,
            }
            result.append(item)
            if catalog_type == "Material" and category in SOFT_CATEGORIES:
                soft = dict(item)
                soft.update({
                    "type": "Soft",
                    "code": f"MH-SOF-{row}",
                    "description": f"软装家具｜{category}｜来源：麦鸿装饰主材报价表",
                })
                result.append(soft)
    return result


def post_token(api: str, token: str, uri: str, payload: dict[str, Any]) -> dict[str, Any]:
    response = requests.post(f"{api.rstrip('/')}/{uri.lstrip('/')}", json=payload,
                             headers={"token": token}, timeout=30)
    return response.json()


def existing_keys(api: str, token: str) -> set[tuple[str, str, str]]:
    result: set[tuple[str, str, str]] = set()
    for catalog_type in ["Construction", "Material", "Soft", "General", "Custom"]:
        tree = post_token(api, token, "catalog/item/tree", {"type": catalog_type}).get("data") or {}
        for category in tree.get("categories", []):
            current = 1
            while True:
                envelope = post_token(api, token, "catalog/item/page", {
                    "type": catalog_type, "category": category, "current": current, "size": 100
                })
                data = envelope.get("data") or {}
                content = data.get("content") or []
                for item in content:
                    result.add((catalog_type, category, item.get("name", "")))
                total = int(data.get("totalElements") or 0)
                if current * 100 >= total or not content:
                    break
                current += 1
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--xlsx", default="/Users/zhangwei/Desktop/麦鸿装饰报价9.5.xlsx")
    parser.add_argument("--api", default="http://127.0.0.1/api")
    parser.add_argument("--token", required=True)
    parser.add_argument("--output", default="data/design/maihong-catalog-import.json")
    args = parser.parse_args()

    items = rows(Path(args.xlsx))
    existing = existing_keys(args.api, args.token)
    items = [item for item in items
             if (item["type"], item["category"], item["name"]) not in existing]
    success = 0
    failed: list[dict[str, str]] = []
    for item in items:
        try:
            response = requests.post(
                f"{args.api.rstrip('/')}/catalog/item/save",
                json=item,
                headers={"token": args.token},
                timeout=20,
            )
            envelope = response.json()
            if not envelope.get("success"):
                code = (envelope.get("errorCode") or {}).get("code", "")
                message = (envelope.get("errorCode") or {}).get("message", "未知错误")
                failed.append({"name": item["name"], "error": f"{code}: {message}"})
            else:
                success += 1
        except Exception as exc:
            failed.append({"name": item["name"], "error": str(exc)})

    result = {
        "source": args.xlsx,
        "total": len(items),
        "success": success,
        "failed": failed,
        "byType": {
            key: len([item for item in items if item["type"] == key])
            for key in ["Construction", "Material", "Soft", "General", "Custom"]
        },
    }
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if not failed else 1


if __name__ == "__main__":
    raise SystemExit(main())
