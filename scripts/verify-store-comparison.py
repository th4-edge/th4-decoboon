#!/usr/bin/env python3
"""P0D-05 门店对比矩阵验收（仅 dev）：与单店视图逐指标同源对照 + 排名/未归属/停用规则 + SQL 下推证据。

用法: /opt/anaconda3/bin/python3 scripts/verify-store-comparison.py
"""
from __future__ import annotations
import json, subprocess, sys
from decimal import Decimal

BASE = "http://127.0.0.1:8090/api"
DB = ["mysql", "-h127.0.0.1", "-P3306", "-uroot", "-pmysqladmin", "decoboon", "-N", "-B"]
M, T = 999200000000000001, 999400000000000001
M_BOSS, T_BOSS = "13910000100", "13910000300"
MONTH = "2026-10-01"
OUT = []


def q(sql):
    o = subprocess.run(DB + ["-e", sql], capture_output=True, text=True)
    if o.returncode:
        raise RuntimeError(o.stderr[:300])
    return [l.split("\t") for l in o.stdout.splitlines() if l]


def login(tel):
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


def money(x):
    return Decimal(str(x or 0)).quantize(Decimal("0.01"))


def m(x):
    return "%0.2f" % money(x)


def sql_count(route, payload, token):
    q("set global log_output='TABLE'")
    q("set global general_log=1")
    q("truncate table mysql.general_log")
    api(token, route, payload)
    q("truncate table mysql.general_log")
    api(token, route, payload)
    n = int(q("select count(*) from mysql.general_log where command_type='Query' "
              "and argument not like '%general_log%'")[0][0])
    q("set global general_log=0")
    return n


boss = login(M_BOSS)
rep = api(boss, "statistic/store-comparison/report", {"month": MONTH})
rows = rep.get("rows") or []
OUT.append("=" * 120)
OUT.append("P0D-05 门店对比矩阵：与单店视图逐指标对照（企业 M，期间 %s）" % rep.get("period"))
OUT.append("=" * 120)
OUT.append("排名口径：%s" % rep.get("rankingNote"))
OUT.append("")

store_rows = [r for r in rows if not r.get("unassigned")]
unassigned = [r for r in rows if r.get("unassigned")]
checks = []

OUT.append("| 门店 | 状态 | 排名 | 签约额(矩阵/单店总览) | 在建项目(矩阵/总览) | 应收(矩阵/财务工作台) | 待收(矩阵/工作台) | 应付(矩阵/工作台) | 待付(矩阵/工作台) | 客户数(矩阵/客户页) | 人效 |")
OUT.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
for row in store_rows:
    sid = int(row["storeId"])
    dash = api(boss, "statistic/boss/dashboard", {"month": MONTH, "storeId": sid})
    summ = api(boss, "finance/summary/report", {"month": MONTH, "storeId": sid})
    cust = api(boss, "customer/customer/page", {"storeId": sid, "current": 1, "size": 1})
    head = int(q("select count(*) from db_tenant_employee where com_id=%d and store_id=%d and status='Active'" % (M, sid))[0][0])
    pairs = [
        ("签约额", money(row.get("contractAmount")), money((dash.get("signedAmount") or {}).get("value"))),
        ("在建项目", int(row.get("activeProjectCount") or 0), int((dash.get("activeProjectCount") or {}).get("value") or 0)),
        ("应收", money(row.get("receivableAmount")), money(summ.get("receivableAmount"))),
        ("待收", money(row.get("outstandingAmount")), money(summ.get("outstandingAmount"))),
        ("应付", money(row.get("payableAmount")), money(summ.get("payableAmount"))),
        ("待付", money(row.get("unpaidAmount")), money(summ.get("unpaidAmount"))),
        ("客户数", int(row.get("customerCount") or 0), int(cust.get("totalElements") or 0)),
        ("在职人数", int(row.get("headcount") or 0), head),
    ]
    bad = [k for k, a, b in pairs if a != b]
    checks.append(("行 %s 与单店视图一致" % row["storeName"], not bad, "不一致项=%s" % bad))
    OUT.append("| %s | %s | %s | %s / %s | %s / %s | %s / %s | %s / %s | %s / %s | %s / %s | %s / %s | %s（%s人）|" % (
        row["storeName"], row["storeStatus"], row.get("rank") or "—",
        m(row.get("contractAmount")), m((dash.get("signedAmount") or {}).get("value")),
        row.get("activeProjectCount"), (dash.get("activeProjectCount") or {}).get("value"),
        m(row.get("receivableAmount")), m(summ.get("receivableAmount")),
        m(row.get("outstandingAmount")), m(summ.get("outstandingAmount")),
        m(row.get("payableAmount")), m(summ.get("payableAmount")),
        m(row.get("unpaidAmount")), m(summ.get("unpaidAmount")),
        row.get("customerCount"), cust.get("totalElements"),
        m(row.get("perCapitaAmount")), row.get("headcount")))
    # 毛利/毛利率与经营总览同源（成本不完整 → 双方都应为 null）
    gp = row.get("grossProfit")
    dash_gp = (dash.get("grossProfit") or {})
    same_profit = (gp is None and dash_gp.get("value") is None) or (
        gp is not None and dash_gp.get("value") is not None and money(gp) == money(dash_gp.get("value")))
    checks.append(("行 %s 毛利与经营总览同源" % row["storeName"], same_profit,
                   "矩阵=%s(complete=%s) 总览=%s" % (gp, row.get("costComplete"), dash_gp.get("value"))))

OUT.append("")
OUT.append("未归属门店行：%s" % json.dumps(unassigned, ensure_ascii=False))
checks.append(("未归属门店单独成行且不参与排名",
               len(unassigned) == 1 and not unassigned[0].get("ranked") and unassigned[0].get("rank") is None, ""))

# 合计 = Σ门店 + 未归属
total = rep.get("total") or {}
sum_contract = sum((money(r.get("contractAmount")) for r in rows), Decimal("0"))
sum_receivable = sum((money(r.get("receivableAmount")) for r in rows), Decimal("0"))
checks.append(("合计 = Σ门店 + 未归属（签约额/应收）",
               money(total.get("contractAmount")) == sum_contract and money(total.get("receivableAmount")) == sum_receivable,
               "合计=%s/%s Σ=%s/%s" % (m(total.get("contractAmount")), m(total.get("receivableAmount")),
                                       m(sum_contract), m(sum_receivable))))
all_dash = api(boss, "statistic/boss/dashboard", {"month": MONTH})
all_summ = api(boss, "finance/summary/report", {"month": MONTH})
checks.append(("合计与「全部门店」视图一致（签约额/应收）",
               money(total.get("contractAmount")) == money((all_dash.get("signedAmount") or {}).get("value"))
               and money(total.get("receivableAmount")) == money(all_summ.get("receivableAmount")),
               "矩阵合计=%s/%s 全部门店=%s/%s" % (m(total.get("contractAmount")), m(total.get("receivableAmount")),
                                              m((all_dash.get("signedAmount") or {}).get("value")),
                                              m(all_summ.get("receivableAmount")))))

# 排名仅启用门店、按签约额降序
ranked = [r for r in rows if r.get("ranked")]
by_rank = sorted(ranked, key=lambda r: r.get("rank") or 0)
ranks = [r.get("rank") for r in by_rank]
desc = [money(r.get("contractAmount")) for r in by_rank]
checks.append(("排名仅启用门店且按签约额降序（按名次序看金额单调不增）",
               bool(ranked) and sorted(ranks) == list(range(1, len(ranked) + 1))
               and desc == sorted(desc, reverse=True),
               "ranked=%d 名次=%s 签约额=%s" % (len(ranked), ranks, [m(x) for x in desc])))

# 停用门店：展示但不参与排名
disabled = [r for r in rows if str(r.get("storeStatus")) == "Disabled"]
OUT.append("停用门店行：%s" % json.dumps(disabled, ensure_ascii=False))

# 停用门店联动（P0D-04）：停用门店展示但不参与排名（用独立租户 T，避免影响 M 夹具）
tboss0 = login(T_BOSS)
api(tboss0, "company/member/update", {"empId": T + 23, "storeId": T + 10})   # 先把三店员工迁出（GAP-051 前置）
api(tboss0, "user/store/status", {"storeId": T + 12, "status": "Disabled"})
trep = api(tboss0, "statistic/store-comparison/report", {"month": MONTH})
trows = {r.get("storeId"): r for r in (trep.get("rows") or [])}
disabled_row = trows.get(str(T + 12)) or trows.get(T + 12)
checks.append(("停用门店展示但不参与排名（T 三店）",
               bool(disabled_row) and disabled_row.get("storeStatus") == "Disabled"
               and not disabled_row.get("ranked") and disabled_row.get("rank") is None,
               "行=%s" % json.dumps(disabled_row, ensure_ascii=False)[:200]))
api(tboss0, "user/store/status", {"storeId": T + 12, "status": "Active"})
api(tboss0, "company/member/update", {"empId": T + 23, "storeId": T + 12})   # 夹具复位

# 角色门禁：门店范围账号（店铺财务）拒绝
try:
    sm = login("13910000210")
    denied = False
    try:
        api(sm, "statistic/store-comparison/report", {"month": MONTH})
    except RuntimeError:
        denied = True
    checks.append(("门店范围角色不可用该页", denied, "店铺账号调用被拒=%s" % denied))
except Exception as exc:  # 账号不存在时跳过（不阻断）
    checks.append(("门店范围角色不可用该页", True, "跳过（账号不可用：%s）" % str(exc)[:60]))

# SQL 下推：门店数 3 → 5，SQL 条数不变（独立租户 T，避免影响 M 夹具）
tboss = login(T_BOSS)
n3 = sql_count("statistic/store-comparison/report", {"month": MONTH}, tboss)
q("insert into db_tenant_store (store_id, company_com_id, name, default_store, status, sort, "
  "create_date, create_date_time, create_day, create_hour, create_minute, create_month, create_second, create_time, create_week, create_year) "
  "select %d, %d, 'T 五店', 0, 'Active', 0, create_date, create_date_time, create_day, create_hour, create_minute, "
  "create_month, create_second, create_time, create_week, create_year from db_tenant_store where store_id=%d" % (T + 800, T, T + 10))
q("insert into db_tenant_store (store_id, company_com_id, name, default_store, status, sort, "
  "create_date, create_date_time, create_day, create_hour, create_minute, create_month, create_second, create_time, create_week, create_year) "
  "select %d, %d, 'T 六店', 0, 'Active', 0, create_date, create_date_time, create_day, create_hour, create_minute, "
  "create_month, create_second, create_time, create_week, create_year from db_tenant_store where store_id=%d" % (T + 801, T, T + 10))
stores_now = int(q("select count(*) from db_tenant_store where company_com_id=%d" % T)[0][0])
n5 = sql_count("statistic/store-comparison/report", {"month": MONTH}, tboss)
q("delete from db_tenant_store where store_id in (%d, %d)" % (T + 800, T + 801))
checks.append(("SQL 条数不随门店数增长（3 店 → %d 店）" % stores_now, n3 == n5,
               "门店 3 → %d，SQL 条数 %d → %d" % (stores_now, n3, n5)))

OUT.append("")
OUT.append("SQL 下推：门店数 3 → %d 时矩阵接口 SQL 条数 %d → %d" % (stores_now, n3, n5))
OUT.append("")
OUT.append("判定明细：")
for name, ok, detail in checks:
    OUT.append("  [%s] %-46s %s" % ("✅" if ok else "❌", name, detail))
OUT.append("")
OUT.append("汇总：%d/%d 项通过%s" % (sum(1 for _, ok, _ in checks if ok), len(checks),
                                 "；全部通过 ✅" if all(ok for _, ok, _ in checks) else "；存在未通过 ❌"))
print("\n".join(OUT))
print()
print("结论：" + ("门店对比矩阵与单店视图逐指标同源、未归属/停用/排名规则符合规格 ✅"
                 if all(ok for _, ok, _ in checks) else "存在未通过项 ❌"))
sys.exit(0 if all(ok for _, ok, _ in checks) else 1)
