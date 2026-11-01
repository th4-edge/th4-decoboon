#!/usr/bin/env python3
"""PAY-OPS-01 薪酬工作台 10× MySQL 数据与并发压测。

仅允许本地 dev 库。脚本会创建 950... 开头的隔离合成 ID 段，并在结束时清理。
"""
from __future__ import annotations

import asyncio
import json
import math
import os
import statistics
import subprocess
import time
from pathlib import Path

import aiohttp
import requests

ROOT = Path(__file__).resolve().parent.parent
API = os.environ.get('DECOBOON_API_BASE', 'http://127.0.0.1:8091/api')
OUT = ROOT / 'docs/acceptance/runs/20261031-pay-ops-01/raw/mysql-10x-benchmark.json'
COM = 199500000000000001
STORE = 199500000000000011
BOSS = 199500000000000102
PHONE = '13910002001'
PASSWORD = 'Audit2026!'
CURRENT_PERIOD = '2026-10'
PERIODS = ['2025-11','2025-12','2026-01','2026-02','2026-03','2026-04',
           '2026-05','2026-06','2026-07','2026-08','2026-09','2026-10']
BASE = 950_000_000_000_000_000


def load_db_env():
    env = dict(os.environ)
    env_file = ROOT / '.env.dev.local'
    if env_file.exists():
        for line in env_file.read_text().splitlines():
            if line.startswith('export '):
                key, value = line[7:].split('=', 1)
                env[key] = value.strip().strip("'")
    return env


DB_ENV = load_db_env()
MYSQL = ['mysql', '-h127.0.0.1', '-u' + DB_ENV['DB_USERNAME'], '-p' + DB_ENV['DB_PASSWORD'], 'decoboon']


def mysql_exec(sql: str):
    proc = subprocess.run(MYSQL, input=sql, text=True, capture_output=True)
    if proc.returncode:
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip())


def mysql_scalar(sql: str) -> str:
    return subprocess.check_output(MYSQL + ['-N', '-B', '-e', sql], text=True,
                                   stderr=subprocess.DEVNULL).strip()


def cleanup():
    mysql_exec(f"""
START TRANSACTION;
DELETE FROM db_finance_payable WHERE payable_id BETWEEN {BASE + 700000} AND {BASE + 799999};
DELETE FROM db_payroll_payslip WHERE payslip_id BETWEEN {BASE + 500000} AND {BASE + 599999};
DELETE FROM db_payroll_commission_item WHERE commission_id BETWEEN {BASE + 600000} AND {BASE + 699999};
DELETE FROM db_payroll_item WHERE item_id BETWEEN {BASE + 400000} AND {BASE + 499999};
DELETE FROM db_payroll_sheet WHERE sheet_id BETWEEN {BASE + 300000} AND {BASE + 399999};
DELETE FROM db_payroll_attendance_summary WHERE summary_id BETWEEN {BASE + 200000} AND {BASE + 299999};
DELETE FROM db_payroll_compensation_scheme WHERE scheme_id BETWEEN {BASE + 1000} AND {BASE + 1999};
DELETE FROM db_tenant_employee WHERE emp_id BETWEEN {BASE + 1} AND {BASE + 999};
COMMIT;
""")


def insert_values(table: str, columns: list[str], rows: list[tuple]):
    def sql(v):
        if v is None:
            return 'NULL'
        if isinstance(v, (int, float)):
            return str(v)
        return "'" + str(v).replace("'", "''") + "'"
    values = ','.join('(' + ','.join(sql(v) for v in row) + ')' for row in rows)
    mysql_exec(f"INSERT INTO {table} ({','.join(columns)}) VALUES {values};")


def seed(employee_count: int):
    employees = [BASE + n for n in range(1, employee_count + 1)]
    insert_values('db_tenant_employee', ['emp_id','name','role','status','tel_no','com_id','store_id'], [
        (emp, f'PERF薪酬{n:03d}', 'Marketer', 'Active', f'1880000{n:04d}', COM, STORE)
        for n, emp in enumerate(employees, 1)
    ])
    insert_values('db_payroll_compensation_scheme',
                  ['scheme_id','base_salary','effective_from','scheduled_days','status','com_id','creator_id','emp_id','store_id','remark'],
                  [(BASE + 1000 + n, 5000.00, '2025-01-01', 22, 'Active', COM, BOSS, emp, STORE, 'PAY-OPS-01 10X')
                   for n, emp in enumerate(employees, 1)])

    sheet_by_period = {}
    for pi, period in enumerate(PERIODS):
        sheet_id = BASE + 300000 + pi
        sheet_by_period[period] = sheet_id
        status = 'Draft' if period == CURRENT_PERIOD else 'Paid'
        totals = 5000.00 * employee_count
        insert_values('db_payroll_sheet',
                      ['sheet_id','attendance_complete','commission_complete','period','status',
                       'total_deduction_amount','total_gross_amount','total_net_amount','com_id','creator_id','owner_id','store_id'],
                      [(sheet_id, 0 if period == CURRENT_PERIOD else 1, 0, period, status,
                        0.00, totals, totals, COM, BOSS, BOSS, STORE)])

    att_rows, item_rows, payslip_rows, commission_rows, payable_rows = [], [], [], [], []
    for pi, period in enumerate(PERIODS):
        sheet_id = sheet_by_period[period]
        for n, emp in enumerate(employees, 1):
            if period == CURRENT_PERIOD:
                status = 'Pending' if n % 4 == 1 else ('Disputed' if n % 4 == 2 else 'Confirmed')
            else:
                status = 'Confirmed'
            att_rows.append((BASE + 200000 + pi * 1000 + n, 0, 0, 0, 0, 0, 0, 0,
                             period, 22, status, COM, BOSS, emp, STORE, 'PAY-OPS-01 10X'))
            item_id = BASE + 400000 + pi * 1000 + n
            item_rows.append((item_id, 0.00, 0.00, 0.00, 5000.00, 0.00, 0.00, 5000.00, 5000.00, 0.00,
                              'Company', 'COMPANY', 0.00, 0.00, 'Draft' if period == CURRENT_PERIOD else 'Paid',
                              COM, BOSS, emp, sheet_id, STORE, 'PAY-OPS-01 10X'))
            payslip_status = 'PendingPayment' if period == CURRENT_PERIOD else 'Published'
            payslip_rows.append((BASE + 500000 + pi * 1000 + n, 0.00, 5000.00, 5000.00, period,
                                 None if period == CURRENT_PERIOD else '2026-10-01 00:00:00',
                                 payslip_status, COM, BOSS, emp, sheet_id, STORE))
            commission_id = BASE + 600000 + pi * 1000 + n
            commission_rows.append((commission_id, 100.00, 100.00, 10000.00, 1.0000, 40.00, 100.00,
                                    'Released', COM, BOSS, emp, item_id, STORE, 'PAY-OPS-01 10X'))
            payable_rows.append((BASE + 700000 + pi * 1000 + n, 100.00, 'New', '2026-10-31', 0,
                                 'PAY-OPS-01 提成应付', 40.00, f'PERF{n:03d}', period, 'PAY-OPS-01 10X',
                                 commission_id, 'PERF-COMMISSION', 'Pending', 'EmployeeCommission', 0,
                                 COM, BOSS, emp, emp, STORE, None, None, None))
        # current-period salary payable, one per employee
        if period == CURRENT_PERIOD:
            for n, emp in enumerate(employees, 1):
                item_id = BASE + 400000 + pi * 1000 + n
                payable_rows.append((BASE + 750000 + n, 5000.00, 'New', '2026-10-31', 0,
                                     'PAY-OPS-01 工资应付', 0.00, f'PERF{n:03d}', period, 'PAY-OPS-01 10X',
                                     item_id, 'PERF-SALARY', 'Pending', 'EmployeeSalary', 0,
                                     COM, BOSS, emp, emp, STORE, None, None, None))

    insert_values('db_payroll_attendance_summary',
                  ['summary_id','absence_days','annual_leave_days','comp_leave_days','external_days',
                   'normal_days','personal_leave_days','sick_leave_days','payroll_month','scheduled_days',
                   'status','com_id','creator_id','emp_id','store_id','remark'], att_rows)
    insert_values('db_payroll_item',
                  ['item_id','allowance_amount','attendance_deduction','bonus_amount','base_salary_amount',
                   'commission_amount','deduction_amount','gross_amount','net_amount','other_deduction',
                   'attribution','attribution_key','social_insurance_amount','tax_amount','status',
                   'com_id','creator_id','emp_id','sheet_id','store_id','remark'], item_rows)
    insert_values('db_payroll_payslip',
                  ['payslip_id','deduction_amount','gross_amount','net_amount','period','published_at',
                   'status','com_id','creator_id','emp_id','sheet_id','store_id'], payslip_rows)
    insert_values('db_payroll_commission_item',
                  ['commission_id','accrued_amount','commission_amount','contract_amount','manual_rate',
                   'paid_amount','released_amount','status','com_id','creator_id','emp_id','payroll_item_id',
                   'store_id','remark'], commission_rows)
    insert_values('db_finance_payable',
                  ['payable_id','amount','business_origin','due_date','estimated','name','paid_amount',
                   'payee_name','period','remark','source_id','source_type','status','type','urgent',
                   'com_id','creator_id','owner_id','payee_emp_id','store_id','project_id','property_id','purchase_order_id'], payable_rows)


def status_value(name: str) -> int:
    return int(mysql_scalar(f"SHOW GLOBAL STATUS LIKE '{name}'").split()[-1])


async def measure_sql(token: str):
    async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=10)) as session:
        await one_request(session, token)
        before = status_value('Questions')
        result = await one_request(session, token)
        after = status_value('Questions')
        return {'questions_delta': after - before, 'status': result[1], 'success': result[3],
                'response_bytes': result[2]}


def login() -> str:
    response = requests.post(API + '/auth/session/password-login', json={'phone': PHONE, 'password': PASSWORD}, timeout=10)
    response.raise_for_status()
    payload = response.json()
    if not payload.get('success'):
        raise RuntimeError('登录失败')
    return payload['data']['token']


async def one_request(session: aiohttp.ClientSession, token: str):
    started = time.perf_counter()
    async with session.post(API + '/payroll/workbench/report', json={'period': CURRENT_PERIOD},
                            headers={'token': token}) as response:
        body = await response.read()
        elapsed = (time.perf_counter() - started) * 1000
        try:
            parsed = json.loads(body)
        except Exception:
            parsed = {}
        return elapsed, response.status, len(body), bool(parsed.get('success'))


def pct(values, p):
    ordered = sorted(values)
    idx = max(0, math.ceil(len(ordered) * p / 100) - 1)
    return ordered[min(idx, len(ordered) - 1)]


def summarize(samples, duration_ms=None):
    times = [x[0] for x in samples]
    return {
        'samples': len(samples),
        'p50_ms': round(pct(times, 50), 2),
        'p95_ms': round(pct(times, 95), 2),
        'p99_ms': round(pct(times, 99), 2),
        'max_ms': round(max(times), 2),
        'avg_ms': round(statistics.mean(times), 2),
        'error_count': sum(1 for x in samples if x[1] != 200 or not x[3]),
        'error_rate': round(sum(1 for x in samples if x[1] != 200 or not x[3]) / len(samples), 6),
        'response_bytes_last': samples[-1][2],
        'throughput_rps': round(len(samples) / (duration_ms / 1000), 2) if duration_ms else None,
    }


async def sequential(token: str, count: int = 30):
    timeout = aiohttp.ClientTimeout(total=10)
    async with aiohttp.ClientSession(timeout=timeout) as session:
        for _ in range(10):
            await one_request(session, token)
        samples = []
        for _ in range(count):
            samples.append(await one_request(session, token))
        return summarize(samples)


async def concurrent(token: str, total: int = 200, concurrency: int = 20):
    timeout = aiohttp.ClientTimeout(total=20)
    sem = asyncio.Semaphore(concurrency)
    async with aiohttp.ClientSession(timeout=timeout) as session:
        async def worker():
            async with sem:
                return await one_request(session, token)
        started = time.perf_counter()
        samples = await asyncio.gather(*[worker() for _ in range(total)])
        elapsed_ms = (time.perf_counter() - started) * 1000
        return summarize(samples, elapsed_ms)


def data_counts():
    tables = {
        'employees': f"SELECT COUNT(*) FROM db_tenant_employee WHERE emp_id BETWEEN {BASE + 1} AND {BASE + 999}",
        'attendance': f"SELECT COUNT(*) FROM db_payroll_attendance_summary WHERE summary_id BETWEEN {BASE + 200000} AND {BASE + 299999}",
        'sheets': f"SELECT COUNT(*) FROM db_payroll_sheet WHERE sheet_id BETWEEN {BASE + 300000} AND {BASE + 399999}",
        'items': f"SELECT COUNT(*) FROM db_payroll_item WHERE item_id BETWEEN {BASE + 400000} AND {BASE + 499999}",
        'payslips': f"SELECT COUNT(*) FROM db_payroll_payslip WHERE payslip_id BETWEEN {BASE + 500000} AND {BASE + 599999}",
        'commissions': f"SELECT COUNT(*) FROM db_payroll_commission_item WHERE commission_id BETWEEN {BASE + 600000} AND {BASE + 699999}",
        'payables': f"SELECT COUNT(*) FROM db_finance_payable WHERE payable_id BETWEEN {BASE + 700000} AND {BASE + 799999}",
    }
    return {k: int(mysql_scalar(v)) for k, v in tables.items()}


def main():
    result = {'api': API, 'period': CURRENT_PERIOD, 'dataset': {}, 'sequence': {}, 'cleanup': {}}
    cleanup()
    try:
        seed(8)
        result['dataset']['baseline'] = data_counts()
        token = login()
        result['sequence']['baseline_8_employees'] = asyncio.run(sequential(token))
        result['sql_status'] = {'baseline': asyncio.run(measure_sql(token))}

        cleanup()
        seed(80)
        result['dataset']['ten_x'] = data_counts()
        token = login()
        result['sequence']['ten_x_sequential'] = asyncio.run(sequential(token))
        result['sequence']['ten_x_concurrent_20x200'] = asyncio.run(concurrent(token))
        result['sql_status']['ten_x'] = asyncio.run(measure_sql(token))
    finally:
        cleanup()
        result['cleanup']['after'] = data_counts()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
