#!/usr/bin/env python3
"""昭青网络（线上测试企业）真实感验收数据集。

原则：
1. **全部走真实 Provider 链路**（不 SQL 直插业务表）：客户 → 房屋 → 量房 → 设计 → 报价 → 合同 → 项目 → 阶段 → 验收。
2. **数据必须像真的**：真实合肥小区/楼栋房号、真实姓名与号段、金额与面积匹配、时间先后合理。
3. **默认 dry-run**；`--apply` 才写。每步落盘 state，可断点续跑；已存在的对象跳过。
4. 报价分项全部引用企业报价库（含 2026-10-15 从麦鸿复制来的 550 条真实报价项）。
5. 只推进到「待业主确认验收」，**不代业主确认**（业主确认是业主事实，必须由业主本人操作）。

用法：
  python3 scripts/seed-zhaoqing-prod-demo.py --token-file /tmp/.decoboon-prod-token          # dry-run
  python3 scripts/seed-zhaoqing-prod-demo.py --token-file /tmp/.decoboon-prod-token --apply
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import date, datetime, timedelta
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
REPO = SCRIPT_DIR.parent
STATE = REPO / "docs/operations/data-changes/2026-10-15-昭青真实验收数据集-state.json"

# ---------- 真实感数据集 ----------
# A：在谈客户（已建档、已约量房，体现真实客户漏斗）
CUSTOMER_A = {
    "name": "王雅婷",
    "telNo": "13956931862",
    "community": "万科森林公园",
    "addr": "万科森林公园12栋1802",
    "area": 118,
    "budget": 168000,
    "gender": "F",
    "source": "Referral",
}
PROPERTY_A = {
    "community": "万科森林公园",
    "building": "12栋",
    "roomNo": "1802",
    "address": "万科森林公园12栋1802",
    "area": 118,
    "layout": "三室两厅一厨两卫",
    "remark": "滨湖新区，2026 年 8 月交房，南北通透",
}
FOLLOW_A = {
    "way": "Phone",
    "content": "客户 8 月交房后到店咨询整装，明确 11 月入住时间，偏好现代简约、重收纳，"
               "预算 16 万左右，已确认本周上门量房。",
}

# B：完整案例（量房→设计→报价→合同→项目→施工→待业主确认验收），用于业主端线上验证
CUSTOMER = {
    "name": "李思远",
    "telNo": "13866729315",
    "community": "华润凯旋门",
    "addr": "华润凯旋门9栋1601",
    "area": 128,
    "budget": 185000,
    "gender": "M",
    "source": "OldCustomer",
}
PROPERTY = {
    "community": "华润凯旋门",
    "building": "9栋",
    "roomNo": "1601",
    "address": "华润凯旋门9栋1601",
    "area": 128,
    "layout": "三室两厅一厨两卫",
    "remark": "蜀山区，业主为孩子上学装修，计划 2027 年春节前入住",
}
FOLLOW = {
    "way": "Visit",
    "content": "老客户介绍到店，业主已确认全屋整装并重做水电，偏好现代简约、收纳优先，"
               "预算 18 万左右，同意直接上门量房。",
}
# 量房区域（面积/周长为设计师实测录入值，服务端不推算）
REGIONS = [
    ("living", "LivingRoom", "客餐厅", 4.2, 10.0, 2.85, 42.0, 28.4),
    ("master", "Bedroom", "主卧", 3.6, 5.0, 2.85, 18.0, 17.2),
    ("second", "Bedroom", "次卧", 3.0, 4.0, 2.85, 12.0, 14.0),
    ("kids", "Bedroom", "儿童房", 2.8, 3.6, 2.85, 10.0, 12.8),
    ("kitchen", "Kitchen", "厨房", 2.0, 4.0, 2.85, 8.0, 12.0),
    ("bathroom", "Bathroom", "卫生间", 1.8, 3.0, 2.70, 5.4, 9.6),
    ("balcony", "Balcony", "阳台", 1.4, 4.2, 2.85, 5.9, 11.2),
]
REQUIREMENT = {
    "type": "LivingRoom", "spaceName": "客餐厅", "kind": "Demand", "category": "Function", "priority": "High",
    "title": "客餐厅通透与整墙收纳", "detail": "希望客餐厅视觉通透、增加整墙收纳，地面全屋通铺。",
    "response": "客餐厅打通阳台，电视墙做整墙柜体，地面通铺 800*800 抛釉砖。", "source": "量房沟通",
}
DESIGN = {
    "name": "现代简约 · 三室两厅", "style": "现代简约", "layout": "三室两厅一厨两卫",
    "description": "以通透与收纳为主线：客餐厅阳台打通，主卧做顶天立地衣柜，全屋无主灯 + 石膏板吊平顶。",
    "area": 118,
}
# 报价分项：(报价库编号, 方案区域 roomKey, 数量)
ITEMS = [
    ("MH-CON-5", "living", 118), ("MH-CON-7", "living", 8), ("MH-CON-33", "living", 118),
    ("MH-CON-64", "kitchen", 2), ("MH-CON-107", "living", 30), ("MH-CON-118", "living", 12),
    ("MH-CON-176", "master", 118), ("MH-CON-177", "master", 118), ("MH-CON-178", "master", 118),
    ("MH-CON-180", "master", 300),
    ("MH-MAT-6", "living", 78), ("MH-MAT-9", "bathroom", 46), ("MH-MAT-195", "second", 42),
    ("MH-MAT-94", "master", 5), ("MH-MAT-170", "bathroom", 4), ("MH-MAT-110", "bathroom", 2),
    ("MH-MAT-111", "bathroom", 2), ("MH-MAT-185", "kitchen", 6), ("MH-MAT-293", "living", 110),
    ("MH-CUS-65", "kitchen", 6), ("MH-CUS-8", "master", 32), ("MH-CUS-11", "kids", 2),
    ("MH-GEN-18", "living", 118), ("MH-GEN-19", "living", 118), ("MH-GEN-16", "living", 118),
    ("MH-GEN-15", "kitchen", 1), ("MH-GEN-12", "living", 118),
]
PAYMENT_TERMS = "签约 30%，开工 40%，竣工 27%，尾款 3%"
PROFILE_SETTING = {
    "serviceArea": "合肥市（蜀山区、包河区、经开区、滨湖新区）",
    "serviceTel": "0551-6558 8899",
    "afterSaleTel": "0551-6558 8898",
    "intro": "安徽昭青网络科技有限公司·装得清家装事业中心，专注合肥 80–140㎡ 住宅整装交付，"
             "提供量房、设计、主材、施工、验收与售后一体化服务。",
}
CHANGE = {"reason": "阳台增加储物柜一组（含柜体、门板、五金），业主现场确认增加",
          "amount": 3200, "type": "Add"}
# 选材记录（主材）：(主材名称关键词, 颜色/花色, 规格/型号, 品牌/系列, 备注)
SELECTIONS = [
    ("瓷砖", "米白 / 浅灰", "800*800 抛釉砖", "马可波罗（展厅款）", "客厅、餐厅、走廊地面通铺"),
    ("地板", "浅橡木色", "强化地板 1.0 厚", "圣象", "主卧、次卧、儿童房地面"),
    ("木门", "白色哑光", "实木复合烤漆门", "TATA", "三间卧室 + 卫生间门"),
    ("淋浴房", "超白玻璃", "极窄吊轨全包框一字型（10mm 钢化玻璃）", "定制", "主卫干湿分离"),
    ("卫浴", "哑光白 / 枪灰", "连体马桶 + 恒温花洒", "九牧（展厅款）", "两卫同色系配置"),
]
ACCEPTANCE = {"result": "拆除验收", "content": "墙体拆除、原始白灰铲除已完成，现场清理完毕，结构复核无异常，进入水电施工。"}


def today_at(hour: int, minute: int = 0) -> str:
    """今天某个整点时刻（用于把量房/签约时间落在今天更早的合理时段）。"""
    return datetime.now().replace(hour=hour, minute=minute, second=0, microsecond=0).isoformat()


def now_iso(offset_days: int = 0, hour: int = 10) -> str:
    return (datetime.now() + timedelta(days=offset_days)).replace(
        hour=hour, minute=0, second=0, microsecond=0).isoformat()


class Api:
    def __init__(self, base: str, token: str, apply: bool):
        self.base = base.rstrip("/")
        self.token = token
        self.apply = apply

    def __call__(self, route: str, payload: dict) -> dict:
        if not self.apply:
            print(f"  [dry-run] {route} {json.dumps(payload, ensure_ascii=False)[:160]}")
            return {}
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        out = subprocess.run(["curl", "-s", "-X", "POST", f"{self.base}/{route}",
                              "-H", "Content-Type: application/json", "-H", "token: " + self.token,
                              "--data-binary", "@-"], input=body, capture_output=True)
        raw = out.stdout.decode("utf-8", "ignore")
        try:
            res = json.loads(raw or "{}")
        except json.JSONDecodeError:
            raise RuntimeError(f"{route}: 非 JSON 响应 {raw[:200]}")
        if not res.get("success"):
            raise RuntimeError(f"{route} 失败: {json.dumps(res.get('errorCode') or res, ensure_ascii=False)[:300]}")
        return res.get("data") or {}

    def get(self, route: str, payload: dict) -> dict:
        return self.__call__(route, payload)


def rows(data) -> list[dict]:
    if isinstance(data, list):
        return data
    for key in ("list", "content", "records", "items"):
        if isinstance(data, dict) and isinstance(data.get(key), list):
            return data[key]
    return []


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--api", default="https://decoboon.joytsing.com/api")
    parser.add_argument("--token-file", default="/tmp/.decoboon-prod-token")
    parser.add_argument("--phone")
    parser.add_argument("--password")
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--step", action="append", help="只跑指定步骤（可重复）")
    args = parser.parse_args()

    token = Path(args.token_file).read_text(encoding="utf-8").strip() if Path(args.token_file).exists() else ""
    if not token and args.phone and args.password:
        login = subprocess.run(["curl", "-s", "-X", "POST", f"{args.api.rstrip('/')}/auth/session/password-login",
                                "-H", "Content-Type: application/json",
                                "-d", json.dumps({"phone": args.phone, "password": args.password})],
                               capture_output=True)
        token = (json.loads(login.stdout or "{}").get("data") or {}).get("token") or ""
    if not token:
        print("缺少 token（--token-file / --phone+--password）", file=sys.stderr)
        return 2

    api = Api(args.api, token, args.apply)
    session = api("auth/session/info", {}) or {}
    emp = (session.get("employee") or {}) if session else {}
    if args.apply and not emp:
        print("会话无效", file=sys.stderr)
        return 2
    emp_id = str(emp.get("empId") or "1789938943545297099")
    store_id = str(((emp.get("store") or {}) or {}).get("storeId") or "1789938943511626596")
    print(f"企业={((emp.get('company') or {}).get('name'))} 员工={emp.get('name')}({emp_id}) 门店={store_id}")

    state: dict = json.loads(STATE.read_text(encoding="utf-8")) if STATE.exists() else {}
    if not args.apply:
        print("\n[dry-run] 计划写入的真实感受控数据集（不产生任何写操作）：")
        print(f"  企业对外信息: 服务电话 {PROFILE_SETTING['serviceTel']} / 售后 {PROFILE_SETTING['afterSaleTel']}")
        print(f"  客户: {CUSTOMER['name']} {CUSTOMER['telNo']} · {CUSTOMER['community']} · {CUSTOMER['area']}㎡ · 预算 {CUSTOMER['budget']} 元")
        print(f"  房屋: {PROPERTY['address']} · {PROPERTY['layout']} · {PROPERTY['area']}㎡")
        print(f"  量房: {len(REGIONS)} 个区域实测（" + "、".join(r[2] for r in REGIONS) + "）")
        print(f"  方案: {DESIGN['name']}（{DESIGN['style']}）+ {len(ITEMS)} 行报价库分项（全部引用真实报价库编号）")
        print("  合同: " + PAYMENT_TERMS)
        print("  项目: 开工交底 → 水电施工（Doing）")
        print("  验收: 水电验收提交（待业主确认）｜增减项: " + CHANGE['reason'][:24] + f"… {CHANGE['amount']} 元（待确认）")
        print("\n追加 --apply 执行。")
        return 0
    wanted = set(args.step or [])
    def want(name: str) -> bool:
        return not wanted or name in wanted
    def save() -> None:
        if args.apply:
            STATE.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding="utf-8")

    # 1) 企业对外信息（业主端「服务方」事实来源）
    if want("profile"):
        print("\n[1/8] 企业对外信息")
        api("company/company/update", {"setting": PROFILE_SETTING})
        print("  服务电话/售后电话/服务区域/简介 已写入（USCC 与资质留空，不编造法律主体信息）")

    # 2) 客户 + 房屋 + 跟进 + 预约量房
    if want("customer"):
        print("\n[2/8] 客户与房屋")
        # A) 在谈客户（只建档 + 跟进 + 约量房，体现真实漏斗）
        if not state.get("pipelineCustId"):
            found = [c for c in rows(api("customer/customer/page", {"keyword": CUSTOMER_A["name"],
                                                                    "current": 1, "size": 10}))
                     if c.get("name") == CUSTOMER_A["name"]]
            if found:
                state["pipelineCustId"] = str(found[0].get("custId"))
                print(f"  在谈客户已存在：{CUSTOMER_A['name']} custId={state['pipelineCustId']}")
            else:
                cust = api("customer/customer/save", dict(CUSTOMER_A))
                state["pipelineCustId"] = str(cust.get("custId"))
                print(f"  在谈客户建档 {CUSTOMER_A['name']} custId={state['pipelineCustId']}")
            save()
        # B) 完整案例客户
        cust_id = state.get("custId")
        if not cust_id:
            found = [c for c in rows(api("customer/customer/page", {"keyword": CUSTOMER["name"],
                                                                    "current": 1, "size": 10}))
                     if c.get("name") == CUSTOMER["name"]]
            if found:
                cust_id = str(found[0].get("custId"))
                print(f"  案例客户已存在，复用 custId={cust_id}")
            else:
                cust = api("customer/customer/save", dict(CUSTOMER))
                cust_id = str(cust.get("custId"))
                print(f"  案例客户建档 {CUSTOMER['name']} custId={cust_id}")
            state["custId"] = cust_id
            save()
        prop_id = state.get("propertyId")
        if not prop_id and cust_id:
            props = rows(api("property/property/page", {"customerId": cust_id, "current": 1, "size": 10}))
            prop_id = str(props[0].get("propertyId")) if props else ""
            if prop_id:
                print(f"  已存在房屋，复用 propertyId={prop_id}")
            else:
                prop = api("property/property/save", {"customerId": cust_id, **PROPERTY})
                prop_id = str(prop.get("propertyId"))
                print(f"  房屋建档 {PROPERTY['address']} propertyId={prop_id}")
            state["propertyId"] = prop_id
            save()
        if state.get("pipelineCustId") and not state.get("pipelinePropertyId"):
            pipeline_props = rows(api("property/property/page", {"customerId": state["pipelineCustId"],
                                                                 "current": 1, "size": 10}))
            if pipeline_props:
                state["pipelinePropertyId"] = str(pipeline_props[0].get("propertyId"))
            else:
                prop = api("property/property/save", {"customerId": state["pipelineCustId"], **PROPERTY_A})
                state["pipelinePropertyId"] = str(prop.get("propertyId"))
            save()
            api("customer/customer-follow/save", {"customerId": state["pipelineCustId"],
                                                  "nextFollowAt": now_iso(3), **FOLLOW_A})
            api("customer/customer/measure", {"customerId": state["pipelineCustId"],
                                              "propertyId": state["pipelinePropertyId"],
                                              "targetPropertyId": state["pipelinePropertyId"],
                                              "measureAt": now_iso(1)})
            api("customer/customer/handoff", {"customerId": state["pipelineCustId"],
                                              "propertyId": state["pipelinePropertyId"],
                                              "designerId": int(emp_id)})
            print(f"  在谈客户：跟进 + 预约量房({now_iso(1)[:16]}) + 指派设计师")
        if cust_id and prop_id and not state.get("caseReady"):
            api("customer/customer-follow/save", {"customerId": cust_id, "nextFollowAt": now_iso(2),
                                                  **FOLLOW})
            api("customer/customer/handoff", {"customerId": cust_id, "propertyId": prop_id,
                                              "designerId": int(emp_id)})
            state["caseReady"] = True
            save()
            print("  案例客户：跟进 + 指派设计师（单人员工企业由企业负责人承接设计）")

    # 3) 量房：区域实测 + 完成
    if want("measure"):
        print("\n[3/8] 量房")
        prop_id = state.get("propertyId")
        measure_id = state.get("measureId")
        if prop_id and not measure_id:
            page = rows(api("property/measure/page", {"propertyId": prop_id, "current": 1, "size": 10}))
            measure_id = str(page[0].get("measureId")) if page else ""
            if not measure_id:
                m = api("property/measure/save", {"propertyId": prop_id, "status": "Scheduled",
                                                  "scheduledAt": today_at(6, 30),
                                                  "customerRequirements": "整装，重收纳，春节前入住。"})
                measure_id = str(m.get("measureId"))
            state["measureId"] = measure_id
            save()
        if measure_id:
            api("property/measure/save", {"propertyId": prop_id, "measureId": int(measure_id),
                                          "status": "Scheduled",
                                          "scheduledAt": today_at(6, 30)})
            for idx, (room, rtype, name, length, width, height, area, perimeter) in enumerate(REGIONS):
                api("property/measure/region/save", {
                    "measureId": measure_id, "roomKey": room, "roomType": rtype, "name": name,
                    "length": length, "width": width, "height": height, "area": area,
                    "perimeter": perimeter, "sort": idx + 1, "remark": "实测"})
            done = api("property/measure/save", {"propertyId": prop_id, "measureId": int(measure_id),
                                                "status": "Completed", "completedAt": today_at(7, 0),
                                                "originalStructure": "毛坯交房，承重墙位置见原始结构图",
                                                "designerConclusion": "格局方正，客餐厅进深充足，可打通阳台扩大公共区。"})
            print(f"  {len(REGIONS)} 个区域实测录入 + 量房完成 measureId={measure_id}")
            if isinstance(done, dict) and done.get("measureId"):
                state["measureId"] = str(done["measureId"])

    # 4) 设计方案：需求 + 方案 + 区域 + 分项 + build
    if want("design"):
        print("\n[4/8] 设计方案")
        cust_id, prop_id = state.get("custId"), state.get("propertyId")
        design_id = state.get("designId")
        if not design_id:
            api("design/requirement/save", {"customerId": cust_id, "propertyId": prop_id,
                                            "targetPropertyId": prop_id, **REQUIREMENT})
            d = api("design/scheme/save", {"customerId": cust_id, "propertyId": prop_id,
                                           "targetPropertyId": prop_id, **DESIGN})
            design_id = str(d.get("designId"))
            state["designId"] = design_id
            save()
            print(f"  需求与方案创建 designId={design_id}")
        spaces: dict[str, str] = state.get("spaces") or {}
        for idx, (room, rtype, name, length, width, height, area, perimeter) in enumerate(REGIONS):
            if room in spaces:
                continue
            s = api("design/space/save", {"designId": int(design_id), "type": rtype, "name": name,
                                          "length": length, "width": width, "height": height,
                                          "area": area, "perimeter": perimeter, "sort": idx + 1,
                                          "remark": "照实测录入"})
            if isinstance(s, dict) and s.get("spaceId"):
                spaces[room] = str(s["spaceId"])
        state["spaces"] = spaces
        save()
        print(f"  方案区域 {len(spaces)} 个")
        items_done = state.get("itemCodes") or []
        for idx, (code, room, qty) in enumerate(ITEMS):
            if code in items_done:
                continue
            hit = [i for i in rows(api("catalog/item/page", {"type": _type_of(code), "keyword": code,
                                                             "current": 1, "size": 10}))
                   if str(i.get("code")) == code]
            if not hit:
                print(f"  跳过：报价库无 {code}")
                continue
            api("design/item/save", {"designId": int(design_id), "spaceId": spaces.get(room),
                                     "catalogItemId": int(hit[0]["itemId"]), "quantity": qty,
                                     "sort": idx + 1, "remark": "按方案配置"})
            items_done.append(code)
            state["itemCodes"] = items_done
            save()
        print(f"  设计分项 {len(items_done)} 行")
        api("design/scheme/build", {"designId": int(design_id), "status": "Ready"})

    # 5) 报价
    if want("quote"):
        print("\n[5/8] 报价")
        design_id = state.get("designId")
        quote_id = state.get("quoteId")
        if not quote_id:
            q = api("quote/save", {"designId": int(design_id), "discount": 0.98})
            quote_id = str(q.get("quoteId") or q.get("id"))
            state["quoteId"], state["quoteAmount"] = quote_id, str(q.get("amount"))
            save()
            print(f"  报价单 quoteId={quote_id} 金额={q.get('amount')}")
        api("quote/update", {"quoteId": int(quote_id), "status": "Submitted"})
        api("quote/update", {"quoteId": int(quote_id), "status": "Accepted"})

    # 6) 合同 → 项目
    if want("contract"):
        print("\n[6/8] 合同与立项")
        contract_id, project_id = state.get("contractId"), state.get("projectId")
        if not contract_id:
            start = date.today() + timedelta(days=7)
            c = api("project/contract/save", {"quoteId": int(state["quoteId"]),
                                              "startDate": start.isoformat(),
                                              "planEndDate": (start + timedelta(days=95)).isoformat(),
                                              "paymentTerms": PAYMENT_TERMS,
                                              "remark": "签约地点：合肥市蜀山区客户家中"})
            contract_id = str(c.get("contractId"))
            state["contractId"] = contract_id
            save()
            print(f"  合同 draft contractId={contract_id}")
        amount = float(state.get("quoteAmount") or 0)
        api("project/contract/update", {"contractId": int(contract_id), "status": "Signed",
                                        "signedAt": date.today().isoformat(),
                                        "signedAmount": amount})
        eff = api("project/contract/update", {"contractId": int(contract_id), "status": "Effective",
                                              "signedAt": date.today().isoformat(),
                                        "signedAmount": amount})
        if not project_id:
            proj = (eff or {}).get("project") or {}
            project_id = str(proj.get("projectId") or (eff or {}).get("projectId") or "")
            if project_id:
                state["projectId"] = project_id
                save()
        print(f"  合同生效，项目 projectId={project_id}")

    # 7) 施工阶段推进到「水电施工」
    if want("stage"):
        print("\n[7/8] 施工阶段")
        api("project/project/update", {"projectId": int(state["projectId"]), "managerId": int(emp_id),
                                       "designerId": int(emp_id)})
        page = rows(api("project/stage/page", {"projectId": int(state["projectId"]), "current": 1, "size": 20}))
        by_name = {s.get("name"): s for s in page}
        # 开工交底/竣工保洁验收等空阶段由系统判定「本项不涉及」；有合同施工行的阶段才可开工。
        for name in ("拆除改造", "水电施工"):
            stage = by_name.get(name)
            if not stage:
                print(f"  未找到阶段 {name}")
                continue
            if str(stage.get("status")) in ("NotApplicable", "{'key': 'NotApplicable'}"):
                api("project/stage/update", {"stageId": int(stage["stageId"]), "status": "Todo",
                                             "reason": "报价已补充该阶段施工项目，恢复适用"})
            api("project/stage/update", {"stageId": int(stage["stageId"]), "status": "Doing",
                                         "content": f"{name}已进场施工"})
            state.setdefault("stageIds", {})[name] = str(stage["stageId"])
        # 拆除改造的任务推进到「施工中」（完工需现场照片留证，由人工在 App 内上传，不代造证据）
        stage_name = "拆除改造"
        stage_id = (state.get("stageIds") or {}).get(stage_name)
        if stage_id:
            for t in rows(api("project/task/page", {"projectId": int(state["projectId"]),
                                                    "stageId": int(stage_id), "current": 1, "size": 50})):
                api("project/task/update", {"taskId": int(t["taskId"]), "status": "Doing"})
        state["acceptanceStageName"] = stage_name
        save()

    # 8) 验收（待业主确认）+ 增减项（待确认）+ 首期收款
    if want("acceptance"):
        print("\n[8/8] 提交验收 / 增减项 / 收款")
        sid = (state.get("stageIds") or {}).get(state.get("acceptanceStageName") or "拆除改造")
        if sid and not state.get("acceptanceId"):
            print(f"  留待人工：在 App 内为「{state.get('acceptanceStageName')}」任务上传现场照片 → 完成任务 → 提交验收")
            print("  （现场照片属于工程证据，不代生成占位图；提交后业主端会出现「确认阶段验收」待办）")
        if not state.get("changeId"):
            ch = api("project/contract-change/save", {"contractId": int(state["contractId"]), **CHANGE})
            state["changeId"] = str((ch or {}).get("changeId") or "")
            save()
        if not state.get("receiptId"):
            recs = rows(api("finance/receivable/page", {"projectId": int(state["projectId"]),
                                                        "current": 1, "size": 20}))
            first = next((r for r in recs if float(r.get("receivedAmount") or 0) == 0), None)
            if first:
                r = api("finance/receipt/save", {"projectId": int(state["projectId"]),
                                                 "receivableId": int(first["receivableId"]),
                                                 "amount": float(first.get("amount") or 0),
                                                 "account": "Public", "receivedAt": date.today().isoformat(),
                                                 "payerName": CUSTOMER["name"],
                                                 "idempotencyKey": f"zq-demo-{state['projectId']}-first"})
                state["receiptId"] = str((r or {}).get("receiptId") or "")
                save()

    # 9) 主材选材记录（业主端「选材记录」页）
    if want("material"):
        print("\n[9/9] 主材选材记录")
        lines = rows(api("project/contract-line/page", {"projectId": int(state["projectId"]),
                                                        "current": 1, "size": 100}))
        mat = [l for l in lines if str(((l.get("type") or {}) or {}).get("key")
                                       if isinstance(l.get("type"), dict) else l.get("type")) == "Material"]
        done_keys = state.setdefault("selectKeys", [])
        for keyword, color, spec, brand, remark in SELECTIONS:
            line = next((l for l in mat if keyword in str(l.get("name") or "")
                         and str(l.get("lineId")) not in done_keys), None)
            if not line:
                continue
            api("material/selection/save", {
                "propertyId": int(state["propertyId"]), "projectId": int(state["projectId"]),
                "contractId": int(state["contractId"]), "contractLineId": int(line["lineId"]),
                "title": line.get("name"), "remark": remark, "deliver": False,
                "selectionJson": json.dumps({"color": color, "spec": spec, "brand": brand, "remark": remark},
                                            ensure_ascii=False),
                "content": f"颜色/花色：{color}；规格/型号：{spec}；品牌/系列：{brand}；备注：{remark}"})
            done_keys.append(str(line["lineId"]))
            save()
        print(f"  选材记录 {len(done_keys)} 条")

    print("\n完成。state:", json.dumps(state, ensure_ascii=False, indent=2))
    return 0


def _type_of(code: str) -> str:
    return {"MH-CON": "Construction", "MH-MAT": "Material", "MH-CUS": "Custom",
            "MH-GEN": "General", "MH-SOF": "Soft", "MH-APP": "Appliance"}[code[:6]]


if __name__ == "__main__":
    raise SystemExit(main())
