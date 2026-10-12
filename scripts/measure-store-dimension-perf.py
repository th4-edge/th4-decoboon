#!/usr/bin/env python3
"""门店维度统计接口性能采集（A11，仅 dev）。

口径（按业务侧测量纪律）：
- **端到端**：curl 真实 HTTP（含 Provider 过滤器与 JSON 序列化），不是方法内部读数；
- 每接口 **3 次重复取中位数**；
- **SQL 条数**：MySQL `general_log`（TABLE 输出）按请求时间窗计数（窗口内其它连接写入已尽量排除）；
- 返回行数：统计数组长度；响应体：字节数。

用法：/opt/anaconda3/bin/python3 scripts/measure-store-dimension-perf.py <label> <outJson> [storeId]
"""
from __future__ import annotations
import json, subprocess, sys, time
from pathlib import Path

import os
BASE = os.environ.get("DECOBOON_API_BASE", "http://127.0.0.1:8090/api")
DB = ["mysql", "-h127.0.0.1", "-P3306", "-uroot", "-pmysqladmin", os.environ.get("DECOBOON_DB", "decoboon"), "-N", "-B"]
ENDPOINTS = [
    ("statistic/boss/dashboard", {"month": "2026-10-01"}),
    ("statistic/boss/business/report", {"month": "2026-10-01", "topN": 5}),
    ("statistic/organization/report", {"month": "2026-10-01", "topN": 5}),
    ("statistic/performance/report", {"month": "2026-10-01"}),
    ("statistic/store/report", {}),
]


def q(sql: str) -> str:
    return subprocess.run(DB + ["-e", sql], capture_output=True, text=True).stdout.strip()


def login(tel: str = "13910000100") -> str:
    out = subprocess.run(["curl", "-s", "-X", "POST", BASE + "/auth/session/password-login",
                          "-H", "Content-Type: application/json",
                          "-d", json.dumps({"phone": tel, "password": "Zdq123456"})],
                         capture_output=True, text=True).stdout
    return json.loads(out)["data"]["token"]


def post(token: str, route: str, payload: dict) -> tuple[float, int, str]:
    body = json.dumps(payload, ensure_ascii=False)
    t0 = time.time()
    out = subprocess.run(["curl", "-s", "-X", "POST", "%s/%s" % (BASE, route),
                          "-H", "Content-Type: application/json", "-H", "token: " + token, "-d", body],
                         capture_output=True, text=True).stdout
    return (time.time() - t0) * 1000.0, len(out.encode()), out


def rows_of(route: str, body: str) -> int:
    try:
        data = json.loads(body).get("data") or {}
    except json.JSONDecodeError:
        return -1
    if route == "statistic/store/report":
        return len(data.get("designerPerformances") or []) + len(data.get("marketerPerformances") or []) \
            + len(data.get("pmPerformances") or [])
    if route == "statistic/performance/report":
        return len(data.get("performances") or [])
    if route == "statistic/boss/business/report":
        return len(data.get("channels") or []) + len(data.get("regions") or [])
    if route == "statistic/organization/report":
        return len(data.get("designerRanking") or []) + len(data.get("marketerRanking") or [])
    return len(data)


def sql_count(route: str, payload: dict, token: str) -> int:
    q("set global log_output='TABLE'")
    q("set global general_log=1")
    q("truncate table mysql.general_log")
    post(token, route, payload)          # 预热（不进计数）
    q("truncate table mysql.general_log")
    post(token, route, payload)
    n = int(q("select count(*) from mysql.general_log where command_type='Query' "
              "and argument not like '%general_log%'"))
    q("set global general_log=0")
    return n


def pct(vals: list[float], p: float) -> float:
    s = sorted(vals)
    return s[min(len(s) - 1, int(round(p * (len(s) - 1))))]


def main() -> int:
    label, out_path = sys.argv[1], sys.argv[2]
    store_id = sys.argv[3] if len(sys.argv) > 3 else None
    token = login()
    result = {"label": label, "storeId": store_id, "machine": {"cpu": q("select @@version_comment").strip()},
              "endpoints": []}
    for route, payload in ENDPOINTS:
        p = dict(payload)
        if store_id:
            p["storeId"] = store_id
        times, sizes, rows = [], [], 0
        for i in range(3):
            ms, size, body = post(token, route, p)
            times.append(ms); sizes.append(size); rows = rows_of(route, body)
        result["endpoints"].append({
            "route": route, "reps": [round(t, 1) for t in times],
            "median_ms": round(sorted(times)[1], 1), "p95_ms": round(max(times), 1),
            "bytes": sizes[-1], "rows": rows, "sqlCount": sql_count(route, p, token)})
        print(route, result["endpoints"][-1])
    Path(out_path).write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print("->", out_path)
    return 0


if __name__ == "__main__":
    sys.exit(main())
