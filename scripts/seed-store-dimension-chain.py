#!/usr/bin/env python3
"""多门店数据集 · 阶段 2：业务链路造数 + SQL 差异场景 + A1–A10 断言（仅 dev）。

沿用阶段 1 的租户骨架（企业 M：A/B/C 三店；企业 S：单店），本阶段：
  chain      : 走**真实 HTTP 链路**造客户→房屋→量房→设计→报价→合同→项目→手工应收/收款/应付/付款/台账/结算
  unassigned : SQL 造「未归属池」（store_id 为空）——**模拟历史数据**
  misalign   : SQL 造「房屋门店 ≠ 客户门店」——**模拟历史数据**
  assert     : A1–A10 断言（SQL + HTTP 只读）
  snapshot   : 企业×门店×对象条数/金额快照
用法：/opt/anaconda3/bin/python3 scripts/seed-store-dimension-chain.py chain --stores A --customers 1
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
import time
from datetime import date, datetime, timedelta
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
MANIFEST = REPO / "docs/operations/data-changes/2026-10-13-门店维度造数manifest.json"
CHAIN_STATE = REPO / "docs/operations/data-changes/2026-10-13-门店维度造数-chain-state.json"
BASE = "http://127.0.0.1:8090/api"
PASSWORD = "Zdq123456"
DB = ["mysql", "-h127.0.0.1", "-P3306", "-uroot", "-pmysqladmin", "decoboon", "-N", "-B"]
COM_M = 999200000000000001
COM_S = 999300000000000001
STORE_M_A = 999200000000000011
STORE_M_B = 999200000000000012
STORE_M_C = 999200000000000013
TABLES = ["cust_customer", "db_property", "db_measure", "quote", "db_project_contract", "db_project",
          "db_finance_receivable", "db_finance_receipt", "db_finance_payable", "db_finance_payment",
          "db_finance_ledger", "db_finance_settlement"]
NAMES = ["王芳", "李娜", "张敏", "刘洋", "陈晨", "杨帆", "赵磊", "黄静", "周涛", "吴桐"]
COMMUNITIES = ["滨湖世纪城", "香樟花园", "望江府", "锦绣园", "翡翠湖畔", "万象公馆"]
LAYOUTS = ["三室两厅一厨两卫", "三室两厅一厨一卫", "四室两厅两卫", "两室两厅一厨一卫"]


def sql(stmt: str) -> list[list[str]]:
    out = subprocess.run(DB + ["-e", stmt], capture_output=True, text=True)
    if out.returncode != 0:
        raise RuntimeError("SQL 失败: %s\n%s" % (stmt[:200], out.stderr))
    return [l.split("\t") for l in out.stdout.splitlines() if l]


def api(token: str, route: str, payload: dict) -> dict:
    body = json.dumps(payload, ensure_ascii=False)
    out = subprocess.run(["curl", "-s", "-X", "POST", "%s/%s" % (BASE, route),
                          "-H", "Content-Type: application/json", "-H", "token: " + token,
                          "-d", body], capture_output=True, text=True)
    try:
        res = json.loads(out.stdout or "{}")
    except json.JSONDecodeError:
        raise RuntimeError("非 JSON 响应 %s: %s" % (route, out.stdout[:200]))
    if not res.get("success"):
        raise RuntimeError("接口失败 %s: %s" % (route, json.dumps(res, ensure_ascii=False)[:300]))
    return res.get("data") or {}


def login(tel: str) -> str:
    body = json.dumps({"phone": tel, "password": PASSWORD})
    out = subprocess.run(["curl", "-s", "-X", "POST", BASE + "/auth/session/password-login",
                          "-H", "Content-Type: application/json", "-d", body], capture_output=True, text=True)
    res = json.loads(out.stdout or "{}")
    if not res.get("success"):
        raise RuntimeError("登录失败 %s: %s" % (tel, out.stdout[:200]))
    return res["data"]["token"]


def cp(ctx, key: str, rec: dict) -> None:
    """checkpoint：对象创建后立即写 state（支持中断续造，避免重试重复造数）。"""
    ctx.state[key] = rec
    ctx.save()


class Ctx:
    def __init__(self, manifest: dict):
        self.manifest = manifest
        self.tokens: dict[str, str] = {}
        self.state = json.loads(CHAIN_STATE.read_text(encoding="utf-8")) if CHAIN_STATE.exists() else {}

    def save(self):
        CHAIN_STATE.write_text(json.dumps(self.state, ensure_ascii=False, indent=2), encoding="utf-8")

    def employees(self, com: int) -> list[dict]:
        return self.manifest["companies"][str(com)]["employees"]

    def stores(self, com: int) -> list[dict]:
        return self.manifest["companies"][str(com)]["stores"]

    def token(self, com: int, role: str, store_id=None) -> str:
        key = "%s-%s-%s" % (com, role, store_id)
        if key not in self.tokens:
            cands = [e for e in self.employees(com) if e["role"] == role
                     and (store_id is None or e["storeId"] == store_id)]
            assert cands, "缺少员工 role=%s store=%s com=%s" % (role, store_id, com)
            self.tokens[key] = login(cands[0]["tel"])
        return self.tokens[key]


CATALOG_ITEMS = [
    ("Construction", "拆除", "拆除--墙顶面白灰/腻子粉铲除", "Area", "ManualOnly", 0, 6.8, 4.5),
    ("Construction", "水电", "水电--强电布线（含开槽）", "Area", "ManualOnly", 0, 38.0, 25.0),
    ("Construction", "泥木", "泥木--墙地砖铺贴（含辅料）", "Area", "MaterialAndManual", 45.0, 32.0, 55.0),
    ("Material", "瓷砖", "主材--抛釉砖 800x800", "Area", "MaterialOnly", 88.0, 0, 62.0),
    ("Material", "地板", "主材--实木复合地板", "Area", "MaterialOnly", 168.0, 0, 120.0),
    ("General", "综合", "综合--全屋保洁", "House", "FixedTotal", 0, 0, 600.0),
]


def seed_catalog(ctx: Ctx, com: int) -> int:
    """为企业建报价目录（造数前置：方案 build 需施工/材料分项）。走真实接口 catalog/item/save。"""
    token = ctx.token(com, "Boss")
    created = 0
    for (itype, category, name, unit, pricing, material, manual, cost) in CATALOG_ITEMS:
        payload = {"type": itype, "category": category, "name": name, "unit": unit,
                   "pricingMode": pricing, "materialPrice": material, "manualPrice": manual, "costPrice": cost}
        if pricing == "FixedTotal":
            payload["fixedTotalPrice"] = 600.0
        try:
            api(token, "catalog/item/save", payload)
            created += 1
        except RuntimeError as e:
            print("catalog skip:", name, str(e)[:120])
    return created


def create_customer(ctx: Ctx, com: int, store_id, idx: int) -> str:
    t = ctx.token(com, "Marketer", store_id)
    data = api(t, "customer/customer/save", {
        "name": "%s（造数%03d）" % (NAMES[idx % len(NAMES)], idx),
        "telNo": "137%08d" % (idx % 100000000),
        "community": COMMUNITIES[idx % len(COMMUNITIES)],
        "addr": "%s%d栋%d01" % (COMMUNITIES[idx % len(COMMUNITIES)], idx % 12 + 1, idx % 18 + 1),
        "area": 88 + (idx % 6) * 12, "budget": 18 + (idx % 8) * 3,
        "gender": "F" if idx % 3 == 0 else "M",
        "source": ["Lead", "Referral", "WalkIn", "Video"][idx % 4],
    })
    return str(data["custId"])


def create_property(ctx: Ctx, com: int, store_id, customer_id: str, idx: int) -> str:
    t = ctx.token(com, "Marketer", store_id)
    data = api(t, "property/property/save", {
        "customerId": customer_id, "community": COMMUNITIES[idx % len(COMMUNITIES)],
        "address": "%s%d栋%d02" % (COMMUNITIES[idx % len(COMMUNITIES)], idx % 10 + 1, idx % 20 + 1),
        "building": "%d栋" % (idx % 10 + 1), "roomNo": "%d02" % (idx % 20 + 1),
        "area": 90 + (idx % 7) * 10, "layout": LAYOUTS[idx % len(LAYOUTS)], "remark": "造数房屋",
    })
    return str(data["propertyId"])


def chain_one(ctx: Ctx, com: int, store: dict, idx: int, deep: int, key: str) -> dict:
    """按 idx 造一条客户；deep 越大推进链路越深（1=客户+房屋，2=+设计+报价，3=+合同+项目+应收/收款）。"""
    store_id = store["storeId"]
    cust = create_customer(ctx, com, store_id, idx)
    prop = create_property(ctx, com, store_id, cust, idx)
    rec = {"customerId": cust, "propertyId": prop, "storeId": store_id}
    cp(ctx, key, rec)  # 立即落盘：客户/房屋已建
    if deep < 2:
        return rec
    # 链路前置（真实业务顺序）：跟进 → 预约量房 → 指派承接设计 → 完成量房
    api(ctx.token(com, "Marketer", store_id), "customer/customer-follow/save", {
        "customerId": cust, "way": ["Phone", "Wechat", "Visit"][idx % 3],
        "content": "已沟通装修预算、入住时间与初步风格偏好，客户同意推进量房。",
        "nextFollowAt": (date.today() + timedelta(days=2 + idx % 5)).isoformat()})
    measure_at = (datetime.now() + timedelta(days=1 + idx % 6)).replace(microsecond=0)
    api(ctx.token(com, "Marketer", store_id), "customer/customer/measure", {
        "customerId": cust, "propertyId": prop, "targetPropertyId": prop,
        "measureAt": measure_at.isoformat()})
    designer = [e for e in ctx.employees(com) if e["role"] == "Designer" and e["storeId"] == store_id][0]
    api(ctx.token(com, "StoreManager", store_id), "customer/customer/handoff", {
        "customerId": cust, "propertyId": prop, "designerId": designer["empId"]})
    d = ctx.token(com, "Designer", store_id)
    api(d, "property/measure/save", {
        "propertyId": prop,
        "measureTime": (measure_at + timedelta(hours=2)).isoformat(),  # 完成时间必须晚于预约时间
        "status": "Completed", "sitePhotos": ["/demo/seed/measure/site.jpg"]})
    api(d, "design/requirement/save", {
        "customerId": cust, "propertyId": prop, "targetPropertyId": prop, "type": "LivingRoom",
        "spaceName": "客餐厅", "kind": "Demand", "category": "Function", "priority": "High",
        "title": "客餐厅通透与收纳需求", "detail": "希望采光通透并增加收纳。", "response": "开放布局 + 整墙收纳。",
        "source": "量房沟通"})
    scheme = api(d, "design/scheme/save", {
        "customerId": cust, "propertyId": prop, "targetPropertyId": prop,
        "name": "造数方案 %03d" % idx, "style": ["现代简约", "奶油风", "原木风", "新中式"][idx % 4],
        "layout": LAYOUTS[idx % len(LAYOUTS)], "description": "造数设计方案。", "area": 90 + (idx % 7) * 10})
    design_id = str(scheme["designId"])
    # 方案 build 需先有施工/材料分项 → 从企业目录取可用户型项建分项。
    # 必须**同时含材料项**：合同生效后按合同行类型生成材料计划（`MaterialSourceType.Selection`），
    # 材料计划是「采购 → 下单 → 到货 → 自动应付」的前置（A4 自动应付侧的证据来源）。
    by_type = {}
    for itype in ("Construction", "Material", "General", "Custom", "Soft", "Appliance"):
        page = api(d, "catalog/item/page", {"type": itype, "current": 1, "size": 50})
        rows = page.get("list") or (page.get("content") or [])
        by_type[itype] = [i for i in rows
                          if float(i.get("materialPrice") or 0) + float(i.get("manualPrice") or 0)
                          + float(i.get("fixedTotalPrice") or 0) > 0]
    assert by_type.get("Material"), "企业目录缺少材料项：请先跑 `catalog` 阶段建目录项"
    usable = by_type.get("Construction", [])[:2] + by_type.get("Material", [])[:1]
    for k, item in enumerate(usable):
        api(d, "design/item/save", {"designId": design_id, "catalogItemId": str(item["itemId"]),
                                    "quantity": 10 + k, "sort": k, "remark": "造数分项"})
    api(d, "design/scheme/build", {"designId": design_id, "status": "Ready"})
    q = api(d, "quote/save", {"designId": design_id, "discount": 0.95})
    quote_id = str(q.get("quoteId") or q.get("id"))
    rec.update({"designId": design_id, "quoteId": quote_id, "amount": q.get("amount")})
    cp(ctx, key, rec)
    if deep < 3:
        return rec
    # 报价状态机：设计提交 → 企业负责人接受（顺序不可颠倒）
    api(d, "quote/update", {"quoteId": quote_id, "status": "Submitted"})
    api(ctx.token(com, "Boss"), "quote/update", {"quoteId": quote_id, "status": "Accepted"})
    start = date.today() + timedelta(days=5 + idx % 10)
    c = api(ctx.token(com, "Boss"), "project/contract/save", {
        "quoteId": quote_id, "startDate": start.isoformat(),
        "planEndDate": (start + timedelta(days=95)).isoformat(),
        "paymentTerms": "签约30%，水电验收30%，泥木验收30%，竣工10%", "remark": "造数合同 %03d" % idx})
    contract_id = str(c["contractId"])
    # 合同状态机：save=Draft → 设计提交 PendingSign → 企业负责人确认 Effective（此后生成项目与应收）
    # update 仅支持 Signed / Effective（PendingSign 不被接受）
    api(ctx.token(com, "Boss"), "project/contract/update", {
        "contractId": contract_id, "status": "Signed",
        "signedAt": (date.today() - timedelta(days=2)).isoformat(),
        "signedAmount": float(q.get("amount") or 100000)})
    eff = api(ctx.token(com, "Boss"), "project/contract/update", {"contractId": contract_id, "status": "Effective",
        "signedAt": (date.today() - timedelta(days=2)).isoformat(),
        "signedAmount": float(q.get("amount") or 100000)})
    proj = eff.get("project") or {}
    project_id = str(proj.get("projectId") or proj.get("id") or eff.get("projectId") or "")
    assert project_id, "签约生效未返回项目：%s" % json.dumps(eff, ensure_ascii=False)[:200]
    rec.update({"contractId": contract_id, "projectId": project_id})
    cp(ctx, key, rec)
    # 手工应收（A3 双路径：另有合同节点自动生成）
    api(ctx.token(com, "Finance", store_id), "finance/receivable/save", {
        "projectId": project_id, "name": "手工补充应收", "stageName": "补充款", "type": "Progress",
        "amount": 5000, "dueDate": (date.today() + timedelta(days=30)).isoformat()})
    # 收款
    # 自动应收（合同付款节点）：生效后应已生成，取其一条作为收款对象（同时是 A3 双路径的「自动」一侧）
    rec_sel = api(ctx.token(com, "Finance", store_id), "finance/receivable/page",
                  {"projectId": project_id, "current": 1, "size": 50})
    recs = rec_sel.get("list") or rec_sel.get("content") or []
    rec["autoReceivables"] = len(recs)
    cp(ctx, key, rec)
    if recs:
        rec["autoReceivableId"] = str(recs[0].get("receivableId"))
    # 收款金额不得超过应收余额 → 取该应收金额与 3000 的较小值
    balance = float((recs[0].get("amount") if recs else 0) or 0)
    rec["receivableAmount"] = balance
    if balance > 0:
        api(ctx.token(com, "Finance", store_id), "finance/receipt/save", {
            "projectId": project_id, "receivableId": rec.get("autoReceivableId"),
            "amount": min(3000.0, balance), "account": "Public", "receivedAt": date.today().isoformat(),
            "payerName": "造数业主", "idempotencyKey": "seed-%s-%s" % (project_id, idx)})
    # 手工应付（A4 多路径：另有采购/到货生成）
    api(ctx.token(com, "Finance", store_id), "finance/payable/save", {
        "projectId": project_id, "name": "手工应付", "type": "Material", "amount": 2000,
        "dueDate": (date.today() + timedelta(days=20)).isoformat(), "payeeName": "造数供应商"})
    # 台账（项目内）+ 非项目手工流水
    api(ctx.token(com, "Finance", store_id), "finance/ledger/save", {
        "projectId": project_id, "direction": "Expense", "purpose": "ProjectIncidentalCost", "category": "零星其他支出",
        "amount": 800, "account": "Public", "occurredAt": date.today().isoformat(),
        "counterparty": "造数商户", "summary": "造数项目零星其他支出"})
    api(ctx.token(com, "Finance", store_id), "finance/ledger/save", {
        "direction": "Expense", "purpose": "CompanyExpense", "category": "企业其他费用",
        "amount": 300, "account": "Public", "occurredAt": date.today().isoformat(),
        "counterparty": "造数商户", "summary": "造数企业其他费用"})
    material_chain(ctx, com, store, project_id, prop, contract_id, rec)
    return rec


def enum_of(value) -> str:
    """枚举字段归一：响应里是 {key,value,name} 对象，SQL/旧版可能是字符串。"""
    if isinstance(value, dict):
        return str(value.get("key") or value.get("value") or "")
    return "" if value is None else str(value)


def material_chain(ctx: Ctx, com: int, store: dict, project_id: str, property_id: str,
                   contract_id: str, rec: dict) -> None:
    """材料链（A4 自动应付侧 + A3 材料选材）：选材确认 → 采购 → 下单 → 到货 → 自动应付 → 付款 → 结算。

    全部走真实接口与真实状态机（不 SQL 直插）：
    `material/selection/save{deliver:true}`（Planned→ReadyToOrder）→ `project/purchase/save`
    → `project/purchase/order`（生成自动应付）→ `project/goods-receipt/save`（回填到货日/到期日）
    → `finance/payment/save`（对自动应付付款）→ `finance/settlement/save`（结算单）。
    """
    store_id = store["storeId"]
    sm = ctx.token(com, "StoreManager", store_id)
    fin = ctx.token(com, "Finance", store_id)
    lines = api(sm, "project/contract-line/page", {"projectId": project_id, "current": 1, "size": 100})
    ll = lines.get("list") or lines.get("content") or []
    mat_lines = [l for l in ll if enum_of(l.get("type")) == "Material"]
    plans = api(sm, "project/material-plan/page", {"projectId": project_id, "current": 1, "size": 100})
    plist = plans.get("list") or plans.get("content") or []
    rec["materialLines"] = len(mat_lines)
    rec["materialPlans"] = len(plist)
    if not mat_lines or not plist:
        return
    line, plan = mat_lines[0], plist[0]
    api(sm, "material/selection/save", {
        "propertyId": property_id, "contractId": contract_id, "projectId": project_id,
        "contractLineId": str(line["lineId"]), "title": "造数主材选材",
        "content": "业主已确认主材品牌、系列与花色，交付财务下单。", "deliver": True})
    qty = min(5.0, float(plan.get("netQuantity") or plan.get("quantity") or 0) or 0)
    if qty <= 0:
        return
    po = api(sm, "project/purchase/save", {
        "materialPlanId": str(plan["planId"]), "quantity": qty, "unitPrice": 100,
        "deliveryMode": "Direct", "supplierName": "造数供应商", "expectedAt": (date.today() + timedelta(days=3)).isoformat()})
    purchase_id = str(po.get("purchaseId") or po.get("id") or "")
    assert purchase_id, "采购单未返回主键：%s" % json.dumps(po, ensure_ascii=False)[:200]
    api(sm, "project/purchase/order", {"purchaseId": purchase_id})          # → 已下单 + 自动应付
    api(sm, "project/goods-receipt/save", {"purchaseId": purchase_id, "quantity": qty})  # 到货
    rec["purchaseId"] = purchase_id
    rec["purchaseNo"] = po.get("no")
    pays = api(fin, "finance/payable/page", {"projectId": project_id, "current": 1, "size": 100})
    pl = pays.get("list") or pays.get("content") or []
    auto = [x for x in pl if str(x.get("sourceNo") or "") == str(po.get("no") or "")]
    rec["autoPayableId"] = str(auto[0]["payableId"]) if auto else None
    rec["autoPayableStore"] = (auto[0].get("project") or {}).get("storeId") if auto else None
    if auto:
        amount = min(500.0, float(auto[0].get("amount") or 0) or 0)
        if amount > 0:
            api(fin, "finance/payment/save", {
                "payableId": rec["autoPayableId"], "amount": amount, "account": "Public",
                "paidAt": date.today().isoformat(), "payeeName": "造数供应商",
                "idempotencyKey": "seed-pay-%s" % purchase_id})
    api(fin, "finance/settlement/save", {
        "projectId": project_id, "deductionAmount": 100, "rewardAmount": 50, "retentionRate": 3,
        "retentionReleaseAt": (date.today() + timedelta(days=180)).isoformat(),
        "remark": "造数结算单"})


# ---- 未归属池 / 历史错位（**模拟历史数据**：按 com_id 直插 SQL，规格 §2-2 允许） ----
# 未归属池：按**依赖顺序**克隆并重映射父键（报价→合同→项目→应收→付款→台账）
CLONE_CHAIN = [
    ("db_property", "property_id", 9992000000000900, None),
    ("quote", "quote_id", 9992000000000902, None),
    ("db_project_contract", "contract_id", 9992000000000903, {"quote_id": "quote_id"}),
    ("db_project", "project_id", 9992000000000904, {"contract_id": "contract_id"}),
    ("db_finance_receivable", "receivable_id", 9992000000000905, {"project_id": "project_id"}),
    ("db_finance_payment", "payment_id", 9992000000000906, None),
    ("db_finance_ledger", "ledger_id", 9992000000000907, {"project_id": "project_id"}),
]
CLONE_SPEC = [(t, i, b) for (t, i, b, _r) in CLONE_CHAIN]


def unique_cols(table: str) -> list[str]:
    """**单列**唯一索引列（非主键）：克隆时必须改成造数值，否则撞唯一键。

    ⚠️ 只返回「唯一索引仅由这一列构成」的列：复合唯一索引（如 `db_project.uk_project_company_property(com_id, property_id)`、
    `db_finance_payment.uk_fin_payment_company_idempotency(com_id, idempotency_key)`）的列**不能**自动改值——
    早前版本用 `group by column_name, index_name` 导致复合索引的每一列都被判为「单列唯一」，
    于是 `com_id` 被覆盖成雪花造数值 → 克隆时 `ERROR 1452 FOREIGN KEY (com_id)` 失败（即「链式 FK 问题」的真实根因）。
    """
    return [r[0] for r in sql(
        "select s.column_name from information_schema.statistics s where s.table_schema='decoboon' "
        "and s.table_name='%s' and s.non_unique=0 and s.index_name<>'PRIMARY' "
        "and (select count(*) from information_schema.statistics s2 where s2.table_schema=s.table_schema "
        "and s2.table_name=s.table_name and s2.index_name=s.index_name)=1 "
        "group by s.column_name" % table)]


def is_text(table: str, col: str) -> bool:
    rows = sql("select data_type from information_schema.columns where table_schema='decoboon' "
               "and table_name='%s' and column_name='%s'" % (table, col))
    return bool(rows) and rows[0][0] not in ("bigint", "int", "smallint", "tinyint", "decimal")


def clone_row(table: str, id_col: str, where: str, new_id: int, overrides: dict) -> None:
    ov = dict(overrides)
    # 唯一列自动改为造数值（模拟历史数据仍须保持库内唯一）
    for col in unique_cols(table):
        if col in ov or col == id_col:
            continue
        ov[col] = "'SEED-%s-%s'" % (table, new_id) if is_text(table, col) else str(new_id)
    cols = [r[0] for r in sql("show columns from " + table)]
    names, values = [], []
    for col in cols:
        names.append(col)
        values.append(str(ov[col]) if col in ov else col)
    sql("insert into %s (%s) select %s from %s where %s limit 1" % (
        table, ", ".join(names), ", ".join(values), table, where))


def _is_text(table: str, col: str) -> bool:
    rows = sql("select data_type from information_schema.columns where table_schema='decoboon' "
               "and table_name='%s' and column_name='%s'" % (table, col))
    return bool(rows) and rows[0][0] not in ("bigint", "int", "smallint", "tinyint", "decimal", "bigint unsigned")


UNASSIGNED_ID_COL = {
    "cust_customer": "cust_id", "db_property": "property_id", "quote": "quote_id",
    "db_project_contract": "contract_id", "db_project": "project_id",
    "db_finance_payable": "payable_id", "db_finance_receivable": "receivable_id",
    "db_finance_payment": "payment_id", "db_finance_ledger": "ledger_id",
}
# 未归属池链（**按 FK 依赖顺序**；`@x` = 引用本次克隆出的对象主键）
UNASSIGNED_CHAIN = [
    ("db_property", 900, {"store_id": "NULL", "customer_id": "@cust"}, ""),
    ("quote", 902, {"store_id": "NULL", "customer_id": "@cust", "property_id": "@db_property"}, ""),
    # 合同↔项目 是**循环引用**：先建合同并把 project_id 置空，项目建好后回填
    ("db_project_contract", 903, {"store_id": "NULL", "customer_id": "@cust", "property_id": "@db_property",
                                  "quote_id": "@quote", "project_id": "NULL"}, ""),
    ("db_project", 904, {"store_id": "NULL", "customer_id": "@cust", "property_id": "@db_property",
                         "quote_id": "@quote", "contract_id": "@db_project_contract"}, ""),
    # 付款.payable_id 为 NOT NULL：必须连带克隆应付并重映射（否则未归属付款会挂到 A 店应付上）
    ("db_finance_payable", 908, {"store_id": "NULL", "property_id": "@db_property",
                                 "project_id": "@db_project"}, " and source_no is null"),
    ("db_finance_receivable", 905, {"store_id": "NULL", "customer_id": "@cust", "property_id": "@db_property",
                                    "project_id": "@db_project", "contract_id": "@db_project_contract"}, ""),
    ("db_finance_payment", 906, {"store_id": "NULL", "project_id": "@db_project",
                                 "payable_id": "@db_finance_payable",
                                 "idempotency_key": "'SEED-UNASSIGNED-1'"}, ""),
    ("db_finance_ledger", 907, {"store_id": "NULL", "project_id": "@db_project"}, ""),
]


def unassigned(com: int) -> str:
    """未归属池（**模拟历史数据**）：客户 3 + 报价/合同/项目/应付/应收/付款/台账 各 1，`store_id = NULL`。

    全部按**依赖顺序**克隆 A 店真实数据并**重映射父键**（含合同↔项目循环引用、付款→应付 NOT NULL 父键），
    只改门店归属与唯一键造数值，不改金额/状态语义。
    """
    store = STORE_M_A
    reserved = {"cust_customer": [com + 901 + i for i in range(3)]}
    for (table, tail, _ov, _extra) in UNASSIGNED_CHAIN:
        reserved.setdefault(table, []).append(com + tail)
    # 清理**上一版未归属池残留**（早期实现把保留 id 写成 16 位 `999200000000090x`，
    # 与 `com_id + 90x` 不同批次，不清理会在库里留下「半条链」并污染 A1/A2 计数）
    for table, legacy in (("db_property", [9992000000000900]), ("quote", [9992000000000902]),
                          ("db_project_contract", [9992000000000903]), ("db_project", [9992000000000904])):
        reserved.setdefault(table, []).extend(legacy)
    stmts = ["set foreign_key_checks=0;"]
    for table, ids in reserved.items():
        stmts.append("delete from %s where %s in (%s);"
                     % (table, UNASSIGNED_ID_COL[table], ", ".join(str(x) for x in ids)))
    stmts.append("set foreign_key_checks=1;")
    out = subprocess.run(DB + ["-e", " ".join(stmts)], capture_output=True, text=True)
    if out.returncode != 0:
        raise RuntimeError("未归属池清理失败: " + out.stderr[:200])

    done = []
    for i in range(3):
        cid = com + 901 + i
        clone_row("cust_customer", "cust_id", "com_id=%s and store_id=%s" % (com, store), cid,
                  {"cust_id": cid, "store_id": "NULL",
                   "name": "'未归属客户%d（模拟历史）'" % (i + 1), "tel_no": "'136%08d'" % (900 + i)})
    done.append("cust_customer×3")
    keys = {"cust": com + 901}
    for (table, tail, overrides, extra) in UNASSIGNED_CHAIN:
        new_id = com + tail
        ov = {UNASSIGNED_ID_COL[table]: new_id}
        for col, value in overrides.items():
            ov[col] = keys[value[1:]] if value.startswith("@") else value
        clone_row(table, UNASSIGNED_ID_COL[table],
                  "com_id=%s and store_id=%s%s" % (com, store, extra), new_id, ov)
        keys[table] = new_id
        done.append(table)
    # 回填合同的项目父键（循环引用收口）
    sql("update db_project_contract set project_id=%s where contract_id=%s"
        % (keys["db_project"], keys["db_project_contract"]))
    return "%s；合同.project_id 回填=%s" % (", ".join(done), keys["db_project"])


def misalign(com: int) -> str:
    """历史错位：把 A 店某房屋的门店改成 B 店（模拟历史数据），客户门店保持 A 店 → 用于 A6。"""
    rows = sql("select property_id from db_property where com_id=%s and store_id=%s order by property_id limit 1"
               % (com, STORE_M_A))
    assert rows, "没有 A 店房屋可错位"
    pid = rows[0][0]
    sql("update db_property set store_id=%s where property_id=%s" % (STORE_M_B, pid))
    return pid


def verify(com: int) -> str:
    """A1/A2/A6 数据级核对。"""
    out = []
    for t in TABLES:
        rows = sql("select ifnull(store_id,-1) sid, count(*) from %s where com_id=%s group by 1 order by 1" % (t, com))
        per = {r[0]: int(r[1]) for r in rows}
        total = sum(per.values())
        unassigned_cnt = per.pop("-1", 0)
        scattered = sum(per.values())
        ok = (scattered + unassigned_cnt == total)
        out.append("A1 %-24s total=%d Σ门店=%d 未归属=%d 恒等式=%s" % (t, total, scattered, unassigned_cnt, "PASS" if ok else "FAIL"))
    # A6：错位房屋
    rows = sql("select p.project_id, p.store_id, c.store_id, pr.store_id from db_project p "
               "join cust_customer c on c.cust_id=p.customer_id join db_property pr on pr.property_id=p.property_id "
               "where p.com_id=%s and pr.store_id<>c.store_id" % com)
    if rows:
        for (pid, pstore, cstore, propstore) in rows:
            out.append("A6 项目=%s 项目门店=%s 客户门店=%s 房屋门店=%s → %s"
                       % (pid, pstore, cstore, propstore, "PASS(项目取客户门店)" if pstore == cstore else "FAIL"))
    else:
        out.append("A6 无「房屋门店≠客户门店」样本")
    return "\n".join(out)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("stage", choices=["catalog", "chain", "material", "snapshot", "assert", "clean",
                                      "unassigned", "misalign", "verify"])
    ap.add_argument("--com", type=int, default=COM_M)
    ap.add_argument("--stores", default="A,B,C")
    ap.add_argument("--customers", type=int, default=1, help="每店客户数（试点用）")
    ap.add_argument("--deep", type=int, default=3)
    args = ap.parse_args()
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    ctx = Ctx(manifest)
    if args.stage == "catalog":
        print("catalog items created:", seed_catalog(ctx, args.com))
    elif args.stage == "chain":
        wanted = set(args.stores.split(","))
        for store in ctx.stores(args.com):
            tag = store["name"].split()[0]
            if tag not in wanted:
                continue
            for i in range(args.customers):
                idx = int(str(store["storeId"])[-2:]) * 100 + i
                key = "%s-%s" % (store["storeId"], i)
                if key in ctx.state:
                    print("skip", key)
                    continue
                rec = chain_one(ctx, args.com, store, idx, args.deep, key)
                print("ok", store["name"], key, "project=", rec.get("projectId"))
    elif args.stage == "clean":
        # 清业务对象（保留租户骨架）：动态枚举「有 com_id 列且非 db_tenant_*」的表，关外键检查后按 com_id 删除。
        # 仅作用于造数 com_id。
        # 目录（catalog_*）是企业**配置数据**，与租户骨架同等保留（否则每次 clean 都要重建报价目录）
        tables = [r[0] for r in sql(
            "select table_name from information_schema.columns where table_schema='decoboon' "
            "and column_name='com_id' and table_name not like 'db_tenant%%' "
            "and table_name not like 'catalog%%' order by table_name")]
        stmts = ["set foreign_key_checks=0;"] + [
            "delete from %s where com_id=%s;" % (t, args.com) for t in tables] + ["set foreign_key_checks=1;"]
        out = subprocess.run(DB + ["-e", " ".join(stmts)], capture_output=True, text=True)
        if out.returncode != 0:
            raise RuntimeError("清理失败: " + out.stderr[:300])
        print("cleaned %d 张业务表 (com=%s)" % (len(tables), args.com))
        if CHAIN_STATE.exists():
            CHAIN_STATE.unlink()
        print("chain-state reset")
    elif args.stage == "material":
        # 对已造链路补「选材 → 采购 → 下单 → 到货 → 自动应付 → 付款 → 结算」（幂等：已完成的跳过）
        done = 0
        for key, rec in list(ctx.state.items()):
            if rec.get("purchaseId") or not rec.get("projectId"):
                continue
            store = next((x for x in ctx.stores(args.com) if int(x["storeId"]) == int(rec["storeId"])), None)
            if store is None:
                continue
            material_chain(ctx, args.com, store, str(rec["projectId"]), str(rec["propertyId"]),
                           str(rec["contractId"]), rec)
            cp(ctx, key, rec)
            done += 1
        print("material chain done:", done)
    elif args.stage == "unassigned":
        print("unassigned created:", unassigned(args.com))
    elif args.stage == "misalign":
        print("misaligned property:", misalign(args.com))
    elif args.stage == "verify":
        print(verify(args.com))
    elif args.stage == "snapshot":
        for com in (COM_M, COM_S):
            print("== com_id=%s ==" % com)
            for t in TABLES:
                rows = sql("select ifnull(store_id,-1), count(*) from %s where com_id=%s group by 1 order by 1" % (t, com))
                for store_id, cnt in rows:
                    print("%s\tstore=%s\tcount=%s" % (t, store_id, cnt))
    return 0


if __name__ == "__main__":
    sys.exit(main())
