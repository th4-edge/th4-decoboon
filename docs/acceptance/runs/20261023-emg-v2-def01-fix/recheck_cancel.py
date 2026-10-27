import json
import os
import subprocess
import time
from pathlib import Path

ROOT = Path('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon')
EVID = ROOT / 'docs/acceptance/runs/20261023-emg-v2-def01-fix'
BASE = 'http://127.0.0.1:8090/api/'
PASSWORD = os.environ.get('EMG_ACCEPT_PASSWORD')
if not PASSWORD:
    raise SystemExit('missing EMG_ACCEPT_PASSWORD')

def api(route, body, token=None):
    cmd = ['curl', '-sS', '--max-time', '90', '-w', '\n%{http_code}', '-X', 'POST', BASE + route,
           '-H', 'Content-Type: application/json']
    if token:
        cmd += ['-H', 'token: ' + token]
    cmd += ['-d', json.dumps(body, ensure_ascii=False)]
    payload, code = subprocess.check_output(cmd, text=True).rsplit('\n', 1)
    return int(code), json.loads(payload)

def login(phone):
    http, response = api('auth/session/password-login', {'phone': phone, 'password': PASSWORD})
    token = (response.get('data') or {}).get('token')
    if not response.get('success') or not token:
        raise SystemExit(f'login failed: {phone}: {response}')
    return token

def code(response):
    return str((response.get('errorCode') or {}).get('code') or '')

def save(name, route, http, response):
    safe = name.replace('/', '-')
    (EVID / f'{safe}.json').write_text(json.dumps({'route': route, 'http': http, 'response': response}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def sql(statement):
    return subprocess.check_output(['mysql', '--socket=/tmp/syzd-mysql.sock', '-uroot', '-D', 'decoboon_accept', '--batch', '--raw', '-e', statement], text=True)

boss = login('13990000001')
manager = login('13990000002')
suffix = str(int(time.time()) % 1_000_000).zfill(6)
results = {}

# 店长创建并取消自己的 Pending 财务邀请。
phone_manager = '13993' + suffix
h, created_manager = api('invite/invitation/save', {
    'inviteName': 'DEF01 店长取消', 'invitePhone': phone_manager, 'role': 'Finance'
}, manager)
save('def01-manager-create', 'invite/invitation/save', h, created_manager)
manager_invite_id = (created_manager.get('data') or {}).get('inviteId')
h, cancelled_manager = api('invite/invitation/cancel', {
    'inviteId': manager_invite_id, 'reason': 'EMG-V2-DEF-01 店长取消复验'
}, manager)
save('def01-manager-cancel', 'invite/invitation/cancel', h, cancelled_manager)
results['manager_cancel'] = {
    'create_success': bool(created_manager.get('success')),
    'cancel_success': bool(cancelled_manager.get('success')),
    'cancel_code': code(cancelled_manager),
    'status': ((cancelled_manager.get('data') or {}).get('status') or {}).get('key'),
    'inviteId': manager_invite_id,
}

# 店长创建 Pending 邀请，BOSS 取消。
phone_boss = '13994' + suffix
h, created_for_boss = api('invite/invitation/save', {
    'inviteName': 'DEF01 BOSS取消', 'invitePhone': phone_boss, 'role': 'Marketer'
}, manager)
save('def01-boss-target-create', 'invite/invitation/save', h, created_for_boss)
boss_invite_id = (created_for_boss.get('data') or {}).get('inviteId')
h, cancelled_boss = api('invite/invitation/cancel', {
    'inviteId': boss_invite_id, 'reason': 'EMG-V2-DEF-01 BOSS取消复验'
}, boss)
save('def01-boss-cancel', 'invite/invitation/cancel', h, cancelled_boss)
results['boss_cancel'] = {
    'create_success': bool(created_for_boss.get('success')),
    'cancel_success': bool(cancelled_boss.get('success')),
    'cancel_code': code(cancelled_boss),
    'status': ((cancelled_boss.get('data') or {}).get('status') or {}).get('key'),
    'inviteId': boss_invite_id,
}

# 清理独立验收遗留的 Pending 邀请，同时验证 BOSS 可取消历史 Pending。
legacy_invite_id = '1792687401274365047'
h, cancelled_legacy = api('invite/invitation/cancel', {
    'inviteId': legacy_invite_id, 'reason': 'EMG-V2-DEF-01 清理独立验收遗留邀请'
}, boss)
save('def01-legacy-cancel', 'invite/invitation/cancel', h, cancelled_legacy)
results['legacy_cleanup'] = {
    'cancel_success': bool(cancelled_legacy.get('success')),
    'cancel_code': code(cancelled_legacy),
    'status': ((cancelled_legacy.get('data') or {}).get('status') or {}).get('key'),
    'inviteId': legacy_invite_id,
}

ids = [str(value) for value in (manager_invite_id, boss_invite_id, legacy_invite_id) if value]
db_rows = sql(
    "SELECT invite_id,invite_name,role,status,store_id,com_id,cancelled_at "
    "FROM db_tenant_invite WHERE invite_id IN (" + ','.join(ids) + ") ORDER BY invite_id;"
)
(EVID / 'def01-cancelled-db.txt').write_text(db_rows, encoding='utf-8')

passed = (
    all(item['create_success'] and item['cancel_success'] and item['status'] == 'Cancelled'
        for item in (results['manager_cancel'], results['boss_cancel']))
    and results['legacy_cleanup']['cancel_success']
    and results['legacy_cleanup']['status'] == 'Cancelled'
    and all(row.split('\t')[3] == 'Cancelled' for row in db_rows.strip().splitlines()[1:])
)
report = {'at': time.strftime('%Y-%m-%dT%H:%M:%S%z'), 'results': results, 'db': db_rows, 'pass': passed}
(EVID / 'def01-results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, ensure_ascii=False, indent=2))
if not passed:
    raise SystemExit(1)
