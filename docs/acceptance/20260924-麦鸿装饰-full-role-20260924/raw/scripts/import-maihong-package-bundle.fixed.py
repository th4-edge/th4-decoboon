#!/usr/bin/env python3
"""导入麦鸿报价项目库，并创建两套套餐模板。

先运行准备脚本生成：
  data/import/maihong-9.5/catalog-items.json
  data/import/maihong-9.5/package-template-plan.json

执行导入：
  python3 scripts/import-maihong-package-bundle.py \
    --api http://127.0.0.1:8090/api \
    --token "<BOSS token>"
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

import requests

TYPE_NAMES = ["Construction", "Material", "Soft", "General", "Custom", "Appliance"]


class Api:
    def __init__(self, base: str, token: str):
        self.base = base.rstrip("/")
        self.token = token

    def post(self, uri: str, payload: dict[str, Any] | None = None) -> Any:
        response = requests.post(
            f"{self.base}/{uri.lstrip('/')}",
            json=payload or {},
            headers={"token": self.token},
            timeout=60,
        )
        envelope = response.json()
        if response.status_code >= 400 or not envelope.get("success"):
            error = envelope.get("errorCode") or {}
            raise RuntimeError(f"{uri}: {error.get('code', response.status_code)} {error.get('message', '')}")
        return envelope.get("data")


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def list_catalog(api: Api, catalog_type: str) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    current = 1
    while True:
        data = api.post("catalog/item/page", {
            "type": catalog_type,
            "current": current,
            "size": 200,
            "includeInactive": True,
        }) or {}
        content = data.get("content") or []
        result.extend(content)
        if not content or current * 200 >= int(data.get("totalElements") or 0):
            break
        current += 1
    return result


def existing_catalog(api: Api) -> dict[tuple[str, str, str], dict[str, Any]]:
    result: dict[tuple[str, str, str], dict[str, Any]] = {}
    for catalog_type in TYPE_NAMES:
        for item in list_catalog(api, catalog_type):
            result[(catalog_type, item.get("category", ""), item.get("name", ""))] = item
    return result


def import_catalog(api: Api, items: list[dict[str, Any]]) -> dict[str, Any]:
    existing = existing_catalog(api)
    missing = [item for item in items
               if (item["type"], item["category"], item["name"]) not in existing]
    imported = api.post("catalog/item/import", {"items": missing}) if missing else {
        "total": 0, "success": 0, "failed": 0, "failures": []
    }
    return {"existing": len(items) - len(missing), "missing": len(missing), "result": imported}


def template_by_name(api: Api, name: str) -> dict[str, Any] | None:
    result = api.post("pack/template/page", {"current": 1, "size": 200}) or {}
    if isinstance(result, dict):
        templates = result.get("content") or result.get("list") or result.get("records") or []
    else:
        templates = result
    return next((item for item in templates if item.get("name") == name), None)


def create_template(api: Api, plan: dict[str, Any], catalog: dict[tuple[str, str, str], dict[str, Any]]) -> dict[str, Any]:
    current = template_by_name(api, plan["name"])
    if current:
        template = api.post("pack/template/update", {
            "templateId": current.get("templateId") or current.get("id"),
            "name": plan["name"],
            "area": plan["standardArea"],
            "price": plan["price"],
            "discount": plan["discount"],
        })
    else:
        template = api.post("pack/template/save", {
            "name": plan["name"],
            "area": plan["standardArea"],
            "price": plan["price"],
            "discount": plan["discount"],
        })
    template_id = str(template.get("templateId") or template.get("id"))
    detail = api.post("pack/template/info", {"templateId": template_id}) or template
    existing_sections = {
        (line.get("section") or {}).get("name"): str((line.get("section") or {}).get("groupId") or (line.get("section") or {}).get("id"))
        for line in (detail.get("items") or [])
        if (line.get("section") or {}).get("name")
    }
    existing_lines = {
        ((line.get("section") or {}).get("groupId"), line.get("name")): line
        for line in (detail.get("items") or [])
    }
    created_sections = 0
    created_lines = 0
    skipped_lines = 0
    for section_plan in plan["sections"]:
        section_id = existing_sections.get(section_plan["name"])
        if not section_id:
            section = api.post("pack/section/save", {
                "name": section_plan["name"],
                "spaceTypes": [section_plan["spaceType"]] if section_plan.get("spaceType") else [],
                "maxDeleteNum": 1,
                "priority": section_plan["priority"],
            })
            section_id = str(section.get("groupId") or section.get("id"))
            existing_sections[section_plan["name"]] = section_id
            created_sections += 1
        for line_plan in section_plan["lines"]:
            key = line_plan["catalogKey"]
            catalog_key = (key["type"], key["category"], key["name"])
            catalog_item = catalog.get(catalog_key)
            if not catalog_item:
                raise RuntimeError(f"报价项目不存在，无法创建套餐行: {catalog_key}")
            existing = existing_lines.get((section_id, line_plan["name"]))
            if existing:
                skipped_lines += 1
                continue
            api.post("pack/line/save", {
                "templateId": template_id,
                "sectionId": section_id,
                "catalogItemId": str(catalog_item.get("itemId") or catalog_item.get("id")),
                "name": line_plan["name"],
                "quantityFactor": line_plan["quantityFactor"],
                "pricingMethod": line_plan["pricingMethod"],
                "included": line_plan["included"],
                "deductible": line_plan["deductible"],
                "sort": line_plan["sort"],
                "criterions": [line_plan["rule"]],
            })
            created_lines += 1
    final = api.post("pack/template/info", {"templateId": template_id})
    return {
        "templateId": template_id,
        "name": final.get("name"),
        "cost": final.get("cost"),
        "sections": created_sections,
        "createdLines": created_lines,
        "skippedLines": skipped_lines,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--api", default="http://127.0.0.1:8090/api")
    parser.add_argument("--token", required=True)
    parser.add_argument("--catalog", default="data/import/maihong-9.5/catalog-items.json")
    parser.add_argument("--plan", default="data/import/maihong-9.5/package-template-plan.json")
    parser.add_argument("--output", default="data/import/maihong-9.5/import-result.json")
    args = parser.parse_args()

    api = Api(args.api, args.token)
    catalog_items = load_json(Path(args.catalog))
    package_plan = load_json(Path(args.plan))
    catalog_result = import_catalog(api, catalog_items)
    catalog = existing_catalog(api)
    templates = [create_template(api, template, catalog) for template in package_plan["templates"]]
    result = {
        "catalog": catalog_result,
        "templates": templates,
    }
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
