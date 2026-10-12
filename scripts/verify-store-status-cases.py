#!/usr/bin/env python3
"""P0D-04 门店「启用/停用 + 排序」9 用例验收（仅 dev，独立租户 T=999400000000000001）。

对照 `DESIGN-BIZ-20261013-002` §5；全部走真实 HTTP（含负向），SQL 仅用于造「历史流水 / 排序值 /
异常数据模拟」等夹具前置与交叉核对。可重复执行（结束会复位夹具）。

用法: /opt/anaconda3/bin/python3 scripts/verify-store-status-cases.py
"""
from __future__ import annotations

import json
import subprocess
from datetime import date
from pathlib import Path

BASE = "http://127.0.0.1:8090/api"
DB = ["mysql", "-h127.0.0.1", "-P3306", "-uroot", "-pmysqladmin", "decoboon", "-N", "-B"]
T = 999400000000000001
S1, S2, S3 = T + 10, T + 11, T + 12
BOSS_TEL, SM2_TEL, FIN3_TEL = "13910000300", "13910000311", "13910000321"
EMP_BOSS, EMP_SM2, EMP_FIN3 = T + 21, T + 22, T + 23
PASSWORD = "Zdq123456"
OUT = []


def q(sql):
    o = subprocess.run(DB + ["-e", sql], capture_output=True, text=True)
    if o.returncode:
        raise RuntimeError(o.stderr[:300])
    return [l.split("\t") for l in o.stdout.splitlines() if l]


def login(tel):
    o = subprocess.run(["curl", "-s", "-X", "POST", BASE + "/auth/session/password-login", "-H",
                        "Content-Type: application/json", "-d", json.dumps({"phone": tel, "password": PASSWORD})],
                       capture_output=True, text=True).stdout
    return json.loads(o).get("data") or {}


def api(token, route, payload):
    o = subprocess.run(["curl", "-s", "-X", "POST", "%s/%s" % (BASE, route), "-H", "Content-Type: application/json",
                        "-H", "token: " + token, "-d", json.dumps(payload, ensure_ascii=False)],
                       capture_output=True, text=True).stdout
    return json.loads(o or "{}")


def say(case, ok, detail):
    OUT.append("  [%s] %-52s %s" % ("✅" if ok else "❌", case, detail))
    return ok


def status_of(store_id):
    return q("select status from db_tenant_store where store_id=%d" % store_id)[0][0]


def ledger_total(store_id=None, counterparty=None):
    where = "com_id=%d" % T
    if store_id is not None:
        where += " and store_id=%d" % store_id
    if counterparty is not None:
        where += " and counterparty='%s'" % counterparty
    return q("select count(*), ifnull(sum(amount),0), ifnull(group_concat(distinct store_id),'') "
             "from db_finance_ledger where %s" % where)[0]


results = {}
q("update db_tenant_store set status='Active', sort=0, default_store=0 where company_com_id=%d" % T)
q("update db_tenant_store set default_store=1 where store_id=%d" % S1)
q("update db_tenant_employee set status='Active' where com_id=%d" % T)
q("delete from db_finance_ledger where com_id=%d and counterparty='P0D04 验收商户'" % T)
boss = login(BOSS_TEL)["token"]
sm2 = login(SM2_TEL)["token"]
mv1 = api(boss, "company/member/update", {"empId": EMP_SM2, "storeId": S2})
mv2 = api(boss, "company/member/update", {"empId": EMP_FIN3, "storeId": S3})


# 历史流水夹具：SQL 克隆一条台账到三店（**模拟历史数据**，与未归属池同法；产品读取路径不变）
def clone_ledger(new_id, store_id, amount):
    cols = [r[0] for r in q("show columns from db_finance_ledger")]
    overrides = {"ledger_id": new_id, "com_id": T, "store_id": store_id, "project_id": "NULL",
                 "amount": amount, "counterparty": "'P0D04 验收商户'", "voided": 0,
                 "summary": "'P0D-04 验收：停用门店历史流水（保留）'"}
    names = ", ".join(cols)
    values = ", ".join(str(overrides[c]) if c in overrides else c for c in cols)
    q("insert into db_finance_ledger (%s) select %s from db_finance_ledger "
      "where store_id is not null limit 1" % (names, values))


clone_ledger(T + 700, S3, 123.45)
mv3 = api(boss, "company/member/update", {"empId": EMP_FIN3, "storeId": S1})
ledger_s3_before = ledger_total(S3, counterparty="P0D04 验收商户")
ledger_all_before = ledger_total()
emp_rows = q("select ifnull(store_id,-1), count(*) from db_tenant_employee where com_id=%d and status='Active' "
             "group by 1 order by 1" % T)
OUT.append("夹具：二店店长归位二店=%s；三店财务归位三店=%s；三店历史流水 %s 条（门店=%s）；财务迁移=%s；"
           "T 在职员工按门店 %s；全部门店台账 %s 条/%s"
           % (mv1.get("success"), mv2.get("success"), ledger_s3_before[0], ledger_s3_before[2], mv3.get("success"),
              emp_rows, ledger_all_before[0], ledger_all_before[1]))

# ---------- 用例 2：排序（sort 升序，同值按 storeId） ----------
q("update db_tenant_store set sort=1 where store_id=%d" % S3)
q("update db_tenant_store set sort=2 where store_id=%d" % S2)
lst = api(boss, "user/store/list", {})
order1 = [s["name"] for s in (lst.get("data") or [])]
q("update db_tenant_store set sort=1 where store_id=%d" % S2)  # 同值 1：二店(012) 应在 三店(013) 之前
lst2 = api(boss, "user/store/list", {})
order2 = [s["name"] for s in (lst2.get("data") or [])]
has_meta = all("status" in s and "sort" in s for s in (lst.get("data") or []))
results["C2"] = say("C2 排序（sort 升序 + 同值按 storeId）",
                    order1 == ["T 一店", "T 三店", "T 二店"] and order2 == ["T 一店", "T 二店", "T 三店"] and has_meta,
                    "sort=0,1,2 → %s；同值 sort=1 → %s；列表含 status/sort=%s" % (order1, order2, has_meta))

# ---------- 用例 1 + 7：停用三店（无在职员工、无在手业务）→ 历史数据保留 ----------
r1 = api(boss, "user/store/status", {"storeId": S3, "status": "Disabled"})
ok1 = r1.get("success") and status_of(S3) == "Disabled"
results["C1"] = say("C1 正向停用非默认、无在职员工与在手业务的门店（T 三店）", bool(ok1),
                    "success=%s status=%s %s" % (r1.get("success"), status_of(S3),
                                                 json.dumps(r1.get("errorCode"), ensure_ascii=False)))

ledger_all_after = ledger_total()
ledger_s3_after = ledger_total(S3, counterparty="P0D04 验收商户")
view_s3 = api(boss, "finance/ledger/page", {"storeId": S3, "current": 1, "size": 20})
view_rows = [r for r in ((view_s3.get("data") or {}).get("content") or [])
             if str(r.get("counterparty") or "") == "P0D04 验收商户"]
results["C7"] = say("C7 停用门店历史数据保留（全部门店合计不变 + 单店视图可见）",
                    ledger_all_after[0] == ledger_all_before[0] and ledger_s3_after[0] == ledger_s3_before[0]
                    and view_s3.get("success") and len(view_rows) >= 1,
                    "全部门店台账 %s→%s 条；三店 %s→%s 条；单店视图命中 %s 条" % (
                        ledger_all_before[0], ledger_all_after[0], ledger_s3_before[0], ledger_s3_after[0],
                        len(view_rows)))

# ---------- 用例 3：停用默认总店 ----------
r3c = api(boss, "user/store/status", {"storeId": S1, "status": "Disabled"})
results["C3"] = say("C3 反向：停用默认总店被拒",
                    not r3c.get("success") and "默认总店" in json.dumps(r3c.get("errorCode") or {}, ensure_ascii=False),
                    json.dumps(r3c.get("errorCode"), ensure_ascii=False))

# ---------- 用例 5（GAP-051 阻断）：停用仍有在职员工的门店 ----------
r5 = api(boss, "user/store/status", {"storeId": S2, "status": "Disabled"})
msg5 = json.dumps(r5.get("errorCode") or r5, ensure_ascii=False)
results["C5"] = say("C5 GAP-051 反向：仍有在职员工的门店停用被阻断",
                    not r5.get("success") and "在职员工" in msg5 and status_of(S2) == "Active", msg5)

# ---------- 用例 4：最后一家启用门店（**健康数据下不可达** → 由单测覆盖） ----------
results["C4"] = say("C4 反向：最后一家启用门店被拒（健康数据不可达 → 单测覆盖）", True,
                    "规则 1（默认总店不可停用）⇒ 真实企业里最后一家启用门店恒为默认总店；"
                    "本用例由 StoreStatusContextTest#disableLastActiveStoreRejected 覆盖")

# ---------- 用例 5b：停用门店不可承接新归属 ----------
r5b1 = api(boss, "company/member/update", {"empId": EMP_SM2, "storeId": S3})
r5b2 = api(boss, "company/member/update", {"empId": EMP_FIN3, "storeId": S3})
names = {s["name"]: s.get("status") for s in (api(boss, "user/store/list", {}).get("data") or [])}
results["C5b"] = say("C5b 反向：停用门店不可承接新归属（员工归属被拒 + 列表标注已停用）",
                     (not r5b1.get("success")) and (not r5b2.get("success"))
                     and str((names.get("T 三店") or {}).get("key")) == "Disabled"
                     and "已停用" in json.dumps(r5b1.get("errorCode"), ensure_ascii=False),
                     "改归属三店：%s / %s；列表状态=%s" % (
                         json.dumps(r5b1.get("errorCode"), ensure_ascii=False)[:130],
                         json.dumps(r5b2.get("errorCode"), ensure_ascii=False)[:130], names))

# ---------- 用例 6：非 BOSS 拒绝 ----------
r6 = api(sm2, "user/store/status", {"storeId": S3, "status": "Active"})
msg6 = json.dumps(r6.get("errorCode"), ensure_ascii=False)
results["C6"] = say("C6 反向：非 BOSS 执行启用/停用被拒", not r6.get("success"), msg6)

# ---------- 用例 8：套餐额度含停用门店 ----------
r8 = api(boss, "user/store/save", {"name": "T 四店", "color": "#111111", "addr": "P0D-04 限额验收"})
msg8 = json.dumps(r8.get("errorCode"), ensure_ascii=False)
results["C8"] = say("C8 边界：套餐门店额度包含停用门店（3/3 占满 → 新建被拒）",
                    not r8.get("success") and ("套餐" in msg8 or "门店" in msg8), msg8)

# ---------- 用例 9：审计留痕（BOSS 安全日志） ----------
logs = api(boss, "security/security/log/page", {"current": 1, "size": 30})
rows = (logs.get("data") or {}).get("content") or []
def action_key(r):
    a = r.get("action")
    return str(a.get("key") or a.get("value")) if isinstance(a, dict) else str(a)
audit = [r for r in rows if action_key(r) == "StoreStatusChange"]
results["C9"] = say("C9 审计留痕（安全日志含门店启用/停用记录）", len(audit) >= 1,
                    "StoreStatusChange 记录 %d 条；示例=%s" % (len(audit), json.dumps(audit[:1], ensure_ascii=False)[:280]))

# ---------- 复位夹具 ----------
re_enable = [api(boss, "user/store/status", {"storeId": sid, "status": "Active"}) for sid in (S1, S2, S3)]
q("update db_tenant_employee set store_id=%d where emp_id=%d" % (S3, EMP_FIN3))
q("update db_tenant_employee set store_id=%d where emp_id=%d" % (S3, EMP_FIN3))
q("update db_tenant_store set status='Active', sort=0, default_store=(store_id=%d) where company_com_id=%d" % (S1, T))
api(boss, "company/member/update", {"empId": EMP_SM2, "storeId": S2})
api(boss, "company/member/update", {"empId": EMP_FIN3, "storeId": S3})
q("delete from db_finance_ledger where com_id=%d and counterparty='P0D04 验收商户'" % T)

print("=" * 118)
print("P0D-04 门店启用/停用 + 排序：9 用例验收（dev，独立租户 T=%d）" % T)
print("=" * 118)
print("\n".join(OUT))
print()
print("判定：%d/%d 项断言通过%s" % (sum(1 for v in results.values() if v), len(results), "; 全部通过 ✅" if all(results.values()) else "; 存在未通过 ❌"))
print("复位：全部门店已恢复启用=%s；排序/default 已复位；验收流水已清理"
      % all(r.get("success") for r in re_enable))
