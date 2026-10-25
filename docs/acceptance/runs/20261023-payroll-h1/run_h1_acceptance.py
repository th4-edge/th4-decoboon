import json, os, subprocess
from pathlib import Path

ROOT = Path('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon')
EVID = ROOT / 'docs/acceptance/runs/20261023-payroll-h1'
HTTP = EVID / 'http'
SQL = EVID / 'sql'
BASE = 'http://127.0.0.1:8090/api/'
MYSQL = ['mysql', '--socket=' + Path('/tmp/th4-h1-mysql-path').read_text().strip() + '/mysql.sock', '-uroot', 'decoboon_h1_schema', '--batch', '--raw']
PASSWORD = 'H1Accept123'
PHONES = {
    'boss': '18880000001',
    'finance': '18880000002',
    'manager': '18880000003',
    'store2finance': '18880000006',
}
results = []

def api(route, body, token=None):
    cmd = ['curl', '-sS', '--max-time', '30', '-w', '\n%{http_code}', '-X', 'POST', BASE + route,
           '-H', 'Content-Type: application/json']
    if token:
        cmd += ['-H', 'token: ' + token]
    cmd += ['-d', json.dumps(body, ensure_ascii=False)]
    raw = subprocess.check_output(cmd, text=True)
    payload, code = raw.rsplit('\n', 1)
    return int(code), json.loads(payload)

def sql(statement):
    return subprocess.check_output(MYSQL + ['-e', statement], text=True)

def save(name, route, status, payload):
    (HTTP / f'{name}.json').write_text(json.dumps({'route': route, 'http': status, 'response': payload}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def errcode(payload):
    return str((payload.get('errorCode') or {}).get('code') or '')

def record(case, expected, actual, passed, evidence):
    results.append({'case': case, 'expected': expected, 'actual': actual, 'result': 'PASS' if passed else 'FAIL', 'evidence': evidence})

tokens = {}
for role, phone in PHONES.items():
    status, payload = api('auth/session/password-login', {'phone': phone, 'password': PASSWORD})
    save(f'login-{role}', 'auth/session/password-login', status, {**payload, 'data': {'token': '<redacted>' if payload.get('data', {}).get('token') else None}})
    assert payload.get('success'), (role, payload)
    tokens[role] = payload['data']['token']

# H1-03 attendance scope
for case, role, summary_id, expected_code in [
    ('H1-03-01', 'manager', 9100000000000000501, None),
    ('H1-03-02', 'manager', 9100000000000000502, '404'),
    ('H1-03-03', 'finance', 9100000000000000501, None),
    ('H1-03-04', 'finance', 9100000000000000502, None),
    ('H1-03-05', 'store2finance', 9100000000000000502, None),
    ('H1-03-06', 'store2finance', 9100000000000000501, '404'),
    ('H1-03-07', 'boss', 9100000000000000503, '404'),
    ('H1-03-08', 'boss', 9100000000000000502, None),
]:
    status, payload = api('attendance/summary/info', {'summaryId': summary_id}, tokens[role])
    save(case, 'attendance/summary/info', status, payload)
    ok = payload.get('success') if expected_code is None else (not payload.get('success') and errcode(payload) == expected_code)
    record(case, 'success' if expected_code is None else expected_code,
           {'success': payload.get('success'), 'code': errcode(payload), 'summaryId': summary_id},
           ok, f'{case}.json')

# H1-01 preconditions. Finance must not confirm.
status, payload = api('payroll/sheet/update', {'sheetId': 9100000000000000701}, tokens['finance'])
save('H1-01-01-finance-rejected', 'payroll/sheet/update', status, payload)
record('H1-01-01', '403', {'success': payload.get('success'), 'code': errcode(payload)}, not payload.get('success') and errcode(payload) == '403', 'H1-01-01-finance-rejected.json')

for case, sheet_id, expected_message in [
    ('H1-01-02', 9100000000000000702, '考勤未确认'),
    ('H1-01-03', 9100000000000000703, '没有工资项'),
    ('H1-01-04', 9100000000000000704, '实发为负'),
]:
    status, p = api('payroll/sheet/update', {'sheetId': sheet_id}, tokens['boss'])
    save(case, 'payroll/sheet/update', status, p)
    message = str((p.get('errorCode') or {}).get('message') or '')
    record(case, expected_message, {'success': p.get('success'), 'code': errcode(p), 'message': message}, not p.get('success') and expected_message in message, f'{case}.json')

before = sql("SELECT (SELECT COUNT(*) FROM db_finance_payable WHERE source_type='PayrollItem' AND source_id=9100000000000000801), (SELECT COUNT(*) FROM db_payroll_payslip WHERE sheet_id=9100000000000000701);")
(SQL / 'H1-01-before.txt').write_text(before, encoding='utf-8')
status, payload = api('payroll/sheet/update', {'sheetId': 9100000000000000701}, tokens['boss'])
save('H1-01-05-boss-confirm', 'payroll/sheet/update', status, payload)
record('H1-01-05', 'success despite commissionComplete=false', {'success': payload.get('success'), 'code': errcode(payload), 'status': ((payload.get('data') or {}).get('status') or {}).get('key'), 'commissionComplete': (payload.get('data') or {}).get('commissionComplete')}, payload.get('success') and ((payload.get('data') or {}).get('status') or {}).get('key') == 'Confirmed' and (payload.get('data') or {}).get('commissionComplete') is False, 'H1-01-05-boss-confirm.json')
after1 = sql("SELECT (SELECT COUNT(*) FROM db_finance_payable WHERE source_type='PayrollItem' AND source_id=9100000000000000801), (SELECT COUNT(*) FROM db_payroll_payslip WHERE sheet_id=9100000000000000701);")
(SQL / 'H1-01-after-first.txt').write_text(after1, encoding='utf-8')

status, payload2 = api('payroll/sheet/update', {'sheetId': 9100000000000000701}, tokens['boss'])
save('H1-01-06-idempotent', 'payroll/sheet/update', status, payload2)
after2 = sql("SELECT (SELECT COUNT(*) FROM db_finance_payable WHERE source_type='PayrollItem' AND source_id=9100000000000000801), (SELECT COUNT(*) FROM db_payroll_payslip WHERE sheet_id=9100000000000000701);")
(SQL / 'H1-01-after-second.txt').write_text(after2, encoding='utf-8')
record('H1-01-06', 'repeat confirm leaves counts unchanged', {'first': after1.strip(), 'second': after2.strip(), 'success': payload2.get('success')}, payload2.get('success') and after1 == after2, 'H1-01-06-idempotent.json; sql/H1-01-after-first.txt; sql/H1-01-after-second.txt')

summary = {'passed': sum(1 for r in results if r['result'] == 'PASS'), 'failed': sum(1 for r in results if r['result'] == 'FAIL'), 'results': results}
(EVID / 'api-results.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(summary, ensure_ascii=False, indent=2))
