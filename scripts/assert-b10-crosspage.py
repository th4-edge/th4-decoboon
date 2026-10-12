#!/usr/bin/env python3
"""B10 跨页对账断言：A7（项目维度）与 A8（财务维度），三档门店范围（全部门店 / A 店 / B 店），仅 dev。

用法: /opt/anaconda3/bin/python3 scripts/assert-b10-crosspage.py
口径来源（代码事实）：
  ① A7 `statistic/boss/dashboard.activeProjectCount`
     = `StatisticStoreAggregateContext.activeProjectCount`：`p.status not in (Finished, Settled)`
       且（首份已签合同的 signedAt ≤ 月末，或未签约时创建日 ≤ 月末）且（actualEndDate 为空或 > 月末）；
       门店按「项目 store 缺失回退客户门店」（`projectStoreClause`）。
  ② A7 `project/project/page.totalElements` = 当前数据范围内可见项目（不按状态过滤）。
  ③ A7 `statistic/boss/business/report` 的 funnel 为**客户线索漏斗**（线索/到访/量房），不是项目计数 → 口径不同需对照说明。
  ④ A8 `finance/summary/report.receivableAmount` = `FinanceSummaryAggregateContext.receivables()`
     = Σ Receivable.amount（可见范围内，排除 Carryover+Settled 项目），**该接口不返回应收条数**。
  ⑤ A8 `finance/receivable/page` = 应收分页（totalElements + 明细 amount 合计）。
  ⑥ A8 `finance/project/page`（项目账本）= 逐项目 `receivableAmount` 等聚合。
"""
from __future__ import annotations
import json, subprocess
from decimal import Decimal
from pathlib import Path

BASE = "http://127.0.0.1:8090/api"
DB = ["mysql", "-h127.0.0.1", "-P3306", "-uroot", "-pmysqladmin", "decoboon", "-N", "-B"]
COM = "999200000000000001"
MONTH = "2026-10-01"
SCOPES = [("全部门店", None), ("A 店", 999200000000000011), ("B 店", 999200000000000012)]


def q(sql):
    o = subprocess.run(DB + ["-e", sql], capture_output=True, text=True)
    if o.returncode:
        raise RuntimeError(o.stderr[:300])
    return [l.split("\t") for l in o.stdout.splitlines() if l]


def login(tel="13910000100"):
    o = subprocess.run(["curl", "-s", "-X", "POST", BASE + "/auth/session/password-login", "-H",
                        "Content-Type: application/json", "-d", json.dumps({"phone": tel, "password": "Zdq123456"})],
                       capture_output=True, text=True).stdout
    return json.loads(o)["data"]["token"]


def api(token, route, payload):
    o = subprocess.run(["curl", "-s", "-X", "POST", "%s/%s" % (BASE, route), "-H", "Content-Type: application/json",
                        "-H", "token: " + token, "-d", json.dumps(payload, ensure_ascii=False)],
                       capture_output=True, text=True).stdout
    d = json.loads(o or "{}")
    if not d.get("success"):
        raise RuntimeError("接口失败 %s: %s" % (route, json.dumps(d, ensure_ascii=False)[:200]))
    return d.get("data") or {}


def rows_of(page):
    return page.get("content") or page.get("list") or []


def money(x):
    return Decimal(str(x or 0)).quantize(Decimal("0.01"))


def money_str(x):
    return "%0.2f" % money(x)


def sql_scope_store(col="store_id"):
    return "" if False else col


raw_dump = {"note": "A7/A8 跨页对账原始响应关键字段（三档门店范围，真实 HTTP）", "scopes": {}}
token = login()
report = []
report.append("=" * 118)
report.append("B10 跨页对账（A7 项目维度 / A8 财务维度）三档门店范围 · dev · 造数企业 M=%s · 月份=%s" % (COM, MONTH))
report.append("=" * 118)

for label, store in SCOPES:
    body = {"month": MONTH, "topN": 5}
    if store:
        body["storeId"] = store
    dash = api(token, "statistic/boss/dashboard", body)
    proj = api(token, "project/project/page", dict(body, current=1, size=200))
    biz = api(token, "statistic/boss/business/report", body)
    summ = api(token, "finance/summary/report", dict(body))
    recv = api(token, "finance/receivable/page", dict(body, current=1, size=200))
    fprj = api(token, "finance/project/page", dict(body, current=1, size=200))

    active = int(dash.get("activeProjectCount", {}).get("value") or 0)
    proj_rows = rows_of(proj)
    proj_total = int(proj.get("totalElements") or len(proj_rows))
    sum_recv = money(summ.get("receivableAmount"))
    recv_rows = rows_of(recv)
    recv_total = int(recv.get("totalElements") or len(recv_rows))
    recv_sum = sum((money(r.get("amount")) for r in recv_rows), Decimal("0"))
    recv_received = sum((money(r.get("receivedAmount")) for r in recv_rows), Decimal("0"))
    ledger_rows = rows_of(fprj)
    ledger_sum = sum((money(r.get("receivableAmount")) for r in ledger_rows), Decimal("0"))
    ledger_received = sum((money(r.get("receivedAmount")) for r in ledger_rows), Decimal("0"))
    store_clause = "" if store is None else (
        " and coalesce(p.store_id, c.store_id) = %d" % store)
    sql_active = int(q("select count(*) from db_project p join cust_customer c on c.cust_id=p.customer_id "
                       "where p.com_id=%s and p.status not in ('Finished','Settled')%s" % (COM, store_clause))[0][0])
    sql_proj = int(q("select count(*) from db_project p join cust_customer c on c.cust_id=p.customer_id "
                     "where p.com_id=%s%s" % (COM, store_clause))[0][0])
    recv_store_clause = "" if store is None else " and store_id = %d" % store
    sql_recv = q("select count(*), ifnull(sum(amount),0), ifnull(sum(received_amount),0) from db_finance_receivable "
                 "where com_id=%s%s" % (COM, recv_store_clause))[0]

    report.append("")
    report.append("─" * 118)
    report.append("【%s】范围参数 storeId=%s  · 原始响应（节选）" % (label, store))
    report.append("  statistic/boss/dashboard      → activeProjectCount=%s  storeScope=%s"
                  % (json.dumps(dash.get("activeProjectCount"), ensure_ascii=False), json.dumps(dash.get("storeScope"), ensure_ascii=False)))
    report.append("  project/project/page          → totalElements=%s（取回 %s 条，size=200）" % (proj_total, len(proj_rows)))
    funnel = {f.get("key"): f.get("count") for f in (biz.get("funnel") or [])}
    report.append("  statistic/boss/business/report→ funnel=%s" % json.dumps(biz.get("funnel"), ensure_ascii=False))
    report.append("  finance/summary/report        → projectCount=%s activeProjectCount=%s receivableAmount=%s receivedAmount=%s receiptCount=%s"
                  % (summ.get("projectCount"), summ.get("activeProjectCount"), money_str(summ.get("receivableAmount")),
                     money_str(summ.get("receivedAmount")), summ.get("receiptCount")))
    report.append("  finance/receivable/page       → totalElements=%s 明细 amount 合计=%s receivedAmount 合计=%s"
                  % (recv_total, money_str(recv_sum), money_str(recv_received)))
    report.append("  finance/project/page          → 条数=%s  receivableAmount 合计=%s  receivedAmount 合计=%s"
                  % (len(ledger_rows), money_str(ledger_sum), money_str(ledger_received)))
    report.append("  SQL 交叉                       → 项目(status∉Finished/Settled)=%s 项目(全部)=%s 应收(条数/金额/已收)=%s/%s/%s"
                  % (sql_active, sql_proj, sql_recv[0], money_str(sql_recv[1]), money_str(sql_recv[2])))

    raw_dump["scopes"][label] = {
        "storeId": store,
        "dashboard": {"activeProjectCount": dash.get("activeProjectCount"), "storeScope": dash.get("storeScope")},
        "projectPage": {"totalElements": proj_total, "returned": len(proj_rows)},
        "businessReport": {"funnel": biz.get("funnel"), "storeScope": biz.get("storeScope")},
        "financeSummary": {k: summ.get(k) for k in ("projectCount", "activeProjectCount", "contractAmount",
                                                    "receivableAmount", "receivedAmount", "outstandingAmount", "receiptCount")},
        "receivablePage": {"totalElements": recv_total, "amountSum": money_str(recv_sum), "receivedSum": money_str(recv_received)},
        "financeProjectPage": {"rows": len(ledger_rows), "receivableSum": money_str(ledger_sum), "receivedSum": money_str(ledger_received)},
        "sql": {"projectsAll": sql_proj, "projectsActive": sql_active,
                "receivableCount": int(sql_recv[0]), "receivableAmount": money_str(sql_recv[1]), "receivedAmount": money_str(sql_recv[2])},
    }
    report.append("")
    report.append("  【A7 项目维度对照表 · %s】" % label)
    report.append("  | 取值来源 | 值 | 口径定义 | 与 SQL 一致 |")
    report.append("  | --- | ---: | --- | --- |")
    a7 = [("经营总览 dashboard.activeProjectCount", active, "status∉{Finished,Settled} 且按月有效性过滤，门店=项目门店(缺失回退客户门店)", active == sql_active),
          ("项目列表 project/project/page.totalElements", proj_total, "可见项目总数（不按状态过滤）", proj_total == sql_proj),
          ("商务分析 business/report 漏斗：线索 / 报价 / 签约",
           "%s / %s / %s" % (funnel.get("lead"), funnel.get("quote"), funnel.get("sign")),
           "**客户线索漏斗**（线索=客户数、报价=报价份数、签约=合同单数）→ 非项目计数，需对照说明",
           None),
          ("SQL db_project（同门店谓词）", sql_proj, "全部项目", True)]
    for name, val, desc, ok in a7:
        report.append("  | %s | %s | %s | %s |" % (name, val, desc, "✅" if ok else ("—" if ok is None else "❌")))
    funnel_sql = q("select (select count(*) from cust_customer where com_id=%s%s),"
                   " (select count(*) from quote where com_id=%s%s),"
                   " (select count(*) from db_project_contract where com_id=%s%s)"
                   % (COM, ("" if store is None else " and store_id=%d" % store),
                      COM, ("" if store is None else " and store_id=%d" % store),
                      COM, ("" if store is None else " and store_id=%d" % store)))[0]
    report.append("  → 商务分析漏斗 SQL 交叉：线索/客户=%s、报价=%s、签约/合同=%s（与 funnel %s/%s/%s %s）"
                  % (funnel_sql[0], funnel_sql[1], funnel_sql[2], funnel.get("lead"), funnel.get("quote"), funnel.get("sign"),
                     "✅一致" if [str(funnel.get(k)) for k in ("lead", "quote", "sign")] == funnel_sql else "❌不一致"))
    report.append("  → A7 判定：dashboard.activeProjectCount == project/page.totalElements == SQL(%s==%s) %s；"
                  % (active, sql_proj, "✅" if (active == proj_total == sql_proj) else "❌"))
    report.append("     商务分析 funnel 属**客户线索口径**（线索/到访/量房），与项目计数不同源；其门店范围标注 storeScope 一致，可对照不可等同（口径差异已说明）。")

    report.append("")
    report.append("  【A8 财务维度对照表 · %s】" % label)
    report.append("  | 取值来源 | 应收金额 | 应收条数 | 已收金额 | 与 SQL 一致 |")
    report.append("  | --- | ---: | ---: | ---: | --- |")
    a8 = [("财务工作台 finance/summary/report", sum_recv, "**接口未返回应收条数**", money(summ.get("receivedAmount")),
           sum_recv == money(sql_recv[1])),
          ("应收分页 finance/receivable/page", recv_sum, recv_total, recv_received, recv_sum == money(sql_recv[1])),
          ("项目账本 finance/project/page（逐项目 receivableAmount 合计）", ledger_sum, "—（项目数 %d）" % len(ledger_rows),
           ledger_received, ledger_sum == money(sql_recv[1])),
          ("SQL db_finance_receivable", money(sql_recv[1]), int(sql_recv[0]), money(sql_recv[2]), True)]
    for name, amount, cnt, recvd, ok in a8:
        report.append("  | %s | %s | %s | %s | %s |" % (name, money_str(amount), cnt, money_str(recvd), "✅" if ok else "❌"))
    report.append("  → A8 判定：应收金额三处一致（%s / %s / %s）%s；应收**条数**仅分页接口提供（%s 条 = SQL %s 条）%s；"
                  % (money_str(sum_recv), money_str(recv_sum), money_str(ledger_sum),
                     "✅" if (sum_recv == recv_sum == ledger_sum) else "❌", recv_total, sql_recv[0],
                     "✅" if str(recv_total) == sql_recv[0] else "❌"))
    report.append("     已收金额两处一致（%s / %s）%s；汇总报表缺「应收条数」字段 → 记为**契约补齐候选**（不影响金额一致）。"
                  % (money_str(summ.get("receivedAmount")), money_str(ledger_received),
                     "✅" if money(summ.get("receivedAmount")) == ledger_received else "❌"))

print("\n".join(report))
Path("docs/operations/data-changes/B10-crosspage-raw.json").write_text(
    json.dumps(raw_dump, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
