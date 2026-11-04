import json, os, subprocess, urllib.request
from pathlib import Path

BASE='http://127.0.0.1:8090/api'
PASSWORD=os.environ['TH4_TEST_PASSWORD']
FIN='13910002003'; BOSS='13910002001'; STORE_FIN='13910002010'
MONTH='2026-11'
EMP1='199500000000000104'; EMP2='199500000000000107'

def post(route, body=None, token=None):
    cmd=['curl','-fsS','-X','POST',f'{BASE}/{route}','-H','Content-Type: application/json']
    if token: cmd += ['-H',f'token: {token}']
    cmd += ['--data-binary',json.dumps(body or {},ensure_ascii=False)]
    raw=subprocess.check_output(cmd,text=True,timeout=20)
    return json.loads(raw)

def login(phone):
    r=post('auth/session/password-login', {'phone':phone,'password':PASSWORD})
    assert r.get('success'), r
    return r['data']['token']

def db(sql):
    env=dict(os.environ, MYSQL_PWD='mysqladmin')
    out=subprocess.check_output(['mysql','-h','127.0.0.1','-P','3306','-u','root','-D','decoboon','--default-character-set=utf8mb4','-N','-B','-e',sql],env=env,text=True)
    return [line.split('\t') for line in out.splitlines() if line]

fin=login(FIN); boss=login(BOSS); store=login(STORE_FIN)
out={'month':MONTH,'login':{'finance':True,'boss':True,'store_finance':True},'cases':{},'responses':{}}

def record(name,resp): out['responses'][name]=resp

# Reset only this synthetic fixture slice before execution.
db(f"DELETE i FROM db_payroll_item i JOIN db_payroll_sheet s ON s.sheet_id=i.sheet_id WHERE s.com_id=199500000000000001 AND s.period='{MONTH}'")
db(f"DELETE FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}'")
db(f"DELETE FROM db_payroll_attendance_summary WHERE com_id=199500000000000001 AND payroll_month='{MONTH}' AND emp_id IN ('{EMP1}','{EMP2}')")
db(f"DELETE FROM db_payroll_compensation_scheme WHERE com_id=199500000000000001 AND emp_id IN ('{EMP1}','{EMP2}') AND remark='FIN-P0-001 fixture'")

# Ensure no current payroll sheet for this isolated month.
assert db(f"SELECT COUNT(*) FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}'")[0][0]=='0'
# Compensation schemes for two employees.
for emp in (EMP1,EMP2):
    r=post('payroll/compensation/save', {'empId':emp,'baseSalary':5000,'scheduledDays':26,'effectiveFrom':'2026-01-01','remark':'FIN-P0-001 fixture'}, fin)
    record(f'compensation_{emp}',r); assert r.get('success'), r

# EMP1 has pending attendance; EMP2 has none => build must block.
r=post('attendance/summary/save', {'empId':EMP1,'month':MONTH,'scheduledDays':26,'normalDays':26,'personalLeaveDays':0,'sickLeaveDays':0,'absenceDays':0,'externalDays':0,'annualLeaveDays':0,'compLeaveDays':0,'remark':'FIN-P0-001'}, fin)
record('attendance_save_emp1',r); assert r.get('success'), r
summary1=r['data']['summaryId']
before_sheets=db(f"SELECT COUNT(*) FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}'")[0][0]
before_items=db(f"SELECT COUNT(*) FROM db_payroll_item WHERE com_id=199500000000000001")[0][0]
r=post('payroll/sheet/build', {'period':MONTH}, fin); record('case001_build_blocked',r)
after_sheets=db(f"SELECT COUNT(*) FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}'")[0][0]
after_items=db(f"SELECT COUNT(*) FROM db_payroll_item WHERE com_id=199500000000000001")[0][0]
out['cases']['TC-FIN-PAYROLL-ATTENDANCE-BLOCK-001']={
  'pass': r.get('success') is False and '考勤未确认' in json.dumps(r,ensure_ascii=False) and before_sheets==after_sheets=='0' and before_items==after_items,
  'response': r, 'before':{'sheets':before_sheets,'items':before_items}, 'after':{'sheets':after_sheets,'items':after_items}
}

# Create second attendance and confirm both.
r=post('attendance/summary/save', {'empId':EMP2,'month':MONTH,'scheduledDays':26,'normalDays':26,'personalLeaveDays':0,'sickLeaveDays':0,'absenceDays':0,'externalDays':0,'annualLeaveDays':0,'compLeaveDays':0,'remark':'FIN-P0-001'}, fin)
record('attendance_save_emp2',r); assert r.get('success'), r
summary2=r['data']['summaryId']
for sid in (summary1,summary2):
    rr=post('attendance/summary/update', {'summaryId':sid,'status':'Confirmed'}, fin); record(f'confirm_{sid}',rr); assert rr.get('success'), rr
# Build success should lock both.
r=post('payroll/sheet/build', {'period':MONTH}, fin); record('case002_build_success',r)
sheet_id=r.get('data',{}).get('sheetId')
att=db(f"SELECT summary_id,status FROM db_payroll_attendance_summary WHERE summary_id IN ('{summary1}','{summary2}') ORDER BY summary_id")
items=db(f"SELECT item_id,status,emp_id FROM db_payroll_item WHERE com_id=199500000000000001 AND sheet_id='{sheet_id}' ORDER BY item_id") if sheet_id else []
sheet=db(f"SELECT sheet_id,status FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}'") if sheet_id else []
out['cases']['TC-FIN-PAYROLL-LOCK-ATOMIC-002']={
 'pass': bool(sheet_id) and all(x[1]=='InPayroll' for x in att) and len(att)==2 and len(items)==2 and sheet and sheet[0][1]=='Draft',
 'sheet_id':sheet_id,'attendance':att,'items':items,'sheet':sheet
}

# Pending cannot rebuild; Boss returns.
r=post('payroll/sheet/update', {'sheetId':sheet_id,'status':'Pending'}, fin); record('case003_submit_pending',r)
status_pending=db(f"SELECT status FROM db_payroll_sheet WHERE sheet_id='{sheet_id}'")[0][0]
r=post('payroll/sheet/build', {'period':MONTH}, fin); record('case003_finance_rebuild_pending',r)
status_after=db(f"SELECT status FROM db_payroll_sheet WHERE sheet_id='{sheet_id}'")[0][0]
r2=post('payroll/sheet/update', {'sheetId':sheet_id,'status':'Draft','reason':'FIN-P0-001 return'}, boss); record('case003_boss_return',r2)
status_return=db(f"SELECT status FROM db_payroll_sheet WHERE sheet_id='{sheet_id}'")[0][0]
out['cases']['TC-FIN-PAYROLL-PENDING-NO-REBUILD-003']={
 'pass': status_pending=='Pending' and r.get('success') is False and status_after=='Pending' and r2.get('success') is True and status_return=='Draft',
 'statuses':{'before':status_pending,'after_rebuild_attempt':status_after,'after_return':status_return},'finance_response':r,'boss_return':r2
}

# Draft delete restores attendance.
r=post('payroll/sheet/delete', {'sheetId':sheet_id}, fin); record('case004_delete_draft',r)
sheet_exists=db(f"SELECT COUNT(*) FROM db_payroll_sheet WHERE sheet_id='{sheet_id}'")[0][0]
item_count=db(f"SELECT COUNT(*) FROM db_payroll_item WHERE com_id=199500000000000001 AND sheet_id='{sheet_id}'")[0][0]
att_after=db(f"SELECT status FROM db_payroll_attendance_summary WHERE summary_id IN ('{summary1}','{summary2}') ORDER BY summary_id")
out['cases']['TC-FIN-PAYROLL-DRAFT-RESTORE-004']={
 'pass': r.get('success') is True and sheet_exists=='0' and item_count=='0' and all(x[0]=='Confirmed' for x in att_after),
 'response':r,'sheet_exists':sheet_exists,'item_count':item_count,'attendance_after':att_after
}

# State bypass: ordinary update directly to InPayroll should be rejected.
r=post('attendance/summary/update', {'summaryId':summary1,'status':'InPayroll'}, fin); record('case005_direct_inpayroll',r)
status5=db(f"SELECT status FROM db_payroll_attendance_summary WHERE summary_id='{summary1}'")[0][0]
out['cases']['TC-FIN-ATTENDANCE-STATE-BYPASS-005']={
 'pass': r.get('success') is False and status5!='InPayroll', 'response':r,'status_after':status5
}
# Restore to Confirmed for the next case if the illegal transition unexpectedly succeeded.
if status5!='Confirmed':
    post('attendance/summary/update', {'summaryId':summary1,'status':'Confirmed'}, fin)
    db(f"UPDATE db_payroll_attendance_summary SET status='Confirmed' WHERE summary_id='{summary1}' AND status='InPayroll'")

# Confirmed edit must reset confirmation or block.
pre=db(f"SELECT confirmed_by,DATE_FORMAT(confirmed_at,'%Y-%m-%d %H:%i:%s') FROM db_payroll_attendance_summary WHERE summary_id='{summary1}'")[0]
r=post('attendance/summary/update', {'summaryId':summary1,'normalDays':25}, fin); record('case006_confirmed_edit',r)
postrow=db(f"SELECT normal_days,confirmed_by,DATE_FORMAT(confirmed_at,'%Y-%m-%d %H:%i:%s'),status FROM db_payroll_attendance_summary WHERE summary_id='{summary1}'")[0]
out['cases']['TC-FIN-ATTENDANCE-CONFIRMED-EDIT-006']={
 'pass': r.get('success') is False or postrow[2]!=pre[1] or postrow[3]!='Confirmed',
 'pre':pre,'response':r,'post':postrow
}
# Restore normalDays and Confirmed state.
post('attendance/summary/update', {'summaryId':summary1,'normalDays':26,'status':'Confirmed'}, fin)

# Store finance cross-store update should fail and not change target.
pre_normal=db(f"SELECT normal_days FROM db_payroll_attendance_summary WHERE summary_id='{summary1}'")[0][0]
r=post('attendance/summary/update', {'summaryId':summary1,'normalDays':1}, store); record('case007_cross_store_update',r)
post_normal=db(f"SELECT normal_days FROM db_payroll_attendance_summary WHERE summary_id='{summary1}'")[0][0]
out['cases']['TC-FIN-FINANCE-STORE-SCOPE-007']={
 'pass': r.get('success') is False and pre_normal==post_normal, 'pre_normal':pre_normal,'response':r,'post_normal':post_normal
}
# Restore if unexpected cross-store write succeeded.
if post_normal!=pre_normal:
    post('attendance/summary/update', {'summaryId':summary1,'normalDays':int(pre_normal)}, fin)

Path('/tmp/finance_p0_payroll_probe.json').write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v['pass'] for k,v in out['cases'].items()},ensure_ascii=False,indent=2))
