#!/usr/bin/env python3
import json, os, subprocess
BASE='http://127.0.0.1:8090/api'; MONTH='2026-11'
PASSWORD=os.environ['TH4_TEST_PASSWORD']
A_FIN='13910002003'; B_FIN='13910002011'; B_EMP='199600000000000104'
def post(route,body=None,token=None):
 cmd=['curl','-fsS','-X','POST',f'{BASE}/{route}','-H','Content-Type: application/json']
 if token: cmd+=['-H',f'token: {token}']
 cmd+=['--data-binary',json.dumps(body or {},ensure_ascii=False)]
 return json.loads(subprocess.check_output(cmd,text=True,timeout=20))
def login(phone): return post('auth/session/password-login',{'phone':phone,'password':PASSWORD})['data']['token']
def db(sql):
 env=dict(os.environ,MYSQL_PWD='mysqladmin')
 out=subprocess.check_output(['mysql','-h','127.0.0.1','-P','3306','-u','root','-D','decoboon','--default-character-set=utf8mb4','-N','-B','-e',sql],env=env,text=True)
 return [x.split('\t') for x in out.splitlines() if x]
a=login(A_FIN); b=login(B_FIN)
# reset only isolated company B payroll slice
db("DELETE i FROM db_payroll_item i JOIN db_payroll_sheet s ON s.sheet_id=i.sheet_id WHERE s.com_id=199600000000000001 AND s.period='2026-11'")
db("DELETE FROM db_payroll_sheet WHERE com_id=199600000000000001 AND period='2026-11'")
db("DELETE FROM db_payroll_attendance_summary WHERE com_id=199600000000000001 AND payroll_month='2026-11'")
db("DELETE FROM db_payroll_compensation_scheme WHERE com_id=199600000000000001 AND emp_id='199600000000000104' AND remark='FIN-P0-003 tenant B'")
r=post('payroll/compensation/save',{'empId':B_EMP,'baseSalary':4000,'scheduledDays':26,'effectiveFrom':'2026-01-01','remark':'FIN-P0-003 tenant B'},b); assert r.get('success'),r
r=post('attendance/summary/save',{'empId':B_EMP,'month':MONTH,'scheduledDays':26,'normalDays':26,'personalLeaveDays':0,'sickLeaveDays':0,'absenceDays':0,'externalDays':0,'annualLeaveDays':0,'compLeaveDays':0,'remark':'FIN-P0-003 tenant B'},b); assert r.get('success'),r
summary_id=r['data']['summaryId']
r=post('attendance/summary/update',{'summaryId':summary_id,'status':'Confirmed'},b); assert r.get('success'),r
r=post('payroll/sheet/build',{'period':MONTH},b); assert r.get('success'),r
sheet_id=r['data']['sheetId']
items=db(f"SELECT item_id FROM db_payroll_item WHERE com_id=199600000000000001 AND sheet_id='{sheet_id}'")
item_id=items[0][0]
checks={
 'attendance_info_from_a':post('attendance/summary/info',{'summaryId':summary_id},a),
 'attendance_update_from_a':post('attendance/summary/update',{'summaryId':summary_id,'normalDays':25},a),
 'sheet_info_from_a':post('payroll/sheet/info',{'sheetId':sheet_id},a),
 'items_from_a':post('payroll/item/page',{'current':1,'size':20,'sheetId':sheet_id},a),
 'payable_from_a':post('finance/payment/employee/page',{'current':1,'size':20,'sheetId':sheet_id},a),
 'attendance_info_from_b':post('attendance/summary/info',{'summaryId':summary_id},b),
 'sheet_info_from_b':post('payroll/sheet/info',{'sheetId':sheet_id},b),
 'items_from_b':post('payroll/item/page',{'current':1,'size':20,'sheetId':sheet_id},b)
}
result={'tenantA':199500000000000001,'tenantB':199600000000000001,'sheetId':sheet_id,'summaryId':summary_id,'itemId':item_id,'checks':checks}
print(json.dumps(result,ensure_ascii=False,indent=2))
