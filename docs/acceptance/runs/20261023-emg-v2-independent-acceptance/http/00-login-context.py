import json, subprocess, os
from pathlib import Path
BASE='http://127.0.0.1:8090/api/'
OUT=Path(__file__).resolve().parent

def post(route, body, token=None):
    cmd=['curl','-sS','-w','\n%{http_code}','-X','POST',BASE+route,'-H','Content-Type: application/json']
    if token:
        cmd += ['-H','token: '+token]
    cmd += ['-d',json.dumps(body,ensure_ascii=False)]
    raw=subprocess.check_output(cmd,text=True)
    payload,code=raw.rsplit('\n',1)
    return int(code),json.loads(payload)

PASSWORD=os.environ.get('EMG_ACCEPT_PASSWORD')
if not PASSWORD: raise SystemExit('missing EMG_ACCEPT_PASSWORD')
result={}
for name, phone in [('boss','13990000001'),('manager','13990000002')]:
    status, login=post('auth/session/password-login', {'phone':phone,'password':PASSWORD})
    token=(login.get('data') or {}).get('token')
    item={'login_http':status,'login_success':login.get('success'),'token_present':bool(token)}
    if token:
        for route,body in [('auth/session/info',{}),('company/member/page',{'current':1,'size':200}),('user/store/list',{}),('user/team/list',{}),('invite/invitation/page',{'current':1,'size':200})]:
            s,data=post(route,body,token)
            item[route]={'http':s,'response':data}
    result[name]=item
(OUT/'00-login-context.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
summary={}
for k,v in result.items():
    mp=(v.get('company/member/page') or {}).get('response',{})
    stores=(v.get('user/store/list') or {}).get('response',{})
    teams=(v.get('user/team/list') or {}).get('response',{})
    summary[k]={'login':v['login_success'],'token':v['token_present'],'member_page':mp.get('success'),'members':[{'empId':x.get('empId'),'name':x.get('name'),'role':(x.get('role') or {}).get('key'),'status':(x.get('status') or {}).get('key'),'storeId':(x.get('store') or {}).get('storeId')} for x in (mp.get('data') or {}).get('content',[])],'stores':stores.get('data'),'teams':teams.get('data'),'storeError':stores.get('errorCode'),'teamError':teams.get('errorCode')}
print(json.dumps(summary,ensure_ascii=False,indent=2))
