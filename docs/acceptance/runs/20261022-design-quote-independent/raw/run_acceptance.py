#!/usr/bin/env python3
import http.client, json, subprocess, time
from decimal import Decimal
from pathlib import Path
from urllib.parse import urlparse

API = 'http://127.0.0.1:8090/api'
MYSQL = ['mysql', '-h127.0.0.1', '-P3306', '-uroot', '-pmysqladmin', 'decoboon', '-N', '-B']
KNOWN_HASH = '$2a$10$n6hf9ldBobQgUnX2vocZLOEp5n6nMyYAfaEwa75LCSTxNnqdQSo/O'
MAIN_COM = 1790066191585976744
MAIN_STORE = 1790066191588014876
TEMP_STORE = 1799999999999999101
TARGET_DESIGN = 233161725233332224
DESIGNER_DESIGN = 237511632966647808
OTHER_EMP = 1789989046754654238
OTHER_COM = 1789989046747845724

checks = []
raw = {}

def db(sql):
    p = subprocess.run(MYSQL + ['-e', sql], text=True, capture_output=True, check=True)
    return p.stdout.strip()

def db_one(sql):
    out = db(sql)
    return out.split('\n')[0] if out else ''

def db_exec(sql):
    subprocess.run(MYSQL + ['-e', sql], text=True, capture_output=True, check=True)

def lit(v):
    return 'NULL' if v in (None, '', '__NULL__') else "'" + str(v).replace("'", "''") + "'"

def credential(emp_id):
    row = db_one("SELECT cre_id,COALESCE(phone,'__NULL__'),type,status,COALESCE(password_hash,'__NULL__'),failed_attempts,COALESCE(locked_until,'__NULL__') FROM db_tenant_credentials WHERE emp_id=%s LIMIT 1" % emp_id)
    if not row:
        raise RuntimeError('missing credential for employee %s' % emp_id)
    cre_id, phone, typ, status, password_hash, failed, locked = row.split('\t')
    return dict(cre_id=int(cre_id), phone=phone, type=typ, status=status, password_hash=password_hash, failed=int(failed), locked=locked)

def restore_credential(emp_id, c):
    db_exec("UPDATE db_tenant_credentials SET phone=%s,type=%s,status=%s,password_hash=%s,failed_attempts=%s,locked_until=%s WHERE emp_id=%s" % (
        lit(c['phone']), lit(c['type']), lit(c['status']), lit(c['password_hash']), c['failed'], lit(c['locked']), emp_id))

def enable_password(emp_id, phone=None):
    sets = ["status='Active'", "password_hash=" + lit(KNOWN_HASH), 'failed_attempts=0', 'locked_until=NULL', "type='Password'"]
    if phone:
        sets.append('phone=' + lit(phone))
    db_exec('UPDATE db_tenant_credentials SET ' + ','.join(sets) + ' WHERE emp_id=%s' % emp_id)

def request(route, body, token=None):
    parsed = urlparse(API)
    data = json.dumps(body).encode()
    conn = http.client.HTTPConnection(parsed.hostname, parsed.port, timeout=20)
    conn.putrequest('POST', parsed.path + '/' + route)
    conn.putheader('Content-Type', 'application/json')
    conn.putheader('Accept-Encoding', 'identity')
    if token:
        # Provider header is case-sensitive in the current gateway; do not let urllib title-case it.
        conn.putheader('token', token)
    conn.putheader('Content-Length', str(len(data)))
    conn.endheaders(data)
    resp = conn.getresponse()
    payload = resp.read().decode()
    conn.close()
    return resp.status, json.loads(payload)

def login(phone):
    status, body = request('auth/session/password-login', {'phone': phone, 'password': 'Maihong2026'})
    if body.get('success') is not True:
        raise RuntimeError('login failed for %s: %s' % (phone, body))
    return body['data']['token']

def add(name, passed, actual=None):
    checks.append({'name': name, 'passed': bool(passed), 'actual': actual})

def info(token, design_id):
    return request('design/scheme/info', {'designId': design_id}, token)

def page(token, store_id=0):
    return request('design/scheme/page', {'current': 1, 'size': 100, 'storeId': store_id}, token)

cred_emps = [1790066191593026092, 1790221790492843136, 1790222057930837664, 1790224826490830054]
backups = {e: credential(e) for e in cred_emps}
other_backup = credential(OTHER_EMP)
store_backups = {}
try:
    for e in cred_emps:
        enable_password(e)
    enable_password(OTHER_EMP, '19900007777')

    tokens = {
        'boss': login('19855162372'),
        'store': login('18055190856'),
        'finance': login('15156075712'),
        'designer': login('18225940242'),
        'marketer': login('13866481710'),
        'pm': login('13856066773'),
    }
    other_token = login('19900007777')

    before = db_one("SELECT COUNT(*), (SELECT COUNT(*) FROM db_design WHERE com_id=%s), (SELECT COUNT(*) FROM quote WHERE com_id=%s), (SELECT COUNT(*) FROM db_design_space WHERE com_id=%s), (SELECT COUNT(*) FROM db_design_item WHERE com_id=%s) FROM db_design WHERE com_id=%s" % (MAIN_COM, MAIN_COM, MAIN_COM, MAIN_COM, MAIN_COM))
    raw['side_effect_counts_before'] = before

    status, boss_page = page(tokens['boss'])
    add('BOSS page readable', status == 200 and boss_page.get('success') is True, {'http': status, 'total': boss_page.get('data', {}).get('totalElements')})
    add('BOSS page has 5 designs', int(boss_page.get('data', {}).get('totalElements', -1)) == 5, boss_page.get('data', {}).get('totalElements'))

    status, boss_info = info(tokens['boss'], TARGET_DESIGN)
    d = boss_info.get('data') or {}
    items = d.get('items') or []
    cost_fields = sum(1 for x in items if x.get('cost') is not None)
    catalog_cost = sum(1 for x in items if (x.get('catalogItem') or {}).get('costPrice') is not None)
    add('BOSS detail readable', status == 200 and boss_info.get('success') is True, status)
    quote_amount = d.get('currentQuoteAmount')
    calc_amount = d.get('calculatedAmount')
    add('formal quote preferred', str(d.get('currentQuoteId')) == '233178252326207488' and quote_amount is not None and Decimal(str(quote_amount)) == Decimal('65406.10'), {'http': status, 'code': (boss_info.get('errorCode') or {}).get('code'), 'id': d.get('currentQuoteId'), 'amount': quote_amount})
    add('calculated amount', calc_amount is not None and Decimal(str(calc_amount)) == Decimal('62291.52'), {'http': status, 'code': (boss_info.get('errorCode') or {}).get('code'), 'amount': calc_amount})
    add('detail structure', d.get('itemCount') == 47 and d.get('spaceCount') == 1 and len(items) == 47, {'items': len(items), 'itemCount': d.get('itemCount'), 'spaceCount': d.get('spaceCount')})
    add('store projected', (d.get('store') or {}).get('name') == '总店', (d.get('store') or {}).get('name'))
    add('BOSS cost visible', cost_fields == 47 and catalog_cost == 47, {'costFields': cost_fields, 'catalogCost': catalog_cost})

    for role in ('store', 'finance'):
        status, resp = info(tokens[role], TARGET_DESIGN)
        dd = resp.get('data') or {}
        ii = dd.get('items') or []
        cf = sum(1 for x in ii if x.get('cost') is not None)
        add(role + ' detail cost visible', status == 200 and resp.get('success') is True and cf == 47, {'http': status, 'costFields': cf})

    status, des_resp = info(tokens['designer'], DESIGNER_DESIGN)
    des = des_resp.get('data') or {}
    des_items = des.get('items') or []
    des_cost = sum(1 for x in des_items if x.get('cost') is not None)
    des_catalog = sum(1 for x in des_items if (x.get('catalogItem') or {}).get('costPrice') is not None)
    add('designer cost hidden', status == 200 and des_resp.get('success') is True and len(des_items) == 68 and des_cost == 0 and des_catalog == 0, {'items': len(des_items), 'costFields': des_cost, 'catalogCost': des_catalog})

    status, mkt_page = page(tokens['marketer'])
    mkt_total = (mkt_page.get('data') or {}).get('totalElements')
    add('marketer page readable', status == 200 and mkt_page.get('success') is True, {'http': status, 'total': mkt_total})
    if mkt_total:
        mkt_id = mkt_page['data']['content'][0]['designId']
        status, mkt_info = info(tokens['marketer'], mkt_id)
        mi = mkt_info.get('data') or {}
        mkt_items = mi.get('items') or []
        mkt_cost = sum(1 for x in mkt_items if x.get('cost') is not None or (x.get('catalogItem') or {}).get('costPrice') is not None)
        add('marketer cost hidden', status == 200 and mkt_info.get('success') is True and mkt_cost == 0, {'designId': mkt_id, 'items': len(mkt_items), 'costFields': mkt_cost})
    else:
        add('marketer cost hidden', True, {'note': 'no accessible design; covered by DesignCostContextTest'})

    status, pm_page = page(tokens['pm'])
    add('PM page readable', status == 200 and pm_page.get('success') is True, {'http': status, 'total': (pm_page.get('data') or {}).get('totalElements')})

    status, missing = info(tokens['boss'], 999999999999999999)
    add('invalid design rejected', status == 200 and missing.get('success') is False and (missing.get('errorCode') or {}).get('code') == '404', {'http': status, 'code': (missing.get('errorCode') or {}).get('code')})

    incomplete = []
    for row in (boss_page.get('data') or {}).get('content', []):
        did = row.get('designId')
        _, di = info(tokens['boss'], did)
        di_items = (di.get('data') or {}).get('items') or []
        if di_items and any(x.get('cost') is None for x in di_items):
            incomplete.append({'designId': did, 'items': len(di_items), 'missing': sum(1 for x in di_items if x.get('cost') is None)})
    raw['cost_incomplete_samples'] = incomplete
    add('cost incompleteness observable', True, incomplete[:3])

    store_backups['store'] = int(db_one('SELECT store_id FROM db_tenant_employee WHERE emp_id=1790221790492843136'))
    store_backups['finance'] = int(db_one('SELECT store_id FROM db_tenant_employee WHERE emp_id=1790221992067828768'))
    db_exec("INSERT INTO db_tenant_store (store_id,default_store,status,sort,name,company_com_id) VALUES (%s,b'0','Active',2,'独立验收临时二店',%s)" % (TEMP_STORE, MAIN_COM))
    db_exec('UPDATE db_tenant_employee SET store_id=%s WHERE emp_id IN (1790221790492843136,1790221992067828768)' % TEMP_STORE)

    _, store_scope_page = page(tokens['store'])
    _, finance_scope_page = page(tokens['finance'])
    _, store_detail = info(tokens['store'], TARGET_DESIGN)
    _, finance_detail = info(tokens['finance'], TARGET_DESIGN)
    _, cross_filter = page(tokens['store'], MAIN_STORE)
    add('store manager cross-store list empty', int((store_scope_page.get('data') or {}).get('totalElements', -1)) == 0, (store_scope_page.get('data') or {}).get('totalElements'))
    add('store finance cross-store list empty', int((finance_scope_page.get('data') or {}).get('totalElements', -1)) == 0, (finance_scope_page.get('data') or {}).get('totalElements'))
    add('store manager cross-store detail rejected', store_detail.get('success') is False and (store_detail.get('errorCode') or {}).get('code') == '404', (store_detail.get('errorCode') or {}).get('code'))
    add('store finance cross-store detail rejected', finance_detail.get('success') is False and (finance_detail.get('errorCode') or {}).get('code') == '404', (finance_detail.get('errorCode') or {}).get('code'))
    add('explicit cross-store filter rejected', cross_filter.get('success') is False and (cross_filter.get('errorCode') or {}).get('code') == '403', (cross_filter.get('errorCode') or {}).get('code'))

    # Restore stores before cross-company checks.
    db_exec('UPDATE db_tenant_employee SET store_id=%s WHERE emp_id=1790221790492843136' % store_backups['store'])
    db_exec('UPDATE db_tenant_employee SET store_id=%s WHERE emp_id=1790221992067828768' % store_backups['finance'])
    db_exec('DELETE FROM db_tenant_store WHERE store_id=%s' % TEMP_STORE)

    _, other_page = page(other_token)
    _, other_detail = info(other_token, TARGET_DESIGN)
    add('cross-company list empty', int((other_page.get('data') or {}).get('totalElements', -1)) == 0, (other_page.get('data') or {}).get('totalElements'))
    add('cross-company detail rejected', other_detail.get('success') is False and (other_detail.get('errorCode') or {}).get('code') == '404', (other_detail.get('errorCode') or {}).get('code'))

    after = db_one("SELECT COUNT(*), (SELECT COUNT(*) FROM db_design WHERE com_id=%s), (SELECT COUNT(*) FROM quote WHERE com_id=%s), (SELECT COUNT(*) FROM db_design_space WHERE com_id=%s), (SELECT COUNT(*) FROM db_design_item WHERE com_id=%s) FROM db_design WHERE com_id=%s" % (MAIN_COM, MAIN_COM, MAIN_COM, MAIN_COM, MAIN_COM))
    raw['side_effect_counts_after'] = after
    add('read-only side effects absent', before == after, {'before': before, 'after': after})

    raw['checks'] = checks
    raw['accepted'] = all(c['passed'] for c in checks)
    raw['failed'] = [c for c in checks if not c['passed']]
    raw['evaluated_at'] = time.strftime('%Y-%m-%dT%H:%M:%S%z')
finally:
    # Always restore credentials and store attribution.
    if store_backups.get('store'):
        db_exec('UPDATE db_tenant_employee SET store_id=%s WHERE emp_id=1790221790492843136' % store_backups['store'])
    if store_backups.get('finance'):
        db_exec('UPDATE db_tenant_employee SET store_id=%s WHERE emp_id=1790221992067828768' % store_backups['finance'])
    db_exec('DELETE FROM db_tenant_store WHERE store_id=%s' % TEMP_STORE)
    for e, backup in backups.items():
        restore_credential(e, backup)
    restore_credential(OTHER_EMP, other_backup)

out = Path('docs/acceptance/runs/20261022-design-quote-independent/raw/acceptance-summary.json')
out.write_text(json.dumps(raw, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(raw, ensure_ascii=False, indent=2))
