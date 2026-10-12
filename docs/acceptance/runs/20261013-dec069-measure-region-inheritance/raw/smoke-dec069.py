#!/usr/bin/env python3
"""DEC-069 / DEC-066 走查：量房原始区域 → 实例化继承 → 量房完成后冻结 → 不联动 → 参考值不进金额。

用法：python3 smoke-dec069.py
环境：本地 dev 后端 http://127.0.0.1:8090/api，设计师 董夏鹏（手机号 18225940242，走查期间临时重置为本地测试口令，事后恢复原哈希）。
只操作本地 dev DB；脚本创建的数据保留在 dev 库（命名带 走查 前缀），不做跨环境操作。
"""
import http.client
import json
import sys

HOST = "127.0.0.1"
PORT = 8090
PROPERTY_ID = 228771992327684096  # H1790233093343，designer = 董夏鹏（19900000005）
EMP_ID = 1790221396230505495
PHONE = "18225940242"  # 董夏鹏（Designer）本地 dev 登录手机号
AREA_ITEM_ID = 102080338905269488  # 单位 Area(㎡)，pricingMode ManualOnly
LOG = []


def call(provider, payload, token=None):
    """注意：后端要求请求头名**精确为小写 token**（大写 Token 会被判未授权），因此用 http.client 直发。"""
    headers = {"Content-Type": "application/json"}
    if token:
        headers["token"] = token
    connection = http.client.HTTPConnection(HOST, PORT, timeout=30)
    connection.request("POST", "/api/" + provider, body=json.dumps(payload).encode("utf-8"), headers=headers)
    response = connection.getresponse()
    body = json.loads(response.read().decode("utf-8") or "{}")
    connection.close()
    LOG.append({"provider": provider, "request": payload, "response": body})
    return body


def ok(provider, payload, token=None):
    body = call(provider, payload, token)
    if not body.get("success"):
        raise AssertionError(f"{provider} 失败: {body.get('errorCode')}")
    return body.get("data")


def expect_fail(provider, payload, token, keyword):
    body = call(provider, payload, token)
    code = (body.get("errorCode") or {}).get("code")
    message = (body.get("errorCode") or {}).get("message") or ""
    assert not body.get("success"), f"{provider} 本应失败但成功: {body}"
    assert keyword in message, f"{provider} 失败文案不含「{keyword}」: {message}"
    print(f"  ✅ {provider} 被拒（{code}）：{message}")
    return message


def main():
    print("== 1. 设计师登录 ==")
    login = ok("auth/session/password-login", {"phone": PHONE, "password": "Design2026!"})
    token = login["token"]
    session = ok("auth/session/info", {}, token)
    me = session.get("employee") or {}
    assert str(me.get("empId")) == str(EMP_ID), f"登录身份不符: {me}"
    print(f"  ✅ {me.get('name')} / {me.get('role')} / empId={me.get('empId')}")

    print("== 2. 量房单（量房中）+ 录入原始区域 ==")
    measure = ok("property/measure/save", {"propertyId": PROPERTY_ID, "status": "Scheduled",
                                          "originalStructure": "走查：原始结构（DEC-069）"}, token)
    measure_id = measure["measureId"]
    assert (measure["status"] or {}).get("key") == "Scheduled", measure["status"]
    print(f"  ✅ 量房单 {measure_id} 状态=量房中")

    ok("property/measure/region/save", {"measureId": measure_id, "roomKey": "R1", "roomType": "Bedroom",
                                        "name": "主卧", "area": 15.30, "perimeter": 16.20}, token)
    ok("property/measure/region/save", {"measureId": measure_id, "roomKey": "R2", "roomType": "LivingRoom",
                                        "name": "客厅", "area": 26.00, "perimeter": 24.00}, token)
    regions = ok("property/measure/region/list", {"measureId": measure_id}, token)
    assert len(regions) == 2, regions
    assert all(r["areaSource"]["value"] == "Measured" for r in regions), regions
    print(f"  ✅ 原始区域 2 条（主卧 15.30/16.20、客厅 26.00/24.00），来源=Measured")

    print("== 3. 量房完成 → 原始区域冻结 ==")
    ok("property/measure/save", {"propertyId": PROPERTY_ID, "status": "Completed"}, token)
    expect_fail("property/measure/region/save", {"measureId": measure_id, "roomKey": "R1", "roomType": "Bedroom",
                                                 "name": "主卧", "area": 18.00, "perimeter": 17.00}, token,
                "原始结构不可再修改")
    expect_fail("property/measure/region/delete", {"regionId": regions[0]["regionId"]}, token,
                "原始结构不可再修改")

    print("== 4. 模版（区域骨架 + 公式 =A）→ 发布 → 实例化继承 ==")
    template = ok("design/template/save", {"name": "走查-DEC069-继承", "houseType": "三室两厅", "scope": "Personal"}, token)
    template_id = template["templateId"]
    version_id = template["currentVersion"]["versionId"]
    ok("design/template/room/save", {"versionId": version_id, "roomKey": "R1", "name": "主卧",
                                     "roomType": "Bedroom", "sortNo": 0, "count": 1}, token)
    ok("design/template/items/batchsave", {"versionId": version_id, "deletedItemIds": [], "items": [
        {"roomKey": "R1", "catalogItem": {"itemId": AREA_ITEM_ID}, "quantityFormula": "=A", "sortNo": 0}
    ]}, token)
    ok("design/template/publish", {"templateId": template_id}, token)
    design = ok("design/template/instantiate", {"templateId": template_id, "propertyId": PROPERTY_ID}, token)
    design_id = design["designId"]
    scheme = ok("design/scheme/info", {"designId": design_id}, token)

    space = (scheme.get("spaces") or [])[0]
    assert abs(float(space["area"]) - 15.30) < 1e-6, space
    assert space["areaSource"]["value"] == "Measured", space
    assert space["valueOrigin"]["value"] == "Measure", space
    assert str(space["sourceMeasureRegionId"]) == str(regions[0]["regionId"]), space
    item = (scheme.get("items") or [])[0]
    assert abs(float(item["quantity"]) - 15.30) < 1e-6, item
    assert not item.get("pendingMeasure"), item
    assert abs(float(scheme["area"]) - 15.30) < 1e-6, scheme.get("area")
    print(f"  ✅ 方案 {scheme['no']}：主卧面积=15.30（来源=量房原始结构继承）、分项工程量=15.30、方案面积=15.30")

    print("== 5. 复尺（新量房单改原始区域）→ 已建方案不联动 ==")
    remeasure = ok("property/measure/save", {"propertyId": PROPERTY_ID, "status": "Scheduled"}, token)
    remeasure_id = remeasure["measureId"]
    ok("property/measure/region/save", {"measureId": remeasure_id, "roomKey": "R1", "roomType": "Bedroom",
                                        "name": "主卧", "area": 18.00, "perimeter": 17.00}, token)
    ok("property/measure/save", {"propertyId": PROPERTY_ID, "status": "Completed"}, token)
    scheme_after = ok("design/scheme/info", {"designId": design_id}, token)
    space_after = (scheme_after.get("spaces") or [])[0]
    item_after = (scheme_after.get("items") or [])[0]
    assert abs(float(space_after["area"]) - 15.30) < 1e-6, space_after
    assert abs(float(item_after["quantity"]) - 15.30) < 1e-6, item_after
    print(f"  ✅ 复尺后原始区域=18.00，已建方案仍为 15.30 / 工程量 15.30（不联动）")

    print("== 6. 金额门禁：模版参考值只打底、不进金额 ==")
    template2 = ok("design/template/save", {"name": "走查-DEC069-参考值", "houseType": "三室两厅", "scope": "Personal"}, token)
    template2_id = template2["templateId"]
    version2_id = template2["currentVersion"]["versionId"]
    ok("design/template/room/save", {"versionId": version2_id, "roomKey": "R9", "name": "次卧",
                                     "roomType": "Bedroom", "sortNo": 0, "count": 1,
                                     "referenceArea": 14.60, "referencePerimeter": 15.80}, token)
    ok("design/template/items/batchsave", {"versionId": version2_id, "deletedItemIds": [], "items": [
        {"roomKey": "R9", "catalogItem": {"itemId": AREA_ITEM_ID}, "quantityFormula": "=A", "sortNo": 0}
    ]}, token)
    ok("design/template/publish", {"templateId": template2_id}, token)
    design2 = ok("design/template/instantiate", {"templateId": template2_id, "propertyId": PROPERTY_ID}, token)
    scheme2 = ok("design/scheme/info", {"designId": design2["designId"]}, token)
    space2 = (scheme2.get("spaces") or [])[0]
    item2 = (scheme2.get("items") or [])[0]
    assert abs(float(space2["area"]) - 14.60) < 1e-6, space2
    assert space2["areaSource"]["value"] == "Estimated", space2
    assert space2["valueOrigin"]["value"] == "TemplateReference", space2
    assert float(item2["quantity"]) == 0 and item2.get("pendingMeasure"), item2
    assert abs(float(scheme2["area"])) < 1e-6, scheme2.get("area")
    print(f"  ✅ 参考值 14.60 只作预览起点：来源=模版参考值/估算，分项待补、方案面积 0（不进金额）")

    print("\n全部断言通过 ✅")
    return 0


if __name__ == "__main__":
    try:
        code = main()
    except AssertionError as error:
        print(f"\n❌ 走查失败：{error}")
        code = 1
    finally:
        with open("docs/acceptance/runs/20261013-dec069-measure-region-inheritance/raw/console/out.json", "w",
                  encoding="utf-8") as handle:
            json.dump(LOG, handle, ensure_ascii=False, indent=2)
    sys.exit(code)
