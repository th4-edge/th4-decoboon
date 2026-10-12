#!/usr/bin/env python3
"""增减项存量 Approved（旧「待客户确认」）收口执行器 —— DEC-068 / PLAN-20261013-001。

设计原则（**必须遵守**）：
1. 只走业务入口 `project/contract-change/customer-confirm`，禁止直接 UPDATE db_contract_change.status；
   生效结算（合同额 / 应收 / 任务 / 材料 / 成本）由服务端 settle() 在同一事务内完成。
2. **默认 dry-run**：不加 --apply 只读状态并打印收口计划，不产生任何写操作。
3. --apply 必须同时提供 --backup（备份路径）作为人工确认凭证，否则拒绝执行。
4. 逐单独立请求、单条失败不中断、幂等（已生效的单直接跳过）。

用法：
  # 0) 先按 docs/operations/data-changes/2026-10-13-增减项存量Approved清点.sql 导出 plan.csv
  #    必需列：change_id（其余列仅用于报告）
  # 目标环境：生产（开发/test 数据不作为依据）。
  # 1) dry-run（默认）
  python3 2026-10-13-增减项存量Approved收口.py --api http://<生产主机>:<端口>/api \\
      --plan plan.csv --accounts accounts.json --out report.csv
  # 2) 正式执行（需备份路径）
  python3 2026-10-13-增减项存量Approved收口.py --api http://<生产主机>:<端口>/api \\
      --plan plan.csv --accounts accounts.json --out report.csv \\
      --apply --backup /opt/decoboon/backups/pre-approve-settle-YYYYmmdd-HHMM.sql

accounts.json 结构（每个企业一个企业负责人账号，满足入口的数据范围校验）：
  {"<com_id>": {"phone": "19900000001", "password": "***"}}
"""
from __future__ import annotations

import argparse
import csv
import json
import sys
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path
from typing import Any

CONFIRM_URI = "project/contract-change/customer-confirm"
INFO_URI = "project/contract-change/info"
LOGIN_URI = "auth/session/password-login"

# 已生效 = 历史字面量 CustomerConfirmed（DEC-068）
EFFECTIVE_STATUSES = {"CustomerConfirmed", "customerConfirmed"}


def now() -> str:
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def status_key(value: Any) -> str:
    """服务端可能返回枚举对象 {key/value}、字符串或 null。"""
    if isinstance(value, dict):
        return str(value.get("key") or value.get("value") or value.get("name") or "")
    return str(value or "")


def http_post(url: str, payload: dict[str, Any], token: str | None = None) -> tuple[bool, Any]:
    """纯标准库 POST（不依赖第三方包）。返回 (是否成功, data 或错误文案)。"""
    headers = {"Content-Type": "application/json"}
    if token:
        headers["token"] = token
    request = urllib.request.Request(url, data=json.dumps(payload).encode("utf-8"),
                                     headers=headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            raw = response.read().decode("utf-8", "ignore")
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", "ignore")
        try:
            body = json.loads(raw)
        except ValueError:
            return False, f"HTTP {error.code}: {raw[:200]}"
        return False, body.get("message") or f"HTTP {error.code}"
    except Exception as error:
        return False, f"{type(error).__name__}: {error}"
    try:
        body = json.loads(raw)
    except ValueError:
        return False, f"非 JSON 响应: {raw[:200]}"
    if body.get("success") is False:
        return False, body.get("message") or json.dumps(body, ensure_ascii=False)
    return True, body.get("data")


class Api:
    def __init__(self, base: str) -> None:
        self.base = base.rstrip("/")
        self.tokens: dict[str, str] = {}

    def login(self, com_id: str, phone: str, password: str) -> str:
        if com_id in self.tokens:
            return self.tokens[com_id]
        ok, data = http_post(f"{self.base}/{LOGIN_URI}", {"phone": phone, "password": password})
        if not ok:
            raise RuntimeError(f"com_id={com_id} 登录失败: {data}")
        self.tokens[com_id] = data["token"]
        return self.tokens[com_id]

    def post(self, com_id: str, uri: str, payload: dict[str, Any]) -> tuple[bool, Any]:
        return http_post(f"{self.base}/{uri}", payload, self.tokens.get(com_id))


def load_plan(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        rows = [row for row in csv.DictReader(handle)]
    if not rows or "change_id" not in rows[0]:
        raise SystemExit(f"计划文件缺少 change_id 列: {path}")
    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description="增减项存量 Approved 收口（默认 dry-run）")
    parser.add_argument("--api", required=True,
                        help="后端 API 基址（生产，例如 http://127.0.0.1:8080/api 或内网域名/api）")
    parser.add_argument("--plan", required=True, help="清点 SQL §2 导出的 CSV（需含 change_id、com_id）")
    parser.add_argument("--accounts", required=True, help="企业负责人账号 JSON：com_id -> {phone,password}")
    parser.add_argument("--out", required=True, help="执行报告 CSV 输出路径")
    parser.add_argument("--apply", action="store_true", help="正式执行（缺省为 dry-run）")
    parser.add_argument("--backup", default="", help="--apply 必填：备份文件路径（人工确认凭证）")
    args = parser.parse_args()

    if args.apply and not args.backup:
        raise SystemExit("拒绝执行：--apply 必须同时提供 --backup <备份路径>")

    plan = load_plan(Path(args.plan))
    accounts = json.loads(Path(args.accounts).read_text(encoding="utf-8"))
    api = Api(args.api)

    mode = "APPLY" if args.apply else "DRY-RUN"
    print(f"[{now()}] 模式={mode} 计划单数={len(plan)} api={args.api}")
    if args.apply:
        print(f"[{now()}] 备份凭证={args.backup}")

    report: list[dict[str, Any]] = []
    counters = {"收口成功": 0, "跳过已生效": 0, "跳过非待收口": 0, "失败": 0}

    for row in plan:
        change_id = str(row.get("change_id", "")).strip()
        com_id = str(row.get("com_id", "")).strip()
        record: dict[str, Any] = {"change_id": change_id, "change_no": row.get("change_no", ""),
                                  "com_id": com_id, "project_id": row.get("project_id", ""),
                                  "amount": row.get("变更金额") or row.get("amount", ""),
                                  "收口前合同额": row.get("收口前合同额", ""), "mode": mode, "结果": "", "详情": ""}
        if not change_id or not com_id:
            record["结果"] = "失败"
            record["详情"] = "计划行缺少 change_id 或 com_id"
            counters["失败"] += 1
            report.append(record)
            continue
        credential = accounts.get(com_id)
        if not credential:
            record["结果"] = "失败"
            record["详情"] = f"accounts.json 缺少 com_id={com_id} 的企业负责人账号"
            counters["失败"] += 1
            report.append(record)
            continue

        try:
            api.login(com_id, credential["phone"], credential["password"])
            ok, data = api.post(com_id, INFO_URI, {"changeId": change_id})
            if not ok:
                record["结果"] = "失败"
                record["详情"] = f"读取状态失败: {data}"
                counters["失败"] += 1
                report.append(record)
                continue
            status = status_key((data or {}).get("status"))
            record["服务端状态"] = status
            if status in EFFECTIVE_STATUSES:
                record["结果"] = "跳过已生效"
                record["详情"] = "幂等：已是已生效，无需处理"
                counters["跳过已生效"] += 1
                report.append(record)
                continue
            if status not in {"Approved", "approved"}:
                record["结果"] = "跳过非待收口"
                record["详情"] = f"当前状态={status or '未知'}，仅 Approved 可收口"
                counters["跳过非待收口"] += 1
                report.append(record)
                continue

            if not args.apply:
                record["结果"] = "计划收口（dry-run 未执行）"
                record["详情"] = "确认后将调用 customer-confirm，由服务端在同一事务内完成合同额/应收/任务/材料/成本联动"
                report.append(record)
                continue

            ok, data = api.post(com_id, CONFIRM_URI, {"changeId": change_id})
            if ok:
                record["结果"] = "收口成功"
                record["详情"] = f"新状态={status_key((data or {}).get('status'))}"
                counters["收口成功"] += 1
            else:
                record["结果"] = "失败"
                record["详情"] = str(data)
                counters["失败"] += 1
        except Exception as error:  # 单条失败不中断整批
            record["结果"] = "失败"
            record["详情"] = f"{type(error).__name__}: {error}"
            counters["失败"] += 1
        report.append(record)

    out = Path(args.out)
    fieldnames = ["change_id", "change_no", "com_id", "project_id", "amount",
                  "收口前合同额", "服务端状态", "mode", "结果", "详情"]
    with out.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        for record in report:
            writer.writerow({key: record.get(key, "") for key in fieldnames})

    print(f"[{now()}] 汇总: " + " / ".join(f"{key}={value}" for key, value in counters.items()))
    print(f"[{now()}] 报告: {out}")
    if not args.apply:
        print("提示：以上为 dry-run，未产生任何写操作；确认清单与备份后再加 --apply 执行。")
    return 1 if counters["失败"] else 0


if __name__ == "__main__":
    sys.exit(main())
