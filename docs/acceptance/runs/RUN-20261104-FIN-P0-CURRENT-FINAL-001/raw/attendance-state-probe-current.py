#!/usr/bin/env python3
import json, os, subprocess
BASE='http://127.0.0.1:8090/api'; PASSWORD=os.environ['TH4_TEST_PASSWORD']
FIN='13910002003'; STORE_FIN='13910002010'; SUMMARY='243612653916782592'
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
fin=login(FIN)
store=login(STORE_FIN)
pre=db(f"SELECT status,confirmed_by,confirmed_at,normal_days FROM db_payroll_attendance_summary WHERE summary_id='{SUMMARY}'")[0]
bypass=post('attendance/summary/update',{'summaryId':SUMMARY,'status':'InPayroll'},fin)
after_bypass=db(f"SELECT status,confirmed_by,confirmed_at,normal_days FROM db_payroll_attendance_summary WHERE summary_id='{SUMMARY}'")[0]
edit=post('attendance/summary/update',{'summaryId':SUMMARY,'normalDays':25},fin)
after_edit=db(f"SELECT status,confirmed_by,confirmed_at,normal_days FROM db_payroll_attendance_summary WHERE summary_id='{SUMMARY}'")[0]
cross=post('attendance/summary/update',{'summaryId':SUMMARY,'normalDays':24},store)
after_cross=db(f"SELECT status,confirmed_by,confirmed_at,normal_days FROM db_payroll_attendance_summary WHERE summary_id='{SUMMARY}'")[0]
print(json.dumps({'summaryId':SUMMARY,'pre':pre,'bypass':bypass,'afterBypass':after_bypass,'edit':edit,'afterEdit':after_edit,'storeFinanceCrossStore':cross,'afterCross':after_cross},ensure_ascii=False,indent=2))
