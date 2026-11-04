#!/usr/bin/env python3
import json, os, subprocess, sys
BASE='http://127.0.0.1:8090/api'; MONTH=os.environ.get('FIN_MONTH','2026-12')
FIN='13910002003'; BOSS='13910002001'; EMP1='199500000000000104'; EMP2='199500000000000107'
PASSWORD=os.environ.get('TH4_TEST_PASSWORD')
if not PASSWORD: raise SystemExit('TH4_TEST_PASSWORD required')
def post(route, body=None, token=None):
    cmd=['curl','-fsS','-X','POST',f'{BASE}/{route}','-H','Content-Type: application/json']
    if token: cmd += ['-H',f'token: {token}']
    cmd += ['--data-binary',json.dumps(body or {},ensure_ascii=False)]
    return json.loads(subprocess.check_output(cmd,text=True,timeout=20))
def login(phone): return post('auth/session/password-login',{'phone':phone,'password':PASSWORD})['data']['token']
def db(sql):
    env=dict(os.environ,MYSQL_PWD='mysqladmin')
    out=subprocess.check_output(['mysql','-h','127.0.0.1','-P','3306','-u','root','-D','decoboon','--default-character-set=utf8mb4','-N','-B','-e',sql],env=env,text=True)
    return [x.split('\t') for x in out.splitlines() if x]
def reset(fin):
    db(f"DELETE i FROM db_payroll_item i JOIN db_payroll_sheet s ON s.sheet_id=i.sheet_id WHERE s.com_id=199500000000000001 AND s.period='{MONTH}'")
    db(f"DELETE FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}'")
    db(f"DELETE FROM db_payroll_attendance_summary WHERE com_id=199500000000000001 AND payroll_month='{MONTH}' AND emp_id IN ('{EMP1}','{EMP2}')")
    db(f"DELETE FROM db_payroll_compensation_scheme WHERE com_id=199500000000000001 AND emp_id IN ('{EMP1}','{EMP2}') AND remark LIKE 'FIN-P0-%'")
    for emp in (EMP1,EMP2):
        r=post('payroll/compensation/save',{'empId':emp,'baseSalary':5000,'scheduledDays':26,'effectiveFrom':'2026-01-01','remark':'FIN-P0-CURRENT fixture'},fin)
        assert r.get('success'),r
def create_emp1_attendance(fin):
    r=post('attendance/summary/save',{'empId':EMP1,'month':MONTH,'scheduledDays':26,'normalDays':26,'personalLeaveDays':0,'sickLeaveDays':0,'absenceDays':0,'externalDays':0,'annualLeaveDays':0,'compLeaveDays':0,'remark':'FIN-P0-CURRENT'},fin)
    assert r.get('success'),r
    return r['data']['summaryId']
cmd=sys.argv[1]
fin=login(FIN); boss=login(BOSS)
if cmd=='setup-incomplete':
    reset(fin); sid=create_emp1_attendance(fin); print(json.dumps({'summaryId':sid}))
elif cmd=='complete-and-build':
    sid=db(f"SELECT summary_id FROM db_payroll_attendance_summary WHERE com_id=199500000000000001 AND emp_id='{EMP1}' AND payroll_month='{MONTH}'")[0][0]
    r=post('attendance/summary/save',{'empId':EMP2,'month':MONTH,'scheduledDays':26,'normalDays':26,'personalLeaveDays':0,'sickLeaveDays':0,'absenceDays':0,'externalDays':0,'annualLeaveDays':0,'compLeaveDays':0,'remark':'FIN-P0-CURRENT'},fin); assert r.get('success'),r
    sid2=r['data']['summaryId']
    for s in (sid,sid2):
        rr=post('attendance/summary/update',{'summaryId':s,'status':'Confirmed'},fin); assert rr.get('success'),rr
    rr=post('payroll/sheet/build',{'period':MONTH},fin); assert rr.get('success'),rr
    print(json.dumps({'sheetId':rr['data']['sheetId'],'summaryIds':[sid,sid2]}))
elif cmd=='submit-review':
    sheet=db(f"SELECT sheet_id FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}'")[0][0]
    r=post('payroll/sheet/update',{'sheetId':sheet,'status':'Pending'},fin); assert r.get('success'),r
    print(json.dumps({'sheetId':sheet,'status':'Pending'}))
elif cmd=='boss-return':
    sheet=db(f"SELECT sheet_id FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}'")[0][0]
    r=post('payroll/sheet/update',{'sheetId':sheet,'status':'Draft','reason':'FIN-P0-CURRENT UI retest'},boss); assert r.get('success'),r
    print(json.dumps({'sheetId':sheet,'status':'Draft'}))
elif cmd=='state':
    print(json.dumps({'sheet':db(f"SELECT sheet_id,status FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}'"),'attendance':db(f"SELECT summary_id,status FROM db_payroll_attendance_summary WHERE com_id=199500000000000001 AND payroll_month='{MONTH}' ORDER BY summary_id"),'items':db(f"SELECT item_id,status,emp_id FROM db_payroll_item WHERE com_id=199500000000000001 ORDER BY item_id")},ensure_ascii=False))
elif cmd=='build-confirm':
    existing=db(f"SELECT sheet_id FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}' AND status IN ('Confirmed','Paid')")
    if existing:
        sheet=existing[0][0]; confirm1={'success':True,'existing':True}
    else:
        build=post('payroll/sheet/build',{'period':MONTH},fin); assert build.get('success'),build
        sheet=build['data']['sheetId']
        confirm1=post('payroll/sheet/update',{'sheetId':sheet},boss); assert confirm1.get('success'),confirm1
    pay1=post('finance/payment/employee/page',{'current':1,'size':20,'sheetId':sheet,'type':'EmployeeSalary'},fin); assert pay1.get('success'),pay1
    count1=len(pay1['data']['content'])
    confirm2=post('payroll/sheet/update',{'sheetId':sheet},boss); assert confirm2.get('success'),confirm2
    pay2=post('finance/payment/employee/page',{'current':1,'size':20,'sheetId':sheet,'type':'EmployeeSalary'},fin); assert pay2.get('success'),pay2
    count2=len(pay2['data']['content'])
    total=sum(float(x['amount'])-float(x['paidAmount']) for x in pay2['data']['content'])
    dbrows=db(f"SELECT COUNT(*),COALESCE(SUM(amount),0) FROM db_finance_payable WHERE com_id=199500000000000001 AND period='{MONTH}' AND type='EmployeeSalary'")
    print(json.dumps({'sheetId':sheet,'payableCount1':count1,'payableCount2':count2,'outstandingTotal':total,'db':dbrows[0],'payables':[{'payableId':x['payableId'],'amount':x['amount'],'paidAmount':x['paidAmount']} for x in pay2['data']['content']]},ensure_ascii=False))
elif cmd=='pay-twice':
    sheet=db(f"SELECT sheet_id FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}' AND status='Confirmed'")[0][0]
    pays=post('finance/payment/employee/page',{'current':1,'size':20,'sheetId':sheet,'type':'EmployeeSalary'},fin); assert pays.get('success'),pays
    all_rows=pays['data']['content']; rows=[all_rows[0]]; total=float(rows[0]['amount'])-float(rows[0]['paidAmount'])
    body={'payableIds':[rows[0]['payableId']],'amount':total,'account':'CompanyBank','paidAt':'2026-11-04','batchNo':'FIN-P0-CURRENT','idempotencyKey':'FIN-P0-CURRENT-PAY-001','remark':'FIN-P0-CURRENT payment idempotency'}
    p1=post('finance/payment/employee/save',body,fin); p2=post('finance/payment/employee/save',body,fin)
    payment_rows=db("SELECT payment_id,payable_id,amount,idempotency_key FROM db_finance_payment WHERE com_id=199500000000000001 AND idempotency_key='FIN-P0-CURRENT-PAY-001'")
    ledger_count=db("SELECT COUNT(*) FROM db_finance_ledger WHERE com_id=199500000000000001 AND source='Payment' AND source_id IN (SELECT payment_id FROM db_finance_payment WHERE idempotency_key='FIN-P0-CURRENT-PAY-001')")
    payable_after=db(f"SELECT payable_id,status,amount,paid_amount FROM db_finance_payable WHERE com_id=199500000000000001 AND period='{MONTH}' AND type='EmployeeSalary' ORDER BY payable_id")
    payslip=db(f"SELECT payslip_id,status,net_amount FROM db_payroll_payslip WHERE com_id=199500000000000001 AND period='{MONTH}' ORDER BY payslip_id")
    print(json.dumps({'payables':rows,'total':total,'first':p1,'second':p2,'paymentRows':payment_rows,'ledgerCount':ledger_count,'payableAfter':payable_after,'payslips':payslip},ensure_ascii=False))
elif cmd=='pay-second':
    sheet=db(f"SELECT sheet_id FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}'")[0][0]
    pays=post('finance/payment/employee/page',{'current':1,'size':20,'sheetId':sheet,'type':'EmployeeSalary'},fin); assert pays.get('success'),pays
    pending=[x for x in pays['data']['content'] if x['status']['key']=='Pending']; assert pending,pays
    row=pending[0]; total=float(row['amount'])-float(row['paidAmount'])
    body={'payableIds':[row['payableId']],'amount':total,'account':'CompanyBank','paidAt':'2026-11-04','batchNo':'FIN-P0-CURRENT-B','idempotencyKey':'FIN-P0-CURRENT-PAY-002','remark':'FIN-P0-CURRENT second employee payment'}
    result=post('finance/payment/employee/save',body,fin); assert result.get('success'),result
    sheet_after=db(f"SELECT sheet_id,status FROM db_payroll_sheet WHERE sheet_id='{sheet}'")
    payslips=db(f"SELECT payslip_id,status,net_amount FROM db_payroll_payslip WHERE com_id=199500000000000001 AND period='{MONTH}' ORDER BY payslip_id")
    print(json.dumps({'result':result,'sheetAfter':sheet_after,'payslips':payslips},ensure_ascii=False))
elif cmd=='history-immutable':
    sheet=db(f"SELECT sheet_id FROM db_payroll_sheet WHERE com_id=199500000000000001 AND period='{MONTH}' AND status='Paid'")[0][0]
    sid=db(f"SELECT summary_id FROM db_payroll_attendance_summary WHERE com_id=199500000000000001 AND emp_id='{EMP1}' AND payroll_month='{MONTH}'")[0][0]
    before=db(f"SELECT status,total_net_amount FROM db_payroll_sheet WHERE sheet_id='{sheet}'")[0]
    attendance_before=db(f"SELECT status,normal_days FROM db_payroll_attendance_summary WHERE summary_id='{sid}'")[0]
    update_attendance=post('attendance/summary/update',{'summaryId':sid,'normalDays':25},fin)
    delete_sheet=post('payroll/sheet/delete',{'sheetId':sheet},fin)
    rebuild=post('payroll/sheet/build',{'period':MONTH},fin)
    after=db(f"SELECT status,total_net_amount FROM db_payroll_sheet WHERE sheet_id='{sheet}'")[0]
    attendance_after=db(f"SELECT status,normal_days FROM db_payroll_attendance_summary WHERE summary_id='{sid}'")[0]
    print(json.dumps({'sheetId':sheet,'before':before,'after':after,'attendanceBefore':attendance_before,'attendanceAfter':attendance_after,'updateAttendance':update_attendance,'deleteSheet':delete_sheet,'rebuild':rebuild},ensure_ascii=False))
else: raise SystemExit('unknown command')
