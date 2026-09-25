#!/usr/bin/env python3
"""校验 App 全链路测试数据构造结果，输出可复核 JSON。"""

from __future__ import annotations

import json
import subprocess
from pathlib import Path

import requests

API = "http://127.0.0.1:8090/api"
PASSWORD = "Maihong2026"
ACCOUNTS = {
    "storeManager": "19900000003",
    "marketer": "19900000004",
    "designerDong": "19900000005",
    "designerXie": "19900000006",
    "finance": "19900000002",
}


def mysql_rows(sql: str) -> list[list[str]]:
    output = subprocess.check_output(
        ["mysql", "-h127.0.0.1", "-uroot", "-pmysqladmin", "decoboon", "-N", "-B", "-e", sql],
        text=True,
        stderr=subprocess.DEVNULL,
    )
    return [line.split("\t") for line in output.splitlines() if line.strip()]


def scalar(sql: str) -> int:
    rows = mysql_rows(sql)
    return int(rows[0][0]) if rows else 0


def page_items(data):
    if isinstance(data, list):
        return data
    if isinstance(data, dict):
        for key in ("content", "list", "records", "items"):
            if isinstance(data.get(key), list):
                return data[key]
    return []


def enum_key(value):
    if isinstance(value, dict):
        return str(value.get("key") or value.get("value") or value.get("name") or "")
    return str(value or "")


DEMO_PROJECTS = [
    int(row[0])
    for row in mysql_rows(
        "SELECT project_id FROM db_project WHERE name LIKE '麦鸿演示项目 %' ORDER BY name"
    )
]
PROJECT_SQL = ",".join(str(item) for item in DEMO_PROJECTS)


class Api:
    def __init__(self):
        self.tokens = {}
        for role, phone in ACCOUNTS.items():
            body = requests.post(f"{API}/auth/session/password-login", json={"phone": phone, "password": PASSWORD}, timeout=20).json()
            if not body.get("success"):
                raise RuntimeError(f"登录失败 {role}: {body}")
            self.tokens[role] = body["data"]["token"]

    def post(self, role: str, uri: str, payload=None):
        body = requests.post(
            f"{API}/{uri}",
            headers={"token": self.tokens[role]},
            json=payload or {},
            timeout=40,
        ).json()
        if not body.get("success"):
            raise RuntimeError(f"{role} {uri} 失败: {body}")
        return body.get("data")


def main() -> int:
    failures: list[str] = []
    evidence: dict[str, object] = {"batch": "MH-FULL-ROLE-20260924"}

    # 1. 每个项目必须有一条已确认选材，且原合同材料计划已绑定该选材。
    selection_rows = mysql_rows(f"""
        SELECT mp.project_id, mp.status, mp.selection_id, s.status
        FROM db_material_plan mp
        LEFT JOIN db_material_selection s ON s.selection_id = mp.selection_id
        WHERE mp.project_id IN ({PROJECT_SQL}) AND mp.contract_line_id IS NOT NULL
        ORDER BY mp.project_id
    """)
    evidence["selectionRows"] = selection_rows
    confirmed_selection_projects = {
        int(project_id)
        for project_id, plan_status, selection_id, selection_status in selection_rows
        if selection_id not in ("", "NULL") and selection_status == "Confirmed"
    }
    if confirmed_selection_projects != set(DEMO_PROJECTS):
        failures.append(f"已确认选材项目不完整: {sorted(confirmed_selection_projects)}")

    # 2. 采购、到场、应付账期闭环；37~39 已到场，40~42 已下单待到货，43~44 待下单。
    purchase_rows = mysql_rows(f"""
        SELECT po.project_id, po.status, po.quantity, po.received_quantity, pay.amount, pay.due_date
        FROM db_purchase_order po
        LEFT JOIN db_finance_payable pay ON pay.purchase_order_id = po.purchase_id
        WHERE po.project_id IN ({PROJECT_SQL})
        ORDER BY po.project_id
    """)
    evidence["purchaseRows"] = purchase_rows
    purchase_by_project = {int(row[0]): row[1:] for row in purchase_rows}
    for project_id in (37, 38, 39, 40, 41, 42):
        project_key = DEMO_PROJECTS[project_id - 37]
        if project_key not in purchase_by_project:
            failures.append(f"项目 {project_id} 缺少采购单")
            continue
        status, quantity, received, payable_amount, due_date = purchase_by_project[project_key]
        if project_id in (37, 38, 39):
            if status != "Received" or float(received) != float(quantity):
                failures.append(f"项目 {project_id} 到货状态不符: {purchase_by_project[project_key]}")
            if due_date in ("", "NULL"):
                failures.append(f"项目 {project_id} 到场后应付 due_date 仍为空")
        else:
            if status != "Ordered":
                failures.append(f"项目 {project_id} 应为已下单待到货: {purchase_by_project[project_key]}")
            if due_date not in ("", "NULL"):
                failures.append(f"项目 {project_id} 未到场但已回填 due_date")
        if payable_amount in ("", "NULL"):
            failures.append(f"项目 {project_id} 下单后未形成应付")
    for project_id in (43, 44):
        project_key = DEMO_PROJECTS[project_id - 37]
        if project_key in purchase_by_project:
            failures.append(f"项目 {project_id} 应保留待下单，实际已有采购单")

    plan_rows = mysql_rows(f"""
        SELECT project_id, plan_id, status, quantity, selection_id
        FROM db_material_plan
        WHERE project_id IN ({PROJECT_SQL}) AND contract_line_id IS NOT NULL
        ORDER BY project_id
    """)
    evidence["planRows"] = plan_rows
    plan_by_project = {int(row[0]): row[1:] for row in plan_rows}
    for project_id in (37, 38, 39, 43, 44):
        project_key = DEMO_PROJECTS[project_id - 37]
        status = plan_by_project.get(project_key, ["", "", ""])[1]
        expected = "Arrived" if project_id in (37, 38, 39) else "ReadyToOrder"
        if status != expected:
            failures.append(f"项目 {project_id} 材料计划状态应为 {expected}，实际 {status}")
    for project_id in (43, 44):
        project_key = DEMO_PROJECTS[project_id - 37]
        quantity = float(plan_by_project.get(project_key, ["0", "0", "0"])[2])
        if quantity != 27.0:
            failures.append(f"项目 {project_id} 数量变更未落到材料计划: {quantity}")

    # 3. 增减项：每项目至少 Add + Remove；项目38保留一条待审批；合同金额与已客户确认变更一致。
    change_rows = mysql_rows(f"""
        SELECT project_id, type, status, amount, cost_delta, reason
        FROM db_contract_change
        WHERE project_id IN ({PROJECT_SQL})
        ORDER BY project_id, change_id
    """)
    evidence["changeRows"] = change_rows
    by_project: dict[int, list[list[str]]] = {}
    for row in change_rows:
        by_project.setdefault(int(row[0]), []).append(row)
    for project_id in DEMO_PROJECTS:
        rows = by_project.get(project_id, [])
        types = {row[1] for row in rows}
        if not {"Add", "Remove"}.issubset(types):
            failures.append(f"项目 {project_id} 缺少增项或减项: {sorted(types)}")
        if len(rows) < 2:
            failures.append(f"项目 {project_id} 变更单不足 2 条")
    status_counts: dict[str, int] = {}
    for row in change_rows:
        status_counts[row[2]] = status_counts.get(row[2], 0) + 1
    evidence["changeStatusCounts"] = status_counts
    if status_counts.get("CustomerConfirmed", 0) < 10:
        failures.append(f"客户已确认变更不足 10 条: {status_counts}")
    if status_counts.get("Approved", 0) < 2:
        failures.append(f"待客户确认变更不足 2 条: {status_counts}")
    if status_counts.get("Pending", 0) < 1:
        failures.append(f"待审批变更不足 1 条: {status_counts}")

    contract_math = mysql_rows(f"""
        SELECT c.project_id, p.contract_amount, c.baseline_amount, c.current_amount,
               COALESCE(SUM(CASE WHEN ch.status = 'CustomerConfirmed' THEN ch.amount ELSE 0 END), 0) AS confirmed_delta
        FROM db_project_contract c
        JOIN db_project p ON p.project_id = c.project_id
        LEFT JOIN db_contract_change ch ON ch.contract_id = c.contract_id
        WHERE c.project_id IN ({PROJECT_SQL})
        GROUP BY c.contract_id, c.project_id, p.contract_amount, c.baseline_amount, c.current_amount
        ORDER BY c.project_id
    """)
    evidence["contractMath"] = contract_math
    for project_id, project_amount, baseline, current, delta in contract_math:
        expected = round(float(baseline) + float(delta), 2)
        if round(float(current), 2) != expected:
            failures.append(f"项目 {project_id} 合同当前额不等于基线和已确认变更之和: {baseline}+{delta}!={current}")
        if round(float(project_amount), 2) != round(float(current), 2):
            failures.append(f"项目 {project_id} 项目合同额未同步: project={project_amount}, contract={current}")

    # 4. 员工数据量：每个人都有 >=2 条可进入 App 的有效事实。
    employee_counts = {
        "黄贤杰/业务员-录入客户": scalar("SELECT COUNT(*) FROM cust_customer WHERE name LIKE '%（演示%'"),
        "黄贤杰/业务员-跟进": scalar("SELECT COUNT(*) FROM cust_customer_follow f JOIN cust_customer c ON c.cust_id=f.customer_id WHERE c.name LIKE '%（演示%'"),
        "黄贤杰/业务员-量房": scalar("SELECT COUNT(*) FROM db_measure m JOIN cust_customer c ON c.cust_id=m.customer_id WHERE c.name LIKE '%（演示%'"),
        "谢玲玲/设计师-方案": scalar("SELECT COUNT(*) FROM db_design d JOIN db_tenant_employee e ON e.emp_id=d.emp_id WHERE e.name='谢玲玲'"),
        "谢玲玲/设计师-报价": scalar("SELECT COUNT(*) FROM quote q JOIN db_tenant_employee e ON e.emp_id=q.emp_id WHERE e.name='谢玲玲'"),
        "谢玲玲/设计师-选材": scalar("SELECT COUNT(*) FROM db_material_selection s JOIN db_tenant_employee e ON e.emp_id=s.designer_id WHERE e.name='谢玲玲'"),
        "谢玲玲/设计师-变更": scalar("SELECT COUNT(*) FROM db_contract_change ch JOIN db_tenant_employee e ON e.emp_id=ch.requester_id WHERE e.name='谢玲玲'"),
        "董夏鹏/设计师-方案": scalar("SELECT COUNT(*) FROM db_design d JOIN db_tenant_employee e ON e.emp_id=d.emp_id WHERE e.name='董夏鹏'"),
        "董夏鹏/设计师-报价": scalar("SELECT COUNT(*) FROM quote q JOIN db_tenant_employee e ON e.emp_id=q.emp_id WHERE e.name='董夏鹏'"),
        "董夏鹏/设计师-选材": scalar("SELECT COUNT(*) FROM db_material_selection s JOIN db_tenant_employee e ON e.emp_id=s.designer_id WHERE e.name='董夏鹏'"),
        "董夏鹏/设计师-变更": scalar("SELECT COUNT(*) FROM db_contract_change ch JOIN db_tenant_employee e ON e.emp_id=ch.requester_id WHERE e.name='董夏鹏'"),
        "吴濛濛/财务-采购下单": scalar("SELECT COUNT(*) FROM db_purchase_order po JOIN db_tenant_employee e ON e.emp_id=po.creator_id WHERE e.name='吴濛濛'"),
        "吴濛濛/财务-到场确认": scalar("SELECT COUNT(*) FROM db_goods_receipt gr JOIN db_tenant_employee e ON e.emp_id=gr.inspector_id WHERE e.name='吴濛濛'"),
        "吴濛濛/财务-变更审批": scalar("SELECT COUNT(*) FROM db_contract_change ch JOIN db_tenant_employee e ON e.emp_id=ch.approver_id WHERE e.name='吴濛濛'"),
        "钱小明/BOSS-合同": scalar("SELECT COUNT(*) FROM db_project_contract c JOIN db_tenant_employee e ON e.emp_id=c.creator_id WHERE e.name='钱小明' AND c.project_id IN (" + PROJECT_SQL + ")"),
        "徐勇/项目经理-项目": scalar("SELECT COUNT(*) FROM db_project p JOIN db_tenant_employee e ON e.emp_id=p.manager_id WHERE e.name='徐勇' AND p.project_id IN (" + PROJECT_SQL + ")"),
        "王昌富/项目经理-项目": scalar("SELECT COUNT(*) FROM db_project p JOIN db_tenant_employee e ON e.emp_id=p.manager_id WHERE e.name='王昌富' AND p.project_id IN (" + PROJECT_SQL + ")"),
        "麦鸿演示材料商/供应关系": scalar("SELECT COUNT(*) FROM db_material_supplier ms JOIN db_partner p ON p.partner_id=ms.partner_id WHERE p.name='麦鸿演示材料商'"),
    }
    evidence["employeeCounts"] = employee_counts
    for label, count in employee_counts.items():
        if count < 2:
            failures.append(f"{label} 不足 2 条: {count}")

    # 5. App 实际调用：工作台、选材分项、项目采购和变更列表均能读到真实数据。
    api = Api()
    workbench_xie = page_items(api.post("designerXie", "material/plan/workbench", {}))
    workbench_dong = page_items(api.post("designerDong", "material/plan/workbench", {}))
    evidence["workbenchProjects"] = {"xie": len(workbench_xie), "dong": len(workbench_dong)}
    if len(workbench_xie) < 2 or len(workbench_dong) < 2:
        failures.append(f"设计师材料计划工作台项目不足: xie={len(workbench_xie)}, dong={len(workbench_dong)}")
    store_customers = page_items(api.post("storeManager", "customer/customer/page", {"current": 1, "size": 200}))
    evidence["storeManagerCustomers"] = len(store_customers)
    if len(store_customers) < 2:
        failures.append(f"店长客户列表不足 2 条: {len(store_customers)}")
    api_checks = []
    for project_id in DEMO_PROJECTS:
        project = api.post("finance", "project/project/info", {"projectId": str(project_id)})
        role = "designerXie" if project["designer"]["name"] == "谢玲玲" else "designerDong"
        selection_items = page_items(api.post(role, "material/selection/item/page", {"projectId": str(project_id), "current": 1, "size": 100}))
        changes = page_items(api.post(role, "project/contract-change/page", {"projectId": str(project_id), "current": 1, "size": 100}))
        api_checks.append({
            "projectId": str(project_id),
            "name": project.get("name"),
            "selectionItems": len(selection_items),
            "confirmedSelections": sum(1 for item in selection_items if enum_key(item.get("status")) == "Confirmed" or item.get("status") == "Confirmed"),
            "changes": len(changes),
        })
    evidence["apiChecks"] = api_checks
    for item in api_checks:
        if item["confirmedSelections"] < 1:
            failures.append(f"{item['name']} App 选材分项无已确认记录")
        if item["changes"] < 2:
            failures.append(f"{item['name']} App 变更列表不足 2 条")

    result = {"failures": failures, "evidence": evidence}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
