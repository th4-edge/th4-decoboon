#!/usr/bin/env python3
"""P0/P1 修复后的真实链路复验（项目 37）。

- DEF-PROJ37-001：`project/task-node/save` 三节点可用性（本脚本在项目 37 新建临时工艺任务做
  Start 节点留证、重复节点、非法节点，验证后由 SQL 清理临时任务）
- DEF-PROJ37-002：同一笔收款的重复提交必须返回原收款记录，新的超额事实仍被阻断
"""
from __future__ import annotations

import json
import pathlib
import sys
from datetime import date

import requests

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8093/api"
PROJECT_ID = 227467763537412096
RECEIVABLE_ID = 227467764040728576  # 合同签约款，已全额核销（10,675.77）
ORIGINAL_RECEIPT_ID = 228219731256016896
PASSWORD = "Maihong2026"
OUT = pathlib.Path(__file__).parent / "reverify-after-fix.json"
LOG: list[dict] = []


def brief(body):
    if not isinstance(body, dict):
        return body
    err = body.get("errorCode")
    if isinstance(err, dict):
        err = {"code": err.get("code"), "message": err.get("message")}
    return {"success": body.get("success"), "errorCode": err, "data": body.get("data")}


def step(name: str, route: str, payload=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["token"] = token
    resp = requests.post(f"{BASE}/{route}", json=payload or {}, headers=headers, timeout=30)
    try:
        body = resp.json()
    except ValueError:
        body = {"raw": resp.text}
    entry = {"step": name, "route": route, "request": payload, "httpStatus": resp.status_code,
             "response": brief(body)}
    LOG.append(entry)
    result = entry["response"]
    code = (result.get("errorCode") or {}).get("code") if isinstance(result, dict) else ""
    print(f"[{name}] http={resp.status_code} success={result.get('success') if isinstance(result, dict) else '?'} {code or ''}")
    return body


def login(phone: str) -> str:
    body = step(f"login:{phone}", "auth/session/password-login", {"phone": phone, "password": PASSWORD}).get("data") or {}
    token = body.get("token")
    if not token:
        raise RuntimeError(f"登录失败 {phone}")
    return token


def upload(path: str, token: str) -> str:
    with open(path, "rb") as handle:
        resp = requests.post(f"{BASE}/file/private/grant", headers={"token": token},
                             data={"businessType": "Task", "projectId": str(PROJECT_ID)},
                             files={"file": (pathlib.Path(path).name, handle, "image/png")}, timeout=30)
    body = resp.json()
    grant = (body.get("data") or [{}])[0]
    LOG.append({"step": "upload:task", "route": "file/private/grant",
                "request": {"businessType": "Task", "file": pathlib.Path(path).name},
                "httpStatus": resp.status_code, "response": brief(body)})
    print(f"[upload:task] http={resp.status_code} path={grant.get('path')}")
    return grant.get("path")


def main() -> int:
    result: dict = {"projectId": PROJECT_ID, "base": BASE, "date": date.today().isoformat()}

    pm = login("19900000006")
    finance = login("19900000005")

    stages = (step("stage/page", "project/stage/page", {"projectId": PROJECT_ID}, pm).get("data") or {})
    stage_list = stages.get("content") if isinstance(stages, dict) else stages
    carpentry = next((s for s in (stage_list or []) if s.get("stageCode") == "CARPENTRY"), {})
    result["stage"] = {"stageId": carpentry.get("stageId"), "status": carpentry.get("status")}

    # 复用项目内既有施工任务引用的工艺库条目，避免额外目录查询参数差异
    tasks = (step("task/page", "project/task/page",
                  {"projectId": PROJECT_ID, "stageId": carpentry.get("stageId")}, pm).get("data") or {})
    task_list = tasks.get("content") if isinstance(tasks, dict) else tasks
    source_task = next((t for t in (task_list or []) if t.get("catalogItem")), {})
    item = source_task.get("catalogItem") or {}
    result["catalogItem"] = {"itemId": item.get("itemId"), "name": item.get("name"),
                             "unit": item.get("unit"), "from": "project/task/page"}

    created = (step("task/save:临时复验任务", "project/task/save",
                    {"projectId": PROJECT_ID, "stageId": carpentry.get("stageId"),
                     "catalogId": item.get("itemId"), "zoneId": "LivingRoom", "quantity": 1,
                     "plannedStart": carpentry.get("plannedStart"), "plannedEnd": carpentry.get("plannedEnd"),
                     "name": "复验临时工艺任务（P0 回归）", "priority": "normal",
                     "remark": "DEF-PROJ37-001 复验用临时任务，复验后清理"}, pm).get("data") or {})
    task_id = created.get("taskId")
    result["tempTaskId"] = task_id
    if not task_id:
        raise RuntimeError("临时任务创建失败，无法复验节点接口")

    evidence = upload("/tmp/project37-task-start.png", pm)

    start = step("task-node/save:Start", "project/task-node/save",
                 {"taskId": task_id, "key": "Start", "note": "P0 修复复验：施工进行留证",
                  "medias": [{"url": evidence}]}, pm)
    result["nodeStart"] = brief(start)
    after = (step("task/info:Start 后", "project/task/info", {"taskId": task_id}, pm).get("data") or {})
    result["taskAfterStart"] = {"status": after.get("status"), "progress": after.get("progress")}

    duplicate = step("task-node/save:重复 Start", "project/task-node/save",
                     {"taskId": task_id, "key": "Start", "note": "重复留证",
                      "medias": [{"url": evidence}]}, pm)
    result["nodeDuplicate"] = brief(duplicate)

    invalid = step("task-node/save:非法节点", "project/task-node/save",
                   {"taskId": task_id, "key": "NotANode", "medias": [{"url": evidence}]}, pm)
    result["nodeInvalid"] = brief(invalid)

    # 收款幂等：与 2026-09-23 02:23 首次收款完全相同的业务事实
    payload = {"projectId": PROJECT_ID, "receivableId": RECEIVABLE_ID, "amount": 10675.77,
               "account": "CompanyBank", "receivedAt": date.today().isoformat(),
               "payerName": "端到端验证-业主", "remark": "项目 37 端到端验证收款"}
    repeat = step("finance/receipt/save:重复提交", "finance/receipt/save", payload, finance)
    result["receiptRepeat"] = brief(repeat)
    result["receiptIdMatchesOriginal"] = str((repeat.get("data") or {}).get("receiptId")) == str(ORIGINAL_RECEIPT_ID)

    over = step("finance/receipt/save:新事实超额", "finance/receipt/save",
                {**payload, "amount": 100.00, "payerName": "端到端验证-业主乙"}, finance)
    result["receiptOverPayment"] = brief(over)

    OUT.write_text(json.dumps({"result": result, "log": LOG}, ensure_ascii=False, indent=2), encoding="utf-8")
    print("\n复验结果：")
    print(json.dumps(result, ensure_ascii=False, indent=2)[:2000])
    print(f"\n证据：{OUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
