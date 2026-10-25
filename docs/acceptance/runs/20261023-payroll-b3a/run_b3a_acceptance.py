import json
import subprocess
from pathlib import Path

ROOT = Path('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon')
EVID = ROOT / 'docs/acceptance/runs/20261023-payroll-b3a'
HTTP = EVID / 'http'
SQL = EVID / 'sql'
BASE = 'http://127.0.0.1:8090/api/'
SOCKET = Path('/tmp/th4-b3a-mysql-live.sock')
DB = Path('/tmp/th4-b3a-db-name').read_text().strip()
MYSQL = ['mysql', f'--socket={SOCKET}', '-uroot', '-pmysqladmin', DB, '--batch', '--raw', '-N']
PASSWORD = 'B3AAccept123'
PHONES = {'boss': '19855162372', 'finance': '15156075712', 'manager': '18055190856'}
SHEET_SEP = 239338424358666240
PERIOD_SEP = '2026-09'
PERIOD_DELETE = '2026-10'
results = []

def api(route, body, token=None):
    cmd = ['curl', '-sS', '--max-time', '30', '-w', '\n%{http_code}', '-X', 'POST', BASE + route,
           '-H', 'Content-Type: application/json']
    if token:
        cmd += ['-H', 'token: ' + token]
    cmd += ['-d', json.dumps(body, ensure_ascii=False)]
    raw = subprocess.check_output(cmd, text=True)
    payload, status = raw.rsplit('\n', 1)
    return int(status), json.loads(payload)

def sql(statement):
    return subprocess.check_output(MYSQL + ['-e', statement], text=True, stderr=subprocess.DEVNULL)

def save(name, route, status, payload):
    (HTTP / f'{name}.json').write_text(json.dumps({'route': route, 'http': status, 'response': payload}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def code(payload):
    return str((payload.get('errorCode') or {}).get('code') or '')

def data(payload):
    return payload.get('data') or {}

def record(case, expected, actual, passed, evidence):
    results.append({'case': case, 'expected': expected, 'actual': actual, 'result': 'PASS' if passed else 'FAIL', 'evidence': evidence})

tokens = {}
for role, phone in PHONES.items():
    status, payload = api('auth/session/password-login', {'phone': phone, 'password': PASSWORD})
    assert payload.get('success'), (role, status, payload)
    tokens[role] = payload['data']['token']

# 1. Boss detail and full item composition.
status, payload = api('payroll/sheet/info', {'sheetId': SHEET_SEP}, tokens['boss'])
save('B3A-01-boss-sheet-info', 'payroll/sheet/info', status, payload)
d = data(payload)
record('B3A-01', 'Confirmed sheet, complete attendance, totals present',
       {'success': payload.get('success'), 'status': (d.get('status') or {}).get('key'),
        'attendanceComplete': d.get('attendanceComplete'), 'gross': d.get('totalGrossAmount'),
        'deduction': d.get('totalDeductionAmount'), 'net': d.get('totalNetAmount')},
       payload.get('success') and (d.get('status') or {}).get('key') == 'Confirmed'
       and d.get('attendanceComplete') is True and d.get('totalGrossAmount') == 21000.0
       and d.get('totalDeductionAmount') == 0.0 and d.get('totalNetAmount') == 21000.0,
       'B3A-01-boss-sheet-info.json')

status, payload = api('payroll/item/page', {'sheetId': SHEET_SEP, 'current': 1, 'size': 200}, tokens['boss'])
save('B3A-02-boss-items', 'payroll/item/page', status, payload)
items = (data(payload).get('content') or [])
required = {'baseSalaryAmount', 'attendanceDeduction', 'commissionAmount', 'bonusAmount', 'allowanceAmount',
            'socialInsuranceAmount', 'taxAmount', 'otherDeduction', 'grossAmount', 'deductionAmount', 'netAmount', 'status'}
record('B3A-02', '5 complete payroll items with amount fields and attribution',
       {'success': payload.get('success'), 'count': len(items),
        'allAmountFields': all(required.issubset(set(item)) for item in items),
        'allHaveEmployee': all((item.get('employee') or {}).get('name') for item in items),
        'allHaveAttribution': all((item.get('attribution') or {}).get('key') for item in items)},
       payload.get('success') and len(items) == 5 and all(required.issubset(set(item)) for item in items)
       and all((item.get('employee') or {}).get('name') for item in items)
       and all((item.get('attribution') or {}).get('key') for item in items),
       'B3A-02-boss-items.json')

# 2. Finance and StoreManager can read but cannot confirm/delete/build.
for role, prefix in [('finance', '03'), ('manager', '04')]:
    for suffix, route, body in [
        ('sheet-info', 'payroll/sheet/info', {'sheetId': SHEET_SEP}),
        ('items', 'payroll/item/page', {'sheetId': SHEET_SEP, 'current': 1, 'size': 200})
    ]:
        status, payload = api(route, body, tokens[role])
        save(f'B3A-{prefix}-{suffix}', route, status, payload)
        record(f'B3A-{prefix}-{suffix}', 'read success', {'success': payload.get('success'), 'code': code(payload)},
               payload.get('success'), f'B3A-{prefix}-{suffix}.json')

for role, prefix in [('finance', '05'), ('manager', '06')]:
    for suffix, route, body in [
        ('confirm', 'payroll/sheet/update', {'sheetId': SHEET_SEP}),
        ('delete', 'payroll/sheet/delete', {'sheetId': SHEET_SEP}),
        ('build', 'payroll/sheet/build', {'period': '2026-12'})
    ]:
        status, payload = api(route, body, tokens[role])
        save(f'B3A-{prefix}-{suffix}', route, status, payload)
        if role == 'finance' and suffix == 'build':
            record(f'B3A-{prefix}-{suffix}', 'Finance may build a draft', {
                'success': payload.get('success'), 'code': code(payload),
                'status': (data(payload).get('status') or {}).get('key')
            }, payload.get('success') and (data(payload).get('status') or {}).get('key') == 'Draft',
                f'B3A-{prefix}-{suffix}.json')
        else:
            record(f'B3A-{prefix}-{suffix}', '403', {'success': payload.get('success'), 'code': code(payload)},
                   not payload.get('success') and code(payload) == '403', f'B3A-{prefix}-{suffix}.json')

# 3. Confirmed sheet cannot be deleted.
status, payload = api('payroll/sheet/delete', {'sheetId': SHEET_SEP}, tokens['boss'])
save('B3A-07-delete-confirmed-rejected', 'payroll/sheet/delete', status, payload)
record('B3A-07', '409', {'success': payload.get('success'), 'code': code(payload), 'message': (payload.get('errorCode') or {}).get('message')},
       not payload.get('success') and code(payload) == '409', 'B3A-07-delete-confirmed-rejected.json')

# 4. Repeat confirmation is idempotent: payable and payslip facts stay at one per item/employee.
before = sql(
    "SELECT (SELECT COUNT(*) FROM db_finance_payable WHERE source_type='PayrollItem' AND source_id IN "
    f"(SELECT item_id FROM db_payroll_item WHERE sheet_id={SHEET_SEP})), "
    f"(SELECT COUNT(*) FROM db_payroll_payslip WHERE sheet_id={SHEET_SEP});")
(SQL / 'B3A-08-facts-before.txt').write_text(before, encoding='utf-8')
status, payload = api('payroll/sheet/update', {'sheetId': SHEET_SEP}, tokens['boss'])
save('B3A-08-repeat-confirm', 'payroll/sheet/update', status, payload)
after = sql(
    "SELECT (SELECT COUNT(*) FROM db_finance_payable WHERE source_type='PayrollItem' AND source_id IN "
    f"(SELECT item_id FROM db_payroll_item WHERE sheet_id={SHEET_SEP})), "
    f"(SELECT COUNT(*) FROM db_payroll_payslip WHERE sheet_id={SHEET_SEP});")
(SQL / 'B3A-08-facts-after.txt').write_text(after, encoding='utf-8')
record('B3A-08', 'repeat confirm leaves 5 payables and 5 payslips unchanged',
       {'success': payload.get('success'), 'code': code(payload), 'before': before.strip(), 'after': after.strip()},
       payload.get('success') and before == after and before.strip() == '5\t5',
       'B3A-08-repeat-confirm.json; sql/B3A-08-facts-before.txt; sql/B3A-08-facts-after.txt')

# 5. Rebuild a disposable draft and delete it as Boss.
status, payload = api('payroll/sheet/build', {'period': PERIOD_DELETE}, tokens['finance'])
save('B3A-09-build-draft', 'payroll/sheet/build', status, payload)
draft_id = data(payload).get('sheetId')
record('B3A-09', 'draft created', {'success': payload.get('success'), 'sheetId': draft_id, 'status': (data(payload).get('status') or {}).get('key')},
       payload.get('success') and draft_id and (data(payload).get('status') or {}).get('key') == 'Draft', 'B3A-09-build-draft.json')

if draft_id:
    status, payload = api('payroll/sheet/delete', {'sheetId': draft_id}, tokens['boss'])
    save('B3A-10-delete-draft', 'payroll/sheet/delete', status, payload)
    record('B3A-10', 'draft deleted', {'success': payload.get('success'), 'code': code(payload)},
           payload.get('success'), 'B3A-10-delete-draft.json')
    status, payload = api('payroll/sheet/info', {'sheetId': draft_id}, tokens['boss'])
    save('B3A-11-deleted-hidden', 'payroll/sheet/info', status, payload)
    record('B3A-11', '404 after delete', {'success': payload.get('success'), 'code': code(payload)},
           not payload.get('success') and code(payload) == '404', 'B3A-11-deleted-hidden.json')

# 6. Header totals equal item sums.
amounts = sql(
    f"SELECT (SELECT total_gross_amount FROM db_payroll_sheet WHERE sheet_id={SHEET_SEP}), "
    f"(SELECT SUM(gross_amount) FROM db_payroll_item WHERE sheet_id={SHEET_SEP}), "
    f"(SELECT total_deduction_amount FROM db_payroll_sheet WHERE sheet_id={SHEET_SEP}), "
    f"(SELECT SUM(deduction_amount) FROM db_payroll_item WHERE sheet_id={SHEET_SEP}), "
    f"(SELECT total_net_amount FROM db_payroll_sheet WHERE sheet_id={SHEET_SEP}), "
    f"(SELECT SUM(net_amount) FROM db_payroll_item WHERE sheet_id={SHEET_SEP});")
(SQL / 'B3A-12-amount-reconciliation.txt').write_text(amounts, encoding='utf-8')
values = amounts.strip().split('\t')
record('B3A-12', 'sheet gross/deduction/net equal item sums',
       {'values': values}, len(values) == 6 and values[0] == values[1] and values[2] == values[3] and values[4] == values[5],
       'sql/B3A-12-amount-reconciliation.txt')

# 7. Store-scoped roles cannot read another store's known sheet ID.
OTHER_STORE_SHEET = 239338999999999992
status, payload = api('payroll/sheet/info', {'sheetId': OTHER_STORE_SHEET}, tokens['manager'])
save('B3A-13-manager-cross-store-hidden', 'payroll/sheet/info', status, payload)
record('B3A-13', '404 for StoreManager reading another store by known ID',
       {'success': payload.get('success'), 'code': code(payload)},
       not payload.get('success') and code(payload) == '404', 'B3A-13-manager-cross-store-hidden.json')

status, payload = api('payroll/sheet/info', {'sheetId': OTHER_STORE_SHEET}, tokens['finance'])
save('B3A-14-hq-finance-cross-store-visible', 'payroll/sheet/info', status, payload)
record('B3A-14', 'HQ Finance retains company-scope read',
       {'success': payload.get('success'), 'code': code(payload),
        'storeSheet': data(payload).get('sheetId')},
       payload.get('success') and str(data(payload).get('sheetId')) == str(OTHER_STORE_SHEET),
       'B3A-14-hq-finance-cross-store-visible.json')

summary = {'passed': sum(1 for r in results if r['result'] == 'PASS'),
           'failed': sum(1 for r in results if r['result'] == 'FAIL'), 'results': results}
(EVID / 'api-results.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(summary, ensure_ascii=False, indent=2))
