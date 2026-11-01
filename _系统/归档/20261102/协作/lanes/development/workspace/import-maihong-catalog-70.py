#!/usr/bin/env python3
"""从《麦鸿装饰报价9.5.xlsx》生成 70% 成本价报价项目导入 SQL。

数据口径：
- 来源页签：综合、施工、主材、全屋定制/橱柜报价清单；
- 售价：材料基价 + 人工基价（单元格 E + F）；
- 成本：售价 * 70%，四舍五入保留两位；
- 零价格和备注行跳过，重复名称保留为不同报价项；
- 仅写入目标企业的 catalog_item 和对应类型明细表。
"""

from __future__ import annotations

import argparse
from collections import Counter
from decimal import Decimal, ROUND_HALF_UP
import json
import re
from pathlib import Path
from typing import Any

from openpyxl import load_workbook

SHEETS = {
    "综合项目报价清单": ("General", "综合", "ZH"),
    "施工报价清单": ("Construction", "施工", "SG"),
    "主材报价清单": ("Material", "主材", "ZC"),
    "全屋定制、橱柜报价清单": ("Custom", "全屋定制", "DZ"),
}
UNIT_API = {
    "㎡": "Area", "平米": "Area", "m²": "Area", "平方米": "Area", "平米²": "Area", "元/㎡": "Area",
    "m": "Metre", "M": "Metre", "米": "Metre",
    "cm": "Centimeter", "厘米": "Centimeter",
    "个": "Count", "只": "Count", "项": "Item", "车": "Item", "处": "Place",
    "套": "Set", "户": "House", "步": "Step", "樘": "Plate", "块": "Block",
    "根": "Root", "扇": "Hole", "台": "Pcs", "支": "Stick", "格": "Grid", "片": "Sheet",
}
BRANDS = [
    "马可波罗", "东鹏", "亚细亚", "大角鹿", "利家福", "高尔夫", "利德福",
    "TATA", "欧派", "九牧", "惠达", "OXO", "爱智贞", "创博", "圣象",
    "大自然", "大艺树", "荣程", "管仲", "雷士", "金斯卡", "德力西",
    "固帝", "依诺", "欧普", "三棵树", "立邦", "志邦",
]


def clean(value: Any) -> str:
    return "" if value is None else re.sub(r"\s+", " ", str(value).replace("\n", " ")).strip()


def number(value: Any) -> float:
    if value in (None, "", "/", "//", "-", "—", "#VALUE!"):
        return 0.0
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def money(value: float | Decimal) -> Decimal:
    return Decimal(str(value)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def infer_brand(name: str) -> str:
    return next((brand for brand in BRANDS if brand.lower() in name.lower()), "")


def infer_spec(name: str) -> str:
    size = re.search(r"(\d{3,4}\s*[*×xX]\s*\d{3,4}(?:\s*[*×xX]\s*\d{3,4})?)", name)
    if not size:
        return ""
    return re.sub(r"\s+", "", size.group(1)).replace("x", "*").replace("X", "*").replace("×", "*")


def pricing(material: float, manual: float) -> tuple[str, str]:
    if material > 0 and manual > 0:
        return "MaterialAndManual", "(材料单价 + 人工单价) × 工程量"
    if manual > 0:
        return "ManualOnly", "人工单价 × 工程量"
    return "MaterialOnly", "材料单价 × 工程量"


def parse_items(xlsx: Path) -> list[dict[str, Any]]:
    workbook = load_workbook(xlsx, data_only=True, read_only=True)
    items: list[dict[str, Any]] = []
    for sheet_name, (catalog_type, type_name, prefix) in SHEETS.items():
        sheet = workbook[sheet_name]
        section = ""
        sequence = 0
        for row in range(4, sheet.max_row + 1):
            seq_raw = clean(sheet.cell(row, 1).value)
            raw_name = clean(sheet.cell(row, 2).value)
            raw_category = clean(sheet.cell(row, 9).value)
            if seq_raw and not raw_name and not raw_category:
                section = seq_raw
                continue
            if not seq_raw.replace(".", "", 1).isdigit() or not raw_name:
                continue
            material = number(sheet.cell(row, 5).value)
            manual = number(sheet.cell(row, 6).value)
            if material <= 0 and manual <= 0:
                continue
            sequence += 1
            full_name = raw_name
            series = ""
            name = full_name
            if "--" in full_name:
                series, name = [part.strip() for part in full_name.split("--", 1)]
            category = raw_category or section or "未分类"
            brand = infer_brand(full_name)
            if catalog_type == "Custom" and not brand:
                brand = "志邦" if "志邦" in full_name or "志邦" in section else "麦鸿定制"
            sale_price = money(Decimal(str(material)) + Decimal(str(manual)))
            cost_price = money(sale_price * Decimal("0.70"))
            mode, formula = pricing(material, manual)
            unit_raw = clean(sheet.cell(row, 3).value)
            unit = UNIT_API.get(unit_raw, "Item")
            item = {
                "source_sheet": sheet_name,
                "source_row": row,
                "type": catalog_type,
                "type_name": type_name,
                "code": f"MH-{prefix}-{sequence:04d}",
                "category": category[:64],
                "brand": (brand or None),
                "series": (series[:128] or None),
                "spec": (infer_spec(full_name)[:256] or None),
                "name": name[:256],
                "unit": unit,
                "unit_raw": unit_raw or "项",
                "pricing_mode": mode,
                "material_price": money(material),
                "manual_price": money(manual),
                "sale_price": sale_price,
                "cost_price": cost_price,
                "formula": formula,
                "craft": clean(sheet.cell(row, 8).value) or None,
                "description": f"{type_name}｜{category}｜来源：麦鸿装饰报价9.5.xlsx",
                "sort": row,
            }
            items.append(item)
    return items


def sql_text(value: Any) -> str:
    if value is None:
        return "NULL"
    text = str(value).replace("\\", "\\\\").replace("'", "''")
    return f"'{text}'"


def generate_sql(items: list[dict[str, Any]], com_id: int, maintainer_id: int) -> str:
    lines = [
        "SET NAMES utf8mb4;",
        "START TRANSACTION;",
        f"DELETE d FROM catalog_appliance_detail d JOIN catalog_item i ON i.item_id=d.item_id WHERE i.com_id={com_id};",
        f"DELETE d FROM catalog_construction_detail d JOIN catalog_item i ON i.item_id=d.item_id WHERE i.com_id={com_id};",
        f"DELETE d FROM catalog_custom_detail d JOIN catalog_item i ON i.item_id=d.item_id WHERE i.com_id={com_id};",
        f"DELETE d FROM catalog_general_detail d JOIN catalog_item i ON i.item_id=d.item_id WHERE i.com_id={com_id};",
        f"DELETE d FROM catalog_material_detail d JOIN catalog_item i ON i.item_id=d.item_id WHERE i.com_id={com_id};",
        f"DELETE d FROM catalog_soft_detail d JOIN catalog_item i ON i.item_id=d.item_id WHERE i.com_id={com_id};",
        f"DELETE im FROM catalog_item_images im JOIN catalog_item i ON i.item_id=im.item_id WHERE i.com_id={com_id};",
        f"DELETE FROM catalog_item WHERE com_id={com_id};",
    ]
    base_id = 227000000000000000
    for index, item in enumerate(items):
        item_id = base_id + index + 1
        detail_id = base_id + 100000 + index + 1
        item["item_id"] = item_id
        lines.append(
            "INSERT INTO catalog_item (item_id,active,brand,category,code,cost_price,craft,criterion,description,"
            "formula,manual_price,material_price,name,name_initials,pricing_mode,series,sort,spec,type,unit,com_id,maintainer_id) VALUES "
            f"({item_id},b'1',{sql_text(item['brand'])},{sql_text(item['category'])},{sql_text(item['code'])},"
            f"{item['cost_price']},{sql_text(item['craft'])},{sql_text(item['craft'])},{sql_text(item['description'])},"
            f"{sql_text(item['formula'])},{item['manual_price']},{item['material_price']},{sql_text(item['name'])},NULL,"
            f"{sql_text(item['pricing_mode'])},{sql_text(item['series'])},{item['sort']},{sql_text(item['spec'])},"
            f"{sql_text(item['type'])},{sql_text(item['unit'])},{com_id},{maintainer_id});"
        )
        if item["type"] == "Construction":
            concealed = int(any(key in ((item["name"] or "") + (item["craft"] or "")) for key in ("隐蔽", "防水", "水电")))
            lines.append(
                "INSERT INTO catalog_construction_detail (detail_id,concealed,craft,criterion,duration_days,measurement_formula,stage,com_id,item_id) VALUES "
                f"({detail_id},b'{concealed}',{sql_text(item['craft'])},{sql_text(item['craft'])},0,{sql_text(item['formula'])},"
                f"{sql_text(item['category'])},{com_id},{item_id});"
            )
        elif item["type"] == "Material":
            lines.append(
                "INSERT INTO catalog_material_detail (detail_id,color,inventory_mode,loss_rate,material,minimum_order,model,com_id,item_id) VALUES "
                f"({detail_id},NULL,NULL,0,NULL,1,{sql_text(item['spec'])},{com_id},{item_id});"
            )
        elif item["type"] == "General":
            lines.append(
                "INSERT INTO catalog_general_detail (detail_id,allocation_mode,approval_required,discountable,fee_type,invoice_type,com_id,item_id) VALUES "
                f"({detail_id},NULL,b'0',b'1',{sql_text(item['category'])},NULL,{com_id},{item_id});"
            )
        elif item["type"] == "Custom":
            lines.append(
                "INSERT INTO catalog_custom_detail (detail_id,board,cabinet_type,countertop,door,hardware,parametric_formula,com_id,item_id) VALUES "
                f"({detail_id},NULL,{sql_text(item['category'])},NULL,NULL,NULL,{sql_text(item['formula'])},{com_id},{item_id});"
            )
    lines.append("COMMIT;")
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--xlsx", default="/Users/zhangwei/Desktop/麦鸿装饰报价9.5.xlsx")
    parser.add_argument("--com-id", type=int, required=True)
    parser.add_argument("--maintainer-id", type=int, required=True)
    parser.add_argument("--output-dir", required=True)
    args = parser.parse_args()
    items = parse_items(Path(args.xlsx))
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    sql = generate_sql(items, args.com_id, args.maintainer_id)
    (output_dir / "maihong-catalog-70.sql").write_text(sql, encoding="utf-8")
    summary = {
        "source": args.xlsx,
        "target": {"com_id": args.com_id, "maintainer_id": args.maintainer_id},
        "count": len(items),
        "byType": Counter(item["type"] for item in items),
        "saleTotal": str(sum((item["sale_price"] for item in items), Decimal("0.00"))),
        "costTotal": str(sum((item["cost_price"] for item in items), Decimal("0.00"))),
        "costRate": "70%",
        "unitFallbacks": [{"sheet": item["source_sheet"], "row": item["source_row"], "unit": item["unit_raw"], "name": item["name"]} for item in items if item["unit_raw"] != "项" and item["unit"] == "Item"],
    }
    (output_dir / "maihong-catalog-70-preview.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
