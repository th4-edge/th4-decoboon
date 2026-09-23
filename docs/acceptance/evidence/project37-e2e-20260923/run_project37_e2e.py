#!/usr/bin/env python3
"""麦鸿演示项目 37 端到端真实验证：开工 → 节点 → 提交验收 → 收款 → 结算。

所有员工侧动作通过本地 dev 后端（127.0.0.1:8090/api）真实 Provider + 本地 MySQL 执行。
业主动作需要真实微信业主授权会话，本地不可获得，脚本只做守卫式反向验证。
"""
from __future__ import annotations

import json
import pathlib
import sys
from datetime import date

import requests

BASE = "http://127.0.0.1:8090/api"
PROJECT_ID = 227467763537412096
CONTRACT_SIGN_RECEIVABLE_ID = 227467764040728576
PASSWORD = "Maihong2026"
ACCOUNTS = {
    "pm": {"phone": "19900000006", "name": "徐勇"},
    "finance": {"phone": "19900000005", "name": "吴濛濛"},
    "designer": {"phone": "19900000003", "name": "谢玲玲"},
}
OUT = pathlib.Path(__file__).parent / "run.json"
EVIDENCE = {
    "taskStart": "/tmp/project37-task-start.png",
    "taskFinish": "/tmp/project37-task-finish.png",
    "stageAccept": "/tmp/project37-stage-accept.png",
}
LOG: list[dict] = []
UPLOADED: dict[str, dict] = {}


def brief(body):
    if not isinstance(body, dict):
        return body
    err = body.get("errorCode")
    if isinstance(err, dict):
        err = {"code": err.get("code"), "message": err.get("message")}
    return {"success": body.get("success"), "errorCode": err, "data": body.get("data")}


def step(name: str, route: str, payload=None, token=None, expect: str | None = None):
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
    if expect:
        entry["expect"] = expect
    LOG.append(entry)
    print(f"[{name}] http={resp.status_code} success={entry['response'].get('success') if isinstance(entry['response'], dict) else '?'} "
          f"{'' if not isinstance(entry['response'], dict) else (entry['response'].get('errorCode') or '')}")
    return body


def upload(name: str, business_type: str, path: str, token: str, project_id: int | None = None):
    data = {"businessType": business_type}
    if project_id:
        data["projectId"] = str(project_id)
    with open(path, "rb") as handle:
        resp = requests.post(f"{BASE}/file/private/grant", headers={"token": token},
                             data=data, files={"file": (pathlib.Path(path).name, handle, "image/png")},
                             timeout=30)
    body = resp.json()
    grants = (body.get("data") or [{}])
    grant = grants[0] if isinstance(grants, list) and grants else {}
    UPLOADED[name] = {"httpStatus": resp.status_code, "success": body.get("success"),
                      "path": grant.get("path"), "name": grant.get("name"), "size": grant.get("size")}
    LOG.append({"step": f"upload:{name}", "route": "file/private/grant",
                "request": {"businessType": business_type, "projectId": project_id, "file": pathlib.Path(path).name},
                "httpStatus": resp.status_code, "response": brief(body)})
    print(f"[upload:{name}] http={resp.status_code} path={grant.get('path')}")
    return grant.get("path")


def login(role: str) -> str:
    account = ACCOUNTS[role]
    resp = requests.post(f"{BASE}/auth/session/password-login",
                         json={"phone": account["phone"], "password": PASSWORD}, timeout=20)
    body = resp.json()
    token = (body.get("data") or {}).get("token")
    if not token:
        raise RuntimeError(f"登录失败 {role}: {body}")
    LOG.append({"step": f"login:{role}", "route": "auth/session/password-login",
                "request": {"phone": account["phone"]}, "httpStatus": resp.status_code,
                "response": {"success": True, "data": {"tokenIssued": True, "name": account["name"]}}})
    print(f"[login:{role}] ok {account['name']}")
    return token


def main() -> int:
    result: dict = {"projectId": PROJECT_ID, "date": date.today().isoformat()}
    pm = login("pm")
    finance = login("finance")
    designer = login("designer")

    result["projectBefore"] = step("project/info(前)", "project/project/info",
                                   {"projectId": PROJECT_ID}, pm).get("data")
    stages = (step("stage/page", "project/stage/page", {"projectId": PROJECT_ID}, pm).get("data") or {})
    stage_list = stages.get("content") if isinstance(stages, dict) else stages
    sort_key = lambda s: s.get("sort", 0)
    stage_list = sorted(stage_list or [], key=sort_key)
    prepare = next((s for s in stage_list if s.get("stageCode") == "PREPARE"), None)
    carpentry = next((s for s in stage_list if s.get("stageCode") == "CARPENTRY"), None)
    result["stages"] = [{"stageId": s.get("stageId"), "name": s.get("name"), "status": s.get("status")} for s in stage_list]

    tasks = (step("task/page", "project/task/page",
                  {"projectId": PROJECT_ID, "stageId": (carpentry or {}).get("stageId")}, pm).get("data") or {})
    task_list = tasks.get("content") if isinstance(tasks, dict) else tasks
    task_list = task_list or []
    result["tasksBefore"] = [{"taskId": t.get("taskId"), "name": t.get("name"), "status": t.get("status"),
                              "conceal": t.get("conceal"), "progress": t.get("progress")} for t in task_list]

    # 1. 阶段开工：首阶段 + 木作吊顶（带任务的阶段）
    for label, stage in (("PREPARE", prepare), ("CARPENTRY", carpentry)):
        if not stage:
            continue
        step(f"stage/update:开工({label})", "project/stage/update",
             {"stageId": stage["stageId"], "status": "Doing", "content": f"端到端验证开工：{label}"}, pm)
    if prepare:
        step("stage/update:重复开工幂等", "project/stage/update",
             {"stageId": prepare["stageId"], "status": "Doing"}, pm, expect="幂等返回已开工，不重复写动态")

    # 2. 工艺任务节点留证 + 任务完成
    uploaded_start = upload("taskStart", "Task", EVIDENCE["taskStart"], pm, PROJECT_ID)
    uploaded_finish = upload("taskFinish", "Task", EVIDENCE["taskFinish"], pm, PROJECT_ID)
    for task in task_list:
        if task.get("status") == "Cancelled":
            step(f"task:跳过已取消({task.get('name')})", "project/task/info",
                 {"taskId": task.get("taskId")}, pm, expect="已取消任务不参与节点留证")
            continue
        step(f"task-node:施工进行({task.get('name')})", "project/task-node/save",
             {"taskId": task.get("taskId"), "key": "Start", "note": "端到端验证：施工进行留证",
              "medias": [{"url": uploaded_start}]}, pm)
        step(f"task-node:施工完成({task.get('name')})", "project/task-node/save",
             {"taskId": task.get("taskId"), "key": "Finish", "note": "端到端验证：施工完成留证",
              "medias": [{"url": uploaded_finish}]}, pm)
        step(f"task/update:完成({task.get('name')})", "project/task/update",
             {"taskId": task.get("taskId"), "status": "Done", "evidenceUrls": [uploaded_finish]}, pm)
    step("task-node:重复节点被拒", "project/task-node/save",
         {"taskId": task_list[0].get("taskId") if task_list else 0, "key": "Start",
          "medias": [{"url": uploaded_start}]}, pm, expect="重复节点 400")

    # 3. 提交阶段验收
    uploaded_accept = upload("stageAccept", "Acceptance", EVIDENCE["stageAccept"], pm, PROJECT_ID)
    acceptance = step("acceptance/save:提交阶段验收", "project/acceptance/save",
                      {"stageId": (carpentry or {}).get("stageId"), "result": "pass",
                       "content": "端到端验证：木作吊顶阶段验收提交",
                       "images": [uploaded_accept]}, pm)
    result["acceptanceSubmit"] = brief(acceptance)

    # 4. 业主人脸与会话守卫（本地无真实微信业主会话，只能做反向验证）
    result["ownerGuardNoToken"] = brief(step("customer/acceptance/update:无 token", "customer/acceptance/update",
                                             {"acceptanceId": (acceptance.get("data") or {}).get("acceptanceId"),
                                              "confirmed": True}).get("errorCode"))
    result["ownerGuardEmployeeToken"] = brief(step("customer/acceptance/update:员工 token 冒充", "customer/acceptance/update",
                                                  {"acceptanceId": (acceptance.get("data") or {}).get("acceptanceId"),
                                                   "confirmed": True}, pm))
    result["employeeConfirmBlocked"] = brief(step("project/acceptance/update:员工代确认", "project/acceptance/update",
                                                 {"acceptanceId": (acceptance.get("data") or {}).get("acceptanceId"),
                                                  "confirmed": True}, pm))

    # 5. 收款：核销合同签约款 + 幂等 + 超额拒绝
    receivables = (step("finance/receivable/page", "finance/receivable/page",
                        {"projectId": PROJECT_ID, "current": 1, "size": 20}, finance).get("data") or {})
    rows = receivables.get("content") if isinstance(receivables, dict) else receivables
    result["receivablesBefore"] = [{"receivableId": r.get("receivableId"), "name": r.get("name"),
                                    "amount": r.get("amount"), "receivedAmount": r.get("receivedAmount"),
                                    "status": r.get("status")} for r in (rows or [])]
    receipt_payload = {"projectId": PROJECT_ID, "receivableId": CONTRACT_SIGN_RECEIVABLE_ID,
                       "amount": 10675.77, "account": "CompanyBank", "receivedAt": date.today().isoformat(),
                       "payerName": "端到端验证-业主", "remark": "项目 37 端到端验证收款"}
    first = step("finance/receipt/save:合同签约款全额核销", "finance/receipt/save", receipt_payload, finance)
    result["receiptFirst"] = brief(first)
    again = step("finance/receipt/save:重复提交幂等", "finance/receipt/save", receipt_payload, finance,
                 expect="返回同一收款记录，不重复核销")
    result["receiptRepeat"] = brief(again)
    step("finance/receipt/save:超额收款被拒", "finance/receipt/save",
         {**receipt_payload, "amount": 999999.99, "idempotencyKey": "overpay-probe"}, finance,
         expect="超过应收余额 400")

    # 6. 结算快照
    settlement = step("finance/settlement/save:生成结算", "finance/settlement/save",
                      {"projectId": PROJECT_ID, "retentionRate": 3, "deductionAmount": 0,
                       "rewardAmount": 0, "remark": "项目 37 端到端验证结算"}, finance)
    result["settlement"] = brief(settlement)
    result["projectAfter"] = step("project/info(后)", "project/project/info",
                                  {"projectId": PROJECT_ID}, pm).get("data")
    result["stagesAfter"] = [{"name": s.get("name"), "status": s.get("status"), "progress": s.get("progress")}
                             for s in ((step("stage/page(后)", "project/stage/page",
                                             {"projectId": PROJECT_ID}, pm).get("data") or {}).get("content") or [])]
    result["tasksAfter"] = [{"name": t.get("name"), "status": t.get("status"), "progress": t.get("progress")}
                            for t in ((step("task/page(后)", "project/task/page",
                                            {"projectId": PROJECT_ID}, pm).get("data") or {}).get("content") or [])]

    OUT.write_text(json.dumps({"result": result, "uploads": UPLOADED, "log": LOG},
                              ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\n证据已写入 {OUT}")
    print(json.dumps({"acceptanceSubmit": result["acceptanceSubmit"],
                      "receiptFirst": result["receiptFirst"],
                      "settlement": result["settlement"],
                      "projectAfter": (result["projectAfter"] or {}).get("status")},
                     ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
