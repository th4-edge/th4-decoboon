#!/usr/bin/env python3
"""主材选材（DEC-056）本地 dev 接口冒烟。

覆盖：发现可编辑主材项 -> 保存(不交付) -> 选材结果回读 -> 多图上传+回读 ->
保存并交付 -> 材料计划进入 ReadyToOrder -> 权限反向（Pm 写入被拒）。
"""
from __future__ import annotations
import base64, json, pathlib, sys, time
import requests

BASE = "http://127.0.0.1:8090/api"
PASSWORD = "123456"
ACCOUNTS = {
    "finance": {"phone": "19900000005", "name": "吴濛濛"},
    "designer": {"phone": "19900000003", "name": "谢玲玲"},
    "pm": {"phone": "19900000006", "name": "徐勇"},
}
OUT = pathlib.Path(__file__).parent / "smoke.json"
LOG: list[dict] = []


def brief(body):
    if not isinstance(body, dict):
        return body
    err = body.get("errorCode")
    if isinstance(err, dict):
        err = {"code": err.get("code"), "message": err.get("message")}
    return {"success": body.get("success"), "errorCode": err, "data": body.get("data")}


def call(route, payload=None, token=None, files=None, data=None, timeout=40):
    headers = {}
    if token:
        headers["token"] = token
    if files:
        resp = requests.post(f"{BASE}/{route}", headers=headers, data=data or {}, files=files, timeout=timeout)
    else:
        headers["Content-Type"] = "application/json"
        resp = requests.post(f"{BASE}/{route}", json=payload or {}, headers=headers, timeout=timeout)
    try:
        body = resp.json()
    except ValueError:
        body = {"raw": resp.text}
    return resp.status_code, body


def step(name, route, payload=None, token=None, files=None, data=None):
    code, body = call(route, payload, token, files, data)
    b = brief(body)
    LOG.append({"step": name, "route": route,
                "request": {k: v for k, v in (payload or {}).items() if k not in ("selectionJson",)} if payload else (data or None),
                "httpStatus": code, "response": b})
    ok = isinstance(b, dict) and b.get("success")
    print(f"[{name}] http={code} success={ok} err={(b.get('errorCode') or '') if isinstance(b, dict) else ''}")
    return body


def login(role):
    code, body = call("auth/session/password-login", {"phone": ACCOUNTS[role]["phone"], "password": PASSWORD})
    token = (body.get("data") or {}).get("token")
    if not token:
        raise RuntimeError(f"登录失败 {role}: {body}")
    print(f"[login:{role}] ok {ACCOUNTS[role]['name']}")
    return token


def page_rows(body):
    d = body.get("data")
    if isinstance(d, list):
        return d
    if isinstance(d, dict):
        for k in ("content", "list", "records"):
            if isinstance(d.get(k), list):
                return d[k]
    return []


def main():
    result = {"at": time.strftime("%Y-%m-%d %H:%M:%S")}
    boss = login("finance")  # 财务：企业范围，可发现项目与校验材料计划
    designer = login("designer")
    pm = login("pm")
    result["accounts"] = ACCOUNTS

    # 1) 发现一个存在可编辑（未进入采购）主材分项的项目
    projects = page_rows(step("project/page", "project/project/page", {}, boss))
    print(f"  项目数={len(projects)}")
    target = None
    scan = []
    for proj in projects:
        pid = str(proj.get("projectId") or proj.get("id") or "")
        if not pid:
            continue
        body = step(f"item/page:{pid}", "material/selection/item/page", {"projectId": pid}, boss)
        items = page_rows(body)
        editable = [i for i in items if i.get("orderStatus") in (None, "none") and i.get("status") in (None, "Selected")]
        scan.append({"projectId": pid, "projectName": proj.get("name"), "items": len(items), "editable": len(editable)})
        if editable:
            target = {"project": proj, "item": editable[0], "items": items}
            break
    result["scan"] = scan
    if not target:
        result["status"] = "BLOCKED_NO_EDITABLE_ITEM"
        OUT.write_text(json.dumps({"result": result, "log": LOG}, ensure_ascii=False, indent=2), encoding="utf-8")
        print("未发现可编辑主材分项，阻断")
        return 2
    item = target["item"]
    project = target["project"]
    result["target"] = {
        "projectId": str(project.get("projectId") or project.get("id")),
        "projectName": project.get("name"),
        "propertyId": str(item.get("propertyId")),
        "contractLineId": str(item.get("itemId")),
        "materialName": item.get("materialName"),
        "beforeOrderStatus": item.get("orderStatus"),
        "beforeStatus": item.get("status"),
    }
    print(f"  目标: {item.get('materialName')} @ {project.get('name')} orderStatus={item.get('orderStatus')}")

    pid = str(project.get("projectId") or project.get("id"))
    property_id = str(item.get("propertyId"))
    line_id = str(item.get("itemId"))

    # 2) 保存（不交付）——用 designer 身份
    content = "颜色/花色：暖灰石纹；规格/型号：750×1500；品牌/系列：马可波罗；工艺：哑光防滑；备注：接口冒烟"
    sel_json = json.dumps({"color": "暖灰石纹", "spec": "750×1500", "brand": "马可波罗", "craft": "哑光防滑", "remark": "接口冒烟"}, ensure_ascii=False)
    save_body = step("designer save(不交付)", "material/selection/save",
                     {"propertyId": property_id, "projectId": pid, "contractLineId": line_id,
                      "title": item.get("materialName"), "content": content, "selectionJson": sel_json,
                      "attachments": [], "deliver": False}, designer)
    result["save"] = brief(save_body)
    saved = save_body.get("data") or {}
    result["savedSelectionId"] = saved.get("selectionId") or saved.get("id")

    # 3) 回读选材结果
    info = step("item/info 回读", "material/selection/item/info",
                {"projectId": pid, "contractLineId": line_id}, designer)
    info_item = (info.get("data") or {})
    result["afterSave"] = {"status": info_item.get("status"), "statusName": info_item.get("statusName"),
                           "orderStatus": info_item.get("orderStatus"), "orderStatusName": info_item.get("orderStatusName"),
                           "selectionText": info_item.get("selectionText")}

    # 4) 多图上传（businessType=MaterialSelection）+ 回读
    png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M8AAAMBAQDJ/pLvAAAAAElFTkSuQmCC")
    files = {"file": ("smoke-selection.png", png, "image/png")}
    code, body = call("file/private/grant", token=designer, files=files,
                      data={"businessType": "MaterialSelection", "projectId": pid})
    grant = (body.get("data") or [{}])[0] if isinstance(body.get("data"), list) else {}
    result["upload"] = {"httpStatus": code, "success": body.get("success"), "path": grant.get("path")}
    print(f"[upload] http={code} path={grant.get('path')}")
    LOG.append({"step": "upload(MaterialSelection)", "route": "file/private/grant",
                "request": {"businessType": "MaterialSelection", "projectId": pid, "file": "smoke-selection.png"},
                "httpStatus": code, "response": brief(body)})
    photo_path = grant.get("path")

    if photo_path:
        # 重新保存带图 + 交付
        deliver_body = step("designer save(带图并交付)", "material/selection/save",
                            {"propertyId": property_id, "projectId": pid, "contractLineId": line_id,
                             "title": item.get("materialName"), "content": content, "selectionJson": sel_json,
                             "attachments": [photo_path], "deliver": True}, designer)
        result["deliver"] = brief(deliver_body)
        info2 = step("item/info 回读(交付后)", "material/selection/item/info",
                     {"projectId": pid, "contractLineId": line_id}, designer)
        i2 = (info2.get("data") or {})
        result["afterDeliver"] = {"status": i2.get("status"), "orderStatus": i2.get("orderStatus"),
                                  "orderStatusName": i2.get("orderStatusName"),
                                  "attachments": i2.get("attachments"), "nextAction": i2.get("nextAction")}
        # 图片回读授权（Employee）
        code, gbody = call("file/private/grant", {"businessType": "MaterialSelection",
                                                  "businessId": str(i2.get("selectionId") or ""),
                                                  "paths": [photo_path]}, designer)
        result["photoGrant"] = {"httpStatus": code, "success": gbody.get("success"),
                                "urlPresent": bool(((gbody.get("data") or [{}])[0] or {}).get("url"))}
        print(f"[photo-grant] http={code} success={gbody.get('success')}")

    # 5) 材料计划核对
    plans = page_rows(step("plan/page", "project/material-plan/page", {"projectId": pid}, boss))
    matched = [p for p in plans if str((p.get("contractLine") or p.get("sourceContractLine") or {}).get("lineId") or p.get("contractLineId")) == line_id]
    result["plans"] = [{"planId": str(p.get("planId") or p.get("id")), "status": p.get("status"), "statusName": p.get("statusName"),
                        "selectionId": str((p.get("selection") or {}).get("selectionId") or "")} for p in matched]
    print(f"  匹配材料计划 {len(matched)} 条: {[p['status'] for p in result['plans']]}")

    # 6) 权限反向：Pm 保存被拒
    code, pbody = call("material/selection/save",
                       {"propertyId": property_id, "projectId": pid, "contractLineId": line_id,
                        "title": item.get("materialName"), "content": "PM 越权写入", "deliver": False}, pm)
    result["pmNegative"] = {"httpStatus": code, "success": pbody.get("success"), "errorCode": (pbody.get("errorCode") or {})}
    print(f"[pm-negative] success={pbody.get('success')} err={(pbody.get('errorCode') or {}).get('code') if isinstance(pbody.get('errorCode'), dict) else ''}")

    OUT.write_text(json.dumps({"result": result, "log": LOG}, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"证据: {OUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
