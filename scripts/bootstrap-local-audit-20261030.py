#!/usr/bin/env python3
"""创建本轮本地业务驱动测试的唯一测试装企骨架（仅本地 dev）。"""
from __future__ import annotations

import json
import subprocess
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

import bcrypt

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "src/main/resources/application-dev.yml"
MANIFEST = ROOT / "docs/operations/data-changes/2026-10-30-fresh-business-audit-manifest.json"
PASSWORD = "Audit2026!"
NOW = datetime.now(ZoneInfo("Asia/Shanghai"))
COM_ID = 199500000000000001
STORE_A = 199500000000000011
STORE_B = 199500000000000012
TEAM_A = 199500000000000401
SUB_ID = 199500000000000301


def db_password() -> str:
    for line in CONFIG.read_text(encoding="utf-8").splitlines():
        if line.strip().startswith("password:"):
            return line.split(":", 1)[1].strip()
    raise RuntimeError("missing datasource password")


def mysql(sql: str) -> str:
    env = {"MYSQL_PWD": db_password()}
    out = subprocess.run(
        ["mysql", "-h127.0.0.1", "-P3306", "-uroot", "-D", "decoboon", "-N", "-B", "-e", sql],
        capture_output=True, text=True, env={**__import__("os").environ, **env}
    )
    if out.returncode != 0:
        raise RuntimeError(out.stderr.strip())
    return out.stdout.strip()


def q(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def main() -> int:
    if mysql("SELECT COUNT(*) FROM db_tenant_company") != "0":
        raise RuntimeError("目标库不是空租户状态，拒绝重复创建")
    plan = mysql("SELECT p.plan_id,pv.version_id FROM db_tenant_plan p JOIN db_tenant_plan_version pv ON pv.plan_id=p.plan_id WHERE p.code='annual-pro' ORDER BY pv.version_no DESC LIMIT 1").split("\t")
    if len(plan) != 2:
        raise RuntimeError("annual-pro 套餐未初始化")
    plan_id, plan_version_id = plan
    pwd_hash = bcrypt.hashpw(PASSWORD.encode(), bcrypt.gensalt(rounds=10)).decode()
    end = NOW.replace(microsecond=0)
    end = end.replace(year=end.year + 1)
    end_date = int(end.strftime("%Y%m%d"))
    end_time = int(end.strftime("%H%M%S"))
    end_dt = int(end.strftime("%Y%m%d%H%M%S"))

    statements = [
        f"""INSERT INTO db_tenant_company
        (com_id,name,title,size,enable_store,addr,color,brand_primary,custodian_credit,create_date_time)
        VALUES ({COM_ID},{q('昭青家装')},{q('昭青家装')},'A',1,{q('合肥市蜀山区示例路 88 号')},'#5B5CE2','#5B5CE2',0,{int(NOW.timestamp()*1000)})""",
        f"""INSERT INTO db_tenant_store
        (store_id,company_com_id,name,addr,color,default_store,sort,status)
        VALUES ({STORE_A},{COM_ID},{q('总店')},{q('合肥市蜀山区示例路 88 号')},'#5B5CE2',1,0,'Active'),
               ({STORE_B},{COM_ID},{q('滨湖店')},{q('合肥市滨湖新区')},'#5B5CE2',0,1,'Active')""",
    ]

    people = [
        (101, "张伟", "Boss", STORE_A, "13910002001", None),
        (102, "王宏文", "StoreManager", STORE_A, "13910002002", None),
        (103, "吴濛濛", "Finance", STORE_A, "13910002003", None),
        (104, "董夏鹏", "Designer", STORE_A, "13910002004", TEAM_A),
        (105, "黄贤杰", "Marketer", STORE_A, "13910002005", TEAM_A),
        (106, "徐勇", "Pm", STORE_A, "13910002006", TEAM_A),
        (107, "李敏", "TeamLeader", STORE_A, "13910002007", None),
        (108, "陈浩", "StoreManager", STORE_B, "13910002008", None),
        (110, "周雨", "Designer", STORE_A, "13910002009", TEAM_A),
    ]
    employees = []
    for offset, name, role, store_id, tel, team_id in people:
        emp_id = COM_ID + offset
        cre_id = COM_ID + 200 + (offset - 100)
        team_sql = "NULL"
        statements.append(
            f"""INSERT INTO db_tenant_employee
            (emp_id,com_id,store_id,team_id,name,tel_no,role,status,joined_at,avatar_url,invite_id,resigned_at,resign_reason)
            VALUES ({emp_id},{COM_ID},{store_id},{team_sql},{q(name)},{q(tel)},'{role}','Active',NOW(6),NULL,NULL,NULL,NULL)"""
        )
        statements.append(
            f"""INSERT INTO db_tenant_credentials
            (cre_id,emp_id,phone,type,id_key,status,password_hash,password_updated_at,failed_attempts,must_change_password,last_time,locked_until,id_secret)
            VALUES ({cre_id},{emp_id},{q(tel)},'Password',{q('seed-'+tel)},'Active',{q(pwd_hash)},NOW(6),0,0,NULL,NULL,NULL)"""
        )
        employees.append({"empId": str(emp_id), "creId": str(cre_id), "name": name, "role": role,
                          "tel": tel, "storeId": str(store_id), "teamId": team_sql if team_id else None})
    leader_emp = COM_ID + 107
    statements.append(
        f"""INSERT INTO db_tenant_team (team_id,name,color,com_id,store_id,leader_emp_id)
        VALUES ({TEAM_A},{q('设计交付一组')},'#5B5CE2',{COM_ID},{STORE_A},{leader_emp})"""
    )
    statements.append(
        f"UPDATE db_tenant_employee SET team_id={TEAM_A} WHERE emp_id IN ({COM_ID+104},{COM_ID+105},{COM_ID+106},{COM_ID+107},{COM_ID+110})"
    )
    statements.append(
        f"""INSERT INTO db_tenant_subscription
        (sub_id,com_id,plan_id,plan_version_id,status,trial_activated,extra_seats,extra_stores,last_renew_time,
         service_end_date,service_end_time,service_end_date_time,service_end_year,service_end_month,service_end_day,
         service_end_hour,service_end_minute,service_end_second)
        VALUES ({SUB_ID},{COM_ID},{plan_id},{plan_version_id},'Trial',1,0,0,NOW(6),
         {end_date},{end_time},{end_dt},{end.year},{end.month},{end.day},{end.hour},{end.minute},{end.second})"""
    )
    mysql("START TRANSACTION;" + ";\n".join(statements) + "; COMMIT;")
    manifest = {
        "generatedAt": NOW.isoformat(),
        "password": PASSWORD,
        "companies": {
            str(COM_ID): {
                "comId": str(COM_ID),
                "title": "昭青家装",
                "name": "昭青家装",
                "plan": "annual-pro",
                "stores": [
                    {"storeId": str(STORE_A), "name": "总店", "default": True, "status": "Active", "sort": 0},
                    {"storeId": str(STORE_B), "name": "滨湖店", "default": False, "status": "Active", "sort": 1},
                ],
                "employees": employees,
                "teamId": str(TEAM_A),
                "subscriptionId": str(SUB_ID),
            }
        },
    }
    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"companyId": COM_ID, "stores": [STORE_A, STORE_B], "employees": len(employees), "manifest": str(MANIFEST)}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
