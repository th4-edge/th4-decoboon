import json
import subprocess
from pathlib import Path

ROOT=Path('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon')
EVID=ROOT/'docs/acceptance/runs/20261023-payroll-b3b1'
HTTP=EVID/'http'; SQL=EVID/'sql'
BASE='http://127.0.0.1:8090/api/'
SOCK='/tmp/th4-b3b1-mysql.sock'
DB=Path('/tmp/th4-b3b1-db-name').read_text().strip()
MYSQL=['mysql',f'--socket={SOCK}','-uroot','-pmysqladmin',DB,'--batch','--raw','-N']
PASSWORD='B3B1Accept123'
PHONES={'boss':'19855162372','finance':'15156075712','manager':'18055190856'}
SHEET_SEP=239344514467102720
SHEET_OCT=239345238584328192
results=[]

def api(route,body,token=None):
    cmd=['curl','-sS','--max-time','30','-w','\n%{http_code}','-X','POST',BASE+route,'-H','Content-Type: application/json']
    if token: cmd += ['-H','token: '+token]
    cmd += ['-d',json.dumps(body,ensure_ascii=False)]
    raw=subprocess.check_output(cmd,text=True)
    payload,status=raw.rsplit('\n',1)
    return int(status),json.loads(payload)
def sql(statement):
    return subprocess.check_output(MYSQL+['-e',statement],text=True,stderr=subprocess.DEVNULL)
def save(name,route,status,payload):
    (HTTP/f'{name}.json').write_text(json.dumps({'route':route,'http':status,'response':payload},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def code(p): return str((p.get('errorCode') or {}).get('code') or '')
def data(p): return p.get('data') or {}
def rec(case,expected,actual,passed,evidence):
    results.append({'case':case,'expected':expected,'actual':actual,'result':'PASS' if passed else 'FAIL','evidence':evidence})

tokens={}
for role,phone in PHONES.items():
    status,p=api('auth/session/password-login',{'phone':phone,'password':PASSWORD})
    assert p.get('success'),(role,status,p)
    tokens[role]=p['data']['token']

# 1. Confirmed sheet retains return note and remains BOSS-confirmed.
status,p=api('payroll/sheet/info',{'sheetId':SHEET_SEP},tokens['boss'])
save('B3B1-01-sheet-info','payroll/sheet/info',status,p)
d=data(p)
rec('B3B1-01','Confirmed sheet with return reason',{'success':p.get('success'),'status':(d.get('status') or {}).get('key'),'remark':d.get('remark'),'confirmed':bool(d.get('confirmedBy'))},p.get('success') and (d.get('status') or {}).get('key')=='Confirmed' and d.get('remark')=='退回原因：考勤数据需要修正' and d.get('confirmedBy'),'B3B1-01-sheet-info.json')

# 2. Confirm is idempotent.
before=sql(f"SELECT (SELECT COUNT(*) FROM db_finance_payable WHERE source_type='PayrollItem' AND source_id IN (SELECT item_id FROM db_payroll_item WHERE sheet_id={SHEET_SEP})), (SELECT COUNT(*) FROM db_payroll_payslip WHERE sheet_id={SHEET_SEP});")
(SQL/'B3B1-02-before.txt').write_text(before,encoding='utf-8')
status,p=api('payroll/sheet/update',{'sheetId':SHEET_SEP},tokens['boss'])
save('B3B1-02-repeat-confirm','payroll/sheet/update',status,p)
after=sql(f"SELECT (SELECT COUNT(*) FROM db_finance_payable WHERE source_type='PayrollItem' AND source_id IN (SELECT item_id FROM db_payroll_item WHERE sheet_id={SHEET_SEP})), (SELECT COUNT(*) FROM db_payroll_payslip WHERE sheet_id={SHEET_SEP});")
(SQL/'B3B1-02-after.txt').write_text(after,encoding='utf-8')
rec('B3B1-02','repeat confirm keeps 5 payables / 5 payslips',{'success':p.get('success'),'before':before.strip(),'after':after.strip()},p.get('success') and before==after and before.strip()=='5\t5','B3B1-02-repeat-confirm.json; sql/B3B1-02-*.txt')

# 3. Finance and StoreManager cannot confirm/delete.
for role,prefix in [('finance','03'),('manager','04')]:
    for suffix,route,body in [('confirm','payroll/sheet/update',{'sheetId':SHEET_SEP}),('delete','payroll/sheet/delete',{'sheetId':SHEET_SEP})]:
        status,p=api(route,body,tokens[role]); save(f'B3B1-{prefix}-{suffix}',route,status,p)
        rec(f'B3B1-{prefix}-{suffix}','403',{'success':p.get('success'),'code':code(p)},not p.get('success') and code(p)=='403',f'B3B1-{prefix}-{suffix}.json')

# 4. StoreManager cannot submit review or access/pay employee payables.
status,p=api('payroll/sheet/update',{'sheetId':SHEET_OCT,'status':'Pending'},tokens['manager'])
save('B3B1-05-manager-submit','payroll/sheet/update',status,p)
rec('B3B1-05','StoreManager submit review 403',{'success':p.get('success'),'code':code(p)},not p.get('success') and code(p)=='403','B3B1-05-manager-submit.json')
status,p=api('finance/payment/employee/page',{'current':1,'size':20},tokens['manager'])
save('B3B1-06-manager-pay-page','finance/payment/employee/page',status,p)
rec('B3B1-06','StoreManager employee payable page 403',{'success':p.get('success'),'code':code(p)},not p.get('success') and code(p)=='403','B3B1-06-manager-pay-page.json')
status,p=api('finance/payment/employee/save',{'payableIds':[1],'amount':1,'paidAt':'2026-10-23','idempotencyKey':'b3b1-manager-pay'},tokens['manager'])
save('B3B1-07-manager-pay-save','finance/payment/employee/save',status,p)
rec('B3B1-07','StoreManager employee payment 403',{'success':p.get('success'),'code':code(p)},not p.get('success') and code(p)=='403','B3B1-07-manager-pay-save.json')

# 5. Finance submits October draft for review; BOSS return requires reason.
status,p=api('payroll/sheet/update',{'sheetId':SHEET_OCT,'status':'Pending'},tokens['finance'])
save('B3B1-08-finance-submit','payroll/sheet/update',status,p)
rec('B3B1-08','Finance submits Draft -> Pending',{'success':p.get('success'),'status':(data(p).get('status') or {}).get('key')},p.get('success') and (data(p).get('status') or {}).get('key')=='Pending','B3B1-08-finance-submit.json')
status,p=api('payroll/sheet/update',{'sheetId':SHEET_OCT,'status':'Draft'},tokens['boss'])
save('B3B1-09-return-without-reason','payroll/sheet/update',status,p)
rec('B3B1-09','return without reason 400',{'success':p.get('success'),'code':code(p),'message':(p.get('errorCode') or {}).get('message')},not p.get('success') and code(p)=='400' and '退回原因不能为空' in str((p.get('errorCode') or {}).get('message')),'B3B1-09-return-without-reason.json')
status,p=api('payroll/sheet/update',{'sheetId':SHEET_OCT,'status':'Draft','reason':'本月数据需修正'},tokens['boss'])
save('B3B1-10-return-with-reason','payroll/sheet/update',status,p)
rec('B3B1-10','BOSS returns Pending -> Draft',{'success':p.get('success'),'status':(data(p).get('status') or {}).get('key'),'remark':data(p).get('remark')},p.get('success') and (data(p).get('status') or {}).get('key')=='Draft' and data(p).get('remark')=='退回原因：本月数据需修正','B3B1-10-return-with-reason.json')

# 6. Draft deletion regression and audit lines.
status,p=api('payroll/sheet/delete',{'sheetId':SHEET_OCT},tokens['boss'])
save('B3B1-11-delete-draft','payroll/sheet/delete',status,p)
rec('B3B1-11','BOSS deletes returned Draft',{'success':p.get('success'),'code':code(p)},p.get('success'),'B3B1-11-delete-draft.json')
status,p=api('payroll/sheet/info',{'sheetId':SHEET_OCT},tokens['boss'])
save('B3B1-12-deleted-hidden','payroll/sheet/info',status,p)
rec('B3B1-12','deleted Draft info 404',{'success':p.get('success'),'code':code(p)},not p.get('success') and code(p)=='404','B3B1-12-deleted-hidden.json')

summary={'passed':sum(1 for r in results if r['result']=='PASS'),'failed':sum(1 for r in results if r['result']=='FAIL'),'results':results}
(EVID/'api-results.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(summary,ensure_ascii=False,indent=2))
