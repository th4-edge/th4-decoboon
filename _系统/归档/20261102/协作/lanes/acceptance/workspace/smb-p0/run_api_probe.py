#!/usr/bin/env python3
import json, requests

BASE = "http://127.0.0.1:8090/api/"
PASSWORD = "Maihong123"
PHONES = {
    "Boss": "13910000001",
    "StoreManager": "13910000002",
    "TeamLeader": "13910000003",
    "Marketer": "13910000004",
    "Designer": "13910000005",
    "Pm": "13910000006",
    "ProjectSupport": "13910000007",
    "Finance": "13910000008",
}

def request(route, payload=None, token=None, method="POST"):
    headers = {"Content-Type": "application/json", "User-Agent": "smb-p0-acceptance"}
    if token:
        headers["token"] = token
    try:
        resp = requests.request(method, BASE + route, json=payload if payload is not None else {}, headers=headers, timeout=15)
        try:
            body = resp.json()
        except ValueError:
            body = {"raw": resp.text}
        return resp.status_code, body
    except requests.RequestException as error:
        return 0, {"error": str(error)}

def login(role):
    status, body = request("auth/session/password-login", {"phone": PHONES[role], "password": PASSWORD})
    if status != 200 or not body.get("success"):
        raise RuntimeError(f"login failed for {role}: {status} {body}")
    return body["data"]["token"]

def brief(status, body):
    if isinstance(body, dict):
        code = ((body.get("errorCode") or {}).get("code")) if isinstance(body.get("errorCode"), dict) else body.get("errorCode")
        msg = ((body.get("errorCode") or {}).get("message")) if isinstance(body.get("errorCode"), dict) else None
        return {"status": status, "success": body.get("success"), "code": code, "message": msg or body.get("message")}
    return {"status": status, "body": body}

result = {}
tokens = {r: login(r) for r in PHONES}
result["logins"] = {r: len(t) > 20 for r, t in tokens.items()}

acceptance_id = 226692305615060992
result["employee_acceptance_negative"] = {
    role: brief(*request("project/acceptance/update", {"acceptanceId": acceptance_id, "confirmed": True}, tokens[role]))
    for role in ("Pm", "Boss", "Designer")
}
result["customer_acceptance_without_customer_token"] = brief(*request(
    "customer/acceptance/update", {"acceptanceId": acceptance_id, "confirmed": True}, None
))

contract_id = 226691331676700672
quote_id = 226691266090369024
result["contract_update_negative"] = {
    role: brief(*request("project/contract/update", {"contractId": contract_id, "status": "Effective"}, tokens[role]))
    for role in ("Designer", "StoreManager", "Marketer", "TeamLeader", "Finance", "Pm")
}
result["contract_update_boss"] = brief(*request(
    "project/contract/update", {"contractId": contract_id, "status": "Effective"}, tokens["Boss"]
))
result["contract_save_negative"] = {
    role: brief(*request("project/contract/save", {"quoteId": quote_id}, tokens[role]))
    for role in ("Designer", "StoreManager", "Marketer", "TeamLeader", "Finance", "Pm")
}

project_id = 226691387045707776
stage_id = 226691387070873600
result["pm_measure_page"] = brief(*request("property/measure/page", {"propertyId": 226689976694210560}, tokens["Pm"]))
result["project_finished"] = brief(*request("project/project/info", {"projectId": project_id}, tokens["Pm"]))
result["stage_repeat_on_done"] = brief(*request("project/stage/update", {"stageId": stage_id, "status": "Doing"}, tokens["Pm"]))
result["finance_summary"] = brief(*request("finance/summary/report", {}, tokens["Finance"]))
result["finance_project_page"] = brief(*request("finance/project/page", {"current": 1, "size": 20}, tokens["Finance"]))
result["marketer_report"] = brief(*request("statistic/marketer/report", {}, tokens["Marketer"]))

print(json.dumps(result, ensure_ascii=False, indent=2))
