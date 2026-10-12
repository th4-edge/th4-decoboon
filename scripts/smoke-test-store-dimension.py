#!/usr/bin/env python3
"""test 先验冒烟六项（test 实例 :8091 / decoboon-test）"""
import json, subprocess, time
from datetime import date

API = "http://127.0.0.1:8091/api"
DEV = "http://127.0.0.1:8090/api"
DBT, DBD = "decoboon-test", "decoboon"
M, T, S = 999200000000000001, 999400000000000001, 999300000000000001
SM_A, SM_B, SM_C = M + 10, M + 11, M + 12
T2, T3 = T + 11, T + 12
OUT = []


def q(db, sql):
    o = subprocess.run(["mysql", "-h127.0.0.1", "-P3306", "-uroot", "-pmysqladmin", db, "-N", "-B", "-e", sql],
                       capture_output=True, text=True)
    return [l.split("\t") for l in o.stdout.splitlines() if l] if o.returncode == 0 else [["ERR", o.stderr[:80]]]


def login(tel, base=API):
    o = subprocess.run(["curl", "-s", "-X", "POST", base + "/auth/session/password-login", "-H",
                        "Content-Type: application/json", "-d", json.dumps({"phone": tel, "password": "Zdq123456"})],
                       capture_output=True, text=True).stdout
    try:
        return (json.loads(o).get("data") or {}).get("token")
    except Exception:
        return None


def api(token, route, payload, base=API):
    o = subprocess.run(["curl", "-s", "-X", "POST", "%s/%s" % (base, route), "-H", "Content-Type: application/json",
                        "-H", "token: " + (token or ""), "-d", json.dumps(payload, ensure_ascii=False)],
                       capture_output=True, text=True).stdout
    try:
        return json.loads(o or "{}")
    except Exception:
        return {"success": False, "raw": o[:120]}


checks = []
boss_m, boss_t, boss_s = login("13910000100"), login("13910000300"), login("13910000200")

# 1) BOSS 全部门店 ↔ 单店切换
all_dash = api(boss_m, "statistic/boss/dashboard", {"month": "2026-10-01"})
a_dash = api(boss_m, "statistic/boss/dashboard", {"month": "2026-10-01", "storeId": SM_A})
all_proj = api(boss_m, "project/project/page", {"current": 1, "size": 200})
a_proj = api(boss_m, "project/project/page", {"storeId": SM_A, "current": 1, "size": 200})
ok1 = ((all_dash.get("data") or {}).get("activeProjectCount", {}).get("value") == 19
       and (a_dash.get("data") or {}).get("activeProjectCount", {}).get("value") == 5
       and int((all_proj.get("data") or {}).get("totalElements") or 0) == 19
       and int((a_proj.get("data") or {}).get("totalElements") or 0) == 5)
checks.append(("1 BOSS 全部门店↔单店切换（19/5 与项目列表一致）", bool(ok1),
               "dashboard 全部=%s A=%s；项目列表 全部=%s A=%s" % (
                   (all_dash.get("data") or {}).get("activeProjectCount", {}).get("value"),
                   (a_dash.get("data") or {}).get("activeProjectCount", {}).get("value"),
                   (all_proj.get("data") or {}).get("totalElements"), (a_proj.get("data") or {}).get("totalElements"))))

# 2) 单门店企业：门店列表仅 1 条（控件不渲染）
s_stores = api(boss_s, "user/store/list", {})
s_list = s_stores.get("data") or []
checks.append(("2 单门店企业控件不渲染（门店列表仅 1 条）", len(s_list) == 1,
               "门店数=%d %s" % (len(s_list), json.dumps(s_list, ensure_ascii=False)[:160])))

# 3) 财务工作台与上线前一致（test vs dev 同数据同口径）
cmp_rows = []
for scope in (None, SM_A, SM_B):
    p = {"month": "2026-10-01"}
    if scope:
        p["storeId"] = scope
    t = (api(boss_m, "finance/summary/report", p).get("data") or {})
    d = (api(boss_m, "finance/summary/report", p, DEV).get("data") or {})
    keys = ("receivableAmount", "receivableCount", "receivedAmount", "outstandingAmount",
            "payableAmount", "unpaidAmount", "projectCount")
    same = all(str(t.get(k)) == str(d.get(k)) for k in keys)
    cmp_rows.append("%s: %s" % (scope or "全部", "一致 ✅" if same else "差异 %s vs %s" % (
        {k: t.get(k) for k in keys}, {k: d.get(k) for k in keys})))
checks.append(("3 财务工作台与上线前一致（test vs dev 三档）", all("一致" in r for r in cmp_rows), "; ".join(cmp_rows)))

# 4) 深链接/非法值：非法 storeId 与他企业门店
bad = api(boss_m, "statistic/boss/dashboard", {"month": "2026-10-01", "storeId": 999999999999})
other = api(boss_m, "statistic/boss/dashboard", {"month": "2026-10-01", "storeId": S + 10})
ok4 = (not bad.get("success")) and (not other.get("success"))
checks.append(("4 深链接/非法值（不存在门店、他企业门店均拒绝）", ok4,
               "非法=%s；他企业=%s" % (json.dumps(bad.get("errorCode"), ensure_ascii=False),
                                    json.dumps(other.get("errorCode"), ensure_ascii=False))))

# 5) 门店主数据 + 停用仍有员工 → 400 明细
stores = api(boss_t, "user/store/list", {}).get("data") or []
fields_ok = all(("status" in st and "sort" in st) for st in stores)
block = api(boss_t, "user/store/status", {"storeId": T2, "status": "Disabled"})
msg = json.dumps(block.get("errorCode"), ensure_ascii=False)
checks.append(("5 门店主数据（status/sort）+ 停用仍有员工 400 明细", fields_ok and (not block.get("success"))
               and "在职员工" in msg, "字段齐全=%s；停用拦截=%s" % (fields_ok, msg)))

# 6) 安全日志可查门店状态变更（先真实停用/启用一次产生记录）
api(boss_t, "company/member/update", {"empId": T + 23, "storeId": T + 10})
api(boss_t, "user/store/status", {"storeId": T3, "status": "Disabled"})
api(boss_t, "user/store/status", {"storeId": T3, "status": "Active"})
api(boss_t, "company/member/update", {"empId": T + 23, "storeId": T3})
logs = api(boss_t, "security/security/log/page", {"current": 1, "size": 20}).get("data") or {}
audit = [r for r in (logs.get("content") or [])
         if str((r.get("action") or {}).get("key")) == "StoreStatusChange"]
checks.append(("6 安全日志可查门店状态变更", len(audit) >= 1,
               "StoreStatusChange 记录 %d 条；示例备注=%s" % (len(audit),
                                                            (audit[0].get("remark") if audit else None))))

print("=" * 118)
print("test 先验冒烟六项（test 实例 :8091 / decoboon-test）  %s" % time.strftime("%F %T"))
print("=" * 118)
for name, ok, detail in checks:
    print("  [%s] %-46s %s" % ("✅" if ok else "❌", name, detail))
print()
print("冒烟汇总：%d/%d 通过%s" % (sum(1 for _, ok, _ in checks if ok), len(checks),
                               "；全部通过 ✅" if all(ok for _, ok, _ in checks) else "；存在未通过 ❌"))
