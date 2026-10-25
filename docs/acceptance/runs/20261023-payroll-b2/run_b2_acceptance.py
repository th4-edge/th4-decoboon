import json, subprocess
from pathlib import Path

ROOT=Path('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon')
EVID=ROOT/'docs/acceptance/runs/20261023-payroll-b2'
HTTP=EVID/'http'; SQL=EVID/'sql'
BASE='http://127.0.0.1:8090/api/'
MYSQL=['mysql','--socket='+Path('/tmp/th4-h1-mysql-path').read_text().strip()+'/mysql.sock','-uroot','decoboon_h1_schema','--batch','--raw']
PASSWORD='H1Accept123'

def api(route, body, token=None):
    cmd=['curl','-sS','--max-time','30','-w','\n%{http_code}','-X','POST',BASE+route,'-H','Content-Type: application/json']
    if token: cmd += ['-H','token: '+token]
    cmd += ['-d',json.dumps(body,ensure_ascii=False)]
    raw=subprocess.check_output(cmd,text=True)
    payload,code=raw.rsplit('\n',1)
    return int(code),json.loads(payload)

def sql(stmt):
    return subprocess.check_output(MYSQL+['-e',stmt],text=True)

def save(name,route,status,payload):
    (HTTP/f'{name}.json').write_text(json.dumps({'route':route,'http':status,'response':payload},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def code(p): return str((p.get('errorCode') or {}).get('code') or '')
def data(p): return p.get('data') or {}
results=[]
def rec(case,expected,actual,passed,evidence):
    results.append({'case':case,'expected':expected,'actual':actual,'result':'PASS' if passed else 'FAIL','evidence':evidence})

tokens={}
for role,phone in [('boss','18880000001'),('finance','18880000002'),('manager','18880000003'),('store2finance','18880000006')]:
    s,p=api('auth/session/password-login',{'phone':phone,'password':PASSWORD})
    assert p.get('success'),(role,p)
    tokens[role]=p['data']['token']

# 1. scheme by month
s,p=api('payroll/compensation/info',{'empId':9100000000000000204,'month':'2026-11'},tokens['finance'])
save('B2-01-compensation-month','payroll/compensation/info',s,p)
rec('B2-01','scheduledDays=27',{'success':p.get('success'),'scheduledDays':data(p).get('scheduledDays'),'code':code(p)},p.get('success') and data(p).get('scheduledDays')==27,'B2-01-compensation-month.json')

# 2. manager create own-store attendance, schedule snapshot backend
s,p=api('attendance/summary/save',{
    'empId':9100000000000000204,'month':'2026-11','normalDays':27,
    'personalLeaveDays':0,'sickLeaveDays':0,'absenceDays':0,
    'externalDays':0,'annualLeaveDays':0,'compLeaveDays':0,'remark':'B2 manager create'
},tokens['manager'])
save('B2-02-manager-create-own','attendance/summary/save',s,p)
summary_id=(data(p).get('summaryId') or data(p).get('id'))
rec('B2-02','success scheduledDays=27',{'success':p.get('success'),'code':code(p),'scheduledDays':data(p).get('scheduledDays'),'summaryId':summary_id},p.get('success') and data(p).get('scheduledDays')==27,'B2-02-manager-create-own.json')

# 3. manager edit own attendance
if summary_id:
    s,p=api('attendance/summary/update',{'summaryId':summary_id,'normalDays':26,'personalLeaveDays':1,'remark':'B2 manager edit'},tokens['manager'])
    save('B2-03-manager-edit-own','attendance/summary/update',s,p)
    rec('B2-03','success normalDays=26 personalLeave=1',{'success':p.get('success'),'code':code(p),'normalDays':data(p).get('normalDays'),'personalLeaveDays':data(p).get('personalLeaveDays')},p.get('success') and data(p).get('normalDays')==26 and data(p).get('personalLeaveDays')==1,'B2-03-manager-edit-own.json')
else:
    rec('B2-03','summary created','no summaryId',False,'B2-02-manager-create-own.json')

# 4. manager create another own record for deletion
s,p=api('attendance/summary/save',{
    'empId':9100000000000000204,'month':'2026-12','normalDays':27,
    'personalLeaveDays':0,'sickLeaveDays':0,'absenceDays':0,
    'externalDays':0,'annualLeaveDays':0,'compLeaveDays':0,'remark':'B2 manager delete'
},tokens['manager'])
save('B2-04-manager-create-delete','attendance/summary/save',s,p)
delete_id=(data(p).get('summaryId') or data(p).get('id'))
rec('B2-04','success scheduledDays=27',{'success':p.get('success'),'code':code(p),'scheduledDays':data(p).get('scheduledDays'),'summaryId':delete_id},p.get('success') and data(p).get('scheduledDays')==27,'B2-04-manager-create-delete.json')

# 5. cross-store create denied
s,p=api('attendance/summary/save',{
    'empId':9100000000000000205,'month':'2026-11','normalDays':27,
    'personalLeaveDays':0,'sickLeaveDays':0,'absenceDays':0,
    'externalDays':0,'annualLeaveDays':0,'compLeaveDays':0,'remark':'B2 cross store'
},tokens['manager'])
save('B2-05-manager-create-cross-store','attendance/summary/save',s,p)
rec('B2-05','404',{'success':p.get('success'),'code':code(p)},not p.get('success') and code(p)=='404','B2-05-manager-create-cross-store.json')

# 6. manager delete own draft
if delete_id:
    s,p=api('attendance/summary/delete',{'summaryId':delete_id},tokens['manager'])
    save('B2-06-manager-delete-own','attendance/summary/delete',s,p)
    rec('B2-06','success',{'success':p.get('success'),'code':code(p)},p.get('success'),'B2-06-manager-delete-own.json')
else:
    rec('B2-06','record created','no summaryId',False,'B2-04-manager-create-delete.json')

# 7. cross-store delete denied
s,p=api('attendance/summary/delete',{'summaryId':9100000000000000502},tokens['manager'])
save('B2-07-manager-delete-cross-store','attendance/summary/delete',s,p)
rec('B2-07','404',{'success':p.get('success'),'code':code(p)},not p.get('success') and code(p)=='404','B2-07-manager-delete-cross-store.json')

# 8. saved record snapshot in DB
if summary_id:
    db=sql(f"SELECT summary_id,emp_id,store_id,payroll_month,scheduled_days,normal_days,personal_leave_days,status FROM db_payroll_attendance_summary WHERE summary_id={summary_id};")
    (SQL/'B2-08-own-record.txt').write_text(db,encoding='utf-8')
    rec('B2-08','DB scheduled_days=27',db.strip(),'27' in db and '2026-11' in db,'sql/B2-08-own-record.txt')

summary={'passed':sum(1 for r in results if r['result']=='PASS'),'failed':sum(1 for r in results if r['result']=='FAIL'),'results':results}
(EVID/'api-results.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(summary,ensure_ascii=False,indent=2))
