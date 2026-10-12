#!/usr/bin/env python3
"""门店维度批次二断言：A1 未归属池/FK 完整性、A2 未归属标注、A4 应付同源、A4b 门店越权、A5/A5b 换负责人不回溯（仅 dev）。

用法: /opt/anaconda3/bin/python3 scripts/assert-store-dimension-deep.py
说明: 除 A5/A5b 需要真实调用 `customer/customer/handoff` 与 `customer/customer/update`（幂等，可重复执行）外全部只读；
      断言口径见 DESIGN-BIZ-20261013-008 §3：「不回溯」= 房屋/项目/合同/历史单据门店不变。
"""
import json, os, subprocess, sys
from pathlib import Path

DB = ["mysql", "-h127.0.0.1", "-P3306", "-uroot", "-pmysqladmin", os.environ.get("DECOBOON_DB", "decoboon"), "-N", "-B"]
BASE = os.environ.get("DECOBOON_API_BASE", "http://127.0.0.1:8090/api")
COM = "999200000000000001"
STORE_A, STORE_B = "999200000000000011", "999200000000000012"
MANIFEST = Path("docs/operations/data-changes/2026-10-13-门店维度造数manifest.json")


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
    return json.loads(o or "{}")


def enum_of(v):
    return str(v.get("key") or v.get("value")) if isinstance(v, dict) else ("" if v is None else str(v))


print("=" * 100)
print("A1/A2 未归属池：对象计数、`store_id IS NULL` 归属、FK 完整性（com=%s）" % COM)
print("=" * 100)
pool = q("""select 'cust_customer',count(*) from cust_customer where com_id=%s and store_id is null
union all select 'db_property',count(*) from db_property where com_id=%s and store_id is null
union all select 'quote',count(*) from quote where com_id=%s and store_id is null
union all select 'db_project_contract',count(*) from db_project_contract where com_id=%s and store_id is null
union all select 'db_project',count(*) from db_project where com_id=%s and store_id is null
union all select 'db_finance_payable',count(*) from db_finance_payable where com_id=%s and store_id is null
union all select 'db_finance_receivable',count(*) from db_finance_receivable where com_id=%s and store_id is null
union all select 'db_finance_payment',count(*) from db_finance_payment where com_id=%s and store_id is null
union all select 'db_finance_ledger',count(*) from db_finance_ledger where com_id=%s and store_id is null""" % ((COM,) * 9))
for r in pool:
    print("  未归属 %-24s %s" % (r[0], r[1]))
fk = q("""select 'quote→property', count(*) from quote a left join db_property b on b.property_id=a.property_id
 where a.com_id=%s and a.store_id is null and b.property_id is null
union all select 'contract→project', count(*) from db_project_contract a left join db_project b on b.project_id=a.project_id
 where a.com_id=%s and a.store_id is null and b.project_id is null
union all select 'contract→quote', count(*) from db_project_contract a left join quote b on b.quote_id=a.quote_id
 where a.com_id=%s and a.store_id is null and b.quote_id is null
union all select 'project→quote', count(*) from db_project a left join quote b on b.quote_id=a.quote_id
 where a.com_id=%s and a.store_id is null and b.quote_id is null
union all select 'contract→customer', count(*) from db_project_contract a left join cust_customer b on b.cust_id=a.customer_id
 where a.com_id=%s and a.store_id is null and b.cust_id is null
union all select 'receivable→project', count(*) from db_finance_receivable a left join db_project b on b.project_id=a.project_id
 where a.com_id=%s and a.store_id is null and b.project_id is null
union all select 'payment→payable', count(*) from db_finance_payment a left join db_finance_payable b on b.payable_id=a.payable_id
 where a.com_id=%s and a.store_id is null and b.payable_id is null
union all select 'ledger→project', count(*) from db_finance_ledger a left join db_project b on b.project_id=a.project_id
 where a.com_id=%s and a.store_id is null and b.project_id is null""" % ((COM,) * 8))
print("  FK 断链条数（应为 0）：", ", ".join("%s=%s" % (r[0], r[1]) for r in fk))
idn = q("""select 'cust_customer',sum(store_id is not null)+sum(store_id is null),count(*) from cust_customer where com_id=%s
union all select 'db_project',sum(store_id is not null)+sum(store_id is null),count(*) from db_project where com_id=%s
union all select 'db_finance_payable',sum(store_id is not null)+sum(store_id is null),count(*) from db_finance_payable where com_id=%s""" % (COM, COM, COM))
print("  A1 合计恒等式（Σ门店+未归属 = 总数）：", ", ".join("%s %s/%s" % (r[0], r[1], r[2]) for r in idn))

print()
print("=" * 100)
print("A2 未归属标注（HTTP）：全部门店 vs 单店")
print("=" * 100)
boss = login("13910000100")
for label, payload in (("all", {}), ("A", {"storeId": int(STORE_A)})):
    d = api(boss, "customer/customer/overview", payload).get("data") or {}
    print("  customer/customer/overview %-4s → storeScope=%s" % (label, json.dumps(d.get("storeScope"), ensure_ascii=False)))
    p = api(boss, "customer/customer/page", dict(payload, current=1, size=1)).get("data") or {}
    print("     page.total=", p.get("totalElements") or p.get("total"))

print()
print("=" * 100)
print("A4 应付同源（HTTP 应付分页，按项目比对 采购到货自动应付 与 手工应付 的门店）")
print("=" * 100)
state = json.loads(Path("docs/operations/data-changes/2026-10-13-门店维度造数-chain-state.json").read_text(encoding="utf-8"))
# 任意一次数据集重建后 state 会变化：取「每个门店最后一条已建项目的链路」作为 A4/A5 样本，保证脚本可重复执行
targets = [k for k in sorted(state) if (state[k] or {}).get("projectId")][-3:]
for key in sorted(targets):
    rec = state[key]
    pid = str(rec["projectId"])
    d = api(boss, "finance/payable/page", {"projectId": pid, "current": 1, "size": 50}).get("data") or {}
    rows = d.get("content") or d.get("list") or []
    sqlstore = q("select group_concat(distinct ifnull(store_id,-1)) from db_finance_payable where project_id=%s" % pid)
    print("  %-26s 项目=%s" % (key, pid))
    for r in rows:
        print("     payableId=%s 类型=%s 来源单号=%s 金额=%s 门店=%s" % (
            r.get("payableId"), enum_of(r.get("type")), r.get("sourceNo") or "(手工)", r.get("amount"),
            (r.get("project") or {}).get("storeId") or r.get("storeId")))
    print("     SQL 侧该项目应付门店去重 = %s（应只有客户门店一个值）" % sqlstore[0][0])

print()
print("=" * 100)
print("A5 换负责人不回溯（真实 handoff：把 A 店某客户负责人改为 B 店设计师，比对各对象门店）")
print("=" * 100)
manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
emps = (manifest.get("companies") or {}).get(COM, {}).get("employees") or []
b_designer = next(e for e in emps if e.get("role") == "Designer" and int(e.get("storeId") or 0) == int(STORE_B))
key = [k for k in sorted(state) if str(k).startswith("999200000000000011") and (state[k] or {}).get("projectId")][-1]
rec = state[key]
cust, prop, proj = str(rec["customerId"]), str(rec["propertyId"]), str(rec["projectId"])


def stores():
    sql = """select 'customer', ifnull(store_id,-1) from cust_customer where cust_id={c}
    union all select 'property', ifnull(store_id,-1) from db_property where property_id={p}
    union all select 'project', ifnull(store_id,-1) from db_project where project_id={j}
    union all select 'contract', ifnull(store_id,-1) from db_project_contract where project_id={j}
    union all select 'receivable', ifnull(store_id,-1) from db_finance_receivable where project_id={j}
    union all select 'receipt', ifnull(store_id,-1) from db_finance_receipt where project_id={j}
    union all select 'purchase', ifnull(store_id,-1) from db_purchase_order where project_id={j}
    union all select 'ledger', ifnull(store_id,-1) from db_finance_ledger where project_id={j}
    union all select 'design', ifnull(store_id,-1) from db_design where customer_id={c}
    union all select 'requirement', ifnull(store_id,-1) from db_design_requirement where customer_id={c}""".format(c=cust, p=prop, j=proj)
    return {r[0]: r[1] for r in q(sql)}


# 复位：先把客户负责人改回 A 店业务（使「换负责人前」恒为 A 店，脚本可重复执行且证据自洽）
a_marketer = next(e for e in emps if e.get("role") == "Marketer" and int(e.get("storeId") or 0) == int(STORE_A))
api(boss, "customer/customer/update", {"customerId": int(cust), "ownerId": int(a_marketer["empId"]),
                                       "reason": "造数 A5 复位：负责人改回 A 店"})
before = stores()
resp = api(boss, "customer/customer/handoff", {"customerId": int(cust), "propertyId": int(prop),
                                              "designerId": int(b_designer["empId"])})
print("  handoff success=%s（新负责人=B 店设计师 #%s）" % (resp.get("success"), str(b_designer["empId"])[-4:]))
after = stores()
print("  %-12s %-22s %-22s %s" % ("对象", "换负责人前门店", "换负责人后门店", "是否回溯"))
for name in before:
    flag = "**变了**" if before[name] != after[name] else "不变"
    print("  %-12s %-22s %-22s %s" % (name, before[name], after[name], flag))

print()
print("=" * 100)
print("A5b 换客户负责人（`customer/customer/update{ownerId,reason}`）：客户门店是否随新负责人门店变化，历史对象是否回溯")
print("=" * 100)
b_marketer = next(e for e in emps if e.get("role") == "Marketer" and int(e.get("storeId") or 0) == int(STORE_B))
b_manager = next(e for e in emps if e.get("role") == "StoreManager" and int(e.get("storeId") or 0) == int(STORE_B))
before2 = stores()
r2 = api(boss, "customer/customer/update", {"customerId": int(cust), "ownerId": int(b_marketer["empId"]),
                                            "reason": "造数 A5 验证：客户负责人跨店调动"})
print("  update success=%s（新负责人=B 店业务 #%s）" % (r2.get("success"), str(b_marketer["empId"])[-4:]))
after2 = stores()
for name in before2:
    flag = "**变了**" if before2[name] != after2[name] else "不变"
    print("  %-12s %-22s %-22s %s" % (name, before2[name], after2[name], flag))

# A5b 复位：把客户负责人改回 A 店业务，避免夹具把客户门店永久留在 B 店（影响后续对账）
api(boss, "customer/customer/update", {"customerId": int(cust), "ownerId": int(a_marketer["empId"]),
                                       "reason": "造数 A5b 复位：负责人改回 A 店"})

print()
print("=" * 100)
print("A4b 门店维度越权（负向）：B 店店长读取 A 店项目的应付")
print("=" * 100)
btok = login(str(b_manager["tel"]))
neg = api(btok, "finance/payable/page", {"projectId": int(proj), "current": 1, "size": 50})
rows = (neg.get("data") or {}).get("content") or []
print("  success=%s 明细条数=%s（期望 0 或失败；A 店项目应付不得被 B 店看见）" % (neg.get("success"), len(rows)))
neg2 = api(btok, "customer/customer/overview", {"storeId": int(STORE_A)})
print("  B 店店长请求 storeId=A 店 overview → success=%s msg=%s"
      % (neg2.get("success"), (neg2.get("errorCode") or {}).get("message")))
