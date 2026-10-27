import json, subprocess, time, os
from pathlib import Path

ROOT=Path('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon')
EVID=ROOT/'docs/acceptance/runs/20261023-emg-v2-independent-acceptance'
HTTP=EVID/'http'; SQL=EVID/'sql'
BASE='http://127.0.0.1:8090/api/'
PASSWORD=os.environ.get('EMG_ACCEPT_PASSWORD')
if not PASSWORD: raise SystemExit('missing EMG_ACCEPT_PASSWORD')
BOSS='13990000001'; MANAGER='13990000002'; EMP='13990000004'; OTHER_EMP='13990000003'
cases={}; responses={}; sql_evidence={}

def api(route, body, token=None, timeout=90):
    cmd=['curl','-sS','--max-time',str(timeout),'-w','\n%{http_code}','-X','POST',BASE+route,'-H','Content-Type: application/json']
    if token: cmd += ['-H','token: '+token]
    cmd += ['-d',json.dumps(body,ensure_ascii=False)]
    raw=subprocess.check_output(cmd,text=True)
    payload,code=raw.rsplit('\n',1)
    data=json.loads(payload)
    return int(code),data

def sql(statement):
    return subprocess.check_output(['mysql','--socket=/tmp/syzd-mysql.sock','-uroot','-D','decoboon_accept','--batch','--raw','-e',statement],text=True)

def save(case, name, route, http, body):
    responses.setdefault(case,[]).append({'route':route,'http':http,'response':body})
    safe=name.replace('/','-').replace(' ','-')
    (HTTP/f'{case}-{safe}.json').write_text(json.dumps({'route':route,'http':http,'response':body},ensure_ascii=False,indent=2)+'\n')

def call(case, name, route, body, token=None):
    h,r=api(route,body,token)
    save(case,name,route,h,r)
    return h,r

def result(case, title, expected, actual, passed, evidence):
    cases[case]={'id':case,'title':title,'expected':expected,'actual':actual,'result':'PASS' if passed else 'FAIL','evidence':evidence}

def errcode(r):
    e=r.get('errorCode') or {}
    return str(e.get('code') or '')

def ok(r): return bool(r.get('success'))
def status(r): return ((r.get('data') or {}).get('status') or {}).get('key')
def role(r): return ((r.get('data') or {}).get('role') or {}).get('key')
def storeid(r): return str(((r.get('data') or {}).get('store') or {}).get('storeId') or '')

# login principals
login={}
for name,phone in [('boss',BOSS),('manager',MANAGER),('employee',EMP),('other',OTHER_EMP)]:
    h,r=api('auth/session/password-login',{'phone':phone,'password':PASSWORD})
    responses[f'login-{name}']=[{'route':'auth/session/password-login','http':h,'response':{'success':r.get('success'),'data':{'token':('<redacted>' if (r.get('data') or {}).get('token') else None)},'errorCode':r.get('errorCode')}}]
    login[name]=(r.get('data') or {}).get('token')
bt=login['boss']; mt=login['manager']; et=login['employee']; ot=login['other']
assert bt and mt, 'BOSS/店长登录失败'

# ACC-EMG-V2-01
h,r=call('01','boss-finance-no-store','invite/invitation/save',{'inviteName':'验收财务无门店','invitePhone':'13992000001','role':'Finance'},bt)
result('01','BOSS 邀请财务不选门店','400',{'success':ok(r),'code':errcode(r),'message':(r.get('errorCode') or {}).get('message')}, not ok(r) and errcode(r)=='400',['01-boss-finance-no-store.json'])

# ACC-EMG-V2-02
h,r=call('02','manager-finance-own-store','invite/invitation/save',{'inviteName':'验收本店财务','invitePhone':'13992000002','role':'Finance'},mt)
invite_id=(r.get('data') or {}).get('inviteId')
passed=ok(r) and role(r)=='Finance' and storeid(r)=='2001'
result('02','店长邀请本店财务','成功，storeId=2001',{'success':ok(r),'inviteId':invite_id,'role':role(r),'storeId':storeid(r)},passed,['02-manager-finance-own-store.json'])
if invite_id:
    h,c=call('02','boss-cancel-invite','invite/invitation/cancel',{'inviteId':invite_id,'reason':'独立验收清理待处理邀请'},bt)
    if ok(c): invite_id=None

# ACC-EMG-V2-03
h,r=call('03','manager-finance-other-store','invite/invitation/save',{'inviteName':'验收跨店财务','invitePhone':'13992000003','role':'Finance','storeId':2002},mt)
result('03','店长邀请其他门店财务','403',{'success':ok(r),'code':errcode(r),'message':(r.get('errorCode') or {}).get('message')}, not ok(r) and errcode(r)=='403',['03-manager-finance-other-store.json'])

# ACC-EMG-V2-04
h1,r1=call('04','manager-invite-storemanager','invite/invitation/save',{'inviteName':'验收越权店长','invitePhone':'13992000004','role':'StoreManager'},mt)
h2,r2=call('04','manager-invite-teamleader','invite/invitation/save',{'inviteName':'验收越权TeamLeader','invitePhone':'13992000005','role':'TeamLeader'},mt)
h3,r3=call('04','manager-invite-with-team-id','invite/invitation/save',{'inviteName':'验收冻结Team','invitePhone':'13992000006','role':'Designer','teamId':9999},mt)
passed=(not ok(r1) and errcode(r1) in {'403','400'}) and (not ok(r2) and errcode(r2) in {'403','400'}) and (not ok(r3) and errcode(r3)=='400')
result('04','店长邀请岗位边界','拒绝店长/TeamLeader/Team 写入',{'storeManager':{'code':errcode(r1),'message':(r1.get('errorCode') or {}).get('message')},'teamLeader':{'code':errcode(r2),'message':(r2.get('errorCode') or {}).get('message')},'teamWrite':{'code':errcode(r3),'message':(r3.get('errorCode') or {}).get('message')}},passed,['04-manager-invite-storemanager.json','04-manager-invite-teamleader.json','04-manager-invite-with-team-id.json'])

# ACC-EMG-V2-06 first (same-store manager adjustment)
h,r=call('06','manager-same-store-role-change','company/member/update',{'empId':3004,'role':'Pm','storeId':2001},mt)
passed=ok(r) and role(r)=='Pm' and storeid(r)=='2001'
result('06','店长本店调岗','成功，3004=Pm@2001',{'success':ok(r),'role':role(r),'storeId':storeid(r),'code':errcode(r)},passed,['06-manager-same-store-role-change.json'])

# ACC-EMG-V2-08 while target remains same-store
h,r=call('08','manager-promote-manager','company/member/update',{'empId':3004,'role':'StoreManager','storeId':2001},mt)
passed=not ok(r) and errcode(r)=='403' and '店长' in ((r.get('errorCode') or {}).get('message') or '')
result('08','店长提升他人为店长','403，仅 BOSS 可设置店长',{'success':ok(r),'code':errcode(r),'message':(r.get('errorCode') or {}).get('message')},passed,['08-manager-promote-manager.json'])

# ACC-EMG-V2-05 BOSS cross-store role+store
h,r=call('05','boss-cross-store-role-store','company/member/update',{'empId':3004,'role':'Designer','storeId':2002},bt)
sql_after=sql("SELECT emp_id,name,role,status,store_id,team_id,com_id FROM db_tenant_employee WHERE emp_id=3004; SELECT log_id,emp_id,action,operator_id,before_snapshot,after_snapshot FROM db_company_member_log WHERE emp_id=3004 ORDER BY create_date_time DESC LIMIT 3;")
sql_evidence['05']=sql_after
(EVID/'sql'/'05-boss-cross-store.txt').write_text(sql_after)
passed=ok(r) and role(r)=='Designer' and storeid(r)=='2002' and 'role=Pm,status=Active' in sql_after and 'store=2002,role=Designer' in sql_after
result('05','BOSS 跨店调岗调店','成功，3004=Designer@2002，日志前后值正确',{'success':ok(r),'role':role(r),'storeId':storeid(r),'sql':sql_after.strip()},passed,['05-boss-cross-store-role-store.json','sql/05-boss-cross-store.txt'])

# ACC-EMG-V2-07 manager cross-store
h,r=call('07','manager-cross-store','company/member/update',{'empId':3004,'role':'Pm','storeId':2002},mt)
result('07','店长跨店调整','403',{'success':ok(r),'code':errcode(r),'message':(r.get('errorCode') or {}).get('message')},not ok(r) and errcode(r)=='403',['07-manager-cross-store.json'])

# ACC-EMG-V2-15 cross-company
h1,r1=call('15','cross-company-member-info','company/member/info',{'empId':9503},bt)
h2,r2=call('15','cross-company-store-update','company/member/update',{'empId':3004,'role':'Designer','storeId':9502},bt)
passed=(not ok(r1) and errcode(r1)=='404') and (not ok(r2) and errcode(r2) in {'404','403'})
result('15','跨企业成员/门店','404/拒绝，不泄露存在性',{'member':{'success':ok(r1),'code':errcode(r1),'message':(r1.get('errorCode') or {}).get('message')},'store':{'success':ok(r2),'code':errcode(r2),'message':(r2.get('errorCode') or {}).get('message')}},passed,['15-cross-company-member-info.json','15-cross-company-store-update.json'])

# Prepare other manager, then ACC-EMG-V2-09
h0,r0=call('09','boss-promote-other-manager','company/member/update',{'empId':3003,'role':'StoreManager','storeId':2002},bt)
h1,r1=call('09','manager-update-self','company/member/update',{'empId':3002,'role':'Marketer','storeId':2001},mt)
h2,r2=call('09','manager-suspend-boss','company/member/suspend',{'empId':3001},mt)
h3,r3=call('09','manager-update-other-manager','company/member/update',{'empId':3003,'role':'Pm','storeId':2002},mt)
passed=ok(r0) and (not ok(r1) and errcode(r1) in {'400','403'}) and (not ok(r2) and errcode(r2)=='403') and (not ok(r3) and errcode(r3)=='403')
result('09','店长操作自己/BOSS/其他店长','页面无入口，接口均拒绝',{'setupOtherManager':ok(r0),'self':{'success':ok(r1),'code':errcode(r1),'message':(r1.get('errorCode') or {}).get('message')},'boss':{'success':ok(r2),'code':errcode(r2),'message':(r2.get('errorCode') or {}).get('message')},'otherManager':{'success':ok(r3),'code':errcode(r3),'message':(r3.get('errorCode') or {}).get('message')}},passed,['09-boss-promote-other-manager.json','09-manager-update-self.json','09-manager-suspend-boss.json','09-manager-update-other-manager.json'])

# ACC-EMG-V2-10 suspend and access
h,r=call('10','boss-suspend-employee','company/member/suspend',{'empId':3004},bt)
h2,login_s=api('auth/session/password-login',{'phone':EMP,'password':PASSWORD})
responses['10'].append({'route':'auth/session/password-login (suspended)','http':h2,'response':login_s})
(HTTP/'10-suspended-login.json').write_text(json.dumps({'route':'auth/session/password-login','http':h2,'response':login_s},ensure_ascii=False,indent=2)+'\n')
passed=ok(r) and status(r)=='Suspended' and not ok(login_s) and errcode(login_s)=='403'
result('10','停用员工访问','停用成功，登录 403',{'suspendSuccess':ok(r),'status':status(r),'loginSuccess':ok(login_s),'loginCode':errcode(login_s)},passed,['10-boss-suspend-employee.json','10-suspended-login.json'])

# ACC-EMG-V2-17a duplicate suspend
h,dup_suspend=call('17','duplicate-suspend','company/member/suspend',{'empId':3004},bt)

# ACC-EMG-V2-11 resume, login and duplicate resume
h,r=call('11','boss-resume-employee','company/member/resume',{'empId':3004},bt)
h2,login_r=api('auth/session/password-login',{'phone':EMP,'password':PASSWORD})
responses['11'].append({'route':'auth/session/password-login (resumed)','http':h2,'response':{'success':login_r.get('success'),'data':{'token':('<redacted>' if (login_r.get('data') or {}).get('token') else None)},'errorCode':login_r.get('errorCode')}})
(HTTP/'11-resumed-login.json').write_text(json.dumps({'route':'auth/session/password-login','http':h2,'response':{'success':login_r.get('success'),'data':{'token':('<redacted>' if (login_r.get('data') or {}).get('token') else None)},'errorCode':login_r.get('errorCode')}},ensure_ascii=False,indent=2)+'\n')
h3,dup_resume=call('17','duplicate-resume','company/member/resume',{'empId':3004},bt)
passed=ok(r) and status(r)=='Active' and ok(login_r) and not ok(dup_resume) and errcode(dup_resume)=='400'
result('11','恢复员工','成功，登录恢复；重复恢复明确 400',{'resumeSuccess':ok(r),'status':status(r),'loginSuccess':ok(login_r),'duplicateResumeCode':errcode(dup_resume)},passed,['11-boss-resume-employee.json','11-resumed-login.json','17-duplicate-resume.json'])

# Fixture: one in-hand customer owned by 3004
fixture_sql="""
INSERT INTO cust_customer (cust_id,name,status,tel_no,visit_count,wechat_added,com_id,emp_id,store_id)
SELECT 88001,'EMG-V2独立验收在手客户','Serving','13998000001',0,b'0',1001,3004,2002
WHERE NOT EXISTS (SELECT 1 FROM cust_customer WHERE cust_id=88001);
SELECT cust_id,name,status,com_id,emp_id,store_id FROM cust_customer WHERE cust_id=88001;
"""
fx=sql(fixture_sql); (SQL/'12-responsibility-fixture.txt').write_text(fx); sql_evidence['12-fixture']=fx

# ACC-EMG-V2-12 ordinary resign with responsibility
h,r=call('12','resign-with-responsibility','company/member/resign',{'empId':3004,'reason':'EMG-V2 普通离职未确认','forceHandover':False},bt)
passed=not ok(r) and errcode(r)=='409' and '未交接' in ((r.get('errorCode') or {}).get('message') or '')
result('12','有在手责任员工普通离职','409，要求交接或强制确认',{'success':ok(r),'code':errcode(r),'message':(r.get('errorCode') or {}).get('message')},passed,['12-resign-with-responsibility.json'])

# ACC-EMG-V2-13 confirmed force resign
h,r=call('13','resign-force-confirmed','company/member/resign',{'empId':3004,'reason':'EMG-V2 独立验收强制离职','forceHandover':True},bt)
sql_after=sql("SELECT emp_id,name,role,status,store_id,team_id,com_id,resigned_at,resign_reason FROM db_tenant_employee WHERE emp_id=3004; SELECT cust_id,emp_id,store_id,status FROM cust_customer WHERE cust_id=88001; SELECT log_id,emp_id,action,operator_id,reason,before_snapshot,after_snapshot FROM db_company_member_log WHERE emp_id=3004 ORDER BY create_date_time DESC LIMIT 3;")
sql_evidence['13']=sql_after; (SQL/'13-force-resign-facts.txt').write_text(sql_after)
passed=ok(r) and status(r)=='Resigned' and int((r.get('data') or {}).get('customerCount') or 0)>=1 and 'Resigned' in sql_after and 'EMG-V2 独立验收强制离职' in sql_after and '88001\t3004\t2002\tServing' in sql_after and 'operator_id=3001' not in sql_after
result('13','确认强制离职','Resigned；原因/时间落库；客户历史归属仍为 3004',{'success':ok(r),'status':status(r),'customerCount':(r.get('data') or {}).get('customerCount'),'sql':sql_after.strip()},passed,['13-resign-force-confirmed.json','sql/13-force-resign-facts.txt'])

# ACC-EMG-V2-17b duplicate resign is checked after case 14 to keep evidence ordering but same action target
# ACC-EMG-V2-16 Team freeze regression
h1,teams=call('16','team-list-readonly','user/team/list',{},bt)
h2,team_save=call('16','team-save-absent','user/team/save',{'name':'不得创建团队'},bt)
h3,tl_invite=call('16','invite-teamleader-boss','invite/invitation/save',{'inviteName':'冻结TeamLeader','invitePhone':'13992000007','role':'TeamLeader'},bt)
# temporary direct fixture to prove active TeamLeader cannot log in
sql("UPDATE db_tenant_employee SET role='TeamLeader' WHERE emp_id=3003;")
try:
    h4,tl_login=api('auth/session/password-login',{'phone':OTHER_EMP,'password':PASSWORD})
    responses['16'].append({'route':'auth/session/password-login (temporary TeamLeader)','http':h4,'response':tl_login})
    (HTTP/'16-teamleader-login.json').write_text(json.dumps({'route':'auth/session/password-login','http':h4,'response':tl_login},ensure_ascii=False,indent=2)+'\n')
finally:
    sql("UPDATE db_tenant_employee SET role='StoreManager' WHERE emp_id=3003;")
passed=ok(teams) and (teams.get('data')==[]) and (not ok(team_save) and errcode(team_save)=='404') and (not ok(tl_invite) and errcode(tl_invite) in {'400','403'}) and (not ok(tl_login) and errcode(tl_login) in {'403','400'})
result('16','Team/TeamLeader 回归','Team 只读空列表；写路由不存在；TeamLeader 邀请/登录拒绝',{'teamList':teams.get('data'),'teamSave':{'success':ok(team_save),'code':errcode(team_save)},'tlInvite':{'success':ok(tl_invite),'code':errcode(tl_invite)},'tlLogin':{'success':ok(tl_login),'code':errcode(tl_login)}},passed,['16-team-list-readonly.json','16-team-save-absent.json','16-invite-teamleader-boss.json','16-teamleader-login.json'])

# ACC-EMG-V2-14 admin delegated resign and operator
h,r=call('14','boss-delegated-resign','company/member/resign',{'empId':3003,'reason':'EMG-V2 独立验收管理员代办离职'},bt)
sql_after=sql("SELECT emp_id,name,role,status,store_id,com_id,resigned_at,resign_reason FROM db_tenant_employee WHERE emp_id=3003; SELECT log_id,emp_id,action,operator_id,reason,before_snapshot,after_snapshot FROM db_company_member_log WHERE emp_id=3003 ORDER BY create_date_time DESC LIMIT 3;")
sql_evidence['14']=sql_after; (SQL/'14-admin-delegated-facts.txt').write_text(sql_after)
passed=ok(r) and status(r)=='Resigned' and 'operator_id=3001' in sql_after and 'Resigned' in sql_after and 'EMG-V2 独立验收管理员代办离职' in sql_after
result('14','管理员代办离职','操作人=3001，目标=3003',{'success':ok(r),'status':status(r),'sql':sql_after.strip()},passed,['14-boss-delegated-resign.json','sql/14-admin-delegated-facts.txt'])

# complete duplicate resign probe
h,dup_resign=call('17','duplicate-resign','company/member/resign',{'empId':3004,'reason':'重复离职','forceHandover':True},bt)
passed=not ok(dup_suspend) and errcode(dup_suspend)=='400' and not ok(dup_resume) and errcode(dup_resume)=='400' and not ok(dup_resign) and errcode(dup_resign)=='400'
result('17','重复停用/恢复/离职','均明确状态错误，不产生错误有效状态',{'duplicateSuspend':errcode(dup_suspend),'duplicateResume':errcode(dup_resume),'duplicateResign':errcode(dup_resign)},passed,['17-duplicate-suspend.json','17-duplicate-resume.json','17-duplicate-resign.json'])

# ACC-EMG-V2-18 API fact snapshot for page comparison; page evidence appended later
h,members=call('18','final-api-members','company/member/page',{'current':1,'size':200},bt)
rows={str(x.get('empId')):{'name':x.get('name'),'role':(x.get('role') or {}).get('key'),'status':(x.get('status') or {}).get('key'),'storeId':str((x.get('store') or {}).get('storeId') or '')} for x in ((members.get('data') or {}).get('content') or [])}
passed=ok(members) and rows.get('3004',{}).get('status')=='Resigned' and rows.get('3004',{}).get('storeId')=='2002' and rows.get('3003',{}).get('status')=='Resigned'
result('18','App/Web 双端同源','页面状态/门店/岗位与接口一致',{'apiSuccess':ok(members),'rows':rows,'pageEvidencePending':True},passed,['18-final-api-members.json'])
# Result remains provisional until page capture; mark result text explicitly in summary.

# Final DB snapshot
final_sql=sql("SELECT emp_id,name,role,status,store_id,team_id,com_id,resigned_at,resign_reason FROM db_tenant_employee ORDER BY emp_id; SELECT log_id,emp_id,action,operator_id,reason,create_date_time FROM db_company_member_log ORDER BY create_date_time DESC LIMIT 30; SELECT team_id,name,com_id,store_id FROM db_tenant_team WHERE com_id=1001; SELECT invite_id,invite_name,tel_no,role,status,store_id,team_id,com_id FROM db_tenant_invite WHERE com_id=1001 ORDER BY invite_id DESC LIMIT 20;")
(SQL/'18-final-db-snapshot.txt').write_text(final_sql)
summary={'generatedAt':time.strftime('%Y-%m-%dT%H:%M:%S%z'),'cases':cases,'httpResponses':responses,'notes':['Tokens redacted from all evidence.','Direct SQL was used only for acceptance fixtures and readback, not implementation changes.','Case 18 page evidence is captured separately and final verdict is assembled after page review.']}
(EVID/'http'/'acceptance-results.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({k:{'result':v['result'],'actual':v['actual']} for k,v in cases.items()},ensure_ascii=False,indent=2))
print('FAILS', [k for k,v in cases.items() if v['result']!='PASS'])
